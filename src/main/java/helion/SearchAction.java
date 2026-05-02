package helion;

public record SearchAction(String query, int limit) implements ManagerAction {
}
