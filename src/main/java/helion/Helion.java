package helion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Helion {
    private Helion() {
    }

    public static void main(String[] args) {
        try {
            AgentMode mode = AgentMode.fromCliArg(args.length > 0 ? args[0] : null);
            String prompt = extractPrompt(args, mode);
            if (prompt.isBlank()) {
                printUsage();
                return;
            }

            HelionConfig config = HelionConfig.load();
            BusinessAgent agent = ProviderFactory.createAgent(config);
            String response = agent.respond(new AgentRequest(mode, prompt));
            System.out.println(response);
        } catch (Exception ex) {
            System.err.println("helion error: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String extractPrompt(String[] args, AgentMode mode) throws IOException {
        int promptStart = AgentMode.looksLikeMode(args.length > 0 ? args[0] : null) ? 1 : 0;
        if (args.length > promptStart) {
            return String.join(" ", java.util.Arrays.copyOfRange(args, promptStart, args.length)).trim();
        }
        return readStdIn().trim();
    }

    private static String readStdIn() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private static void printUsage() {
        System.out.println("Usage: helion [plan|analyze|email|general] <prompt>");
        System.out.println("Example: helion plan Launch a niche B2B software consultancy");
        System.out.println("Config via env: HELION_MANAGER_PROVIDER, HELION_WORKER_PROVIDER, HELION_ENABLE_BROWSER, HELION_ENABLE_MEMORY, HELION_LLAMACPP_URL");
    }
}
