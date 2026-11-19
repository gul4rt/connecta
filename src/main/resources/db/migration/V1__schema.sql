-- V1: schema inicial da Fase 0.
-- Por enquanto, apenas as tabelas que a página Início consome.
-- As demais entidades (Morador, Demanda, Trilha etc.) chegam nas Fases 1 a 3.

-- Território usado para calcular as lacunas de mão de obra.
-- O bairro-piloto ainda não foi definido: veja docs/PENDENCIAS.md.
CREATE TABLE neighborhood (
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

-- Categorias de serviço fixas do documento:
-- Manutenção e reparos, Cuidados, Tecnologia, Gastronomia e Outros.
CREATE TABLE service_category (
    id   BIGSERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL
);

INSERT INTO service_category (slug, name) VALUES
    ('manutencao', 'Manutenção e reparos'),
    ('cuidados', 'Cuidados'),
    ('tecnologia', 'Tecnologia'),
    ('gastronomia', 'Gastronomia'),
    ('outros', 'Outros');
