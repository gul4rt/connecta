package br.com.connecta.service;

import br.com.connecta.domain.TrackStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Regras das trilhas (Fase 3), sem dependência de banco ou web:
 * - validar a conclusão com autodeclaração (termo);
 * - calcular o estado de cada etapa da timeline (done / current / pending)
 *   para o stepper da tela.
 * Funções puras: fáceis de testar e de explicar no vídeo técnico.
 */
public final class TrackService {

    private TrackService() {
    }

    /**
     * Valida a autodeclaração de conclusão. Regras:
     * 1) todas as etapas precisam estar concluídas;
     * 2) a pessoa precisa marcar o termo "confirmo que realizei os treinamentos".
     * Devolve a lista de erros em português (vazia = pode concluir).
     */
    public static List<String> validarConclusao(long etapasTotais, long etapasConcluidas,
                                                boolean confirmouTermo) {
        var erros = new ArrayList<String>();
        if (etapasTotais == 0 || etapasConcluidas < etapasTotais) {
            erros.add("Falta concluir etapas da trilha antes de receber o selo ("
                + etapasConcluidas + " de " + etapasTotais + " concluídas).");
        }
        if (!confirmouTermo) {
            erros.add("Marque a confirmação para declarar que realizou os treinamentos.");
        }
        return erros;
    }

    /**
     * Estado de cada etapa para o stepper: done (feita), current (a próxima
     * a fazer — a primeira ainda não feita) e pending (as seguintes).
     * Devolve mapas {etapa, estado} na mesma ordem da lista de entrada.
     */
    public static List<Map<String, Object>> estadosDasEtapas(List<TrackStep> etapas,
                                                             boolean trilhaConcluida) {
        var estados = new ArrayList<Map<String, Object>>();
        boolean achouAtual = trilhaConcluida; // trilha concluída: nenhuma é "atual"
        for (TrackStep etapa : etapas) {
            String estado;
            if (etapa.done()) {
                estado = "done";
            } else if (!achouAtual) {
                estado = "current";
                achouAtual = true;
            } else {
                estado = "pending";
            }
            estados.add(Map.of("etapa", etapa, "estado", estado));
        }
        return estados;
    }
}
