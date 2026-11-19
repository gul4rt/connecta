package br.com.connecta.repository;

import br.com.connecta.domain.Proposal;

import java.util.List;
import java.util.Optional;

/** Contrato de acesso às propostas dos prestadores. */
public interface ProposalRepository {

    void create(long demandId, long providerId, String message);

    /** Propostas recebidas por uma demanda (para o dono, em Minhas demandas). */
    List<Proposal> findByDemand(long demandId);

    /** Propostas que o prestador já enviou, com o status de cada uma. */
    List<Proposal> findByProvider(long providerId);

    /** Proposta do prestador para uma demanda específica (evita duplicadas). */
    Optional<Proposal> findOf(long demandId, long providerId);

    /** Busca uma proposta pelo id (para o fluxo de aceitar). */
    Optional<Proposal> findById(long id);

    boolean existsFor(long demandId, long providerId);

    /** Demanda com proposta aceita do prestador (filtro "Propostas aceitas"). */
    List<br.com.connecta.domain.Demand> findAcceptedByProvider(long providerId);

    /**
     * Aceita uma proposta: marca as outras da mesma demanda como rejeitadas,
     * fecha a demanda como 'attended' e sobe o contador do prestador —
     * tudo em uma transação.
     */
    void aceitar(long proposalId);
}
