package helion;

public final class Ansi {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String BLUE = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final boolean ENABLED = System.console() != null
            && !"dumb".equalsIgnoreCase(System.getenv("TERM"))
            && !"true".equalsIgnoreCase(System.getenv("NO_COLOR"));

    private Ansi() {
    }

    public static String bold(String text) {
        return wrap(BOLD, text);
    }

    public static String cyan(String text) {
        return wrap(CYAN, text);
    }

    public static String blue(String text) {
        return wrap(BLUE, text);
    }

    public static String green(String text) {
        return wrap(GREEN, text);
    }

    public static String yellow(String text) {
        return wrap(YELLOW, text);
    }

    public static String red(String text) {
        return wrap(RED, text);
    }

    private static String wrap(String code, String text) {
        if (!ENABLED || text == null || text.isEmpty()) {
            return text;
        }
        return code + text + RESET;
    }
}
