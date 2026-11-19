package br.com.connecta.domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tela de detalhe da trilha (/trilhas/{slug}): a trilha, as etapas em ordem
 * com o progresso da pessoa e o estado da inscrição.
 */
public record TrackDetail(
    Track track,
    boolean inscrita,
    boolean concluida,
    OffsetDateTime concluidaEm,
    List<TrackStep> etapas
) {
    /** Etapas concluídas pela pessoa. */
    public long etapasConcluidas() {
        return etapas.stream().filter(TrackStep::done).count();
    }

    public long totalEtapas() {
        return etapas.size();
    }

    /** Todas as etapas feitas? Condição para poder assinar o termo. */
    public boolean todasConcluidas() {
        return etapasConcluidas() == totalEtapas() && !etapas.isEmpty();
    }

    /** Ex.: 19/11/2026 */
    public String dataConclusao() {
        return concluidaEm == null ? "" : concluidaEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
