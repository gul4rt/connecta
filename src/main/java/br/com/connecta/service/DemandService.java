package br.com.connecta.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Regras de validação de demanda (sem banco: fácil de testar). */
public class DemandService {

    /**
     * Valida o registro e devolve erros em português.
     * oferta vem como texto do formulário (ex.: "120,50" ou "120.50").
     */
    public static List<String> validar(String categoriaId, String descricao, String oferta,
                                       String telefone, String bairro, List<Long> categoriasValidas) {
        var erros = new ArrayList<String>();
        if (descricao == null || descricao.trim().length() < 10 || descricao.trim().length() > 120) {
            erros.add("Descreva o serviço em até 120 caracteres (mínimo 10). Ex.: preciso de alguém para consertar o portão.");
        }
        try {
            long id = Long.parseLong(categoriaId);
            if (!categoriasValidas.contains(id)) {
                erros.add("Escolha uma categoria da lista.");
            }
        } catch (NumberFormatException e) {
            erros.add("Escolha uma categoria da lista.");
        }
        var valor = parseOferta(oferta);
        if (valor == null || valor.compareTo(BigDecimal.ONE) < 0) {
            erros.add("Informe quanto está disposto a pagar (a partir de R$ 1).");
        }
        if (telefone == null || !telefone.trim().matches("\\d{10,11}")) {
            erros.add("Informe um telefone para contato (DDD + número, só dígitos).");
        }
        if (bairro == null || bairro.trim().length() < 3 || bairro.trim().length() > 80) {
            erros.add("Informe o bairro da demanda.");
        }
        return erros;
    }

    /** Aceita vírgula ou ponto como decimal; devolve null se inválido. */
    public static BigDecimal parseOferta(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
