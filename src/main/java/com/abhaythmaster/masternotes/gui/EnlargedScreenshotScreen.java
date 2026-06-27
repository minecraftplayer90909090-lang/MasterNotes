package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.note.Note;
import com.abhaythmaster.masternotes.screenshot.ScreenshotEntry;
import com.abhaythmaster.masternotes.screenshot.ScreenshotMetaManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

public class EnlargedScreenshotScreen extends Screen {

    private final Screen parent;
    private final List<ScreenshotEntry> entries;
    private int index;

    private TextFieldWidget captionField;
    private TextFieldWidget tagField;

    public EnlargedScreenshotScreen(Screen parent, List<ScreenshotEntry> entries, int index) {
        super(Text.literal("Screenshot View"));
        this.parent  = parent;
        this.entries = entries;
        this.index   = index;
    }

    private ScreenshotEntry current() {
        return entries.get(index);
    }

    @Override
    public void init() {
        int panelW = 200;
        int imgW   = width - panelW - 20;
        int imgH   = height - 30;

        // navigation
        addDrawableChild(ButtonWidget.builder(Text.literal("◀ Prev"),
                b -> { if (index > 0) { index--; init(); } })
                .dimensions(8, height / 2 - 10, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Next ▶"),
                b -> { if (index < entries.size() - 1) { index++; init(); } })
                .dimensions(imgW - 42, height / 2 - 10, 50, 20).build());

        // back
        addDrawableChild(ButtonWidget.builder(Text.literal("✖ Close"),
                b -> client.setScreen(parent))
                .dimensions(imgW + 12, 8, panelW - 4, 18).build());

        // favorite toggle
        boolean fav = current().getMeta().favorite;
        addDrawableChild(ButtonWidget.builder(
                Text.literal(fav ? "★ Unfavorite" : "☆ Favorite"),
                b -> {
                    current().getMeta().favorite = !current().getMeta().favorite;
                    ScreenshotMetaManager.saveMeta(current().getFile(), current().getMeta());
                    init();
                }).dimensions(imgW + 12, 30, panelW - 4, 18).build());

        // delete
        addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete"),
                b -> {
                    current().getFile().delete();
                    entries.remove(index);
                    if (entries.isEmpty()) { client.setScreen(parent); return; }
                    index = Math.min(index, entries.size() - 1);
                    init();
                }).dimensions(imgW + 12, 52, panelW - 4, 18).build());

        // attach to note (simple: opens note list to pick)
        addDrawableChild(ButtonWidget.builder(Text.literal("📋 Attach to Note"),
                b -> client.setScreen(new AttachToNoteScreen(this, current())))
                .dimensions(imgW + 12, 74, panelW - 4, 18).build());

        // caption field
        captionField = new TextFieldWidget(textRenderer, imgW + 12, 110, panelW - 4, 18,
                Text.literal("Caption"));
        captionField.setMaxLength(120);
        captionField.setText(current().getMeta().caption);
        captionField.setPlaceholder(Text.literal("Add a caption..."));
        addDrawableChild(captionField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save Caption"),
                b -> {
                    current().getMeta().caption = captionField.getText();
                    ScreenshotMetaManager.saveMeta(current().getFile(), current().getMeta());
                }).dimensions(imgW + 12, 132, panelW - 4, 16).build());

        // tag field
        tagField = new TextFieldWidget(textRenderer, imgW + 12, 165, panelW - 30, 18,
                Text.literal("Tag"));
        tagField.setPlaceholder(Text.literal("Add tag..."));
        addDrawableChild(tagField);

        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                b -> {
                    String tag = tagField.getText().trim();
                    if (!tag.isEmpty() && !current().getMeta().tags.contains(tag)) {
                        current().getMeta().tags.add(tag);
                        ScreenshotMetaManager.saveMeta(current().getFile(), current().getMeta());
                        tagField.setText("");
                    }
                }).dimensions(imgW + panelW - 16, 165, 18, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xFF050510);

        int panelW = 200;
        int imgX   = 8, imgY = 8;
        int imgW   = width - panelW - 20;
        int imgH   = height - 16;

        // image area background
        ctx.fill(imgX, imgY, imgX + imgW, imgY + imgH, 0xFF0A0A1A);
        ctx.drawBorder(imgX, imgY, imgW, imgH, C_BORDER);

        // draw screenshot
        if (!current().isTextureLoaded()) ScreenshotMetaManager.loadTexture(current());
        Identifier tid = current().getTextureId();
        if (tid != null) {
            // fit image keeping aspect ratio
            int iw = imgW - 2, ih = imgH - 2;
            ctx.drawTexture(tid, imgX + 1, imgY + 1, 0, 0, iw, ih, iw, ih);
        }

        // right info panel
        int px = imgX + imgW + 4;
        ctx.fill(px, imgY, px + panelW - 4, imgY + imgH, C_PANEL);
        ctx.drawBorder(px, imgY, panelW - 4, imgH, C_BORDER);
        ctx.fill(px, imgY, px + panelW - 4, imgY + 2, C_ACCENT);

        // metadata
        int ty = 96;
        ctx.drawText(textRenderer, "SOURCE", px + 4, ty, C_ACCENT, false);
        ty += 10;
        ctx.drawText(textRenderer, current().getMeta().getDisplaySource(), px + 4, ty, 0xFF00FFAA, false);
        ty += 10;
        ctx.drawText(textRenderer, current().getMeta().getDateString(), px + 4, ty, C_SUBTEXT, false);
        ty += 10;
        ctx.drawText(textRenderer, current().getName(), px + 4, ty, C_SUBTEXT, false);

        // caption label
        ty = 100 + 12;
        ctx.drawText(textRenderer, "CAPTION", px + 4, 100, C_ACCENT, false);

        // tags section
        ctx.drawText(textRenderer, "TAGS", px + 4, 153, C_ACCENT, false);
        int tagY = 186;
        for (String tag : current().getMeta().tags) {
            int tw = textRenderer.getWidth(tag) + 8;
            ctx.fill(px + 4, tagY, px + 4 + tw, tagY + 12, 0xFF1E1E3A);
            ctx.drawBorder(px + 4, tagY, tw, 12, C_BORDER);
            ctx.drawText(textRenderer, tag, px + 8, tagY + 2, C_ACCENT2, false);
            // × to remove
            ctx.drawText(textRenderer, "×", px + 4 + tw + 2, tagY + 2, C_RED, false);
            tagY += 16;
            if (tagY > imgY + imgH - 30) break;
        }

        // nav position indicator
        ctx.drawCenteredTextWithShadow(textRenderer,
                (index + 1) + " / " + entries.size(), imgX + imgW / 2, imgY + imgH - 14, C_SUBTEXT);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        // click × on tag chips
        int panelW = 200;
        int px     = width - panelW - 8;
        int tagY   = 186;
        for (int i = 0; i < current().getMeta().tags.size(); i++) {
            String tag = current().getMeta().tags.get(i);
            int tw = textRenderer.getWidth(tag) + 8;
            int xBtn = px + 4 + tw + 2;
            if (mx >= xBtn && mx <= xBtn + 8 && my >= tagY && my <= tagY + 12) {
                current().getMeta().tags.remove(i);
                ScreenshotMetaManager.saveMeta(current().getFile(), current().getMeta());
                return true;
            }
            tagY += 16;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { client.setScreen(parent); return true; }
        if (key == GLFW.GLFW_KEY_LEFT && index > 0) { index--; init(); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT && index < entries.size() - 1) { index++; init(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }
}
