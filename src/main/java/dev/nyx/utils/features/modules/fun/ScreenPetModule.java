package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * A tiny pixel cat living next to your hotbar. It wanders, sits, naps when you go idle,
 * hops when you jump and panics when you get hurt.
 */
public class ScreenPetModule extends Module {
    public enum Coat {
        ORANGE(0xFFFF9A3C), BLACK(0xFF2A2A2A), WHITE(0xFFF2F2F2), GRAY(0xFF8A8A9A), PINK(0xFFFF9AD0);

        final int color;

        Coat(int color) {
            this.color = color;
        }
    }

    private enum State {WALK, SIT, SLEEP}

    private static final String[][] WALK = {{
            ".........1.1",
            "1........111",
            "1........121",
            ".1.......111",
            "..11111111..",
            "..11111111..",
            "..1.1...1.1.",
    }, {
            ".........1.1",
            ".1.......111",
            "1........121",
            "1........111",
            ".111111111..",
            "..11111111..",
            "...1.1..1.1.",
    }};
    private static final String[] SIT = {
            ".........1.1",
            ".........111",
            ".........121",
            "1.......1111",
            ".1....11111.",
            "..1111111...",
            "..1111111...",
            "..1.1.11....",
    };
    private static final String[] SLEEP = {
            "............",
            "............",
            "............",
            "....11111.1.",
            "..111111111.",
            ".1111111131.",
            "11111111111.",
    };

    public final Setting<Coat> coat = mode("Coat", Coat.ORANGE);
    public final Setting<String> name = str("Name", "Nyx");
    public final Setting<Boolean> showName = bool("ShowName", true);
    public final Setting<Integer> scale = num("Scale", 3, 1, 6);

    private final Random random = new Random();
    private State state = State.WALK;
    private float offset, target;
    private float hopY, hopVelocity;
    private float stateTimer = 2;
    private boolean facingLeft;
    private float idleTime;
    private double lastX, lastY, lastZ;
    private float lastYaw;
    private long lastFrame;

    public ScreenPetModule() {
        super("ScreenPet", "A little pixel cat that lives next to your hotbar", Category.FUN);
    }

    @Override
    public void onEnable() {
        lastFrame = 0;
        state = State.WALK;
        idleTime = 0;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;
        GuiGraphics g = event.getContext();
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        // the strip to the right of the hotbar is where the pet lives
        int p = scale.getValue();
        int minX = g.guiWidth() / 2 + 96, maxX = Math.max(minX + 20, g.guiWidth() - 4 - 12 * p);
        int ground = g.guiHeight() - 2;
        float range = maxX - minX;

        trackPlayer(dt);
        boolean hurt = mc.player.hurtTime > 0;

        // hop when the player jumps
        if (hopY == 0 && !mc.player.onGround() && mc.player.getDeltaMovement().y > 0.2 && state != State.SLEEP) {
            hopVelocity = -70;
        }
        if (hopVelocity != 0 || hopY < 0) {
            hopVelocity += 260 * dt;
            hopY = Math.min(0, hopY + hopVelocity * dt);
            if (hopY == 0) hopVelocity = 0;
        }

        if (idleTime > 20 && state != State.SLEEP) state = State.SLEEP;
        if (state == State.SLEEP && idleTime < 1) {
            state = State.WALK;
            target = random.nextFloat() * range;
        }

        stateTimer -= dt;
        float speed = (hurt ? 60 : 16) * Math.max(1, p / 2f);
        switch (state) {
            case WALK -> {
                float diff = target - offset;
                if (Math.abs(diff) < 1) {
                    if (stateTimer <= 0) {
                        state = random.nextInt(3) == 0 ? State.WALK : State.SIT;
                        stateTimer = 2 + random.nextFloat() * 5;
                        target = random.nextFloat() * range;
                    }
                } else {
                    facingLeft = diff < 0;
                    offset += Math.signum(diff) * Math.min(Math.abs(diff), speed * dt);
                }
            }
            case SIT -> {
                if (stateTimer <= 0 || hurt) {
                    state = State.WALK;
                    stateTimer = 1 + random.nextFloat() * 3;
                    target = random.nextFloat() * range;
                }
            }
            default -> {
            }
        }
        if (hurt && state == State.WALK && Math.abs(target - offset) < 4) target = random.nextFloat() * range;
        offset = Math.max(0, Math.min(range, offset));

        String[] frame = switch (state) {
            case WALK -> WALK[(int) ((millis() / (hurt ? 90 : 180)) % 2)];
            case SIT -> SIT;
            case SLEEP -> SLEEP;
        };
        if (facingLeft) frame = mirror(frame);
        int x = (int) (minX + offset), y = ground - frame.length * p + (int) (hopY * p);
        int coatColor = coat.getValue().color;
        // soft shadow
        g.fill(x + 2 * p, ground, x + 10 * p, ground + 1, 0x50000000);
        sprite(g, x, y, p, frame, coatColor, 0xFF4BFF6B, 0xFFFF9AD0);

        if (state == State.SLEEP) {
            float z = (millis() % 2400) / 2400f;
            g.pose().pushMatrix();
            g.pose().translate(x + 9 * p + z * 6 * p, y + p - z * 10 * p);
            g.pose().scale((0.5f + z * 0.3f) * p, (0.5f + z * 0.3f) * p);
            g.drawString(mc.font, "z", 0, 0, alpha(WHITE, (int) (255 * (1 - z))), false);
            g.pose().popMatrix();
        } else if (hurt) {
            g.drawString(mc.font, "!", x + 5 * p, y - 10, RED, true);
        }
        if (showName.getValue() && !name.getValue().isBlank()) {
            g.pose().pushMatrix();
            g.pose().translate(x + 6 * p, y - 6 + (state == State.SLEEP ? 3 * p : 0));
            g.pose().scale(0.5f, 0.5f);
            String label = name.getValue();
            g.drawString(mc.font, label, -mc.font.width(label) / 2, 0, 0xC0FFFFFF, true);
            g.pose().popMatrix();
        }
    }

    private void trackPlayer(float dt) {
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        float yaw = mc.player.getYRot();
        boolean moved = Math.abs(x - lastX) + Math.abs(y - lastY) + Math.abs(z - lastZ) > 0.001 || Math.abs(yaw - lastYaw) > 0.5f;
        idleTime = moved ? 0 : idleTime + dt;
        lastX = x;
        lastY = y;
        lastZ = z;
        lastYaw = yaw;
    }

    private static String[] mirror(String[] rows) {
        String[] out = new String[rows.length];
        for (int i = 0; i < rows.length; i++) out[i] = new StringBuilder(rows[i]).reverse().toString();
        return out;
    }
}
