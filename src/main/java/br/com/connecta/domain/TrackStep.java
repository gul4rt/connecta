package br.com.connecta.domain;

import java.util.List;

/**
 * Uma etapa da timeline da trilha: uma playlist do YouTube.
 * done indica se ESTA pessoa já concluiu a etapa (progresso salvo no banco).
 */
public record TrackStep(
    Long id,
    int position,
    String title,
    List<String> playlistUrls,
    boolean done
) {
}
