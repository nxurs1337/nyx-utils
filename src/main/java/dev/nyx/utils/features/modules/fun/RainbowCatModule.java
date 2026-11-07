package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.settings.Setting;
import dev.nyx.utils.util.render.QuadBatch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * A caped pixel cat flying over your screen, leaving a stepped rainbow trail and twinkling stars.
 */
public class RainbowCatModule extends Module {
    public enum Flight {CROSS, WANDER}

    private static final int[] RAINBOW = {0xFFFF2A2A, 0xFFFF9A1F, 0xFFFFE53B, 0xFF4BFF4B, 0xFF2AA8FF, 0xFF8A4BFF};
    private static final String[][] CAT = {{
            "............1.1.",
            "....4444...11111",
            ".....4444..11211",
            "1.....4411111113",
            ".1.1111111111111",
            "..1111111111111.",
            "..11111111111...",
            ".11..........11.",
    }, {
            "............1.1.",
            "...4444....11111",
            "....4444...11211",
            "1.....4411111113",
            ".1.1111111111111",
            "..1111111111111.",
            "..11111111111...",
            ".1.1........1.1.",
    }};

    public final Setting<Flight> flight = mode("Flight", Flight.CROSS);
    public final Setting<Integer> scale = num("Scale", 2, 1, 5);
    public final Setting<Float> speed = num("Speed", 90f, 20f, 300f);
    public final Setting<Integer> trail = num("Trail", 40, 5, 120);
    public final Setting<Boolean> stars = bool("Stars", true);
    public final Setting<Boolean> meows = bool("Meows", false);

    private final Random random = new Random();
    private final QuadBatch batch = new QuadBatch();
    private final List<float[]> points = new ArrayList<>(); // trail points: x, y
    private final List<float[]> sparkles = new ArrayList<>(); // x, y, vx, life
    private float x = -40, y = 60, vx = 1, vy = 0.6f;
    private float wave;
    private long lastFrame;
    private float meowTimer = 8;

    public RainbowCatModule() {
        super("RainbowCat", "A caped cat flies over your screen on a rainbow", Category.FUN);
    }

    @Override
    public void onEnable() {
        points.clear();
        sparkles.clear();
        lastFrame = 0;
        x = -20 * scale.getValue();
        vx = 1;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        GuiGraphics g = event.getContext();
        int w = g.guiWidth(), h = g.guiHeight();
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        int p = scale.getValue();
        int spriteW = 16 * p, spriteH = 8 * p;

        move(dt, w, h, spriteW, spriteH);
        boolean facingLeft = vx < 0;

        // the trail leaves from the cat's tail
        float tailX = facingLeft ? x + spriteW : x;
        float tailY = y + 2 * p;
        float step = 4f * p;
        if (points.isEmpty() || Math.hypot(tailX - points.getLast()[0], tailY - points.getLast()[1]) >= step) {
            points.add(new float[]{tailX, tailY});
        }
        while (points.size() > trail.getValue()) points.removeFirst();

        batch.begin();
        drawTrail(p, tailX, tailY);
        if (stars.getValue()) drawSparkles(dt, w, h, p);
        batch.submit(g);

        String[] frame = CAT[(int) ((millis() / 120) % 2)];
        if (facingLeft) frame = mirror(frame);
        sprite(g, (int) x, (int) y, p, frame, 0xFFB8B8C8, 0xFF101018, 0xFFFF8AC0, 0xFFE02A3A);

        if (meows.getValue()) {
            meowTimer -= dt;
            if (meowTimer <= 0) {
                meowTimer = 10 + random.nextFloat() * 20;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CAT_AMBIENT, 0.9f + random.nextFloat() * 0.3f, 0.35f));
            }
        }
    }

    private void move(float dt, int w, int h, int spriteW, int spriteH) {
        float v = speed.getValue();
        wave += dt * 3;
        if (flight.getValue() == Flight.CROSS) {
            vx = 1;
            x += v * dt;
            y = h * 0.3f + (float) Math.sin(wave) * h * 0.08f;
            if (x > w + spriteW) {
                // loop around, entering at a fresh height
                x = -spriteW - trail.getValue() * 4f * scale.getValue();
                points.clear();
            }
        } else {
            x += vx * v * dt;
            y += vy * v * dt + (float) Math.cos(wave) * 0.4f;
            if (x < 0) {
                x = 0;
                vx = 1;
            } else if (x + spriteW > w) {
                x = w - spriteW;
                vx = -1;
            }
            if (y < 0) {
                y = 0;
                vy = Math.abs(vy);
            } else if (y + spriteH > h) {
                y = h - spriteH;
                vy = -Math.abs(vy);
            }
        }
    }

    private void drawTrail(int p, float tailX, float tailY) {
        int bands = RAINBOW.length;
        for (int i = 0; i < points.size(); i++) {
            float[] a = points.get(i);
            float bx = i + 1 < points.size() ? points.get(i + 1)[0] : tailX;
            int x1 = (int) Math.min(a[0], bx), x2 = (int) Math.max(a[0], bx);
            if (x2 - x1 < 1) x2 = x1 + Math.max(1, p);
            // stepped wave: every other segment sits one pixel lower
            int offset = ((i + (int) (millis() / 150)) % 2) * p;
            int fade = (int) (255 * Math.min(1, (i + 1) / 6f));
            for (int b = 0; b < bands; b++) {
                int by = (int) a[1] + offset + b * p;
                batch.add(x1, by, x2 + 1, by + p, alpha(RAINBOW[b], fade));
            }
        }
    }

    private void drawSparkles(float dt, int w, int h, int p) {
        if (random.nextFloat() < dt * 10) {
            sparkles.add(new float[]{random.nextFloat() * w, random.nextFloat() * h, -40 - random.nextFloat() * 60, 1.2f});
        }
        sparkles.removeIf(s -> (s[3] -= dt) <= 0);
        for (float[] s : sparkles) {
            s[0] += s[2] * dt;
            int sx = (int) s[0], sy = (int) s[1];
            int arm = s[3] > 0.8f ? 1 : s[3] > 0.4f ? 2 : 1;
            int color = alpha(WHITE, (int) (255 * Math.min(1, s[3])));
            batch.add(sx, sy, sx + p, sy + p, color);
            batch.add(sx - arm * p, sy, sx, sy + p, color);
            batch.add(sx + p, sy, sx + p + arm * p, sy + p, color);
            batch.add(sx, sy - arm * p, sx + p, sy, color);
            batch.add(sx, sy + p, sx + p, sy + p + arm * p, color);
        }
    }

    private static String[] mirror(String[] rows) {
        String[] out = new String[rows.length];
        for (int i = 0; i < rows.length; i++) out[i] = new StringBuilder(rows[i]).reverse().toString();
        return out;
    }

    @Override
    public String getDisplayInfo() {
        return flight.getValue().name();
    }
}
