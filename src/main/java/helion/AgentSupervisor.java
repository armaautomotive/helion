package helion;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class AgentSupervisor {
    private static final Duration LOOP_SLEEP = Duration.ofSeconds(10);
    private static final int PROSPECTING_QUEUE_ITEMS_PER_CYCLE = 3;
    private static final int SOCIAL_QUEUE_ITEMS_PER_CYCLE = 3;

    private final HelionConfig config;
    private final BusinessAgent agent;
    private final AgentRegistry agentRegistry;
    private final AgentStatusStore statusStore;
    private final AgentRuntimeStore runtimeStore;
    private final AgentActivityStore activityStore;
    private final ProspectSearchQueueStore prospectSearchQueueStore;
    private final SocialSearchQueueStore socialSearchQueueStore;
    private final String onlyAgentId;

    public AgentSupervisor(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry, String onlyAgentId) {
        this.config = config;
        this.agent = agent;
        this.agentRegistry = agentRegistry;
        this.statusStore = new AgentStatusStore(config);
        this.runtimeStore = new AgentRuntimeStore();
        this.activityStore = new AgentActivityStore();
        this.prospectSearchQueueStore = new ProspectSearchQueueStore(agentRegistry);
        this.socialSearchQueueStore = new SocialSearchQueueStore(agentRegistry);
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
            AgentRuntime runtime = runtimeStore.read(profile, status.executionTarget());
            runtime = recoverStaleRun(profile, agentId, status, runtime, LocalDateTime.now());
            if (!isDue(status, runtime, LocalDateTime.now())) {
                continue;
            }
            LocalDateTime startedAt = LocalDateTime.now();
            String task = taskNameFor(agentId);
            ProspectSearchQueueItem queueItem = queueItemFor(agentId);
            SocialSearchQueueItem socialQueueItem = socialQueueItemFor(agentId);
            if ("prospecting".equals(agentId) && queueItem == null) {
                continue;
            }
        if ("social-media".equals(agentId) && socialQueueItem == null) {
                continue;
            }
            runtimeStore.write(profile, runtime.start(status.executionTarget(), task, startedAt));
            try {
                CycleResult cycle = runAgentCycle(agentId, queueItem, socialQueueItem, status);
                if (cycle == null || cycle.message().isBlank()) {
                    continue;
                }
                statusStore.markRun(profile, LocalDateTime.now());
                AgentRuntime updated = runtime.success(
                        status.executionTarget(),
                        task,
                        startedAt,
                        LocalDateTime.now(),
                        summarize(cycle.message()),
                        cycle.outputPath());
                runtimeStore.write(profile, updated);
                activityStore.append(profile, successEntry(agentId, task, queueItem, socialQueueItem, cycle, startedAt));
            } catch (Exception ex) {
                LocalDateTime finishedAt = LocalDateTime.now();
                AgentRuntime failed = runtime.failure(
                        status.executionTarget(),
                        task,
                        startedAt,
                        finishedAt,
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                runtimeStore.write(profile, failed);
                activityStore.append(profile, failureEntry(task, queueItem, socialQueueItem, startedAt, failed.lastErrorMessage()));
                long cooldownSeconds = cooldownSeconds(status, failed);
                LocalDateTime retryAt = finishedAt.plusSeconds(Math.max(5, cooldownSeconds));
                activityStore.append(profile, blockedEntry(agentId, task, queueItem, socialQueueItem, finishedAt, failed.lastErrorMessage(), cooldownSeconds, retryAt));
            }
        }
    }

    private boolean isDue(AgentStatus status, AgentRuntime runtime, LocalDateTime now) {
        if (runtime != null && "running".equalsIgnoreCase(runtime.runtimeState())) {
            return false;
        }
        LocalDateTime lastCompletedAt = latest(status.lastRun(), runtime == null ? null : runtime.lastTaskFinishedAt());
        if (lastCompletedAt == null) {
            return true;
        }
        long cooldownSeconds = cooldownSeconds(status, runtime);
        return !lastCompletedAt.plusSeconds(Math.max(5, cooldownSeconds)).isAfter(now);
    }

    private AgentRuntime recoverStaleRun(AgentProfile profile, String agentId, AgentStatus status, AgentRuntime runtime, LocalDateTime now) throws IOException {
        if (runtime == null || !"running".equalsIgnoreCase(runtime.runtimeState()) || runtime.lastTaskStartedAt() == null) {
            return runtime;
        }
        long staleAfterSeconds = Math.max(240, status.runIntervalSeconds() * 8L);
        if (!runtime.lastTaskStartedAt().plusSeconds(staleAfterSeconds).isBefore(now)) {
            return runtime;
        }
        AgentRuntime recovered = runtime.failure(
                status.executionTarget(),
                runtime.currentTask().isBlank() ? taskNameFor(agentId) : runtime.currentTask(),
                runtime.lastTaskStartedAt(),
                now,
                "Recovered stale running state");
        runtimeStore.write(profile, recovered);
        activityStore.append(profile, staleRunEntry(agentId, recovered.currentTask(), now, staleAfterSeconds));
        return recovered;
    }

    private long cooldownSeconds(AgentStatus status, AgentRuntime runtime) {
        long cooldownSeconds = status.runIntervalSeconds();
        if (runtime != null && runtime.consecutiveFailures() > 0) {
            cooldownSeconds *= Math.min(6, 1L << Math.min(3, runtime.consecutiveFailures()));
        }
        return cooldownSeconds;
    }

    private LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private CycleResult runAgentCycle(String agentId, ProspectSearchQueueItem queueItem, SocialSearchQueueItem socialQueueItem, AgentStatus status) throws IOException, InterruptedException {
        if ("prospecting".equals(agentId)) {
            AgentProfile profile = agentRegistry.load(agentId);
            if (profile == null) {
                return new CycleResult("", "");
            }
            return new CycleResult(
                    agent.collectQueuedProspectsBatch(agentId, 5, PROSPECTING_QUEUE_ITEMS_PER_CYCLE, status.executionTarget()),
                    outputPathFor(agentId, status));
        }
        if ("social-media".equals(agentId)) {
            AgentProfile profile = agentRegistry.load(agentId);
            if (profile == null) {
                return new CycleResult("", "");
            }
            return new CycleResult(
                    agent.collectQueuedSocialOpportunitiesBatch(agentId, 5, SOCIAL_QUEUE_ITEMS_PER_CYCLE, status.executionTarget()),
                    outputPathFor(agentId, status));
        }
        if ("email-support".equals(agentId)) {
            return new CycleResult(
                    agent.syncAndDraftEmailInbox(agentId, 8, 3),
                    outputPathFor(agentId, status));
        }
        return new CycleResult("", "");
    }

    private String taskNameFor(String agentId) {
        if ("prospecting".equals(agentId)) {
            return "queued prospect batch";
        }
        if ("social-media".equals(agentId)) {
            return "queued social batch";
        }
        if ("email-support".equals(agentId)) {
            return "imap inbox sync";
        }
        return "autonomous cycle";
    }

    private String outputPathFor(String agentId, AgentStatus status) {
        if (status != null && status.primaryOutputFile() != null && !status.primaryOutputFile().isBlank()) {
            return "agents/" + agentId + "/" + status.primaryOutputFile();
        }
        return "";
    }

    private ProspectSearchQueueItem queueItemFor(String agentId) throws IOException {
        if (!"prospecting".equals(agentId)) {
            return null;
        }
        return prospectSearchQueueStore.nextDue(agentId);
    }

    private SocialSearchQueueItem socialQueueItemFor(String agentId) throws IOException {
        if (!"social-media".equals(agentId)) {
            return null;
        }
        return socialSearchQueueStore.nextDue(agentId);
    }

    private AgentActivityEntry successEntry(String agentId, String task, ProspectSearchQueueItem queueItem, SocialSearchQueueItem socialQueueItem, CycleResult cycle, LocalDateTime timestamp) {
        String summary = summarize(cycle.message());
        StringBuilder details = new StringBuilder();
        details.append("Agent: ").append(agentId).append('\n');
        details.append("Task: ").append(task).append('\n');
        if (queueItem != null) {
            details.append("Queue item: ").append(queueItem.id()).append('\n');
            details.append("Query: ").append(queueItem.effectiveQuery()).append('\n');
            details.append("Pass: ").append(queueItem.pass()).append('\n');
            details.append("Region: ").append(blank(queueItem.region())).append('\n');
            details.append("City: ").append(blank(queueItem.city())).append('\n');
            details.append("Industry: ").append(blank(queueItem.industry())).append('\n');
        } else if (socialQueueItem != null) {
            details.append("Queue item: ").append(socialQueueItem.id()).append('\n');
            details.append("Query: ").append(socialQueueItem.effectiveQuery()).append('\n');
            details.append("Pass: ").append(socialQueueItem.pass()).append('\n');
            details.append("Site: ").append(blank(socialQueueItem.site())).append('\n');
            details.append("Topic: ").append(blank(socialQueueItem.topic())).append('\n');
            details.append("Audience: ").append(blank(socialQueueItem.audience())).append('\n');
        }
        details.append('\n');
        details.append(cycle.message());
        return new AgentActivityEntry(timestamp, "success", task, summary, details.toString().trim());
    }

    private AgentActivityEntry failureEntry(String task, ProspectSearchQueueItem queueItem, SocialSearchQueueItem socialQueueItem, LocalDateTime timestamp, String errorMessage) {
        StringBuilder details = new StringBuilder();
        details.append("Task: ").append(task).append('\n');
        if (queueItem != null) {
            details.append("Queue item: ").append(queueItem.id()).append('\n');
            details.append("Query: ").append(queueItem.effectiveQuery()).append('\n');
            details.append("Pass: ").append(queueItem.pass()).append('\n');
        } else if (socialQueueItem != null) {
            details.append("Queue item: ").append(socialQueueItem.id()).append('\n');
            details.append("Query: ").append(socialQueueItem.effectiveQuery()).append('\n');
            details.append("Pass: ").append(socialQueueItem.pass()).append('\n');
        }
        details.append('\n');
        details.append("Error: ").append(blank(errorMessage));
        return new AgentActivityEntry(timestamp, "error", task, blank(errorMessage), details.toString().trim());
    }

    private AgentActivityEntry blockedEntry(String agentId, String task, ProspectSearchQueueItem queueItem, SocialSearchQueueItem socialQueueItem, LocalDateTime timestamp, String errorMessage, long cooldownSeconds, LocalDateTime retryAt) {
        StringBuilder details = new StringBuilder();
        details.append("Agent: ").append(agentId).append('\n');
        details.append("Task: ").append(task).append('\n');
        if (queueItem != null) {
            details.append("Queue item: ").append(queueItem.id()).append('\n');
            details.append("Query: ").append(queueItem.effectiveQuery()).append('\n');
            details.append("Pass: ").append(queueItem.pass()).append('\n');
        } else if (socialQueueItem != null) {
            details.append("Queue item: ").append(socialQueueItem.id()).append('\n');
            details.append("Query: ").append(socialQueueItem.effectiveQuery()).append('\n');
            details.append("Pass: ").append(socialQueueItem.pass()).append('\n');
        }
        details.append("Blocked reason: ").append(blank(errorMessage)).append('\n');
        details.append("Cooldown seconds: ").append(cooldownSeconds).append('\n');
        details.append("Next retry after: ").append(retryAt);
        return new AgentActivityEntry(timestamp, "error", "blocked", blockedSummary(errorMessage), details.toString().trim());
    }

    private AgentActivityEntry staleRunEntry(String agentId, String task, LocalDateTime timestamp, long staleAfterSeconds) {
        String details = """
                Agent: %s
                Task: %s
                Blocked reason: stale running state
                Recovery action: reset runtime state to error
                Stale threshold seconds: %d
                """.formatted(agentId, blank(task), staleAfterSeconds).trim();
        return new AgentActivityEntry(timestamp, "error", "runtime", "Recovered stale running state", details);
    }

    private String blockedSummary(String errorMessage) {
        String text = blank(errorMessage).toLowerCase();
        if (text.contains("timed out")) {
            return "Blocked by browser search timeout";
        }
        if (text.contains("connection reset")) {
            return "Blocked by browser connection reset";
        }
        if (text.contains("connect")) {
            return "Blocked by browser connection failure";
        }
        return "Blocked after failure";
    }

    private String summarize(String result) {
        String text = result == null ? "" : result.trim();
        return text.length() <= 240 ? text : text.substring(0, 237) + "...";
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "(none)" : value;
    }

    private record CycleResult(String message, String outputPath) {
    }

    public String describe() {
        String scope = onlyAgentId.isBlank() ? "all agents" : onlyAgentId;
        return "Supervisor running for " + scope + " with poll interval " + LOOP_SLEEP.toSeconds() + " seconds.";
    }
}
