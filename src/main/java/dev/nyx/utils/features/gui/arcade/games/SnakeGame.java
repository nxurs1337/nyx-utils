package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class SnakeGame extends ArcadeGame {
    private static final int GRID = 20;
    private static final int CELL = 8;

    private final Random random = new Random();
    private final LinkedList<int[]> body = new LinkedList<>();
    private final ArrayDeque<int[]> queued = new ArrayDeque<>();
    private int dx, dy;
    private int foodX, foodY;
    private int timer, interval;
    private int grow;

    public SnakeGame() {
        super("snake", "NEON SNAKE", ArcadeRender.GREEN, GRID * CELL, GRID * CELL);
    }

    @Override
    protected void reset() {
        body.clear();
        queued.clear();
        for (int i = 0; i < 4; i++) body.add(new int[]{8 - i, 10});
        dx = 1;
        dy = 0;
        interval = 9;
        timer = 0;
        grow = 0;
        placeFood();
    }

    private void placeFood() {
        do {
            foodX = random.nextInt(GRID);
            foodY = random.nextInt(GRID);
        } while (occupied(foodX, foodY));
    }

    private boolean occupied(int x, int y) {
        for (int[] part : body) {
            if (part[0] == x && part[1] == y) return true;
        }
        return false;
    }

    @Override
    protected void onKey(int key) {
        int[] dir = switch (key) {
            case GLFW_KEY_UP, GLFW_KEY_W -> new int[]{0, -1};
            case GLFW_KEY_DOWN, GLFW_KEY_S -> new int[]{0, 1};
            case GLFW_KEY_LEFT, GLFW_KEY_A -> new int[]{-1, 0};
            case GLFW_KEY_RIGHT, GLFW_KEY_D -> new int[]{1, 0};
            default -> null;
        };
        if (dir == null || queued.size() >= 3) return;
        int[] last = queued.isEmpty() ? new int[]{dx, dy} : queued.peekLast();
        if (dir[0] == -last[0] && dir[1] == -last[1]) return;
        if (dir[0] == last[0] && dir[1] == last[1]) return;
        queued.add(dir);
    }

    @Override
    protected void update() {
        if (++timer < interval) return;
        timer = 0;
        if (!queued.isEmpty()) {
            int[] dir = queued.poll();
            dx = dir[0];
            dy = dir[1];
        }
        int[] head = body.getFirst();
        int nx = head[0] + dx, ny = head[1] + dy;
        if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID) {
            gameOver(false);
            return;
        }
        if (grow == 0) body.removeLast();
        else grow--;
        if (occupied(nx, ny)) {
            gameOver(false);
            return;
        }
        body.addFirst(new int[]{nx, ny});
        if (nx == foodX && ny == foodY) {
            score += 10;
            grow += 2;
            interval = Math.max(3, 9 - score / 60);
            ArcadeSound.pickup();
            if (body.size() + grow >= GRID * GRID) {
                gameOver(true);
                return;
            }
            placeFood();
        }
    }

    @Override
    public void render(GuiGraphics g) {
        g.fill(0, 0, getWidth(), getHeight(), 0xFF050A08);
        for (int i = 0; i <= GRID; i++) {
            g.fill(i * CELL, 0, i * CELL + 1, getHeight(), 0x1422FF66);
            g.fill(0, i * CELL, getWidth(), i * CELL + 1, 0x1422FF66);
        }
        int pulse = ArcadeRender.blink(250) ? 1 : 0;
        g.fill(foodX * CELL + 1 - pulse, foodY * CELL + 1 - pulse, foodX * CELL + CELL - 1 + pulse, foodY * CELL + CELL - 1 + pulse, 0x55FF2BD6);
        g.fill(foodX * CELL + 2, foodY * CELL + 2, foodX * CELL + CELL - 2, foodY * CELL + CELL - 2, ArcadeRender.PINK);

        int i = 0;
        for (int[] p : body) {
            int color = ArcadeRender.mix(ArcadeRender.GREEN, ArcadeRender.CYAN, i++ / (float) Math.max(1, body.size()));
            g.fill(p[0] * CELL + 1, p[1] * CELL + 1, p[0] * CELL + CELL - 1, p[1] * CELL + CELL - 1, color);
        }
        if (!body.isEmpty()) {
            int[] h = body.getFirst();
            int ex = h[0] * CELL + 3 + dx, ey = h[1] * CELL + 3 + dy;
            g.fill(ex, ey, ex + 2, ey + 2, 0xFF000000);
        }
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        long t = ArcadeRender.millis() / 150;
        int cols = w / 4 - 1;
        int len = 8;
        for (int i = 0; i < len; i++) {
            int pos = (int) Math.floorMod(t - i, (long) cols * 2);
            int col = pos < cols ? pos : cols * 2 - pos - 1;
            int row = pos < cols ? 0 : 1;
            int px = x + 2 + col * 4, py = y + h / 2 - 4 + row * 5;
            g.fill(px, py, px + 3, py + 3, ArcadeRender.mix(ArcadeRender.GREEN, ArcadeRender.CYAN, i / (float) len));
        }
        g.fill(x + w / 3, y + h / 4, x + w / 3 + 3, y + h / 4 + 3, ArcadeRender.PINK);
        g.fill(x + w * 2 / 3, y + h * 3 / 4, x + w * 2 / 3 + 3, y + h * 3 / 4 + 3, ArcadeRender.PINK);
    }

    @Override
    public String[] controls() {
        return new String[]{"ARROWS/WASD: STEER", "EAT THE GEMS", "DON'T BITE YOURSELF"};
    }

    @Override
    public String getStatus() {
        return "LENGTH " + body.size();
    }
}
