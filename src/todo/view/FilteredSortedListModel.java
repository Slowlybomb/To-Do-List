package todo.view;

import todo.model.Priority;
import todo.model.Task;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class FilteredSortedListModel extends AbstractListModel<Task> implements ListDataListener {
    private final DefaultListModel<Task> source;
    private java.util.List<Integer> index = new java.util.ArrayList<>();
    private String filter = "All"; private String query = ""; private String sort = "Added";

    public FilteredSortedListModel(DefaultListModel<Task> source) {
        this.source = source; source.addListDataListener(this); rebuild();
    }

    public void setFilterAndSort(String filter, String query, String sort) {
        this.filter = filter == null ? "All" : filter;
        this.query = query == null ? "" : query.trim().toLowerCase();
        this.sort = sort == null ? "Added" : sort;
        rebuild(); fireContentsChanged(this, 0, getSize());
    }

        private void rebuild() {
            index.clear();
            for (int i = 0; i < source.size(); i++) {
                Task t = source.get(i);
                if ("Active".equals(filter) && t.completed) continue;
                if ("Completed".equals(filter) && !t.completed) continue;
                if (!query.isEmpty() && !matchesQuery(t, query)) continue;
                index.add(i);
            }
            java.util.Comparator<Integer> cmp;
        switch (sort) {
            case "Title" -> cmp = java.util.Comparator.comparing(i -> source.get(i).title.toLowerCase());
            case "Due Soon" -> cmp = java.util.Comparator.comparing((Integer i) -> {
                    Task t = source.get(i); return t.dueAtMillis == null ? Long.MAX_VALUE : t.dueAtMillis; })
                    .thenComparing(i -> source.get(i).title.toLowerCase());
            case "Priority" -> cmp = java.util.Comparator.<Integer>comparingInt(i -> -priorityRank(source.get(i).priority))
                    .thenComparing(i -> source.get(i).title.toLowerCase());
            default -> cmp = java.util.Comparator.comparingLong(i -> source.get(i).createdAtMillis);
        }
        index.sort(cmp);
    }

    // Support basic operators: tag:foo, due<YYYY-MM-DD, due>YYYY-MM-DD, plain text matches title/desc
    private boolean matchesQuery(Task t, String q) {
        String[] parts = q.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("tag:")) {
                String tag = part.substring(4);
                if (!t.tags.contains(tag)) return false;
            } else if (part.startsWith("priority:")) {
                String val = part.substring(9).toUpperCase();
                try { if (t.priority != todo.model.Priority.valueOf(val)) return false; } catch (Exception e) { return false; }
            } else if (part.startsWith("priority>=")) {
                String val = part.substring(10).toUpperCase();
                try {
                    int need = priorityRank(todo.model.Priority.valueOf(val));
                    if (priorityRank(t.priority) < need) return false;
                } catch (Exception e) { return false; }
            } else if (part.startsWith("due<")) {
                Long d = todo.util.DateUtil.parseDue(part.substring(4)); if (d==null) return false;
                if (t.dueAtMillis == null || !(t.dueAtMillis < d)) return false;
            } else if (part.startsWith("due>")) {
                Long d = todo.util.DateUtil.parseDue(part.substring(4)); if (d==null) return false;
                if (t.dueAtMillis == null || !(t.dueAtMillis > d)) return false;
            } else {
                String needle = part.toLowerCase();
                String hay = (t.title+"\n"+(t.note==null?"":t.note)).toLowerCase();
                if (!hay.contains(needle)) return false;
            }
        }
        return true;
    }

    private int priorityRank(Priority p) { return switch (p) { case URGENT -> 3; case HIGH -> 2; case NORMAL -> 1; case LOW -> 0; }; }
    @Override public int getSize() { return index.size(); }
    @Override public Task getElementAt(int i) { return source.get(index.get(i)); }
    @Override public void intervalAdded(ListDataEvent e) { rebuild(); fireContentsChanged(this, 0, getSize()); }
    @Override public void intervalRemoved(ListDataEvent e) { rebuild(); fireContentsChanged(this, 0, getSize()); }
    @Override public void contentsChanged(ListDataEvent e) { rebuild(); fireContentsChanged(this, 0, getSize()); }
}
