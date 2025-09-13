package todo;

import todo.model.Priority;
import todo.model.Task;
import todo.storage.TaskStorage;
import todo.util.DateUtil;
import todo.view.FilteredSortedListModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MainFrame extends JFrame {
    private final Path storagePath = TaskStorage.defaultPath();
    private final DefaultListModel<Task> model = new DefaultListModel<>();
    private final FilteredSortedListModel viewModel = new FilteredSortedListModel(model);
    private final Deque<List<Task>> undoStack = new ArrayDeque<>();
    private final todo.undo.UndoManager undoManager = new todo.undo.UndoManager();

    private final JTextField titleField = new JTextField(28);
    private final JTextArea descArea = new JTextArea(3, 28);
    private final JCheckBox dueEnable = new JCheckBox("Due:");
    private final JSpinner dueSpinner = new JSpinner(new javax.swing.SpinnerDateModel());
    private final JComboBox<Priority> priorityBox = new JComboBox<>(Priority.values());
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<String> filterBox = new JComboBox<>(new String[]{"All", "Active", "Completed"});
    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{"Added", "Title", "Due Soon", "Priority"});
    private final JLabel status = new JLabel();
    private final JList<Task> taskList = new JList<>(viewModel);
    private final todo.reminder.ReminderScheduler reminder = new todo.reminder.ReminderScheduler();

    public MainFrame() {
        super("To-Do");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        buildUI();
        wireActions();
        TaskStorage.load(storagePath, model);
        updateStatus();
        // Schedule reminders for loaded tasks
        reminder.rescheduleAll(model);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { TaskStorage.save(storagePath, model); }
        });

        pack();
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(titleField::requestFocusInWindow);
    }

    private void buildUI() {
        descArea.setLineWrap(true); descArea.setWrapStyleWord(true);

        // Toolbar with only Add (keep UI lean)
        JToolBar toolBar = new JToolBar(); toolBar.setFloatable(false);
        JButton tbAdd = new JButton("➕ Add"); tbAdd.setFocusable(false); tbAdd.setToolTipText("Add Task (⌘/Ctrl+N)");
        toolBar.add(tbAdd);

        // Filter/search/sort bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.add(new JLabel("Filter:")); filterBar.add(filterBox);
        filterBar.add(new JLabel("Sort:")); filterBar.add(sortBox);
        filterBar.add(Box.createHorizontalStrut(12));
        filterBar.add(new JLabel("Search:")); filterBar.add(searchField);

        // Input form: Title, Description, Priority, Due Date (with picker)
        JScrollPane descScroll = new JScrollPane(descArea);
        JButton addButton = new JButton("Add Task"); addButton.setToolTipText("Add the task (Enter)");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2,2,2,2);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; form.add(new JLabel("Title:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1; form.add(titleField, gc);
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; form.add(new JLabel("Description:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1; gc.fill = GridBagConstraints.BOTH; form.add(descScroll, gc);
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.fill = GridBagConstraints.NONE; form.add(new JLabel("Priority:"), gc);
        gc.gridx = 1; gc.gridy = 2; form.add(priorityBox, gc);
        // Due controls
        JSpinner.DateEditor de = new JSpinner.DateEditor(dueSpinner, "yyyy-MM-dd");
        dueSpinner.setEditor(de);
        JPanel dueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dueRow.add(dueEnable); dueRow.add(dueSpinner); dueSpinner.setEnabled(false);
        gc.gridx = 0; gc.gridy = 3; form.add(new JLabel("Due Date:"), gc);
        gc.gridx = 1; gc.gridy = 3; form.add(dueRow, gc);

        dueEnable.addActionListener(e -> dueSpinner.setEnabled(dueEnable.isSelected()));

        JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.add(form, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);

        // List and renderer
        taskList.setCellRenderer(new todo.ui.TaskCellRenderer());
        taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskList.setToolTipText("Click to toggle. Double-click to edit. Space toggles. Delete removes.");
        JScrollPane listScroll = new JScrollPane(taskList);

        // Root container
        JPanel root = new JPanel(new BorderLayout(10, 10)); root.setBorder(new EmptyBorder(10,10,10,10));
        JPanel topStack = new JPanel(); topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.add(toolBar); topStack.add(Box.createVerticalStrut(6)); topStack.add(filterBar); topStack.add(inputPanel);
        root.add(topStack, BorderLayout.NORTH);
        root.add(listScroll, BorderLayout.CENTER);
        JPanel actionBar = new JPanel(new BorderLayout()); actionBar.add(status, BorderLayout.WEST);
        root.add(actionBar, BorderLayout.SOUTH);
        setContentPane(root);

        // Connect add button
        addButton.addActionListener(e -> addTaskFromInputs());
        tbAdd.addActionListener(e -> addTaskFromInputs());
    }

    private void wireActions() {
        int meta = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        Action addAction = new AbstractAction("Add Task") { @Override public void actionPerformed(ActionEvent e) { addTaskFromInputs(); } };
        Action deleteAction = new AbstractAction("Delete Selected") { @Override public void actionPerformed(ActionEvent e) { deleteSelectedTasks(); } };
        Action clearDoneAction = new AbstractAction("Clear Completed") { @Override public void actionPerformed(ActionEvent e) { clearCompleted(); } };
        Action markAllDone = new AbstractAction("Mark All Completed") { @Override public void actionPerformed(ActionEvent e) { setAllCompleted(true); } };
        Action markAllActive = new AbstractAction("Mark All Active") { @Override public void actionPerformed(ActionEvent e) { setAllCompleted(false); } };
        Action exportAction = new AbstractAction("Export…") { @Override public void actionPerformed(ActionEvent e) { exportTasks(); } };
        Action importAction = new AbstractAction("Import…") { @Override public void actionPerformed(ActionEvent e) { importTasks(); } };
        Action focusSearch = new AbstractAction("Focus Search") { @Override public void actionPerformed(ActionEvent e) { searchField.requestFocusInWindow(); } };
        Action undoDelete = new AbstractAction("Undo Delete") { @Override public void actionPerformed(ActionEvent e) { undoLastDelete(); } };

        // Enter on title adds task; description allows newlines.
        titleField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "addTask");
        titleField.getActionMap().put("addTask", addAction);

        // Space toggles; Delete removes
        taskList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleComplete");
        taskList.getActionMap().put("toggleComplete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                int[] indices = taskList.getSelectedIndices();
                for (int i : indices) {
                    Task t = viewModel.getElementAt(i);
                    t.completed = !t.completed;
                    int src = indexOfModel(t);
                    if (src >= 0) model.set(src, t);
                }
                TaskStorage.save(storagePath, model);
                updateStatus();
            }
        });
        taskList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        taskList.getActionMap().put("delete", deleteAction);

        // Mouse interactions: click to toggle, double-click to edit
        taskList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    int index = taskList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Task t = viewModel.getElementAt(index);
                        t.completed = !t.completed;
                        int src = indexOfModel(t);
                        if (src >= 0) model.set(src, t);
                        TaskStorage.save(storagePath, model);
                        reminder.schedule(t);
                        updateStatus();
                    }
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = taskList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Task t = viewModel.getElementAt(index);
                        if (todo.ui.EditTaskDialog.open(MainFrame.this, t)) {
                            int src = indexOfModel(t);
                            if (src >= 0) model.set(src, t);
                            TaskStorage.save(storagePath, model);
                            reminder.schedule(t);
                            updateStatus();
                        }
                    }
                }
            }
        });

        // Context menu: delete, priority, due date
        JPopupMenu context = new JPopupMenu();
        context.add(new JMenuItem(deleteAction));
        JMenu priorityMenu = new JMenu("Priority");
        for (Priority p : Priority.values()) {
            priorityMenu.add(new JMenuItem(new AbstractAction(p.label) {
                @Override public void actionPerformed(ActionEvent e) {
                    int idx = taskList.getSelectedIndex(); if (idx < 0) return;
                    Task t = viewModel.getElementAt(idx); t.priority = p;
                    int src = indexOfModel(t); if (src >= 0) model.set(src, t);
                    TaskStorage.save(storagePath, model);
                }
            }));
        }
        context.add(priorityMenu);
        context.add(new JMenuItem(new AbstractAction("Set Due Date…") {
            @Override public void actionPerformed(ActionEvent e) {
                int idx = taskList.getSelectedIndex(); if (idx < 0) return;
                Task t = viewModel.getElementAt(idx);
                Long chosen = openDueDatePicker(t.dueAtMillis);
                t.dueAtMillis = chosen;
                int src = indexOfModel(t); if (src >= 0) model.set(src, t);
                TaskStorage.save(storagePath, model);
                reminder.schedule(t);
            }
        }));
        taskList.setComponentPopupMenu(context);

        // Enable drag & drop reordering on the list
        taskList.setDragEnabled(true);
        taskList.setDropMode(DropMode.INSERT);
        taskList.setTransferHandler(new TransferHandler() {
            private int[] indices = null;
            @Override public int getSourceActions(JComponent c) { return MOVE; }
            @Override protected Transferable createTransferable(JComponent c) {
                indices = taskList.getSelectedIndices();
                return new StringSelection("move");
            }
            @Override public boolean canImport(TransferSupport support) { return support.isDrop(); }
            @Override public boolean importData(TransferSupport support) {
                if (!support.isDrop() || indices == null) return false;
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int index = dl.getIndex();
                // Move in backing model according to visible indices
                java.util.List<Task> moving = new java.util.ArrayList<>();
                for (int i = indices.length-1; i>=0; i--) moving.add(0, viewModel.getElementAt(indices[i]));
                // Remove originals
                for (Task t : moving) { int src=indexOfModel(t); if (src>=0) model.remove(src); }
                // Compute target index in model based on visible index
                Task anchor = null; if (index<taskList.getModel().getSize()) anchor = viewModel.getElementAt(index);
                int modelTarget = anchor==null ? model.getSize() : indexOfModel(anchor);
                for (Task t : moving) { model.add(modelTarget++, t); }
                TaskStorage.save(storagePath, model); return true;
            }
        });

        // Model change listeners update status
        model.addListDataListener(new ListDataListener() {
            @Override public void intervalAdded(ListDataEvent e) { updateStatus(); }
            @Override public void intervalRemoved(ListDataEvent e) { updateStatus(); }
            @Override public void contentsChanged(ListDataEvent e) { updateStatus(); }
        });

        // Filter/search/sort wiring
        Runnable refresh = () -> viewModel.setFilterAndSort((String) filterBox.getSelectedItem(), searchField.getText(), (String) sortBox.getSelectedItem());
        filterBox.addActionListener(e -> refresh.run());
        sortBox.addActionListener(e -> refresh.run());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
        });
        refresh.run();

        // Menu bar
        setJMenuBar(buildMenuBar(addAction, deleteAction, undoDelete, clearDoneAction, markAllDone, markAllActive, exportAction, importAction, focusSearch));
    }

    private JMenuBar buildMenuBar(Action addAction, Action deleteAction, Action undoDelete, Action clearDoneAction,
                                  Action markAllDone, Action markAllActive,
                                  Action exportAction, Action importAction, Action focusSearch) {
        int meta = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem miExport = new JMenuItem(exportAction); miExport.setText("Export…");
        JMenuItem miImport = new JMenuItem(importAction); miImport.setText("Import…");
        file.add(miExport); file.add(miImport);
        file.add(new JMenuItem(new AbstractAction("Export JSON…") {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Export JSON");
                if (fc.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    TaskStorage.saveJson(fc.getSelectedFile().toPath(), model);
                }
            }
        }));
        file.add(new JMenuItem(new AbstractAction("Import JSON…") {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Import JSON");
                if (fc.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    DefaultListModel<Task> tmp = new DefaultListModel<>();
                    TaskStorage.loadJson(fc.getSelectedFile().toPath(), tmp);
                    for (int i = 0; i < tmp.size(); i++) model.addElement(tmp.get(i));
                }
            }
        }));
        file.add(new JMenuItem(new AbstractAction("Archive Completed…") {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Archive Completed to JSON");
                if (fc.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    TaskStorage.archiveCompleted(fc.getSelectedFile().toPath(), model);
                    TaskStorage.save(storagePath, model);
                }
            }
        }));
        file.add(new JMenuItem(new AbstractAction("Export Visible as Markdown…") {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Export Markdown");
                if (fc.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    String md = todo.export.Exporter.toMarkdown(taskList.getModel());
                    try { java.nio.file.Files.write(fc.getSelectedFile().toPath(), md.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(MainFrame.this, "Failed to export: "+ex.getMessage()); }
                }
            }
        }));
        file.add(new JMenuItem(new AbstractAction("Copy Visible to Clipboard") {
            @Override public void actionPerformed(ActionEvent e) {
                String md = todo.export.Exporter.toMarkdown(taskList.getModel());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(md), null);
            }
        }));

        JMenu actions = new JMenu("Actions");
        JMenuItem miAdd = new JMenuItem(addAction); miAdd.setText("Add Task"); miAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, meta));
        JMenuItem miDelete = new JMenuItem(deleteAction); miDelete.setText("Delete Selected"); miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        JMenuItem miUndo = new JMenuItem(new AbstractAction("Undo") { @Override public void actionPerformed(ActionEvent e) { undoManager.undo(); } }); miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, meta));
        JMenuItem miRedo = new JMenuItem(new AbstractAction("Redo") { @Override public void actionPerformed(ActionEvent e) { undoManager.redo(); } }); miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, meta));
        JMenuItem miClear = new JMenuItem(clearDoneAction); miClear.setText("Clear Completed"); miClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, meta | KeyEvent.SHIFT_DOWN_MASK));
        JMenuItem miAllDone = new JMenuItem(markAllDone); miAllDone.setText("Mark All Completed");
        JMenuItem miAllActive = new JMenuItem(markAllActive); miAllActive.setText("Mark All Active");
        actions.add(miAdd); actions.add(miDelete); actions.add(miUndo); actions.add(miRedo); actions.addSeparator(); actions.add(miClear); actions.add(miAllDone); actions.add(miAllActive);

        // Bulk edit submenu
        JMenu bulk = new JMenu("Bulk Edit");
        for (Priority p : Priority.values()) {
            bulk.add(new JMenuItem(new AbstractAction("Set Priority: "+p.label) {
                @Override public void actionPerformed(ActionEvent e) {
                    int[] idx = taskList.getSelectedIndices(); if (idx.length==0) return; for (int i: idx){ Task t=viewModel.getElementAt(i); t.priority=p; int src=indexOfModel(t); if(src>=0) model.set(src,t);} TaskStorage.save(storagePath, model);
                }
            }));
        }
        bulk.add(new JMenuItem(new AbstractAction("Set Due Date for Selected…") {
            @Override public void actionPerformed(ActionEvent e) {
                Long chosen = openDueDatePicker(null);
                int[] idx = taskList.getSelectedIndices(); if (idx.length==0) return; for (int i: idx){ Task t=viewModel.getElementAt(i); t.dueAtMillis=chosen; int src=indexOfModel(t); if(src>=0) model.set(src,t);} TaskStorage.save(storagePath, model);
            }
        }));
        actions.add(bulk);

        JMenu view = new JMenu("View");
        JMenuItem miSearch = new JMenuItem(focusSearch); miSearch.setText("Focus Search"); miSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, meta));
        view.add(miSearch);
        JCheckBoxMenuItem miDesc = new JCheckBoxMenuItem(new AbstractAction("Show Descriptions") {
            @Override public void actionPerformed(ActionEvent e) {
                todo.ui.TaskCellRenderer.showDescription = ((JCheckBoxMenuItem)e.getSource()).getState();
                taskList.repaint();
            }
        });
        miDesc.setState(todo.ui.TaskCellRenderer.showDescription);
        view.add(miDesc);
        view.add(new JMenuItem(new AbstractAction("Preset: Today") {
            @Override public void actionPerformed(ActionEvent e) {
                String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                searchField.setText("due<"+today+" due>"+today); // between yesterday and tomorrow won't match; set filter to Active + query title today
                filterBox.setSelectedItem("Active");
            }
        }));
        view.add(new JMenuItem(new AbstractAction("Preset: Overdue") {
            @Override public void actionPerformed(ActionEvent e) {
                String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                searchField.setText("due<"+today);
                filterBox.setSelectedItem("Active");
            }
        }));
        view.add(new JMenuItem(new AbstractAction("Preset: High Priority") {
            @Override public void actionPerformed(ActionEvent e) {
                searchField.setText("priority>=HIGH");
            }
        }));

        JMenu tools = new JMenu("Tools");
        JMenuItem miQuickAdd = new JMenuItem(new AbstractAction("Quick Add…") {
            @Override public void actionPerformed(ActionEvent e) {
                String in = JOptionPane.showInputDialog(MainFrame.this, "Title with #tags @YYYY-MM-DD !high:");
                if (in != null) quickAdd(in);
            }
        });
        tools.add(miQuickAdd);

        JMenu help = new JMenu("Help");
        JMenuItem miQuick = new JMenuItem(new AbstractAction("Quick Help") { @Override public void actionPerformed(ActionEvent e) { showHelpDialog(); } });
        miQuick.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        JMenuItem miAbout = new JMenuItem(new AbstractAction("About") { @Override public void actionPerformed(ActionEvent e) { showAboutDialog(); } });
        help.add(miQuick); help.addSeparator(); help.add(miAbout);

        bar.add(file); bar.add(actions); bar.add(view); bar.add(tools); bar.add(help);
        return bar;
    }

    // Parse "Title words #work #home @2025-01-01 !high" into Task
    private void quickAdd(String input) {
        String s = input.trim(); if (s.isEmpty()) return;
        Task t = new Task(s, false);
        for (String part : s.split("\\s+")) if (part.startsWith("#") && part.length()>1) t.addTag(part.substring(1));
        for (String part : s.split("\\s+")) if (part.startsWith("@") && part.length()>1) {
            Long d = todo.util.DateUtil.parseDue(part.substring(1)); if (d!=null) t.dueAtMillis = d;
        }
        if (s.contains("!high")) t.priority = Priority.HIGH; else if (s.contains("!urgent")) t.priority = Priority.URGENT; else if (s.contains("!low")) t.priority = Priority.LOW;
        t.title = s.replaceAll("[#@!][^\\s]+", "").trim();
        if (t.title.isBlank()) t.title = input.trim();
        model.addElement(t);
        TaskStorage.save(storagePath, model);
    }

    private void addTaskFromInputs() {
        String title = oneLine(titleField.getText());
        if (title.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter a task title.");
            return;
        }
        Task t = new Task(title, false);
        String desc = descArea.getText().trim();
        if (!desc.isEmpty()) t.note = desc;
        t.priority = (Priority) priorityBox.getSelectedItem();
        if (dueEnable.isSelected()) {
            java.util.Date date = (java.util.Date) dueSpinner.getValue();
            t.dueAtMillis = todo.util.DateUtil.startOfDayMillis(date);
        }
        model.addElement(t);
        titleField.setText(""); descArea.setText(""); dueEnable.setSelected(false); dueSpinner.setEnabled(false);
        TaskStorage.save(storagePath, model);
        // Undo: remove the added task; Redo: add it back
        Task added = t;
        undoManager.apply(new todo.undo.Command() {
            @Override public void execute() { if (indexOfModel(added) < 0) { model.addElement(added); TaskStorage.save(storagePath, model);} }
            @Override public void undo() { int i=indexOfModel(added); if (i>=0) { model.remove(i); TaskStorage.save(storagePath, model);} }
        });
    }

    private void clearCompleted() {
        for (int i = model.size() - 1; i >= 0; i--) if (model.get(i).completed) model.remove(i);
        TaskStorage.save(storagePath, model);
    }

    private void setAllCompleted(boolean completed) {
        for (int i = 0; i < model.size(); i++) { model.get(i).completed = completed; model.set(i, model.get(i)); }
        TaskStorage.save(storagePath, model);
    }

    private void deleteSelectedTasks() {
        int[] selected = taskList.getSelectedIndices(); if (selected.length == 0) return;
        List<Task> removed = new ArrayList<>();
        for (int i = selected.length - 1; i >= 0; i--) {
            Task t = viewModel.getElementAt(selected[i]);
            removed.add(0, t);
            int src = indexOfModel(t);
            if (src >= 0) model.remove(src);
        }
        TaskStorage.save(storagePath, model);
        // Undo: re-add removed tasks in order; Redo: remove them again
        undoManager.apply(new todo.undo.Command() {
            @Override public void execute() { for (Task t : removed) if (indexOfModel(t)<0) model.addElement(t); TaskStorage.save(storagePath, model);} 
            @Override public void undo() { for (int i=model.size()-1;i>=0;i--){ if(removed.contains(model.get(i))) model.remove(i);} TaskStorage.save(storagePath, model);} 
        });
    }

    private void undoLastDelete() {
        if (undoStack.isEmpty()) return;
        List<Task> last = undoStack.pop();
        for (Task t : last) model.addElement(t);
        TaskStorage.save(storagePath, model);
    }

    private void exportTasks() {
        JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Export Tasks");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) TaskStorage.save(fc.getSelectedFile().toPath(), model);
    }

    private void importTasks() {
        JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Import Tasks");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            DefaultListModel<Task> tmp = new DefaultListModel<>();
            TaskStorage.load(fc.getSelectedFile().toPath(), tmp);
            for (int i = 0; i < tmp.size(); i++) model.addElement(tmp.get(i));
        }
    }

    private void updateStatus() {
        int total = model.getSize(); int done = 0; for (int i = 0; i < total; i++) if (model.get(i).completed) done++;
        status.setText(total + " tasks • " + done + " completed");
    }

    private void showHelpDialog() {
        String msg = "Quick Help\n\n" +
                "• Add Task: Enter Title (and optional Description), press Enter or Actions → Add (⌘/Ctrl+N)\n" +
                "• Toggle Complete: Click a task or press Space\n" +
                "• Delete: Select task(s) and press Delete (Undo with ⌘/Ctrl+Z)\n" +
                "• Clear Completed: Actions → Clear Completed (⌘/Ctrl+Shift+C)\n" +
                "• Edit Title: Double‑click a task\n" +
                "• Due Date: Right‑click → Set Due Date… (date picker)\n\n" +
                "Tips\n" +
                "• Use filters/search/sort to focus\n" +
                "• Tasks auto‑save to tasks.txt next to the app";
        JOptionPane.showMessageDialog(this, msg, "Quick Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        String msg = "To‑Do List\nSimple Swing app for tasks.\n\n© You. Built with ❤️.";
        JOptionPane.showMessageDialog(this, msg, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String oneLine(String s) { return s.replace('\n', ' ').replace('\r', ' ').trim(); }
    private int indexOfModel(Task t) { for (int i = 0; i < model.size(); i++) if (model.get(i) == t) return i; return -1; }

    // Date picker dialog for due date with enable/disable.
    private Long openDueDatePicker(Long currentMillis) {
        JCheckBox enable = new JCheckBox("Set due date", currentMillis != null);
        JSpinner spinner = new JSpinner(new javax.swing.SpinnerDateModel());
        JSpinner.DateEditor de = new JSpinner.DateEditor(spinner, "yyyy-MM-dd"); spinner.setEditor(de);
        if (currentMillis != null) spinner.setValue(todo.util.DateUtil.toDate(currentMillis));
        spinner.setEnabled(enable.isSelected());
        enable.addActionListener(ev -> spinner.setEnabled(enable.isSelected()));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); panel.add(enable); panel.add(spinner);
        int res = JOptionPane.showConfirmDialog(this, panel, "Due Date", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            if (!enable.isSelected()) return null;
            java.util.Date date = (java.util.Date) spinner.getValue();
            return todo.util.DateUtil.startOfDayMillis(date);
        }
        return currentMillis;
    }
}
