package br.com.connecta.service;

import br.com.connecta.domain.Resident;
import br.com.connecta.web.ProfileRoutes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras de privacidade e validação do perfil.
 * Inclui a regra de LGPD do telefone: só de quem PRESTA serviço,
 * e nunca exibido antes do aceite de um pedido (guarada na Fase 4).
 */
class ProfileValidationTest {

    private Resident contaDe(String role) {
        return new Resident(1L, "maria", role, null, null, null, null, null, false, 0);
    }

    @Test
    void perfilCompletoPassa() {
        assertTrue(ProfileRoutes.validarPerfil(contaDe("seeker"), "Maria Silva", "Jardim Ângela", "").isEmpty());
        assertTrue(ProfileRoutes.validarPerfil(contaDe("provider"), "Maria Silva", "Jardim Ângela", "11987654321").isEmpty());
    }

    @Test
    void prestadorSemTelefoneReprova() {
        assertFalse(ProfileRoutes.validarPerfil(contaDe("provider"), "Maria Silva", "Jardim Ângela", "").isEmpty());
        assertFalse(ProfileRoutes.validarPerfil(contaDe("provider"), "Maria Silva", "Jardim Ângela", "123").isEmpty());
    }

    @Test
    void contratanteNaoPrecisaDeTelefone() {
        // Coleta mínima: quem só contrata não informa telefone
        assertTrue(ProfileRoutes.validarPerfil(contaDe("seeker"), "Maria Silva", "Jardim Ângela", "").isEmpty());
    }

    @Test
    void nomeOuBairroCurtoReprova() {
        assertFalse(ProfileRoutes.validarPerfil(contaDe("seeker"), "M", "Jardim Ângela", "").isEmpty());
        assertFalse(ProfileRoutes.validarPerfil(contaDe("seeker"), "Maria Silva", "Ab", "").isEmpty());
    }

    @Test
    void telefoneComCaracteresNaoNumericosReprova() {
        assertFalse(ProfileRoutes.validarPerfil(contaDe("provider"), "Maria", "Jardim Ângela", "(11) 98765-4321").isEmpty());
    }
}
