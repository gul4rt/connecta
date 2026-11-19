package br.com.connecta.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes do rótulo de procura a partir da lacuna de mão de obra.
 * Limiares agora vêm de configuração (CONNECTA_GAP_MUITA / CONNECTA_GAP_MEDIA);
 * os testes exercitam o padrão (5 e 2) e valores alternativos.
 */
class GapLevelTest {

    private static final int MUITA = 5;
    private static final int MEDIA = 2;

    @Test
    void lacunaAltaÉMuitaProcura() {
        assertEquals(GapLevel.MUITA_PROCURA, GapLevel.fromGap(5, MUITA, MEDIA));
        assertEquals(GapLevel.MUITA_PROCURA, GapLevel.fromGap(12, MUITA, MEDIA));
    }

    @Test
    void lacunaMédiaÉProcuraMédia() {
        assertEquals(GapLevel.PROCURA_MEDIA, GapLevel.fromGap(2, MUITA, MEDIA));
        assertEquals(GapLevel.PROCURA_MEDIA, GapLevel.fromGap(4, MUITA, MEDIA));
    }

    @Test
    void lacunaBaixaOuZeroÉPoucaProcura() {
        assertEquals(GapLevel.POUCA_PROCURA, GapLevel.fromGap(1, MUITA, MEDIA));
        assertEquals(GapLevel.POUCA_PROCURA, GapLevel.fromGap(0, MUITA, MEDIA));
    }

    @Test
    void limiaresConfiguráveisMudamORótulo() {
        // Com limiar médio = 6, uma lacuna de 4 passa a ser "pouca procura"
        assertEquals(GapLevel.POUCA_PROCURA, GapLevel.fromGap(4, 10, 6));
        assertEquals(GapLevel.PROCURA_MEDIA, GapLevel.fromGap(6, 10, 6));
    }
}
