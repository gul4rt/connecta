package br.com.connecta.repository;

import br.com.connecta.domain.Track;
import br.com.connecta.domain.TrackConcluded;
import br.com.connecta.domain.TrackDetail;
import br.com.connecta.domain.TrackProgress;

import java.util.List;
import java.util.Optional;

/**
 * Operações de banco para as trilhas (timeline de cursos, Fase 3).
 * Os métodos que guardam progresso recebem o id do morador; os de
 * navegação aceitam null (visitante logado não vê progresso nenhum).
 */
public interface TrackRepository {

    /** Todas as trilhas com o progresso do morador (residentId nulo = sem progresso). */
    List<TrackProgress> listAll(Long residentId);

    /** Uma trilha com as etapas em ordem e o progresso do morador. */
    Optional<TrackDetail> findDetail(String slug, Long residentId);

    /** Cria a inscrição (se ainda não existe). true = inscrição nova. */
    boolean inscrever(long residentId, String slug);

    /** Marca uma etapa como concluída (só se a etapa pertencer à trilha). */
    boolean marcarEtapa(long residentId, String slug, long stepId);

    /**
     * Conclui a trilha com a autodeclaração (termo): marca a inscrição como
     * completed, registra data/hora e concede o selo verificado.
     * Só funciona se TODAS as etapas estiverem concluídas (checado no banco).
     */
    boolean concluir(long residentId, String slug);

    /** Quantas trilhas a pessoa já concluiu (base do selo no perfil). */
    long countConcluidas(long residentId);

    /** A pessoa tem o selo verificado? */
    boolean temSelo(long residentId);

    /** Busca uma trilha pelo slug (para validar rotas). */
    Optional<Track> findBySlug(String slug);

    /** Trilhas concluídas por uma pessoa, mais recentes primeiro (mostra no selo). */
    List<TrackConcluded> trilhasConcluidasDe(long residentId);
}
