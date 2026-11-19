package br.com.connecta.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras de avaliação (Fase 5), puras e testáveis.
 * A regra "só avalia quem contratou" fica no banco (ver JdbcReviewRepository);
 * aqui ficam a validação do formulário e a fórmula do selo por avaliações:
 *
 *   SELO POR AVALIAÇÕES: média das notas >= 4 com no mínimo 2 avaliações.
 *   (Mesma regra aplicada na concessão — mantida nos dois lugares por
 *   clareza didática; o teste cobre a fórmula.)
 */
public final class ReviewService {

    public static final int NOTA_MEDIA_MINIMA = 4;
    public static final int AVALIACOES_MINIMAS = 2;

    private ReviewService() {
    }

    /** Valida nota e comentário do formulário. Erros em português. */
    public static List<String> validar(String nota, String comentario) {
        var erros = new ArrayList<String>();
        int valor = 0;
        try {
            valor = Integer.parseInt(nota == null ? "" : nota.trim());
        } catch (NumberFormatException ignorada) {
            // cai no erro de faixa abaixo
        }
        if (valor < 1 || valor > 5) {
            erros.add("Escolha uma nota de 1 a 5.");
        }
        if (comentario != null && comentario.length() > 300) {
            erros.add("O comentário pode ter até 300 caracteres.");
        }
        return erros;
    }

    /**
     * O prestador ganha o selo por avaliações? Fórmula documentada:
     * media = AVG(nota); selo se media >= 4 E total >= 2.
     */
    public static boolean mereceSeloPorAvaliacoes(double media, long total, boolean jaTemSelo) {
        return !jaTemSelo && total >= AVALIACOES_MINIMAS && media >= NOTA_MEDIA_MINIMA;
    }
}
