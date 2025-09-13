package todo.reminder;

import todo.model.Task;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;

public class ReminderScheduler {
    private final Timer timer = new Timer("reminders", true);
    private final Map<Task, TimerTask> tasks = new HashMap<>();

    // Schedule a reminder at 9:00 local time on the task's due date (if in future)
    public void schedule(Task t) {
        cancel(t);
        if (t == null || t.completed || t.dueAtMillis == null) return;
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(t.dueAtMillis);
        cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        long when = cal.getTimeInMillis();
        if (when <= System.currentTimeMillis()) return;
        TimerTask tt = new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Reminder: " + t.title, "Task Due", JOptionPane.INFORMATION_MESSAGE));
            }
        };
        timer.schedule(tt, new Date(when));
        tasks.put(t, tt);
    }

    public void cancel(Task t) {
        TimerTask tt = tasks.remove(t); if (tt != null) tt.cancel();
    }

    public void rescheduleAll(ListModel<Task> model) {
        for (int i = 0; i < model.getSize(); i++) schedule(model.getElementAt(i));
    }
}
