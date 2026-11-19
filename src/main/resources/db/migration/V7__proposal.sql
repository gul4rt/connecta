-- V7: propostas de prestadores nas demandas (feedback do grupo, 2026-11-19).
-- Fluxo: prestador envia proposta (mensagem opcional) → dono da demanda vê
-- em "Minhas demandas" → ao ACEITAR, o contato dos dois é liberado, a
-- demanda vira 'attended', as outras propostas viram 'rejected' e o
-- contador de trabalhos realizados do prestador sobe (portfólio).

CREATE TABLE proposal (
    id          BIGSERIAL PRIMARY KEY,
    demand_id   BIGINT NOT NULL REFERENCES demand(id) ON DELETE CASCADE,
    provider_id BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    message     TEXT,
    status      TEXT NOT NULL DEFAULT 'pending'
                CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- uma proposta por prestador em cada demanda
    UNIQUE (demand_id, provider_id)
);

-- Contador de trabalhos realizados (portfólio do prestador)
ALTER TABLE resident ADD COLUMN jobs_done INTEGER NOT NULL DEFAULT 0;
