package com.abhaythmaster.masternotes.goal;

import com.abhaythmaster.masternotes.MasterNotesClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GoalManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Goal> goals = new ArrayList<>();

    public static List<Goal> getGoals() { return goals; }

    public static List<Goal> getGoalsByStatus(GoalStatus status) {
        if (status == null) return new ArrayList<>(goals);
        return goals.stream().filter(g -> g.getStatus() == status).toList();
    }

    // ── disk I/O ──────────────────────────────────────────────────────────────

    public static void load() {
        File file = goalsFile();
        if (!file.exists()) { goals = new ArrayList<>(); return; }
        try (Reader r = new FileReader(file)) {
            Type type = new TypeToken<List<Goal>>(){}.getType();
            List<Goal> loaded = GSON.fromJson(r, type);
            goals = loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) {
            MasterNotesClient.LOGGER.error("Failed to load goals", e);
            goals = new ArrayList<>();
        }
    }

    public static void save() {
        File file = goalsFile();
        try (Writer w = new FileWriter(file)) {
            GSON.toJson(goals, w);
        } catch (IOException e) {
            MasterNotesClient.LOGGER.error("Failed to save goals", e);
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public static void add(Goal g)    { goals.add(g); save(); }
    public static void remove(Goal g) { goals.removeIf(x -> x.getId().equals(g.getId())); save(); }
    public static void update(Goal g) {
        for (int i = 0; i < goals.size(); i++) {
            if (goals.get(i).getId().equals(g.getId())) { goals.set(i, g); break; }
        }
        save();
    }

    private static File goalsFile() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, ".masternotes/goals");
        dir.mkdirs();
        return new File(dir, "goals.json");
    }
}
