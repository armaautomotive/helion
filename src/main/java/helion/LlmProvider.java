package helion;

import java.io.IOException;
import java.util.List;

public interface LlmProvider {
    String name();

    String chat(List<LlmMessage> messages) throws IOException, InterruptedException;

    default String complete(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        return chat(List.of(
                new LlmMessage("system", systemPrompt),
                new LlmMessage("user", userPrompt)));
    }
}
