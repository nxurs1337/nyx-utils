package dev.nyx.utils.features.gui.badges;

import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * Old-web style 88x31 buttons, drawn procedurally so they animate at any zoom level.
 * Every badge is drawn in its own 88x31 space, callers scale it with the pose stack.
 */
public final class Badges implements Util {
    public static final int W = 88;
    public static final int H = 31;

    public enum Pattern {GRADIENT, STRIPES, CHECKER, STARS, SCANLINES, PLASMA, HAZARD}

    public enum Anim {NONE, BLINK, RAINBOW, MARQUEE, WAVE}

    public record Badge(String id, String line1, String line2, Icon icon, int bgA, int bgB, Pattern pattern,
                        int textColor, Anim anim) {
        public String text() {
            return line1 + " " + line2;
        }
    }

    public enum Icon {
        MOON(new String[]{
                "..1111..",
                ".11.....",
                "11......",
                "11......",
                "11......",
                "11....1.",
                ".11111..",
                "..111...",
        }, YELLOW, WHITE),
        HEART(new String[]{
                ".11.11..",
                "1121111.",
                "1211111.",
                "1111111.",
                ".11111..",
                "..111...",
                "...1....",
                "........",
        }, RED, WHITE),
        GEM(new String[]{
                "..1111..",
                ".122221.",
                "12222221",
                ".122221.",
                "..1221..",
                "...11...",
                "........",
                "........",
        }, CYAN, WHITE),
        BLOCK(new String[]{
                "11111111",
                "12122121",
                "22222222",
                "33233323",
                "33333333",
                "33323333",
                "33333233",
                "33333333",
        }, GREEN, 0xFF3FB040, 0xFF8B5A2B),
        BOLT(new String[]{
                "...111..",
                "..111...",
                ".111....",
                "1111111.",
                "...111..",
                "..111...",
                ".11.....",
                "1.......",
        }, YELLOW, WHITE),
        PICKAXE(new String[]{
                ".11111..",
                "1.....1.",
                "....2...",
                "...2....",
                "..2.....",
                ".2......",
                "2.......",
                "........",
        }, CYAN, 0xFF9A6A3A),
        SKULL(new String[]{
                ".111111.",
                "11111111",
                "12211221",
                "12211221",
                "11111111",
                ".111111.",
                ".1.11.1.",
                "........",
        }, WHITE, 0xFF000000),
        STAR(new String[]{
                "...1....",
                "...1....",
                "1111111.",
                ".11111..",
                "..111...",
                ".11.11..",
                "1.....1.",
                "........",
        }, YELLOW, WHITE),
        FIRE(new String[]{
                "...1....",
                "..11..1.",
                ".111.11.",
                ".112111.",
                "1122211.",
                "1122221.",
                ".122221.",
                "..1111..",
        }, ORANGE, YELLOW),
        GLOBE(new String[]{
                "..1111..",
                ".121121.",
                "11211211",
                "22222222",
                "11211211",
                ".121121.",
                "..1111..",
                "........",
        }, 0xFF3B7BFF, GREEN),
        DISK(new String[]{
                "1111111.",
                "1222221.",
                "1222221.",
                "1111111.",
                "1133311.",
                "1133311.",
                "1111111.",
                "........",
        }, 0xFF3050C0, WHITE, 0xFF202020),
        CLOCK(new String[]{
                "..1111..",
                ".122221.",
                "12223221",
                "12223221",
                "12233221",
                ".122221.",
                "..1111..",
                "........",
        }, YELLOW, WHITE, 0xFF000000),
        COFFEE(new String[]{
                ".1.1....",
                "..1.1...",
                "2222222.",
                "2333322.",
                "2333322.",
                "233332..",
                ".2222...",
                "........",
        }, 0xFFDDDDDD, WHITE, 0xFF6B3A1A),
        CAT(new String[]{
                "1.....1.",
                "11...11.",
                "1111111.",
                "1211121.",
                "1111111.",
                "1113111.",
                ".11111..",
                "........",
        }, ORANGE, GREEN, PINK),
        POTION(new String[]{
                "..11....",
                "..22....",
                "..22....",
                ".2332...",
                "233332..",
                "233332..",
                ".2222...",
                "........",
        }, 0xFF8B5A2B, WHITE, PINK),
        WIFI(new String[]{
                "......1.",
                "......1.",
                "....1.1.",
                "....1.1.",
                "..1.1.1.",
                "..1.1.1.",
                "1.1.1.1.",
                "........",
        }, GREEN, WHITE);

