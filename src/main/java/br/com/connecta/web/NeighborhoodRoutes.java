package br.com.connecta.web;

import br.com.connecta.service.NeighborhoodService;
import io.javalin.http.Context;

/**
 * GET /bairros?termo=jardim — devolve nomes de bairros de São Paulo/SP
 * encontrados na ViaCEP, em JSON, para a search bar do perfil.
 * Sem JS, o campo continua funcionando: a pessoa digita o nome e salva.
 */
public class NeighborhoodRoutes {

    private final NeighborhoodService bairros;

    public NeighborhoodRoutes(NeighborhoodService bairros) {
        this.bairros = bairros;
    }

    public void buscar(Context ctx) {
        ctx.json(bairros.buscarBairros(ctx.queryParam("termo")));
    }
}
