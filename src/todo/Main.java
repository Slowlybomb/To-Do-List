package todo;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeelQuietly();
            new MainFrame().setVisible(true);
        });
    }

    private static void setSystemLookAndFeelQuietly() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
    }
}
