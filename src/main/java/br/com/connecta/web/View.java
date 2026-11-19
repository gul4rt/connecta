package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Monta o "modelo" comum de todas as páginas:
 * - tema (claro/escuro) via cookie, aplicado no <html> pelo template base;
 * - estado de login, lido do atributo "residente" que o App preenche;
 * - token CSRF para os formulários.
 * Tudo funciona sem JavaScript: o tema é um link com ícone e o popup de
 * entrada é uma melhoria sobre a página /entrar.
 */
public final class View {

    public static final String COOKIE_TEMA = "connecta_tema";

    private View() {
    }

    /** Junta os dados da página com o estado comum. */
    public static Map<String, Object> modelo(Context ctx, Map<String, Object> dados) {
        var modelo = new HashMap<String, Object>(dados);
        modelo.put("temaEscuro", "escuro".equals(ctx.cookie(COOKIE_TEMA)));
        modelo.put("caminho", ctx.path());
        modelo.put("logado", logado(ctx));
        Resident conta = ctx.attribute("residente");
        modelo.put("usuarioNome", conta != null && conta.displayName() != null
            ? primeiroNome(conta.displayName()) : "");
        modelo.put("usuarioUsername", conta != null ? conta.username() : "");
        modelo.put("usuarioBairro", conta != null && conta.neighborhoodName() != null
            ? conta.neighborhoodName() : "");
        modelo.put("perfilPendente", conta != null && !conta.profileCompleted());
        modelo.put("usuarioRole", conta != null ? conta.role() : "");
        modelo.put("usuarioRoleLabel", conta != null ? conta.roleLabel() : "");
        modelo.put("usuarioInicial", inicial(conta != null && conta.displayName() != null
            ? conta.displayName() : (conta != null ? conta.username() : "")));
        modelo.put("csrfToken", Csrf.tokenAtual(ctx));
        return modelo;
    }

    /** O visitante tem sessão válida? (atributo preenchido pelo App) */
    public static boolean logado(Context ctx) {
        return ctx.attribute("residente") != null;
    }

    /**
     * Rota /preferencias: grava o tema em cookie e volta para a página de
     * origem. O botão lua/sol do cabeçalho é um link para cá.
     */
    public static void salvarPreferencias(Context ctx) {
        String tema = ctx.queryParam("tema");

        if ("escuro".equals(tema) || "claro".equals(tema)) {
            gravarCookie(ctx, COOKIE_TEMA, tema);
        }

        // Volta para onde a pessoa estava (com proteção contra redirecionamento externo)
        String para = ctx.queryParam("para");
        if (para == null || !para.startsWith("/") || para.startsWith("//")) {
            para = "/";
        }
        ctx.redirect(para);
    }

    /** Grava um cookie via API de Servlet, visível e sem "mágica". */
    private static void gravarCookie(Context ctx, String nome, String valor) {
        var cookie = new jakarta.servlet.http.Cookie(nome, valor);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // O projeto roda localmente (http); a flag Secure ficaria para um futuro deploy
        cookie.setSecure(false);
        ctx.res().addCookie(cookie);
    }

    private static String primeiroNome(String nomeCompleto) {
        int espaco = nomeCompleto.indexOf(' ');
        return espaco > 0 ? nomeCompleto.substring(0, espaco) : nomeCompleto;
    }

    /** Primeira letra do nome, para o avatar (maiúscula). */
    private static String inicial(String nome) {
        return (nome == null || nome.isBlank()) ? "?" : nome.trim().substring(0, 1).toUpperCase();
    }
}
