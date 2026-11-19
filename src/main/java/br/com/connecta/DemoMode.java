package br.com.connecta;

import br.com.connecta.config.AppConfig;
import br.com.connecta.config.Database;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Modo demonstração (Fase 3): APAGA e recria o banco com dados de exemplo,
 * claramente fictícios, para gravar o vídeo técnico sem sujar a base real.
 *
 * Rodar com um comando:
 *   ./mvnw compile exec:java -Dexec.args=demo
 *   (ou java -jar target/connecta.jar demo)
 *
 * ATENÇÃO: o modo demo LIMPA o banco (Flyway clean + migrate + seed).
 * Nada de dados reais sobrevive. Os dados de exemplo são todos marcados
 * com "demo." / "Exemplo" e fáceis de remover (basta rodar de novo sem "demo").
 */
public final class DemoMode {

    private static final Logger log = LoggerFactory.getLogger(DemoMode.class);

    private DemoMode() {
    }

    /** Clean + migrate + dados de exemplo. Devolve o DataSource pronto. */
    public static DataSource recriarBanco() {
        var url = AppConfig.dbUrl();
        log.info("DEMO: limpando o banco ({}) ...", url);
        // cleanDisabled(false): a partir do Flyway 9 o clean vem desligado
        // por segurança; aqui o clean é o propósito do modo demo
        Flyway.configure()
            .dataSource(url, AppConfig.dbUser(), AppConfig.dbPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load()
            .clean();
        DataSource dataSource = Database.init();
        DemoSeed.inserir(dataSource);
        return dataSource;
    }
}
