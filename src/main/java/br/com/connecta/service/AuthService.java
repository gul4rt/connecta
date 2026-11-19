package br.com.connecta.service;

import br.com.connecta.domain.Resident;
import br.com.connecta.repository.ResidentRepository;
import br.com.connecta.repository.SessionRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Regras de cadastro, login e sessão.
 *
 * Segurança:
 * - senha SEMPRE com hash BCrypt (nunca texto puro, nunca em log);
 * - sessão com token aleatório de 32 bytes em cookie HttpOnly/SameSite=Lax;
 * - CSRF com cookie "double-submit" (ver web/Csrf.java);
 * - limite de tentativas de login (LoginAttemptLimiter).
 */
public class AuthService {

    public static final String COOKIE_SESSAO = "connecta_sessao";
    public static final int DIAS_SESSAO = 30;
    private static final int SEGURANCA_BCRYPT = 10;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Token da sessão recém-criada e a qual conta pertence. */
    public record SessaoCriada(String token, long residentId) {
    }

    /** Resultado de um cadastro ou troca de senha: sessão + código em claro (mostrado UMA vez). */
    public record ContaCriada(SessaoCriada sessao, String codigoRecuperacao) {
    }

    private final ResidentRepository residents;
    private final SessionRepository sessions;
    private final LoginAttemptLimiter limiteDeTentativas = new LoginAttemptLimiter();

    public AuthService(ResidentRepository residents, SessionRepository sessions) {
        this.residents = residents;
        this.sessions = sessions;
    }

    // ---------- Validações (sem banco: fáceis de testar) ----------

    /** Valida o cadastro e devolve erros em português para mostrar na tela. */
    public static List<String> validarCadastro(String username, String senha,
                                               String confirmacao, String role, boolean consentiu) {
        var erros = new ArrayList<String>();
        if (!usernameValido(username)) {
            erros.add("O username precisa ter de 3 a 40 caracteres (letras, números, ponto ou _).");
        }
        if (senha == null || senha.length() < 8) {
            erros.add("A senha precisa ter pelo menos 8 caracteres.");
        }
        if (senha != null && !senha.equals(confirmacao)) {
            erros.add("A confirmação não é igual à senha.");
        }
        if (!"provider".equals(role) && !"seeker".equals(role)) {
            erros.add("Escolha se você quer prestar ou contratar serviços.");
        }
        if (!consentiu) {
            erros.add("Precisamos do seu consentimento para usar seus dados (LGPD).");
        }
        return erros;
    }

    /** Username: 3-40 caracteres, letras, números, ponto ou underline. */
    public static boolean usernameValido(String username) {
        return username != null && username.matches("[A-Za-z0-9._]{3,40}");
    }

    // ---------- Cadastro, login e senha ----------

    /** Cria a conta e já devolve a sessão e o código de recuperação (mostrar UMA vez). */
    public ContaCriada registrar(String username, String senha, String confirmacao,
                                 String role, boolean consentiu) {
        var erros = validarCadastro(username, senha, confirmacao, role, consentiu);
        if (erros.isEmpty() && residents.findByUsername(username).isPresent()) {
            erros.add("Esse username já está em uso. Escolha outro.");
        }
        if (!erros.isEmpty()) {
            throw new ExcecaoValidacao(erros);
        }
        String hash = BCrypt.hashpw(senha, BCrypt.gensalt(SEGURANCA_BCRYPT));
        String codigo = gerarCodigoRecuperacao();
        // Guardamos o hash do código NORMALIZADO (mesma forma usada na comparação)
        String hashDoCodigo = BCrypt.hashpw(normalizarCodigo(codigo), BCrypt.gensalt(SEGURANCA_BCRYPT));
        long id = residents.create(username.trim(), hash, role, hashDoCodigo);
        return new ContaCriada(criarSessao(id), codigo);
    }

    /** Autentica e devolve a sessão. Lança ExcecaoValidacao com mensagem clara. */
    public SessaoCriada entrar(String username, String senha) {
        String usuario = (username == null ? "" : username).trim();
        if (limiteDeTentativas.bloqueado(usuario)) {
            throw new ExcecaoValidacao(List.of(
                "Muitas tentativas. Tente de novo em " + limiteDeTentativas.segundosRestantes(usuario) + " segundos."));
        }
        // Mensagem única para usuário inexistente e senha errada:
        // não revela quais usernames existem na plataforma.
        var hash = residents.findPasswordHashByUsername(usuario);
        boolean senhaOk = hash.isPresent() && BCrypt.checkpw(senha == null ? "" : senha, hash.get());
        if (!senhaOk) {
            limiteDeTentativas.registrarFalha(usuario);
            throw new ExcecaoValidacao(List.of("Username ou senha incorretos."));
        }
        limiteDeTentativas.limpar(usuario);
        long id = residents.findByUsername(usuario).orElseThrow().id();
        return criarSessao(id);
    }