        final String[] rows;
        final int[] palette;

        Icon(String[] rows, int... palette) {
            this.rows = rows;
            this.palette = palette;
        }
    }

    private static final int NAVY = 0xFF10104A;
    private static final int BLACK = 0xFF000000;

    public static final List<Badge> ALL = List.of(
            new Badge("nyx", "NYX UTILS", "GET IT NOW", Icon.MOON, 0xFF2A0B5A, 0xFF0B0221, Pattern.STARS, YELLOW, Anim.BLINK),
            new Badge("fps", "BEST VIEWED", "AT 12 FPS", Icon.GLOBE, 0xFF003366, 0xFF0066AA, Pattern.SCANLINES, WHITE, Anim.NONE),
            new Badge("love", "MINED WITH", "LOVE", Icon.HEART, 0xFFFF6AB4, 0xFFFF2B8A, Pattern.GRADIENT, WHITE, Anim.WAVE),
            new Badge("cobble", "100% ORGANIC", "COBBLESTONE", Icon.BLOCK, 0xFF6A6A6A, 0xFF4A4A4A, Pattern.CHECKER, WHITE, Anim.NONE),
            new Badge("creeper", "CREEPER FREE", "ZONE (LIES)", Icon.SKULL, 0xFF1E7A1E, 0xFF0E3A0E, Pattern.CHECKER, GREEN, Anim.BLINK),
            new Badge("ping", "PING: 999MS", "VERY PLAYABLE", Icon.WIFI, BLACK, 0xFF103010, Pattern.SCANLINES, GREEN, Anim.MARQUEE),
            new Badge("grass", "TOUCH GRASS", "LITERALLY", Icon.BLOCK, 0xFF59C135, 0xFF2D7A1A, Pattern.STRIPES, WHITE, Anim.NONE),
            new Badge("redstone", "POWERED BY", "REDSTONE", Icon.BOLT, 0xFF8A0000, 0xFF2A0000, Pattern.PLASMA, YELLOW, Anim.NONE),
            new Badge("night", "NIGHT MODE", "ALWAYS ON", Icon.MOON, NAVY, BLACK, Pattern.STARS, CYAN, Anim.NONE),
            new Badge("glass", "DIAMONDS?", "NO, GLASS", Icon.GEM, 0xFF00A0B0, 0xFF004050, Pattern.GRADIENT, WHITE, Anim.RAINBOW),
            new Badge("oneblock", "JUST ONE", "MORE BLOCK", Icon.CLOCK, 0xFF5A3A1A, 0xFF2A1A0A, Pattern.STRIPES, YELLOW, Anim.NONE),
            new Badge("nether", "I SURVIVED", "A NETHER TRIP", Icon.FIRE, 0xFFAA2200, 0xFF330000, Pattern.PLASMA, ORANGE, Anim.MARQUEE),
            new Badge("web", "WEB 1.0", "COMPLIANT", Icon.GLOBE, 0xFFC0C0C0, 0xFF909090, Pattern.GRADIENT, NAVY, Anim.NONE),
            new Badge("save", "SAVE EARLY", "SAVE OFTEN", Icon.DISK, 0xFF2040A0, 0xFF101850, Pattern.SCANLINES, WHITE, Anim.NONE),
            new Badge("lag", "LAG INCLUDED", "AT NO EXTRA COST", Icon.CLOCK, 0xFFFFE53B, 0xFFFF8A1F, Pattern.GRADIENT, BLACK, Anim.MARQUEE),
            new Badge("speedrun", "SPEEDRUN", "ANY% (2 HOURS)", Icon.STAR, 0xFF6A0DAD, 0xFF2A0050, Pattern.STRIPES, YELLOW, Anim.NONE),
            new Badge("coffee", "COFFEE", "POWERED BUILD", Icon.COFFEE, 0xFF6B3A1A, 0xFF3A1A0A, Pattern.GRADIENT, 0xFFFFD9A8, Anim.NONE),
            new Badge("cat", "MY PET CAT", "WAS HERE", Icon.CAT, 0xFFFFC0E0, 0xFFFF8AC0, Pattern.CHECKER, 0xFF6A0040, Anim.WAVE),
            new Badge("grind", "GRIND MODE", "ENGAGED", Icon.PICKAXE, 0xFF303030, BLACK, Pattern.SCANLINES, CYAN, Anim.BLINK),
            new Badge("hiscore", "HI-SCORE", "CHASER", Icon.STAR, BLACK, 0xFF2A0B5A, Pattern.STARS, PINK, Anim.RAINBOW),
            new Badge("vibes", "NO CHEATS", "JUST VIBES", Icon.HEART, 0xFF9B4BFF, 0xFFFF2BD6, Pattern.GRADIENT, WHITE, Anim.WAVE),
            new Badge("construction", "UNDER", "CONSTRUCTION", Icon.PICKAXE, YELLOW, BLACK, Pattern.HAZARD, BLACK, Anim.BLINK),
            new Badge("guestbook", "SIGN MY", "GUESTBOOK", Icon.STAR, 0xFF000080, 0xFF0000C0, Pattern.STARS, YELLOW, Anim.NONE),
            new Badge("different", "BUILT", "DIFFERENT", Icon.BLOCK, 0xFF8B5A2B, 0xFF4A2A10, Pattern.CHECKER, WHITE, Anim.NONE),
            new Badge("respawn", "RESPAWN?", "Y / N", Icon.SKULL, 0xFF600000, BLACK, Pattern.SCANLINES, RED, Anim.BLINK),
            new Badge("phantoms", "SLEEP IS", "FOR PHANTOMS", Icon.MOON, 0xFF1A1A50, BLACK, Pattern.STARS, 0xFFB0B0FF, Anim.NONE),
            new Badge("potions", "POTION", "SELLER", Icon.POTION, 0xFFFF2BD6, 0xFF6A0DAD, Pattern.PLASMA, WHITE, Anim.NONE),
            new Badge("homework", "ZOMBIES ATE", "MY HOMEWORK", Icon.SKULL, 0xFF2E5A2E, 0xFF102010, Pattern.STRIPES, GREEN, Anim.MARQUEE),
            new Badge("hugs", "FREE HUGS", "(NOT CREEPERS)", Icon.HEART, 0xFFFFFFFF, 0xFFFFC0D0, Pattern.CHECKER, RED, Anim.MARQUEE),
            new Badge("coin", "INSERT COIN", "TO CONTINUE", Icon.STAR, BLACK, 0xFF202020, Pattern.SCANLINES, YELLOW, Anim.BLINK)
    );

