package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Maze chase game: a little robot collects energy dots while glitch creatures hunt it.
 */
public class DotDashGame extends ArcadeGame {
    private static final String[] MAZE = {
            "#####################",
            "#.........#.........#",
            "#o##.####.#.####.##o#",
            "#...................#",
            "#.##.#.#######.#.##.#",
            "#....#....#....#....#",
            "####.####.#.####.####",
            "   #.#.........#.#   ",
            "####.#.###-###.#.####",
            "     ..##   ##..     ",
            "####.#.#######.#.####",
            "   #.#.........#.#   ",
            "####.#.#######.#.####",
            "#.........#.........#",
            "#o##.####.#.####.##o#",
            "#..#.............#..#",
            "##.#.#.#######.#.#.##",
            "#....#....#....#....#",
            "#.#######.#.#######.#",
            "#...................#",
            "#####################",
    };
    private static final int COLS = 21;
    private static final int ROWS = 21;
    private static final int T = 8;
    private static final int TOP = 10;
    private static final int[] DX = {0, -1, 0, 1};
    private static final int[] DY = {-1, 0, 1, 0};
    private static final int UP = 0, LEFT = 1, DOWN = 2, RIGHT = 3;
    private static final int GATE_X = 10 * T, HOME_Y = 9 * T, EXIT_Y = 7 * T;
    private static final int[] SCHEDULE = {7 * 60, 20 * 60, 7 * 60, 20 * 60, 5 * 60};

    private static final String[] BOT = {
            "...3....",
            "..111...",
            ".11111..",
            "1122221.",
            "1121121.",
            "1111111.",
            ".1...1..",
    };
    private static final String[][] GLITCH = {{
            "1.1111.1",
            ".111111.",
            "11231231",
            "11111111",
            ".111111.",
            "1.1..1.1",
    }, {
            "..1111..",
            "11111111",
            "11231231",
            "11111111",
            ".111111.",
            ".1.11.1.",
    }};
    private static final String[] EYES = {
            "........",
            "........",
            "..21.21.",
            "........",
    };
    private static final int[] GLITCH_COLORS = {ArcadeRender.RED, ArcadeRender.PINK, ArcadeRender.CYAN, ArcadeRender.ORANGE};

    private enum Mode {HOME, EXIT, ACTIVE, EATEN, ENTER}

    private static final class Glitch {
        int x, y, dir, index, release;
        Mode mode;
        boolean scared;
    }

    private final Random random = new Random();
    private final Glitch[] glitches = new Glitch[4];
    private boolean[][] dots = new boolean[COLS][ROWS];
    private boolean[][] power = new boolean[COLS][ROWS];
    private int dotsLeft;
    private int px, py, dir, want;
    private int lives, level;
    private int frightTimer, chain;
    private int scheduleIndex, scheduleTimer;
    private int frame;
    private int deathTimer, clearTimer, readyTimer;

    public DotDashGame() {
        super("dotdash", "DOT DASH", ArcadeRender.YELLOW, COLS * T, ROWS * T + TOP);
        for (int i = 0; i < 4; i++) glitches[i] = new Glitch();
        fillDots();
        resetPositions();
    }

    @Override
    protected void reset() {
        lives = 3;
        level = 1;
        fillDots();
        resetPositions();
    }

