package br.com.connecta;

import br.com.connecta.config.AppConfig;
import br.com.connecta.config.Database;
import br.com.connecta.domain.Resident;
import br.com.connecta.repository.JdbcDemandRepository;
import br.com.connecta.repository.JdbcNeighborhoodRepository;
import br.com.connecta.repository.JdbcProposalRepository;
import br.com.connecta.repository.JdbcProviderRepository;
import br.com.connecta.repository.JdbcResidentRepository;
import br.com.connecta.repository.JdbcReviewRepository;
import br.com.connecta.repository.JdbcServiceCategoryRepository;
import br.com.connecta.repository.JdbcServiceRequestRepository;
import br.com.connecta.repository.JdbcSessionRepository;
import br.com.connecta.repository.JdbcSkillRepository;
import br.com.connecta.repository.JdbcTrackRepository;
import br.com.connecta.repository.ResidentRepository;
import br.com.connecta.service.AuthService;
import br.com.connecta.service.HomeService;
import br.com.connecta.service.NeighborhoodService;
import br.com.connecta.web.AuthRoutes;
import br.com.connecta.web.DemandRoutes;
import br.com.connecta.web.NeighborhoodRoutes;
import br.com.connecta.web.ProfileRoutes;
import br.com.connecta.web.ProposalRoutes;
import br.com.connecta.web.RequestRoutes;
import br.com.connecta.web.ReviewRoutes;
import br.com.connecta.web.SkillRoutes;
import br.com.connecta.web.TrackRoutes;
import br.com.connecta.web.View;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinPebble;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.Map;

/**
 * Ponto de entrada: sobe o banco, o pool de conexões e o servidor web.
 * Rotas simples ficam aqui; as que têm regras ficam em web/ (AuthRoutes,
 * ProfileRoutes, NeighborhoodRoutes).
 */
public final class App {

