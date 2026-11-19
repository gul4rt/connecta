package br.com.connecta.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras de validação de habilidades (sem banco: fácil de testar).
 * Cadastro sem barreira: descrição curta em texto livre, sem diploma.
 */
public class SkillService {

    public static final String SEI_FAZER = "have";
    public static final String QUERO_APRENDER = "want";

    /** Valida e devolve erros em português; recebe os ids de categoria válidos. */
    public static List<String> validar(String categoriaId, String kind, String nome,
                                       List<Long> categoriasValidas) {
        var erros = new ArrayList<String>();
        if (nome == null || nome.trim().length() < 3 || nome.trim().length() > 60) {
            erros.add("Descreva a habilidade em até 60 caracteres (mínimo 3).");
        }
        if (!SEI_FAZER.equals(kind) && !QUERO_APRENDER.equals(kind)) {
            erros.add("Escolha se você já sabe fazer ou quer aprender.");
        }
        try {
            long id = Long.parseLong(categoriaId);
            if (!categoriasValidas.contains(id)) {
                erros.add("Escolha uma categoria da lista.");
            }
        } catch (NumberFormatException e) {
            erros.add("Escolha uma categoria da lista.");
        }
        return erros;
    }
}
