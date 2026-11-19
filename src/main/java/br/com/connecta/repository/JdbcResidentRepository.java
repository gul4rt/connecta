package br.com.connecta.repository;

import br.com.connecta.domain.Resident;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/** Implementação JDBC de ResidentRepository. Sempre PreparedStatement. */
public class JdbcResidentRepository implements ResidentRepository {

    // Join com neighborhood já traz o nome do bairro junto
    private static final String SELECT_COM_BAIRRO = """
        SELECT r.id, r.username, r.role, r.display_name, r.phone,
               r.neighborhood_id, n.name AS neighborhood_name,
               r.consent_at, r.profile_completed, r.jobs_done
        FROM resident r
        LEFT JOIN neighborhood n ON n.id = r.neighborhood_id
        """;

    private final DataSource dataSource;

    public JdbcResidentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long create(String username, String passwordHash, String role, String recoveryCodeHash) {
        var sql = "INSERT INTO resident (username, password_hash, role, recovery_code_hash, consent_at) "
            + "VALUES (?, ?, ?, ?, now())";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.setString(4, recoveryCodeHash);
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao criar conta", e);
        }
    }

    @Override
    public Optional<Resident> findByUsername(String username) {
        var sql = SELECT_COM_BAIRRO + " WHERE r.username = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar morador por username", e);
        }
    }

    @Override
    public Optional<String> findPasswordHashByUsername(String username) {
        var sql = "SELECT password_hash FROM resident WHERE username = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("password_hash")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar hash de senha", e);
        }
    }

    @Override
    public Optional<Resident> findById(long id) {
        var sql = SELECT_COM_BAIRRO + " WHERE r.id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar morador por id", e);
        }
    }

    @Override
    public void updateProfile(long id, String displayName, String phone,
                              Long neighborhoodId, boolean profileCompleted) {
        var sql = """
            UPDATE resident
            SET display_name = ?, phone = ?, neighborhood_id = ?, profile_completed = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, displayName);
            stmt.setString(2, phone);
            if (neighborhoodId == null) {
                stmt.setNull(3, java.sql.Types.BIGINT);
            } else {
                stmt.setLong(3, neighborhoodId);
            }
            stmt.setBoolean(4, profileCompleted);
            stmt.setLong(5, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar perfil", e);
        }
    }

    @Override
    public void updatePasswordAndRecoveryCode(long id, String passwordHash, String recoveryCodeHash) {
        var sql = "UPDATE resident SET password_hash = ?, recovery_code_hash = ? WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, recoveryCodeHash);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao trocar senha", e);
        }
    }

    @Override
    public Optional<String> findRecoveryCodeHashByUsername(String username) {
        var sql = "SELECT recovery_code_hash FROM resident WHERE username = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString("recovery_code_hash")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar código de recuperação", e);
        }
    }

    @Override
    public void delete(long id) {
        var sql = "DELETE FROM resident WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao excluir conta", e);
        }
    }

    private static Resident mapear(ResultSet rs) throws SQLException {
        Timestamp consent = rs.getTimestamp("consent_at");
        return new Resident(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("role"),
            rs.getString("display_name"),
            rs.getString("phone"),
            rs.getObject("neighborhood_id") != null ? rs.getLong("neighborhood_id") : null,
            rs.getString("neighborhood_name"),
            consent != null ? consent.toInstant().atOffset(java.time.ZoneOffset.UTC) : null,
            rs.getBoolean("profile_completed"),
            rs.getInt("jobs_done"));
    }
}
