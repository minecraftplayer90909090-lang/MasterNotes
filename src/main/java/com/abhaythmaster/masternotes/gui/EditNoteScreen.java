package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.gui.widget.MultilineTextWidget;
import com.abhaythmaster.masternotes.note.Note;
import com.abhaythmaster.masternotes.note.NoteScope;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

public class EditNoteScreen extends Screen {

    private final Screen parent;
    private Note note;
    private final boolean isNew;

    private TextFieldWidget titleField;
    private MultilineTextWidget bodyField;
    private ButtonWidget scopeBtn;
    private NoteScope currentScope;

    // color picker
    private static final String[] COLORS = {"#00D4FF","#FF4444","#FFB300","#4CAF50","#B040FF","#FF6D00"};
    private int colorIdx = 0;

    public EditNoteScreen(Screen parent, Note note) {
        super(Text.literal(note == null ? "New Note" : "Edit Note"));
        this.parent = parent;
        this.isNew  = (note == null);
        this.note   = isNew ? new Note("New Note", "", NoteScope.getCurrent()) : note;
        this.currentScope = this.note.getScope();
    }

    @Override
    public void init() {
        int leftW  = 120;
        int rightX = leftW + 10;
        int rightW = width - rightX - 8;

        // ── left sidebar buttons ──────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal("💾  Save"),
                b -> saveAndReturn())
                .dimensions(8, 8, leftW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("✖  Cancel"),
                b -> client.setScreen(parent))
                .dimensions(8, 32, leftW, 20).build());

        scopeBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal("Scope: " + currentScope.name()),
                b -> {
                    currentScope = (currentScope == NoteScope.GLOBAL)
                            ? NoteScope.SERVER : NoteScope.GLOBAL;
                    scopeBtn.setMessage(Text.literal("Scope: " + currentScope.name()));
                }).dimensions(8, 56, leftW, 20).build());

        // color cycle
        addDrawableChild(ButtonWidget.builder(Text.literal("🎨 Color"),
                b -> {
                    colorIdx = (colorIdx + 1) % COLORS.length;
                    note.getMeta().color = COLORS[colorIdx];
                }).dimensions(8, 80, leftW, 20).build());

        // insert helpers
        addDrawableChild(ButtonWidget.builder(Text.literal("📍 Coords"),
                b -> insertCoords())
                .dimensions(8, 110, leftW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("🌿 Biome"),
                b -> insertBiome())
                .dimensions(8, 134, leftW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("📸 Screenshots"),
                b -> client.setScreen(new ScreenshotsScreen(this)))
                .dimensions(8, 158, leftW, 20).build());

        // delete (only for existing notes)
        if (!isNew) {
            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete"),
                    b -> {
                        note.delete();
                        client.setScreen(parent);
                    }).dimensions(8, height - 30, leftW, 20).build());
        }

        // ── title field ───────────────────────────────────────────────────────
        titleField = new TextFieldWidget(textRenderer, rightX, 8, rightW, 18,
                Text.literal("Title"));
        titleField.setMaxLength(128);
        titleField.setText(note.getTitle());
        addDrawableChild(titleField);

        // ── multiline body ────────────────────────────────────────────────────
        bodyField = new MultilineTextWidget(textRenderer, rightX, 30, rightW, height - 38);
        bodyField.setValue(note.getText());
        addDrawableChild(bodyField);

        // sync color index
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i].equals(note.getMeta().color)) { colorIdx = i; break; }
        }
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);

        int leftW = 120;
        // left panel background
        ctx.fill(0, 0, leftW + 4, height, C_PANEL);
        ctx.fill(leftW + 4, 0, leftW + 5, height, C_BORDER);

        // top accent line in note's color
        int accent = C_ACCENT;
        try { accent = (int) Long.parseLong(COLORS[colorIdx].replace("#", ""), 16) | 0xFF000000; }
        catch (Exception ignored) {}
        ctx.fill(leftW + 5, 0, width, 2, accent);

        // section labels
        ctx.drawText(textRenderer, "TITLE", leftW + 10, 0, C_SUBTEXT, false);
        ctx.drawText(textRenderer, "BODY",  leftW + 10, 22, C_SUBTEXT, false);

        super.render(ctx, mx, my, delta);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void saveAndReturn() {
        note.setTitle(titleField.getText().isBlank() ? "Untitled" : titleField.getText());
        note.setText(bodyField.getValue());
        note.setScope(currentScope);
        note.getMeta().color = COLORS[colorIdx];
        note.save();
        client.setScreen(parent);
    }

    private void insertCoords() {
        if (client.player != null) {
            bodyField.insertText(String.format("[%d, %d, %d]",
                    (int) client.player.getX(),
                    (int) client.player.getY(),
                    (int) client.player.getZ()));
        }
    }

    private void insertBiome() {
        if (client.world != null && client.player != null) {
            var biome = client.world.getBiome(client.player.getBlockPos());
            biome.getKey().ifPresent(k ->
                    bodyField.insertText("[" + k.getValue().getPath() + "]"));
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (bodyField.keyPressed(key, scan, mods)) return true;
        if (titleField.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (bodyField.charTyped(chr, mods)) return true;
        return super.charTyped(chr, mods);
    }

    @Override
    public boolean shouldPause() { return false; }
}
