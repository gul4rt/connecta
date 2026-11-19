package br.com.connecta.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Limite de tentativas de login: 5 falhas bloqueiam, acerto limpa. */
class LoginAttemptLimiterTest {

    @Test
    void quatroFalhasNaoBloqueiam() {
        var limite = new LoginAttemptLimiter();
        for (int i = 0; i < 4; i++) {
            limite.registrarFalha("maria");
        }
        assertFalse(limite.bloqueado("maria"));
    }

    @Test
    void cincoFalhasBloqueiam() {
        var limite = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("maria");
        }
        assertTrue(limite.bloqueado("maria"));
        assertTrue(limite.segundosRestantes("maria") > 0);
    }

    @Test
    void bloqueioEPorUsername() {
        var limite = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("maria");
        }
        assertTrue(limite.bloqueado("MARIA "));
        assertFalse(limite.bloqueado("joao"));
    }

    @Test
    void acertoLimpaAsFalhas() {
        var limite = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("maria");
        }
        limite.limpar("maria");
        assertFalse(limite.bloqueado("maria"));
    }
}
