package br.com.connecta.web;

import io.javalin.http.Context;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Proteção contra CSRF com o padrão "double-submit cookie":
 * 1. Ao renderizar um formulário, garantimos um cookie connecta_csrf
 *    (aleatório) e colocamos o mesmo valor num campo oculto _csrf.
 * 2. No POST, o campo precisa ser igual ao cookie — um site externo
 *    consegue enviar um POST, mas não consegue LER nosso cookie para
 *    preencher o campo, então o ataque falha.
 *
 * Funciona também para quem ainda não está logado (login e cadastro).
 */
public final class Csrf {

    public static final String COOKIE = "connecta_csrf";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Csrf() {
    }

    /** Devolve o token atual (criando o cookie se ainda não existe). */
    public static String tokenAtual(Context ctx) {
        String token = ctx.cookie(COOKIE);
        if (token == null || token.isBlank()) {
            byte[] bytes = new byte[16];
            ALEATORIO.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            var cookie = new jakarta.servlet.http.Cookie(COOKIE, token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            ctx.res().addCookie(cookie);
        }
        return token;
    }

    /** Verifica o campo _csrf do formulário contra o cookie. Se não bate, bloqueia. */
    public static boolean validar(Context ctx) {
        String cookie = ctx.cookie(COOKIE);
        String campo = ctx.formParam("_csrf");
        return cookie != null && !cookie.isBlank() && cookie.equals(campo);
    }

    /** Mensagem padrão quando o CSRF falha (token velho, recarga da página etc.). */
    public static void recusar(Context ctx) {
        ctx.status(403).result(
            "Esta página ficou aberta por muito tempo. Volte, recarregue e tente de novo.");
    }
}
