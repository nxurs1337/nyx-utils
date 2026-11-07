package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class GateHopperGame extends ArcadeGame {
    private static final int W = 160;
    private static final int H = 200;
    private static final int GROUND = H - 16;
    private static final int GATE_W = 16;
    private static final int PLAYER_X = 40;
    private static final int SIZE = 8;

    private static final String[] JET = {
            "..1111..",
            ".122221.",
            "12233221",
            "12233221",
            "12222221",
            ".122221.",
            "..1..1..",
    };

    private final Random random = new Random();
    private final List<float[]> gates = new ArrayList<>(); // x, gapY, gapSize, passed
    private float y, vy;
    private float speed;
    private float scroll;
    private int spawnTimer;
    private int flameTicks;

    public GateHopperGame() {
        super("gatehopper", "GATE HOPPER", ArcadeRender.YELLOW, W, H);
    }

    @Override
    protected void reset() {
        gates.clear();
        y = H / 2f - 20;
        vy = -2f;
        speed = 1.3f;
        spawnTimer = 30;
        flameTicks = 0;
    }

    @Override
    protected void onKey(int key) {
        if (key == GLFW_KEY_SPACE || key == GLFW_KEY_UP || key == GLFW_KEY_W) flap();
    }

    @Override
    protected void onMouse(int x, int y, int button) {
        if (button == 0) flap();
    }

    private void flap() {
        vy = -3.1f;
        flameTicks = 8;
        ArcadeSound.blip();
    }

    @Override
    protected void update() {
        scroll += speed;
        vy = Math.min(4.5f, vy + 0.19f);
        y += vy;
        if (flameTicks > 0) flameTicks--;

        if (--spawnTimer <= 0) {
            float gap = Math.max(46, 64 - score * 0.6f);
            float gapY = 18 + random.nextFloat() * (GROUND - 36 - gap);
            gates.add(new float[]{W, gapY, gap, 0});
            spawnTimer = (int) (100 / speed);
        }

        for (float[] gate : gates) {
            gate[0] -= speed;
            if (gate[3] == 0 && gate[0] + GATE_W < PLAYER_X) {
                gate[3] = 1;
                score++;
                speed = Math.min(2.6f, speed + 0.03f);
                ArcadeSound.score();
            }
            boolean overlapX = PLAYER_X + SIZE > gate[0] && PLAYER_X < gate[0] + GATE_W;
            boolean inGap = y > gate[1] && y + SIZE - 1 < gate[1] + gate[2];
            if (overlapX && !inGap) {
                crash();
                return;
            }
        }
        gates.removeIf(gate -> gate[0] < -GATE_W);

        if (y + SIZE >= GROUND) {
            y = GROUND - SIZE;
            crash();
        }
        if (y < -10) {
            y = -10;
            vy = 0;
        }
    }

    private void crash() {
        ArcadeSound.explode();
        gameOver(false);
    }

    @Override
    public void render(GuiGraphics g) {
        g.fillGradient(0, 0, W, GROUND, 0xFF14063A, 0xFF5A1466);
        // skyline layers
        skyline(g, 0.25f, 0xFF2A0D52, 40, 7);
        skyline(g, 0.5f, 0xFF1A0838, 26, 13);

        for (float[] gate : gates) {
            int gx = (int) gate[0];
            int top = (int) gate[1], bottom = (int) (gate[1] + gate[2]);
            pillar(g, gx, 0, top);
            pillar(g, gx, bottom, GROUND);
            // laser across the gap edges
            int laser = ArcadeRender.blink(120) ? ArcadeRender.PINK : ArcadeRender.mix(ArcadeRender.PINK, ArcadeRender.WHITE, 0.5f);
            g.fill(gx - 2, top - 3, gx + GATE_W + 2, top, laser);
            g.fill(gx - 2, bottom, gx + GATE_W + 2, bottom + 3, laser);
        }

        // ground
        g.fill(0, GROUND, W, H, 0xFF0A0418);
        g.fill(0, GROUND, W, GROUND + 1, ArcadeRender.CYAN);
        int offset = (int) (scroll % 12);
        for (int x = -offset; x < W; x += 12) g.fill(x, GROUND + 4, x + 6, GROUND + 5, 0x8022F3FF);

        int py = (int) y;
        double tilt = Math.max(-1, Math.min(1, vy / 3));
        if (flameTicks > 0 || state == State.ATTRACT) {
            int len = 3 + (ArcadeRender.blink(60) ? 2 : 0);
            g.fill(PLAYER_X + 2, py + 7, PLAYER_X + 3, py + 7 + len, ArcadeRender.ORANGE);
            g.fill(PLAYER_X + 5, py + 7, PLAYER_X + 6, py + 7 + len, ArcadeRender.ORANGE);
        }
        ArcadeRender.sprite(g, PLAYER_X, py + (int) Math.round(tilt), 1, JET, ArcadeRender.CYAN, 0xFF1B6FA8, ArcadeRender.YELLOW);

        if (state == State.PLAYING || state == State.PAUSED || state == State.GAME_OVER) {
            ArcadeRender.bigText(g, String.valueOf(score), W / 2, 10, 2f, ArcadeRender.WHITE);
        }
    }

    private void pillar(GuiGraphics g, int x, int y1, int y2) {
        if (y2 <= y1) return;
        g.fill(x, y1, x + GATE_W, y2, 0xFF1C0A3A);
        g.fill(x, y1, x + 2, y2, ArcadeRender.PURPLE);
        g.fill(x + GATE_W - 2, y1, x + GATE_W, y2, ArcadeRender.PURPLE);
        for (int yy = y1 + 3; yy < y2; yy += 8) g.fill(x + 4, yy, x + GATE_W - 4, yy + 1, 0x409B4BFF);
    }

    private void skyline(GuiGraphics g, float parallax, int color, int maxHeight, int seed) {
        int offset = (int) (scroll * parallax);
        for (int i = -1; i < W / 14 + 2; i++) {
            int index = i + offset / 14;
            int h = 10 + (int) Math.abs((index * 7919L + seed * 31L) % maxHeight);
            int x = i * 14 - offset % 14;
            g.fill(x, GROUND - h, x + 12, GROUND, color);
            if ((index + seed) % 3 == 0) g.fill(x + 4, GROUND - h + 4, x + 6, GROUND - h + 6, 0x60FFE53B);
        }
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, 0xFF14063A, 0xFF5A1466);
        int off = (int) ((ArcadeRender.millis() / 40) % (w + 10));
        int gx = x + w - off;
        if (gx > x - 6) {
            g.fill(Math.max(x, gx), y, Math.min(x + w, gx + 6), y + h / 3, ArcadeRender.PURPLE);
            g.fill(Math.max(x, gx), y + h * 2 / 3, Math.min(x + w, gx + 6), y + h, ArcadeRender.PURPLE);
        }
        int bob = (int) (Math.sin(ArcadeRender.millis() / 200.0) * 4);
        ArcadeRender.sprite(g, x + 8, y + h / 2 - 4 + bob, 1, JET, ArcadeRender.CYAN, 0xFF1B6FA8, ArcadeRender.YELLOW);
    }

    @Override
    public String[] controls() {
        return new String[]{"SPACE/UP/CLICK: BOOST", "FLY THROUGH THE GATES", "MIND THE LASERS"};
    }
}
