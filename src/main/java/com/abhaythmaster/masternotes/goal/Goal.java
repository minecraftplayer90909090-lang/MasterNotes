package com.abhaythmaster.masternotes.goal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Goal {

    private String id;
    private String title;
    private String description;
    private GoalStatus status;
    private int progress;           // 0-100
    private String deadline;        // ISO date "YYYY-MM-DD" or empty
    private List<String> subtasks;
    private List<Boolean> subtaskDone;

    // default constructor required for Gson
    public Goal() {
        this.id = UUID.randomUUID().toString();
        this.title = "New Goal";
        this.description = "";
        this.status = GoalStatus.ACTIVE;
        this.progress = 0;
        this.deadline = "";
        this.subtasks = new ArrayList<>();
        this.subtaskDone = new ArrayList<>();
    }

    public Goal(String title) {
        this();
        this.title = title;
    }

    // ── subtask helpers ───────────────────────────────────────────────────────

    public void addSubtask(String name) {
        subtasks.add(name);
        subtaskDone.add(false);
        recalcProgress();
    }

    public void removeSubtask(int index) {
        if (index >= 0 && index < subtasks.size()) {
            subtasks.remove(index);
            subtaskDone.remove(index);
            recalcProgress();
        }
    }

    public void setSubtaskDone(int index, boolean done) {
        if (index >= 0 && index < subtaskDone.size()) {
            subtaskDone.set(index, done);
            recalcProgress();
        }
    }

    private void recalcProgress() {
        if (subtasks.isEmpty()) return;
        long done = subtaskDone.stream().filter(b -> b).count();
        progress = (int) (done * 100L / subtasks.size());
        if (progress == 100 && status == GoalStatus.ACTIVE)
            status = GoalStatus.COMPLETED;
    }

    // ── deadline helpers ──────────────────────────────────────────────────────

    public boolean isDeadlineNear() {
        if (deadline == null || deadline.isEmpty()) return false;
        try {
            LocalDate d = LocalDate.parse(deadline);
            LocalDate now = LocalDate.now();
            return !now.isAfter(d) && now.plusDays(3).isAfter(d);
        } catch (Exception e) { return false; }
    }

    public boolean isDeadlinePassed() {
        if (deadline == null || deadline.isEmpty()) return false;
        try { return LocalDate.now().isAfter(LocalDate.parse(deadline)); }
        catch (Exception e) { return false; }
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public GoalStatus getStatus()      { return status; }
    public int getProgress()           { return progress; }
    public String getDeadline()        { return deadline; }
    public List<String> getSubtasks()  { return subtasks; }
    public List<Boolean> getSubtaskDone() { return subtaskDone; }

    public void setTitle(String v)       { this.title = v; }
    public void setDescription(String v) { this.description = v; }
    public void setStatus(GoalStatus v)  { this.status = v; }
    public void setProgress(int v)       { this.progress = Math.max(0, Math.min(100, v)); }
    public void setDeadline(String v)    { this.deadline = v == null ? "" : v.trim(); }
}
