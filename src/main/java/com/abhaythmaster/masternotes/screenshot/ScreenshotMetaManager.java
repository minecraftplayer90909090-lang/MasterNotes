package com.abhaythmaster.masternotes.screenshot;

import com.abhaythmaster.masternotes.MasterNotesClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class ScreenshotMetaManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static int textureCounter = 0;

    // ── meta I/O ─────────────────────────────────────────────────────────────

    public static ScreenshotMeta loadMeta(File png) {
        File mf = metaFile(png);
        if (mf.exists()) {
            try (Reader r = new FileReader(mf)) {
                ScreenshotMeta m = GSON.fromJson(r, ScreenshotMeta.class);
                if (m != null) { m.filename = png.getName(); return m; }
            } catch (Exception ignored) {}
        }
        ScreenshotMeta m = new ScreenshotMeta();
        m.filename  = png.getName();
        m.timestamp = png.lastModified();
        return m;
    }

    public static void saveMeta(File png, ScreenshotMeta meta) {
        File mf = metaFile(png);
        try (Writer w = new FileWriter(mf)) {
            GSON.toJson(meta, w);
        } catch (IOException e) {
            MasterNotesClient.LOGGER.error("Could not save screenshot meta", e);
        }
    }

    private static File metaFile(File png) {
        return new File(png.getParent(), png.getName() + ".meta.json");
    }

    // ── capture context when F2 is pressed ───────────────────────────────────

    /**
     * Called by ScreenshotCaptureMixin right after a screenshot is saved.
     * Finds the newest PNG in the screenshots dir and writes a .meta.json beside it.
     */
    public static void captureContext(File screenshotsDir) {
        if (!screenshotsDir.exists()) return;
        File[] pngs = screenshotsDir.listFiles(f -> f.getName().endsWith(".png"));
        if (pngs == null || pngs.length == 0) return;

        // newest file = the one just saved
        File newest = Arrays.stream(pngs)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
        if (newest == null) return;
        // ignore if it was created more than 5 s ago
        if (System.currentTimeMillis() - newest.lastModified() > 5000) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ScreenshotMeta meta = new ScreenshotMeta();
        meta.filename  = newest.getName();
        meta.timestamp = newest.lastModified();

        if (client.isInSingleplayer() && client.getServer() != null) {
            meta.worldName = client.getServer().getSaveProperties().getLevelName();
        } else if (client.getCurrentServerEntry() != null) {
            meta.serverIp = client.getCurrentServerEntry().address;
        }

        saveMeta(newest, meta);
    }

    // ── loading entries ───────────────────────────────────────────────────────

    public static List<ScreenshotEntry> loadAll() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, "screenshots");
        List<ScreenshotEntry> list = new ArrayList<>();
        if (!dir.exists()) return list;
        File[] pngs = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".png"));
        if (pngs == null) return list;
        for (File f : pngs) list.add(new ScreenshotEntry(f, loadMeta(f)));
        return list;
    }

    public static void sort(List<ScreenshotEntry> list, SortMode mode) {
        switch (mode) {
            case DATE_DESC -> list.sort(Comparator.comparingLong(e -> -e.getMeta().timestamp));
            case DATE_ASC  -> list.sort(Comparator.comparingLong(e ->  e.getMeta().timestamp));
            case NAME_ASC  -> list.sort(Comparator.comparing(e -> e.getName().toLowerCase()));
            case NAME_DESC -> list.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
            case SIZE_DESC -> list.sort(Comparator.comparingLong(e -> -e.getSize()));
            case SERVER    -> list.sort(Comparator.comparing(e -> e.getMeta().getDisplaySource()));
            case CUSTOM    -> list.sort(Comparator.comparingInt(e -> e.getMeta().customOrder));
        }
    }

    public static void shuffle(List<ScreenshotEntry> list) {
        Collections.shuffle(list);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).getMeta().customOrder = i;
            saveMeta(list.get(i).getFile(), list.get(i).getMeta());
        }
    }

    // ── texture loading (call from render thread) ─────────────────────────────

    public static void loadTexture(ScreenshotEntry entry) {
        if (entry.isTextureLoaded()) return;
        try (InputStream is = Files.newInputStream(entry.getFile().toPath())) {
            NativeImage img = NativeImage.read(is);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            Identifier id = Identifier.of("masternotes", "screenshot_" + (textureCounter++));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            entry.setTextureId(id);
        } catch (Exception e) {
            MasterNotesClient.LOGGER.warn("Failed to load screenshot texture: {}", entry.getName());
        }
    }
}
