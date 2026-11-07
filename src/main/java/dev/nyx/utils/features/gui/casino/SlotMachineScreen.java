package dev.nyx.utils.features.gui.casino;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import dev.nyx.utils.features.gui.casino.slots.Feature;
import dev.nyx.utils.features.gui.casino.slots.Outcome;
import dev.nyx.utils.features.gui.casino.slots.SlotGame;
import dev.nyx.utils.features.gui.casino.slots.SlotSymbol;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;
import static dev.nyx.utils.features.gui.casino.CasinoRender.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * One slot machine, played with Nyx Coins from the {@link Wallet}.
 */
public class SlotMachineScreen extends Screen {
    private static final long[] BETS = {10, 20, 50, 100, 200, 500, 1000, 2500, 5000};
    private static final int BAR_H = 52;

    private enum Phase {IDLE, SPINNING, SHOWING, BANNER}

    private final Screen parent;
    private final SlotGame game;
    private final Random random = new Random();
    private final List<float[]> coins = new ArrayList<>();

    private int betIndex = 3;
    private Phase phase = Phase.IDLE;
    private long phaseStart;
    private Outcome current;
    private long spinBet;
    private boolean settled = true;
    private long[] stopAt;
    private boolean[] stopped;
    private int stepIndex;
    private long stepStart;
    private boolean dropping;
    private long runningWin;
    private long lastWin;

    private Feature feature;
    private long featureBet;
    private long featureWin;

    private boolean autoplay;
    private boolean turbo;
    private boolean showInfo;
    private boolean confirmBuy;
    private String bannerTitle, bannerSub;
    private long bannerAmount;
    private int bannerColor;
    private long bannerStart, bannerLength;
    private String toast;
    private long toastUntil;
    private long lastFrame;

    // layout, recomputed every frame
    private int cell, gridX, gridY;

    public SlotMachineScreen(Screen parent, SlotGame game) {
        super(Component.literal(game.title));
        this.parent = parent;
        this.game = game;
        long balance = Wallet.get().balance();
        while (betIndex > 0 && BETS[betIndex] * 20 > balance) betIndex--;
    }

