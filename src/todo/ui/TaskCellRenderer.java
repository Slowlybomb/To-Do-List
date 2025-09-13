package todo.ui;

import todo.model.Task;
import todo.util.DateUtil;
import todo.util.UiUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class TaskCellRenderer extends JPanel implements ListCellRenderer<Task> {
    public static volatile boolean showDescription = true;
    private final JCheckBox check = new JCheckBox();
    private final JLabel title = new JLabel();
    private final JTextArea desc = new JTextArea();
    private final JLabel meta = new JLabel();

    public TaskCellRenderer() {
        super(new BorderLayout(6, 0));
        setOpaque(true);
        check.setOpaque(false);

        // Title styling: a bit larger and bold-ish by default; adjusted in renderer
        title.setOpaque(false);
        title.setBorder(new EmptyBorder(0, 0, 2, 0));

        // Description as a lightweight wrapped text area
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setOpaque(false);
        desc.setBorder(new EmptyBorder(0, 0, 0, 0));

        meta.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Build a content block: header (title + meta) over description
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.CENTER);
        header.add(meta, BorderLayout.EAST);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(header);
        content.add(desc);

        add(check, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        setBorder(new EmptyBorder(6, 6, 6, 6));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Task> list, Task value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        // Checkbox reflects completion state; title shows text with a priority dot.
        check.setSelected(value.completed);
        title.setText(value.title);
        title.setIcon(UiUtil.priorityDot(value.priority));

        // Title and description fonts
        Font base = list.getFont();
        Font titleFont = base.deriveFont(base.getStyle() | Font.BOLD, base.getSize2D() + 1.5f);
        Font descFont = base.deriveFont(Font.PLAIN, Math.max(11f, base.getSize2D() - 1f));
        title.setFont(titleFont);
        desc.setFont(descFont);
        Color darkDesc = new Color(90, 90, 90);
        desc.setForeground(darkDesc);
        desc.setText(value.note == null ? "" : value.note);
        boolean hasDesc = value.note != null && !value.note.isBlank();
        desc.setVisible(showDescription && hasDesc);

        // Meta: due date if present; highlight when overdue.
        boolean hasDue = value.dueAtMillis != null;
        boolean overdue = hasDue && System.currentTimeMillis() > value.dueAtMillis && !value.completed;
        String mt = hasDue ? ((overdue ? "⏰ " : "🗓 ") + DateUtil.formatDue(value.dueAtMillis)) : "";
        meta.setText(mt);

        // Tooltip shows description if present
        String tip = value.note != null && !value.note.isBlank() ? value.note : null;
        setToolTipText(tip);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            title.setForeground(list.getSelectionForeground());
            meta.setForeground(list.getSelectionForeground());
            // Keep description consistently darker; if completed, use muted
            Color muted = UIManager.getColor("Label.disabledForeground");
            if (value.completed && muted != null) desc.setForeground(muted); else desc.setForeground(darkDesc);
            setBorder(new LineBorder(list.getSelectionBackground().darker(), 1, true));
        } else {
            Color bg = list.getBackground();
            if (index % 2 == 0) {
                setBackground(UiUtil.blend(bg, Color.BLACK, 0.02));
            } else {
                setBackground(bg);
            }
            title.setForeground(list.getForeground());
            if (value.completed) {
                Color muted = UIManager.getColor("Label.disabledForeground");
                if (muted == null) muted = new Color(120, 120, 120);
                title.setForeground(muted);
                desc.setForeground(muted);
            } else {
                desc.setForeground(darkDesc);
            }
            if (overdue) meta.setForeground(new Color(200, 60, 60)); else meta.setForeground(list.getForeground());
            setBorder(new LineBorder(UiUtil.blend(bg, Color.BLACK, 0.08), 1, true));
        }
        return this;
    }
}
