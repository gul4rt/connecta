package br.com.connecta.service;

import br.com.connecta.domain.TrackStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regras das trilhas (Fase 3): autodeclaração com termo e o estado
 * das etapas da timeline (done/current/pending).
 */
class TrackServiceTest {

    // ---------- validarConclusao ----------

    @Test
    void conclusaoValidaComTermoETodasEtapas() {
        var erros = TrackService.validarConclusao(9, 9, true);
        assertTrue(erros.isEmpty());
    }

    @Test
    void conclusaoSemTermoEhRecusada() {
        // autodeclaração: SEM o termo marcado não ganha o selo
        var erros = TrackService.validarConclusao(9, 9, false);
        assertEquals(1, erros.size());
        assertTrue(erros.get(0).contains("confirmação"));
    }

    @Test
    void conclusaoComEtapasFaltandoEhRecusada() {
        var erros = TrackService.validarConclusao(9, 7, true);
        assertEquals(1, erros.size());
        assertTrue(erros.get(0).contains("7 de 9"));
    }

    @Test
    void trilhaVaziaNaoPodeSerConcluida() {
        var erros = TrackService.validarConclusao(0, 0, true);
        assertEquals(1, erros.size());
    }

    // ---------- estadosDasEtapas ----------

    private TrackStep etapa(long id, boolean done) {
        return new TrackStep(id, (int) id, "Etapa " + id, List.of("https://youtube.com/"), done);
    }

    @Test
    void primeiraEtapaPendenteFicaComoAtual() {
        var etapas = List.of(etapa(1, true), etapa(2, false), etapa(3, false));
        var estados = TrackService.estadosDasEtapas(etapas, false);

        assertEquals("done", estados.get(0).get("estado"));
        assertEquals("current", estados.get(1).get("estado"));
        assertEquals("pending", estados.get(2).get("estado"));
        assertSame(etapas.get(1), estados.get(1).get("etapa"));
    }

    @Test
    void trilhaNaoComecadaTemPrimeiraEtapaComoAtual() {
        var etapas = List.of(etapa(1, false), etapa(2, false));
        var estados = TrackService.estadosDasEtapas(etapas, false);
        assertEquals("current", estados.get(0).get("estado"));
        assertEquals("pending", estados.get(1).get("estado"));
    }

    @Test
    void trilhaConcluidaNaoTemEtapaAtual() {
        var etapas = List.of(etapa(1, true), etapa(2, true));
        var estados = TrackService.estadosDasEtapas(etapas, true);
        assertEquals("done", estados.get(0).get("estado"));
        assertEquals("done", estados.get(1).get("estado"));
    }
}
