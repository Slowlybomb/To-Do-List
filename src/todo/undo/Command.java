package todo.undo;

public interface Command {
    void execute();
    void undo();
}

