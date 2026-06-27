package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.screenshot.ScreenshotEntry;
import com.abhaythmaster.masternotes.screenshot.ScreenshotMeta;
import com.abhaythmaster.masternotes.screenshot.ScreenshotMetaManager;
import com.abhaythmaster.masternotes.screenshot.SortMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

public class ScreenshotsScreen extends Screen {

    private final Screen parent;

    private List<ScreenshotEntry> all       = new ArrayList<>();
    private List<ScreenshotEntry> displayed = new ArrayList<>();
    private SortMode sortMode   = SortMode.DATE_DESC;
    private int scrollOffset    = 0;
    private int selectedIndex   = -1;

    // grid layout
    private static final int COLS    = 3;
    private static final int THUMB_W = 120;
    private static final int THUMB_H = 72;
    private static final int GAP     = 8;
    private static final int LIST_TOP = 58;

    private TextFieldWidget searchField;

    // drag-to-reorder state
    private int dragFrom = -1;
    private int dragToX, dragToY;

    public ScreenshotsScreen(Screen parent) {
        super(Text.literal("Screenshots"));
        this.parent = parent;
    }

    @Override
    public void init() {
        all = ScreenshotMetaManager.loadAll();
        applySort();

        // sort cycle button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Sort: " + sortMode.label),
                b -> {
                    sortMode = sortMode.next();
                    b.setMessage(Text.literal("Sort: " + sortMode.label));
                    applySort();
                }).dimensions(8, 8, 110, 16).build());

        // shuffle
        addDrawableChild(ButtonWidget.builder(Text.literal("🔀 Shuffle"),
                b -> {
                    ScreenshotMetaManager.shuffle(all);
                    applySort();
                }).dimensions(122, 8, 80, 16).build());

        // search
        searchField = new TextFieldWidget(textRenderer, width / 2 - 80, 8, 160, 16,
                Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Server / filename..."));
        searchField.setChangedListener(q -> { scrollOffset = 0; applyFilter(q); });
        addDrawableChild(searchField);

        // back
        addDrawableChild(ButtonWidget.builder(Text.literal("← Back"),
                b -> client.setScreen(parent))
                .dimensions(width - 70, 8, 62, 16).build());
    }

    private void applySort() {
        ScreenshotMetaManager.sort(all, sortMode);
        applyFilter(searchField != null ? searchField.getText() : "");
    }

    private void applyFilter(String q) {
        if (q == null || q.isBlank()) {
            displayed = new ArrayList<>(all);
            return;
        }
        String lq = q.toLowerCase(Locale.ROOT);
        displayed = all.stream().filter(e ->
                e.getName().toLowerCase(Locale.ROOT).contains(lq) ||
                e.getMeta().getDisplaySource().toLowerCase(Locale.ROOT).contains(lq) ||
                e.getMeta().tags.stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(lq))
        ).toList();
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, "✦ SCREENSHOTS ✦", width / 2, 28, 0xFF00FFAA);
        ctx.drawText(textRenderer, displayed.size() + " found", 8, 30, C_SUBTEXT, false);

        int gridW  = COLS * (THUMB_W + GAP) - GAP;
        int startX = (width - gridW) / 2;

        int rowH   = THUMB_H + 38; // thumbnail + metadata area
        int visRows = (height - LIST_TOP) / (rowH + GAP) + 1;
        int firstRow = scrollOffset;
        int lastRow  = firstRow + visRows;

        ctx.enableScissor(0, LIST_TOP, width, height);

        for (int i = firstRow * COLS; i < Math.min(displayed.size(), lastRow * COLS); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx  = startX + col * (THUMB_W + GAP);
            int cy  = LIST_TOP + (row - firstRow) * (rowH + GAP);

            renderThumb(ctx, displayed.get(i), cx, cy, i, mx, my);
        }

        ctx.disableScissor();

        if (displayed.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "No screenshots found.", width / 2, height / 2, C_SUBTEXT);
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderThumb(DrawContext ctx, ScreenshotEntry e, int x, int y,
                              int idx, int mx, int my) {
        boolean hovered = mx >= x && mx <= x + THUMB_W && my >= y && my <= y + THUMB_H + 38;
        boolean selected = idx == selectedIndex;

        int border = selected ? C_ACCENT : hovered ? 0xFF80FFCC : C_BORDER;
        ctx.fill(x, y, x + THUMB_W, y + THUMB_H, 0xFF111122);
        ctx.drawBorder(x, y, THUMB_W, THUMB_H, border);

        // load texture lazily
        if (!e.isTextureLoaded()) ScreenshotMetaManager.loadTexture(e);

        // draw thumbnail
        Identifier tid = e.getTextureId();
        if (tid != null) {
            ctx.drawTexture(tid, x + 1, y + 1, 0, 0, THUMB_W - 2, THUMB_H - 2, THUMB_W - 2, THUMB_H - 2);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, "Loading...", x + THUMB_W / 2, y + THUMB_H / 2 - 4, C_SUBTEXT);
        }

        // favorite star overlay
        if (e.getMeta().favorite) {
            ctx.drawText(textRenderer, "⭐", x + THUMB_W - 12, y + 2, 0xFFFFD700, false);
        }

        // metadata strip below thumbnail
        int my2 = y + THUMB_H + 2;
        ctx.fill(x, my2, x + THUMB_W, my2 + 36, C_PANEL);

        // server / world
        String src = e.getMeta().getDisplaySource();
        if (src.length() > 18) src = src.substring(0, 15) + "...";
        ctx.drawText(textRenderer, src, x + 2, my2 + 1, 0xFF00FFAA, false);

        // date
        String date = e.getMeta().getDateString();
        ctx.drawText(textRenderer, date, x + 2, my2 + 11, C_SUBTEXT, false);

        // filename (truncated)
        String name = e.getName().length() > 18
                ? e.getName().substring(0, 15) + "..." : e.getName();
        ctx.drawText(textRenderer, name, x + 2, my2 + 21, C_SUBTEXT, false);

        // caption if set
        if (!e.getMeta().caption.isEmpty()) {
            String cap = e.getMeta().caption.length() > 18
                    ? e.getMeta().caption.substring(0, 15) + "..." : e.getMeta().caption;
            ctx.drawText(textRenderer, "\"" + cap + "\"", x + 2, my2 + 31, C_TEXT, false);
        }
    }

    // ── mouse events ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        int gridW  = COLS * (THUMB_W + GAP) - GAP;
        int startX = (width - gridW) / 2;
        int rowH   = THUMB_H + 38;
        int firstRow = scrollOffset;

        for (int i = firstRow * COLS; i < displayed.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx  = startX + col * (THUMB_W + GAP);
            int cy  = LIST_TOP + (row - firstRow) * (rowH + GAP);

            if (mx >= cx && mx <= cx + THUMB_W && my >= cy && my <= cy + rowH) {
                if (btn == 0) { // left click = open enlarged
                    selectedIndex = i;
                    client.setScreen(new EnlargedScreenshotScreen(this, displayed, i));
                }
                if (btn == 1) { // right click = toggle favorite
                    ScreenshotMeta meta = displayed.get(i).getMeta();
                    meta.favorite = !meta.favorite;
                    ScreenshotMetaManager.saveMeta(displayed.get(i).getFile(), meta);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int rowH    = THUMB_H + 38 + GAP;
        int rows    = (int) Math.ceil((double) displayed.size() / COLS);
        int visRows = (height - LIST_TOP) / rowH;
        int maxScroll = Math.max(0, rows - visRows);
        scrollOffset  = Math.max(0, Math.min(maxScroll, scrollOffset - (int) vAmt));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { client.setScreen(parent); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }
}
