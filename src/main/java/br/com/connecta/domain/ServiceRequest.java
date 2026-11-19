package br.com.connecta.domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Pedido direto de serviço (Fase 4): um morador chama um prestador da
 * vitrine sem passar por demanda pública. Mesma mecânica da proposta:
 * pending → accepted/rejected. O telefone do prestador só é mostrado
 * a quem enviou o pedido DEPOIS do aceite (regra de privacidade).
 */
public record ServiceRequest(
    Long id,
    long requesterId,
    String requesterName,
    long providerId,
    String providerName,
    String providerPhone,
    String message,
    String status,
    OffsetDateTime createdAt
) {
    public static final String PENDENTE = "pending";
    public static final String ACEITO = "accepted";
    public static final String RECUSADO = "rejected";

    public String statusLabel() {
        return switch (status) {
            case ACEITO -> "Aceito";
            case RECUSADO -> "Recusado";
            default -> "Aguardando";
        };
    }

    /** Ex.: 19/11/2026 */
    public String dataAbertura() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Ex.: (11) 98765-4321 — só deve aparecer quando o status é ACEITO. */
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
