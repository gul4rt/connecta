package br.com.connecta.config;

/**
 * Lê a configuração do ambiente (variáveis de ambiente ou propriedades do sistema).
 * Segredos nunca ficam no código: em produção vêm de variáveis de ambiente
 * (veja .env.example). Localmente há valores padrão para facilitar o desenvolvimento.
 */
public final class AppConfig {

    private AppConfig() {
        // classe de utilidades: não deve ser instanciada
    }

    /** Busca em variável de ambiente e, se não achar, em propriedade do sistema (-D). */
    public static String get(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /** "dev" por padrão; em "prod" a página /design fica desligada. */
    public static boolean isDev() {
        return !"prod".equalsIgnoreCase(get("CONNECTA_ENV", "dev"));
    }

    public static int port() {
        return Integer.parseInt(get("PORT", "7000"));
    }

    public static String dbUrl() {
        return get("DB_URL", "jdbc:postgresql://localhost:5432/connecta");
    }

    public static String dbUser() {
        return get("DB_USER", "connecta");
    }

    public static String dbPassword() {
        return get("DB_PASSWORD", "connecta");
    }

    /** Limiares da lacuna na home ("Muita procura" / "Procura média"). */
    public static String gapMuita() {
        return get("CONNECTA_GAP_MUITA", "5");
    }

    public static String gapMedia() {
        return get("CONNECTA_GAP_MEDIA", "2");
    }
}
