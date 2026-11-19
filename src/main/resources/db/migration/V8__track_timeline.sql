-- V8: trilhas como timeline de cursos (Fase 3, mudança de escopo de 2026-11-19).
-- As trilhas são playlists do YouTube fornecidas pelo grupo, organizadas em
-- etapas que a pessoa faz em ordem. Cada etapa = uma playlist; cada playlist
-- pode aparecer em mais de uma trilha no futuro (modelo separa etapa da trilha).
-- Os títulos das playlists vêm da própria API pública do YouTube (oEmbed):
-- nenhum título foi inventado.

CREATE TABLE track (
    id            BIGSERIAL PRIMARY KEY,
    slug          TEXT NOT NULL UNIQUE,
    title         TEXT NOT NULL,
    description   TEXT NOT NULL,
    -- categoria do documento à qual a trilha se conecta
    category_slug TEXT NOT NULL REFERENCES service_category(slug)
);

-- Uma etapa da timeline: uma playlist do YouTube, em ordem dentro da trilha.
CREATE TABLE track_step (
    id       BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title    TEXT NOT NULL,
    UNIQUE (track_id, position)
);

-- Links (playlists) da etapa — hoje 1 por etapa, mas o modelo permite mais.
CREATE TABLE playlist (
    id       BIGSERIAL PRIMARY KEY,
    step_id  BIGINT NOT NULL REFERENCES track_step(id) ON DELETE CASCADE,
    url      TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 1
);

-- Inscrição da pessoa na trilha: guarda o progresso e a AUTODECLARAÇÃO
-- (termo aceito com data/hora — decisão do grupo de 2026-11-19).
CREATE TABLE track_enrollment (
    id               BIGSERIAL PRIMARY KEY,
    resident_id      BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    track_id         BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    status           TEXT NOT NULL DEFAULT 'in_progress'
                     CHECK (status IN ('in_progress', 'completed')),
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ,
    -- data/hora do aceite do termo "confirmo que realizei os treinamentos"
    term_accepted_at TIMESTAMPTZ,
    UNIQUE (resident_id, track_id)
);

