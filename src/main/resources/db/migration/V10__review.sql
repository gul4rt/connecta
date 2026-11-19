-- V10: avaliações de serviço (Fase 5).
-- REGRA: só avalia quem CONTRATOU (dono de demanda com proposta aceita
-- ou quem enviou pedido direto ACEITO), e uma única vez por serviço.
-- O selo por boas avaliações é concedido no mesmo INSERT (ver repositório):
-- média >= 4 com no mínimo 2 avaliações.

CREATE TABLE review (
    id          BIGSERIAL PRIMARY KEY,
    -- origem do trabalho: exatamente UMA das duas (xor no CHECK)
    demand_id   BIGINT REFERENCES demand(id) ON DELETE SET NULL,
    request_id  BIGINT REFERENCES service_request(id) ON DELETE SET NULL,
    reviewer_id BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    provider_id BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((demand_id IS NULL) <> (request_id IS NULL))
);

-- uma avaliação por demanda e uma por pedido direto
CREATE UNIQUE INDEX idx_review_demand ON review(demand_id) WHERE demand_id IS NOT NULL;
CREATE UNIQUE INDEX idx_review_request ON review(request_id) WHERE request_id IS NOT NULL;

CREATE INDEX idx_review_provider ON review(provider_id);
