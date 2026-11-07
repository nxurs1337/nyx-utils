package dev.nyx.utils.features.gui.casino;

import dev.nyx.utils.features.gui.casino.slots.SlotTheme;
import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * Vegas-style drawing helpers for the lounge and its machines.
 */
public final class CasinoRender implements Util {
    public static final int GOLD = 0xFFFFC23A;
    public static final int VELVET = 0xFF5A0A1A;
    public static final int VELVET_DARK = 0xFF1A0208;

    private CasinoRender() {
    }

    /** Red velvet with a faint gold diamond lattice. */
    public static void velvet(GuiGraphics g, int w, int h) {
        g.fillGradient(0, 0, w, h, VELVET, VELVET_DARK);
        for (int y = 0; y < h; y += 16) {
            for (int x = (y / 16) % 2 == 0 ? 0 : 8; x < w; x += 16) {
                g.fill(x + 7, y + 7, x + 9, y + 9, 0x18FFC23A);
            }
        }
    }

    /** Animated themed backdrop inside a rectangle. */
    public static void backdrop(GuiGraphics g, SlotTheme theme, int x1, int y1, int x2, int y2) {
        g.fillGradient(x1, y1, x2, y2, theme.bgTop(), theme.bgBottom());
        long t = millis();
        int w = x2 - x1, h = y2 - y1;
        int accent = theme.accent();
        switch (theme.pattern()) {
            case RAYS -> {
                int cx = x1 + w / 2, cy = y1 + h / 2;
                double spin = t / 6000.0;
                for (int i = 0; i < 16; i++) {
                    double a = spin + i * Math.PI / 8;
                    for (int r = 20; r < Math.max(w, h); r += 6) {
                        int px = cx + (int) (Math.cos(a) * r), py = cy + (int) (Math.sin(a) * r);
                        if (px < x1 || py < y1 || px >= x2 || py >= y2) continue;
                        g.fill(px, py, px + 3, py + 3, alpha(accent, 22));
                    }
                }
            }
            case BUBBLES -> {
                for (int i = 0; i < 26; i++) {
                    int bx = x1 + (int) ((i * 7919L) % w);
                    int by = y2 - (int) (((t / 30) + i * 97L) % (h + 20));
                    disc(g, bx, by, 2 + i % 4, alpha(WHITE, 40));
                }
            }
            case STARS, SNOW -> {
                boolean snow = theme.pattern() == SlotTheme.Pattern.SNOW;
                for (int i = 0; i < 60; i++) {
                    int sx = x1 + (int) ((i * 7919L) % w);
                    int sy = snow ? y1 + (int) (((t / 40) + i * 53L) % h) : y1 + (int) ((i * 104729L) % h);
                    boolean on = snow || ((t / 300) + i) % 6 != 0;
                    if (on) g.fill(sx, sy, sx + (i % 3 == 0 ? 2 : 1), sy + (i % 3 == 0 ? 2 : 1), alpha(WHITE, snow ? 160 : 110));
                }
            }
            case SPARKS -> {
                for (int i = 0; i < 30; i++) {
                    int sx = x1 + (int) ((i * 7919L) % w);
                    int sy = y1 + (int) ((i * 104729L) % h);
                    if (((t / 200) + i) % 4 == 0) {
                        g.fill(sx - 2, sy, sx + 3, sy + 1, alpha(accent, 120));
                        g.fill(sx, sy - 2, sx + 1, sy + 3, alpha(accent, 120));
                    }
                }
            }
            case STRIPES -> {
                int offset = (int) (t / 60 % 24);
                for (int x = x1 - h + offset; x < x2; x += 24) {
                    for (int dy = 0; dy < h; dy += 2) {
                        int sx = x + dy;
                        if (sx < x1 || sx + 8 > x2) continue;
                        g.fill(sx, y1 + dy, sx + 8, y1 + dy + 2, alpha(accent, 16));
                    }
                }
            }
            case WAVES -> {
                for (int row = 0; row < 8; row++) {
                    int baseY = y1 + h * (row + 1) / 9;
                    for (int x = x1; x < x2; x += 3) {
                        int wy = baseY + (int) (Math.sin((x + t / 20.0 + row * 40) / 18.0) * 3);
                        g.fill(x, wy, x + 3, wy + 1, alpha(WHITE, 30));
                    }
                }
            }
            case FLAMES -> {
                for (int x = x1; x < x2; x += 4) {
                    double n = Math.sin(x * 0.13 + t / 180.0) + Math.sin(x * 0.05 - t / 300.0);
                    int fh = (int) (h * 0.12 + (n + 2) * h * 0.06);
                    g.fillGradient(x, y2 - fh, x + 4, y2, 0x00FF8A1F, alpha(accent, 90));
                }
            }
            case DIAMONDS -> {
                for (int y = y1; y < y2; y += 14) {
                    for (int x = x1 + ((y - y1) / 14 % 2) * 7; x < x2; x += 14) {
                        boolean lit = ((t / 250) + x / 14 + y / 14) % 9 == 0;
                        g.fill(x + 6, y + 6, x + 8, y + 8, alpha(lit ? accent : WHITE, lit ? 160 : 18));
                    }
                }
            }
        }
    }

    /** A rounded-looking casino button. */
    public static void button(GuiGraphics g, String label, int x, int y, int w, int h, int color, boolean hover, boolean enabled) {
        int face = !enabled ? 0xFF3A3A3A : hover ? mix(color, WHITE, 0.25f) : color;
        g.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x80000000);
        g.fillGradient(x, y, x + w, y + h, mix(face, WHITE, 0.2f), mix(face, 0xFF000000, 0.25f));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, alpha(WHITE, enabled ? 120 : 40));
        outline(g, x, y, x + w, y + h, mix(face, 0xFF000000, 0.5f), 1);
        int ink = !enabled ? 0xFF8A8A8A : (brightness(face) > 150 ? 0xFF201008 : WHITE);
        centered(g, label, x + w / 2, y + (h - 8) / 2, ink);
    }

    private static int brightness(int color) {
        return ((color >> 16 & 255) * 299 + (color >> 8 & 255) * 587 + (color & 255) * 114) / 1000;
    }

    /** Gold-rimmed panel used for the wallet and info boxes. */
    public static void goldPanel(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fillGradient(x1, y1, x2, y2, 0xF0200810, 0xF0100206);
        outline(g, x1, y1, x2, y2, GOLD, 1);
        outline(g, x1 + 2, y1 + 2, x2 - 2, y2 - 2, alpha(GOLD, 70), 1);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
