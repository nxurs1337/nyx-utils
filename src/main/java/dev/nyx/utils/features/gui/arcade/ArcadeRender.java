package dev.nyx.utils.features.gui.arcade;

import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Neon / arcade style drawing helpers shared by the click gui and the arcade machines.
 */
public final class ArcadeRender implements Util {
    public static final int BG_TOP = 0xFF0B0221;
    public static final int BG_BOTTOM = 0xFF2B0B4A;
    public static final int PINK = 0xFFFF2BD6;
    public static final int CYAN = 0xFF22F3FF;
    public static final int YELLOW = 0xFFFFE53B;
    public static final int GREEN = 0xFF4BFF6B;
    public static final int ORANGE = 0xFFFF8A1F;
    public static final int RED = 0xFFFF3B4E;
    public static final int PURPLE = 0xFF9B4BFF;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int DIM = 0xFF8A7FB0;

    private ArcadeRender() {
    }

    public static long millis() {
        return System.currentTimeMillis();
    }

    public static int alpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    /** Cycles through the neon palette over time. */
    public static int cycle(float offset) {
        int[] palette = {PINK, PURPLE, CYAN, GREEN, YELLOW, ORANGE};
        float t = ((millis() % (900L * palette.length)) / 900f + offset) % palette.length;
        int i = (int) t;
        return mix(palette[i], palette[(i + 1) % palette.length], t - i);
    }

    public static boolean blink(int periodMs) {
        return (millis() / periodMs) % 2 == 0;
    }

