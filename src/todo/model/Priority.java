package todo.model;

public enum Priority {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent");

    public final String label;
    Priority(String label) { this.label = label; }
}

