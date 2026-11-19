package br.com.connecta.domain;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Card de prestador da aba "Profissionais" da vitrine (Fase 4).
 * oficios = o que ele declara saber fazer (habilidades "sei fazer");
 * categorias = slugs para o filtro por categoria.
 * mediaAvaliacoes/reviewCount alimentam a nota da vitrine (Fase 5).
 * O telefone NUNCA vem para cá: só aparece após o aceite de um pedido.
 */
public record ProviderCard(
    long providerId,
    String displayName,
    String username,
    String neighborhoodName,
    int jobsDone,
    boolean verified,
    List<String> oficios,
    List<String> categorias,
    Double mediaAvaliacoes,
    long reviewCount
) {
    /** Linha do card: "{ofício} · mora em {bairro}" (certa nos dois escopos de filtro). */
    public String oficioTexto() {
        String oficio = oficios == null || oficios.isEmpty()
            ? "Disponível para serviços"
            : String.join(", ", oficios);
        String bairro = neighborhoodName == null ? "São Paulo" : neighborhoodName;
        return oficio + " · mora em " + bairro;
    }

    public boolean atuaEm(String categorySlug) {
        return categorias != null && categorias.contains(categorySlug);
    }

    /** Ex.: "★ 4,5 · 2 avaliações" (ou null se ainda não tem avaliação). */
    public String notaTexto() {
        if (reviewCount == 0 || mediaAvaliacoes == null) {
            return null;
        }
        var formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        formato.setMaximumFractionDigits(1);
        formato.setMinimumFractionDigits(1);
        String vezes = reviewCount == 1 ? "avaliação" : "avaliações";
        return "★ " + formato.format(mediaAvaliacoes) + " · " + reviewCount + " " + vezes;
    }

    /** Primeira letra do nome, para o avatar. */
    public String inicial() {
        String nome = displayName == null || displayName.isBlank() ? username : displayName;
        return nome == null || nome.isBlank() ? "?" : nome.trim().substring(0, 1).toUpperCase();
    }
}
