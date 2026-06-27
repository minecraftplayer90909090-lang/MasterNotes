package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.note.Note;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

public class NotesListScreen extends Screen {

    private final Screen parent;
    private List<Note> allNotes  = new ArrayList<>();
    private List<Note> filtered  = new ArrayList<>();
    private TextFieldWidget searchField;
    private int scrollOffset = 0;

    private static final int CARD_H  = 44;
    private static final int CARD_GAP = 4;
    private static final int LIST_X  = 10;

    public NotesListScreen(Screen parent) {
        super(Text.literal("Notes"));
        this.parent = parent;
    }

    @Override
    public void init() {
        allNotes = Note.loadAll();
        filtered = new ArrayList<>(allNotes);

        // search bar
        searchField = new TextFieldWidget(textRenderer, width / 2 - 100, 10, 200, 18,
                Text.literal("Search..."));
        searchField.setPlaceholder(Text.literal("Search notes..."));
        searchField.setChangedListener(q -> updateFilter(q));
        addDrawableChild(searchField);

        // new note
        addDrawableChild(ButtonWidget.builder(Text.literal("+ New Note"),
                b -> client.setScreen(new EditNoteScreen(this, null)))
                .dimensions(width - 90, 10, 80, 18).build());

        // back
        addDrawableChild(ButtonWidget.builder(Text.literal("← Back"),
                b -> client.setScreen(parent))
                .dimensions(8, 10, 60, 18).build());
    }

    private void updateFilter(String q) {
        scrollOffset = 0;
        if (q == null || q.isBlank()) { filtered = new ArrayList<>(allNotes); return; }
        String lq = q.toLowerCase();
        filtered = allNotes.stream()
                .filter(n -> n.getTitle().toLowerCase().contains(lq)
                          || n.getText().toLowerCase().contains(lq))
                .toList();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);

        ctx.drawCenteredTextWithShadow(textRenderer, "✦ MY NOTES ✦", width / 2, 35, C_ACCENT);

        int listW   = width - LIST_X * 2;
        int listTop = 55;
        int listBot = height - 10;
        int visible = (listBot - listTop) / (CARD_H + CARD_GAP);

        ctx.enableScissor(LIST_X, listTop, LIST_X + listW, listBot);

        for (int i = scrollOffset; i < Math.min(filtered.size(), scrollOffset + visible + 1); i++) {
            Note n  = filtered.get(i);
            int  cy = listTop + (i - scrollOffset) * (CARD_H + CARD_GAP);
            if (cy > listBot) break;
            renderNoteCard(ctx, n, LIST_X, cy, listW, CARD_H, mx, my);
        }

        ctx.disableScissor();

        // empty state
        if (filtered.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No notes found.", width / 2, height / 2, C_SUBTEXT);
        }

        // scroll hint
        if (filtered.size() > visible) {
            ctx.drawText(textRenderer,
                    Text.literal("↑↓ scroll  •  " + (scrollOffset + 1) + "/" + filtered.size()),
                    LIST_X, listBot - 10, C_SUBTEXT, false);
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderNoteCard(DrawContext ctx, Note n, int x, int y, int w, int h,
                                int mx, int my) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg  = hovered ? C_PANEL_LITE : C_PANEL;
        int bdr = hovered ? C_ACCENT     : C_BORDER;

        // parse accent color from meta
        int accentColor = C_ACCENT;
        try { accentColor = (int) Long.parseLong(
                n.getMeta().color.replace("#", ""), 16) | 0xFF000000; }
        catch (Exception ignored) {}

        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawBorder(x, y, w, h, bdr);
        ctx.fill(x, y, x + 3, y + h, accentColor); // left color strip

        ctx.drawText(textRenderer, n.getTitle(), x + 8, y + 6, C_TEXT, true);
        ctx.drawText(textRenderer, n.getPreview(60), x + 8, y + 18, C_SUBTEXT, false);

        // scope badge
        String scope = n.getScope().name();
        ctx.drawText(textRenderer, scope, x + 8, y + 30, 0xFF7E57C2, false);

        // date
        if (n.getLastModified() > 0) {
            String d = new java.text.SimpleDateFormat("MM/dd HH:mm")
                    .format(new java.util.Date(n.getLastModified()));
            ctx.drawText(textRenderer, d, x + w - textRenderer.getWidth(d) - 4, y + 30, C_SUBTEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int listTop = 55;
        int visible = (height - 10 - listTop) / (CARD_H + CARD_GAP);

        for (int i = scrollOffset; i < Math.min(filtered.size(), scrollOffset + visible + 1); i++) {
            int cy = listTop + (i - scrollOffset) * (CARD_H + CARD_GAP);
            int cx = LIST_X, cw = width - LIST_X * 2;
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + CARD_H) {
                if (btn == 0) client.setScreen(new EditNoteScreen(this, filtered.get(i)));
                if (btn == 1) { // right-click = delete confirm
                    filtered.get(i).delete();
                    allNotes = Note.loadAll();
                    updateFilter(searchField.getText());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int max = Math.max(0, filtered.size() - (height - 65) / (CARD_H + CARD_GAP));
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) vAmt));
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
