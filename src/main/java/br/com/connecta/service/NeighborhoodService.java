package br.com.connecta.service;

import br.com.connecta.repository.NeighborhoodRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bairros: busca de nomes de bairro de São Paulo/SP e cadastro no banco.
 *
 * API usada: Nominatim (OpenStreetMap), porque a busca de endereço do ViaCEP
 * só pesquisa LOGRADOURO (nome de rua) e não encontrava bairros (ver
 * docs/DECISOES.md, 2026-11-19). O Nominatim aceita pesquisa livre e devolve
 * nos "address details" o campo suburb/bairro. A política de uso da API pede
 * um User-Agent identificando a aplicação e no máximo 1 requisição por
 * segundo — nos nossos volumes de protótipo local, isso não é problema.
 */
public class NeighborhoodService {

    private static final int LIMITE_RESULTADOS = 8;
    private static final int MINIMO_CARACTERES = 3;

    private final NeighborhoodRepository neighborhoods;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ObjectMapper json = new ObjectMapper();

    public NeighborhoodService(NeighborhoodRepository neighborhoods) {
        this.neighborhoods = neighborhoods;
    }

    /** Busca bairros pelo nome (São Paulo/SP) e devolve nomes distintos. */
    public List<String> buscarBairros(String termo) {
        if (termo == null || termo.trim().length() < MINIMO_CARACTERES) {
            return List.of();
        }
        String consulta = URLEncoder.encode(termo.trim() + ", São Paulo, Brasil", StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?q=" + consulta
            + "&format=json&addressdetails=1&limit=10";
        try {
            var resposta = http.send(
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "connecta-prototipo-academico")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                return List.of();
            }
            // Guarda só bairros da cidade de São Paulo, sem repetição
            Set<String> distintos = new LinkedHashSet<>();
            for (JsonNode lugar : json.readTree(resposta.body())) {
                JsonNode endereco = lugar.get("address");
                if (endereco == null) {
                    continue;
                }
                JsonNode cidade = endereco.get("city");
                if (cidade == null || !cidade.asText().equalsIgnoreCase("São Paulo")) {
                    continue;
                }
                JsonNode bairro = endereco.get("suburb");
                if (bairro == null || bairro.asText().isBlank()) {
                    bairro = endereco.get("neighbourhood");
                }
                if (bairro != null && !bairro.asText().isBlank()) {
                    distintos.add(bairro.asText());
                    if (distintos.size() >= LIMITE_RESULTADOS) {
                        break;
                    }
                }
            }
            return new ArrayList<>(distintos);
        } catch (Exception e) {
            // API fora do ar não pode derrubar a página: devolve lista vazia
            System.err.println("Aviso: falha ao consultar Nominatim: " + e.getMessage());
            return List.of();
        }
    }

    /** Devolve o id do bairro no banco, criando o bairro se ainda não existir. */
    public long resolverBairro(String nome) {
        return neighborhoods.findIdByNameIgnoreCase(nome)
            .orElseGet(() -> neighborhoods.create(nome));
    }
}
