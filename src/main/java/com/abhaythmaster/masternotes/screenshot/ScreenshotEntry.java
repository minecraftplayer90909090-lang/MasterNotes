package com.abhaythmaster.masternotes.screenshot;

import net.minecraft.util.Identifier;

import java.io.File;

public class ScreenshotEntry {

    private final File file;
    private ScreenshotMeta meta;
    private Identifier textureId;   // null until texture is loaded

    public ScreenshotEntry(File file, ScreenshotMeta meta) {
        this.file = file;
        this.meta = meta;
    }

    public File getFile()              { return file; }
    public ScreenshotMeta getMeta()    { return meta; }
    public void setMeta(ScreenshotMeta m) { this.meta = m; }

    public Identifier getTextureId()   { return textureId; }
    public void setTextureId(Identifier id) { this.textureId = id; }
    public boolean isTextureLoaded()   { return textureId != null; }

    public long getSize()              { return file.length(); }
    public String getName()            { return file.getName(); }
}
