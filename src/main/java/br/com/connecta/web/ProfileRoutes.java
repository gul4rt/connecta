package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import br.com.connecta.repository.ResidentRepository;
import br.com.connecta.repository.TrackRepository;
import br.com.connecta.service.AuthService;
import br.com.connecta.service.NeighborhoodService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Perfil do morador: preencher pela primeira vez (/perfil/completar) e
 * editar a qualquer momento (/perfil). Protegidas: só com sessão ativa
 * (a guarda fica no before-handler do App).
 */
public class ProfileRoutes {

    private final ResidentRepository residents;
    private final NeighborhoodService bairros;
    private final AuthService auth;
    // Selo verificado no cartão do perfil (trilhas concluídas — Fase 3)
    private final TrackRepository trilhas;

    public ProfileRoutes(ResidentRepository residents, NeighborhoodService bairros,
                         AuthService auth, TrackRepository trilhas) {
        this.residents = residents;
        this.bairros = bairros;
        this.auth = auth;
        this.trilhas = trilhas;
    }

    public void paginaCompletar(Context ctx) {
        renderFormulario(ctx, "completar", List.of());
    }

    public void paginaPerfil(Context ctx) {
        renderFormulario(ctx, "editar", List.of());
    }

    // ---------- Segurança: trocar senha e ver o código de recuperação ----------
    public void paginaTrocarSenha(Context ctx) {
        var dados = new HashMap<String, Object>();
        dados.put("erros", List.of());
        ctx.render("trocar-senha", View.modelo(ctx, dados));
    }

    /** POST /perfil/senha: troca a senha e gera um código de recuperação NOVO. */
    public void trocarSenha(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        try {
            String codigoNovo = auth.trocarSenha(conta.id(),
                ctx.formParam("senhaAtual"),
                ctx.formParam("senha"),
                ctx.formParam("confirmacao"));
            ctx.render("codigo-recuperacao", View.modelo(ctx, Map.of(
                "pagina", "codigo",
                "codigo", codigoNovo,
                "modo", "troca")));
        } catch (AuthService.ExcecaoValidacao e) {
            var dados = new HashMap<String, Object>();
            dados.put("erros", e.erros);
            ctx.status(422).render("trocar-senha", View.modelo(ctx, dados));
        }
    }

    // ---------- POST /perfil: salva os dados ----------
    public void salvar(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        String nome = limpar(ctx.formParam("nome"));
        String bairro = limpar(ctx.formParam("bairro"));
        String telefone = limpar(ctx.formParam("telefone"));

        var erros = validarPerfil(conta, nome, bairro, telefone);
        if (!erros.isEmpty()) {
            ctx.status(422);
            renderFormularioComValores(ctx, modo(ctx), nome, bairro, telefone, erros);
            return;
        }

        long neighborhoodId = bairros.resolverBairro(bairro);
        residents.updateProfile(conta.id(), nome,
            "provider".equals(conta.role()) ? telefone : null,
            neighborhoodId, true);
        ctx.redirect("/");
    }

    // ---------- POST /perfil/excluir: LGPD, a conta sai de vez ----------
    public void excluir(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        auth.encerrarSessoesDe(conta.id());
        residents.delete(conta.id());
        AuthRoutes.limparCookieDeSessao(ctx);
        ctx.redirect("/");
    }

    // ---------- Validação (sem banco: testável) ----------

    /** Campos obrigatórios por perfil; telefone só exigido de quem presta serviço. */
    public static List<String> validarPerfil(Resident conta, String nome, String bairro, String telefone) {
        var erros = new java.util.ArrayList<String>();
        if (nome.length() < 2 || nome.length() > 80) {
            erros.add("Informe seu nome (2 a 80 caracteres).");
        }
        if (bairro.length() < 3 || bairro.length() > 80) {
            erros.add("Informe o seu bairro (busque e selecione na lista).");
        }
        if ("provider".equals(conta.role()) && !telefone.matches("\\d{10,11}")) {
            erros.add("Informe um telefone com DDD (10 ou 11 números, só dígitos) para você receber pedidos.");
        }
        return erros;
    }

    // ---------- Internos ----------

    private void renderFormulario(Context ctx, String modo, List<String> erros) {
        Resident conta = ctx.attribute("residente");
        renderFormularioComValores(ctx, modo,
            conta.displayName() == null ? "" : conta.displayName(),
            conta.neighborhoodName() == null ? "" : conta.neighborhoodName(),
            conta.phone() == null ? "" : conta.phone(),
            erros);
    }

    private void renderFormularioComValores(Context ctx, String modo, String nome,
                                            String bairro, String telefone, List<String> erros) {
        Resident conta = ctx.attribute("residente");
        var dados = new HashMap<String, Object>();
        dados.put("modo", modo);
        dados.put("nome", nome);
        dados.put("bairro", bairro);
        dados.put("telefone", telefone);
        dados.put("erros", erros);
        dados.put("temSelo", conta != null && trilhas.temSelo(conta.id()));
        ctx.render("perfil", View.modelo(ctx, dados));
    }

    private static String modo(Context ctx) {
        return "/perfil/completar".equals(ctx.path()) ? "completar" : "editar";
    }

    /** Tira espaços das pontas; null vira string vazia para os templates. */
    private static String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
