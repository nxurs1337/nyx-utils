package dev.nyx.utils.features.gui.arcade.games;

import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import net.minecraft.client.gui.GuiGraphics;

import static org.lwjgl.glfw.GLFW.*;

public class BrickBlasterGame extends ArcadeGame {
    private static final int W = 176;
    private static final int H = 200;
    private static final int COLS = 11;
    private static final int ROWS = 7;
    private static final int BRICK_W = 16;
    private static final int BRICK_H = 7;
    private static final int TOP = 22;
    private static final int PADDLE_Y = H - 14;
    private static final int BALL = 3;
    private static final int[] ROW_COLORS = {
            ArcadeRender.RED, ArcadeRender.ORANGE, ArcadeRender.YELLOW, ArcadeRender.GREEN,
            ArcadeRender.CYAN, ArcadeRender.PURPLE, ArcadeRender.PINK
    };

    private int[][] bricks = new int[COLS][ROWS];
    private float paddleX;
    private int paddleW;
    private float ballX, ballY, vx, vy;
    private boolean stuck;
    private int lives;
    private int round;
    private int shake;

    public BrickBlasterGame() {
        super("brickblaster", "BRICK BLASTER", ArcadeRender.ORANGE, W, H);
        buildRound(1);
        paddleW = 30;
        paddleX = (W - paddleW) / 2f;
        stickBall();
    }

    @Override
    protected void reset() {
        lives = 3;
        round = 1;
        paddleW = 30;
        buildRound(round);
        paddleX = (W - paddleW) / 2f;
        stickBall();
    }

