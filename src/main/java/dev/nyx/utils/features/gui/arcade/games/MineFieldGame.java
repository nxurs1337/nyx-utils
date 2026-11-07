package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class MineFieldGame extends ArcadeGame {
    private static final int COLS = 14;
    private static final int ROWS = 12;
    private static final int CELL = 12;
    private static final int TOP = 14;
    private static final int MINES = 24;
    private static final int[] NUMBER_COLORS = {
            0, ArcadeRender.CYAN, ArcadeRender.GREEN, ArcadeRender.PINK, ArcadeRender.PURPLE,
            ArcadeRender.ORANGE, ArcadeRender.YELLOW, ArcadeRender.RED, ArcadeRender.WHITE
    };

    private final Random random = new Random();
    private boolean[][] mine = new boolean[COLS][ROWS];
    private boolean[][] open = new boolean[COLS][ROWS];
    private boolean[][] flag = new boolean[COLS][ROWS];
    private int[][] count = new int[COLS][ROWS];
    private boolean placed;
    private int cursorX, cursorY;
    private int ticks;
    private int boomX = -1, boomY = -1;

    public MineFieldGame() {
        super("minefield", "MINE FIELD", ArcadeRender.CYAN, COLS * CELL, ROWS * CELL + TOP);
    }

    @Override
    protected void reset() {
        mine = new boolean[COLS][ROWS];
        open = new boolean[COLS][ROWS];
        flag = new boolean[COLS][ROWS];
        count = new int[COLS][ROWS];
        placed = false;
        ticks = 0;
        boomX = boomY = -1;
        cursorX = COLS / 2;
        cursorY = ROWS / 2;
    }

    private void placeMines(int safeX, int safeY) {
        int left = MINES;
        while (left > 0) {
            int x = random.nextInt(COLS), y = random.nextInt(ROWS);
            if (mine[x][y] || (Math.abs(x - safeX) <= 1 && Math.abs(y - safeY) <= 1)) continue;
            mine[x][y] = true;
            left--;
        }
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                int c = 0;
                for (int ox = -1; ox <= 1; ox++)
                    for (int oy = -1; oy <= 1; oy++)
                        if (inside(x + ox, y + oy) && mine[x + ox][y + oy]) c++;
                count[x][y] = c;
            }
        }
        placed = true;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < COLS && y < ROWS;
    }

    private void reveal(int x, int y) {
        if (!inside(x, y) || open[x][y] || flag[x][y]) return;
        if (!placed) placeMines(x, y);
        if (mine[x][y]) {
            boomX = x;
            boomY = y;
            ArcadeSound.explode();
            for (int ix = 0; ix < COLS; ix++)
                for (int iy = 0; iy < ROWS; iy++)
                    if (mine[ix][iy]) open[ix][iy] = true;
            gameOver(false);
            return;
        }
        int opened = 0;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{x, y});
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            if (!inside(p[0], p[1]) || open[p[0]][p[1]] || flag[p[0]][p[1]]) continue;
            open[p[0]][p[1]] = true;
            opened++;
            if (count[p[0]][p[1]] == 0) {
                for (int ox = -1; ox <= 1; ox++)
                    for (int oy = -1; oy <= 1; oy++)
                        queue.add(new int[]{p[0] + ox, p[1] + oy});
            }
        }
        score += opened;
        ArcadeSound.blip();
        checkWin();
    }

    /** Clicking an opened number with the right amount of flags around it opens its neighbours. */
    private void chord(int x, int y) {
        if (!open[x][y] || count[x][y] == 0) return;
        int flags = 0;
        for (int ox = -1; ox <= 1; ox++)
            for (int oy = -1; oy <= 1; oy++)
                if (inside(x + ox, y + oy) && flag[x + ox][y + oy]) flags++;
        if (flags != count[x][y]) return;
        for (int ox = -1; ox <= 1; ox++)
            for (int oy = -1; oy <= 1; oy++)
                if (state == State.PLAYING) reveal(x + ox, y + oy);
    }

    private void toggleFlag(int x, int y) {
        if (!inside(x, y) || open[x][y]) return;
        flag[x][y] = !flag[x][y];
        ArcadeSound.select();
    }

    private void checkWin() {
        if (state != State.PLAYING) return;
        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++)
                if (!mine[x][y] && !open[x][y]) return;
        score += Math.max(0, 500 - ticks / 60) + 200;
        gameOver(true);
    }

    @Override
    protected void onMouse(int mx, int my, int button) {
        if (my < TOP) return;
        int x = mx / CELL, y = (my - TOP) / CELL;
        if (!inside(x, y)) return;
        cursorX = x;
        cursorY = y;
        if (button == 0) {
            if (open[x][y]) chord(x, y);
            else reveal(x, y);
        } else if (button == 1) {
            toggleFlag(x, y);
        }
    }

    @Override
    protected void onKey(int key) {
        switch (key) {
            case GLFW_KEY_UP, GLFW_KEY_W -> cursorY = Math.max(0, cursorY - 1);
            case GLFW_KEY_DOWN, GLFW_KEY_S -> cursorY = Math.min(ROWS - 1, cursorY + 1);
            case GLFW_KEY_LEFT, GLFW_KEY_A -> cursorX = Math.max(0, cursorX - 1);
            case GLFW_KEY_RIGHT, GLFW_KEY_D -> cursorX = Math.min(COLS - 1, cursorX + 1);
            case GLFW_KEY_SPACE -> {
                if (open[cursorX][cursorY]) chord(cursorX, cursorY);
                else reveal(cursorX, cursorY);
            }
            case GLFW_KEY_F -> toggleFlag(cursorX, cursorY);
            default -> {
            }
        }
    }

    @Override
    protected void update() {
        if (placed) ticks++;
    }

    @Override
    public void render(GuiGraphics g) {
        g.fill(0, 0, getWidth(), getHeight(), 0xFF070414);
        int flags = 0;
        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++)
                if (flag[x][y]) flags++;
        ArcadeRender.text(g, "MINES " + (MINES - flags), 2, 3, ArcadeRender.RED);
        String time = "TIME " + ticks / 60;
        ArcadeRender.text(g, time, getWidth() - ArcadeRender.width(time) - 2, 3, ArcadeRender.CYAN);

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                int px = x * CELL, py = TOP + y * CELL;
                if (open[x][y]) {
                    g.fill(px, py, px + CELL, py + CELL, x == boomX && y == boomY ? 0xFFAA1030 : 0xFF140C2C);
                    g.fill(px, py, px + CELL, py + 1, 0xFF22163F);
                    g.fill(px, py, px + 1, py + CELL, 0xFF22163F);
                    if (mine[x][y]) {
                        ArcadeRender.disc(g, px + CELL / 2, py + CELL / 2, 3, ArcadeRender.RED);
                        g.fill(px + 4, py + 4, px + 6, py + 6, ArcadeRender.WHITE);
                    } else if (count[x][y] > 0) {
                        ArcadeRender.text(g, String.valueOf(count[x][y]), px + 3, py + 2, NUMBER_COLORS[count[x][y]]);
                    }
                } else {
                    g.fill(px, py, px + CELL, py + CELL, 0xFF3A2370);
                    g.fill(px, py, px + CELL - 1, py + 1, 0xFF6A4BC0);
                    g.fill(px, py, px + 1, py + CELL - 1, 0xFF6A4BC0);
                    g.fill(px + 1, py + CELL - 1, px + CELL, py + CELL, 0xFF1C0F3A);
                    g.fill(px + CELL - 1, py + 1, px + CELL, py + CELL, 0xFF1C0F3A);
                    if (flag[x][y]) {
                        g.fill(px + 4, py + 2, px + 5, py + 10, ArcadeRender.WHITE);
                        g.fill(px + 5, py + 2, px + 9, py + 6, ArcadeRender.PINK);
                    }
                }
            }
        }
        if (state == State.PLAYING && ArcadeRender.blink(300)) {
            ArcadeRender.outline(g, cursorX * CELL, TOP + cursorY * CELL, cursorX * CELL + CELL, TOP + cursorY * CELL + CELL, ArcadeRender.YELLOW, 1);
        }
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        int cell = 6;
        int cols = w / cell, rows = h / cell;
        long seed = ArcadeRender.millis() / 700;
        for (int cx = 0; cx < cols; cx++) {
            for (int cy = 0; cy < rows; cy++) {
                long hash = (cx * 31L + cy * 17L + seed) % 7;
                int px = x + cx * cell, py = y + cy * cell;
                g.fill(px, py, px + cell - 1, py + cell - 1, hash < 4 ? 0xFF140C2C : 0xFF3A2370);
                if (hash == 5) g.fill(px + 2, py + 1, px + 4, py + 3, ArcadeRender.PINK);
            }
        }
    }

    @Override
    public String[] controls() {
        return new String[]{"LMB/SPACE: DIG", "RMB/F: FLAG", "CLICK NUMBER: CHORD"};
    }

    @Override
    public boolean usesMouse() {
        return true;
    }
}
