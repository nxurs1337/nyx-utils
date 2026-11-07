package dev.nyx.utils.features.gui.items;

import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * Shared look for setting controls: rows of a green CRT service menu.
 */
public final class ItemStyle implements Util {
    public static final int TEXT = 0xFFB8FFC8;
    public static final int VALUE = 0xFFFFE53B;
    public static final int MUTED = 0xFF4E8A5C;

    private ItemStyle() {
    }

    /** Right edge of a setting row; the controls historically draw 7.4px past their width. */
    public static int right(Item item) {
        return Math.round(item.getX() + item.getWidth() + 7.4f);
    }

    public static void row(GuiGraphics g, Item item, int height, boolean hover) {
        int x1 = Math.round(item.getX()), y1 = Math.round(item.getY()), x2 = right(item), y2 = y1 + height - 1;
        g.fill(x1, y1, x2, y2, hover ? 0x3022FF66 : 0x18000000);
        g.fill(x1, y1, x1 + 1, y2, hover ? GREEN : alpha(GREEN, 90));
    }

    public static void label(GuiGraphics g, Item item, String text, int color) {
        ArcadeRender.text(g, text, Math.round(item.getX()) + 4, Math.round(item.getY()) + 5, color);
    }

    public static void value(GuiGraphics g, Item item, String text, int color) {
        ArcadeRender.text(g, text, right(item) - 3 - ArcadeRender.width(text), Math.round(item.getY()) + 5, color);
    }

    /** A little ON/OFF switch at the right side of a row. */
    public static void toggle(GuiGraphics g, Item item, boolean on) {
        int x2 = right(item) - 3, x1 = x2 - 26, y1 = Math.round(item.getY()) + 4, y2 = y1 + 10;
        g.fill(x1, y1, x2, y2, on ? 0xFF0F5A24 : 0xFF3A1016);
        outline(g, x1, y1, x2, y2, on ? GREEN : 0xFF8A2030, 1);
        int knob = on ? x2 - 11 : x1 + 1;
        g.fill(knob, y1 + 1, knob + 10, y2 - 1, on ? GREEN : 0xFFB04050);
        g.fill(knob + 1, y1 + 1, knob + 9, y1 + 2, 0x60FFFFFF);
    }

    /** A keyboard key cap at the right side of a row. */
    public static void keycap(GuiGraphics g, Item item, String key, boolean listening) {
        int w = ArcadeRender.width(key) + 8;
        int x2 = right(item) - 3, x1 = x2 - w, y1 = Math.round(item.getY()) + 2, y2 = y1 + 14;
        g.fill(x1, y1 + 1, x2, y2 + 1, 0xFF0A1A0E);
        g.fill(x1, y1, x2, y2, listening ? 0xFF3A3A10 : 0xFF1C2A20);
        outline(g, x1, y1, x2, y2, listening ? VALUE : MUTED, 1);
        ArcadeRender.text(g, key, x1 + 4, y1 + 4, listening ? VALUE : TEXT);
    }
}
