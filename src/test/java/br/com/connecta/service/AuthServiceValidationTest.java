package br.com.connecta.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras de validação do cadastro (sem tocar no banco). */
class AuthServiceValidationTest {

    @Test
    void cadastroValidoPassa() {
        assertTrue(AuthService.validarCadastro("maria.silva", "senhaSegura1", "senhaSegura1",
            "provider", true).isEmpty());
        assertTrue(AuthService.validarCadastro("joao-01".replace("-", "_"), "senhaSegura1", "senhaSegura1",
            "seeker", true).isEmpty());
    }

    @Test
    void usernameCurtoOuInvalidoReprova() {
        assertFalse(AuthService.validarCadastro("ab", "senhaSegura1", "senhaSegura1", "seeker", true).isEmpty());
        assertFalse(AuthService.validarCadastro("tem espaço", "senhaSegura1", "senhaSegura1", "seeker", true).isEmpty());
        assertFalse(AuthService.validarCadastro(null, "senhaSegura1", "senhaSegura1", "seeker", true).isEmpty());
    }

    @Test
    void senhaCurtaOuConfirmacaoDiferenteReprova() {
        assertFalse(AuthService.validarCadastro("maria", "curta", "curta", "seeker", true).isEmpty());
        assertFalse(AuthService.validarCadastro("maria", "senhaSegura1", "outraSenha1", "seeker", true).isEmpty());
    }

    @Test
    void semRoleOuSemConsentimentoReprova() {
        assertFalse(AuthService.validarCadastro("maria", "senhaSegura1", "senhaSegura1", "", true).isEmpty());
        assertFalse(AuthService.validarCadastro("maria", "senhaSegura1", "senhaSegura1", "provider", false).isEmpty());
    }

    @Test
    void senhasSaoSempreHasheadasDiferente() {
        // Dois hashes do mesmo texto nunca são iguais (salt aleatório do BCrypt)
        var hash1 = org.mindrot.jbcrypt.BCrypt.hashpw("senhaSegura1", org.mindrot.jbcrypt.BCrypt.gensalt());
        var hash2 = org.mindrot.jbcrypt.BCrypt.hashpw("senhaSegura1", org.mindrot.jbcrypt.BCrypt.gensalt());
        assertFalse(hash1.equals(hash2));
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("senhaSegura1", hash1));
        assertFalse(org.mindrot.jbcrypt.BCrypt.checkpw("senhaErrada", hash1));
    }
}
