package helion;

public record WriteMemoryAction(String key, String content) implements ManagerAction {
}
