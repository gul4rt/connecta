package br.com.connecta.repository;

import br.com.connecta.domain.ServiceRequest;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JDBC de ServiceRequestRepository. Sempre PreparedStatement.
 * O telefone do prestador vem no SELECT (join com resident), mas a REGRA
 * de exibir só quando aceito fica no serviço — ver ServiceRequestService.
 */
public class JdbcServiceRequestRepository implements ServiceRequestRepository {

    private final DataSource dataSource;

    public JdbcServiceRequestRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String BASE = """
        SELECT sr.id, sr.requester_id, rq.display_name AS requester_name,
               sr.provider_id, pv.display_name AS provider_name, pv.phone AS provider_phone,
               sr.message, sr.status, sr.created_at
        FROM service_request sr
        JOIN resident rq ON rq.id = sr.requester_id
        JOIN resident pv ON pv.id = sr.provider_id
        """;

    @Override
    public boolean create(long requesterId, long providerId) {
        var sql = """
            INSERT INTO service_request (requester_id, provider_id)
            SELECT ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM service_request
                WHERE requester_id = ? AND provider_id = ? AND status = 'pending')
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, requesterId);
            stmt.setLong(2, providerId);
            stmt.setLong(3, requesterId);
            stmt.setLong(4, providerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao criar pedido de serviço", e);
        }
    }

    @Override
    public List<ServiceRequest> findByRequester(long requesterId) {
        var sql = BASE + "WHERE sr.requester_id = ? ORDER BY sr.created_at DESC";
        return listar(sql, requesterId);
    }

    @Override
    public List<ServiceRequest> findByProvider(long providerId) {
        var sql = BASE + "WHERE sr.provider_id = ? ORDER BY sr.created_at DESC";
        return listar(sql, providerId);
    }

    @Override
    public boolean responder(long requestId, long providerId, String novoStatus) {
        var sql = """
            UPDATE service_request SET status = ?
            WHERE id = ? AND provider_id = ? AND status = 'pending'
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoStatus);
            stmt.setLong(2, requestId);
            stmt.setLong(3, providerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao responder pedido de serviço", e);
        }
    }

    private List<ServiceRequest> listar(String sql, long filtro) {
        var pedidos = new ArrayList<ServiceRequest>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, filtro);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar pedidos de serviço", e);
        }
        return pedidos;
    }

    private static ServiceRequest mapear(ResultSet rs) throws SQLException {
        Timestamp criada = rs.getTimestamp("created_at");
        return new ServiceRequest(
            rs.getLong("id"),
            rs.getLong("requester_id"),
            rs.getString("requester_name"),
            rs.getLong("provider_id"),
            rs.getString("provider_name"),
            rs.getString("provider_phone"),
            rs.getString("message"),
            rs.getString("status"),
            criada != null ? criada.toInstant().atOffset(ZoneOffset.UTC) : null);
    }
}