    public static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int w) {
        g.fill(x1, y1, x2, y1 + w, color);
        g.fill(x1, y2 - w, x2, y2, color);
        g.fill(x1, y1 + w, x1 + w, y2 - w, color);
        g.fill(x2 - w, y1 + w, x2, y2 - w, color);
    }

    /** A neon tube outline with a soft glow around it. */
    public static void neonBox(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        outline(g, x1 - 3, y1 - 3, x2 + 3, y2 + 3, alpha(color, 22), 1);
        outline(g, x1 - 2, y1 - 2, x2 + 2, y2 + 2, alpha(color, 45), 1);
        outline(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, alpha(color, 90), 1);
        outline(g, x1, y1, x2, y2, color, 1);
    }

    public static void panel(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fillGradient(x1, y1, x2, y2, 0xE0140630, 0xE0080214);
        neonBox(g, x1, y1, x2, y2, color);
    }

    /** Nyx's own chunky pixel font (assets/nyxutils/font/arcade.json), falling back to vanilla glyphs. */
    public static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("nyxutils", "arcade"));
    private static final Map<String, Component> STYLED = new HashMap<>();

    /** The text as a component in the arcade font. Cached, gui text repeats every frame. */
    public static Component styled(String text) {
        if (STYLED.size() > 4096) STYLED.clear();
        return STYLED.computeIfAbsent(text, t -> Component.literal(t).withStyle(style -> style.withFont(FONT)));
    }

    /** Width of the text in the arcade font. */
    public static int width(String text) {
        return mc.font.width(styled(text));
    }

    public static void text(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(mc.font, styled(text), x, y, color, false);
    }

    public static void centered(GuiGraphics g, String text, int cx, int y, int color) {
        text(g, text, cx - width(text) / 2, y, color);
    }

    /** Text with a neon glow halo. */
    public static void glowText(GuiGraphics g, String text, int x, int y, int color) {
        int halo = alpha(color, 70);
        Component styled = styled(text);
        g.drawString(mc.font, styled, x - 1, y, halo, false);
        g.drawString(mc.font, styled, x + 1, y, halo, false);
        g.drawString(mc.font, styled, x, y - 1, halo, false);
        g.drawString(mc.font, styled, x, y + 1, halo, false);
        g.drawString(mc.font, styled, x, y, mix(color, WHITE, 0.35f), false);
    }

    public static void glowCentered(GuiGraphics g, String text, int cx, int y, int color) {
        glowText(g, text, cx - width(text) / 2, y, color);
    }

    /** Big scaled neon text centered on cx. */
    public static void bigText(GuiGraphics g, String text, int cx, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(scale, scale);
        glowText(g, text, -width(text) / 2, 0, color);
        g.pose().popMatrix();
    }

    /** Draws a pixel sprite. Each char of a row is a palette index ('.' or ' ' = transparent). */
    public static void sprite(GuiGraphics g, int x, int y, int px, String[] rows, int... palette) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == '.' || ch == ' ') continue;
                int idx = ch - '1';
                if (idx < 0 || idx >= palette.length) continue;
                g.fill(x + c * px, y + r * px, x + (c + 1) * px, y + (r + 1) * px, palette[idx]);
            }
        }
    }

    /** Filled pixel-art circle. */
    public static void disc(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** Synthwave background: gradient sky, stars, sun and a moving perspective grid. */
    public static void synthwave(GuiGraphics g, int w, int h, int gridColor) {
        int horizon = (int) (h * 0.62f);
        g.fillGradient(0, 0, w, horizon, BG_TOP, BG_BOTTOM);
        g.fillGradient(0, horizon, w, h, 0xFF12031F, 0xFF05010C);

        long t = millis();
        // stars
        for (int i = 0; i < 70; i++) {
            int sx = (int) ((i * 7919L) % Math.max(1, w));
            int sy = (int) ((i * 104729L) % Math.max(1, horizon - 10));
            boolean twinkle = ((t / 300 + i) % 9) == 0;
            g.fill(sx, sy, sx + 1, sy + 1, twinkle ? WHITE : 0x88FFFFFF);
        }

        // striped sun
        int cx = w / 2, r = Math.min(w, h) / 7, cy = horizon - r / 3;
        for (int dy = -r; dy <= 0; dy++) {
            int stripe = (dy + r);
            if (dy > -r / 2 && (stripe / 3) % 2 == 1) continue;
            int dx = (int) Math.sqrt(r * r - dy * dy);
            int color = mix(YELLOW, PINK, (dy + r) / (float) r);
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, alpha(color, 200));
        }

        // horizon glow
        g.fillGradient(0, horizon - 6, w, horizon, 0x00FF2BD6, alpha(gridColor, 110));
        g.fill(0, horizon, w, horizon + 1, gridColor);

        // horizontal grid lines scrolling towards the viewer
        float phase = (t % 1200) / 1200f;
        for (int i = 0; i < 12; i++) {
            float z = (i + phase) / 12f;
            int ly = horizon + (int) ((h - horizon) * z * z);
            g.fill(0, ly, w, ly + 1, alpha(gridColor, (int) (40 + 160 * z)));
        }
        // vertical lines fanning out from the vanishing point
        for (int i = -14; i <= 14; i++) {
            int bottomX = cx + i * w / 10;
            int steps = Math.max(1, (h - horizon) / 2);
            for (int s = 0; s < steps; s++) {
                float f = s / (float) steps;
                int px = (int) (cx + (bottomX - cx) * f);
                int py = horizon + (int) ((h - horizon) * f);
                g.fill(px, py, px + 1, py + 2, alpha(gridColor, (int) (30 + 140 * f)));
            }
        }
    }

    /** CRT scanlines + vignette over an area. */
    public static void crt(GuiGraphics g, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y += 2) {
            g.fill(x1, y, x2, y + 1, 0x22000000);
        }
        int v = Math.max(4, (x2 - x1) / 20);
        g.fillGradient(x1, y1, x2, y1 + v, 0x66000000, 0x00000000);
        g.fillGradient(x1, y2 - v, x2, y2, 0x00000000, 0x66000000);
        // rolling refresh bar
        int h = y2 - y1;
        int bar = y1 + (int) ((millis() / 8) % Math.max(1, h + 30)) - 15;
        g.fillGradient(x1, Math.max(y1, bar), x2, Math.min(y2, bar + 15), 0x00FFFFFF, 0x0CFFFFFF);
    }

    /** Chasing marquee light bulbs around a rectangle. */
    public static void bulbs(GuiGraphics g, int x1, int y1, int x2, int y2, int spacing) {
        long step = millis() / 140;
        int i = 0;
        for (int x = x1; x <= x2; x += spacing) {
            bulb(g, x, y1, i++, step);
        }
        for (int y = y1 + spacing; y <= y2; y += spacing) {
            bulb(g, x2, y, i++, step);
        }
        for (int x = x2 - spacing; x >= x1; x -= spacing) {
            bulb(g, x, y2, i++, step);
        }
        for (int y = y2 - spacing; y > y1; y -= spacing) {
            bulb(g, x1, y, i++, step);
        }
    }

    private static void bulb(GuiGraphics g, int x, int y, int i, long step) {
        boolean lit = (i + step) % 3 == 0;
        int color = lit ? YELLOW : 0xFF6B4A12;
        if (lit) g.fill(x - 2, y - 2, x + 3, y + 3, 0x40FFE53B);
        g.fill(x - 1, y - 1, x + 2, y + 2, color);
    }
}
