package br.com.connecta.repository;

import br.com.connecta.domain.Skill;

import java.util.List;

/** Contrato de acesso às habilidades dos moradores. */
public interface SkillRepository {

    List<Skill> findByResident(long residentId);

    void add(long residentId, long categoryId, String kind, String name);

    /** Exclui uma habilidade — só se pertencer ao morador logado. */
    boolean delete(long id, long residentId);
}