    public void sair(String token) {
        sessions.delete(token);
    }

    /** Código de recuperação (sem e-mail no projeto): username + código + nova senha. */
    public String redefinirSenha(String username, String codigo, String novaSenha, String confirmacao) {
        String usuario = (username == null ? "" : username).trim();
        var hashDoCodigo = residents.findRecoveryCodeHashByUsername(usuario);
        boolean codigoOk = hashDoCodigo.isPresent() && BCrypt.checkpw(
            normalizarCodigo(codigo), hashDoCodigo.get());
        if (!codigoOk) {
            throw new ExcecaoValidacao(List.of("Username ou código de recuperação incorretos."));
        }
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new ExcecaoValidacao(List.of("A nova senha precisa ter pelo menos 8 caracteres."));
        }
        if (!novaSenha.equals(confirmacao)) {
            throw new ExcecaoValidacao(List.of("A confirmação não é igual à nova senha."));
        }
        return trocarSenhaInterno(residents.findByUsername(usuario).orElseThrow().id(), novaSenha);
    }

    /** Troca de senha logado: exige a senha atual e gera código novo. */
    public String trocarSenha(long residentId, String senhaAtual, String novaSenha, String confirmacao) {
        var conta = residents.findById(residentId);
        var hash = conta.isPresent()
            ? residents.findPasswordHashByUsername(conta.get().username())
            : Optional.<String>empty();
        boolean atualOk = hash.isPresent() && BCrypt.checkpw(
            senhaAtual == null ? "" : senhaAtual, hash.get());
        if (!atualOk) {
            throw new ExcecaoValidacao(List.of("A senha atual está incorreta."));
        }
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new ExcecaoValidacao(List.of("A nova senha precisa ter pelo menos 8 caracteres."));
        }
        if (!novaSenha.equals(confirmacao)) {
            throw new ExcecaoValidacao(List.of("A confirmação não é igual à nova senha."));
        }
        return trocarSenhaInterno(residentId, novaSenha);
    }

    public Optional<SessionRepository.Session> sessaoValida(String token) {
        return token == null ? Optional.empty() : sessions.findValid(token);
    }

    public void encerrarSessoesDe(long residentId) {
        sessions.deleteByResident(residentId);
    }

    // ---------- Internos ----------

    /** Grava a nova senha e um CÓDIGO NOVO (a cada troca o anterior deixa de valer). */
    private String trocarSenhaInterno(long residentId, String novaSenha) {
        String hashSenha = BCrypt.hashpw(novaSenha, BCrypt.gensalt(SEGURANCA_BCRYPT));
        String codigoNovo = gerarCodigoRecuperacao();
        String hashCodigo = BCrypt.hashpw(normalizarCodigo(codigoNovo), BCrypt.gensalt(SEGURANCA_BCRYPT));
        residents.updatePasswordAndRecoveryCode(residentId, hashSenha, hashCodigo);
        return codigoNovo;
    }

    /**
     * Código de recuperação: 8 caracteres em blocos de 4 (XXXX-XXXX), sem
     * caracteres que se confundem (I, 1, O, 0) para facilitar na hora de digitar.
     */
    public static String gerarCodigoRecuperacao() {
        final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Ignora maiúsculas/minúsculas, espaços e traços ao conferir o código. */
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.toUpperCase().replaceAll("[\\s-]", "");
    }

    private SessaoCriada criarSessao(long residentId) {
        String token = novoToken();
        sessions.create(token, residentId);
        return new SessaoCriada(token, residentId);
    }

    /** Token aleatório de 32 bytes, seguro para URL. */
    public static String novoToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Erros de validação/negócio que voltam para a tela em português. */
    public static class ExcecaoValidacao extends RuntimeException {
        public final List<String> erros;

        public ExcecaoValidacao(List<String> erros) {
            super(String.join(" ", erros));
            this.erros = erros;
        }
    }
}
