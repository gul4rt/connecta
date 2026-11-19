package br.com.connecta.repository;

import br.com.connecta.domain.Track;
import br.com.connecta.domain.TrackConcluded;
import br.com.connecta.domain.TrackDetail;
import br.com.connecta.domain.TrackProgress;
import br.com.connecta.domain.TrackStep;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Implementação JDBC de TrackRepository. Sempre PreparedStatement.
 * A conclusão da trilha (concluir) é transacional: inscrição e selo
 * são gravados juntos ou nada é gravado.
 */
public class JdbcTrackRepository implements TrackRepository {

    private final DataSource dataSource;

    public JdbcTrackRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // SELECT base de trilha (join dá o nome da categoria pronto)
    private static final String BASE_TRACK = """
        SELECT t.id, t.slug, t.title, t.description, t.category_slug, c.name AS category_name
        FROM track t
        JOIN service_category c ON c.slug = t.category_slug
        """;

    @Override
    public List<TrackProgress> listAll(Long residentId) {
        // Uma consulta: trilha + contagem de etapas + progresso do morador
        var sql = """
            SELECT t.id, t.slug, t.title, t.description, t.category_slug,
                   c.name AS category_name,
                   (SELECT COUNT(*) FROM track_step s WHERE s.track_id = t.id) AS total_etapas,
                   (SELECT COUNT(*) FROM track_step_progress p
                     JOIN track_step s2 ON s2.id = p.step_id
                     WHERE s2.track_id = t.id AND p.resident_id = ?) AS etapas_concluidas,
                   e.id AS enrollment_id,
                   e.status AS enrollment_status
            FROM track t
            JOIN service_category c ON c.slug = t.category_slug
            LEFT JOIN track_enrollment e ON e.track_id = t.id AND e.resident_id = ?
            ORDER BY t.id
            """;
        var trilhas = new ArrayList<TrackProgress>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId == null ? -1 : residentId);
            stmt.setLong(2, residentId == null ? -1 : residentId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long total = rs.getLong("total_etapas");
                    long feitas = rs.getLong("etapas_concluidas");
                    boolean inscrita = rs.getLong("enrollment_id") > 0;
                    boolean concluida = inscrita && "completed".equals(rs.getString("enrollment_status"));
                    trilhas.add(new TrackProgress(mapearTrilha(rs), inscrita, concluida, feitas, total));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar trilhas", e);
        }
        return trilhas;
    }

    @Override
    public Optional<TrackDetail> findDetail(String slug, Long residentId) {
        try (var conn = dataSource.getConnection()) {
            // 1) a trilha
            Long trackId = null;
            Track trilha = null;
            var sqlTrilha = BASE_TRACK + " WHERE t.slug = ?";
            try (var stmt = conn.prepareStatement(sqlTrilha)) {
                stmt.setString(1, slug);
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        trilha = mapearTrilha(rs);
                        trackId = trilha.id();
                    }
                }
            }
            if (trilha == null) {
                return Optional.empty();
            }

            // 2) a inscrição (pode não existir)
            boolean inscrita = false;
            boolean concluida = false;
            Timestamp concluidaEm = null;
            var sqlInscricao = "SELECT status, completed_at FROM track_enrollment "
                + "WHERE track_id = ? AND resident_id = ?";
            try (var stmt = conn.prepareStatement(sqlInscricao)) {
                stmt.setLong(1, trackId);
                stmt.setLong(2, residentId == null ? -1 : residentId);
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        inscrita = true;
                        concluida = "completed".equals(rs.getString("status"));
                        concluidaEm = rs.getTimestamp("completed_at");
                    }
                }
            }

            // 3) as etapas em ordem, com links e o progresso do morador
            var sqlEtapas = """
                SELECT s.id, s.position, s.title,
                       (p.resident_id IS NOT NULL) AS done
                FROM track_step s
                LEFT JOIN track_step_progress p
                       ON p.step_id = s.id AND p.resident_id = ?
                WHERE s.track_id = ?
                ORDER BY s.position
                """;
            var etapas = new LinkedHashMap<Long, TrackStep>();
            try (var stmt = conn.prepareStatement(sqlEtapas)) {
                stmt.setLong(1, residentId == null ? -1 : residentId);
                stmt.setLong(2, trackId);
                try (var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        etapas.put(id, new TrackStep(id, rs.getInt("position"),
                            rs.getString("title"), new ArrayList<>(), rs.getBoolean("done")));
                    }
                }
            }
            var sqlLinks = "SELECT step_id, url FROM playlist WHERE step_id IN ("
                + "SELECT id FROM track_step WHERE track_id = ?) ORDER BY step_id, position";
            try (var stmt = conn.prepareStatement(sqlLinks)) {
                stmt.setLong(1, trackId);
                try (var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long stepId = rs.getLong("step_id");
                        TrackStep etapa = etapas.get(stepId);
                        if (etapa != null) {
                            List<String> urls = new ArrayList<>(etapa.playlistUrls());
                            urls.add(rs.getString("url"));
                            etapas.put(stepId, new TrackStep(etapa.id(), etapa.position(),
                                etapa.title(), urls, etapa.done()));
                        }
                    }
                }
            }

            var detail = new TrackDetail(trilha, inscrita, concluida,
                concluidaEm != null ? concluidaEm.toInstant().atOffset(ZoneOffset.UTC) : null,
                new ArrayList<>(etapas.values()));
            return Optional.of(detail);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar trilha " + slug, e);
        }
    }

    @Override
    public boolean inscrever(long residentId, String slug) {
        var sql = """
            INSERT INTO track_enrollment (resident_id, track_id)
            SELECT ?, t.id FROM track t WHERE t.slug = ?
            ON CONFLICT (resident_id, track_id) DO NOTHING
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            stmt.setString(2, slug);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao inscrever na trilha", e);
        }
    }

    @Override
    public boolean marcarEtapa(long residentId, String slug, long stepId) {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // inscrição garante que a pessoa começou a trilha (idempotente)
                var sqlInscricao = """
                    INSERT INTO track_enrollment (resident_id, track_id)
                    SELECT ?, t.id FROM track t WHERE t.slug = ?
                    ON CONFLICT (resident_id, track_id) DO NOTHING
                    """;
                try (var stmt = conn.prepareStatement(sqlInscricao)) {
                    stmt.setLong(1, residentId);
                    stmt.setString(2, slug);
                    stmt.executeUpdate();
                }
                // a etapa só entra se pertencer à trilha do slug
                var sqlEtapa = """
                    INSERT INTO track_step_progress (resident_id, step_id)
                    SELECT ?, s.id
                    FROM track_step s
                    JOIN track t ON t.id = s.track_id
                    WHERE s.id = ? AND t.slug = ?
                    ON CONFLICT (resident_id, step_id) DO NOTHING
                    """;
                try (var stmt = conn.prepareStatement(sqlEtapa)) {
                    stmt.setLong(1, residentId);
                    stmt.setLong(2, stepId);
                    stmt.setString(3, slug);
                    boolean gravou = stmt.executeUpdate() > 0;
                    conn.commit();
                    return gravou;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao marcar etapa da trilha", e);
        }
    }

    @Override
    public boolean concluir(long residentId, String slug) {
        // Transação: atualiza a inscrição E concede o selo juntos.
        // A checagem "todas as etapas concluídas" fica no próprio UPDATE
        // (NOT EXISTS), então ninguém conclui pulando etapas.
        var sqlInscricao = """
            UPDATE track_enrollment e
            SET status = 'completed', completed_at = now(), term_accepted_at = now()
            FROM track t
            WHERE t.id = e.track_id AND e.resident_id = ? AND t.slug = ?
              AND e.status = 'in_progress'
              AND NOT EXISTS (
                SELECT 1 FROM track_step s
                WHERE s.track_id = t.id
                  AND NOT EXISTS (
                    SELECT 1 FROM track_step_progress p
                    WHERE p.step_id = s.id AND p.resident_id = e.resident_id))
            """;
        var sqlSelo = "UPDATE resident SET verified_at = now() WHERE id = ? AND verified_at IS NULL";
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean concluiu;
                try (var stmt = conn.prepareStatement(sqlInscricao)) {
                    stmt.setLong(1, residentId);
                    stmt.setString(2, slug);
                    concluiu = stmt.executeUpdate() > 0;
                }
                if (concluiu) {
                    try (var stmt = conn.prepareStatement(sqlSelo)) {
                        stmt.setLong(1, residentId);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                return concluiu;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao concluir trilha", e);
        }
    }

    @Override
    public long countConcluidas(long residentId) {
        var sql = "SELECT COUNT(*) FROM track_enrollment WHERE resident_id = ? AND status = 'completed'";
        return contarLong(sql, residentId);
    }

    @Override
    public boolean temSelo(long residentId) {
        var sql = "SELECT verified_at IS NOT NULL FROM resident WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao verificar selo do morador", e);
        }
    }

    @Override
    public Optional<Track> findBySlug(String slug) {
        var sql = BASE_TRACK + " WHERE t.slug = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slug);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearTrilha(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar trilha", e);
        }
    }

    @Override
    public List<TrackConcluded> trilhasConcluidasDe(long residentId) {
        var sql = """
            SELECT t.title, e.completed_at
            FROM track_enrollment e
            JOIN track t ON t.id = e.track_id
            WHERE e.resident_id = ? AND e.status = 'completed'
            ORDER BY e.completed_at DESC
            """;
        var trilhas = new ArrayList<TrackConcluded>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp concluida = rs.getTimestamp("completed_at");
                    trilhas.add(new TrackConcluded(
                        rs.getString("title"),
                        concluida != null ? concluida.toInstant().atOffset(ZoneOffset.UTC) : null));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar trilhas concluídas", e);
        }
        return trilhas;
    }

    // ---------- Internos ----------

    private long contarLong(String sql, long residentId) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao contar no banco", e);
        }
    }

    private static Track mapearTrilha(ResultSet rs) throws SQLException {
        return new Track(
            rs.getLong("id"),
            rs.getString("slug"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("category_slug"),
            rs.getString("category_name"));
    }
}
