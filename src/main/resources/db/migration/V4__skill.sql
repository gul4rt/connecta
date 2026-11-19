-- V4: habilidades do morador (Fase 1).
-- kind: 'have' (sei fazer) ou 'want' (quero aprender).
-- Cadastro sem barreira: habilidade aprendida na prática, sem diploma.

CREATE TABLE skill (
    id           BIGSERIAL PRIMARY KEY,
    resident_id  BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    category_id  BIGINT NOT NULL REFERENCES service_category(id),
    kind         TEXT NOT NULL CHECK (kind IN ('have', 'want')),
    name         TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_skill_resident ON skill(resident_id);
