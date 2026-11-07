package dev.nyx.utils.features.gui.arcade;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.gui.arcade.games.BrickBlasterGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Standing in front of a single arcade cabinet.
 */
public class ArcadeMachineScreen extends Screen {
    private static final double STEP = 1.0 / 60.0;

    private final Screen parent;
    private final ArcadeGame game;
    private long lastTime = -1;
    private double accumulator;
    private long gameOverSince = -1;

    // layout, recomputed every frame
    private int screenX, screenY, screenW, screenH;
    private float scale;

    public ArcadeMachineScreen(Screen parent, ArcadeGame game) {
        super(Component.literal(game.getTitle()));
        this.parent = parent;
        this.game = game;
    }

    @Override
    public void onClose() {
        if (game.getState() == ArcadeGame.State.PLAYING) game.togglePause();
        game.held.clear();
        NyxUtils.configManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    private void layout() {
        int marquee = 34, deck = 46;
        int availH = height - marquee - deck - 48;
        int availW = width - 220;
        if (availW < game.getWidth() * 0.6f) availW = width - 40;
        scale = Math.min(availW / (float) game.getWidth(), availH / (float) game.getHeight());
        scale = Math.max(0.5f, scale);
        screenW = Math.round(game.getWidth() * scale);
        screenH = Math.round(game.getHeight() * scale);
        screenX = (width - screenW) / 2;
        screenY = 6 + marquee + 12;
    }

    private void tickGame() {
        long now = System.nanoTime();
        if (lastTime < 0) lastTime = now;
        accumulator += Math.min(0.25, (now - lastTime) / 1e9);
        lastTime = now;
        while (accumulator >= STEP) {
            game.step();
            accumulator -= STEP;
        }
        if (game.getState() == ArcadeGame.State.GAME_OVER) {
            if (gameOverSince < 0) gameOverSince = millis();
        } else {
            gameOverSince = -1;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        tickGame();
        layout();
        int accent = game.getAccent();

        synthwave(g, width, height, accent);
        g.fill(0, 0, width, height, 0x99000000);

        int cabX1 = screenX - 26, cabX2 = screenX + screenW + 26;
        int cabY1 = 6, cabY2 = height;

        // cabinet body with side art
        g.fillGradient(cabX1, cabY1, cabX2, cabY2, 0xFF1A0F2E, 0xFF0A0614);
        g.fill(cabX1, cabY1, cabX1 + 8, cabY2, mix(accent, 0xFF000000, 0.55f));
        g.fill(cabX2 - 8, cabY1, cabX2, cabY2, mix(accent, 0xFF000000, 0.55f));
        for (int y = cabY1 + 10; y < cabY2; y += 14) {
            g.fill(cabX1 + 2, y, cabX1 + 6, y + 6, alpha(accent, 140));
            g.fill(cabX2 - 6, y + 7, cabX2 - 2, y + 13, alpha(accent, 140));
        }
        neonBox(g, cabX1, cabY1, cabX2, cabY2 + 4, accent);

        // marquee
        int mx1 = cabX1 + 10, mx2 = cabX2 - 10, my1 = cabY1 + 4, my2 = cabY1 + 36;
        g.fillGradient(mx1, my1, mx2, my2, mix(accent, 0xFF000000, 0.35f), mix(accent, 0xFF000000, 0.75f));
        bulbs(g, mx1 + 2, my1 + 2, mx2 - 2, my2 - 2, 7);
        bigText(g, game.getTitle(), width / 2, my1 + 9, 2f, cycle(0));

        // bezel and screen
        g.fill(screenX - 10, screenY - 8, screenX + screenW + 10, screenY + screenH + 8, 0xFF050308);
        outline(g, screenX - 10, screenY - 8, screenX + screenW + 10, screenY + screenH + 8, 0xFF2A2440, 1);
        g.fill(screenX - 2, screenY - 2, screenX + screenW + 2, screenY + screenH + 2, alpha(accent, 50));

        g.enableScissor(screenX, screenY, screenX + screenW, screenY + screenH);
        g.pose().pushMatrix();
        g.pose().translate(screenX, screenY);
        g.pose().scale(scale, scale);
        game.render(g);
        g.pose().popMatrix();
        renderOverlay(g);
        crt(g, screenX, screenY, screenX + screenW, screenY + screenH);
        // glass reflection
        g.fillGradient(screenX, screenY, screenX + screenW, screenY + screenH / 3, 0x14FFFFFF, 0x00FFFFFF);
        g.disableScissor();

        renderDeck(g, cabX1, cabX2);
        renderSidePanels(g, cabX1, cabX2);
    }

    private void renderOverlay(GuiGraphics g) {
        int cx = screenX + screenW / 2;
        int cy = screenY + screenH / 2;
        switch (game.getState()) {
            case ATTRACT -> {
                g.fill(screenX, screenY, screenX + screenW, screenY + screenH, 0xB0000000);
                bigText(g, game.getTitle(), cx, cy - 46, 2f, game.getAccent());
                glowCentered(g, "HI-SCORE " + ArcadeScores.getInstance().get(game.getId()), cx, cy - 18, CYAN);
                if (blink(500)) bigText(g, "INSERT COIN", cx, cy + 2, 1.5f, YELLOW);
                centered(g, "PRESS ENTER TO START", cx, cy + 24, WHITE);
                String[] controls = game.controls();
                for (int i = 0; i < controls.length; i++) {
                    centered(g, controls[i], cx, cy + 42 + i * 10, DIM);
                }
            }
            case PAUSED -> {
                g.fill(screenX, screenY, screenX + screenW, screenY + screenH, 0x90000000);
                bigText(g, "PAUSED", cx, cy - 12, 2f, CYAN);
                centered(g, "P: RESUME   ESC: LEAVE", cx, cy + 12, WHITE);
            }
            case GAME_OVER -> {
                g.fill(screenX, screenY, screenX + screenW, screenY + screenH, 0xA0000000);
                bigText(g, game.isWon() ? "YOU WIN!" : "GAME OVER", cx, cy - 34, 2.2f, game.isWon() ? GREEN : RED);
                glowCentered(g, "SCORE " + game.getScore(), cx, cy - 6, WHITE);
                if (game.isNewHiScore() && blink(200)) glowCentered(g, "NEW HI-SCORE!", cx, cy + 8, cycle(0));
                if (blink(500)) centered(g, "ENTER: PLAY AGAIN", cx, cy + 26, YELLOW);
            }
            default -> {
            }
        }
    }

    private void renderDeck(GuiGraphics g, int cabX1, int cabX2) {
        int top = screenY + screenH + 12;
        int bottom = Math.min(height - 2, top + 40);
        if (bottom - top < 20) return;
        // slanted control panel
        g.fillGradient(cabX1 + 10, top, cabX2 - 10, bottom, 0xFF2A1A48, 0xFF140B24);
        g.fill(cabX1 + 10, top, cabX2 - 10, top + 1, alpha(game.getAccent(), 200));

        int midY = (top + bottom) / 2;
        int stickX = screenX + screenW / 2 - 60, stickY = midY;
        // joystick follows the arrow keys
        int jx = 0, jy = 0;
        if (game.isHeld(GLFW_KEY_LEFT, GLFW_KEY_A)) jx -= 4;
        if (game.isHeld(GLFW_KEY_RIGHT, GLFW_KEY_D)) jx += 4;
        if (game.isHeld(GLFW_KEY_UP, GLFW_KEY_W)) jy -= 4;
        if (game.isHeld(GLFW_KEY_DOWN, GLFW_KEY_S)) jy += 4;
        disc(g, stickX, stickY + 3, 8, 0xFF0A0612);
        g.fill(stickX - 1 + jx / 2, stickY - 6 + jy / 2, stickX + 2 + jx / 2, stickY + 3, 0xFF9A9AAA);
        disc(g, stickX + jx, stickY - 8 + jy, 5, RED);
        disc(g, stickX + jx - 1, stickY - 10 + jy, 2, 0xFFFF9AA4);

        // action buttons light up when pressed
        int bx = screenX + screenW / 2 + 20;
        button(g, bx, midY, PINK, game.isHeld(GLFW_KEY_SPACE), "A");
        button(g, bx + 22, midY - 4, CYAN, game.isHeld(GLFW_KEY_ENTER), "B");
        button(g, bx + 44, midY, YELLOW, game.isHeld(GLFW_KEY_P), "P");

        // coin door
        int coinX = screenX + screenW / 2 - 12;
        if (bottom + 26 < height) {
            g.fill(coinX - 10, bottom + 4, coinX + 34, bottom + 24, 0xFF100A1A);
            outline(g, coinX - 10, bottom + 4, coinX + 34, bottom + 24, 0xFF3A3050, 1);
            g.fill(coinX + 10, bottom + 8, coinX + 14, bottom + 18, blink(400) ? RED : 0xFF551018);
        }
    }

    private void button(GuiGraphics g, int x, int y, int color, boolean pressed, String label) {
        disc(g, x, y + 2, 7, 0xFF050308);
        if (pressed) disc(g, x, y + 1, 9, alpha(color, 60));
        disc(g, x, y + (pressed ? 1 : 0), 6, pressed ? mix(color, WHITE, 0.4f) : mix(color, 0xFF000000, 0.15f));
        centered(g, label, x + 1, y - 3 + (pressed ? 1 : 0), 0xFF000000);
    }

    private void renderSidePanels(GuiGraphics g, int cabX1, int cabX2) {
        int leftW = cabX1 - 16;
        if (leftW < 70) {
            // narrow window: compact score strip above the screen
            String line = "SCORE " + game.getScore() + "   HI " + game.getHiScore();
            centered(g, line, width / 2, screenY - 8 + 1, WHITE);
            return;
        }
        int w = Math.min(130, leftW);
        int lx1 = cabX1 - 8 - w, lx2 = cabX1 - 8;
        int top = screenY;

        panel(g, lx1, top, lx2, top + 96, RED);
        glowText(g, "1UP", lx1 + 6, top + 6, RED);
        led(g, String.format("%07d", game.getScore()), lx1 + 6, top + 18, lx2 - 6, RED);
        glowText(g, "HI-SCORE", lx1 + 6, top + 40, YELLOW);
        led(g, String.format("%07d", game.getHiScore()), lx1 + 6, top + 52, lx2 - 6, YELLOW);
        String status = game.getStatus();
        if (!status.isEmpty()) text(g, status, lx1 + 6, top + 78, CYAN);

        int rx1 = cabX2 + 8, rx2 = cabX2 + 8 + w;
        String[] controls = game.controls();
        int panelH = 30 + (controls.length + 3) * 11;
        panel(g, rx1, top, rx2, top + panelH, CYAN);
        glowText(g, "HOW TO PLAY", rx1 + 6, top + 6, CYAN);
        int y = top + 22;
        for (String line : controls) {
            text(g, line, rx1 + 6, y, WHITE);
            y += 11;
        }
        y += 4;
        text(g, "ENTER: START", rx1 + 6, y, DIM);
        text(g, "P: PAUSE", rx1 + 6, y + 11, DIM);
        text(g, "ESC: BACK TO ARCADE", rx1 + 6, y + 22, DIM);
    }

    /** Seven-segment style score readout. */
    private void led(GuiGraphics g, String digits, int x1, int y, int x2, int color) {
        g.fill(x1, y - 2, x2, y + 12, 0xFF050208);
        int textW = width(digits);
        float s = Math.min(1.6f, (x2 - x1 - 6) / (float) textW);
        g.pose().pushMatrix();
        g.pose().translate(x1 + 3, y);
        g.pose().scale(s, s);
        text(g, "8888888", 0, 0, alpha(color, 30));
        text(g, digits, 0, 0, color);
        g.pose().popMatrix();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (key == GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        ArcadeGame.State state = game.getState();
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
            game.held.add(GLFW_KEY_ENTER);
            boolean cooldownOver = gameOverSince < 0 || millis() - gameOverSince > 700;
            if (state == ArcadeGame.State.ATTRACT || (state == ArcadeGame.State.GAME_OVER && cooldownOver)) {
                game.start();
                accumulator = 0;
                return true;
            }
        }
        if (key == GLFW_KEY_P) {
            game.togglePause();
            ArcadeSound.select();
        }
        game.keyPressed(key);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        int key = input.key();
        game.keyReleased(key == GLFW_KEY_KP_ENTER ? GLFW_KEY_ENTER : key);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        boolean inside = mx >= screenX && mx < screenX + screenW && my >= screenY && my < screenY + screenH;
        if (!inside) return true;
        if (game.getState() == ArcadeGame.State.ATTRACT) {
            game.start();
            accumulator = 0;
            return true;
        }
        game.mouseClicked((int) ((mx - screenX) / scale), (int) ((my - screenY) / scale), click.button());
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (game instanceof BrickBlasterGame brick && scale > 0) {
            brick.mouseMoved((int) ((mouseX - screenX) / scale));
        }
    }
}
