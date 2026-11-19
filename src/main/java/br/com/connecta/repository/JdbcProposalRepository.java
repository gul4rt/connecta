package br.com.connecta.repository;

import br.com.connecta.domain.Demand;
import br.com.connecta.domain.Proposal;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementação JDBC de ProposalRepository. Sempre PreparedStatement. */
public class JdbcProposalRepository implements ProposalRepository {

    private final DataSource dataSource;

    public JdbcProposalRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(long demandId, long providerId, String message) {
        var sql = "INSERT INTO proposal (demand_id, provider_id, message) VALUES (?, ?, ?)";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, demandId);
            stmt.setLong(2, providerId);
            // mensagem é opcional: null quando vier vazia
            stmt.setString(3, message == null || message.isBlank() ? null : message.trim());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao enviar proposta", e);
        }
    }

    @Override
    public List<Proposal> findByDemand(long demandId) {
        var sql = BASE + "WHERE p.demand_id = ? ORDER BY p.created_at";
        return listar(sql, demandId);
    }

    @Override
    public List<Proposal> findByProvider(long providerId) {
        var sql = BASE + "WHERE p.provider_id = ? ORDER BY p.created_at DESC";
        return listar(sql, providerId);
    }

    @Override
    public Optional<Proposal> findOf(long demandId, long providerId) {
        var sql = BASE + "WHERE p.demand_id = ? AND p.provider_id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, demandId);
            stmt.setLong(2, providerId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar proposta", e);
        }
    }

    @Override
    public Optional<Proposal> findById(long id) {
        var sql = BASE + "WHERE p.id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar proposta", e);
        }
    }

    @Override
    public boolean existsFor(long demandId, long providerId) {
        return findOf(demandId, providerId).isPresent();
    }

    @Override
    public List<Demand> findAcceptedByProvider(long providerId) {
        var sql = """
            SELECT d.id, d.resident_id, r.username AS owner_username,
                   c.name AS category_name, n.name AS neighborhood_name,
                   d.description, d.offer_amount, d.contact_phone, d.status, d.created_at
            FROM demand d
            JOIN proposal p ON p.demand_id = d.id AND p.provider_id = ? AND p.status = 'accepted'
            JOIN service_category c ON c.id = d.category_id
            JOIN neighborhood n ON n.id = d.neighborhood_id
            JOIN resident r ON r.id = d.resident_id
            WHERE d.status = 'attended'
            ORDER BY d.created_at DESC
            """;
        var demandas = new ArrayList<Demand>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, providerId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    demandas.add(mapearDemanda(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar demandas aceitas", e);
        }
        return demandas;
    }

    @Override
    public void aceitar(long proposalId) {
        var aceitar = "UPDATE proposal SET status = 'accepted' WHERE id = ?";
        var rejeitarOutras = """
            UPDATE proposal SET status = 'rejected'
            WHERE demand_id = (SELECT demand_id FROM proposal WHERE id = ?)
              AND id <> ?
            """;
        var fecharDemanda = """
            UPDATE demand SET status = 'attended'
            WHERE id = (SELECT demand_id FROM proposal WHERE id = ?)
            """;
        var contarTrabalho = """
            UPDATE resident SET jobs_done = jobs_done + 1
            WHERE id = (SELECT provider_id FROM proposal WHERE id = ?)
            """;
        try (var conn = dataSource.getConnection()) {
            // Transação: as quatro atualizações acontecem juntas ou nenhuma acontece
            conn.setAutoCommit(false);
            try {
                executar(conn, aceitar, proposalId);
                executar(conn, rejeitarOutras, proposalId, proposalId);
                executar(conn, fecharDemanda, proposalId);
                executar(conn, contarTrabalho, proposalId);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao aceitar proposta", e);
        }
    }

    // ---------- Internos ----------

    private static final String BASE = """
        SELECT p.id, p.demand_id, p.provider_id, r.username AS provider_username,
               r.jobs_done AS provider_jobs_done,
               (r.verified_at IS NOT NULL) AS provider_verified,
               r.phone AS provider_phone,
               p.message, p.status, p.created_at
        FROM proposal p
        JOIN resident r ON r.id = p.provider_id
        """;

    private void executar(java.sql.Connection conn, String sql, Object... params) throws SQLException {
        try (var stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        }
    }

    private List<Proposal> listar(String sql, long filtro) {
        var propostas = new ArrayList<Proposal>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, filtro);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    propostas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar propostas", e);
        }
        return propostas;
    }

    private static Proposal mapear(ResultSet rs) throws SQLException {
        Timestamp criada = rs.getTimestamp("created_at");
        return new Proposal(
            rs.getLong("id"),
            rs.getLong("demand_id"),
            rs.getLong("provider_id"),
            rs.getString("provider_username"),
            rs.getInt("provider_jobs_done"),
            rs.getBoolean("provider_verified"),
            rs.getString("provider_phone"),
            rs.getString("message"),
            rs.getString("status"),
            criada != null ? criada.toInstant().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    private static Demand mapearDemanda(ResultSet rs) throws SQLException {
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
