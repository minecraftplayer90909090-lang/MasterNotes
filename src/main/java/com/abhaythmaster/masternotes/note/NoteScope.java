package com.abhaythmaster.masternotes.note;

import net.minecraft.client.MinecraftClient;

import java.io.File;

public enum NoteScope {
    GLOBAL,
    SERVER;

    public File getSaveDirectory() {
        File base = new File(MinecraftClient.getInstance().runDirectory, ".masternotes/notes");
        if (this == GLOBAL) {
            File dir = new File(base, "global");
            dir.mkdirs();
            return dir;
        }
        // SERVER scope — name dir after server IP or world name
        String name = getContextName();
        File dir = new File(base, name);
        dir.mkdirs();
        return dir;
    }

    public static String getContextName() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "sp_" + client.getServer().getSaveProperties().getLevelName()
                    .replaceAll("[^a-zA-Z0-9_-]", "_");
        } else if (client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().address
                    .replaceAll("[^a-zA-Z0-9_.-]", "_");
        }
        return "global";
    }

    public static NoteScope getCurrent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) return SERVER;
        return GLOBAL;
    }
}
