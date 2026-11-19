package br.com.connecta.domain;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Demanda: o serviço que um morador está procurando.
 * status: "open" (aberta) ou "attended" (atendida por prestador do bairro).
 * offerAmount: quanto quem abre está disposto a pagar; contactPhone:
 * telefone para contato sobre ESTA demanda.
 */
public record Demand(
    Long id,
    long residentId,
    String ownerUsername,
    String categoryName,
    String neighborhoodName,
    String description,
    BigDecimal offerAmount,
    String contactPhone,
    String status,
    OffsetDateTime createdAt
) {
    public static final String ABERTA = "open";
    public static final String ATENDIDA = "attended";

    public String statusLabel() {
        return ABERTA.equals(status) ? "Aberta" : "Atendida";
    }

    /** Ex.: R$ 120,00 */
    public String ofertaFormatada() {
        var formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return formato.format(offerAmount);
    }

    /** Ex.: 19/11/2026 */
    public String dataAbertura() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Ex.: (11) 98765-4321 */
    public String telefoneFormatado() {
        if (contactPhone == null || contactPhone.length() < 10) {
            return contactPhone == null ? "" : contactPhone;
        }
        String d = contactPhone;
        String ddd = d.substring(0, 2);
        String parte1 = d.length() == 11 ? d.substring(2, 7) : d.substring(2, 6);
        String parte2 = d.length() == 11 ? d.substring(7) : d.substring(6);
        return "(" + ddd + ") " + parte1 + "-" + parte2;
    }
}
