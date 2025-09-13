package todo.model;

import java.util.Calendar;

// Minimal recurrence: DAILY, WEEKLY, MONTHLY. Can be extended later.
public class RecurrenceRule {
    public enum Type { NONE, DAILY, WEEKLY, MONTHLY }
    public final Type type;

    public RecurrenceRule(Type type) { this.type = type == null ? Type.NONE : type; }

    public static RecurrenceRule parse(String s) {
        if (s == null) return new RecurrenceRule(Type.NONE);
        String t = s.trim().toUpperCase();
        try { return new RecurrenceRule(Type.valueOf(t)); } catch (Exception e) { return new RecurrenceRule(Type.NONE); }
    }

    // Compute next occurrence at local midnight after the given millis.
    public Long nextOccurrence(Long lastDueAtMillis) {
        if (lastDueAtMillis == null || type == Type.NONE) return null;
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(lastDueAtMillis);
        switch (type) {
            case DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1);
            case WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1);
            case MONTHLY -> cal.add(Calendar.MONTH, 1);
            default -> {}
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}

