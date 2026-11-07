package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class BlockDropGame extends ArcadeGame {
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int CELL = 8;
    private static final int SIDE = 52;

    // the seven four-cell shapes, as (x, y) offsets inside a 4x4 box
    private static final int[][][] SHAPES = {
            {{0, 1}, {1, 1}, {2, 1}, {3, 1}},
            {{0, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{2, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
            {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
            {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}}
    };
    private static final int[] COLORS = {
            ArcadeRender.CYAN, ArcadeRender.PURPLE, ArcadeRender.ORANGE, ArcadeRender.YELLOW,
            ArcadeRender.GREEN, ArcadeRender.PINK, ArcadeRender.RED
    };
    private static final int[] LINE_SCORES = {0, 100, 300, 500, 800};

    private final List<Integer> bag = new ArrayList<>();
    private int[][] board = new int[COLS][ROWS];
    private int[][] cells = new int[4][2];
    private int piece, next;
    private int pieceX, pieceY;
    private int gravityTimer, lockTimer;
    private int dasTimer, dasDir;
    private int lines, level;
    private int flash;

    public BlockDropGame() {
        super("blockdrop", "BLOCK DROP", ArcadeRender.PURPLE, COLS * CELL + SIDE, ROWS * CELL);
    }

    @Override
    protected void reset() {
        board = new int[COLS][ROWS];
        bag.clear();
        lines = 0;
        level = 1;
        flash = 0;
        next = draw();
        spawn();
    }

    private int draw() {
        if (bag.isEmpty()) {
            for (int i = 0; i < SHAPES.length; i++) bag.add(i);
            Collections.shuffle(bag);
        }
        return bag.removeFirst();
    }

    private void spawn() {
        piece = next;
        next = draw();
        cells = new int[4][2];
        for (int i = 0; i < 4; i++) {
            cells[i][0] = SHAPES[piece][i][0];
            cells[i][1] = SHAPES[piece][i][1];
        }
        pieceX = 3;
        pieceY = -1;
        gravityTimer = 0;
        lockTimer = 0;
        if (!fits(cells, pieceX, pieceY)) gameOver(false);
    }

    private boolean fits(int[][] shape, int ox, int oy) {
        for (int[] c : shape) {
            int x = c[0] + ox, y = c[1] + oy;
            if (x < 0 || x >= COLS || y >= ROWS) return false;
            if (y >= 0 && board[x][y] != 0) return false;
        }
        return true;
    }

    private boolean move(int dx, int dy) {
        if (!fits(cells, pieceX + dx, pieceY + dy)) return false;
        pieceX += dx;
        pieceY += dy;
        if (dx != 0) lockTimer = 0;
        return true;
    }

    private void rotate(int dir) {
        if (piece == 3) return; // the square doesn't rotate
        int size = piece == 0 ? 4 : 3;
        int[][] rotated = new int[4][2];
        for (int i = 0; i < 4; i++) {
            int x = cells[i][0], y = cells[i][1];
            rotated[i][0] = dir > 0 ? size - 1 - y : y;
            rotated[i][1] = dir > 0 ? x : size - 1 - x;
        }
        for (int kick : new int[]{0, -1, 1, -2, 2}) {
            if (fits(rotated, pieceX + kick, pieceY)) {
                cells = rotated;
                pieceX += kick;
                lockTimer = 0;
                ArcadeSound.select();
                return;
            }
        }
    }

    private void lock() {
        for (int[] c : cells) {
            int x = c[0] + pieceX, y = c[1] + pieceY;
            if (y < 0) {
                gameOver(false);
                return;
            }
            board[x][y] = piece + 1;
        }
        int cleared = 0;
        for (int y = ROWS - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < COLS; x++) {
                if (board[x][y] == 0) {
                    full = false;
                    break;
                }
            }
            if (!full) continue;
            cleared++;
            for (int yy = y; yy > 0; yy--)
                for (int x = 0; x < COLS; x++)
                    board[x][yy] = board[x][yy - 1];
            for (int x = 0; x < COLS; x++) board[x][0] = 0;
            y++;
        }
        if (cleared > 0) {
            score += LINE_SCORES[cleared] * level;
            lines += cleared;
            flash = 8;
            int newLevel = 1 + lines / 10;
            if (newLevel > level) {
                level = newLevel;
                ArcadeSound.levelUp();
            } else {
                ArcadeSound.score();
            }
        } else {
            ArcadeSound.hit();
        }
        spawn();
    }

    @Override
    protected void onKey(int key) {
        switch (key) {
            case GLFW_KEY_LEFT, GLFW_KEY_A -> {
                move(-1, 0);
                dasDir = -1;
                dasTimer = 0;
            }
            case GLFW_KEY_RIGHT, GLFW_KEY_D -> {
                move(1, 0);
                dasDir = 1;
                dasTimer = 0;
            }
            case GLFW_KEY_UP, GLFW_KEY_W, GLFW_KEY_X -> rotate(1);
            case GLFW_KEY_Z -> rotate(-1);
            case GLFW_KEY_SPACE -> {
                int dropped = 0;
                while (move(0, 1)) dropped++;
                score += dropped * 2;
                lock();
            }
            default -> {
            }
        }
    }

    @Override
    protected void update() {
        if (flash > 0) flash--;

        // auto repeat for held left/right
        boolean left = isHeld(GLFW_KEY_LEFT, GLFW_KEY_A), right = isHeld(GLFW_KEY_RIGHT, GLFW_KEY_D);
        if ((dasDir == -1 && left) || (dasDir == 1 && right)) {
            if (++dasTimer > 10 && dasTimer % 3 == 0) move(dasDir, 0);
        } else {
            dasDir = 0;
        }

        boolean soft = isHeld(GLFW_KEY_DOWN, GLFW_KEY_S);
        int speed = soft ? 2 : Math.max(3, 48 - (level - 1) * 5);
        if (++gravityTimer >= speed) {
            gravityTimer = 0;
            if (move(0, 1)) {
                if (soft) score += 1;
                lockTimer = 0;
            }
        }
        if (!fits(cells, pieceX, pieceY + 1)) {
            if (++lockTimer > 25) lock();
        }
    }

    private void cell(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + CELL, y + CELL, ArcadeRender.mix(color, 0xFF000000, 0.45f));
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, color);
        g.fill(x + 1, y + 1, x + CELL - 1, y + 2, ArcadeRender.mix(color, ArcadeRender.WHITE, 0.5f));
    }

    @Override
    public void render(GuiGraphics g) {
        int boardW = COLS * CELL;
        g.fill(0, 0, getWidth(), getHeight(), 0xFF07031A);
        g.fill(0, 0, boardW, getHeight(), flash > 0 && flash % 2 == 0 ? 0xFF2A1450 : 0xFF0C0624);
        for (int x = 1; x < COLS; x++) g.fill(x * CELL, 0, x * CELL + 1, getHeight(), 0x10FFFFFF);
        for (int y = 1; y < ROWS; y++) g.fill(0, y * CELL, boardW, y * CELL + 1, 0x10FFFFFF);

        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++)
                if (board[x][y] != 0) cell(g, x * CELL, y * CELL, COLORS[board[x][y] - 1]);

        if (state != State.ATTRACT) {
            int ghostY = pieceY;
            while (fits(cells, pieceX, ghostY + 1)) ghostY++;
            for (int[] c : cells) {
                int gx = (c[0] + pieceX) * CELL, gy = (c[1] + ghostY) * CELL;
                if (gy >= 0) ArcadeRender.outline(g, gx, gy, gx + CELL, gy + CELL, ArcadeRender.alpha(COLORS[piece], 110), 1);
            }
            for (int[] c : cells) {
                int y = c[1] + pieceY;
                if (y >= 0) cell(g, (c[0] + pieceX) * CELL, y * CELL, COLORS[piece]);
            }
        }
        g.fill(boardW, 0, boardW + 1, getHeight(), ArcadeRender.PURPLE);

        int sx = boardW + 6;
        ArcadeRender.text(g, "NEXT", sx, 4, ArcadeRender.DIM);
        ArcadeRender.outline(g, sx, 14, sx + 40, 38, 0x60FFFFFF, 1);
        for (int[] c : SHAPES[next]) {
            int px = sx + 4 + c[0] * 8, py = 18 + c[1] * 8;
            cell(g, px, py, COLORS[next]);
        }
        ArcadeRender.text(g, "LEVEL", sx, 50, ArcadeRender.DIM);
        ArcadeRender.text(g, String.valueOf(level), sx, 60, ArcadeRender.YELLOW);
        ArcadeRender.text(g, "LINES", sx, 76, ArcadeRender.DIM);
        ArcadeRender.text(g, String.valueOf(lines), sx, 86, ArcadeRender.CYAN);
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        int c = 5;
        int fall = (int) ((ArcadeRender.millis() / 160) % Math.max(1, h / c - 3));
        int[][] stack = {{0, 1, 1, 0, 1, 1, 1, 0}, {1, 1, 1, 0, 1, 1, 1, 1}};
        for (int r = 0; r < stack.length; r++)
            for (int i = 0; i < stack[r].length && i * c < w; i++)
                if (stack[r][i] == 1)
                    g.fill(x + 2 + i * c, y + h - (2 - r) * c - 1, x + 2 + i * c + c - 1, y + h - (2 - r) * c + c - 2, COLORS[(i + r) % COLORS.length]);
        for (int[] cc : SHAPES[5]) {
            g.fill(x + 12 + cc[0] * c, y + 2 + (fall + cc[1]) * c, x + 12 + cc[0] * c + c - 1, y + 2 + (fall + cc[1]) * c + c - 1, ArcadeRender.PINK);
        }
    }

    @Override
    public String[] controls() {
        return new String[]{"LEFT/RIGHT: MOVE", "UP/X  Z: ROTATE", "DOWN: SOFT  SPACE: DROP"};
    }

    @Override
    public String getStatus() {
        return "LEVEL " + level + "  LINES " + lines;
    }
}