    private void buildRound(int round) {
        bricks = new int[COLS][ROWS];
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                // every round gets a different pattern, tougher bricks on top
                boolean hole = switch (round % 3) {
                    case 2 -> (x + y) % 4 == 0;
                    case 0 -> Math.abs(x - COLS / 2) + y < 2;
                    default -> false;
                };
                if (hole) continue;
                bricks[x][y] = y < Math.min(3, round) ? 2 : 1;
            }
        }
    }

    private void stickBall() {
        stuck = true;
        ballX = paddleX + paddleW / 2f - BALL / 2f;
        ballY = PADDLE_Y - BALL - 1;
        float speed = 1.9f + round * 0.15f;
        vx = speed * 0.55f;
        vy = -speed;
    }

    @Override
    protected void onKey(int key) {
        if (key == GLFW_KEY_SPACE || key == GLFW_KEY_UP || key == GLFW_KEY_W) launch();
    }

    @Override
    protected void onMouse(int x, int y, int button) {
        if (button == 0) launch();
    }

    private void launch() {
        if (!stuck) return;
        stuck = false;
        ArcadeSound.shoot();
    }

    /** The machine screen feeds the mouse position in canvas coordinates. */
    public void mouseMoved(int x) {
        if (state != State.PLAYING) return;
        paddleX = Math.max(0, Math.min(W - paddleW, x - paddleW / 2f));
    }

    @Override
    protected void update() {
        if (shake > 0) shake--;
        float move = 3.2f;
        if (isHeld(GLFW_KEY_LEFT, GLFW_KEY_A)) paddleX -= move;
        if (isHeld(GLFW_KEY_RIGHT, GLFW_KEY_D)) paddleX += move;
        paddleX = Math.max(0, Math.min(W - paddleW, paddleX));

        if (stuck) {
            ballX = paddleX + paddleW / 2f - BALL / 2f;
            ballY = PADDLE_Y - BALL - 1;
            return;
        }

        // sub-step so the ball never tunnels through bricks
        int steps = 3;
        for (int i = 0; i < steps; i++) {
            if (!stepBall(vx / steps, vy / steps)) return;
        }
    }

    private boolean stepBall(float dx, float dy) {
        ballX += dx;
        if (ballX < 0) {
            ballX = 0;
            vx = Math.abs(vx);
        } else if (ballX + BALL > W) {
            ballX = W - BALL;
            vx = -Math.abs(vx);
        } else if (hitBrick()) {
            ballX -= dx;
            vx = -vx;
        }

        ballY += dy;
        if (ballY < 0) {
            ballY = 0;
            vy = Math.abs(vy);
        } else if (hitBrick()) {
            ballY -= dy;
            vy = -vy;
        }

        // paddle: the bounce angle depends on where the ball lands
        if (vy > 0 && ballY + BALL >= PADDLE_Y && ballY + BALL <= PADDLE_Y + 4
                && ballX + BALL >= paddleX && ballX <= paddleX + paddleW) {
            float speed = (float) Math.sqrt(vx * vx + vy * vy);
            float rel = ((ballX + BALL / 2f) - (paddleX + paddleW / 2f)) / (paddleW / 2f);
            rel = Math.max(-0.9f, Math.min(0.9f, rel));
            vx = speed * rel;
            vy = -(float) Math.sqrt(Math.max(0.5f, speed * speed - vx * vx));
            ballY = PADDLE_Y - BALL;
            ArcadeSound.blip();
        }

        if (ballY > H) {
            lives--;
            shake = 10;
            ArcadeSound.hit();
            if (lives <= 0) {
                gameOver(false);
            } else {
                stickBall();
            }
            return false;
        }
        return true;
    }

    private boolean hitBrick() {
        int x1 = (int) ballX, y1 = (int) ballY - TOP;
        int x2 = x1 + BALL - 1, y2 = y1 + BALL - 1;
        boolean hit = false;
        for (int bx = Math.max(0, x1 / BRICK_W); bx <= Math.min(COLS - 1, x2 / BRICK_W); bx++) {
            for (int by = Math.max(0, Math.floorDiv(y1, BRICK_H)); by <= Math.min(ROWS - 1, Math.floorDiv(y2, BRICK_H)); by++) {
                if (y2 < 0 || bricks[bx][by] == 0) continue;
                bricks[bx][by]--;
                hit = true;
                if (bricks[bx][by] == 0) {
                    score += (ROWS - by) * 10;
                    ArcadeSound.pickup();
                } else {
                    ArcadeSound.select();
                }
            }
        }
        if (hit && cleared()) {
            score += 500;
            round++;
            ArcadeSound.levelUp();
            buildRound(round);
            stickBall();
        }
        return hit;
    }

    private boolean cleared() {
        for (int[] column : bricks)
            for (int b : column)
                if (b > 0) return false;
        return true;
    }

    @Override
    public void render(GuiGraphics g) {
        int sx = shake > 0 ? (shake % 2 == 0 ? 2 : -2) : 0;
        g.pose().pushMatrix();
        g.pose().translate(sx, 0);
        g.fillGradient(0, 0, W, H, 0xFF0A0420, 0xFF1A0634);
        for (int y = 0; y < H; y += 10) g.fill(0, y, W, y + 1, 0x0CFFFFFF);

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (bricks[x][y] == 0) continue;
                int px = x * BRICK_W, py = TOP + y * BRICK_H;
                int color = bricks[x][y] == 2 ? ArcadeRender.mix(ROW_COLORS[y], ArcadeRender.WHITE, 0.3f) : ROW_COLORS[y];
                g.fill(px + 1, py + 1, px + BRICK_W - 1, py + BRICK_H - 1, color);
                g.fill(px + 1, py + 1, px + BRICK_W - 1, py + 2, ArcadeRender.mix(color, ArcadeRender.WHITE, 0.5f));
                if (bricks[x][y] == 2) ArcadeRender.outline(g, px + 1, py + 1, px + BRICK_W - 1, py + BRICK_H - 1, ArcadeRender.WHITE, 1);
            }
        }

        int px = (int) paddleX;
        g.fill(px - 1, PADDLE_Y - 1, px + paddleW + 1, PADDLE_Y + 5, 0x4022F3FF);
        g.fill(px, PADDLE_Y, px + paddleW, PADDLE_Y + 4, ArcadeRender.CYAN);
        g.fill(px, PADDLE_Y, px + 3, PADDLE_Y + 4, ArcadeRender.PINK);
        g.fill(px + paddleW - 3, PADDLE_Y, px + paddleW, PADDLE_Y + 4, ArcadeRender.PINK);

        int bx = (int) ballX, by = (int) ballY;
        g.fill(bx - 1, by - 1, bx + BALL + 1, by + BALL + 1, 0x50FFFFFF);
        g.fill(bx, by, bx + BALL, by + BALL, ArcadeRender.WHITE);

        ArcadeRender.text(g, "ROUND " + round, 3, 6, ArcadeRender.DIM);
        for (int i = 0; i < lives; i++) g.fill(W - 10 - i * 12, 8, W - 2 - i * 12, 11, ArcadeRender.CYAN);
        if (stuck && state == State.PLAYING && ArcadeRender.blink(400)) {
            ArcadeRender.centered(g, "SPACE TO LAUNCH", W / 2, H / 2 + 20, ArcadeRender.YELLOW);
        }
        g.pose().popMatrix();
    }

    @Override
    public void renderPreview(GuiGraphics g, int x, int y, int w, int h) {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < w / 8; c++)
                if ((c + r) % 5 != 0)
                    g.fill(x + 1 + c * 8, y + 3 + r * 4, x + 8 + c * 8, y + 6 + r * 4, ROW_COLORS[r]);
        long t = ArcadeRender.millis() / 30;
        int bx = (int) Math.abs(t % (2L * (w - 3)) - (w - 3));
        int by = (int) Math.abs((t * 2 / 3) % (2L * (h / 2)) - h / 2);
        g.fill(x + bx, y + h / 3 + by / 2, x + bx + 2, y + h / 3 + by / 2 + 2, ArcadeRender.WHITE);
        g.fill(x + bx - 6, y + h - 4, x + bx + 8, y + h - 2, ArcadeRender.CYAN);
    }

    @Override
    public String[] controls() {
        return new String[]{"MOUSE/ARROWS: PADDLE", "SPACE/CLICK: LAUNCH", "CLEAR EVERY BRICK"};
    }

    @Override
    public String getStatus() {
        return "LIVES " + lives + "  ROUND " + round;
    }

    @Override
    public boolean usesMouse() {
        return true;
    }
}