    @Override
    public void onClose() {
        settleEverything();
        NyxUtils.configManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    private Wallet wallet() {
        return Wallet.get();
    }

    private long bet() {
        return feature != null ? featureBet : BETS[betIndex];
    }

    private long coinsOf(double timesBet, long bet) {
        return Math.round(timesBet * bet);
    }

    // ------------------------------------------------------------ game flow

    private void spin() {
        spin(false);
    }

    private long buyPrice() {
        return BETS[betIndex] * game.buyCost;
    }

    private boolean canBuy() {
        return game.canBuyBonus() && phase == Phase.IDLE && feature == null && !autoplay && wallet().balance() >= buyPrice();
    }

    private void spin(boolean buy) {
        if (phase != Phase.IDLE || showInfo || confirmBuy) return;
        if (buy && (!canBuy() || !wallet().withdraw(buyPrice(), "bonus-buy:" + game.id))) {
            showToast("NOT ENOUGH CHIPS FOR THE BONUS BUY");
            ArcadeSound.hit();
            return;
        }
        if (buy) {
            spinBet = BETS[betIndex];
            lastWin = 0;
        } else if (feature == null) {
            long bet = BETS[betIndex];
            if (!wallet().withdraw(bet, "slot:" + game.id)) {
                autoplay = false;
                showToast("NOT ENOUGH CHIPS - GRAB FREE CHIPS IN THE LOBBY");
                ArcadeSound.hit();
                return;
            }
            spinBet = bet;
            lastWin = 0;
        } else {
            feature.spinsLeft--;
            feature.spinsPlayed++;
            spinBet = featureBet;
        }
        current = buy ? game.buySpin(random) : game.spin(random, feature);
        settled = false;
        runningWin = 0;
        stepIndex = 0;
        dropping = false;
        phase = Phase.SPINNING;
        long now = millis();
        phaseStart = now;
        stopAt = new long[game.cols];
        stopped = new boolean[game.cols];
        long base = turbo ? 250 : 650, stagger = turbo ? 70 : 160;
        for (int c = 0; c < game.cols; c++) stopAt[c] = now + base + c * stagger;
        ArcadeSound.select();
    }

    /** Pays the current spin out. Safe to call more than once. */
    private void settle(boolean animate) {
        if (settled || current == null) return;
        settled = true;
        long win = coinsOf(current.totalWin, spinBet);
        if (feature != null) {
            long cap = (long) (SlotGame.MAX_WIN * featureBet);
            win = Math.min(win, Math.max(0, cap - featureWin));
            featureWin += win;
            feature.totalWin += current.totalWin;
            feature.spinsLeft += current.freeSpinsAwarded;
            if (animate && current.levelUp) {
                banner("LEVEL UP!", "+10 FREE SPINS  -  CATCHES NOW x" + game.collectMultiplier(feature.level), 0, CYAN, 2200);
            } else if (animate && current.freeSpinsAwarded > 0) {
                banner("RETRIGGER!", "+" + current.freeSpinsAwarded + " FREE SPINS", 0, GOLD, 2000);
            }
        } else if (current.freeSpinsAwarded > 0) {
            feature = new Feature(current.freeSpinsAwarded);
            featureBet = spinBet;
            featureWin = win;
            autoplay = false;
            if (animate) banner("FREE SPINS!", current.freeSpinsAwarded + " SPINS ON THE HOUSE", 0, GOLD, 2600);
        }
        wallet().deposit(win, "slot:" + game.id);
        lastWin = feature != null ? featureWin : win;
        if (!animate) return;
        if (win > 0) {
            if (bannerTitle == null && win >= spinBet * 15) bigWin(win);
            else ArcadeSound.score();
            spawnCoins(Math.min(120, (int) (win / Math.max(1, spinBet)) * 6 + 10));
        }
    }

    private void bigWin(long win) {
        double x = win / (double) spinBet;
        String title = x >= 100 ? "EPIC WIN!" : x >= 40 ? "MEGA WIN!" : "BIG WIN!";
        banner(title, null, win, x >= 100 ? cycle(0) : GOLD, x >= 100 ? 4500 : 3200);
        ArcadeSound.levelUp();
    }

    private void banner(String title, String sub, long amount, int color, long length) {
        bannerTitle = title;
        bannerSub = sub;
        bannerAmount = amount;
        bannerColor = color;
        bannerStart = millis();
        bannerLength = turbo ? length / 2 : length;
        phase = Phase.BANNER;
    }

    private void endFeature() {
        long total = featureWin;
        feature = null;
        lastWin = total;
        banner("FEATURE COMPLETE", "TOTAL WIN", total, GOLD, 3200);
        ArcadeSound.levelUp();
        spawnCoins(100);
    }

    /** Leaving mid-spin or mid-feature still pays everything out. */
    private void settleEverything() {
        settle(false);
        int guard = 0;
        while (feature != null && feature.spinsLeft > 0 && guard++ < 500) {
            feature.spinsLeft--;
            feature.spinsPlayed++;
            spinBet = featureBet;
            current = game.spin(random, feature);
            settled = false;
            settle(false);
        }
        feature = null;
    }

    private void update() {
        long now = millis();
        switch (phase) {
            case SPINNING -> {
                for (int c = 0; c < game.cols; c++) {
                    if (!stopped[c] && now >= stopAt[c]) {
                        stopped[c] = true;
                        ArcadeSound.blip();
                    }
                }
                if (now >= stopAt[game.cols - 1] + 120) {
                    phase = Phase.SHOWING;
                    stepStart = now;
                }
            }
            case SHOWING -> {
                Outcome.Step step = current.steps.get(stepIndex);
                long dropTime = turbo ? 180 : 320;
                long showTime = step.win > 0 || current.scatterWin > 0 || current.collected > 0 ? (turbo ? 450 : 900) : 150;
                if (dropping) {
                    if (now - stepStart >= dropTime) {
                        dropping = false;
                        stepStart = now;
                    }
                    return;
                }
                if (now - stepStart < showTime) return;
                if (step.win > 0 && stepIndex < current.steps.size() - 1) {
                    runningWin += coinsOf(step.win, spinBet);
                    stepIndex++;
                    dropping = true;
                    stepStart = now;
                    ArcadeSound.pickup();
                    return;
                }
                runningWin = coinsOf(current.totalWin, spinBet);
                settle(true);
                if (phase == Phase.SHOWING) {
                    phase = Phase.IDLE;
                    phaseStart = now;
                }
            }
            case BANNER -> {
                if (now - bannerStart >= bannerLength) {
                    bannerTitle = null;
                    phase = Phase.IDLE;
                    phaseStart = now;
                }
            }
            case IDLE -> {
                if (feature != null) {
                    if (now - phaseStart < (turbo ? 200 : 500)) return;
                    if (feature.spinsLeft > 0) spin();
                    else endFeature();
                } else if (autoplay && now - phaseStart > (turbo ? 150 : 400)) {
                    spin();
                }
            }
        }
    }

    // --------------------------------------------------------------- layout

    private void layout() {
        int availW = width - 30, availH = height - 40 - BAR_H - 10;
        cell = Math.max(18, Math.min(72, Math.min(availW / game.cols, availH / game.rows)));
        gridX = (width - cell * game.cols) / 2;
        gridY = 40 + Math.max(0, (availH - cell * game.rows) / 2);
    }

    private int barY() {
        return height - BAR_H;
    }

    private int[] spinButton() {
        return new int[]{width / 2, barY() + BAR_H / 2 + 2, 19};
    }

    private int[] betMinus() {
        return new int[]{width / 2 - 104, barY() + 26, 16, 16};
    }

    private int[] betPlus() {
        return new int[]{width / 2 - 40, barY() + 26, 16, 16};
    }

    private int[] autoButton() {
        return new int[]{width / 2 + 26, barY() + 10, 46, 15};
    }

    private int[] turboButton() {
        return new int[]{width / 2 + 26, barY() + 28, 46, 15};
    }

    private int[] infoButton() {
        return new int[]{width / 2 + 76, barY() + 28, 22, 15};
    }

    // --------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        update();
        layout();
        long now = millis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        backdrop(g, game.theme, 0, 0, width, height);
        g.fill(0, 0, width, height, 0x50000000);

        // title
        float titleScale = Math.min(2f, (width - 40) / (float) width(game.title));
        int titleW = Math.max((int) (width(game.title) * titleScale), width(game.tagline)) + 24;
        g.fill(width / 2 - titleW / 2, 3, width / 2 + titleW / 2, 34, 0xA0000000);
        outline(g, width / 2 - titleW / 2, 3, width / 2 + titleW / 2, 34, game.theme.frame(), 1);
        bigText(g, game.title, width / 2, 6, titleScale, mix(game.theme.frame(), WHITE, 0.55f));
        centered(g, game.tagline, width / 2, 25, alpha(WHITE, 220));

        renderReels(g, now);
        renderBar(g, mouseX, mouseY);
        if (game.canBuyBonus()) renderBuyPanel(g, mouseX, mouseY);
        updateCoins(g, dt);
        if (bannerTitle != null) renderBanner(g, now);
        if (showInfo) renderInfo(g);
        if (confirmBuy) renderBuyConfirm(g, mouseX, mouseY);
        if (toast != null && now < toastUntil) {
            int tw = width(toast) + 16;
            goldPanel(g, width / 2 - tw / 2, gridY - 4, width / 2 + tw / 2, gridY + 14);
            centered(g, toast, width / 2, gridY + 1, GOLD);
        }
    }

