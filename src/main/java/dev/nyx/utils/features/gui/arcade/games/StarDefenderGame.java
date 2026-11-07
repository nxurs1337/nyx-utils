package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class StarDefenderGame extends ArcadeGame {
    private static final int W = 192;
    private static final int H = 208;
    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int SPACING_X = 16;
    private static final int SPACING_Y = 14;
    private static final int PLAYER_Y = H - 18;

    private static final String[][] ALIENS = {
            { // jelly
                    "..1111..",
                    ".111111.",
                    "11211211",
                    "11111111",
                    ".1.11.1.",
                    "1.1..1.1",
            }, {
                    "..1111..",
                    ".111111.",
                    "11211211",
                    "11111111",
                    "..1..1..",
                    ".1....1.",
            }, { // horned bug
                    "1......1",
                    ".1.11.1.",
                    ".111111.",
                    "11211211",
                    "11111111",
                    "1.1..1.1",
            }, {
                    "1......1",
                    ".1.11.1.",
                    ".111111.",
                    "11211211",
                    "11111111",
                    ".1.11.1.",
            }, { // orb drone
                    "...11...",
                    ".111111.",
                    "11122111",
                    "11122111",
                    ".111111.",
                    "1.1..1.1",
            }, {
                    "...11...",
                    ".111111.",
                    "11122111",
                    "11122111",
                    ".111111.",
                    ".1.11.1.",
            }
    };
    private static final String[] SHIP = {
            "....11....",
            "...1111...",
            "...1221...",
            ".11111111.",
            "1111111111",
            "11.1..1.11",
    };
    private static final String[] COMET = {
            "...1111...",
            ".11222211.",
            "1121121121",
            ".11111111.",
    };
    private static final int[] ALIEN_COLORS = {ArcadeRender.PINK, ArcadeRender.GREEN, ArcadeRender.CYAN};
    private static final int[] ALIEN_POINTS = {30, 20, 10};

    private final Random random = new Random();
    private final List<float[]> playerShots = new ArrayList<>();
    private final List<float[]> enemyShots = new ArrayList<>();
    private final List<int[]> explosions = new ArrayList<>(); // x, y, ticks
    private boolean[][] alive = new boolean[COLS][ROWS];
    private boolean[][] shields;
    private int formationX, formationY, formationDir;
    private int stepTimer, animFrame;
    private float playerX;
    private int lives, wave;
    private int fireCooldown, respawn;
    private float cometX = -1;
    private int cometDir;
    private final float[] stars = new float[60];

    public StarDefenderGame() {
        super("stardefender", "STAR DEFENDER", ArcadeRender.PINK, W, H);
        for (int i = 0; i < stars.length; i++) stars[i] = random.nextFloat() * H;
        buildWave();
    }

    @Override
    protected void reset() {
        lives = 3;
        wave = 0;
        playerX = W / 2f - 5;
        buildWave();
        buildShields();
    }

    private void buildWave() {
        alive = new boolean[COLS][ROWS];
        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++)
                alive[x][y] = true;
        formationX = 16;
        formationY = 26 + Math.min(wave, 5) * 6;
        formationDir = 1;
        stepTimer = 0;
        playerShots.clear();
        enemyShots.clear();
        cometX = -1;
    }

    private void buildShields() {
        // four bunkers made of 2x2 blocks
        shields = new boolean[W / 2][8];
        for (int s = 0; s < 4; s++) {
            int cx = (W / 8 + s * W / 4) / 2;
            for (int bx = -6; bx < 6; bx++) {
                for (int by = 0; by < 8; by++) {
                    boolean arch = by > 4 && Math.abs(bx + 0.5) < 3;
                    boolean corner = by == 0 && Math.abs(bx + 0.5) > 4;
                    if (!arch && !corner) shields[cx + bx][by] = true;
                }
            }
        }
    }

    private static int typeOf(int row) {
        return row == 0 ? 0 : row < 3 ? 1 : 2;
    }

    private int aliveCount() {
        int count = 0;
        for (boolean[] column : alive)
            for (boolean a : column)
                if (a) count++;
        return count;
    }

    @Override
    protected void onKey(int key) {
        if (key == GLFW_KEY_SPACE || key == GLFW_KEY_UP || key == GLFW_KEY_W) fire();
    }

    private void fire() {
        if (respawn > 0 || fireCooldown > 0 || playerShots.size() >= 2) return;
        playerShots.add(new float[]{playerX + 4.5f, PLAYER_Y - 2});
        fireCooldown = 14;
        ArcadeSound.shoot();
    }

    @Override
    protected void update() {
        for (int i = 0; i < stars.length; i++) stars[i] = (stars[i] + 0.2f + (i % 3) * 0.15f) % H;
        explosions.removeIf(e -> --e[2] <= 0);
        if (fireCooldown > 0) fireCooldown--;
        if (respawn > 0) {
            respawn--;
            return;
        }
        if (isHeld(GLFW_KEY_SPACE)) fire();

        if (isHeld(GLFW_KEY_LEFT, GLFW_KEY_A)) playerX -= 1.8f;
        if (isHeld(GLFW_KEY_RIGHT, GLFW_KEY_D)) playerX += 1.8f;
        playerX = Math.max(2, Math.min(W - 12, playerX));

        // formation marches faster the fewer aliens are left
        int count = aliveCount();
        int interval = Math.max(2, count * 40 / (COLS * ROWS) - Math.min(wave, 6));
        if (++stepTimer >= interval) {
            stepTimer = 0;
            animFrame ^= 1;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            for (int x = 0; x < COLS; x++)
                for (int y = 0; y < ROWS; y++)
                    if (alive[x][y]) {
                        minX = Math.min(minX, formationX + x * SPACING_X);
                        maxX = Math.max(maxX, formationX + x * SPACING_X + 8);
                    }
            if ((formationDir > 0 && maxX + 3 > W - 2) || (formationDir < 0 && minX - 3 < 2)) {
                formationDir = -formationDir;
                formationY += 6;
            } else {
                formationX += 3 * formationDir;
            }
        }

        // aliens shoot from the lowest alive alien of a random column
        if (random.nextInt(Math.max(12, 60 - wave * 6)) == 0) {
            int column = random.nextInt(COLS);
            for (int y = ROWS - 1; y >= 0; y--) {
                if (alive[column][y]) {
                    enemyShots.add(new float[]{formationX + column * SPACING_X + 4, formationY + y * SPACING_Y + 6});
                    break;
                }
            }
        }

        // bonus comet
        if (cometX < 0 && random.nextInt(900) == 0) {
            cometDir = random.nextBoolean() ? 1 : -1;
            cometX = cometDir > 0 ? 0.01f : W - 10;
        }
        if (cometX >= 0) {
            cometX += cometDir * 0.9f;
            if (cometX > W || cometX < 0) cometX = -1;
        }

        updatePlayerShots();
        updateEnemyShots();

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (alive[x][y] && formationY + y * SPACING_Y + 6 >= PLAYER_Y) {
                    lives = 0;
                    ArcadeSound.explode();
                    gameOver(false);
                    return;
                }
            }
        }

        if (aliveCount() == 0) {
            wave++;
            score += 250;
            ArcadeSound.levelUp();
            buildWave();
        }
    }

    private void updatePlayerShots() {
        shots:
        for (int i = playerShots.size() - 1; i >= 0; i--) {
            float[] shot = playerShots.get(i);
            shot[1] -= 4f;
            if (shot[1] < 0) {
                playerShots.remove(i);
                continue;
            }
            if (hitShield((int) shot[0], (int) shot[1])) {
                playerShots.remove(i);
                continue;
            }
            if (cometX >= 0 && shot[0] >= cometX && shot[0] <= cometX + 10 && shot[1] >= 12 && shot[1] <= 16) {
                score += 100 + random.nextInt(4) * 50;
                explosions.add(new int[]{(int) cometX + 5, 14, 20});
                cometX = -1;
                playerShots.remove(i);
                ArcadeSound.levelUp();
                continue;
            }
            for (int x = 0; x < COLS; x++) {
                for (int y = 0; y < ROWS; y++) {
                    if (!alive[x][y]) continue;
                    int ax = formationX + x * SPACING_X, ay = formationY + y * SPACING_Y;
                    if (shot[0] >= ax && shot[0] <= ax + 8 && shot[1] >= ay && shot[1] <= ay + 6) {
                        alive[x][y] = false;
                        score += ALIEN_POINTS[typeOf(y)];
                        explosions.add(new int[]{ax + 4, ay + 3, 12});
                        playerShots.remove(i);
                        ArcadeSound.hit();
                        continue shots;
                    }
                }
            }
        }
    }

    private void updateEnemyShots() {
        for (int i = enemyShots.size() - 1; i >= 0; i--) {
            float[] shot = enemyShots.get(i);
            shot[1] += 1.6f + Math.min(wave, 5) * 0.15f;
            if (shot[1] > H) {
                enemyShots.remove(i);
                continue;
            }
            if (hitShield((int) shot[0], (int) shot[1] + 3)) {
                enemyShots.remove(i);
                continue;
            }
            if (shot[0] >= playerX && shot[0] <= playerX + 10 && shot[1] + 3 >= PLAYER_Y && shot[1] <= PLAYER_Y + 6) {
                enemyShots.remove(i);
                lives--;
                explosions.add(new int[]{(int) playerX + 5, PLAYER_Y + 3, 30});
                ArcadeSound.explode();
                if (lives <= 0) {
                    gameOver(false);
                    return;
                }
                respawn = 60;
                enemyShots.clear();
                return;
            }
        }
    }

    private int shieldTop() {
        return PLAYER_Y - 30;
    }

    private boolean hitShield(int x, int y) {
        int sx = x / 2, sy = (y - shieldTop()) / 2;
        if (y < shieldTop() || sx < 0 || sx >= shields.length || sy < 0 || sy >= shields[0].length) return false;
        if (!shields[sx][sy]) return false;
        for (int ox = -1; ox <= 1; ox++)
            for (int oy = -1; oy <= 1; oy++)
                if (sx + ox >= 0 && sx + ox < shields.length && sy + oy >= 0 && sy + oy < shields[0].length
                        && random.nextInt(3) > 0)
                    shields[sx + ox][sy + oy] = false;
        shields[sx][sy] = false;
        return true;
    }

    @Override
    public void render(GuiGraphics g) {
        g.fill(0, 0, W, H, 0xFF03010A);
        for (int i = 0; i < stars.length; i++) {
            int sx = (int) ((i * 7919L) % W);
            g.fill(sx, (int) stars[i], sx + 1, (int) stars[i] + 1, i % 3 == 0 ? 0xFFFFFFFF : 0x80AFA8FF);
        }

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (!alive[x][y]) continue;
                int type = typeOf(y);
                ArcadeRender.sprite(g, formationX + x * SPACING_X, formationY + y * SPACING_Y, 1,
                        ALIENS[type * 2 + animFrame], ALIEN_COLORS[type], ArcadeRender.WHITE);
            }
        }

        if (cometX >= 0) {
            ArcadeRender.sprite(g, (int) cometX, 12, 1, COMET, ArcadeRender.RED, ArcadeRender.YELLOW);
        }

        if (shields != null) {
            int top = shieldTop();
            for (int x = 0; x < shields.length; x++)
                for (int y = 0; y < shields[0].length; y++)
                    if (shields[x][y]) g.fill(x * 2, top + y * 2, x * 2 + 2, top + y * 2 + 2, 0xFF2BD66B);
        }

        for (float[] shot : playerShots) g.fill((int) shot[0], (int) shot[1], (int) shot[0] + 1, (int) shot[1] + 4, ArcadeRender.YELLOW);
        for (float[] shot : enemyShots) {
            int zig = ((int) shot[1] / 2) % 2;
            g.fill((int) shot[0] + zig, (int) shot[1], (int) shot[0] + zig + 1, (int) shot[1] + 2, ArcadeRender.RED);
            g.fill((int) shot[0] + 1 - zig, (int) shot[1] + 2, (int) shot[0] + 2 - zig, (int) shot[1] + 4, ArcadeRender.RED);
        }

        for (int[] e : explosions) {
            int r = (e[2] % 6) / 2 + 2;
            g.fill(e[0] - r, e[1], e[0] + r, e[1] + 1, ArcadeRender.ORANGE);
            g.fill(e[0], e[1] - r, e[0] + 1, e[1] + r, ArcadeRender.ORANGE);
            g.fill(e[0] - r / 2, e[1] - r / 2, e[0] + r / 2 + 1, e[1] + r / 2 + 1, ArcadeRender.YELLOW);
        }

        if (state != State.ATTRACT && (respawn == 0 || ArcadeRender.blink(100))) {
            ArcadeRender.sprite(g, (int) playerX, PLAYER_Y, 1, SHIP, ArcadeRender.CYAN, ArcadeRender.WHITE);
        }

        g.fill(0, H - 9, W, H - 8, 0xFF2BD66B);
        for (int i = 0; i < lives - 1; i++) ArcadeRender.sprite(g, 4 + i * 14, H - 7, 1, SHIP, ArcadeRender.CYAN, ArcadeRender.WHITE);
        String waveText = "WAVE " + (wave + 1);
        ArcadeRender.text(g, waveText, W - ArcadeRender.width(waveText) - 3, 2, ArcadeRender.DIM);
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF03010A);
        int frame = (int) ((ArcadeRender.millis() / 400) % 2);
        int shift = (int) ((ArcadeRender.millis() / 400) % 6) - 3;
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 2; r++)
                ArcadeRender.sprite(g, x + 5 + c * 11 + shift, y + 4 + r * 9, 1, ALIENS[r * 2 + frame], ALIEN_COLORS[r], ArcadeRender.WHITE);
        int shot = (int) ((ArcadeRender.millis() / 30) % (h - 16));
        g.fill(x + w / 2, y + h - 12 - shot, x + w / 2 + 1, y + h - 9 - shot, ArcadeRender.YELLOW);
        ArcadeRender.sprite(g, x + w / 2 - 5, y + h - 8, 1, SHIP, ArcadeRender.CYAN, ArcadeRender.WHITE);
    }

    @Override
    public String[] controls() {
        return new String[]{"LEFT/RIGHT: MOVE", "SPACE: FIRE", "SHOOT THE COMET!"};
    }

    @Override
    public String getStatus() {
        return "LIVES " + lives + "  WAVE " + (wave + 1);
    }
}
