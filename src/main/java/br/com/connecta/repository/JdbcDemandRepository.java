package br.com.connecta.repository;

import br.com.connecta.domain.Demand;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Implementação JDBC de DemandRepository. Sempre PreparedStatement. */
public class JdbcDemandRepository implements DemandRepository {

    private final DataSource dataSource;

    public JdbcDemandRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(long residentId, long categoryId, long neighborhoodId,
                       String description, BigDecimal offerAmount, String contactPhone) {
        var sql = """
            INSERT INTO demand (resident_id, category_id, neighborhood_id, description,
                                offer_amount, contact_phone)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            stmt.setLong(2, categoryId);
            stmt.setLong(3, neighborhoodId);
            stmt.setString(4, description);
            stmt.setBigDecimal(5, offerAmount);
            stmt.setString(6, contactPhone);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao registrar demanda", e);
        }
    }

    @Override
    public List<Demand> findOpenByNeighborhood(long neighborhoodId) {
        var sql = BASE + "WHERE d.neighborhood_id = ? AND d.status = 'open' "
            + "ORDER BY d.created_at DESC";
        return listar(sql, neighborhoodId);
    }

    @Override
    public List<Demand> findOpenAll() {
        var sql = BASE + "WHERE d.status = 'open' ORDER BY d.created_at DESC";
        return listar(sql, null);
    }

    @Override
    public java.util.Optional<Demand> findById(long id) {
        var sql = BASE + "WHERE d.id = ?";
        var todas = listar(sql, id);
        return todas.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(todas.get(0));
    }

    @Override
    public List<Demand> findByResident(long residentId) {
        var sql = BASE + "WHERE d.resident_id = ? ORDER BY d.created_at DESC";
        return listar(sql, residentId);
    }

    /**
     * Lacuna por categoria com uma única consulta: o GROUP BY deixa o banco
     * agregar (SQL visível e direto) e o Java só mapeia o resultado.
     */
    @Override
    public List<CategoryCount> countOpenByCategory(long neighborhoodId) {
        var sql = """
            SELECT c.id AS category_id, c.name AS category_name, COUNT(*) AS total
            FROM demand d
            JOIN service_category c ON c.id = d.category_id
            WHERE d.neighborhood_id = ? AND d.status = 'open'
            GROUP BY c.id, c.name
            ORDER BY total DESC, c.name
            """;
        var contagens = new ArrayList<CategoryCount>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, neighborhoodId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    contagens.add(new CategoryCount(
                        rs.getLong("category_id"),
                        rs.getString("category_name"),
                        rs.getLong("total")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao contar demandas por categoria", e);
        }
        return contagens;
    }

    @Override
    public boolean markAttended(long id, long residentId) {
        var sql = "UPDATE demand SET status = 'attended' WHERE id = ? AND resident_id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, residentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao marcar demanda como atendida", e);
        }
    }

    @Override
    public boolean delete(long id, long residentId) {
        var sql = "DELETE FROM demand WHERE id = ? AND resident_id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, residentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao excluir demanda", e);
        }
    }

    // SELECT base compartilhado pelas três listagens (join dá nomes prontos)
    private static final String BASE = """
        SELECT d.id, d.resident_id, r.username AS owner_username,
               c.name AS category_name, n.name AS neighborhood_name,
               d.description, d.offer_amount, d.contact_phone, d.status, d.created_at
        FROM demand d
        JOIN service_category c ON c.id = d.category_id
        JOIN neighborhood n ON n.id = d.neighborhood_id
        JOIN resident r ON r.id = d.resident_id
        """;

    private List<Demand> listar(String sql, Long filtro) {
        var demandas = new ArrayList<Demand>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            if (filtro != null) {
                stmt.setLong(1, filtro);
            }
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    demandas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar demandas", e);
        }
        return demandas;
    }

    private static Demand mapear(ResultSet rs) throws SQLException {
        Timestamp criada = rs.getTimestamp("created_at");
        return new Demand(
            rs.getLong("id"),
            rs.getLong("resident_id"),
            rs.getString("owner_username"),
            rs.getString("category_name"),
            rs.getString("neighborhood_name"),
            rs.getString("description"),
            rs.getBigDecimal("offer_amount"),
            rs.getString("contact_phone"),
            rs.getString("status"),
            criada != null ? criada.toInstant().atOffset(java.time.ZoneOffset.UTC) : null);
    }
}