    public static void main(String[] args) {
        // Modo demonstração: ./mvnw compile exec:java -Dexec.args=demo
        // (apaga e recria o banco com dados de exemplo — ver DemoMode/DemoSeed)
        boolean demo = args.length > 0 && "demo".equalsIgnoreCase(args[0]);
        DataSource dataSource = demo ? DemoMode.recriarBanco() : Database.init();

        var repositorioDeCategorias = new JdbcServiceCategoryRepository(dataSource);
        var repositorioDeDemandas = new JdbcDemandRepository(dataSource);
        var homeService = new HomeService(repositorioDeCategorias, repositorioDeDemandas);

        // Fase 1: contas, sessões e perfil
        ResidentRepository repositorioDeMoradores = new JdbcResidentRepository(dataSource);
        var repositorioDeSessoes = new JdbcSessionRepository(dataSource);
        var repositorioDeBairros = new JdbcNeighborhoodRepository(dataSource);
        var authService = new AuthService(repositorioDeMoradores, repositorioDeSessoes);
        var bairrosService = new NeighborhoodService(repositorioDeBairros);
        // Trilhas (Fase 3): o perfil precisa delas para mostrar o selo verificado
        var repositorioDeTrilhas = new JdbcTrackRepository(dataSource);
        var authRoutes = new AuthRoutes(authService, repositorioDeMoradores);
        var profileRoutes = new ProfileRoutes(repositorioDeMoradores, bairrosService, authService,
            repositorioDeTrilhas);
        var neighborhoodRoutes = new NeighborhoodRoutes(bairrosService);

        // Habilidades (Fase 1): usa o repositório de categorias que já existia
        var repositorioDeHabilidades = new JdbcSkillRepository(dataSource);
        var skillRoutes = new SkillRoutes(repositorioDeHabilidades, repositorioDeCategorias);

        // Demandas, propostas e vitrine (Fase 2) + Profissionais (Fase 4)
        var repositorioDePropostas = new JdbcProposalRepository(dataSource);
        var repositorioDePrestadores = new JdbcProviderRepository(dataSource);
        // Avaliações (Fase 5): usadas pelas demandas, pelos pedidos e pela vitrine
        var repositorioDeAvaliacoes = new JdbcReviewRepository(dataSource);
        var demandRoutes = new DemandRoutes(repositorioDeDemandas, repositorioDeCategorias,
            bairrosService, repositorioDePropostas, repositorioDeTrilhas,
            repositorioDePrestadores, repositorioDeAvaliacoes);
        var proposalRoutes = new ProposalRoutes(repositorioDePropostas, repositorioDeDemandas);

        // Pedidos diretos de serviço (Fase 4)
        var repositorioDePedidos = new JdbcServiceRequestRepository(dataSource);
        var requestRoutes = new RequestRoutes(repositorioDePedidos, repositorioDePrestadores,
            repositorioDeAvaliacoes);
        var reviewRoutes = new ReviewRoutes(repositorioDeAvaliacoes);

        // Trilhas como timeline de cursos (Fase 3)
        var trackRoutes = new TrackRoutes(repositorioDeTrilhas);

        // Templates Pebble: carrega do classpath, pasta templates/, extensão .peb
        // cacheActive(false): no Pebble 3.1.6, o cache re-registra as macros
        // importadas a cada render e quebra na segunda visita à mesma página
        // (ver docs/PROBLEMAS_TECNICOS.md, problema 3). Projeto local, tráfego
        // mínimo: recompilar o template por request não é um problema aqui.
        var loader = new ClasspathLoader();
        loader.setPrefix("templates/");
        loader.setSuffix(".peb");
        loader.setCharset("UTF-8");

        var motorDeTemplates = new PebbleEngine.Builder()
            .loader(loader)
            .cacheActive(false)
            .defaultLocale(new Locale("pt", "BR"))
            .build();

        var app = Javalin.create(config -> {
            // Estáticos servidos em /static/... mesmo caminho dos recursos
            config.staticFiles.add(staticConfig -> {
                staticConfig.hostedPath = "/static";
                staticConfig.directory = "/static";
                staticConfig.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
            config.fileRenderer(new JavalinPebble(motorDeTemplates));
            config.showJavalinBanner = false;
        });

        // Antes de qualquer rota: resolve a sessão e protege o que exige conta
        app.before(ctx -> {
            var sessao = authService.sessaoValida(ctx.cookie(AuthService.COOKIE_SESSAO));
            sessao.ifPresent(s -> repositorioDeMoradores.findById(s.residentId())
                .ifPresent(conta -> ctx.attribute("residente", conta)));

            // Ações exigem login (navegação fica livre). /perfil/senha também
            // é protegida: começa com /perfil.
            if (ctx.path().startsWith("/perfil") && !View.logado(ctx)) {
                ctx.redirect("/entrar");
                return;
            }

            // Páginas de uso da plataforma: pedem login E perfil completo
            // (nome, bairro e telefone definidos) — sem bairro não há
            // demandas nem lacuna do bairro para mostrar.
            if (exigePerfilCompleto(ctx.path())) {
                if (!View.logado(ctx)) {
                    ctx.redirect("/entrar");
                    return;
                }
                var conta = ctx.<Resident>attribute("residente");
                if (conta != null && !conta.profileCompleted()) {
                    ctx.redirect("/perfil/completar");
                }
            }
        });

        app.get("/", ctx -> {
            // Dados da home: reais quando o morador tem bairro; de exemplo, sem login
            var conta = ctx.<Resident>attribute("residente");
            Long bairroId = conta != null && conta.neighborhoodId() != null
                ? conta.neighborhoodId() : null;
            ctx.render("home", View.modelo(ctx, Map.of(
                "pagina", "inicio",
                "procura", homeService.topDemands(bairroId),
                "dadosReais", bairroId != null,
                "bairroNome", bairroId != null ? conta.neighborhoodName() : ""
            )));
        });

        // Fase 2: demandas e a Vitrine com abas (Demandas / Profissionais)
        app.get("/vitrine", demandRoutes::vitrine);
        app.get("/demandas/nova", demandRoutes::paginaNova);
        app.post("/demandas", demandRoutes::registrar);
        app.get("/demandas/{id}/proposta", proposalRoutes::formulario);
        app.post("/demandas/proposta", proposalRoutes::enviar);
        app.get("/perfil/demandas", demandRoutes::minhas);
        app.post("/perfil/demandas/status", demandRoutes::marcarAtendida);
        app.post("/perfil/demandas/excluir", demandRoutes::excluir);
        app.post("/perfil/demandas/proposta/aceitar", proposalRoutes::aceitar);

        // Fase 4: pedidos diretos de serviço
        app.post("/prestadores/{id}/pedidos", requestRoutes::chamar);
        app.get("/perfil/pedidos", requestRoutes::pagina);
        app.post("/perfil/pedidos/aceitar", requestRoutes::responder);
        app.post("/perfil/pedidos/recusar", requestRoutes::responder);

        // Fase 5: avaliações (só de quem contratou)
        app.get("/avaliar/demanda/{id}", reviewRoutes::paginaDemanda);
        app.get("/avaliar/pedido/{id}", reviewRoutes::paginaPedido);
        app.post("/avaliar", reviewRoutes::avaliar);

        // Fase 3: trilhas como timeline de playlists
        app.get("/trilhas", trackRoutes::listar);
        app.get("/trilhas/{slug}", trackRoutes::detalhe);
        app.post("/trilhas/{slug}/inscrever", trackRoutes::inscrever);
        app.post("/trilhas/{slug}/etapas", trackRoutes::marcarEtapa);
        app.post("/trilhas/{slug}/concluir", trackRoutes::concluir);

        // Fase 1: autenticação e perfil
        app.get("/entrar", authRoutes::paginaEntrar);
        app.post("/login", authRoutes::login);
        app.get("/cadastro", authRoutes::paginaCadastro);
        app.post("/cadastro", authRoutes::registrar);
        app.get("/recuperar", authRoutes::paginaRecuperar);
        app.post("/recuperar", authRoutes::redefinirSenha);
        app.post("/logout", authRoutes::logout);
        app.get("/perfil", profileRoutes::paginaPerfil);
        app.get("/perfil/completar", profileRoutes::paginaCompletar);
        app.post("/perfil", profileRoutes::salvar);
        app.get("/perfil/senha", profileRoutes::paginaTrocarSenha);
        app.post("/perfil/senha", profileRoutes::trocarSenha);
        app.post("/perfil/excluir", profileRoutes::excluir);
        app.get("/perfil/habilidades", skillRoutes::pagina);
        app.post("/perfil/habilidades", skillRoutes::adicionar);
        app.post("/perfil/habilidades/excluir", skillRoutes::excluir);
        app.get("/bairros", neighborhoodRoutes::buscar);

        app.get("/preferencias", View::salvarPreferencias);

        // Página de design system, só em desenvolvimento
        if (AppConfig.isDev()) {
            app.get("/design", ctx -> {
                var conta = ctx.<Resident>attribute("residente");
                Long bairroId = conta != null && conta.neighborhoodId() != null
                    ? conta.neighborhoodId() : null;
                ctx.render("design", View.modelo(ctx, Map.of(
                    "pagina", "design",
                    "procura", homeService.topDemands(bairroId)
                )));
            });
        }

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).result("Erro inesperado. Tente novamente.");
        });

        app.start(AppConfig.port());
        System.out.println("Connecta no ar em http://localhost:" + AppConfig.port());
    }

    /** Caminhos que a pessoa só acessa logada e com o perfil completo. */
    private static final String[] ROTAS_QUE_EXIGEM_PERFIL_COMPLETO = {
        "/perfil/habilidades",
        "/vitrine",
        "/demandas",
        "/perfil/demandas",
        "/perfil/pedidos",
        // nas próximas fases entram aqui: avaliações etc.
    };

    private static boolean exigePerfilCompleto(String caminho) {
        for (String prefixo : ROTAS_QUE_EXIGEM_PERFIL_COMPLETO) {
            if (caminho.startsWith(prefixo)) {
                return true;
            }
        }
        return false;
    }

    private App() {
    }
}
