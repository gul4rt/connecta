package br.com.connecta.repository;

import java.util.Optional;

/** Contrato de acesso aos bairros. */
public interface NeighborhoodRepository {

    Optional<Long> findIdByNameIgnoreCase(String name);

    /** Cria o bairro e devolve o id gerado. */
    long create(String name);
}
