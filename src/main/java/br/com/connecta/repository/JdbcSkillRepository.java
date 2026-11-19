package br.com.connecta.repository;

import br.com.connecta.domain.Skill;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Implementação JDBC de SkillRepository. Sempre PreparedStatement. */
public class JdbcSkillRepository implements SkillRepository {

    private final DataSource dataSource;

    public JdbcSkillRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Skill> findByResident(long residentId) {
        var sql = """
            SELECT s.id, s.resident_id, c.name AS category_name, s.kind, s.name
            FROM skill s
            JOIN service_category c ON c.id = s.category_id
            WHERE s.resident_id = ?
            ORDER BY s.kind, s.created_at
            """;
        var habilidades = new ArrayList<Skill>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    habilidades.add(new Skill(
                        rs.getLong("id"),
                        rs.getLong("resident_id"),
                        rs.getString("category_name"),
                        rs.getString("kind"),
                        rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar habilidades", e);
        }
        return habilidades;
    }

    @Override
    public void add(long residentId, long categoryId, String kind, String name) {
        var sql = "INSERT INTO skill (resident_id, category_id, kind, name) VALUES (?, ?, ?, ?)";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, residentId);
            stmt.setLong(2, categoryId);
            stmt.setString(3, kind);
            stmt.setString(4, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao adicionar habilidade", e);
        }
    }

    @Override
    public boolean delete(long id, long residentId) {
        // O WHERE com resident_id garante que alguém só exclui a própria habilidade
        var sql = "DELETE FROM skill WHERE id = ? AND resident_id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, residentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao excluir habilidade", e);
        }
    }
}
