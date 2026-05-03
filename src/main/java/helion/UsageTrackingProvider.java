package helion;

import java.io.IOException;
import java.util.List;

public final class UsageTrackingProvider implements LlmProvider {
    private final LlmProvider delegate;
    private final UsageTracker tracker;

    public UsageTrackingProvider(LlmProvider delegate, UsageTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }

    @Override
    public LlmResult chatResult(List<LlmMessage> messages) throws IOException, InterruptedException {
        LlmResult result = delegate.chatResult(messages);
        tracker.record(result.usage());
        return result;
    }
}