    private void renderReels(GuiGraphics g, long now) {
        int x1 = gridX, y1 = gridY, x2 = gridX + cell * game.cols, y2 = gridY + cell * game.rows;
        int frame = game.theme.frame();
        // cabinet frame with marquee lights
        g.fill(x1 - 9, y1 - 9, x2 + 9, y2 + 9, mix(frame, 0xFF000000, 0.55f));
        outline(g, x1 - 9, y1 - 9, x2 + 9, y2 + 9, frame, 2);
        bulbs(g, x1 - 6, y1 - 6, x2 + 5, y2 + 5, Math.max(6, cell / 3));
        g.fill(x1, y1, x2, y2, game.theme.reelBg());
        for (int c = 1; c < game.cols; c++) g.fill(x1 + c * cell, y1, x1 + c * cell + 1, y2, alpha(frame, 90));

        g.enableScissor(x1, y1, x2, y2);
        if (current == null) {
            // idle machine shows a calm attract grid
            for (int c = 0; c < game.cols; c++)
                for (int r = 0; r < game.rows; r++)
                    drawCell(g, game.symbols.get((c * 3 + r * 5) % game.symbols.size()), 0, c, r, 0, false, false);
        } else if (phase == Phase.SPINNING) {
            Outcome.Step first = current.steps.getFirst();
            for (int c = 0; c < game.cols; c++) {
                if (stopped[c]) {
                    long since = now - stopAt[c];
                    float bounce = since < 160 ? (float) Math.sin(since / 160.0 * Math.PI) * cell * 0.12f : 0;
                    for (int r = 0; r < game.rows; r++)
                        drawCell(g, game.symbols.get(first.grid[c][r]), first.values[c][r], c, r, bounce, false, false);
                } else {
                    // blurred strip rushing down
                    float offset = (now % 60) / 60f * cell;
                    for (int r = -1; r < game.rows; r++) {
                        int index = (int) Math.floorMod(now / 60 + r * 7L + c * 13L, game.symbols.size());
                        drawCell(g, game.symbols.get(index), 0, c, r, offset, false, false);
                    }
                    g.fillGradient(x1 + c * cell, y1, x1 + (c + 1) * cell, y1 + cell / 2, 0x80000000, 0x00000000);
                    g.fillGradient(x1 + c * cell, y2 - cell / 2, x1 + (c + 1) * cell, y2, 0x00000000, 0x80000000);
                }
            }
        } else {
            renderStep(g, now);
        }
        g.disableScissor();

        // left / right side info during the feature
        if (feature != null) {
            String spins = "FREE SPINS " + feature.spinsPlayed + " / " + (feature.spinsPlayed + feature.spinsLeft);
            goldPanel(g, width / 2 - 90, y1 - 26, width / 2 + 90, y1 - 12);
            centered(g, spins, width / 2, y1 - 22, GOLD);
            if (game.mechanic == SlotGame.Mechanic.COLLECT) {
                String level = "x" + game.collectMultiplier(feature.level) + "  CATCHES";
                text(g, level, x2 + 14, y1 + 2, CYAN);
                int filled = feature.level >= 4 ? 4 : feature.wilds % 4;
                for (int i = 0; i < 4; i++) {
                    disc(g, x2 + 18 + i * 9, y1 + 18, 3, i < filled ? GOLD : 0xFF3A3A3A);
                }
            }
            if (game.accumulate && feature.accumulated > 0) {
                text(g, "TOTAL x" + feature.accumulated, x2 + 14, y1 + 2, GOLD);
            }
        }
    }

