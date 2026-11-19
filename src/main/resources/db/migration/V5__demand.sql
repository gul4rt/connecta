-- V5: demandas (Fase 2) — o serviço que um morador está procurando.
-- status: 'open' (aberta) ou 'attended' (atendida por um prestador do bairro).
-- A demanda nasce no bairro do morador que a registrou (coleta mínima:
-- nunca guardamos endereço completo).

CREATE TABLE demand (
    id              BIGSERIAL PRIMARY KEY,
    resident_id     BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    category_id     BIGINT NOT NULL REFERENCES service_category(id),
    neighborhood_id BIGINT NOT NULL REFERENCES neighborhood(id),
    description     TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'attended')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Consulta mais comum: demandas abertas de um bairro (home e vitrine)
CREATE INDEX idx_demand_neighborhood_status ON demand(neighborhood_id, status);
