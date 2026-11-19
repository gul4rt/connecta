package br.com.connecta.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Cria o pool de conexões (HikariCP) e aplica as migrações do banco (Flyway).
 * As migrações ficam em src/main/resources/db/migration, em SQL puro versionado.
 */
public final class Database {

    private Database() {
    }

    /** Sobe o pool, aplica as migrações e devolve o DataSource para os repositórios. */
    public static DataSource init() {
        var config = new HikariConfig();
        config.setJdbcUrl(AppConfig.dbUrl());
        config.setUsername(AppConfig.dbUser());
        config.setPassword(AppConfig.dbPassword());
        // Pool pequeno: aplicação simples, um usuário por vez nos testes
        config.setMaximumPoolSize(5);
        config.setPoolName("connecta-pool");

        HikariDataSource dataSource;
        try {
            dataSource = new HikariDataSource(config);
            // Abre uma conexão para falhar rápido com mensagem clara
            try (var ignored = dataSource.getConnection()) {
                // conexão OK
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                "Não foi possível conectar ao banco de dados (" + AppConfig.dbUrl() + "). "
                    + "Verifique se o Postgres está no ar: docker compose up -d db. Causa: "
                    + rootMessage(e), e);
        }

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate();
        return dataSource;
    }

    private static String rootMessage(Throwable e) {
        if (e instanceof SQLException && e.getCause() != null) {
            return rootMessage(e.getCause());
        }
        return e.getMessage();
    }
}
