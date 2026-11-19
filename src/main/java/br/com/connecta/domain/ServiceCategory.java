package br.com.connecta.domain;

/**
 * Categoria de serviço usada em todo o sistema: demandas, trilhas e vitrine.
 * O documento define cinco: Manutenção e reparos, Cuidados, Tecnologia,
 * Gastronomia e Outros.
 */
public record ServiceCategory(Long id, String slug, String name) {
}
