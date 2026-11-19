package br.com.connecta.web;

import br.com.connecta.domain.Demand;
import br.com.connecta.domain.Proposal;
import br.com.connecta.domain.ProviderCard;
import br.com.connecta.domain.Resident;
import br.com.connecta.domain.Review;
import br.com.connecta.domain.TrackConcluded;
import br.com.connecta.repository.DemandRepository;
import br.com.connecta.repository.ProviderRepository;
import br.com.connecta.repository.ProposalRepository;
import br.com.connecta.repository.ReviewRepository;
import br.com.connecta.repository.ServiceCategoryRepository;
import br.com.connecta.repository.TrackRepository;
import br.com.connecta.service.DemandService;
import br.com.connecta.service.NeighborhoodService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demandas: registro (/demandas/nova), "Minhas demandas" (/perfil/demandas)
 * e a aba Demandas da Vitrine com filtros em chips para o prestador
 * (Meu bairro / Todos os bairros / Propostas aceitas).
 * As propostas em si ficam em ProposalRoutes.
 */
public class DemandRoutes {

    /** Demanda + propostas recebidas + a proposta do próprio prestador (cards). */
    public record DemandaComPropostas(Demand demanda, List<Proposal> propostas, Proposal minha) {
    }

    /**
     * Proposta recebida + as trilhas que o prestador concluiu (aparecem ao
     * clicar no "Selo verificado") — só usado em "Minhas demandas".
     */
    public record PropostaComTrilhas(Proposal proposta, List<TrackConcluded> trilhas) {
    }

    /** Demanda do dono com as propostas enriquecidas (tela "Minhas demandas"). */
    public record DemandaDoDono(Demand demanda, List<PropostaComTrilhas> propostas,
                                boolean avaliada) {
    }

    private final DemandRepository demands;
    private final ServiceCategoryRepository categorias;
    private final NeighborhoodService bairros;
    private final ProposalRepository propostas;
    private final TrackRepository trilhas;
    private final ProviderRepository prestadores;
    private final ReviewRepository avaliacoes;

    public DemandRoutes(DemandRepository demands, ServiceCategoryRepository categorias,
                        NeighborhoodService bairros, ProposalRepository propostas,
                        TrackRepository trilhas, ProviderRepository prestadores,
                        ReviewRepository avaliacoes) {
        this.demands = demands;
        this.categorias = categorias;
        this.bairros = bairros;
        this.propostas = propostas;
        this.trilhas = trilhas;
        this.prestadores = prestadores;
        this.avaliacoes = avaliacoes;
    }

    /** GET /vitrine: aba Demandas com filtros (chips, funcionam sem JS). */
    public void vitrine(Context ctx) {
        Resident conta = ctx.attribute("residente");
        String aba = "profissionais".equals(ctx.queryParam("aba")) ? "profissionais" : "demandas";
        // Filtro do prestador: meu bairro (padrão), todos ou aceitas
        String filtro = ctx.queryParam("filtro");
        if (filtro == null || !List.of("todos", "aceitas").contains(filtro)) {
            filtro = "bairro";
        }

        var dados = new HashMap<String, Object>();
        dados.put("aba", aba);
        dados.put("bairroNome", conta.neighborhoodName());
        dados.put("filtro", filtro);
        dados.put("ehPrestador", "provider".equals(conta.role()));

        if ("demandas".equals(aba)) {
            List<Demand> lista;
            if ("aceitas".equals(filtro)) {
                lista = propostas.findAcceptedByProvider(conta.id());
            } else if ("todos".equals(filtro)) {
                lista = demands.findOpenAll();
            } else {
                lista = demands.findOpenByNeighborhood(conta.neighborhoodId());
            }

            // Propostas do prestador, indexadas por demanda (badge do card)
            Map<Long, Proposal> minhas = new HashMap<>();
            for (Proposal p : propostas.findByProvider(conta.id())) {
                minhas.put(p.demandId(), p);
            }
            var cards = new ArrayList<DemandaComPropostas>();
            for (Demand demanda : lista) {
                cards.add(new DemandaComPropostas(demanda,
                    propostas.findByDemand(demanda.id()), minhas.get(demanda.id())));
            }
            dados.put("cards", cards);
            dados.put("residenteId", conta.id());
        } else {
            // Fase 4: aba Profissionais — painel de filtro (escopo + tipos de serviço)
            var todasCategorias = categorias.listAll();

            // escopo: meu bairro (padrão) ou todos os bairros
            String onde = ctx.queryParam("onde");
            if (!"todos".equals(onde)) {
                onde = "bairro";
            }

            // tipos de serviço marcados (multi-seleção, slugs validados)
            var catsSelecionadas = new ArrayList<String>();
            String[] catsPedidas = ctx.queryParams("cats").toArray(new String[0]);
            for (String pedida : catsPedidas) {
                todasCategorias.stream()
                    .map(c -> c.slug())
                    .filter(s -> s.equals(pedida))
                    .findFirst()
                    .ifPresent(catsSelecionadas::add);
            }

            List<ProviderCard> lista = "todos".equals(onde)
                ? prestadores.findAll()
                : prestadores.findByNeighborhood(conta.neighborhoodId());
            if (!catsSelecionadas.isEmpty()) {
                lista = lista.stream()
                    .filter(p -> catsSelecionadas.stream().anyMatch(p::atuaEm))
                    .toList();
            }

            // avaliações por prestador (cacheia repetidos; comentários abrem no card)
            Map<Long, List<Review>> avaliacoesPorPrestador = new HashMap<>();
            for (ProviderCard p : lista) {
                avaliacoesPorPrestador.computeIfAbsent(p.providerId(),
                    avaliacoes::findByProvider);
            }

            dados.put("prestadores", lista);
            dados.put("onde", onde);
            dados.put("catsSelecionadas", catsSelecionadas);
            dados.put("todasCategorias", todasCategorias);
            dados.put("avaliacoes", avaliacoesPorPrestador);
            dados.put("residenteId", conta.id());
        }
        ctx.render("vitrine", View.modelo(ctx, dados));
    }

