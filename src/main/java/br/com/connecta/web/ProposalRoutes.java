package br.com.connecta.web;

import br.com.connecta.domain.Demand;
import br.com.connecta.domain.Resident;
import br.com.connecta.repository.DemandRepository;
import br.com.connecta.repository.ProposalRepository;
import br.com.connecta.service.ProposalService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;

/**
 * Propostas: prestador envia (/demandas/{id}/proposta) e o dono da demanda
 * aceita (/perfil/demandas/proposta/aceitar). Ao aceitar: demanda vira
 * 'attended', outras propostas são rejeitadas, contador do prestador sobe
 * e o contato dos dois é liberado.
 */
public class ProposalRoutes {

    private final ProposalRepository propostas;
    private final DemandRepository demands;

    public ProposalRoutes(ProposalRepository propostas, DemandRepository demands) {
        this.propostas = propostas;
        this.demands = demands;
    }

    /** GET /demandas/{id}/proposta: formulário com resumo da demanda. */
    public void formulario(Context ctx) {
        Resident conta = ctx.attribute("residente");
        long demandaId = idDe(ctx.pathParam("id"));
        var demandaOpt = demands.findById(demandaId);

        if (demandaOpt.isEmpty() || !podePropor(conta, demandaOpt.get())) {
            ctx.redirect("/vitrine");
            return;
        }
        var dados = new HashMap<String, Object>();
        dados.put("erros", List.of());
        dados.put("mensagem", "");
        dados.put("trabalhos", conta.jobsDone());
        dados.put("demanda", demandaOpt.get());
        ctx.render("proposta", View.modelo(ctx, dados));
    }

    /** POST /demandas/proposta: registra a proposta do prestador. */
    public void enviar(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        long demandaId = idDe(ctx.formParam("id"));
        String mensagem = ctx.formParam("mensagem");
        var demandaOpt = demands.findById(demandaId);

        if (demandaOpt.isEmpty() || !podePropor(conta, demandaOpt.get())) {
            ctx.redirect("/vitrine");
            return;
        }
        var erros = ProposalService.validar(mensagem);
        if (!erros.isEmpty()) {
            ctx.status(422);
            var dados = new HashMap<String, Object>();
            dados.put("erros", erros);
            dados.put("mensagem", mensagem == null ? "" : mensagem);
            dados.put("trabalhos", conta.jobsDone());
            dados.put("demanda", demandaOpt.get());
            ctx.render("proposta", View.modelo(ctx, dados));
            return;
        }
        propostas.create(demandaId, conta.id(), mensagem);
        ctx.redirect("/vitrine");
    }

    /** POST /perfil/demandas/proposta/aceitar: dono aceita uma proposta. */
    public void aceitar(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        long propostaId = idDe(ctx.formParam("id"));
        var proposta = propostas.findById(propostaId);

        // Só o DONO da demanda pode aceitar (conferência pelo id do dono)
        if (proposta.isPresent()) {
            var demanda = demands.findById(proposta.get().demandId());
            if (demanda.isPresent() && demanda.get().residentId() == conta.id()) {
                propostas.aceitar(propostaId);
            }
        }
        ctx.redirect("/perfil/demandas");
    }

    // ---------- Internos ----------

    /** Prestador propõe: demanda aberta, não é dele e ainda não propôs. */
    private boolean podePropor(Resident conta, Demand demanda) {
        return Demand.ABERTA.equals(demanda.status())
            && demanda.residentId() != conta.id()
            && !propostas.existsFor(demanda.id(), conta.id());
    }

    private static long idDe(String texto) {
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
