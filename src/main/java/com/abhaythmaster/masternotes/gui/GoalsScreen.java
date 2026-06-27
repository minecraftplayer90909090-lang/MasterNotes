package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.goal.Goal;
import com.abhaythmaster.masternotes.goal.GoalManager;
import com.abhaythmaster.masternotes.goal.GoalStatus;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

public class GoalsScreen extends Screen {

    private final Screen parent;
    private GoalStatus filterStatus = null; // null = All
    private List<Goal> displayed = new ArrayList<>();
    private int scrollOffset = 0;

    private static final int CARD_H  = 56;
    private static final int CARD_GAP = 4;
    private static final int LIST_X  = 10;

    public GoalsScreen(Screen parent) {
        super(Text.literal("Goals"));
        this.parent = parent;
    }

    @Override
    public void init() {
        refreshDisplayed();

        // filter tabs
        String[] labels = {"All","Active","Completed","Failed"};
        GoalStatus[] statuses = {null, GoalStatus.ACTIVE, GoalStatus.COMPLETED, GoalStatus.FAILED};
        int tw = (width - 20) / 4;
        for (int i = 0; i < 4; i++) {
            int fi = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(labels[i]),
                    b -> { filterStatus = statuses[fi]; scrollOffset = 0; refreshDisplayed(); init(); })
                    .dimensions(10 + i * tw, 10, tw - 2, 16).build());
        }

        // new goal
        addDrawableChild(ButtonWidget.builder(Text.literal("+ New Goal"),
                b -> client.setScreen(new EditGoalScreen(this, null)))
                .dimensions(width - 90, 30, 80, 16).build());

        // back
        addDrawableChild(ButtonWidget.builder(Text.literal("← Back"),
                b -> client.setScreen(parent))
                .dimensions(8, 30, 60, 16).build());
    }

    private void refreshDisplayed() {
        displayed = filterStatus == null
                ? new ArrayList<>(GoalManager.getGoals())
                : GoalManager.getGoalsByStatus(filterStatus);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, "✦ GOALS ✦", width / 2, 30, C_ACCENT2);

        // active filter indicator
        String fLabel = filterStatus == null ? "All" : filterStatus.label;
        ctx.drawText(textRenderer, "Filter: " + fLabel, width / 2 - 30, 49, C_SUBTEXT, false);

        int listTop = 52;
        int listBot = height - 10;
        int listW   = width - LIST_X * 2;
        int visible = (listBot - listTop) / (CARD_H + CARD_GAP);

        ctx.enableScissor(LIST_X, listTop, LIST_X + listW, listBot);

        for (int i = scrollOffset; i < Math.min(displayed.size(), scrollOffset + visible + 2); i++) {
            int cy = listTop + (i - scrollOffset) * (CARD_H + CARD_GAP);
            if (cy > listBot) break;
            renderGoalCard(ctx, displayed.get(i), LIST_X, cy, listW, CARD_H, mx, my);
        }

        ctx.disableScissor();

        if (displayed.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No goals here. Add one!", width / 2, height / 2, C_SUBTEXT);
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderGoalCard(DrawContext ctx, Goal g, int x, int y, int w, int h,
                                int mx, int my) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        ctx.fill(x, y, x + w, y + h, hovered ? C_PANEL_LITE : C_PANEL);
        ctx.drawBorder(x, y, w, h, hovered ? g.getStatus().color : C_BORDER);

        // status color strip on left
        ctx.fill(x, y, x + 3, y + h, g.getStatus().color);

        // title
        ctx.drawText(textRenderer, g.getTitle(), x + 8, y + 5, C_TEXT, true);

        // description preview
        if (!g.getDescription().isBlank()) {
            String preview = g.getDescription().length() > 50
                    ? g.getDescription().substring(0, 47) + "..." : g.getDescription();
            ctx.drawText(textRenderer, preview, x + 8, y + 17, C_SUBTEXT, false);
        }

        // progress bar
        int bx = x + 8, by = y + 31, bw = w - 80, bh = 6;
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF1E1E3A);
        int filled = bw * g.getProgress() / 100;
        int barCol = g.getProgress() == 100 ? C_GREEN
                   : g.getProgress() > 60   ? C_ACCENT
                   : g.getProgress() > 30   ? C_YELLOW : C_RED;
        ctx.fill(bx, by, bx + filled, by + bh, barCol);
        ctx.drawText(textRenderer, g.getProgress() + "%", bx + bw + 4, by - 1, C_SUBTEXT, false);

        // status badge
        String badge = g.getStatus().label;
        ctx.drawText(textRenderer, badge, x + w - textRenderer.getWidth(badge) - 5, y + 5, g.getStatus().color, false);

        // deadline
        if (!g.getDeadline().isEmpty()) {
            int dlColor = g.isDeadlinePassed() ? C_RED : g.isDeadlineNear() ? C_YELLOW : C_SUBTEXT;
            String dlTxt = (g.isDeadlinePassed() ? "⚠ PAST " : g.isDeadlineNear() ? "⏰ SOON " : "📅 ") + g.getDeadline();
            ctx.drawText(textRenderer, dlTxt, x + 8, y + 42, dlColor, false);
        }

        // subtask progress
        if (!g.getSubtasks().isEmpty()) {
            long done = g.getSubtaskDone().stream().filter(b -> b).count();
            String st = "✓ " + done + "/" + g.getSubtasks().size() + " tasks";
            ctx.drawText(textRenderer, st, x + w - textRenderer.getWidth(st) - 5, y + 42, C_SUBTEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int listTop = 52;
        int visible = (height - 10 - listTop) / (CARD_H + CARD_GAP);

        for (int i = scrollOffset; i < Math.min(displayed.size(), scrollOffset + visible + 1); i++) {
            int cy = listTop + (i - scrollOffset) * (CARD_H + CARD_GAP);
            int cx = LIST_X, cw = width - LIST_X * 2;
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + CARD_H) {
                if (btn == 0) client.setScreen(new EditGoalScreen(this, displayed.get(i)));
                if (btn == 1) { GoalManager.remove(displayed.get(i)); refreshDisplayed(); }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int max = Math.max(0, displayed.size() - (height - 62) / (CARD_H + CARD_GAP));
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) vAmt));
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
