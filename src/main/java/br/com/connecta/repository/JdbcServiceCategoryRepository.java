package br.com.connecta.repository;

import br.com.connecta.domain.ServiceCategory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JDBC de ServiceCategoryRepository.
 * Regra do projeto: sempre PreparedStatement, nunca concatenar SQL.
 */
public class JdbcServiceCategoryRepository implements ServiceCategoryRepository {

    private final DataSource dataSource;

    public JdbcServiceCategoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<ServiceCategory> listAll() {
        var sql = "SELECT id, slug, name FROM service_category ORDER BY id";
        var allCategories = new ArrayList<ServiceCategory>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                allCategories.add(new ServiceCategory(
                    rs.getLong("id"),
                    rs.getString("slug"),
                    rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar categorias de serviço", e);
        }
        return allCategories;
    }
}