    private Badges() {
    }

    public static Badge byId(String id) {
        for (Badge badge : ALL) {
            if (badge.id().equals(id)) return badge;
        }
        return ALL.getFirst();
    }

    /** Renders a badge in 88x31 space at (x, y). */
    public static void render(GuiGraphics g, Badge badge, int x, int y) {
        long t = millis();
        // classic outer border and bevel
        g.fill(x, y, x + W, y + H, BLACK);
        background(g, badge, x + 1, y + 1, x + W - 1, y + H - 1, t);
        g.fill(x + 1, y + 1, x + W - 1, y + 2, 0x80FFFFFF);
        g.fill(x + 1, y + 1, x + 2, y + H - 1, 0x80FFFFFF);
        g.fill(x + 1, y + H - 2, x + W - 1, y + H - 1, 0x80000000);
        g.fill(x + W - 2, y + 1, x + W - 1, y + H - 1, 0x80000000);

        // icon plate
        g.fill(x + 3, y + 3, x + 28, y + 28, 0x60000000);
        g.fill(x + 3, y + 3, x + 28, y + 4, 0x40FFFFFF);
        int bob = badge.anim() == Anim.WAVE ? (int) Math.round(Math.sin(t / 250.0) * 1.5) : 0;
        sprite(g, x + 4, y + 4 + bob, 3, badge.icon().rows, badge.icon().palette);
        g.fill(x + 28, y + 3, x + 29, y + 28, 0x60000000);

        // text area
        int tx1 = x + 31, tx2 = x + W - 3;
        g.enableScissor(tx1, y + 2, tx2, y + H - 2);
        text(g, badge, badge.line1(), tx1, tx2, y + 6, t, 0);
        text(g, badge, badge.line2(), tx1, tx2, y + 17, t, 1);
        g.disableScissor();
    }

