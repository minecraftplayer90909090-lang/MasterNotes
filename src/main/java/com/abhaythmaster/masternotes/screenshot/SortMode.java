package com.abhaythmaster.masternotes.screenshot;

public enum SortMode {
    DATE_DESC ("Newest First"),
    DATE_ASC  ("Oldest First"),
    NAME_ASC  ("Name A→Z"),
    NAME_DESC ("Name Z→A"),
    SIZE_DESC ("Largest First"),
    SERVER    ("By Server"),
    CUSTOM    ("Custom Order");

    public final String label;

    SortMode(String label) { this.label = label; }

    public SortMode next() {
        SortMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
