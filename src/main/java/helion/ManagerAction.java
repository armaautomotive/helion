package helion;

public sealed interface ManagerAction permits FinalAction, WorkerAction, SearchAction, FetchAction, ReadMemoryAction, WriteMemoryAction {
}
