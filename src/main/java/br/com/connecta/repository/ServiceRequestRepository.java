package br.com.connecta.repository;

import br.com.connecta.domain.ServiceRequest;

import java.util.List;

/**
 * Pedidos diretos de serviço (Fase 4): morador chama um prestador da
 * vitrine sem demanda pública. Aceite/recusa é sempre do prestador.
 */
public interface ServiceRequestRepository {

    /** Cria o pedido pendente. true = criou; false = já existe pedido pendente igual. */
    boolean create(long requesterId, long providerId);

    /** Pedidos que a pessoa ENVIOU (com o nome/telefone do prestador). */
    List<ServiceRequest> findByRequester(long requesterId);

    /** Pedidos que a pessoa RECEBEU (com o nome de quem pediu). */
    List<ServiceRequest> findByProvider(long providerId);

    /** Aceita/recusa um pedido pendente — só o prestador destinatário. */
    boolean responder(long requestId, long providerId, String novoStatus);
}
