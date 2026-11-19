package br.com.connecta.repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

/** Implementação JDBC de SessionRepository. Sempre PreparedStatement. */
public class JdbcSessionRepository implements SessionRepository {

    private final DataSource dataSource;

    public JdbcSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(String token, long residentId) {
        var sql = "INSERT INTO web_session (token, resident_id) VALUES (?, ?)";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setLong(2, residentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao criar sessão", e);
        }
    }

    @Override
    public Optional<Session> findValid(String token) {
        var sql = "SELECT token, resident_id FROM web_session "
            + "WHERE token = ? AND expires_at > now()";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (var rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Session(
                    rs.getString("token"),
                    rs.getLong("resident_id")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar sessão", e);
        }
    }

    @Override
    public void delete(String token) {
        var sql = "DELETE FROM web_session WHERE token = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao encerrar sessão", e);
        }
    }

    @Override
    public void deleteByResident(long residentId) {
        var sql = "DELETE FROM web_session WHERE resident_id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao encerrar sessões do morador", e);
        }
    }
}
