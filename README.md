# Connecta

Ecossistema local que conecta **capacitação, demanda e oferta de serviços no mesmo território** (bairro de periferia). Protótipo em Java para a Global Solution 2026 · CitizenTech.

> Nome antigo no capítulo: *ConectaOfício*. É o mesmo projeto; aqui usamos **Connecta** em tudo.

## Como rodar (resumo rápido)

### Opção 1 — só com Docker (nada pra instalar além do Docker)

```bash
docker compose up -d --build
# Acesse: http://localhost:7000
```

O projeto compila **dentro** do Docker (build multi-stage: Maven + JDK 21 para
compilar, JRE 21 para rodar) e se conecta ao banco pelo hostname interno `db`.
Não precisa de Java, Maven ou qualquer versão de ferramenta na máquina.

Modo demonstração (recria o banco com dados fictícios e sobe):

```bash
docker compose down      # para o app normal
docker compose run --rm app demo
```

### Opção 2 — rodando na própria máquina (desenvolvimento)

```bash
# 1. Banco PostgreSQL local via Docker
docker compose up -d db

# 2. Rodar o sistema (aplica as migrações do banco automaticamente)
./mvnw compile exec:java

# Acesse: http://localhost:7000  (página /design só em desenvolvimento)
```

### Modo demonstração rodando na própria máquina (para o vídeo técnico)

```bash
# APAGA o banco e recria com dados de exemplo fictícios (usuários demo.*,
# senha demo123), depois sobe o sistema já com dados para mostrar:
./mvnw compile exec:java -Dexec.args=demo
```

O roteiro do vídeo está em [`docs/DEMO.md`](docs/DEMO.md).

## Como compilar, testar e empacotar

| Comando | O que faz |
|---|---|
| `./mvnw test` | Compila e roda os testes (JUnit 5) |
| `./mvnw package` | Gera `target/connecta.jar` (fat JAR) |
| `java -jar target/connecta.jar` | Roda o JAR |
| `./mvnw compile exec:java` | Compila e roda em modo desenvolvimento |

> **Sempre use `./mvnw`** (e não o `mvn` instalado no sistema): o plugin `exec` exige Maven 3.9+ e o wrapper já traz a versão certa, sem depender da máquina.

Variáveis de ambiente (veja `.env.example`): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `CONNECTA_ENV` (`dev` mostra a página `/design`; `prod` esconde).

## Stack e por quê

| Camada | Escolha | Por quê (uma frase) |
|---|---|---|
| Linguagem | Java 21 (LTS) | Linguagem do curso; orientação a objetos como eixo |
| Build | Maven | Um comando para compilar, testar e rodar |
| Web | Javalin 6.7.0 | Leve, rotas explícitas, sem "mágica" de anotações |
| Templates | Pebble 3.1.6 | HTML no servidor, com herança e macros (a versão que o Javalin 6 espera) |
| Acesso a dados | JDBC puro + HikariCP 6.3.0 | SQL visível (requisito do trabalho), `PreparedStatement` sempre |
| Migrações | Flyway 11.7.2 | Esquema do banco versionado em SQL (`src/main/resources/db/migration`) |
| Banco | PostgreSQL 17 (Docker) | Relacional, rodando localmente sem instalação |
| Testes | JUnit 5.12.2 | Obrigatórios para o algoritmo de recomendação e regras de privacidade |
| Logs | SLF4J + Logback 1.5.18 | Padrão de fato no ecossistema Java |

## Estrutura do código

```
src/main/java/br/com/connecta/
├── App.java                 # main: sobe o banco e o servidor
├── config/                  # AppConfig (variáveis de ambiente), Database (HikariCP + Flyway)
├── domain/                  # ServiceCategory, GapLevel (sem dependência de web ou banco)
├── repository/              # interface ServiceCategoryRepository + implementação JDBC
├── service/                 # HomeService (regras da página Início)
└── web/                     # View (preferências de acessibilidade via cookie)
```

Camadas: `domain` não depende de web nem de JDBC; `repository` conversa com o banco por interfaces; `web` só traduz HTTP.

## Documentação

Tudo em [`docs/`](docs/): arquitetura, modelo de dados, fluxos, dados coletados (LGPD), decisões, pendências e o roteiro de demonstração.
