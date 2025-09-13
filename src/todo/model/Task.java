package todo.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Task {
    public String title;
    public boolean completed;
    public Priority priority = Priority.NORMAL;
    public Long dueAtMillis; // nullable (start-of-day millis)
    public long createdAtMillis = System.currentTimeMillis();
    public String note; // long description

    // New: tags and subtasks for richer organization
    public final Set<String> tags = new HashSet<>();
    public final List<Subtask> subtasks = new ArrayList<>();

    // Optional recurrence rule
    public RecurrenceRule recurrence;

    public Task(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }

    // Subtasks API
    public void addSubtask(String title) {
        if (title == null) return;
        String t = title.trim();
        if (t.isEmpty()) return;
        subtasks.add(new Subtask(t));
    }

    public void toggleSubtask(int index) {
        if (index < 0 || index >= subtasks.size()) return;
        Subtask s = subtasks.get(index);
        s.done = !s.done;
    }

    // Tag helpers
    public void addTag(String tag) {
        if (tag == null) return;
        String t = tag.trim();
        if (!t.isEmpty()) tags.add(t);
    }
    public boolean hasTag(String tag) { return tags.contains(tag); }

    @Override public String toString() { return (completed ? "[x] " : "[ ] ") + title; }

    // Subtask model
    public static class Subtask {
        public String title;
        public boolean done;
        public Subtask(String title) { this.title = title; }
    }
}
