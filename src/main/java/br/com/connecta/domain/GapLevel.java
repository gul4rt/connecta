package br.com.connecta.domain;

/**
 * Nível de procura de uma categoria no bairro, a partir da lacuna de mão de obra
 * (demandas abertas menos prestadores ativos).
 *
 * Os limiares vêm de configuração (AppConfig), então podem ser ajustados
 * sem mudar código: CONNECTA_GAP_MUITA e CONNECTA_GAP_MEDIA.
 */
public enum GapLevel {
    MUITA_PROCURA("Muita procura"),
    PROCURA_MEDIA("Procura média"),
    POUCA_PROCURA("Pouca procura");

    private final String label;

    GapLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Converte uma lacuna (gap) em rótulo, com os limiares configurados. */
    public static GapLevel fromGap(int gap, int limiarMuita, int limiarMedia) {
        if (gap >= limiarMuita) {
            return MUITA_PROCURA;
        }
        if (gap >= limiarMedia) {
            return PROCURA_MEDIA;
        }
        return POUCA_PROCURA;
    }
}
