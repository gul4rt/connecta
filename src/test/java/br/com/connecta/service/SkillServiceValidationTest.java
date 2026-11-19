package br.com.connecta.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validação de habilidades (sem tocar no banco). */
class SkillServiceValidationTest {

    private static final List<Long> CATEGORIAS = List.of(1L, 2L, 3L, 4L, 5L);

    @Test
    void habilidadeValidaPassa() {
        assertTrue(SkillService.validar("3", "have", "conserto de bicicletas", CATEGORIAS).isEmpty());
        assertTrue(SkillService.validar("1", "want", "instalação elétrica", CATEGORIAS).isEmpty());
    }

    @Test
    void nomeCurtoOuLongoReprova() {
        assertFalse(SkillService.validar("1", "have", "ab", CATEGORIAS).isEmpty());
        assertFalse(SkillService.validar("1", "have", "a".repeat(61), CATEGORIAS).isEmpty());
        assertFalse(SkillService.validar("1", "have", null, CATEGORIAS).isEmpty());
    }

    @Test
    void tipoInvalidoReprova() {
        assertFalse(SkillService.validar("1", "talvez", "conserto de bicicletas", CATEGORIAS).isEmpty());
        assertFalse(SkillService.validar("1", "", "conserto de bicicletas", CATEGORIAS).isEmpty());
    }

    @Test
    void categoriaInexistenteReprova() {
        assertFalse(SkillService.validar("99", "have", "conserto de bicicletas", CATEGORIAS).isEmpty());
        assertFalse(SkillService.validar("abc", "have", "conserto de bicicletas", CATEGORIAS).isEmpty());
    }
}
