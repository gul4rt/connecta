package br.com.connecta.repository;

import br.com.connecta.domain.ServiceCategory;

import java.util.List;

/** Contrato de acesso às categorias de serviço (implementação JDBC ao lado). */
public interface ServiceCategoryRepository {

    /** Lista todas as categorias em ordem fixa de exibição. */
    List<ServiceCategory> listAll();
}
