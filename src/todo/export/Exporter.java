package todo.export;

import todo.model.Task;

import javax.swing.*;

public final class Exporter {
    private Exporter() {}

    public static String toMarkdown(ListModel<Task> model) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < model.getSize(); i++) {
            Task t = model.getElementAt(i);
            sb.append(t.completed ? "- [x] " : "- [ ] ");
            sb.append(t.title);
            if (t.dueAtMillis != null) sb.append(" (due: ").append(todo.util.DateUtil.formatDue(t.dueAtMillis)).append(")");
            if (!t.tags.isEmpty()) { sb.append(" "); for (String tag: t.tags) sb.append('#').append(tag).append(' '); }
            sb.append('\n');
            if (t.note != null && !t.note.isBlank()) sb.append("  \n  ").append(t.note.replace("\n","\n  ")).append('\n');
        }
        return sb.toString();
    }
}

