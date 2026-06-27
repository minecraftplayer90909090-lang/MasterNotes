package com.abhaythmaster.masternotes.note;

import com.abhaythmaster.masternotes.MasterNotesClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** Sidecar .meta.json stored next to each note .txt file */
public class NoteMeta {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Hex color like #FF4444 — used for the note card accent */
    public String color = "#00D4FF";
    public List<String> tags = new ArrayList<>();
    /** Screenshot filenames (just names, not full paths) attached to this note */
    public List<String> attachedScreenshots = new ArrayList<>();

    // ── persistence ──────────────────────────────────────────────────────────

    public static NoteMeta loadFor(File noteFile) {
        File meta = metaFileFor(noteFile);
        if (meta.exists()) {
            try (Reader r = new FileReader(meta)) {
                NoteMeta m = GSON.fromJson(r, NoteMeta.class);
                if (m != null) return m;
            } catch (Exception e) {
                MasterNotesClient.LOGGER.warn("Could not read note meta: {}", meta.getName());
            }
        }
        return new NoteMeta();
    }

    public void saveFor(File noteFile) {
        File meta = metaFileFor(noteFile);
        try (Writer w = new FileWriter(meta)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            MasterNotesClient.LOGGER.error("Could not save note meta", e);
        }
    }

    public void deleteFor(File noteFile) {
        metaFileFor(noteFile).delete();
    }

    private static File metaFileFor(File noteFile) {
        return new File(noteFile.getParent(), noteFile.getName() + ".meta.json");
    }
}
