package br.com.connecta.repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

/** Implementação JDBC de NeighborhoodRepository. Sempre PreparedStatement. */
public class JdbcNeighborhoodRepository implements NeighborhoodRepository {

    private final DataSource dataSource;

    public JdbcNeighborhoodRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Long> findIdByNameIgnoreCase(String name) {
        var sql = "SELECT id FROM neighborhood WHERE lower(name) = lower(?)";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong("id")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar bairro", e);
        }
    }

    @Override
    public long create(String name) {
        var sql = "INSERT INTO neighborhood (name) VALUES (?)";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao criar bairro", e);
        }
    }
}
