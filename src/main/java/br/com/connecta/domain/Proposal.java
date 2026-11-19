package br.com.connecta.domain;

import java.time.OffsetDateTime;

/**
 * Proposta de um prestador para uma demanda.
 * status: "pending" (aguardando), "accepted" (aceita) ou "rejected".
 * Ao ser aceita, libera o contato dos dois lados e sobe o contador de
 * trabalhos realizados do prestador.
 */
public record Proposal(
    Long id,
    long demandId,
    long providerId,
    String providerUsername,
    int providerJobsDone,
    // selo verificado do prestador (trilha concluída — Fase 3): aparece
    // para quem recebe a proposta, ajuda a decidir se aceita
    boolean providerVerified,
    String providerPhone,
    String message,
    String status,
    OffsetDateTime createdAt
) {
    public static final String PENDENTE = "pending";
    public static final String ACEITA = "accepted";
    public static final String REJEITADA = "rejected";

    public String statusLabel() {
        return switch (status) {
            case ACEITA -> "Aceita";
            case REJEITADA -> "Não aceita";
            default -> "Aguardando";
        };
    }

    /** Ex.: (11) 98765-4321 — o telefone do prestador, só visível quando aceita. */
    public String telefoneFormatado() {
        String d = providerPhone == null ? "" : providerPhone;
        if (d.length() < 10) {
            return d;
        }
        String ddd = d.substring(0, 2);
        String parte1 = d.length() == 11 ? d.substring(2, 7) : d.substring(2, 6);
        String parte2 = d.length() == 11 ? d.substring(7) : d.substring(6);
        return "(" + ddd + ") " + parte1 + "-" + parte2;
    }
}
