package br.com.connecta.service;

import br.com.connecta.domain.ServiceRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras do pedido direto de serviço (Fase 4), sem banco e sem web:
 * fáceis de testar e de explicar no vídeo técnico.
 *
 * Regra de privacidade central: o telefone do prestador só é revelado
 * para quem enviou o pedido DEPOIS que o prestador ACEITA.
 */
public final class ServiceRequestService {

    private ServiceRequestService() {
    }

    /**
     * Valida o pedido de contato: não pode chamar a si mesmo (o card nem
     * mostra o botão, mas o POST também é validado) nem chamar alguém que
     * não é prestador com perfil completo.
     */
    public static List<String> validarPedido(long quemPede, long prestadorId,
                                             boolean prestadorExiste, boolean ehPrestador) {
        var erros = new ArrayList<String>();
        if (quemPede == prestadorId) {
            erros.add("Você não pode chamar você mesmo para um serviço.");
        }
        if (!prestadorExiste || !ehPrestador) {
            erros.add("Este prestador não está mais disponível na vitrine.");
        }
        return erros;
    }

    /** Só o prestador destinatário pode responder, e só pedidos pendentes. */
    public static boolean podeResponder(ServiceRequest pedido, long prestadorId) {
        return pedido.providerId() == prestadorId && ServiceRequest.PENDENTE.equals(pedido.status());
    }

    /**
     * O contato do prestador aparece para quem pediu? REGRA DE PRIVACIDADE:
     * somente quando o pedido está ACEITO.
     */
    public static boolean contatoVisivel(ServiceRequest pedido) {
        return ServiceRequest.ACEITO.equals(pedido.status());
    }

    /**
     * Prepara um pedido para a tela de QUEM ENVIOU: se não aceito, o
     * telefone é REMOVIDO do objeto (nem chega ao template) — a regra não
     * depende de o template lembrar de esconder.
     */
    public static ServiceRequest ocultarContatoSeNaoAceito(ServiceRequest pedido) {
        if (contatoVisivel(pedido)) {
            return pedido;
        }
        return new ServiceRequest(pedido.id(), pedido.requesterId(), pedido.requesterName(),
            pedido.providerId(), pedido.providerName(), null, pedido.message(),
            pedido.status(), pedido.createdAt());
    }
}
