package br.com.connecta.service;

import br.com.connecta.config.AppConfig;
import br.com.connecta.domain.GapLevel;
import br.com.connecta.domain.ServiceCategory;
import br.com.connecta.repository.DemandRepository;
import br.com.connecta.repository.ServiceCategoryRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prepara a seção "O bairro está procurando" da home.
 *
 * Com morador logado e bairro definido: dados REAIS — demandas abertas do
 * bairro por categoria, contadas pelo banco (GROUP BY). A lacuna completa
 * (demandas - prestadores ativos) entra quando a vitrine ganhar as
 * categorias dos prestadores (Fase 4); até lá a lacuna é o número de
 * demandas abertas.
 *
 * Sem login (ou sem bairro): dados de EXEMPLO, marcados como tal na tela.
 */
public class HomeService {

    /** Estrutura simples que a tela consome: categoria + nível de procura. */
    public record CategoryDemand(String categoryName, GapLevel level, int gap) {
        public String demandLabel() {
            return level.label();
        }
    }

    // Limiares configuráveis (AppConfig lê env/propriedade com padrão)
    private static final int LIMIAR_MUITA =
        Integer.parseInt(AppConfig.get("CONNECTA_GAP_MUITA", "5"));
    private static final int LIMIAR_MEDIA =
        Integer.parseInt(AppConfig.get("CONNECTA_GAP_MEDIA", "2"));

    // Lacunas fictícias, só para a home sem login demonstrar o visual
    private static final Map<String, Integer> EXAMPLE_GAPS = Map.of(
        "Manutenção e reparos", 6,
        "Cuidados", 3,
        "Tecnologia", 4,
        "Gastronomia", 1,
        "Outros", 0);

    private final ServiceCategoryRepository categories;
    private final DemandRepository demands;

    public HomeService(ServiceCategoryRepository categories, DemandRepository demands) {
        this.categories = categories;
        this.demands = demands;
    }

    /**
     * As 3 categorias com maior lacuna do bairro. Com bairro, usa os dados
     * reais do banco; sem bairro, os de exemplo. Ordena do maior para o
     * menor (List + Comparator, estruturas explícitas como o projeto pede).
     */
    public List<CategoryDemand> topDemands(Long neighborhoodId) {
        var lista = new ArrayList<CategoryDemand>();
        if (neighborhoodId == null) {
            for (ServiceCategory category : categories.listAll()) {
                int gap = EXAMPLE_GAPS.getOrDefault(category.name(), 0);
                lista.add(new CategoryDemand(category.name(),
                    GapLevel.fromGap(gap, LIMIAR_MUITA, LIMIAR_MEDIA), gap));
            }
            lista.sort(Comparator.comparingInt(CategoryDemand::gap).reversed());
        } else {
            // Índice por id para achar a contagem real de cada categoria
            Map<Long, Long> contagens = new HashMap<>();
            for (DemandRepository.CategoryCount c : demands.countOpenByCategory(neighborhoodId)) {
                contagens.put(c.categoryId(), c.total());
            }
            for (ServiceCategory category : categories.listAll()) {
                int gap = contagens.getOrDefault(category.id(), 0L).intValue();
                lista.add(new CategoryDemand(category.name(),
                    GapLevel.fromGap(gap, LIMIAR_MUITA, LIMIAR_MEDIA), gap));
            }
            // Mais demanda aberta primeiro
            lista.sort(Comparator.comparingInt(CategoryDemand::gap).reversed());
        }
        return lista.stream().limit(3).toList();
    }
}
