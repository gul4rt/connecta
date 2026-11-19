package br.com.connecta.repository;

import java.util.Optional;

/** Sessões de login persistidas no banco. */
public interface SessionRepository {

    record Session(String token, long residentId) {
    }

    void create(String token, long residentId);

    /** Devolve a sessão só se existir e não estiver expirada. */
    Optional<Session> findValid(String token);

    void delete(String token);

    void deleteByResident(long residentId);
}
