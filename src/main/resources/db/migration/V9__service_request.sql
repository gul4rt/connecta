-- V9: pedido direto de serviço (Fase 4 — vitrine verificada).
-- O botão "Chamar para um serviço" no card do prestador cria um pedido
-- SEM demanda pública (mesma mecânica de proposta da Fase 2):
-- prestador aceita/recusa → aceite libera o contato do prestador.
-- O telefone do prestador continua OCULTO até o aceite (regra de privacidade).

CREATE TABLE service_request (
    id           BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    provider_id  BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    message      TEXT,
    status       TEXT NOT NULL DEFAULT 'pending'
                 CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- um pedido pendente por par morador→prestador (aceite/recusa libera novo)
CREATE UNIQUE INDEX idx_service_request_pending
    ON service_request(requester_id, provider_id)
    WHERE status = 'pending';
