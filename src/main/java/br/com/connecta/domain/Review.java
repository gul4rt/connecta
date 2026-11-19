package br.com.connecta.domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Uma avaliação escrita por quem contratou (Fase 5).
 * Aparece na vitrine, aberta no card do prestador (detalhes clicáveis).
 * reviewerName já vem só o primeiro nome — avaliação é pública, mas o
 * sobrenome da pessoa não precisa ir junto.
 */
public record Review(
    long id,
    long providerId,
    String reviewerName,
    int rating,
    String comment,
    OffsetDateTime createdAt
) {
    /** Ex.: 19/11/2026 */
    public String dataAvaliacao() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
