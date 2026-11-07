package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * The screensaver classic: a logo bouncing around your screen. Hitting a corner exactly
 * throws a little party.
 */
public class BouncingLogoModule extends Module {
    private static final int[] COLORS = {PINK, CYAN, YELLOW, GREEN, ORANGE, PURPLE, RED};
    private static final int LOGO_W = 52;
    private static final int LOGO_H = 22;
    private static final String[] MOON = {
            "..111...",
            ".11.....",
            "11......",
            "11......",
            "11......",
            "11....1.",
            ".11111..",
            "..111...",
    };

    public final Setting<Float> speed = num("Speed", 60f, 10f, 250f);
    public final Setting<Boolean> counter = bool("CornerCounter", true);

    private final Random random = new Random();
    private final List<float[]> confetti = new ArrayList<>(); // x, y, vx, vy, life, color
    private float x = 40, y = 40, dirX = 1, dirY = 1;
    private int colorIndex;
    private int corners;
    private long lastFrame;
    private long partyUntil;

    public BouncingLogoModule() {
        super("BouncingLogo", "A screensaver logo bouncing around your screen", Category.FUN);
    }

    @Override
    public void onEnable() {
        lastFrame = 0;
        dirX = random.nextBoolean() ? 1 : -1;
        dirY = random.nextBoolean() ? 1 : -1;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        GuiGraphics g = event.getContext();
        int w = g.guiWidth(), h = g.guiHeight();
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        x += dirX * speed.getValue() * dt;
        y += dirY * speed.getValue() * dt;
        boolean hitX = false, hitY = false;
        if (x <= 0) {
            x = 0;
            dirX = 1;
            hitX = true;
        } else if (x + LOGO_W >= w) {
            x = w - LOGO_W;
            dirX = -1;
            hitX = true;
        }
        if (y <= 0) {
            y = 0;
            dirY = 1;
            hitY = true;
        } else if (y + LOGO_H >= h) {
            y = h - LOGO_H;
            dirY = -1;
            hitY = true;
        }
        if (hitX || hitY) colorIndex = (colorIndex + 1 + random.nextInt(COLORS.length - 1)) % COLORS.length;
        // close enough to both walls at once counts as a corner hit
        boolean nearX = x <= 2 || x + LOGO_W >= w - 2, nearY = y <= 2 || y + LOGO_H >= h - 2;
        if ((hitX && nearY) || (hitY && nearX)) corner();

        int color = COLORS[colorIndex];
        int lx = (int) x, ly = (int) y;
        g.fill(lx, ly, lx + LOGO_W, ly + LOGO_H, alpha(color, 50));
        neonBox(g, lx, ly, lx + LOGO_W, ly + LOGO_H, color);
        sprite(g, lx + 3, ly + 3, 2, MOON, color);
        g.pose().pushMatrix();
        g.pose().translate(lx + 18, ly + 4);
        g.pose().scale(1.5f, 1.5f);
        g.drawString(mc.font, "NYX", 0, 0, color, false);
        g.pose().popMatrix();
        g.pose().pushMatrix();
        g.pose().translate(lx + 19, ly + 16);
        g.pose().scale(0.5f, 0.5f);
        g.drawString(mc.font, "UTILS", 0, 0, alpha(color, 200), false);
        g.pose().popMatrix();

        updateConfetti(g, dt, h);
        if (counter.getValue() && corners > 0) {
            String text = "CORNER HITS: " + corners;
            g.drawString(mc.font, text, w - mc.font.width(text) - 4, 4, millis() < partyUntil ? cycle(0) : 0xA0FFFFFF, true);
        }
    }

    private void corner() {
        corners++;
        partyUntil = millis() + 2500;
        ArcadeSound.levelUp();
        for (int i = 0; i < 70; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float power = 40 + random.nextFloat() * 120;
            confetti.add(new float[]{x + LOGO_W / 2f, y + LOGO_H / 2f,
                    (float) Math.cos(angle) * power, (float) Math.sin(angle) * power - 60,
                    2.5f, COLORS[random.nextInt(COLORS.length)]});
        }
    }

    private void updateConfetti(GuiGraphics g, float dt, int h) {
        confetti.removeIf(p -> (p[4] -= dt) <= 0 || p[1] > h + 10);
        for (float[] p : confetti) {
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            p[3] += 140 * dt;
            p[2] *= 1 - dt;
            int flip = ((int) (p[4] * 10)) % 2;
            g.fill((int) p[0], (int) p[1], (int) p[0] + 1 + flip, (int) p[1] + 2 - flip, (int) p[5]);
        }
    }

    @Override
    public String getDisplayInfo() {
        return corners > 0 ? String.valueOf(corners) : null;
    }
}
