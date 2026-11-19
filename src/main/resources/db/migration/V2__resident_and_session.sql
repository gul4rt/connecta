-- V2: contas (Fase 1) e sessões de login.
-- Senha guardada SEMPRE com hash BCrypt (nunca em texto).

CREATE TABLE resident (
    id                 BIGSERIAL PRIMARY KEY,
    username           TEXT NOT NULL UNIQUE,
    password_hash      TEXT NOT NULL,
    -- perfil escolhido no cadastro: quem oferta ou quem contrata serviço
    role               TEXT NOT NULL CHECK (role IN ('provider', 'seeker')),
    display_name       TEXT,
    -- só coletado de quem presta serviço; nunca aparece antes do aceite de um pedido
    phone              TEXT,
    neighborhood_id    BIGINT REFERENCES neighborhood(id),
    -- consentimento LGPD: data/hora do aceite no cadastro
    consent_at         TIMESTAMPTZ,
    profile_completed  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sessões de login no próprio banco (visível para o vídeo técnico).
-- Proteção CSRF fica em um cookie "double-submit" (ver web/Csrf.java),
-- que também cobre os formulários de quem ainda não está logado.
CREATE TABLE web_session (
    token        TEXT PRIMARY KEY,
    resident_id  BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL DEFAULT now() + INTERVAL '30 days'
);
