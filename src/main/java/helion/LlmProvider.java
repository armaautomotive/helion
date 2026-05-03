package helion;

import java.io.IOException;
import java.util.List;

public interface LlmProvider {
    String name();

    String modelName();

    LlmResult chatResult(List<LlmMessage> messages) throws IOException, InterruptedException;

    default String chat(List<LlmMessage> messages) throws IOException, InterruptedException {
        return chatResult(messages).content();
    }

    default String complete(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        return chatResult(List.of(
                new LlmMessage("system", systemPrompt),
                new LlmMessage("user", userPrompt))).content();
    }
}
