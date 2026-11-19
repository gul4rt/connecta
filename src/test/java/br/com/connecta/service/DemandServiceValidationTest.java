package br.com.connecta.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Validação do registro de demanda com oferta e contato (sem tocar no banco). */
class DemandServiceValidationTest {

    private static final List<Long> CATEGORIAS = List.of(1L, 2L, 3L, 4L, 5L);
    private static final String OK_DESC = "preciso de alguém para consertar o portão";

    @Test
    void demandaValidaPassa() {
        assertTrue(DemandService.validar("2", OK_DESC, "120,00", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
    }

    @Test
    void descricaoCurtaOuLongaReprova() {
        assertFalse(DemandService.validar("2", "curta", "120", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("2", "a".repeat(121), "120", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("2", null, "120", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
    }

    @Test
    void categoriaInvalidaReprova() {
        assertFalse(DemandService.validar("99", OK_DESC, "120", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("abc", OK_DESC, "120", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
    }

    @Test
    void ofertaObrigatoriaEApartirDeUmReal() {
        assertFalse(DemandService.validar("2", OK_DESC, null, "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("2", OK_DESC, "0", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("2", OK_DESC, "abc", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertTrue(DemandService.validar("2", OK_DESC, "1", "11987654321", "Jardim Ângela", CATEGORIAS).isEmpty());
    }

    @Test
    void telefoneInvalidoReprova() {
        assertFalse(DemandService.validar("2", OK_DESC, "120", "123", "Jardim Ângela", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("2", OK_DESC, "120", "(11) 98765-4321", "Jardim Ângela", CATEGORIAS).isEmpty());
    }

    @Test
    void bairroInvalidoReprova() {
        assertFalse(DemandService.validar("2", OK_DESC, "120", "11987654321", "ab", CATEGORIAS).isEmpty());
        assertFalse(DemandService.validar("2", OK_DESC, "120", "11987654321", null, CATEGORIAS).isEmpty());
    }

    @Test
    void parseOfertaAceitaVirgulaEPonto() {
        assertEquals(new java.math.BigDecimal("120.50"), DemandService.parseOferta("120,50"));
        assertEquals(new java.math.BigDecimal("120.50"), DemandService.parseOferta("120.50"));
        assertNull(DemandService.parseOferta("reais"));
        assertNull(DemandService.parseOferta(""));
    }
}
