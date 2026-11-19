package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import br.com.connecta.domain.ServiceRequest;
import br.com.connecta.repository.ProviderRepository;
import br.com.connecta.repository.ReviewRepository;
import br.com.connecta.repository.ServiceRequestRepository;
import br.com.connecta.service.ServiceRequestService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Pedido direto de serviço (Fase 4): "Chamar para um serviço" no card do
 * prestador cria um pedido sem demanda pública; o prestador aceita/recusa
 * em /perfil/pedidos e o aceite libera o contato dele para quem pediu.
 * A avaliação do trabalho aceito fica em ReviewRoutes (Fase 5).
 */
public class RequestRoutes {

    /** Pedido enviado por quem pediu, com a flag "já avaliou?" (Fase 5). */
    public record PedidoEnviado(ServiceRequest pedido, boolean avaliado) {
    }

    private final ServiceRequestRepository pedidos;
    private final ProviderRepository prestadores;
    private final ReviewRepository avaliacoes;

    public RequestRoutes(ServiceRequestRepository pedidos, ProviderRepository prestadores,
                         ReviewRepository avaliacoes) {
        this.pedidos = pedidos;
        this.prestadores = prestadores;
        this.avaliacoes = avaliacoes;
    }

    /** POST /prestadores/{id}/pedidos — botão do card na vitrine. */
    public void chamar(Context ctx) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        long prestadorId;
        try {
            prestadorId = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException ignorada) {
            prestadorId = -1;
        }
        var card = prestadores.findById(prestadorId);
        // findById já só devolve prestador com perfil completo: existe = é prestador
        var erros = ServiceRequestService.validarPedido(conta.id(), prestadorId,
            card.isPresent(), card.isPresent());

        if (!erros.isEmpty()) {
            ctx.redirect("/perfil/pedidos?pedido=erro");
            return;
        }
        if (!pedidos.create(conta.id(), prestadorId)) {
            // já existe pedido pendente deste morador para este prestador
            ctx.redirect("/perfil/pedidos?pedido=repetido");
            return;
        }
        ctx.redirect("/perfil/pedidos?pedido=enviado");
    }

    /** GET /perfil/pedidos — pedidos enviados e recebidos. */
    public void pagina(Context ctx) {
        Resident conta = ctx.attribute("residente");

        // ENVIADOS: o telefone do prestador só entra se o pedido foi aceito
        var enviados = new ArrayList<PedidoEnviado>();
        for (ServiceRequest pedido : pedidos.findByRequester(conta.id())) {
            var visivel = ServiceRequestService.ocultarContatoSeNaoAceito(pedido);
            enviados.add(new PedidoEnviado(visivel,
                avaliacoes.existePorPedido(pedido.id())));
        }

        var dados = new HashMap<String, Object>();
        dados.put("enviados", enviados);
        dados.put("recebidos", "provider".equals(conta.role())
            ? pedidos.findByProvider(conta.id()) : List.of());
        dados.put("ehPrestador", "provider".equals(conta.role()));
        String aviso = ctx.queryParam("pedido");
        dados.put("aviso", aviso);
        dados.put("avaliadoAgora", "1".equals(ctx.queryParam("avaliado")));
        ctx.render("pedidos", View.modelo(ctx, dados));
    }

    /** POST /perfil/pedidos/aceitar e /perfil/pedidos/recusar — só o destinatário. */
    public void responder(Context ctx) {
        Resident conta = exigeConta(ctx);
        if (conta == null) {
            return;
        }
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        String novoStatus = ctx.path().endsWith("/aceitar")
            ? ServiceRequest.ACEITO : ServiceRequest.RECUSADO;
        long id;
        try {
            id = Long.parseLong(ctx.formParam("id"));
        } catch (NumberFormatException ignorada) {
            id = -1;
        }
        // a checagem final fica no UPDATE (WHERE provider_id = ...): nem o
        // template nem a rota confiáveis — o banco é quem garante
        pedidos.responder(id, conta.id(), novoStatus);
        ctx.redirect("/perfil/pedidos");
    }

    // ---------- Internos ----------

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