    private void fillDots() {
        dotsLeft = 0;
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                char c = MAZE[y].charAt(x);
                dots[x][y] = c == '.';
                power[x][y] = c == 'o';
                if (c == '.' || c == 'o') dotsLeft++;
            }
        }
    }

    private void resetPositions() {
        px = 10 * T;
        py = 15 * T;
        dir = LEFT;
        want = LEFT;
        frightTimer = 0;
        scheduleIndex = 0;
        scheduleTimer = 0;
        readyTimer = 90;
        for (int i = 0; i < 4; i++) {
            Glitch gl = glitches[i];
            gl.index = i;
            gl.scared = false;
            gl.dir = LEFT;
            if (i == 0) {
                gl.mode = Mode.ACTIVE;
                gl.x = GATE_X;
                gl.y = EXIT_Y;
            } else {
                gl.mode = Mode.HOME;
                gl.x = (8 + i) * T;
                gl.y = HOME_Y;
            }
            gl.release = i * Math.max(60, 240 - level * 30);
        }
    }

    private static char tile(int tx, int ty) {
        if (ty < 0 || ty >= ROWS) return '#';
        return MAZE[ty].charAt(Math.floorMod(tx, COLS));
    }

    private static boolean open(int tx, int ty) {
        char c = tile(tx, ty);
        return c != '#' && c != '-';
    }

    private static boolean aligned(int x, int y) {
        return Math.floorMod(x, T) == 0 && Math.floorMod(y, T) == 0;
    }

    private static int wrap(int x) {
        int w = COLS * T;
        if (x <= -T) return x + w;
        if (x >= w) return x - w;
        return x;
    }

    @Override
    protected void onKey(int key) {
        switch (key) {
            case GLFW_KEY_UP, GLFW_KEY_W -> want = UP;
            case GLFW_KEY_LEFT, GLFW_KEY_A -> want = LEFT;
            case GLFW_KEY_DOWN, GLFW_KEY_S -> want = DOWN;
            case GLFW_KEY_RIGHT, GLFW_KEY_D -> want = RIGHT;
            default -> {
            }
        }
    }

    @Override
    protected void update() {
        frame++;
        if (readyTimer > 0) {
            readyTimer--;
            return;
        }
        if (clearTimer > 0) {
            if (--clearTimer == 0) {
                level++;
                fillDots();
                resetPositions();
            }
            return;
        }
        if (deathTimer > 0) {
            if (--deathTimer == 0) {
                if (lives <= 0) gameOver(false);
                else resetPositions();
            }
            return;
        }

        if (frightTimer > 0) {
            if (--frightTimer == 0) {
                for (Glitch gl : glitches) gl.scared = false;
            }
        } else if (scheduleIndex < SCHEDULE.length && ++scheduleTimer >= SCHEDULE[scheduleIndex]) {
            scheduleIndex++;
            scheduleTimer = 0;
            for (Glitch gl : glitches) if (gl.mode == Mode.ACTIVE) gl.dir = (gl.dir + 2) % 4;
        }

        stepPlayer();
        if (clearTimer > 0) return;

        for (Glitch gl : glitches) {
            int steps;
            if (gl.mode == Mode.EATEN || gl.mode == Mode.ENTER) steps = 2;
            else if (gl.scared) steps = frame % 2;
            else if (inTunnel(gl)) steps = frame % 2;
            else steps = frame % Math.max(4, 7 - level) == 0 ? 0 : 1;
            for (int s = 0; s < steps; s++) stepGlitch(gl);
        }
        checkCollisions();
    }

    private boolean inTunnel(Glitch gl) {
        return gl.y == 9 * T && (gl.x < 4 * T || gl.x > 16 * T);
    }

    private void stepPlayer() {
        // the player is slightly faster than the glitches
        int speed = frame % 8 == 0 ? 0 : 1;
        for (int s = 0; s < speed; s++) {
            if (aligned(px, py)) {
                int tx = Math.floorMod(px / T, COLS), ty = py / T;
                if (dots[tx][ty]) {
                    dots[tx][ty] = false;
                    dotsLeft--;
                    score += 10;
                    if (frame % 4 == 0) ArcadeSound.blip();
                } else if (power[tx][ty]) {
                    power[tx][ty] = false;
                    dotsLeft--;
                    score += 50;
                    frighten();
                }
                if (dotsLeft == 0) {
                    clearTimer = 120;
                    ArcadeSound.levelUp();
                    return;
                }
                if (open(tx + DX[want], ty + DY[want])) dir = want;
                if (!open(tx + DX[dir], ty + DY[dir])) return;
            } else if (want == (dir + 2) % 4) {
                dir = want;
            }
            px = wrap(px + DX[dir]);
            py += DY[dir];
        }
    }

    private void frighten() {
        frightTimer = Math.max(120, 420 - level * 40);
        chain = 0;
        ArcadeSound.coin();
        for (Glitch gl : glitches) {
            if (gl.mode == Mode.ACTIVE || gl.mode == Mode.HOME || gl.mode == Mode.EXIT) {
                gl.scared = true;
                if (gl.mode == Mode.ACTIVE) gl.dir = (gl.dir + 2) % 4;
            }
        }
    }

    private void stepGlitch(Glitch gl) {
        switch (gl.mode) {
            case HOME -> {
                if (--gl.release <= 0) gl.mode = Mode.EXIT;
            }
            case EXIT -> {
                if (gl.x != GATE_X) gl.x += Integer.signum(GATE_X - gl.x);
                else if (gl.y > EXIT_Y) gl.y--;
                else {
                    gl.mode = Mode.ACTIVE;
                    gl.dir = random.nextBoolean() ? LEFT : RIGHT;
                }
            }
            case ENTER -> {
                if (gl.y < HOME_Y) gl.y++;
                else {
                    gl.scared = false;
                    gl.mode = Mode.EXIT;
                }
            }
            case ACTIVE, EATEN -> {
                if (aligned(gl.x, gl.y)) {
                    int tx = Math.floorMod(gl.x / T, COLS), ty = gl.y / T;
                    if (gl.mode == Mode.EATEN && tx == 10 && ty == 7) {
                        gl.mode = Mode.ENTER;
                        return;
                    }
                    gl.dir = chooseDir(gl, tx, ty);
                }
                gl.x = wrap(gl.x + DX[gl.dir]);
                gl.y += DY[gl.dir];
            }
        }
    }

    private int chooseDir(Glitch gl, int tx, int ty) {
        int[] target = target(gl);
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        int[] options = new int[4];
        int count = 0;
        for (int d = 0; d < 4; d++) {
            if (d == (gl.dir + 2) % 4) continue;
            if (!open(tx + DX[d], ty + DY[d])) continue;
            options[count++] = d;
            double dx = tx + DX[d] - target[0], dy = ty + DY[d] - target[1];
            double dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        if (count == 0) return (gl.dir + 2) % 4;
        if (gl.scared && gl.mode == Mode.ACTIVE) return options[random.nextInt(count)];
        return best;
    }

    private int[] target(Glitch gl) {
        if (gl.mode == Mode.EATEN) return new int[]{10, 7};
        int ptx = Math.floorMod(px / T, COLS), pty = py / T;
        boolean scatter = scheduleIndex < SCHEDULE.length && scheduleIndex % 2 == 0;
        if (scatter) {
            return switch (gl.index) {
                case 0 -> new int[]{COLS - 2, -2};
                case 1 -> new int[]{1, -2};
                case 2 -> new int[]{COLS - 1, ROWS + 1};
                default -> new int[]{0, ROWS + 1};
            };
        }
        return switch (gl.index) {
            case 0 -> new int[]{ptx, pty};
            case 1 -> new int[]{ptx + DX[dir] * 4, pty + DY[dir] * 4};
            case 2 -> {
                Glitch leader = glitches[0];
                int ax = ptx + DX[dir] * 2, ay = pty + DY[dir] * 2;
                yield new int[]{2 * ax - leader.x / T, 2 * ay - leader.y / T};
            }
            default -> {
                int dx = ptx - gl.x / T, dy = pty - gl.y / T;
                yield dx * dx + dy * dy > 64 ? new int[]{ptx, pty} : new int[]{0, ROWS + 1};
            }
        };
    }

    private void checkCollisions() {
        for (Glitch gl : glitches) {
            if (gl.mode != Mode.ACTIVE) continue;
            if (Math.abs(gl.x - px) < 6 && Math.abs(gl.y - py) < 6) {
                if (gl.scared) {
                    gl.scared = false;
                    gl.mode = Mode.EATEN;
                    score += 200 << Math.min(chain, 3);
                    chain++;
                    ArcadeSound.score();
                } else {
                    lives--;
                    deathTimer = 90;
                    ArcadeSound.explode();
                    return;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g) {
        g.fill(0, 0, getWidth(), getHeight(), 0xFF02010A);
        int wallColor = clearTimer > 0 && (clearTimer / 10) % 2 == 0 ? ArcadeRender.WHITE : 0xFF3B5BFF;
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                char c = tile(x, y);
                int sx = x * T, sy = TOP + y * T;
                if (c == '#') {
                    g.fill(sx, sy, sx + T, sy + T, 0xFF0A0A33);
                    if (tile(x, y - 1) != '#') g.fill(sx, sy, sx + T, sy + 1, wallColor);
                    if (tile(x, y + 1) != '#') g.fill(sx, sy + T - 1, sx + T, sy + T, wallColor);
                    if (x == 0 || tile(x - 1, y) != '#') g.fill(sx, sy, sx + 1, sy + T, wallColor);
                    if (x == COLS - 1 || tile(x + 1, y) != '#') g.fill(sx + T - 1, sy, sx + T, sy + T, wallColor);
                } else if (c == '-') {
                    g.fill(sx, sy + 3, sx + T, sy + 5, ArcadeRender.PINK);
                }
                if (dots[x][y]) g.fill(sx + 3, sy + 3, sx + 5, sy + 5, 0xFFFFD9A8);
                if (power[x][y] && ArcadeRender.blink(220)) ArcadeRender.disc(g, sx + 4, sy + 4, 3, ArcadeRender.YELLOW);
            }
        }

        boolean hideActors = clearTimer > 0;
        if (!hideActors && state != State.ATTRACT) {
            boolean dying = deathTimer > 0;
            if (!dying || ArcadeRender.blink(90)) {
                int face = dir == LEFT ? -1 : dir == RIGHT ? 1 : 0;
                ArcadeRender.sprite(g, px + (face < 0 ? 0 : 1), py + TOP, 1, BOT,
                        dying ? ArcadeRender.RED : ArcadeRender.YELLOW, ArcadeRender.CYAN, ArcadeRender.PINK);
            }
            if (!dying) {
                for (Glitch gl : glitches) drawGlitch(g, gl);
            }
        }

        ArcadeRender.text(g, "LV " + level, 2, 1, ArcadeRender.DIM);
        for (int i = 0; i < lives - 1; i++) {
            ArcadeRender.sprite(g, getWidth() - 10 - i * 10, 1, 1, BOT, ArcadeRender.YELLOW, ArcadeRender.CYAN, ArcadeRender.PINK);
        }
        if (state == State.PLAYING && readyTimer > 0) {
            ArcadeRender.centered(g, "READY!", getWidth() / 2, TOP + 9 * T, ArcadeRender.YELLOW);
        }
    }

    private void drawGlitch(GuiGraphics g, Glitch gl) {
        int sx = gl.x, sy = gl.y + TOP;
        if (gl.mode == Mode.EATEN || gl.mode == Mode.ENTER) {
            ArcadeRender.sprite(g, sx, sy, 1, EYES, ArcadeRender.WHITE, 0xFF2040FF);
            return;
        }
        String[] spr = GLITCH[(frame / 8) % 2];
        if (gl.scared) {
            boolean flash = frightTimer < 120 && (frightTimer / 10) % 2 == 0;
            ArcadeRender.sprite(g, sx, sy, 1, spr, flash ? ArcadeRender.WHITE : 0xFF2B3BFF, 0xFFFFC0C0, 0xFFFFC0C0);
        } else {
            ArcadeRender.sprite(g, sx, sy, 1, spr, GLITCH_COLORS[gl.index], ArcadeRender.WHITE, 0xFF1020A0);
        }
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y + h / 2 - 7, x + w, y + h / 2 - 6, 0xFF3B5BFF);
        g.fill(x, y + h / 2 + 7, x + w, y + h / 2 + 8, 0xFF3B5BFF);
        int t = (int) ((ArcadeRender.millis() / 30) % (w + 40));
        int bx = x + w - t + 30;
        for (int i = 0; i < w / 6; i++) {
            int dx = x + 2 + i * 6;
            if (dx < bx) g.fill(dx, y + h / 2, dx + 2, y + h / 2 + 2, 0xFFFFD9A8);
        }
        if (bx < x + w - 8 && bx > x) ArcadeRender.sprite(g, bx, y + h / 2 - 3, 1, BOT, ArcadeRender.YELLOW, ArcadeRender.CYAN, ArcadeRender.PINK);
        int gx = bx + 14;
        if (gx < x + w - 8 && gx > x) ArcadeRender.sprite(g, gx, y + h / 2 - 3, 1, GLITCH[(int) ((ArcadeRender.millis() / 150) % 2)], ArcadeRender.RED, ArcadeRender.WHITE, 0xFF1020A0);
    }

    @Override
    public String[] controls() {
        return new String[]{"ARROWS/WASD: MOVE", "EAT ALL ENERGY DOTS", "POWER CELL = HUNT THEM"};
    }

    @Override
    public String getStatus() {
        return "LIVES " + lives + "  LEVEL " + level;
    }
}
