package br.com.connecta.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas de login: 5 senhas erradas bloqueiam o username
 * por 15 minutos. Guardado em memória (projeto local; em produção seria
 * no banco ou em cache compartilhado).
 */
public class LoginAttemptLimiter {

    static final int MAX_TENTATIVAS = 5;
    static final long BLOQUEIO_MILLIS = 15 * 60 * 1000L;

    private record Registro(int falhas, long bloqueadoAte) {
    }

    private final Map<String, Registro> tentativas = new ConcurrentHashMap<>();

    /** O username está bloqueado neste momento? */
    public boolean bloqueado(String username) {
        Registro r = tentativas.get(chave(username));
        return r != null && r.bloqueadoAte() > System.currentTimeMillis();
    }

    /** Segundos restantes de bloqueio (para a mensagem de erro). */
    public long segundosRestantes(String username) {
        Registro r = tentativas.get(chave(username));
        if (r == null) {
            return 0;
        }
        return Math.max(0, (r.bloqueadoAte() - System.currentTimeMillis()) / 1000);
    }

    /** Registra uma senha errada; na 5ª, bloqueia. */
    public void registrarFalha(String username) {
        tentativas.compute(chave(username), (k, r) -> {
            long agora = System.currentTimeMillis();
            int falhas;
            if (r == null) {
                falhas = 1;
            } else if (r.bloqueadoAte() > 0 && r.bloqueadoAte() < agora) {
                falhas = 1; // bloqueio anterior já expirou: recomeça a contagem
            } else {
                falhas = r.falhas() + 1;
            }
            long bloqueadoAte = falhas >= MAX_TENTATIVAS ? agora + BLOQUEIO_MILLIS : 0L;
            return new Registro(falhas, bloqueadoAte);
        });
    }

    /** Login com sucesso zera a contagem. */
    public void limpar(String username) {
        tentativas.remove(chave(username));
    }

    private static String chave(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
