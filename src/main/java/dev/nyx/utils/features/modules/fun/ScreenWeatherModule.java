package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.settings.Setting;
import dev.nyx.utils.util.render.QuadBatch;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * Decorative particles drifting over your screen.
 */
public class ScreenWeatherModule extends Module {
    public enum Weather {SNOW, HEARTS, SAKURA, CONFETTI, STARS}

    private static final String[] HEART = {
            "11.11",
            "11111",
            ".111.",
            "..1..",
    };
    private static final int[][] HEART_RUNS = runs(HEART);
    private static final int[] CONFETTI_COLORS = {PINK, CYAN, YELLOW, GREEN, ORANGE, PURPLE};

    public final Setting<Weather> weather = mode("Weather", Weather.SNOW);
    public final Setting<Integer> amount = num("Amount", 80, 10, 300);

    private final Random random = new Random();
    private final QuadBatch batch = new QuadBatch();
    private final List<float[]> particles = new ArrayList<>(); // x, y, vx, vy, size, phase, color
    private Weather lastWeather;
    private long lastFrame;

    public ScreenWeatherModule() {
        super("ScreenWeather", "Snow, hearts, petals or confetti over your screen", Category.FUN);
    }

    @Override
    public void onEnable() {
        particles.clear();
        lastFrame = 0;
    }

    private float[] spawn(int w, int h, boolean anywhere) {
        Weather type = weather.getValue();
        float x = random.nextFloat() * w;
        float y = anywhere ? random.nextFloat() * h : -8;
        float size = 1 + random.nextInt(2);
        float vx = 0, vy;
        int color;
        switch (type) {
            case HEARTS -> {
                vy = 12 + random.nextFloat() * 14;
                color = random.nextBoolean() ? PINK : RED;
            }
            case SAKURA -> {
                vx = 12 + random.nextFloat() * 14;
                vy = 14 + random.nextFloat() * 12;
                color = random.nextBoolean() ? 0xFFFFB7D5 : 0xFFFF8FC0;
            }
            case CONFETTI -> {
                vx = (random.nextFloat() - 0.5f) * 20;
                vy = 30 + random.nextFloat() * 40;
                color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.length)];
            }
            case STARS -> {
                vx = -40 - random.nextFloat() * 60;
                vy = 25 + random.nextFloat() * 30;
                color = random.nextInt(4) == 0 ? YELLOW : WHITE;
                if (!anywhere) x = random.nextFloat() * w * 1.5f;
            }
            default -> {
                vy = 10 + size * 8 + random.nextFloat() * 10;
                color = random.nextInt(3) == 0 ? 0xC0FFFFFF : WHITE;
            }
        }
        // the colour is stored bit-for-bit: a plain float cast would round ARGB values like 0xC0FFFFFF
        return new float[]{x, y, vx, vy, size, random.nextFloat() * 6.28f, Float.intBitsToFloat(color)};
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        GuiGraphics g = event.getContext();
        int w = g.guiWidth(), h = g.guiHeight();
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        if (lastWeather != weather.getValue()) {
            particles.clear();
            lastWeather = weather.getValue();
        }
        // after a fresh start, spread the first batch over the whole screen instead of one band at the top
        boolean refill = particles.isEmpty();
        while (particles.size() < amount.getValue()) particles.add(spawn(w, h, refill));
        while (particles.size() > amount.getValue()) particles.removeLast();

        float time = millis() / 1000f;
        batch.begin();
        for (int i = 0; i < particles.size(); i++) {
            float[] p = particles.get(i);
            float sway = (float) Math.sin(time * 1.5f + p[5]);
            p[0] += (p[2] + sway * 8) * dt;
            p[1] += p[3] * dt;
            if (p[1] > h + 8 || p[0] < -20 || p[0] > w * 1.6f) {
                particles.set(i, spawn(w, h, false));
                continue;
            }
            draw(p, sway, time);
        }
        batch.submit(g);
    }

    private void draw(float[] p, float sway, float time) {
        int x = (int) p[0], y = (int) p[1], size = (int) p[4], color = Float.floatToRawIntBits(p[6]);
        switch (weather.getValue()) {
            case HEARTS -> {
                for (int[] run : HEART_RUNS) {
                    batch.add(x + run[1] * size, y + run[0] * size, x + run[2] * size, y + (run[0] + 1) * size, color);
                }
            }
            case SAKURA -> {
                boolean wide = sway > 0;
                batch.add(x, y, x + (wide ? 3 : 2), y + (wide ? 2 : 3), color);
                batch.add(x + 1, y + 1, x + 2, y + 2, 0xFFFFE0EE);
            }
            case CONFETTI -> {
                float spin = (float) Math.abs(Math.sin(time * 6 + p[5]));
                batch.add(x, y, x + Math.max(1, Math.round(3 * spin)), y + 2, color);
            }
            case STARS -> {
                batch.add(x, y, x + 2, y + 1, color);
                for (int t = 1; t <= 4; t++) batch.add(x + t * 2, y - t, x + t * 2 + 2, y - t + 1, alpha(color, 160 - t * 35));
            }
            default -> batch.add(x, y, x + size, y + size, color);
        }
    }

    /** Horizontal runs of the heart sprite as {row, from, to}, so a heart is 5 rects instead of 14. */
    private static int[][] runs(String[] rows) {
        List<int[]> out = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            int start = -1;
            for (int c = 0; c <= rows[r].length(); c++) {
                boolean filled = c < rows[r].length() && rows[r].charAt(c) == '1';
                if (filled && start < 0) start = c;
                if (!filled && start >= 0) {
                    out.add(new int[]{r, start, c});
                    start = -1;
                }
            }
        }
        return out.toArray(new int[0][]);
    }

    @Override
    public String getDisplayInfo() {
        return weather.getValue().name();
    }
}
