package br.com.connecta.domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Trilha concluída por um prestador: título e quando concluiu.
 * Aparece para quem recebe uma proposta, ao clicar no "Selo verificado".
 */
public record TrackConcluded(
    String trackTitle,
    OffsetDateTime completedAt
) {
    /** Ex.: 19/11/2026 */
    public String dataConclusao() {
        return completedAt == null ? "" : completedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
