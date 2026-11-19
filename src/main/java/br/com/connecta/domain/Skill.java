package br.com.connecta.domain;

/**
 * Habilidade declarada por um morador: algo que SABE fazer ou QUER aprender,
 * sempre ligada a uma categoria de serviço. Não exige diploma.
 */
public record Skill(Long id, long residentId, String categoryName,
                    String kind, String name) {

    /** "have" (sei fazer) ou "want" (quero aprender). */
    public static final String SEI_FAZER = "have";
    public static final String QUERO_APRENDER = "want";

    public String kindLabel() {
        return SEI_FAZER.equals(kind) ? "Sei fazer" : "Quero aprender";
    }
}