    /** GET /demandas/nova: formulário de registro. */
    public void paginaNova(Context ctx) {
        renderNova(ctx, List.of());
    }

    /** POST /demandas: registra a demanda (bairro escolhível no formulário). */
    public void registrar(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        String descricao = ctx.formParam("descricao");
        String oferta = ctx.formParam("oferta");
        String telefone = ctx.formParam("telefone");
        String bairro = ctx.formParam("bairro");
        String categoriaId = ctx.formParam("categoria");

        var idsValidos = categorias.listAll().stream().map(c -> c.id()).toList();
        var erros = DemandService.validar(categoriaId, descricao, oferta,
            telefone, bairro, idsValidos);

        if (!erros.isEmpty()) {
            ctx.status(422);
            renderNovaComValores(ctx, descricao, oferta, telefone, bairro,
                categoriaId == null ? "" : categoriaId, erros);
            return;
        }

        long neighborhoodId = bairros.resolverBairro(bairro.trim());
        demands.create(conta.id(), Long.parseLong(categoriaId), neighborhoodId,
            descricao.trim(), new BigDecimal(oferta.trim().replace(",", ".")),
            telefone.trim());
        ctx.redirect("/vitrine");
    }

    /** GET /perfil/demandas: "Minhas demandas" com as propostas recebidas. */
    public void minhas(Context ctx) {
        Resident conta = ctx.attribute("residente");
        // trilhas concluídas por prestador (cacheia repetidos no Map)
        Map<Long, List<TrackConcluded>> trilhasPorPrestador = new HashMap<>();
        var comPropostas = new ArrayList<DemandaDoDono>();
        for (Demand demanda : demands.findByResident(conta.id())) {
            var enriquecidas = new ArrayList<PropostaComTrilhas>();
            for (Proposal p : propostas.findByDemand(demanda.id())) {
                List<TrackConcluded> concluidas = trilhasPorPrestador.computeIfAbsent(
                    p.providerId(), trilhas::trilhasConcluidasDe);
                enriquecidas.add(new PropostaComTrilhas(p, concluidas));
            }
            comPropostas.add(new DemandaDoDono(demanda, enriquecidas,
                avaliacoes.existePorDemanda(demanda.id())));
        }
        var dados = new HashMap<String, Object>();
        dados.put("minhasDemandas", comPropostas);
        dados.put("avaliadoAgora", "1".equals(ctx.queryParam("avaliado")));
        ctx.render("minhas-demandas", View.modelo(ctx, dados));
    }

    /** POST /perfil/demandas/status: marca como atendida manualmente (sem proposta). */
    public void marcarAtendida(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        try {
            demands.markAttended(Long.parseLong(ctx.formParam("id")), conta.id());
        } catch (NumberFormatException ignorada) {
            // id inválido: só volta para a lista
        }
        ctx.redirect("/perfil/demandas");
    }

    /** POST /perfil/demandas/excluir: exclui demanda própria (exclusão no banco). */
    public void excluir(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        try {
            demands.delete(Long.parseLong(ctx.formParam("id")), conta.id());
        } catch (NumberFormatException ignorada) {
            // id inválido: só volta para a lista
        }
        ctx.redirect("/perfil/demandas");
    }

    // ---------- Internos ----------

    private void renderNova(Context ctx, List<String> erros) {
        Resident conta = ctx.attribute("residente");
        // Telefone sugerido: o do perfil (prestador já tem)
        renderNovaComValores(ctx, "", "", conta.phone() == null ? "" : conta.phone(),
            conta.neighborhoodName() == null ? "" : conta.neighborhoodName(), "", erros);
    }

    private void renderNovaComValores(Context ctx, String descricao, String oferta,
                                      String telefone, String bairro, String categoriaSelecionada,
                                      List<String> erros) {
        Resident conta = ctx.attribute("residente");
        var dados = new HashMap<String, Object>();
        dados.put("erros", erros);
        dados.put("descricao", descricao);
        dados.put("oferta", oferta);
        dados.put("telefone", telefone);
        dados.put("bairro", bairro);
        dados.put("categoriaSelecionada", categoriaSelecionada);
        dados.put("todasCategorias", categorias.listAll());
        dados.put("bairroPerfil", conta.neighborhoodName());
        ctx.render("demanda-nova", View.modelo(ctx, dados));
    }
}