    private void renderStep(GuiGraphics g, long now) {
        Outcome.Step step = current.steps.get(stepIndex);
        boolean showWin = !dropping && (step.win > 0 || (stepIndex == current.steps.size() - 1
                && (current.scatterWin > 0 || current.collected > 0)));
        float drop = dropping ? 1 - Math.min(1, (now - stepStart) / (float) (turbo ? 180 : 320)) : 0;
        Outcome.Step previous = stepIndex > 0 ? current.steps.get(stepIndex - 1) : null;
        for (int c = 0; c < game.cols; c++) {
            int[] fall = new int[game.rows];
            if (dropping && previous != null) {
                // how far every cell of this column fell during the tumble
                int removed = 0;
                for (int r = 0; r < game.rows; r++) if (previous.winning[c][r]) removed++;
                for (int r = 0; r < removed; r++) fall[r] = removed;
                int below = 0;
                for (int r = game.rows - 1; r >= 0; r--) {
                    if (previous.winning[c][r]) below++;
                    else fall[r + below] = below;
                }
            }
            for (int r = 0; r < game.rows; r++) {
                boolean winning = showWin && step.winning[c][r];
                drawCell(g, game.symbols.get(step.grid[c][r]), step.values[c][r], c, r, -fall[r] * cell * drop,
                        winning, showWin && !winning);
            }
        }
        if (showWin && current.multiplier > 0 && stepIndex == current.steps.size() - 1) {
            bigText(g, "x" + current.multiplier, gridX + cell * game.cols / 2, gridY + cell * game.rows / 2 - 10, 2.5f, GOLD);
        }
        if (showWin && current.collected > 0) {
            goldPanel(g, width / 2 - 80, gridY + cell * game.rows / 2 - 10, width / 2 + 80, gridY + cell * game.rows / 2 + 10);
            centered(g, "CAUGHT " + wallet().format(coinsOf(current.collected, spinBet)), width / 2, gridY + cell * game.rows / 2 - 3, GOLD);
        }
    }

