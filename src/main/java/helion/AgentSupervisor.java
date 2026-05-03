package helion;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class AgentSupervisor {
    private static final Duration LOOP_SLEEP = Duration.ofSeconds(10);

    private final HelionConfig config;
    private final BusinessAgent agent;
    private final AgentRegistry agentRegistry;
    private final AgentStatusStore statusStore;
    private final AgentRuntimeStore runtimeStore;
    private final String onlyAgentId;

    public AgentSupervisor(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry, String onlyAgentId) {
        this.config = config;
        this.agent = agent;
        this.agentRegistry = agentRegistry;
        this.statusStore = new AgentStatusStore(config);
        this.runtimeStore = new AgentRuntimeStore();
        this.onlyAgentId = onlyAgentId == null ? "" : onlyAgentId.trim();
    }

    public void runLoop() throws IOException, InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            runOneCycle();
            Thread.sleep(LOOP_SLEEP.toMillis());
        }
    }

    private void runOneCycle() throws IOException, InterruptedException {
        List<String> agentIds = agentRegistry.listAgentIds();
        for (String agentId : agentIds) {
            if (!onlyAgentId.isBlank() && !onlyAgentId.equals(agentId)) {
                continue;
            }
            AgentProfile profile = agentRegistry.load(agentId);
            if (profile == null) {
                continue;
            }
            AgentStatus status = statusStore.read(profile);
            if (!status.isRunnable()) {
                continue;
            }
            if (!isDue(status, LocalDateTime.now())) {
                continue;
            }
            LocalDateTime startedAt = LocalDateTime.now();
            String task = taskNameFor(agentId);
            AgentRuntime runtime = runtimeStore.read(profile, status.executionState());
            runtimeStore.write(profile, runtime.start(status.executionState(), task, startedAt));
            try {
                String result = runAgentCycle(agentId);
                if (result == null || result.isBlank()) {
                    continue;
                }
                statusStore.markRun(profile, LocalDateTime.now());
                AgentRuntime updated = runtime.success(
                        status.executionState(),
                        task,
                        startedAt,
                        LocalDateTime.now(),
                        summarize(result),
                        outputPathFor(agentId));
                runtimeStore.write(profile, updated);
                System.out.println(Ansi.green("[" + agentId + "] " + result));
            } catch (Exception ex) {
                AgentRuntime failed = runtime.failure(
                        status.executionState(),
                        task,
                        startedAt,
                        LocalDateTime.now(),
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                runtimeStore.write(profile, failed);
                System.err.println(Ansi.red("[" + agentId + "] " + failed.lastErrorMessage()));
            }
        }
    }

    private boolean isDue(AgentStatus status, LocalDateTime now) {
        if (status.lastRun() == null) {
            return true;
        }
        return !status.lastRun().plusSeconds(status.runIntervalSeconds()).isAfter(now);
    }

    private String runAgentCycle(String agentId) throws IOException, InterruptedException {
        if ("prospecting".equals(agentId)) {
            return agent.collectQueuedProspects(agentId, 5);
        }
        return "";
    }

    private String taskNameFor(String agentId) {
        if ("prospecting".equals(agentId)) {
            return "queued prospect search";
        }
        return "autonomous cycle";
    }

    private String outputPathFor(String agentId) {
        if ("prospecting".equals(agentId)) {
            return "agents/" + agentId + "/workspace/prospects.csv";
        }
        return "";
    }

    private String summarize(String result) {
        String text = result == null ? "" : result.trim();
        return text.length() <= 240 ? text : text.substring(0, 237) + "...";
    }

    public String describe() {
        String scope = onlyAgentId.isBlank() ? "all agents" : onlyAgentId;
        return "Supervisor running for " + scope + " with poll interval " + LOOP_SLEEP.toSeconds() + " seconds.";
    }
}
