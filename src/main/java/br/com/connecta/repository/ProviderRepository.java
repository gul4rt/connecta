package br.com.connecta.repository;

import br.com.connecta.domain.ProviderCard;

import java.util.List;

/**
 * Leitura dos prestadores para a aba "Profissionais" da vitrine (Fase 4).
 * Prestador = resident com role 'provider' e perfil completo; as categorias
 * de atuação vêm das habilidades "sei fazer" declaradas no perfil.
 */
public interface ProviderRepository {

    /** Prestadores de um bairro (ordenados: selo e trabalhos primeiro). */
    List<ProviderCard> findByNeighborhood(long neighborhoodId);

    /** Todos os prestadores com perfil completo (para "todos os bairros", se precisar). */
    List<ProviderCard> findAll();

    /** Um prestador pelo id (para validar o pedido direto). */
    java.util.Optional<ProviderCard> findById(long providerId);
}