-- Etapas concluídas: uma linha por pessoa/etapa (a "atualização" do capítulo).
CREATE TABLE track_step_progress (
    resident_id  BIGINT NOT NULL REFERENCES resident(id) ON DELETE CASCADE,
    step_id      BIGINT NOT NULL REFERENCES track_step(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (resident_id, step_id)
);

-- Selo verificado: concedido quando a pessoa conclui uma trilha (ou, na
-- Fase 5, quando soma boas avaliações). Guardamos QUANDO ganhou, não por quê.
ALTER TABLE resident ADD COLUMN verified_at TIMESTAMPTZ;

-- ============================================================
-- Conteúdo das trilhas (playlists enviadas pelo grupo, 2026-11-19)
-- ============================================================

INSERT INTO track (slug, title, description, category_slug) VALUES
    ('programacao-web',
     'Programação Web: desenvolvimento de websites',
     'Aprenda a criar sites do zero: lógica de programação, HTML, CSS, JavaScript, banco de dados e publicação.',
     'tecnologia'),
    ('hardware-e-manutencao',
     'Hardware e manutenção de computadores',
     'Aprenda a montar, configurar e consertar computadores: informática, redes, sistemas operacionais e manutenção.',
     'tecnologia'),
    ('instalacao-eletrica',
     'Instalação elétrica residencial',
     'Aprenda a fazer instalações elétricas residenciais com segurança, do planejamento ao acabamento.',
     'manutencao');

-- ---------- Trilha: Programação Web (9 etapas) ----------

INSERT INTO track_step (track_id, position, title)
SELECT id, 1, 'Curso de Algoritmos e Lógica de Programação' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 2, 'HTML5 e CSS3 — Módulo 1 de 5' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 3, 'HTML5 e CSS3 — Módulo 2 de 5' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 4, 'HTML5 e CSS3 — Módulo 3 de 5' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 5, 'HTML5 e CSS3 — Módulo 4 de 5' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 6, 'JavaScript e ECMAScript para iniciantes' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 7, 'Banco de dados MySQL' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 8, 'Git e GitHub' FROM track WHERE slug = 'programacao-web';
INSERT INTO track_step (track_id, position, title)
SELECT id, 9, 'WordPress: criando um site do zero' FROM track WHERE slug = 'programacao-web';

INSERT INTO playlist (step_id, url)
SELECT s.id, x.url
FROM track_step s
JOIN track t ON t.id = s.track_id
JOIN (VALUES
    ('Curso de Algoritmos e Lógica de Programação', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dmSj0MHol_aoNYCSGFqvfXV'),
    ('HTML5 e CSS3 — Módulo 1 de 5', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dkZ9-atkcmcBaMZdmLHft8n'),
    ('HTML5 e CSS3 — Módulo 2 de 5', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dlUpEXkY1AyVLQGcpSgVF8s'),
    ('HTML5 e CSS3 — Módulo 3 de 5', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dmcAviDwiGgHbeEJToxbOpZ'),
    ('HTML5 e CSS3 — Módulo 4 de 5', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dkcVCk2Bn_fdVQ81Fkrh6WT'),
    ('JavaScript e ECMAScript para iniciantes', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dlsK3Nr9GVvXCbpQyHQl1o1'),
    ('Banco de dados MySQL', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dkBs-795Dsgvau_ekxg8g1r'),
    ('Git e GitHub', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dm7ZULPAmadvNhH6vk9oNZA'),
    ('WordPress: criando um site do zero', 'https://www.youtube.com/playlist?list=PLHz_AreHm4dmDP_RWdiKekjTEmCuq_MW2')
) AS x(titulo, url) ON x.titulo = s.title
WHERE t.slug = 'programacao-web';

-- ---------- Trilha: Hardware e manutenção (7 etapas) ----------

INSERT INTO track_step (track_id, position, title)
SELECT id, 1, 'Informática básica — Módulo 1' FROM track WHERE slug = 'hardware-e-manutencao';
INSERT INTO track_step (track_id, position, title)
SELECT id, 2, 'Informática básica — Módulo 2' FROM track WHERE slug = 'hardware-e-manutencao';
INSERT INTO track_step (track_id, position, title)
SELECT id, 3, 'Fundamentos de redes de computadores e internet' FROM track WHERE slug = 'hardware-e-manutencao';
INSERT INTO track_step (track_id, position, title)
SELECT id, 4, 'Sistemas operacionais' FROM track WHERE slug = 'hardware-e-manutencao';
INSERT INTO track_step (track_id, position, title)
SELECT id, 5, 'Montagem e manutenção de computadores' FROM track WHERE slug = 'hardware-e-manutencao';
INSERT INTO track_step (track_id, position, title)
SELECT id, 6, 'Manutenção de computadores' FROM track WHERE slug = 'hardware-e-manutencao';
INSERT INTO track_step (track_id, position, title)
SELECT id, 7, 'Manutenção de notebook' FROM track WHERE slug = 'hardware-e-manutencao';

INSERT INTO playlist (step_id, url)
SELECT s.id, x.url
FROM track_step s
JOIN track t ON t.id = s.track_id
JOIN (VALUES
    ('Informática básica — Módulo 1', 'https://www.youtube.com/playlist?list=PLAp37wMSBouByrGkI9JkLlqGe7eqI_bYI'),
    ('Informática básica — Módulo 2', 'https://www.youtube.com/playlist?list=PLAp37wMSBouDB89STzjYKZdyYZzu29y-E'),
    ('Fundamentos de redes de computadores e internet', 'https://www.youtube.com/playlist?list=PLAp37wMSBouDdpuuYhZfEK9oH0qk0IANb'),
    ('Sistemas operacionais', 'https://www.youtube.com/playlist?list=PLAp37wMSBouCZmNQGmErvuvdf14gKoiTx'),
    ('Montagem e manutenção de computadores', 'https://www.youtube.com/playlist?list=PLAp37wMSBouCBS51KUcd_yNBOzSd4Rby4'),
    ('Manutenção de computadores', 'https://www.youtube.com/playlist?list=PLAp37wMSBouCFx8IUPffzoqNpRpicZOfp'),
    ('Manutenção de notebook', 'https://www.youtube.com/playlist?list=PLAp37wMSBouBrILKtlcnOFG3L1V0HhHU1')
) AS x(titulo, url) ON x.titulo = s.title
WHERE t.slug = 'hardware-e-manutencao';

-- ---------- Trilha: Instalação elétrica residencial (1 etapa) ----------

INSERT INTO track_step (track_id, position, title)
SELECT id, 1, 'Curso gratuito de instalação elétrica residencial' FROM track WHERE slug = 'instalacao-eletrica';

INSERT INTO playlist (step_id, url)
SELECT s.id, 'https://www.youtube.com/playlist?list=PLogQNNMiBOEeoRU0IW9YlbZ84rie2QuWk'
FROM track_step s
JOIN track t ON t.id = s.track_id
WHERE t.slug = 'instalacao-eletrica';
