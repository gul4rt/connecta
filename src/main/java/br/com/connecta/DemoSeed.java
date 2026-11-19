package br.com.connecta;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * Dados de exemplo do modo demonstração — TODOS claramente fictícios
 * (usuários "demo.*", nomes "Exemplo", bairros inventados), para o
 * vídeo técnico mostrar inserção, consulta e atualização no banco.
 * Senha de todos os usuários demo: demo123 (guardada com hash BCrypt,
 * igual às contas reais).
 */
public final class DemoSeed {

    private static final Logger log = LoggerFactory.getLogger(DemoSeed.class);
    public static final String SENHA_DEMO = "demo123";

    private DemoSeed() {
    }

    public static void inserir(DataSource dataSource) {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                inserirTudo(conn);
                conn.commit();
                log.info("DEMO: dados de exemplo gravados (senha de todos: {})", SENHA_DEMO);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao inserir dados de demonstração", e);
        }
    }

    private static void inserirTudo(Connection conn) throws Exception {
        log.info("DEMO [inserção]: bairros de exemplo");
        long jardim = inserir(conn, "INSERT INTO neighborhood (name) VALUES (?)", "Jardim Exemplo");
        long vila = inserir(conn, "INSERT INTO neighborhood (name) VALUES (?)", "Vila Demonstração");

        // ---------- Moradores demo (inserção) ----------
        log.info("DEMO [inserção]: moradores demo (demo.maria, demo.joao, demo.ana)");
        String hash = BCrypt.hashpw(SENHA_DEMO, BCrypt.gensalt());
        long maria = inserir(conn, """
            INSERT INTO resident (username, password_hash, role, display_name, phone,
                                  neighborhood_id, consent_at, profile_completed)
            VALUES (?, ?, 'provider', ?, ?, ?, now(), TRUE)
            """, "demo.maria", hash, "Maria Exemplo", "11999990001", jardim);
        long joao = inserir(conn, """
            INSERT INTO resident (username, password_hash, role, display_name, phone,
                                  neighborhood_id, consent_at, profile_completed)
            VALUES (?, ?, 'provider', ?, ?, ?, now(), TRUE)
            """, "demo.joao", hash, "João Exemplo", "11999990002", jardim);
        long ana = inserir(conn, """
            INSERT INTO resident (username, password_hash, role, display_name,
                                  neighborhood_id, consent_at, profile_completed)
            VALUES (?, ?, 'seeker', ?, ?, now(), TRUE)
            """, "demo.ana", hash, "Ana Exemplo", vila);

        // ---------- Habilidades ----------
        log.info("DEMO [inserção]: habilidades");
        long catManutencao = idDe(conn, "SELECT id FROM service_category WHERE slug = 'manutencao'");
        long catTecnologia = idDe(conn, "SELECT id FROM service_category WHERE slug = 'tecnologia'");
        long catCuidados = idDe(conn, "SELECT id FROM service_category WHERE slug = 'cuidados'");
        long catGastronomia = idDe(conn, "SELECT id FROM service_category WHERE slug = 'gastronomia'");
        inserir(conn, "INSERT INTO skill (resident_id, category_id, kind, name) VALUES (?, ?, 'have', ?)",
            maria, catManutencao, "Instalação elétrica básica");
        inserir(conn, "INSERT INTO skill (resident_id, category_id, kind, name) VALUES (?, ?, 'want', ?)",
            maria, catTecnologia, "Programação de sites");
        inserir(conn, "INSERT INTO skill (resident_id, category_id, kind, name) VALUES (?, ?, 'have', ?)",
            joao, catTecnologia, "Manutenção de computadores");
        inserir(conn, "INSERT INTO skill (resident_id, category_id, kind, name) VALUES (?, ?, 'want', ?)",
            ana, catGastronomia, "Doces para festa");

        // ---------- Demandas (5 de manutenção = "Muita procura" na home) ----------
        log.info("DEMO [inserção]: demandas de exemplo");
        demanda(conn, ana, jardim, catManutencao, "Trocar disjuntor que desarma sozinho", 120);
        demanda(conn, ana, jardim, catManutencao, "Instalar chuveiro elétrico novo", 100);
        demanda(conn, ana, jardim, catManutencao, "Consertar fio exposto na varanda", 80);
        demanda(conn, joao, jardim, catManutencao, "Instalar tomadas na cozinha", 150);
        demanda(conn, joao, jardim, catManutencao, "Revisar quadro de energia da casa", 200);
        long d6 = demanda(conn, ana, jardim, catTecnologia, "Formatar notebook que está lento", 120);
        long d7 = demanda(conn, joao, jardim, catTecnologia, "Configurar Wi-Fi em toda a casa", 100);
        demanda(conn, ana, jardim, catGastronomia, "Salgados para festa da rua (100 pessoas)", 300);
        long d9 = demanda(conn, ana, vila, catCuidados, "Cuidadora para manhãs de segunda a sexta", 400);

        // ---------- Propostas (atualização: aceite libera contato e sobe o portfólio) ----------
        log.info("DEMO [inserção]: propostas");
        long pAceita = inserir(conn, """
            INSERT INTO proposal (demand_id, provider_id, message, status)
            VALUES (?, ?, ?, 'accepted')
            """, d9, maria, "Tenho experiência com cuidado de idosos, posso começar segunda.");
        inserir(conn, "UPDATE demand SET status = 'attended' WHERE id = ?", d9);
        inserir(conn, "UPDATE resident SET jobs_done = 1 WHERE id = ?", maria);
        inserir(conn, """
            INSERT INTO proposal (demand_id, provider_id, message, status)
            VALUES (?, ?, ?, 'pending')
            """, d6, joao, "Formatei vários notebooks do bairro, orçamento é esse mesmo.");

        // ---------- Trilhas (timeline de cursos, Fase 3) ----------
        log.info("DEMO [inserção]: progresso de trilhas");
        // Maria: trilha de elétrica CONCLUÍDA com termo (ganhou o selo verificado)
        long etEletrica = idDe(conn, "SELECT id FROM track WHERE slug = 'instalacao-eletrica'");
        inserir(conn, "INSERT INTO track_step_progress (resident_id, step_id) "
            + "SELECT ?, id FROM track_step WHERE track_id = ?", maria, etEletrica);
        inserir(conn, """
            INSERT INTO track_enrollment (resident_id, track_id, status, completed_at, term_accepted_at)
            VALUES (?, ?, 'completed', now(), now())
            """, maria, etEletrica);
        inserir(conn, "UPDATE resident SET verified_at = now() WHERE id = ?", maria);
        log.info("DEMO [atualização]: demo.maria concluiu a trilha de elétrica e ganhou o selo");

        // Maria: trilha de programação EM ANDAMENTO (2 de 9 etapas)
        long etWeb = idDe(conn, "SELECT id FROM track WHERE slug = 'programacao-web'");
        inserir(conn, "INSERT INTO track_enrollment (resident_id, track_id) VALUES (?, ?)", maria, etWeb);
        inserir(conn, """
            INSERT INTO track_step_progress (resident_id, step_id)
            SELECT ?, id FROM track_step WHERE track_id = ? AND position <= 2
            """, maria, etWeb);

        // ---------- Resumo do que o vídeo pode mostrar ----------
        log.info("DEMO: resumo — moradores: {}, demandas: {}, proposta aceita: {}, selo: demo.maria",
            3, 9, pAceita);
    }

    // ---------- Auxiliares de inserção (sempre PreparedStatement) ----------

    /** Roda um INSERT/UPDATE com os parâmetros e devolve a chave gerada (ou -1). */
    private static long inserir(Connection conn, String sql, Object... params) throws Exception {
        try (var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    /** Consulta de um único número por slug/descrição. */
    private static long idDe(Connection conn, String sql) throws Exception {
        try (var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    /** Atalho para demandas de exemplo (oferta e telefone preenchidos). */
    private static long demanda(Connection conn, long residentId, long bairroId,
                                long categoriaId, String descricao, double oferta) throws Exception {
        return inserir(conn, """
            INSERT INTO demand (resident_id, neighborhood_id, category_id, description,
                                offer_amount, contact_phone)
            VALUES (?, ?, ?, ?, ?, '11999990000')
            """, residentId, bairroId, categoriaId, descricao, oferta);
    }
}