    private void drawCell(GuiGraphics g, SlotSymbol symbol, double value, int c, int r, float offsetY, boolean winning, boolean dim) {
        float cx = gridX + c * cell + cell / 2f;
        float cy = gridY + r * cell + cell / 2f + offsetY;
        float size = cell * 0.62f;
        if (winning) {
            float pulse = (float) (Math.sin(millis() / 110.0) * 0.5 + 0.5);
            int x = gridX + c * cell, y = (int) (gridY + r * cell + offsetY);
            g.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, alpha(mix(game.theme.accent(), GOLD, 0.6f), (int) (60 + 80 * pulse)));
            outline(g, x + 1, y + 1, x + cell - 1, y + cell - 1, alpha(GOLD, (int) (140 + 110 * pulse)), 1);
            size *= 1 + 0.12f * pulse;
        }
        if (symbol.isScatter() || symbol.kind() == SlotSymbol.Kind.MULTIPLIER || symbol.isWild()) {
            disc(g, (int) cx, (int) cy, (int) (cell * 0.42f), alpha(symbol.isScatter() ? PINK : symbol.isWild() ? CYAN : GOLD, 60));
        }
        SlotIcons.draw(g, symbol, cx, cy - (value > 0 ? cell * 0.06f : 0), size);
        if (value > 0) {
            String label = symbol.kind() == SlotSymbol.Kind.MULTIPLIER ? "x" + (int) value
                    : compact(coinsOf(value * game.payScale, spinBet == 0 ? bet() : spinBet));
            float s = Math.min(1f, cell / 40f + 0.25f);
            g.pose().pushMatrix();
            g.pose().translate(cx, cy + cell * 0.22f);
            g.pose().scale(s, s);
            int w = font.width(label); // value tags stay in the vanilla font
            g.fill(-w / 2 - 2, -1, w / 2 + 2, 8, 0xC0000000);
            g.drawString(font, label, -w / 2, 0, symbol.kind() == SlotSymbol.Kind.MULTIPLIER ? GOLD : 0xFF7CFF8A, false);
            g.pose().popMatrix();
        }
        if (dim) {
            int x = gridX + c * cell, y = (int) (gridY + r * cell + offsetY);
            g.fill(x, y, x + cell, y + cell, 0x90000000);
        }
    }

    private static String compact(long amount) {
        if (amount >= 1_000_000) return String.format("%.1fM", amount / 1_000_000.0);
        if (amount >= 10_000) return (amount / 1000) + "K";
        return String.valueOf(amount);
    }

    private void renderBar(GuiGraphics g, int mouseX, int mouseY) {
        int y = barY();
        g.fillGradient(0, y, width, height, 0xF0200810, 0xF0080204);
        g.fill(0, y, width, y + 1, GOLD);
        boolean idle = phase == Phase.IDLE && feature == null;

        text(g, "BALANCE", 10, y + 8, DIM);
        glowText(g, wallet().format(wallet().balance()), 10, y + 19, GOLD);
        text(g, feature != null ? "FEATURE WIN" : "WIN", 10, y + 33, DIM);
        long shownWin = phase == Phase.SHOWING ? runningWin : lastWin;
        text(g, wallet().format(shownWin), 10 + width(feature != null ? "FEATURE WIN " : "WIN "), y + 33, shownWin > 0 ? GREEN : WHITE);

        // bet selector
        text(g, "BET", width / 2 - 104, y + 12, DIM);
        int[] minus = betMinus(), plus = betPlus();
        button(g, "-", minus[0], minus[1], minus[2], minus[3], 0xFF8A1030, inside(mouseX, mouseY, minus[0], minus[1], minus[2], minus[3]), idle && betIndex > 0);
        button(g, "+", plus[0], plus[1], plus[2], plus[3], 0xFF1A8A30, inside(mouseX, mouseY, plus[0], plus[1], plus[2], plus[3]), idle && betIndex < BETS.length - 1);
        centered(g, compact(bet()), (minus[0] + minus[2] + plus[0]) / 2, y + 30, WHITE);

        // the big spin button
        int[] spin = spinButton();
        boolean hover = Math.hypot(mouseX - spin[0], mouseY - spin[1]) <= spin[2];
        boolean canSpin = phase == Phase.IDLE && feature == null;
        disc(g, spin[0], spin[1] + 2, spin[2] + 2, 0xFF050102);
        disc(g, spin[0], spin[1], spin[2], canSpin ? (hover ? 0xFFFFE070 : GOLD) : 0xFF6A5A30);
        disc(g, spin[0], spin[1], spin[2] - 3, canSpin ? (hover ? 0xFFFFD040 : 0xFFE0A020) : 0xFF4A3A20);
        disc(g, spin[0] - 6, spin[1] - 7, 3, alpha(WHITE, 120));
        String label = autoplay ? "STOP" : feature != null ? "FREE" : "SPIN";
        centered(g, label, spin[0] + 1, spin[1] - 3, 0xFF3A1A00);

        int[] auto = autoButton(), turboRect = turboButton(), info = infoButton();
        button(g, autoplay ? "AUTO ON" : "AUTO", auto[0], auto[1], auto[2], auto[3], autoplay ? GREEN : 0xFF3A3A5A, inside(mouseX, mouseY, auto[0], auto[1], auto[2], auto[3]), feature == null);
        button(g, turbo ? "TURBO!" : "TURBO", turboRect[0], turboRect[1], turboRect[2], turboRect[3], turbo ? ORANGE : 0xFF3A3A5A, inside(mouseX, mouseY, turboRect[0], turboRect[1], turboRect[2], turboRect[3]), true);
        button(g, "i", info[0], info[1], info[2], info[3], 0xFF3A6AAA, inside(mouseX, mouseY, info[0], info[1], info[2], info[3]), true);

        String hint = game.canBuyBonus() ? "SPACE: SPIN   B: BONUS BUY   A: AUTO   T: TURBO   ESC: LOBBY"
                : "SPACE: SPIN   UP/DOWN: BET   A: AUTO   T: TURBO   ESC: LOBBY";
        float s = Math.min(1f, (width / 2f - 120) / width(hint));
        if (s > 0.4f) {
            g.pose().pushMatrix();
            g.pose().translate(width - 8 - width(hint) * s, y + 40);
            g.pose().scale(s, s);
            text(g, hint, 0, 0, DIM);
            g.pose().popMatrix();
        }
    }

    private int[] buyPanel() {
        int w = 84, h = 58;
        int x = Math.max(4, gridX - 18 - w);
        int y = gridY + cell * game.rows / 2 - h / 2;
        return new int[]{x, y, w, h};
    }

    private void renderBuyPanel(GuiGraphics g, int mouseX, int mouseY) {
        int[] r = buyPanel();
        boolean enabled = canBuy();
        boolean hover = enabled && inside(mouseX, mouseY, r[0], r[1], r[2], r[3]);
        int x2 = r[0] + r[2], y2 = r[1] + r[3];
        g.fill(r[0] + 2, r[1] + 3, x2 + 2, y2 + 3, 0x90000000);
        g.fillGradient(r[0], r[1], x2, y2, enabled ? 0xFF8A1A1A : 0xFF3A2A2A, enabled ? 0xFF3A0606 : 0xFF1A1414);
        outline(g, r[0], r[1], x2, y2, enabled ? (hover ? WHITE : GOLD) : 0xFF6A5A4A, 1);
        if (hover) outline(g, r[0] - 2, r[1] - 2, x2 + 2, y2 + 2, alpha(GOLD, 100), 1);
        int cx = r[0] + r[2] / 2;
        bigText(g, "BONUS", cx, r[1] + 5, 1.2f, enabled ? (hover ? cycle(0) : GOLD) : 0xFF8A7A6A);
        centered(g, "BUY", cx, r[1] + 18, enabled ? WHITE : 0xFF8A7A6A);
        centered(g, compact(buyPrice()), cx, r[1] + 32, enabled ? GOLD : 0xFF8A7A6A);
        centered(g, game.buyCost + "x BET", cx, r[1] + 44, enabled ? DIM : 0xFF6A5A4A);
    }

    private int[] confirmRect() {
        return new int[]{width / 2 - 130, height / 2 - 60, 260, 120};
    }

    private void renderBuyConfirm(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, 0xC0000000);
        int[] r = confirmRect();
        goldPanel(g, r[0], r[1], r[0] + r[2], r[1] + r[3]);
        int cx = width / 2;
        bigText(g, "BUY FREE SPINS?", cx, r[1] + 10, 1.5f, GOLD);
        centered(g, "PRICE  " + wallet().format(buyPrice()) + "  (" + game.buyCost + "x BET)", cx, r[1] + 36, WHITE);
        centered(g, game.scatterTrigger() + " scatters guaranteed, " + game.freeSpins + " free spins", cx, r[1] + 50, DIM);
        centered(g, "Bet stays at " + wallet().format(BETS[betIndex]) + " during the feature", cx, r[1] + 62, DIM);
        int by = r[1] + r[3] - 28;
        button(g, "BUY", cx - 108, by, 100, 18, GOLD, inside(mouseX, mouseY, cx - 108, by, 100, 18), true);
        button(g, "CANCEL", cx + 8, by, 100, 18, 0xFF6A6A7A, inside(mouseX, mouseY, cx + 8, by, 100, 18), true);
    }

    private void confirmPurchase() {
        confirmBuy = false;
        ArcadeSound.coin();
        spin(true);
    }

    private void renderBanner(GuiGraphics g, long now) {
        float t = Math.min(1, (now - bannerStart) / 300f);
        int cy = gridY + cell * game.rows / 2;
        g.fill(0, 0, width, height, alpha(0xFF000000, (int) (120 * t)));
        float scale = 1.5f + 1.5f * t + (float) Math.sin(now / 150.0) * 0.08f;
        bigText(g, bannerTitle, width / 2, cy - 30, scale, bannerColor == GOLD ? (blink(200) ? GOLD : WHITE) : bannerColor);
        if (bannerSub != null) glowCentered(g, bannerSub, width / 2, cy + 2, WHITE);
        if (bannerAmount > 0) {
            // counts up like a real machine
            float progress = Math.min(1, (now - bannerStart) / (bannerLength * 0.7f));
            long shown = (long) (bannerAmount * progress);
            bigText(g, wallet().format(shown), width / 2, cy + (bannerSub != null ? 16 : 6), 2f, GOLD);
        }
        centered(g, "CLICK TO CONTINUE", width / 2, cy + 46, alpha(WHITE, blink(500) ? 200 : 90));
    }

    private void renderInfo(GuiGraphics g) {
        int w = Math.min(380, width - 20), h = Math.min(260, height - 20);
        int x1 = (width - w) / 2, y1 = (height - h) / 2;
        g.fill(0, 0, width, height, 0xB0000000);
        goldPanel(g, x1, y1, x1 + w, y1 + h);
        glowCentered(g, game.title + " - PAYTABLE", width / 2, y1 + 8, GOLD);
        String rules = switch (game.mechanic) {
            case TUMBLE -> "8+ matching symbols anywhere pay. Winners tumble away. 4+ scatters: 10 free spins.";
            case LINES -> game.lines.length + " lines, left to right. Wild substitutes. 3+ scatters: " + game.freeSpins + " free spins.";
            case WAYS -> "243 ways: match on adjacent reels from the left. 3+ scatters: " + game.freeSpins + " free spins.";
            case COLLECT -> "10 lines. 3+ scatters: free spins where every wild collects all money symbols. Every 4 wilds: +10 spins and a bigger multiplier.";
        };
        List<String> ruleLines = wrapText(rules, w - 16);
        for (int i = 0; i < ruleLines.size(); i++) centered(g, ruleLines.get(i), width / 2, y1 + 22 + i * 10, WHITE);

        int top = y1 + 30 + ruleLines.size() * 10;
        int cols = Math.max(1, (w - 16) / 120);
        long bet = bet();
        for (int i = 0; i < game.symbols.size(); i++) {
            SlotSymbol symbol = game.symbols.get(i);
            int cx = x1 + 8 + (i % cols) * 120, cy = top + (i / cols) * 26;
            if (cy + 22 > y1 + h - 14) break;
            SlotIcons.draw(g, symbol, cx + 10, cy + 10, 16);
            String pays = describe(symbol, bet);
            g.pose().pushMatrix();
            g.pose().translate(cx + 22, cy + 3);
            g.pose().scale(0.6f, 0.6f);
            List<String> lines = wrapText(pays, (int) (95 / 0.6f));
            for (int l = 0; l < Math.min(3, lines.size()); l++) text(g, lines.get(l), 0, l * 10, 0xFFE8D8C8);
            g.pose().popMatrix();
        }
        centered(g, "PAYOUTS SHOWN FOR YOUR CURRENT BET  -  CLICK TO CLOSE", width / 2, y1 + h - 12, DIM);
    }

    private String describe(SlotSymbol symbol, long bet) {
        return switch (symbol.kind()) {
            case SCATTER -> "SCATTER - triggers free spins";
            case WILD_SCATTER -> "WILD + SCATTER - substitutes and triggers free spins";
            case WILD -> game.mechanic == SlotGame.Mechanic.COLLECT ? "WILD - free spins only, collects money" : "WILD - substitutes for symbols";
            case MULTIPLIER -> "MULTIPLIER x2 - x100, multiplies the spin win";
            case MONEY -> "MONEY - carries a cash value";
            case NORMAL -> {
                StringBuilder builder = new StringBuilder();
                double unit = switch (game.mechanic) {
                    case TUMBLE -> bet;
                    case WAYS -> bet / (double) game.waysDivisor;
                    default -> bet / (double) Math.max(1, game.lines.length);
                };
                String[] counts = game.mechanic == SlotGame.Mechanic.TUMBLE ? new String[]{"8+", "10+", "12+"} : null;
                for (int i = 0; i < symbol.pays().length; i++) {
                    if (symbol.pays()[i] <= 0) continue;
                    String count = counts != null ? counts[i] : i + "x";
                    builder.append(count).append(' ').append(compact(Math.round(symbol.pays()[i] * unit * game.payScale))).append("  ");
                }
                yield builder.toString().trim();
            }
        };
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (width(next) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // ---------------------------------------------------------------- coins

    private void spawnCoins(int count) {
        for (int i = 0; i < count; i++) {
            coins.add(new float[]{random.nextFloat() * width, -10 - random.nextFloat() * 80,
                    (random.nextFloat() - 0.5f) * 40, 60 + random.nextFloat() * 120, random.nextFloat() * 6.28f});
        }
    }

    private void updateCoins(GuiGraphics g, float dt) {
        coins.removeIf(c -> c[1] > height + 10);
        for (float[] c : coins) {
            c[0] += c[2] * dt;
            c[1] += c[3] * dt;
            c[3] += 200 * dt;
            c[4] += dt * 8;
            int w = Math.max(1, (int) Math.abs(Math.cos(c[4]) * 4));
            g.fill((int) c[0] - w, (int) c[1] - 4, (int) c[0] + w, (int) c[1] + 4, GOLD);
            g.fill((int) c[0] - w / 2, (int) c[1] - 3, (int) c[0] + w / 2, (int) c[1] + 3, 0xFFFFE890);
        }
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = millis() + 2200;
    }

    // ---------------------------------------------------------------- input

    private void changeBet(int direction) {
        if (phase != Phase.IDLE || feature != null) return;
        int next = Math.max(0, Math.min(BETS.length - 1, betIndex + direction));
        if (next != betIndex) {
            betIndex = next;
            ArcadeSound.select();
        }
    }

    private void skipBanner() {
        if (phase == Phase.BANNER && millis() - bannerStart > 300) bannerStart = millis() - bannerLength;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (showInfo) {
            showInfo = false;
            return true;
        }
        if (confirmBuy) {
            int[] r = confirmRect();
            int cx = width / 2, by = r[1] + r[3] - 28;
            if (inside(mx, my, cx - 108, by, 100, 18)) confirmPurchase();
            else if (inside(mx, my, cx + 8, by, 100, 18)) confirmBuy = false;
            return true;
        }
        if (game.canBuyBonus()) {
            int[] r = buyPanel();
            if (inside(mx, my, r[0], r[1], r[2], r[3])) {
                if (canBuy()) {
                    confirmBuy = true;
                    ArcadeSound.select();
                } else {
                    showToast(feature != null || phase != Phase.IDLE ? "FINISH THE CURRENT SPIN FIRST"
                            : autoplay ? "STOP AUTOPLAY FIRST" : "NOT ENOUGH CHIPS FOR THE BONUS BUY");
                    ArcadeSound.hit();
                }
                return true;
            }
        }
        if (phase == Phase.BANNER) {
            skipBanner();
            return true;
        }
        int[] spin = spinButton();
        if (Math.hypot(mx - spin[0], my - spin[1]) <= spin[2]) {
            if (autoplay) autoplay = false;
            else spin();
            return true;
        }
        int[] minus = betMinus(), plus = betPlus(), auto = autoButton(), turboRect = turboButton(), info = infoButton();
        if (inside(mx, my, minus[0], minus[1], minus[2], minus[3])) changeBet(-1);
        else if (inside(mx, my, plus[0], plus[1], plus[2], plus[3])) changeBet(1);
        else if (inside(mx, my, auto[0], auto[1], auto[2], auto[3]) && feature == null) {
            autoplay = !autoplay;
            ArcadeSound.select();
        } else if (inside(mx, my, turboRect[0], turboRect[1], turboRect[2], turboRect[3])) {
            turbo = !turbo;
            ArcadeSound.select();
        } else if (inside(mx, my, info[0], info[1], info[2], info[3])) {
            showInfo = true;
            ArcadeSound.blip();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (key == GLFW_KEY_ESCAPE) {
            if (confirmBuy) confirmBuy = false;
            else if (showInfo) showInfo = false;
            else onClose();
            return true;
        }
        if (confirmBuy) {
            if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) confirmPurchase();
            return true;
        }
        if (showInfo) {
            showInfo = false;
            return true;
        }
        switch (key) {
            case GLFW_KEY_SPACE, GLFW_KEY_ENTER -> {
                if (phase == Phase.BANNER) skipBanner();
                else if (autoplay) autoplay = false;
                else spin();
            }
            case GLFW_KEY_UP, GLFW_KEY_W -> changeBet(1);
            case GLFW_KEY_DOWN, GLFW_KEY_S -> changeBet(-1);
            case GLFW_KEY_A -> {
                if (feature == null) autoplay = !autoplay;
            }
            case GLFW_KEY_T -> turbo = !turbo;
            case GLFW_KEY_I -> showInfo = true;
            case GLFW_KEY_B -> {
                if (canBuy()) confirmBuy = true;
            }
            default -> {
            }
        }
        return true;
    }
}
