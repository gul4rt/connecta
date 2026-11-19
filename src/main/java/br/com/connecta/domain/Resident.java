package br.com.connecta.domain;

import java.time.OffsetDateTime;

/**
 * Morador com conta na plataforma.
 *
 * role: "provider" (quer prestar serviços) ou "seeker" (quer contratar).
 * phone só existe para provider e nunca é exibido antes do aceite de um pedido.
 */
public record Resident(
    Long id,
    String username,
    String role,
    String displayName,
    String phone,
    Long neighborhoodId,
    String neighborhoodName,
    OffsetDateTime consentAt,
    boolean profileCompleted,
    int jobsDone
) {
    /** Rótulo em português para a interface. */
    public String roleLabel() {
        return "provider".equals(role) ? "Quero prestar serviços" : "Quero contratar um serviço";
    }
}
