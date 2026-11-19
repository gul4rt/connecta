package br.com.connecta.repository;

import br.com.connecta.domain.Demand;

import java.util.List;

/** Contrato de acesso às demandas. */
public interface DemandRepository {

    void create(long residentId, long categoryId, long neighborhoodId,
                String description, java.math.BigDecimal offerAmount, String contactPhone);

    /** Demandas abertas de um bairro, mais recentes primeiro. */
    List<Demand> findOpenByNeighborhood(long neighborhoodId);

    /** Demandas abertas de todos os bairros (filtro do prestador). */
    List<Demand> findOpenAll();

    /** Busca uma demanda pelo id (para a tela de proposta). */
    java.util.Optional<Demand> findById(long id);

    /** Todas as demandas de um morador (para "Minhas demandas"). */
    List<Demand> findByResident(long residentId);

    /** Quantidade de demandas abertas por categoria, num bairro (lacuna). */
    List<CategoryCount> countOpenByCategory(long neighborhoodId);

    /** Marca como atendida — só se for do morador logado. Devolve true se mudou. */
    boolean markAttended(long id, long residentId);

    /** Exclui uma demanda aberta — só se for do morador logado. */
    boolean delete(long id, long residentId);

    /** Contagem por categoria para a lacuna do bairro. */
    record CategoryCount(long categoryId, String categoryName, long total) {
    }
}
