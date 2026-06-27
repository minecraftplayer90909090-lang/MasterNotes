package com.abhaythmaster.masternotes.gui;

import com.abhaythmaster.masternotes.goal.Goal;
import com.abhaythmaster.masternotes.goal.GoalManager;
import com.abhaythmaster.masternotes.goal.GoalStatus;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import static com.abhaythmaster.masternotes.gui.MasterDashboardScreen.*;

public class EditGoalScreen extends Screen {

    private final Screen parent;
    private final Goal goal;
    private final boolean isNew;

    private TextFieldWidget titleField;
    private TextFieldWidget descField;
    private TextFieldWidget deadlineField;
    private TextFieldWidget subtaskField;
    private int progressDrag = -1; // -1 = not dragging

    public EditGoalScreen(Screen parent, Goal goal) {
        super(Text.literal(goal == null ? "New Goal" : "Edit Goal"));
        this.parent = parent;
        this.isNew  = (goal == null);
        this.goal   = isNew ? new Goal("New Goal") : goal;
    }

    private static final int LEFT   = 10;
    private static final int LABEL_COL = 0xFF9E9EC0;

    @Override
    public void init() {
        int fw = width / 2 - 20;

        // ── title ─────────────────────────────────────────────────────────────
        titleField = addDrawableChild(new TextFieldWidget(
                textRenderer, LEFT, 30, fw, 18, Text.literal("Title")));
        titleField.setMaxLength(80);
        titleField.setText(goal.getTitle());

        // ── description ───────────────────────────────────────────────────────
        descField = addDrawableChild(new TextFieldWidget(
                textRenderer, LEFT, 60, fw, 18, Text.literal("Description")));
        descField.setMaxLength(200);
        descField.setText(goal.getDescription());

        // ── deadline ──────────────────────────────────────────────────────────
        deadlineField = addDrawableChild(new TextFieldWidget(
                textRenderer, LEFT, 90, fw / 2, 18, Text.literal("YYYY-MM-DD")));
        deadlineField.setMaxLength(10);
        deadlineField.setText(goal.getDeadline());
        deadlineField.setPlaceholder(Text.literal("YYYY-MM-DD"));

        // ── status cycle ──────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Status: " + goal.getStatus().label),
                b -> {
                    goal.setStatus(goal.getStatus().next());
                    b.setMessage(Text.literal("Status: " + goal.getStatus().label));
                }).dimensions(LEFT + fw / 2 + 4, 90, fw / 2 - 4, 18).build());

        // ── subtask input ─────────────────────────────────────────────────────
        subtaskField = addDrawableChild(new TextFieldWidget(
                textRenderer, LEFT, height - 55, fw - 24, 18, Text.literal("New subtask")));
        subtaskField.setPlaceholder(Text.literal("Add subtask..."));

        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                b -> {
                    String st = subtaskField.getText().trim();
                    if (!st.isEmpty()) { goal.addSubtask(st); subtaskField.setText(""); }
                }).dimensions(LEFT + fw - 22, height - 55, 22, 18).build());

        // ── save / cancel ─────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal("💾 Save"),
                b -> saveAndReturn())
                .dimensions(LEFT, height - 28, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("✖ Cancel"),
                b -> client.setScreen(parent))
                .dimensions(LEFT + 74, height - 28, 70, 20).build());

        if (!isNew) {
            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete"),
                    b -> { GoalManager.remove(goal); client.setScreen(parent); })
                    .dimensions(LEFT + 148, height - 28, 70, 20).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);

        int fw = width / 2 - 20;

        ctx.drawCenteredTextWithShadow(textRenderer,
                isNew ? "✦ NEW GOAL ✦" : "✦ EDIT GOAL ✦", width / 4, 12, C_ACCENT2);

        // labels
        ctx.drawText(textRenderer, "Title",       LEFT, 20, LABEL_COL, false);
        ctx.drawText(textRenderer, "Description", LEFT, 50, LABEL_COL, false);
        ctx.drawText(textRenderer, "Deadline",    LEFT, 80, LABEL_COL, false);

        // ── progress bar / slider ─────────────────────────────────────────────
        ctx.drawText(textRenderer, "Progress: " + goal.getProgress() + "%", LEFT, 118, LABEL_COL, false);
        int bx = LEFT, by = 130, bw = fw, bh = 12;
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF1E1E3A);
        ctx.fill(bx, by, bx + bw * goal.getProgress() / 100, by + bh, C_ACCENT);
        ctx.drawBorder(bx, by, bw, bh, C_BORDER);
        ctx.drawText(textRenderer, "◀ drag ▶", bx + bw / 2 - 20, by + 2, C_SUBTEXT, false);

        // ── subtasks ──────────────────────────────────────────────────────────
        ctx.drawText(textRenderer, "Subtasks (" + goal.getSubtasks().size() + ")", LEFT, 152, LABEL_COL, false);
        int sy = 163;
        for (int i = 0; i < goal.getSubtasks().size() && sy < height - 80; i++) {
            boolean done = goal.getSubtaskDone().get(i);
            // checkbox
            ctx.fill(LEFT, sy, LEFT + 10, sy + 10, done ? C_GREEN : C_PANEL_LITE);
            ctx.drawBorder(LEFT, sy, 10, 10, done ? C_GREEN : C_BORDER);
            if (done) ctx.drawText(textRenderer, "✓", LEFT + 1, sy + 1, 0xFF000000, false);

            int textCol = done ? C_SUBTEXT : C_TEXT;
            ctx.drawText(textRenderer, goal.getSubtasks().get(i), LEFT + 14, sy + 1, textCol, false);

            // small [x] delete button
            ctx.drawText(textRenderer, "×", LEFT + fw - 6, sy + 1, C_RED, false);
            sy += 14;
        }
        ctx.drawText(textRenderer, "Add subtask:", LEFT, height - 68, LABEL_COL, false);

        // ── right panel: preview ──────────────────────────────────────────────
        int rx = width / 2 + 10;
        ctx.fill(rx, 10, width - 10, height - 10, C_PANEL);
        ctx.drawBorder(rx, 10, width - rx - 10, height - 20, C_BORDER);
        ctx.drawText(textRenderer, "PREVIEW", rx + 4, 14, C_ACCENT2, false);

        String titlePrev = titleField != null ? titleField.getText() : goal.getTitle();
        ctx.drawText(textRenderer, titlePrev, rx + 6, 30, C_TEXT, true);

        // preview progress bar
        int pbw = width - rx - 20;
        ctx.fill(rx + 6, 50, rx + 6 + pbw, 58, 0xFF1E1E3A);
        ctx.fill(rx + 6, 50, rx + 6 + pbw * goal.getProgress() / 100, 58, C_ACCENT);
        ctx.drawText(textRenderer, goal.getProgress() + "%", rx + 6 + pbw + 2, 50, C_SUBTEXT, false);

        ctx.drawText(textRenderer, goal.getStatus().label, rx + 6, 62, goal.getStatus().color, false);
        if (!goal.getDeadline().isEmpty()) {
            int dlCol = goal.isDeadlinePassed() ? C_RED : goal.isDeadlineNear() ? C_YELLOW : C_SUBTEXT;
            ctx.drawText(textRenderer, "📅 " + goal.getDeadline(), rx + 6, 74, dlCol, false);
        }

        super.render(ctx, mx, my, delta);
    }

    // ── progress slider drag ──────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int fw = width / 2 - 20;
        int bx = LEFT, by = 130, bw = fw, bh = 12;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
            progressDrag = 1;
            goal.setProgress((int) ((mx - bx) * 100 / bw));
            return true;
        }

        // subtask checkbox / delete click
        int sy = 163;
        for (int i = 0; i < goal.getSubtasks().size() && sy < height - 80; i++) {
            if (mx >= LEFT && mx <= LEFT + 10 && my >= sy && my <= sy + 10) {
                goal.setSubtaskDone(i, !goal.getSubtaskDone().get(i));
                return true;
            }
            if (mx >= LEFT + fw - 8 && mx <= LEFT + fw && my >= sy && my <= sy + 10) {
                goal.removeSubtask(i);
                return true;
            }
            sy += 14;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (progressDrag >= 0) {
            int fw = width / 2 - 20;
            goal.setProgress((int) Math.max(0, Math.min(100, (mx - LEFT) * 100 / fw)));
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        progressDrag = -1;
        return super.mouseReleased(mx, my, btn);
    }

    private void saveAndReturn() {
        goal.setTitle(titleField.getText().isBlank() ? "Untitled Goal" : titleField.getText());
        goal.setDescription(descField.getText());
        goal.setDeadline(deadlineField.getText());
        if (isNew) GoalManager.add(goal);
        else GoalManager.update(goal);
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