    private static void text(GuiGraphics g, Badge badge, String line, int x1, int x2, int y, long t, int index) {
        float scale = 0.75f;
        int width = (int) (mc.font.width(line) * scale);
        int area = x2 - x1;
        int color = badge.textColor();
        switch (badge.anim()) {
            case BLINK -> {
                if (index == 0 && (t / 450) % 2 == 1) color = alpha(color, 70);
            }
            case RAINBOW -> color = cycle(index * 0.7f);
            default -> {
            }
        }
        float drawX;
        if (width > area || (badge.anim() == Anim.MARQUEE && index == 1)) {
            // scroll text that does not fit, like the real thing
            int loop = width + 16;
            drawX = x2 - (t / 30 % (loop + area));
        } else {
            drawX = x1 + (area - width) / 2f;
        }
        g.pose().pushMatrix();
        g.pose().translate(drawX, y);
        g.pose().scale(scale, scale);
        g.drawString(mc.font, line, 1, 1, 0x90000000, false);
        g.drawString(mc.font, line, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private static void background(GuiGraphics g, Badge badge, int x1, int y1, int x2, int y2, long t) {
        int a = badge.bgA(), b = badge.bgB();
        switch (badge.pattern()) {
            case GRADIENT -> g.fillGradient(x1, y1, x2, y2, a, b);
            case STRIPES -> {
                g.fill(x1, y1, x2, y2, b);
                int offset = (int) (t / 120 % 8);
                for (int x = x1 - 30 + offset; x < x2; x += 8) {
                    for (int dy = 0; dy < y2 - y1; dy++) {
                        int sx = x + dy / 2;
                        if (sx + 3 < x1 || sx >= x2) continue;
                        g.fill(Math.max(x1, sx), y1 + dy, Math.min(x2, sx + 3), y1 + dy + 1, a);
                    }
                }
            }
            case HAZARD -> {
                g.fill(x1, y1, x2, y2, a);
                int offset = (int) (t / 80 % 12);
                for (int x = x1 - 40 + offset; x < x2; x += 12) {
                    for (int dy = 0; dy < y2 - y1; dy++) {
                        int sx = x + dy;
                        if (sx + 6 < x1 || sx >= x2) continue;
                        g.fill(Math.max(x1, sx), y1 + dy, Math.min(x2, sx + 6), y1 + dy + 1, b);
                    }
                }
                g.fill(x1 + 30, y1 + 3, x2 - 2, y2 - 3, 0xD0FFE53B);
            }
            case CHECKER -> {
                for (int x = x1; x < x2; x += 4)
                    for (int y = y1; y < y2; y += 4)
                        g.fill(x, y, Math.min(x2, x + 4), Math.min(y2, y + 4), ((x - x1) / 4 + (y - y1) / 4) % 2 == 0 ? a : b);
            }
            case STARS -> {
                g.fillGradient(x1, y1, x2, y2, a, b);
                for (int i = 0; i < 18; i++) {
                    int sx = x1 + (int) ((i * 37L) % (x2 - x1));
                    int sy = y1 + (int) ((i * 53L) % (y2 - y1));
                    boolean on = ((t / 200) + i) % 5 != 0;
                    if (on) g.fill(sx, sy, sx + 1, sy + 1, i % 3 == 0 ? WHITE : 0xA0FFFFFF);
                }
            }
            case SCANLINES -> {
                g.fill(x1, y1, x2, y2, a);
                int roll = (int) (t / 60 % (y2 - y1));
                for (int y = y1; y < y2; y += 2) g.fill(x1, y, x2, y + 1, alpha(b, 160));
                g.fill(x1, y1 + roll, x2, y1 + roll + 2, 0x20FFFFFF);
            }
            case PLASMA -> {
                double time = t / 600.0;
                for (int x = x1; x < x2; x += 3) {
                    for (int y = y1; y < y2; y += 3) {
                        double v = Math.sin(x * 0.12 + time) + Math.sin(y * 0.2 - time) + Math.sin((x + y) * 0.07 + time * 1.3);
                        g.fill(x, y, Math.min(x2, x + 3), Math.min(y2, y + 3), mix(a, b, (float) (v + 3) / 6f));
                    }
                }
            }
        }
    }
}
