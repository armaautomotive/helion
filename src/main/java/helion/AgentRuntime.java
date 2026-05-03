package helion;

import java.time.LocalDateTime;

public record AgentRuntime(
        String agentId,
        String executionState,
        String runtimeState,
        String currentTask,
        LocalDateTime lastTaskStartedAt,
        LocalDateTime lastTaskFinishedAt,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastErrorAt,
        int consecutiveFailures,
        int totalSuccesses,
        int totalFailures,
        String lastResultSummary,
        String lastOutputPath,
        long averageCycleSeconds,
        String lastErrorMessage) {

    public static AgentRuntime initial(String agentId, String executionState) {
        return new AgentRuntime(
                agentId,
                executionState,
                "idle",
                "",
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                "",
                "",
                0,
                "");
    }

    public AgentRuntime start(String executionState, String task, LocalDateTime startedAt) {
        return new AgentRuntime(
                agentId,
                executionState,
                "running",
                task,
                startedAt,
                lastTaskFinishedAt,
                lastSuccessAt,
                lastErrorAt,
                consecutiveFailures,
                totalSuccesses,
                totalFailures,
                lastResultSummary,
                lastOutputPath,
                averageCycleSeconds,
                "");
    }

    public AgentRuntime success(String executionState, String task, LocalDateTime startedAt, LocalDateTime finishedAt, String summary, String outputPath) {
        long cycleSeconds = startedAt == null ? 0 : java.time.Duration.between(startedAt, finishedAt).getSeconds();
        long average = totalSuccesses <= 0 ? cycleSeconds : Math.round(((double) (averageCycleSeconds * totalSuccesses) + cycleSeconds) / (totalSuccesses + 1));
        return new AgentRuntime(
                agentId,
                executionState,
                "idle",
                task,
                startedAt,
                finishedAt,
                finishedAt,
                lastErrorAt,
                0,
                totalSuccesses + 1,
                totalFailures,
                summary == null ? "" : summary,
                outputPath == null ? "" : outputPath,
                average,
                "");
    }

    public AgentRuntime failure(String executionState, String task, LocalDateTime startedAt, LocalDateTime finishedAt, String errorMessage) {
        long cycleSeconds = startedAt == null ? 0 : java.time.Duration.between(startedAt, finishedAt).getSeconds();
        long average = averageCycleSeconds <= 0 ? cycleSeconds : averageCycleSeconds;
        return new AgentRuntime(
                agentId,
                executionState,
                "error",
                task,
                startedAt,
                finishedAt,
                lastSuccessAt,
                finishedAt,
                consecutiveFailures + 1,
                totalSuccesses,
                totalFailures + 1,
                lastResultSummary,
                lastOutputPath,
                average,
                errorMessage == null ? "" : errorMessage);
    }

    public String toJson() {
        return "{"
                + "\"agentId\":\"" + TextUtils.escapeJson(agentId) + "\","
                + "\"executionState\":\"" + TextUtils.escapeJson(executionState) + "\","
                + "\"runtimeState\":\"" + TextUtils.escapeJson(runtimeState) + "\","
                + "\"currentTask\":\"" + TextUtils.escapeJson(currentTask) + "\","
                + "\"lastTaskStartedAt\":\"" + TextUtils.escapeJson(format(lastTaskStartedAt)) + "\","
                + "\"lastTaskFinishedAt\":\"" + TextUtils.escapeJson(format(lastTaskFinishedAt)) + "\","
                + "\"lastSuccessAt\":\"" + TextUtils.escapeJson(format(lastSuccessAt)) + "\","
                + "\"lastErrorAt\":\"" + TextUtils.escapeJson(format(lastErrorAt)) + "\","
                + "\"consecutiveFailures\":" + consecutiveFailures + ","
                + "\"totalSuccesses\":" + totalSuccesses + ","
                + "\"totalFailures\":" + totalFailures + ","
                + "\"lastResultSummary\":\"" + TextUtils.escapeJson(lastResultSummary) + "\","
                + "\"lastOutputPath\":\"" + TextUtils.escapeJson(lastOutputPath) + "\","
                + "\"averageCycleSeconds\":" + averageCycleSeconds + ","
                + "\"lastErrorMessage\":\"" + TextUtils.escapeJson(lastErrorMessage) + "\""
                + "}";
    }

    public String render() {
        return """
                Runtime state: %s
                Current task: %s
                Last task started: %s
                Last task finished: %s
                Last success: %s
                Last error: %s
                Consecutive failures: %d
                Total successes: %d
                Total failures: %d
                Average cycle seconds: %d
                Last result summary: %s
                Last output path: %s
                Last error message: %s
                """.formatted(
                blank(runtimeState),
                blank(currentTask),
                blank(format(lastTaskStartedAt)),
                blank(format(lastTaskFinishedAt)),
                blank(format(lastSuccessAt)),
                blank(format(lastErrorAt)),
                consecutiveFailures,
                totalSuccesses,
                totalFailures,
                averageCycleSeconds,
                blank(lastResultSummary),
                blank(lastOutputPath),
                blank(lastErrorMessage)).trim();
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.toString();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "(none)" : value;
    }
}
