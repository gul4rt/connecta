package br.com.connecta.repository;

import br.com.connecta.domain.Review;
import br.com.connecta.domain.ReviewTarget;

import java.util.List;
import java.util.Optional;

/**
 * Avaliações de serviço (Fase 5). "Quem contratou" é confirmado PELO BANCO
 * nos INSERTs (joins com proposta aceita / pedido aceito) — nem serviço
 * nem template podem furar a regra.
 */
public interface ReviewRepository {

    /** O trabalho (demanda com proposta aceita) que esta pessoa pode avaliar. */
    Optional<ReviewTarget> alvoDemanda(long demandId, long reviewerId);

    /** O trabalho (pedido direto aceito) que esta pessoa pode avaliar. */
    Optional<ReviewTarget> alvoPedido(long requestId, long reviewerId);

    /** Já foi avaliado? (para os botões das listagens) */
    boolean existePorDemanda(long demandId);

    boolean existePorPedido(long requestId);

    /**
     * Grava a avaliação e, na MESMA transação, concede o selo por boas
     * avaliações se for o caso (média >= 4 com >= 2 avaliações).
     * true = avaliação gravada; false = regra de contratação não bateu.
     */
    boolean avaliarDemanda(long demandId, long reviewerId, int rating, String comment);

    boolean avaliarPedido(long requestId, long reviewerId, int rating, String comment);

    /** Todas as avaliações de um prestador, mais recentes primeiro (vitrine). */
    List<Review> findByProvider(long providerId);
}
