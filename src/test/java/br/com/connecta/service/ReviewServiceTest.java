package br.com.connecta.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regras de avaliação (Fase 5). A regra central "não avalia sem ter
 * contratado" é garantida pelo SQL (JdbcReviewRepository); aqui testamos
 * o formulário e a fórmula do selo.
 */
class ReviewServiceTest {

    // ---------- validar (formulário) ----------

    @Test
    void notaForaDaFaixaEhRecusada() {
        assertFalse(ReviewService.validar("0", "").isEmpty());
        assertFalse(ReviewService.validar("6", "").isEmpty());
        assertFalse(ReviewService.validar(null, "").isEmpty());
        assertFalse(ReviewService.validar("abc", "").isEmpty());
    }

    @Test
    void notaValidaComComentarioCurtoPassa() {
        assertTrue(ReviewService.validar("5", "Fez um ótimo trabalho, rapido e caprichado.").isEmpty());
        assertTrue(ReviewService.validar("1", null).isEmpty());
    }

    @Test
    void comentarioMuitoLongoEhRecusado() {
        String longo = "x".repeat(301);
        assertFalse(ReviewService.validar("4", longo).isEmpty());
        assertTrue(ReviewService.validar("4", "x".repeat(300)).isEmpty());
    }

    // ---------- mereceSeloPorAvaliacoes ----------

    @Test
    void mediaBoaComDuasAvaliacoesDaSelo() {
        assertTrue(ReviewService.mereceSeloPorAvaliacoes(4.0, 2, false));
        assertTrue(ReviewService.mereceSeloPorAvaliacoes(5.0, 3, false));
    }

    @Test
    void mediaBaixaNaoDaSelo() {
        assertFalse(ReviewService.mereceSeloPorAvaliacoes(3.9, 5, false));
    }

    @Test
    void soUmaAvaliacaoNaoDaSelo() {
        // uma única avaliação 5: média alta, mas amostra pequena demais
        assertFalse(ReviewService.mereceSeloPorAvaliacoes(5.0, 1, false));
    }

    @Test
    void quemJaTemSeloNaoPrecisaDeNovo() {
        assertFalse(ReviewService.mereceSeloPorAvaliacoes(5.0, 2, true));
    }
}
