package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import br.com.connecta.repository.ResidentRepository;
import br.com.connecta.service.AuthService;

import java.util.HashMap;
import java.util.Map;

/**
 * Rotas de autenticação: /entrar, /cadastro, /login, /logout.
 * Os formulários funcionam sem JavaScript: o popup na home é uma
 * melhoria (JS), e /entrar é a página completa de fallback.
 */
public class AuthRoutes {

    private final AuthService auth;
    private final ResidentRepository residentRepo;

    public AuthRoutes(AuthService auth, ResidentRepository residentRepo) {
        this.auth = auth;
        this.residentRepo = residentRepo;
    }

    // ---------- GET /entrar (página completa; sem JS é o caminho do popup) ----------
    public void paginaEntrar(io.javalin.http.Context ctx) {
        if (View.logado(ctx)) {
            ctx.redirect("/");
            return;
        }
        ctx.render("entrar", View.modelo(ctx, Map.of("username", "", "erros", java.util.List.of())));
    }

    // ---------- POST /login ----------
    public void login(io.javalin.http.Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String username = ctx.formParam("username");
        String senha = ctx.formParam("senha");
        try {
            var sessao = auth.entrar(username, senha);
            definirCookieDeSessao(ctx, sessao.token());
            // Depois do login, perfil incompleto vai para "preencher perfil"
            Resident conta = residentRepo.findById(sessao.residentId()).orElseThrow();
            ctx.redirect(conta.profileCompleted() ? "/" : "/perfil/completar");
        } catch (AuthService.ExcecaoValidacao e) {
            var dados = new HashMap<String, Object>();
            dados.put("username", username == null ? "" : username);
            dados.put("erros", e.erros);
            ctx.status(422).render("entrar", View.modelo(ctx, dados));
        }
    }

    // ---------- GET /cadastro ----------
    public void paginaCadastro(io.javalin.http.Context ctx) {
        if (View.logado(ctx)) {
            ctx.redirect("/perfil");
            return;
        }
        ctx.render("cadastro", View.modelo(ctx, Map.of(
            "username", "", "role", "", "erros", java.util.List.of())));
    }

    // ---------- POST /cadastro ----------
    public void registrar(io.javalin.http.Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String username = ctx.formParam("username");
        String senha = ctx.formParam("senha");
        String confirmacao = ctx.formParam("confirmacao");
        String role = ctx.formParam("role");
        boolean consentiu = "on".equals(ctx.formParam("consentimento"));
        try {
            var criada = auth.registrar(username, senha, confirmacao, role, consentiu);
            definirCookieDeSessao(ctx, criada.sessao().token());
            // Mostra o código de recuperação UMA vez, logo após criar a conta
            ctx.render("codigo-recuperacao", View.modelo(ctx, Map.of(
                "pagina", "codigo",
                "codigo", criada.codigoRecuperacao(),
                "modo", "cadastro")));
        } catch (AuthService.ExcecaoValidacao e) {
            var dados = new HashMap<String, Object>();
            dados.put("username", username == null ? "" : username);
            dados.put("role", role == null ? "" : role);
            dados.put("erros", e.erros);
            ctx.status(422).render("cadastro", View.modelo(ctx, dados));
        }
    }

    // ---------- GET /recuperar: esqueci minha senha ----------
    public void paginaRecuperar(io.javalin.http.Context ctx) {
        ctx.render("recuperar", View.modelo(ctx, Map.of("erros", java.util.List.of())));
    }

    // ---------- POST /recuperar ----------
    public void redefinirSenha(io.javalin.http.Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        try {
            String codigoNovo = auth.redefinirSenha(
                ctx.formParam("username"),
                ctx.formParam("codigo"),
                ctx.formParam("senha"),
                ctx.formParam("confirmacao"));
            ctx.render("codigo-recuperacao", View.modelo(ctx, Map.of(
                "pagina", "codigo",
                "codigo", codigoNovo,
                "modo", "recuperacao")));
        } catch (AuthService.ExcecaoValidacao e) {
            var dados = new HashMap<String, Object>();
            dados.put("erros", e.erros);
            ctx.status(422).render("recuperar", View.modelo(ctx, dados));
        }
    }

    // ---------- POST /logout ----------
    public void logout(io.javalin.http.Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String token = ctx.cookie(AuthService.COOKIE_SESSAO);
        if (token != null) {
            auth.sair(token);
        }
        limparCookieDeSessao(ctx);
        ctx.redirect("/");
    }

    // ---------- Helpers ----------

    /** Cookie de sessão: HttpOnly e SameSite=Lax; projeto local roda em http. */
    public static void definirCookieDeSessao(io.javalin.http.Context ctx, String token) {
        ctx.header("Set-Cookie", AuthService.COOKIE_SESSAO + "=" + token
            + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + (AuthService.DIAS_SESSAO * 24 * 3600));
    }

    public static void limparCookieDeSessao(io.javalin.http.Context ctx) {
        ctx.header("Set-Cookie", AuthService.COOKIE_SESSAO + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }
}
