package br.com.connecta.domain;

/**
 * Alvo de avaliação (Fase 5): o trabalho que a pessoa pode avaliar.
 * tipo: "demanda" (proposta aceita) ou "pedido" (pedido direto aceito).
 * O banco confirma, no INSERT, que quem avalia contratou — esta classe
 * só carrega o que a tela precisa mostrar.
 */
public record ReviewTarget(
    String tipo,
    long alvoId,
    String providerName
) {
}
