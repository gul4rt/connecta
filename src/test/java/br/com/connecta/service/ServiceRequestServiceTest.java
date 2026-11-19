package br.com.connecta.service;

import br.com.connecta.domain.ServiceRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regras de privacidade do pedido direto de serviço (aceite da Fase 4):
 * o telefone do prestador só aparece DEPOIS do aceite.
 */
class ServiceRequestServiceTest {

    private ServiceRequest pedido(String status, long prestadorId) {
        return new ServiceRequest(1L, 10L, "Ana Exemplo", prestadorId, "Maria Exemplo",
            "11999990001", null, status, OffsetDateTime.now());
    }

    // ---------- validarPedido ----------

    @Test
    void naoPodeChamarSiMesmo() {
        var erros = ServiceRequestService.validarPedido(7L, 7L, true, true);
        assertEquals(1, erros.size());
        assertTrue(erros.get(0).contains("você mesmo"));
    }

    @Test
    void naoPodeChamarQuemNaoEhPrestadorOuNaoExiste() {
        // prestador não existe (foi excluído, por exemplo)
        assertFalse(ServiceRequestService.validarPedido(1L, 2L, false, true).isEmpty());
        // existe mas não é prestador com perfil completo
        assertFalse(ServiceRequestService.validarPedido(1L, 2L, true, false).isEmpty());
    }

    @Test
    void pedidoValidoNaoTemErros() {
        assertTrue(ServiceRequestService.validarPedido(1L, 2L, true, true).isEmpty());
    }

    // ---------- podeResponder ----------

    @Test
    void soOPrestadorDestinatarioPodeResponder() {
        assertTrue(ServiceRequestService.podeResponder(pedido("pending", 5L), 5L));
        assertFalse(ServiceRequestService.podeResponder(pedido("pending", 5L), 6L));
    }

    @Test
    void pedidoJaRespondidoNaoPodeSerRespondidoDeNovo() {
        assertFalse(ServiceRequestService.podeResponder(pedido("accepted", 5L), 5L));
        assertFalse(ServiceRequestService.podeResponder(pedido("rejected", 5L), 5L));
    }

    // ---------- contatoVisivel / ocultarContatoSeNaoAceito ----------

    @Test
    void contatoSoComPedidoAceito() {
        assertTrue(ServiceRequestService.contatoVisivel(pedido("accepted", 5L)));
        assertFalse(ServiceRequestService.contatoVisivel(pedido("pending", 5L)));
        assertFalse(ServiceRequestService.contatoVisivel(pedido("rejected", 5L)));
    }

    @Test
    void telefoneSaiDoObjetoEnquantoNaoAceito() {
        // pending: o telefone é removido do objeto — nem chega ao template
        var oculto = ServiceRequestService.ocultarContatoSeNaoAceito(pedido("pending", 5L));
        assertNull(oculto.providerPhone());

        // rejected: idem
        var recusado = ServiceRequestService.ocultarContatoSeNaoAceito(pedido("rejected", 5L));
        assertNull(recusado.providerPhone());

        // accepted: o telefone permanece (contato liberado)
        var aceito = ServiceRequestService.ocultarContatoSeNaoAceito(pedido("accepted", 5L));
        assertEquals("11999990001", aceito.providerPhone());
    }
}
