package br.com.connecta.service;

import java.util.ArrayList;
import java.util.List;

/** Regras de validação de proposta (sem banco: fácil de testar). */
public class ProposalService {

    public static final int MAXIMO_MENSAGEM = 500;

    /** A mensagem é opcional; quando existe, cabe em 500 caracteres. */
    public static List<String> validar(String mensagem) {
        var erros = new ArrayList<String>();
        if (mensagem != null && mensagem.length() > MAXIMO_MENSAGEM) {
            erros.add("A mensagem pode ter no máximo " + MAXIMO_MENSAGEM + " caracteres.");
        }
        return erros;
    }
}
