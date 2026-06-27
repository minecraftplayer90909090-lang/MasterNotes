package com.abhaythmaster.masternotes.note;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Note {

    private String title;
    private String text;
    private NoteScope scope;
    private File file;          // may be null for brand-new unsaved notes
    private NoteMeta meta;

    // ── constructors ─────────────────────────────────────────────────────────

    /** Create a new, unsaved note */
    public Note(String title, String text, NoteScope scope) {
        this.title = title;
        this.text = text;
        this.scope = scope;
        this.meta = new NoteMeta();
    }

    /** Load an existing note from disk */
    public Note(File file) {
        this.file = file;
        this.scope = NoteScope.GLOBAL; // default; overridden later if needed
        reload();
        this.meta = NoteMeta.loadFor(file);
    }

    private void reload() {
        this.title = file.getName().replace(".txt", "");
        try {
            this.text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.text = "";
        }
    }

    // ── persistence ──────────────────────────────────────────────────────────

    public void save() {
        if (file == null) {
            file = resolveNewFile();
        }
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            meta.saveFor(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Rename = delete old file, update path, save under new name */
    public void saveAs(String newTitle) {
        if (file != null && file.exists()) {
            meta.deleteFor(file);
            file.delete();
        }
        this.title = newTitle;
        this.file = null;
        save();
    }

    public void delete() {
        if (file != null) {
            meta.deleteFor(file);
            file.delete();
        }
    }

    private File resolveNewFile() {
        File dir = scope.getSaveDirectory();
        String safeName = title.replaceAll("[^a-zA-Z0-9 _-]", "_").trim();
        if (safeName.isEmpty()) safeName = "Note";
        File f = new File(dir, safeName + ".txt");
        int i = 1;
        while (f.exists()) {
            f = new File(dir, safeName + "_" + (i++) + ".txt");
        }
        return f;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    public String getPreview(int maxLen) {
        if (text == null || text.isBlank()) return "(empty)";
        String flat = text.replace('\n', ' ').trim();
        return flat.length() <= maxLen ? flat : flat.substring(0, maxLen - 3) + "...";
    }

    public long getLastModified() {
        return file != null && file.exists() ? file.lastModified() : 0;
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getTitle()     { return title; }
    public String getText()      { return text; }
    public NoteScope getScope()  { return scope; }
    public File getFile()        { return file; }
    public NoteMeta getMeta()    { return meta; }

    public void setTitle(String t) { this.title = t; }
    public void setText(String t)  { this.text = t; }
    public void setScope(NoteScope s) { this.scope = s; }

    // ── static helpers ────────────────────────────────────────────────────────

    public static List<Note> loadAll() {
        List<Note> notes = new ArrayList<>();
        for (NoteScope scope : NoteScope.values()) {
            File dir = scope.getSaveDirectory();
            if (!dir.exists()) continue;
            File[] files = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".txt"));
            if (files == null) continue;
            for (File f : files) notes.add(new Note(f));
        }
        notes.sort(Comparator.comparingLong(Note::getLastModified).reversed());
        return notes;
    }
}
