package br.com.connecta.repository;

import br.com.connecta.domain.Resident;

import java.util.Optional;

/** Contrato de acesso aos moradores com conta. */
public interface ResidentRepository {

    /** Cria a conta com hash de senha + hash do código de recuperação; devolve o id. */
    long create(String username, String passwordHash, String role, String recoveryCodeHash);

    Optional<Resident> findByUsername(String username);

    /** Só o hash da senha, para comparação no login. Nunca devolver ao usuário. */
    Optional<String> findPasswordHashByUsername(String username);

    /** Busca por id, já com o nome do bairro (join). */
    Optional<Resident> findById(long id);

    /** Salva os dados do perfil e marca como completo. */
    void updateProfile(long id, String displayName, String phone,
                       Long neighborhoodId, boolean profileCompleted);

    /** Troca a senha e o código de recuperação (ambos com hash). */
    void updatePasswordAndRecoveryCode(long id, String passwordHash, String recoveryCodeHash);

    /** Hash do código de recuperação, para comparar no fluxo "esqueci a senha". */
    Optional<String> findRecoveryCodeHashByUsername(String username);

    /** Exclui a conta (LGPD: a qualquer momento). Sessões caem junto (ON DELETE CASCADE). */
    void delete(long id);
}
