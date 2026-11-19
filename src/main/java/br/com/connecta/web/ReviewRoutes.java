package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import br.com.connecta.domain.ReviewTarget;
import br.com.connecta.repository.ReviewRepository;
import br.com.connecta.service.ReviewService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;

/**
 * Avaliação de serviço (Fase 5): nota 1–5 + comentário, APENAS de quem
 * contratou (proposta aceita ou pedido direto aceito) e uma vez por
 * trabalho. O banco reconfirma a regra no INSERT.
 */
public class ReviewRoutes {

    private final ReviewRepository avaliacoes;

    public ReviewRoutes(ReviewRepository avaliacoes) {
        this.avaliacoes = avaliacoes;
    }

    /** GET /avaliar/demanda/{id} — avaliar o prestador de uma demanda atendida. */
    public void paginaDemanda(Context ctx) {
        renderFormulario(ctx, "demanda", ctx.pathParamAsClass("id", Long.class)
            .getOrDefault(-1L));
    }

    /** GET /avaliar/pedido/{id} — avaliar o prestador de um pedido aceito. */
    public void paginaPedido(Context ctx) {
        renderFormulario(ctx, "pedido", ctx.pathParamAsClass("id", Long.class)
            .getOrDefault(-1L));
    }

    /** POST /avaliar — grava a avaliação e concede o selo se for o caso. */
    public void avaliar(Context ctx) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String tipo = "pedido".equals(ctx.formParam("tipo")) ? "pedido" : "demanda";
        long id;
        try {
            id = Long.parseLong(ctx.formParam("id"));
        } catch (NumberFormatException ignorada) {
            id = -1;
        }
        String nota = ctx.formParam("nota");
        String comentario = ctx.formParam("comentario");

        List<String> erros = ReviewService.validar(nota, comentario);
        if (!erros.isEmpty()) {
            renderFormularioComErros(ctx, tipo, id, erros);
            return;
        }

        boolean gravou;
        if ("pedido".equals(tipo)) {
            gravou = avaliacoes.avaliarPedido(id, conta.id(),
                Integer.parseInt(nota.trim()), comentario == null ? "" : comentario.trim());
        } else {
            gravou = avaliacoes.avaliarDemanda(id, conta.id(),
                Integer.parseInt(nota.trim()), comentario == null ? "" : comentario.trim());
        }

        if (!gravou) {
            // não contratou / já avaliou / trabalho não aceito: o banco recusou
            renderFormularioComErros(ctx, tipo, id, List.of(
                "Não foi possível avaliar. Só avalia quem contratou o serviço, uma vez por trabalho."));
            return;
        }

        ctx.redirect("pedido".equals(tipo) ? "/perfil/pedidos?avaliado=1" : "/perfil/demandas?avaliado=1");
    }

    // ---------- Internos ----------

    private void renderFormulario(Context ctx, String tipo, long id) {
        renderFormularioComErros(ctx, tipo, id, List.of());
    }

    private void renderFormularioComErros(Context ctx, String tipo, long id, List<String> erros) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        var alvo = "pedido".equals(tipo)
            ? avaliacoes.alvoPedido(id, conta.id())
            : avaliacoes.alvoDemanda(id, conta.id());

        var dados = new HashMap<String, Object>();
        if (alvo.isEmpty()) {
            // não contratou ou já avaliou: página só explica a regra
            dados.put("alvo", null);
        } else {
            ReviewTarget trabalho = alvo.get();
            dados.put("alvo", new ReviewTarget(tipo, trabalho.alvoId(), trabalho.providerName()));
        }
        dados.put("tipo", tipo);
        dados.put("id", id);
        dados.put("erros", erros);
        ctx.render("avaliar", View.modelo(ctx, dados));
    }

    /** Ações exigem sessão e perfil completo (mesma regra do resto). */
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
}
