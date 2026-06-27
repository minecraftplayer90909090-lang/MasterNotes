package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.goal.Goal;
import com.abhaythmaster.masternotes.goal.GoalManager;
import com.abhaythmaster.masternotes.note.Note;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Main dashboard: three panels — Notes | Goals | Screenshots
 */
public class MasterDashboardScreen extends Screen {

    // colour palette (glassmorphism dark-cyber)
    static final int C_BG         = 0xFF0D0D1A;
    static final int C_PANEL      = 0xCC111124;
    static final int C_PANEL_LITE = 0xCC1A1A38;
    static final int C_BORDER     = 0x4D7EB8FF;
    static final int C_ACCENT     = 0xFF00D4FF;  // cyan
    static final int C_ACCENT2    = 0xFFB040FF;  // purple
    static final int C_TEXT       = 0xFFE0E0FF;
    static final int C_SUBTEXT    = 0xFFAAAAAA;
    static final int C_GREEN      = 0xFF4CAF50;
    static final int C_YELLOW     = 0xFFFFB300;
    static final int C_RED        = 0xFFEF5350;

    public MasterDashboardScreen() {
        super(Text.literal("Master Notes"));
    }

    @Override
    public void init() {
        int pw   = (width - 30) / 3;   // panel width
        int ph   = height - 80;         // panel height
        int py   = 45;
        int btnH = 20;

        // ── Notes panel button ──
        addDrawableChild(ButtonWidget.builder(
                Text.literal("📋  Notes"),
                b -> client.setScreen(new NotesListScreen(this)))
                .dimensions(10, py + ph + 8, pw, btnH).build());

        // ── Goals panel button ──
        addDrawableChild(ButtonWidget.builder(
                Text.literal("🎯  Goals"),
                b -> client.setScreen(new GoalsScreen(this)))
                .dimensions(10 + pw + 5, py + ph + 8, pw, btnH).build());

        // ── Screenshots panel button ──
        addDrawableChild(ButtonWidget.builder(
                Text.literal("📸  Screenshots"),
                b -> client.setScreen(new ScreenshotsScreen(this)))
                .dimensions(10 + (pw + 5) * 2, py + ph + 8, pw, btnH).build());

        // ── Close ──
        addDrawableChild(ButtonWidget.builder(
                Text.literal("✖  Close"),
                b -> client.setScreen(null))
                .dimensions(width / 2 - 40, height - 24, 80, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // full background
        ctx.fill(0, 0, width, height, C_BG);
        renderStars(ctx);

        int pw = (width - 30) / 3;
        int ph = height - 80;
        int py = 45;

        // title
        ctx.drawCenteredTextWithShadow(textRenderer, "✦ MASTER NOTES ✦", width / 2, 15, C_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "by AbhayTheMaster", width / 2, 27, C_SUBTEXT);

        // draw three panels
        drawPanel(ctx, 10,               py, pw, ph, "NOTES",       C_ACCENT);
        drawPanel(ctx, 15 + pw,          py, pw, ph, "GOALS",       C_ACCENT2);
        drawPanel(ctx, 20 + pw * 2,      py, pw, ph, "SCREENSHOTS", 0xFF00FFAA);

        // fill panels with preview content
        renderNotesPreview (ctx, 10,           py, pw, ph);
        renderGoalsPreview (ctx, 15 + pw,      py, pw, ph);
        renderSsPreview    (ctx, 20 + pw * 2,  py, pw, ph);

        super.render(ctx, mx, my, delta);
    }

    // ── panel drawing ─────────────────────────────────────────────────────────

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, String label, int accent) {
        ctx.fill(x, y, x + w, y + h, C_PANEL);
        ctx.fill(x, y, x + w, y + 20, C_PANEL_LITE);      // header strip
        ctx.drawBorder(x, y, w, h, C_BORDER);
        // accent top line
        ctx.fill(x + 1, y, x + w - 1, y + 2, accent);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 6, accent);
    }

    // ── notes preview ─────────────────────────────────────────────────────────

    private void renderNotesPreview(DrawContext ctx, int x, int y, int w, int h) {
        List<Note> notes = Note.loadAll();
        if (notes.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No notes yet", x + w / 2, y + h / 2, C_SUBTEXT);
            return;
        }
        int ty = y + 24;
        for (int i = 0; i < Math.min(notes.size(), 6); i++) {
            Note n = notes.get(i);
            ctx.drawText(textRenderer, "• " + n.getTitle(), x + 5, ty, C_TEXT, false);
            ctx.drawText(textRenderer, n.getPreview(w / textRenderer.fontHeight - 4),
                    x + 8, ty + 10, C_SUBTEXT, false);
            ty += 24;
            if (ty > y + h - 10) break;
        }
    }

    // ── goals preview ─────────────────────────────────────────────────────────

    private void renderGoalsPreview(DrawContext ctx, int x, int y, int w, int h) {
        List<Goal> goals = GoalManager.getGoals();
        if (goals.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No goals yet", x + w / 2, y + h / 2, C_SUBTEXT);
            return;
        }
        int ty = y + 24;
        for (int i = 0; i < Math.min(goals.size(), 5); i++) {
            Goal g = goals.get(i);
            ctx.drawText(textRenderer, g.getTitle(), x + 5, ty, g.getStatus().color, false);
            // progress bar
            int barX = x + 5, barY = ty + 11, barW = w - 12, barH = 4;
            ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF1E1E3A);
            ctx.fill(barX, barY, barX + (barW * g.getProgress() / 100), barY + barH, C_ACCENT);
            ctx.drawText(textRenderer, g.getProgress() + "%", x + w - 22, ty, C_SUBTEXT, false);
            ty += 22;
            if (ty > y + h - 10) break;
        }
    }

    // ── screenshots preview ───────────────────────────────────────────────────

    private void renderSsPreview(DrawContext ctx, int x, int y, int w, int h) {
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Click to browse", x + w / 2, y + h / 2, C_SUBTEXT);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "your screenshots", x + w / 2, y + h / 2 + 12, C_SUBTEXT);
    }

    // ── decorative stars ──────────────────────────────────────────────────────

    private void renderStars(DrawContext ctx) {
        long t = System.currentTimeMillis();
        int[][] stars = {{30,8},{80,5},{140,12},{200,3},{260,9},{320,6},{380,10},
                         {50,height-15},{110,height-8},{180,height-12},{240,height-5},{300,height-10}};
        for (int[] s : stars) {
            int alpha = (int)(128 + 127 * Math.sin((t / 800.0) + s[0]));
            int col = (alpha << 24) | 0x00D4FF;
            ctx.fill(s[0], s[1], s[0] + 1, s[1] + 1, col);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
