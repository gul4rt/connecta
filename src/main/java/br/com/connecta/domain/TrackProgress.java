package br.com.connecta.domain;

import java.time.format.DateTimeFormatter;

/**
 * Trilha + progresso de UM morador, para a listagem /trilhas.
 * enrolled: a pessoa já começou ("Quero fazer esta trilha");
 * completed: trilha concluída com autodeclaração (termo) — ganhou o selo.
 */
public record TrackProgress(
    Track track,
    boolean enrolled,
    boolean completed,
    long etapasConcluidas,
    long totalEtapas
) {
    /** Ex.: "3 de 9 etapas concluídas" */
    public String progressoTexto() {
        return etapasConcluidas + " de " + totalEtapas + " etapas concluídas";
    }

    public int porcentagem() {
        return totalEtapas == 0 ? 0 : (int) (etapasConcluidas * 100 / totalEtapas);
    }
}
