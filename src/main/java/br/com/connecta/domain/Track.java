package br.com.connecta.domain;

/**
 * Trilha de capacitação: uma timeline de cursos (playlists do YouTube)
 * que a pessoa faz em ordem para aprender um ofício.
 * categorySlug liga a trilha a uma categoria de serviço do bairro.
 */
public record Track(
    Long id,
    String slug,
    String title,
    String description,
    String categorySlug,
    String categoryName
) {
}
