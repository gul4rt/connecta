package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import br.com.connecta.domain.TrackDetail;
import br.com.connecta.repository.TrackRepository;
import br.com.connecta.service.TrackService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trilhas como timeline de cursos (Fase 3):
 * - GET  /trilhas            — lista de trilhas com o progresso da pessoa;
 * - GET  /trilhas/{slug}     — timeline em etapas (stepper) com as playlists;
 * - POST /trilhas/{slug}/inscrever  — "Quero fazer esta trilha";
 * - POST /trilhas/{slug}/etapas     — marca uma etapa como concluída;
 * - POST /trilhas/{slug}/concluir   — autodeclaração (termo) + selo verificado.
 * Navegação fica livre (sem login dá para ver as trilhas); as AÇÕES exigem
 * sessão e perfil completo, como nas demais telas.
 */
public class TrackRoutes {

    private final TrackRepository trilhas;

    public TrackRoutes(TrackRepository trilhas) {
        this.trilhas = trilhas;
    }

    /** GET /trilhas */
    public void listar(Context ctx) {
        Resident conta = ctx.attribute("residente");
        var dados = new HashMap<String, Object>();
        dados.put("trilhas", trilhas.listAll(conta == null ? null : conta.id()));
        dados.put("temSelo", conta != null && trilhas.temSelo(conta.id()));
        ctx.render("trilhas", View.modelo(ctx, dados));
    }

    /** GET /trilhas/{slug} */
    public void detalhe(Context ctx) {
        Resident conta = ctx.attribute("residente");
        Optional<TrackDetail> detail = trilhas.findDetail(ctx.pathParam("slug"),
            conta == null ? null : conta.id());
        if (detail.isEmpty()) {
            ctx.status(404).result("Trilha não encontrada.");
            return;
        }
        var dados = new HashMap<String, Object>();
        dados.put("trilha", detail.get());
        // estados done/current/pending de cada etapa para o stepper
        dados.put("etapas", TrackService.estadosDasEtapas(detail.get().etapas(),
            detail.get().concluida()));
        dados.put("erros", ctx.queryParam("erros"));
        ctx.render("trilha-detalhe", View.modelo(ctx, dados));
    }

    /** POST /trilhas/{slug}/inscrever — "Quero fazer esta trilha". */
    public void inscrever(Context ctx) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String slug = ctx.pathParam("slug");
        if (trilhas.findBySlug(slug).isEmpty()) {
            ctx.status(404).result("Trilha não encontrada.");
            return;
        }
        trilhas.inscrever(conta.id(), slug);
        ctx.redirect("/trilhas/" + slug);
    }

    /** POST /trilhas/{slug}/etapas — marca uma etapa como concluída. */
    public void marcarEtapa(Context ctx) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String slug = ctx.pathParam("slug");
        long stepId;
        try {
            stepId = Long.parseLong(ctx.formParam("etapa"));
        } catch (NumberFormatException ignorada) {
            stepId = -1; // id inválido: a consulta não grava nada
        }
        trilhas.marcarEtapa(conta.id(), slug, stepId);
        ctx.redirect("/trilhas/" + slug);
    }

    /** POST /trilhas/{slug}/concluir — autodeclaração (termo) e selo. */
    public void concluir(Context ctx) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String slug = ctx.pathParam("slug");
        Optional<TrackDetail> detail = trilhas.findDetail(slug, conta.id());
        if (detail.isEmpty()) {
            ctx.status(404).result("Trilha não encontrada.");
            return;
        }
        TrackDetail trilha = detail.get();
        boolean confirmouTermo = ctx.formParam("termo") != null;

        // Regras de negócio (puras, testadas em TrackServiceTest)
        List<String> erros = TrackService.validarConclusao(
            trilha.totalEtapas(), trilha.etapasConcluidas(), confirmouTermo);
        if (!erros.isEmpty()) {
            // volta para a timeline mostrando os erros (funciona sem JS)
            ctx.redirect("/trilhas/" + slug + "?erros=" + urlCodificada(erros));
            return;
        }
        trilhas.concluir(conta.id(), slug);
        ctx.redirect("/trilhas/" + slug);
    }

    // ---------- Internos ----------

    /**
     * Ações exigem sessão e perfil completo. null = já redirecionei.
     * (Decisão da Fase 1: navegação livre, ações bloqueadas sem conta.)
     */
    private Resident exigeConta(Context ctx) {
        if (!View.logado(ctx)) {
            ctx.redirect("/entrar");
            return null;
        }
        Resident conta = ctx.attribute("residente");
        if (conta != null && !conta.profileCompleted()) {
            ctx.redirect("/perfil/completar");
            return null;
        }
        return conta;
    }

    /** Junta os erros numa querystring simples (frases sem espaços problemáticos). */
    private String urlCodificada(List<String> erros) {
        var texto = String.join(" ", erros);
        return java.net.URLEncoder.encode(texto, java.nio.charset.StandardCharsets.UTF_8);
    }
}
