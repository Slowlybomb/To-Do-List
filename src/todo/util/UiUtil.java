package todo.util;

import todo.model.Priority;

import javax.swing.*;
import java.awt.*;

public final class UiUtil {
    private UiUtil() {}

    public static Color secondaryText(Color foreground, Color background) {
        // Returns a muted text color that contrasts with the background
        Color blendBase = foreground.getRed()+foreground.getGreen()+foreground.getBlue() > 382 ? Color.BLACK : Color.WHITE;
        return blend(background, blendBase, 0.4);
    }

    // ---------------- Existing UI helpers ----------------
    public static Color priorityColor(Priority p) {
        return switch (p) {
            case LOW -> new Color(100, 149, 237);
            case NORMAL -> new Color(120, 120, 120);
            case HIGH -> new Color(220, 120, 80);
            case URGENT -> new Color(200, 50, 50);
        };
    }

    // Small colored dot icon for priority.
    public static Icon priorityDot(Priority p) {
        int d = 8; Color col = priorityColor(p);
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col); g2.fillOval(x, y + (getIconHeight()-d)/2, d, d); g2.dispose();
            }
            public int getIconWidth() { return d + 4; }
            public int getIconHeight() { return d + 4; }
        };
    }

    // Blend two colors by ratio (0..1). Used for subtle striping.
    public static Color blend(Color c1, Color c2, double ratio) {
        double r = Math.max(0, Math.min(1, ratio));
        int red = (int) Math.round(c1.getRed() * (1 - r) + c2.getRed() * r);
        int green = (int) Math.round(c1.getGreen() * (1 - r) + c2.getGreen() * r);
        int blue = (int) Math.round(c1.getBlue() * (1 - r) + c2.getBlue() * r);
        return new Color(red, green, blue);
    }
}
