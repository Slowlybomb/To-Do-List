package todo.ui;

import todo.model.Priority;
import todo.model.Task;
import todo.util.DateUtil;

import javax.swing.*;
import java.awt.*;

public class EditTaskDialog extends JDialog {
    private boolean ok;
    private final JTextField title = new JTextField(28);
    private final JTextArea desc = new JTextArea(4, 28);
    private final JComboBox<Priority> priority = new JComboBox<>(Priority.values());
    private final JCheckBox dueEnable = new JCheckBox("Due:");
    private final JSpinner dueSpinner = new JSpinner(new javax.swing.SpinnerDateModel());

    public EditTaskDialog(Window owner, Task task) {
        super(owner, "Edit Task", ModalityType.APPLICATION_MODAL);
        title.setText(task.title);
        desc.setText(task.note == null ? "" : task.note);
        desc.setLineWrap(true); desc.setWrapStyleWord(true);
        priority.setSelectedItem(task.priority);
        JSpinner.DateEditor de = new JSpinner.DateEditor(dueSpinner, "yyyy-MM-dd");
        dueSpinner.setEditor(de);
        if (task.dueAtMillis != null) {
            dueEnable.setSelected(true);
            dueSpinner.setValue(todo.util.DateUtil.toDate(task.dueAtMillis));
        }
        dueSpinner.setEnabled(dueEnable.isSelected());
        dueEnable.addActionListener(e -> dueSpinner.setEnabled(dueEnable.isSelected()));

        JPanel form = new JPanel(new GridBagLayout()); GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4); gc.anchor = GridBagConstraints.WEST; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx=0;gc.gridy=0; form.add(new JLabel("Title:"), gc);
        gc.gridx=1;gc.gridy=0; gc.weightx=1; form.add(title, gc);
        gc.gridx=0;gc.gridy=1; gc.weightx=0; form.add(new JLabel("Description:"), gc);
        gc.gridx=1;gc.gridy=1; gc.weightx=1; gc.fill=GridBagConstraints.BOTH; form.add(new JScrollPane(desc), gc);
        gc.gridx=0;gc.gridy=2; gc.weightx=0; gc.fill=GridBagConstraints.NONE; form.add(new JLabel("Priority:"), gc);
        gc.gridx=1;gc.gridy=2; form.add(priority, gc);
        JPanel dueRow = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); dueRow.add(dueEnable); dueRow.add(dueSpinner);
        gc.gridx=0;gc.gridy=3; form.add(new JLabel("Due Date:"), gc);
        gc.gridx=1;gc.gridy=3; form.add(dueRow, gc);

        JButton okBtn = new JButton("Save"); JButton cancelBtn=new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancelBtn); buttons.add(okBtn);
        okBtn.addActionListener(e -> { ok=true; setVisible(false); });
        cancelBtn.addActionListener(e -> setVisible(false));

        getContentPane().setLayout(new BorderLayout(8,8));
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(owner);
    }

    public boolean applyTo(Task t) {
        if (!ok) return false;
        String newTitle = title.getText().trim(); if (newTitle.isBlank()) return false;
        t.title = newTitle;
        String d = desc.getText().trim(); t.note = d.isEmpty()?null:d;
        t.priority = (Priority) priority.getSelectedItem();
        if (dueEnable.isSelected()) t.dueAtMillis = DateUtil.startOfDayMillis((java.util.Date) dueSpinner.getValue());
        else t.dueAtMillis = null;
        return true;
    }

    public static boolean open(Window parent, Task t) {
        EditTaskDialog dlg = new EditTaskDialog(parent, t); dlg.setVisible(true); return dlg.applyTo(t);
    }
}

