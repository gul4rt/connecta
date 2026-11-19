package br.com.connecta.repository;

import br.com.connecta.domain.ProviderCard;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação JDBC de ProviderRepository. Sempre PreparedStatement.
 * Duas consultas: os prestadores e, em seguida, as habilidades "sei fazer"
 * de todos eles de uma vez (evita uma consulta por card — N+1).
 */
public class JdbcProviderRepository implements ProviderRepository {

    private final DataSource dataSource;

    public JdbcProviderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String BASE = """
        SELECT r.id, r.display_name, r.username, r.jobs_done,
               (r.verified_at IS NOT NULL) AS verified, n.name AS neighborhood_name,
               (SELECT ROUND(AVG(rating)::numeric, 1) FROM review rv WHERE rv.provider_id = r.id) AS avg_rating,
               (SELECT COUNT(*) FROM review rv WHERE rv.provider_id = r.id) AS review_count
        FROM resident r
        JOIN neighborhood n ON n.id = r.neighborhood_id
        WHERE r.role = 'provider' AND r.profile_completed
        """;

    @Override
    public List<ProviderCard> findByNeighborhood(long neighborhoodId) {
        var sql = BASE + "AND r.neighborhood_id = ? "
            + "ORDER BY r.verified_at DESC NULLS LAST, r.jobs_done DESC, r.display_name";
        return listar(sql, neighborhoodId);
    }

    @Override
    public List<ProviderCard> findAll() {
        var sql = BASE + "ORDER BY r.verified_at DESC NULLS LAST, r.jobs_done DESC, r.display_name";
        return listar(sql, null);
    }

    @Override
    public Optional<ProviderCard> findById(long providerId) {
        var sql = BASE + "AND r.id = ?";
        var todos = listar(sql, providerId);
        return todos.isEmpty() ? Optional.empty() : Optional.of(todos.get(0));
    }

    // ---------- Internos ----------

    private List<ProviderCard> listar(String sql, Long filtro) {
        var prestadores = new ArrayList<ProviderCard>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            if (filtro != null) {
                stmt.setLong(1, filtro);
            }
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    var media = rs.getBigDecimal("avg_rating");
                    prestadores.add(new ProviderCard(
                        rs.getLong("id"),
                        rs.getString("display_name"),
                        rs.getString("username"),
                        rs.getString("neighborhood_name"),
                        rs.getInt("jobs_done"),
                        rs.getBoolean("verified"),
                        new ArrayList<>(), new ArrayList<>(),
                        media != null ? media.doubleValue() : null,
                        rs.getLong("review_count")));
                }
            }
            if (!prestadores.isEmpty()) {
                anexarHabilidades(conn, prestadores);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar prestadores", e);
        }
        return prestadores;
    }

    /** Preenche ofícios e categorias dos cards com UMA consulta (sem N+1). */
    private void anexarHabilidades(java.sql.Connection conn, List<ProviderCard> cards)
            throws SQLException {
        var ids = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                ids.append(',');
            }
            ids.append('?');
        }
        var sql = """
            SELECT s.resident_id, s.name AS skill_name, c.slug AS category_slug
            FROM skill s
            JOIN service_category c ON c.id = s.category_id
            WHERE s.kind = 'have' AND s.resident_id IN (%s)
            ORDER BY s.created_at
            """.formatted(ids);

        Map<Long, ProviderCard> porId = new HashMap<>();
        for (ProviderCard card : cards) {
            porId.put(card.providerId(), card);
        }

        try (var stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < cards.size(); i++) {
                stmt.setLong(i + 1, cards.get(i).providerId());
            }
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProviderCard base = porId.get(rs.getLong("resident_id"));
                    if (base == null) {
                        continue;
                    }
                    var oficios = new ArrayList<>(base.oficios());
                    var categorias = new ArrayList<>(base.categorias());
                    oficios.add(rs.getString("skill_name"));
                    if (!categorias.contains(rs.getString("category_slug"))) {
                        categorias.add(rs.getString("category_slug"));
                    }
                    porId.put(base.providerId(), new ProviderCard(
                        base.providerId(), base.displayName(), base.username(),
                        base.neighborhoodName(), base.jobsDone(), base.verified(),
                        oficios, categorias, base.mediaAvaliacoes(), base.reviewCount()));
                }
            }
        }
        // devolve na ordem original (SELECT com IN não garante ordem)
        for (int i = 0; i < cards.size(); i++) {
            cards.set(i, porId.get(cards.get(i).providerId()));
        }
    }
}
