package todo.storage;

import todo.model.Priority;
import todo.model.Task;
import todo.model.RecurrenceRule;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class TaskStorage {
    private TaskStorage() {}

    public static Path defaultPath() { return Paths.get("tasks.txt"); }

    // Save tasks to disk. v2 format:
    // v2|completed(0/1)|priority|dueMillis|createdMillis|base64(title)|base64(note)
    public static void save(Path path, DefaultListModel<Task> model) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.size(); i++) {
                Task t = model.get(i);
                String title64 = Base64.getEncoder().encodeToString(t.title.getBytes(StandardCharsets.UTF_8));
                String note64 = Base64.getEncoder().encodeToString((t.note == null ? "" : t.note).getBytes(StandardCharsets.UTF_8));
                sb.append("v2|")
                  .append(t.completed ? '1' : '0').append('|')
                  .append(t.priority.name()).append('|')
                  .append(t.dueAtMillis == null ? "" : t.dueAtMillis).append('|')
                  .append(t.createdAtMillis).append('|')
                  .append(title64).append('|')
                  .append(note64).append('\n');
            }
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            System.err.println("Failed to save tasks: " + ex.getMessage());
        }
    }

    // Load tasks from disk, supporting legacy v1 and new v2 formats.
    public static void load(Path path, DefaultListModel<Task> model) {
        try {
            if (!Files.exists(path)) return;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                if (line.startsWith("v2|")) {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 7) {
                        boolean completed = "1".equals(parts[1]);
                        Priority pr;
                        try { pr = Priority.valueOf(parts[2]); } catch (Exception e) { pr = Priority.NORMAL; }
                        Long due = parts[3].isEmpty() ? null : Long.parseLong(parts[3]);
                        long created = parts[4].isEmpty() ? System.currentTimeMillis() : Long.parseLong(parts[4]);
                        String title = new String(Base64.getDecoder().decode(parts[5]), StandardCharsets.UTF_8);
                        String note = new String(Base64.getDecoder().decode(parts[6]), StandardCharsets.UTF_8);
                        Task t = new Task(oneLine(title), completed);
                        t.priority = pr;
                        t.dueAtMillis = due;
                        t.createdAtMillis = created;
                        t.note = note.isEmpty() ? null : note;
                        model.addElement(t);
                    }
                } else {
                    int sep = line.indexOf('|');
                    if (sep <= 0 || sep >= line.length() - 1) continue;
                    boolean completed = line.charAt(0) == '1';
                    String encoded = line.substring(sep + 1);
                    String title = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                    model.addElement(new Task(oneLine(title), completed));
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to load tasks: " + ex.getMessage());
        }
    }

    private static String oneLine(String s) {
        return s.replace('\n', ' ').replace('\r', ' ').trim();
    }

    // ---- JSON import/export (simple, self-contained) ----
    public static void saveJson(Path path, DefaultListModel<Task> model) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < model.size(); i++) {
            Task t = model.get(i);
            if (i > 0) sb.append(",\n");
            sb.append("  {");
            sb.append("\"title\":").append(quote(t.title)).append(",");
            sb.append("\"completed\":").append(t.completed).append(",");
            sb.append("\"priority\":").append(quote(t.priority.name())).append(",");
            sb.append("\"dueAtMillis\":").append(t.dueAtMillis == null ? "null" : t.dueAtMillis).append(",");
            sb.append("\"createdAtMillis\":").append(t.createdAtMillis).append(",");
            sb.append("\"note\":").append(quoteOrNull(t.note)).append(",");
            // tags
            sb.append("\"tags\":[");
            int c = 0; for (String tag : t.tags) { if (c++>0) sb.append(','); sb.append(quote(tag)); }
            sb.append("],");
            // subtasks
            sb.append("\"subtasks\":[");
            for (int j = 0; j < t.subtasks.size(); j++) {
                var s = t.subtasks.get(j);
                if (j>0) sb.append(',');
                sb.append('{').append("\"title\":").append(quote(s.title)).append(',').append("\"done\":").append(s.done).append('}');
            }
            sb.append("],");
            // recurrence
            sb.append("\"recurrence\":").append(t.recurrence == null ? "null" : quote(t.recurrence.type.name()));
            sb.append("}");
        }
        sb.append("\n]\n");
        try { Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8)); }
        catch (IOException e) { System.err.println("Failed to save JSON: "+e.getMessage()); }
    }

    public static void loadJson(Path path, DefaultListModel<Task> model) {
        try {
            if (!Files.exists(path)) return;
            String json = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (!json.startsWith("[") || !json.endsWith("]")) return;
            // very small, permissive parser for our own format
            String body = json.substring(1, json.length()-1).trim();
            if (body.isEmpty()) return;
            // split top-level objects by '},' boundaries (assumes no nested objects beyond subtasks)
            List<String> objs = new ArrayList<>();
            int depth = 0, start = 0; for (int i=0;i<body.length();i++){ char ch=body.charAt(i); if(ch=='{'){ if(depth++==0) start=i;} else if(ch=='}'){ if(--depth==0){ objs.add(body.substring(start,i+1)); } } }
            for (String obj : objs) {
                String o = obj;
                Task t = new Task(extractString(o, "title"), extractBoolean(o, "completed"));
                String pr = extractString(o, "priority");
                try { t.priority = Priority.valueOf(pr); } catch (Exception ignored) {}
                Long due = extractLongOrNull(o, "dueAtMillis");
                if (due != null) t.dueAtMillis = due;
                Long created = extractLongOrNull(o, "createdAtMillis");
                if (created != null) t.createdAtMillis = created;
                t.note = extractStringOrNull(o, "note");
                // tags
                for (String tag : extractStringArray(o, "tags")) t.addTag(tag);
                // subtasks
                for (String sub : extractObjectArray(o, "subtasks")) {
                    String st = extractString(sub, "title"); boolean done = extractBoolean(sub, "done");
                    Task.Subtask s = new Task.Subtask(st); s.done = done; t.subtasks.add(s);
                }
                String rr = extractStringOrNull(o, "recurrence");
                if (rr != null) t.recurrence = RecurrenceRule.parse(rr);
                model.addElement(t);
            }
        } catch (Exception e) {
            System.err.println("Failed to load JSON: "+e.getMessage());
        }
    }

    // archive completed tasks to an external file and remove them from the model
    public static void archiveCompleted(Path archivePath, DefaultListModel<Task> model) {
        DefaultListModel<Task> completed = new DefaultListModel<>();
        for (int i = 0; i < model.size(); i++) if (model.get(i).completed) completed.addElement(model.get(i));
        if (completed.isEmpty()) return;
        saveJson(archivePath, completed);
        for (int i = model.size()-1; i>=0; i--) if (model.get(i).completed) model.remove(i);
    }

    // migrate any supported format to our current v2 line format
    public static void migrate(Path src, Path dest) {
        DefaultListModel<Task> tmp = new DefaultListModel<>();
        if (src.toString().endsWith(".json")) loadJson(src, tmp); else load(src, tmp);
        save(dest, tmp);
    }

    // --- tiny JSON helpers ---
    private static String quote(String s) { return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n","\\n") + '"'; }
    private static String quoteOrNull(String s) { return s == null ? "null" : quote(s); }
    private static String extractString(String obj, String key) { String v = extractStringOrNull(obj, key); return v==null?"":v; }
    private static String extractStringOrNull(String obj, String key) {
        int i = obj.indexOf('"'+key+'"'); if (i<0) return null; i=obj.indexOf(':',i); if(i<0) return null; i++;
        while (i<obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        if (obj.charAt(i)=='n') return null; // null
        if (obj.charAt(i)!='"') return null; i++; StringBuilder sb=new StringBuilder();
        while (i<obj.length()) { char ch=obj.charAt(i++); if(ch=='\\'){ if(i<obj.length()) { char esc=obj.charAt(i++); if(esc=='n') sb.append('\n'); else sb.append(esc);} } else if(ch=='"'){ break; } else sb.append(ch);} return sb.toString();
    }
    private static boolean extractBoolean(String obj, String key) { int i=obj.indexOf('"'+key+'"'); if(i<0) return false; i=obj.indexOf(':',i)+1; return obj.substring(i).trim().startsWith("true"); }
    private static Long extractLongOrNull(String obj, String key) { int i=obj.indexOf('"'+key+'"'); if(i<0) return null; i=obj.indexOf(':',i)+1; String rem=obj.substring(i).trim(); if(rem.startsWith("null")) return null; int j=0; while(j<rem.length() && (Character.isDigit(rem.charAt(j)))) j++; try { return Long.parseLong(rem.substring(0,j)); } catch(Exception e){ return null; } }
    private static List<String> extractStringArray(String obj, String key) {
        List<String> out=new ArrayList<>(); int i=obj.indexOf('"'+key+'"'); if(i<0) return out; i=obj.indexOf('[',i); int j=obj.indexOf(']',i); if(i<0||j<0) return out; String arr=obj.substring(i+1,j); int p=0; while(p<arr.length()){ while(p<arr.length()&&Character.isWhitespace(arr.charAt(p)))p++; if(p>=arr.length())break; if(arr.charAt(p)=='"'){ int q=p+1; StringBuilder sb=new StringBuilder(); while(q<arr.length()){ char ch=arr.charAt(q++); if(ch=='\\'){ if(q<arr.length()){ char esc=arr.charAt(q++); if(esc=='n') sb.append('\n'); else sb.append(esc);} } else if(ch=='"'){ break; } else sb.append(ch);} out.add(sb.toString()); p=q; } else { break; } while(p<arr.length()&&arr.charAt(p)!=',')p++; if(p<arr.length()&&arr.charAt(p)==',')p++; }
        return out;
    }
    private static List<String> extractObjectArray(String obj, String key) {
        List<String> out=new ArrayList<>(); int i=obj.indexOf('"'+key+'"'); if(i<0) return out; i=obj.indexOf('[',i); int j=obj.indexOf(']',i); if(i<0||j<0) return out; String arr=obj.substring(i+1,j); int depth=0,start=0; for(int k=0;k<arr.length();k++){ char ch=arr.charAt(k); if(ch=='{'){ if(depth++==0) start=k; } else if(ch=='}'){ if(--depth==0) out.add(arr.substring(start,k+1)); }} return out;
    }
}
