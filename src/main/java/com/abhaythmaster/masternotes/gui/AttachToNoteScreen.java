package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.note.Note;
import com.abhaythmaster.masternotes.screenshot.ScreenshotEntry;
import com.abhaythmaster.masternotes.screenshot.ScreenshotMetaManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

/**
 * Lists all notes so the user can pick one to attach the current screenshot to.
 */
public class AttachToNoteScreen extends Screen {

    private final Screen parent;
    private final ScreenshotEntry entry;
    private final List<Note> notes;
    private int scroll = 0;
    private static final int ROW_H = 22;

    public AttachToNoteScreen(Screen parent, ScreenshotEntry entry) {
        super(Text.literal("Attach to Note"));
        this.parent = parent;
        this.entry  = entry;
        this.notes  = Note.loadAll();
    }

    @Override
    public void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("← Cancel"),
                b -> client.setScreen(parent))
                .dimensions(8, 8, 70, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, "Pick a note to attach:", width / 2, 14, C_ACCENT);
        ctx.drawText(textRenderer, entry.getName(), width / 2 - textRenderer.getWidth(entry.getName()) / 2,
                26, C_SUBTEXT, false);

        int listTop = 40;
        ctx.enableScissor(0, listTop, width, height);
        int visible = (height - listTop) / ROW_H;
        for (int i = scroll; i < Math.min(notes.size(), scroll + visible + 1); i++) {
            Note n  = notes.get(i);
            int ry  = listTop + (i - scroll) * ROW_H;
            boolean hov = mx >= 10 && mx <= width - 10 && my >= ry && my <= ry + ROW_H - 2;
            ctx.fill(10, ry, width - 10, ry + ROW_H - 2, hov ? C_PANEL_LITE : C_PANEL);
            ctx.drawBorder(10, ry, width - 20, ROW_H - 2, hov ? C_ACCENT : C_BORDER);
            ctx.drawText(textRenderer, n.getTitle(), 16, ry + 6, C_TEXT, false);
            ctx.drawText(textRenderer, n.getPreview(40),
                    16 + textRenderer.getWidth(n.getTitle()) + 6, ry + 6, C_SUBTEXT, false);
        }
        ctx.disableScissor();
        if (notes.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No notes found.", width / 2, height / 2, C_SUBTEXT);
        }
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int listTop = 40;
        int visible = (height - listTop) / ROW_H;
        for (int i = scroll; i < Math.min(notes.size(), scroll + visible + 1); i++) {
            int ry = listTop + (i - scroll) * ROW_H;
            if (mx >= 10 && mx <= width - 10 && my >= ry && my <= ry + ROW_H - 2) {
                attachAndReturn(notes.get(i));
                return true;
            }
        }
        return false;
    }

    private void attachAndReturn(Note note) {
        String filename = entry.getName();
        if (!note.getMeta().attachedScreenshots.contains(filename)) {
            note.getMeta().attachedScreenshots.add(filename);
            note.save();
        }
        client.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        scroll = Math.max(0, Math.min(Math.max(0, notes.size() - (height - 40) / ROW_H),
                scroll - (int) vAmt));
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
