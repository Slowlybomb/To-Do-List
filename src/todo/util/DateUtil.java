package todo.util;

import java.util.Calendar;
import java.util.Date;

public final class DateUtil {
    private DateUtil() {}

    // Parse YYYY-MM-DD to epoch millis (midnight system zone). Returns null for blanks/invalid.
    public static Long parseDue(String s) {
        try {
            if (s == null) return null; s = s.trim(); if (s.isEmpty()) return null;
            String[] parts = s.split("-"); if (parts.length != 3) return null;
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, y);
            cal.set(Calendar.MONTH, m - 1);
            cal.set(Calendar.DAY_OF_MONTH, d);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception ex) { return null; }
    }

    public static String formatDue(Long millis) {
        if (millis == null) return "";
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(millis);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", y, m, d);
    }

    // Normalize a Date to local midnight millis.
    public static long startOfDayMillis(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Turn millis back into a Date object.
    public static Date toDate(Long millis) {
        return millis == null ? new Date() : new Date(millis);
    }
}
