package br.com.connecta.repository;

import br.com.connecta.domain.Review;
import br.com.connecta.domain.ReviewTarget;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação JDBC de ReviewRepository. Sempre PreparedStatement.
 *
 * Os INSERTs embutem a regra "só avalia quem contratou": o SELECT de
 * origem só devolve linha quando a demanda está atendida com proposta
 * ACEITA (ou o pedido direto ACEITO) e ainda não tem avaliação. Se a
 * pessoa não contratou, o INSERT não insere nada — o banco é quem garante.
 *
 * Selo por boas avaliações (regra documentada em docs/DECISOES.md):
 * média >= 4 com no mínimo 2 avaliações — concedido na mesma transação.
 */
public class JdbcReviewRepository implements ReviewRepository {

    private final DataSource dataSource;

    public JdbcReviewRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // limiar do selo por avaliações (mantido junto da consulta que aplica)
    private static final int NOTA_MEDIA_MINIMA = 4;
    private static final int AVALIACOES_MINIMAS = 2;

    @Override
    public Optional<ReviewTarget> alvoDemanda(long demandId, long reviewerId) {
        var sql = """
            SELECT r.display_name AS provider_name
            FROM demand d
            JOIN proposal p ON p.demand_id = d.id AND p.status = 'accepted'
            JOIN resident r ON r.id = p.provider_id
            WHERE d.id = ? AND d.resident_id = ? AND d.status = 'attended'
              AND NOT EXISTS (SELECT 1 FROM review WHERE demand_id = d.id)
            """;
        return consultarAlvo(sql, demandId, reviewerId);
    }

    @Override
    public Optional<ReviewTarget> alvoPedido(long requestId, long reviewerId) {
        var sql = """
            SELECT r.display_name AS provider_name
            FROM service_request sr
            JOIN resident r ON r.id = sr.provider_id
            WHERE sr.id = ? AND sr.requester_id = ? AND sr.status = 'accepted'
              AND NOT EXISTS (SELECT 1 FROM review WHERE request_id = sr.id)
            """;
        return consultarAlvo(sql, requestId, reviewerId);
    }

    @Override
    public boolean existePorDemanda(long demandId) {
        return existe("SELECT 1 FROM review WHERE demand_id = ?", demandId);
    }

    @Override
    public boolean existePorPedido(long requestId) {
        return existe("SELECT 1 FROM review WHERE request_id = ?", requestId);
    }

    @Override
    public boolean avaliarDemanda(long demandId, long reviewerId, int rating, String comment) {
        var sql = """
            INSERT INTO review (demand_id, reviewer_id, provider_id, rating, comment)
            SELECT d.id, ?, p.provider_id, ?, ?
            FROM demand d
            JOIN proposal p ON p.demand_id = d.id AND p.status = 'accepted'
            WHERE d.id = ? AND d.resident_id = ? AND d.status = 'attended'
              AND NOT EXISTS (SELECT 1 FROM review WHERE demand_id = d.id)
            """;
        return avaliar(sql, demandId, reviewerId, rating, comment);
    }

    @Override
    public boolean avaliarPedido(long requestId, long reviewerId, int rating, String comment) {
        var sql = """
            INSERT INTO review (request_id, reviewer_id, provider_id, rating, comment)
            SELECT sr.id, ?, sr.provider_id, ?, ?
            FROM service_request sr
            WHERE sr.id = ? AND sr.requester_id = ? AND sr.status = 'accepted'
              AND NOT EXISTS (SELECT 1 FROM review WHERE request_id = sr.id)
            """;
        return avaliar(sql, requestId, reviewerId, rating, comment);
    }

    // ---------- Internos ----------

    @Override
    public List<Review> findByProvider(long providerId) {
        var sql = """
            SELECT rv.id, rv.provider_id, rv.rating, rv.comment, rv.created_at,
                   r.display_name AS reviewer_name
            FROM review rv
            JOIN resident r ON r.id = rv.reviewer_id
            WHERE rv.provider_id = ?
            ORDER BY rv.created_at DESC
            """;
        var avaliacoes = new ArrayList<Review>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, providerId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    avaliacoes.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar avaliações do prestador", e);
        }
        return avaliacoes;
    }

    private static Review mapear(ResultSet rs) throws SQLException {
        Timestamp criada = rs.getTimestamp("created_at");
        String nome = rs.getString("reviewer_name");
        // avaliação é pública, mas só o primeiro nome vai para a tela
        if (nome != null && nome.contains(" ")) {
            nome = nome.substring(0, nome.indexOf(' '));
        }
        return new Review(
            rs.getLong("id"),
            rs.getLong("provider_id"),
            nome,
            rs.getInt("rating"),
            rs.getString("comment"),
            criada != null ? criada.toInstant().atOffset(ZoneOffset.UTC) : null);
    }

    private Optional<ReviewTarget> consultarAlvo(String sql, long alvoId, long reviewerId) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, alvoId);
            stmt.setLong(2, reviewerId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ReviewTarget("", alvoId, rs.getString("provider_name")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar trabalho para avaliar", e);
        }
    }

    private boolean existe(String sql, long id) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao verificar avaliação", e);
        }
    }

    /**
     * INSERT + concessão do selo na MESMA transação: ou os dois acontecem,
     * ou nenhum. Retorna o provider_id da avaliação gravada (ou vazio).
     */
    private boolean avaliar(String sql, long alvoId, long reviewerId, int rating, String comment) {
        var sqlSelo = """
            UPDATE resident SET verified_at = now()
            WHERE verified_at IS NULL
              AND id = (SELECT provider_id FROM review WHERE id = ?)
              AND (SELECT AVG(rating) FROM review WHERE provider_id = resident.id) >= ?
              AND (SELECT COUNT(*) FROM review WHERE provider_id = resident.id) >= ?
            """;
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long providerId = -1;
                long reviewId = -1;
                try (var stmt = conn.prepareStatement(sql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    // ordem dos ? no SQL: reviewer_id, rating, comment,
                    // alvo (demand/pedido), reviewer_id (checagem de dono)
                    stmt.setLong(1, reviewerId);
                    stmt.setInt(2, rating);
                    stmt.setString(3, comment);
                    stmt.setLong(4, alvoId);
                    stmt.setLong(5, reviewerId);
                    int linhas = stmt.executeUpdate();
                    if (linhas == 0) {
                        conn.rollback();
                        return false; // não contratou / já avaliou / serviço não aceito
                    }
                    try (var keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            reviewId = keys.getLong(1);
                        }
                    }
                }
                try (var stmt = conn.prepareStatement(
                        "SELECT provider_id FROM review WHERE id = ?")) {
                    stmt.setLong(1, reviewId);
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            providerId = rs.getLong(1);
                        }
                    }
                }
                if (providerId > 0) {
                    try (var stmt = conn.prepareStatement(sqlSelo)) {
                        stmt.setLong(1, reviewId);
                        stmt.setInt(2, NOTA_MEDIA_MINIMA);
                        stmt.setInt(3, AVALIACOES_MINIMAS);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao gravar avaliação", e);
        }
    }
}
