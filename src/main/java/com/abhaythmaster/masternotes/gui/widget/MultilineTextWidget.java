package com.abhaythmaster.masternotes.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Simple scrollable multiline text editor for note bodies.
 * Supports: typing, backspace, delete, arrow keys, Enter, Ctrl+A/C/V.
 */
public class MultilineTextWidget extends ClickableWidget {

    private static final int PAD        = 5;
    private static final int LINE_STEP  = 11;
    private static final int BG_COLOR   = 0xAA0A0A1A;
    private static final int BORDER     = 0x4D7EB8FF;
    private static final int TEXT_COLOR = 0xFFE0E0FF;
    private static final int CURSOR_COL = 0xFF00D4FF;

    private final TextRenderer tr;
    private final StringBuilder buf;
    private int cursor;     // index into buf
    private int scrollLine; // first visible line index

    public MultilineTextWidget(TextRenderer tr, int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
        this.tr  = tr;
        this.buf = new StringBuilder();
    }

    // ── value ────────────────────────────────────────────────────────────────

    public String getValue() { return buf.toString(); }

    public void setValue(String text) {
        buf.setLength(0);
        if (text != null) buf.append(text);
        cursor     = buf.length();
        scrollLine = 0;
    }

    // ── render ───────────────────────────────────────────────────────────────

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // background + border
        ctx.fill(x, y, x + w, y + h, BG_COLOR);
        ctx.drawBorder(x, y, w, h, isFocused() ? 0xFF00D4FF : BORDER);

        ctx.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);

        List<OrderedText> lines = wrappedLines();
        int visLines  = (h - PAD * 2) / LINE_STEP;
        int maxScroll = Math.max(0, lines.size() - visLines);
        scrollLine    = Math.min(scrollLine, maxScroll);

        for (int i = scrollLine; i < Math.min(lines.size(), scrollLine + visLines); i++) {
            ctx.drawText(tr, lines.get(i), x + PAD, y + PAD + (i - scrollLine) * LINE_STEP, TEXT_COLOR, false);
        }

        // cursor blinking
        if (isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int[] cl = cursorLineAndCol(lines);
            int cl0 = cl[0], col = cl[1];
            if (cl0 >= scrollLine && cl0 < scrollLine + visLines) {
                int cx = x + PAD + col;
                int cy = y + PAD + (cl0 - scrollLine) * LINE_STEP;
                ctx.fill(cx, cy, cx + 1, cy + LINE_STEP - 1, CURSOR_COL);
            }
        }

        ctx.disableScissor();
    }

    private List<OrderedText> wrappedLines() {
        return tr.wrapLines(Text.literal(buf.toString()), getWidth() - PAD * 2);
    }

    /** Returns [lineIndex, pixelX] of the cursor within the wrapped lines */
    private int[] cursorLineAndCol(List<OrderedText> lines) {
        String full = buf.toString();
        // find which wrapped line the cursor falls in by counting chars
        int remaining = cursor;
        int textW = getWidth() - PAD * 2;
        int lineIdx = 0;
        for (int i = 0; i < lines.size(); i++) {
            String lineText = orderedToString(lines.get(i));
            if (remaining <= lineText.length()) { lineIdx = i; break; }
            remaining -= lineText.length();
            lineIdx = i + 1;
        }
        int colPx = tr.getWidth(full.length() > cursor
                ? full.substring(Math.max(0, cursor - remaining), cursor) : "");
        return new int[]{lineIdx, Math.min(colPx, textW)};
    }

    /** Best-effort: pull plain chars from OrderedText (approximate) */
    private String orderedToString(OrderedText ot) {
        StringBuilder sb = new StringBuilder();
        ot.accept((idx, style, ch) -> { sb.appendCodePoint(ch); return true; });
        return sb.toString();
    }

    // ── input ────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!isFocused()) return false;
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE:
                if (ctrl) deleteWordBack();
                else if (cursor > 0) { buf.deleteCharAt(--cursor); }
                return true;
            case GLFW.GLFW_KEY_DELETE:
                if (cursor < buf.length()) buf.deleteCharAt(cursor);
                return true;
            case GLFW.GLFW_KEY_LEFT:
                if (cursor > 0) cursor--; return true;
            case GLFW.GLFW_KEY_RIGHT:
                if (cursor < buf.length()) cursor++; return true;
            case GLFW.GLFW_KEY_HOME:
                cursor = 0; scrollLine = 0; return true;
            case GLFW.GLFW_KEY_END:
                cursor = buf.length(); return true;
            case GLFW.GLFW_KEY_UP:
                scrollLine = Math.max(0, scrollLine - 1); return true;
            case GLFW.GLFW_KEY_DOWN:
                scrollLine++; return true;
            case GLFW.GLFW_KEY_ENTER: case GLFW.GLFW_KEY_KP_ENTER:
                buf.insert(cursor++, '\n'); return true;
            case GLFW.GLFW_KEY_A:
                if (ctrl) { cursor = buf.length(); return true; }
                break;
            case GLFW.GLFW_KEY_C:
                if (ctrl) {
                    MinecraftClient.getInstance().keyboard.setClipboard(buf.toString());
                    return true;
                }
                break;
            case GLFW.GLFW_KEY_V:
                if (ctrl) {
                    String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                    buf.insert(cursor, clip);
                    cursor += clip.length();
                    return true;
                }
                break;
            case GLFW.GLFW_KEY_X:
                if (ctrl) {
                    MinecraftClient.getInstance().keyboard.setClipboard(buf.toString());
                    buf.setLength(0); cursor = 0; return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (!isFocused()) return false;
        if (chr >= 32) { // printable
            buf.insert(cursor++, chr);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (isMouseOver(mx, my)) {
            scrollLine = Math.max(0, scrollLine - (int) vAmt);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (isMouseOver(mx, my)) {
            setFocused(true);
            return true;
        }
        setFocused(false);
        return false;
    }

    private void deleteWordBack() {
        if (cursor == 0) return;
        int i = cursor - 1;
        while (i > 0 && buf.charAt(i - 1) != ' ' && buf.charAt(i - 1) != '\n') i--;
        buf.delete(i, cursor);
        cursor = i;
    }

    // ── public extras ─────────────────────────────────────────────────────────

    public void insertText(String s) {
        buf.insert(cursor, s);
        cursor += s.length();
    }

    @Override
    protected void updateNarration(NarrationMessageBuilder b) {}
}
