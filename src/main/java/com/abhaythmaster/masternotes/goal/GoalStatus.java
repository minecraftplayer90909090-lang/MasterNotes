package com.abhaythmaster.masternotes.goal;

public enum GoalStatus {
    ACTIVE    (0xFF4FC3F7, "Active"),
    COMPLETED (0xFF66BB6A, "Completed"),
    FAILED    (0xFFEF5350, "Failed");

    public final int color;
    public final String label;

    GoalStatus(int color, String label) {
        this.color = color;
        this.label = label;
    }

    public GoalStatus next() {
        GoalStatus[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
