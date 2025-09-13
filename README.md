# To-Do List (Java Swing)

A lightweight, fast, and friendly desktop to‑do list app built with Java Swing. It focuses on clarity and productivity with rich task details, powerful filtering/search, and handy import/export options.

## Features

- Add tasks with title, description, priority, and due date (date picker)
- Toggle complete (click or Space) and rename (double‑click)
- Bulk edit (set priority and due date for multiple tasks)
- Filters (All / Active / Completed) and live search
- Advanced query syntax: `tag:work`, `priority:HIGH`, `priority>=HIGH`, `due<2025-01-01`, `due>2025-01-01`, plain text
- Sorting: Added, Title, Due Soon, Priority
- Quick Add parser: `Title words #tag1 #tag2 @YYYY-MM-DD !high|!urgent|!low`
- Drag & drop reordering in the list
- Context menu: set priority, set due date, delete
- Undo/Redo scaffold (implemented for add/delete)
- Export visible tasks to Markdown; copy to clipboard
- Import/Export JSON; archive completed tasks to JSON
- Persistent storage (`tasks.txt`) with backward compatibility
- Status bar with counts and a simple Help/About menu

## Requirements

- Java JDK 8 or newer (JDK 11+ recommended)

## Build & Run

- macOS/Linux:
  ```bash
  find src -name '*.java' -print0 | xargs -0 javac
  java -cp src todo.Main
  ```
- Windows (PowerShell):
  ```powershell
  Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName | javac -@
  java -cp src todo.Main
  ```

If you see `Error: Could not find or load main class Main`, make sure you run `java -cp src todo.Main` (note the `todo` package).

## Keyboard Shortcuts

- Add Task: Ctrl/Cmd+N
- Delete Selected: Delete
- Toggle Complete: Space
- Undo / Redo: Ctrl/Cmd+Z / Ctrl/Cmd+Y
- Focus Search: Ctrl/Cmd+F
- Edit Task: Double‑click a task

## Filters & Query Syntax

Type queries in the search box; combine terms with spaces:

- Tag: `tag:work`
- Priority equals: `priority:HIGH`
- Priority at least: `priority>=HIGH` (values: LOW, NORMAL, HIGH, URGENT)
- Due before/after: `due<2025-01-01`, `due>2025-01-01`
- Text search: any other words match title and description

Presets are available under View (Today, Overdue, High Priority).

## Data & Persistence

Tasks are stored by default in `tasks.txt` at the project (working) folder using a robust line format with UTF‑8 Base64 encoding.

- v2 line format:
  ```
  v2|completed(0/1)|priority|dueMillis|createdMillis|base64(title)|base64(note)
  ```
- Legacy v1 (`0|<base64-title>`) is still read and automatically upgraded on save.

You can also import/export JSON from the File menu.

## Import/Export

- Export visible tasks to Markdown (File → Export Visible as Markdown…)
- Copy visible tasks as Markdown to clipboard
- Import/Export JSON
- Archive completed tasks to a JSON file (and remove them from the list)

## Project Structure

```
src/
  todo/
    Main.java               # Entrypoint
    MainFrame.java          # App window: menus, toolbar, list, actions
    export/Exporter.java    # Markdown export
    model/
      Task.java             # Task model (title, description, priority, due, tags, subtasks)
      Priority.java         # Priority enum
      RecurrenceRule.java   # Minimal recurrence (DAILY/WEEKLY/MONTHLY)
    reminder/ReminderScheduler.java # 9am on due date reminder
    storage/TaskStorage.java# Load/save (line format + JSON), archive, migrate
    ui/
      TaskCellRenderer.java # Task “card” renderer (title + description + meta)
      EditTaskDialog.java   # Full task editor dialog
    undo/
      Command.java          # Command interface
      UndoManager.java      # Simple undo/redo stack (add/delete wired)
    util/
      DateUtil.java         # Date parse/format helpers
      UiUtil.java           # UI helpers (colors, blending, icons)
  tasks.txt                 # App data (created at runtime; git-ignored)
```

## Development Notes

- Keep UI responsive (EDT‑friendly code) and prefer updating the backing model via `DefaultListModel#set` to trigger repaints.
- Filtering and sorting live in `FilteredSortedListModel` and never mutate the source data.
- Persistence is defensive: failures print to stderr without crashing the app.

## Troubleshooting

- Class not found: Use `java -cp src todo.Main` (package qualified).
- Compile errors on `Timer`: Ensure JDK is installed and the code is compiled as shown above.
- Nothing saves: Confirm you have write permissions in the working folder for `tasks.txt`.

## Roadmap Ideas

- Full undo/redo for edits, bulk changes, and reorders (Command pattern is in place)
- Tag editor UI and tag “chips” under titles
- Recurrence UI and auto‑schedule next instance
- Debounced save to reduce disk writes during bulk operations

---

PRs and suggestions welcome.
