package dev.nyx.utils.features.gui.casino;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import dev.nyx.utils.features.gui.casino.slots.SlotGame;
import dev.nyx.utils.features.gui.casino.slots.SlotGames;
import dev.nyx.utils.features.gui.casino.slots.SlotSymbol;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;
import static dev.nyx.utils.features.gui.casino.CasinoRender.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

/**
 * The 18+ high roller lounge: an age gate, the wallet and fifteen slot cabinets.
 */
public class CasinoScreen extends Screen {
    private static final int CARD_W = 86;
    private static final int CARD_H = 108;
    private static final int GAP = 10;
    private static final int TOP = 74;

    private final Screen parent;
    private int scroll;
    private int hovered = -1;
    private String toast;
    private long toastUntil;

    public CasinoScreen(Screen parent) {
        super(Component.literal("High Roller Lounge"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
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

    private boolean gated() {
        return Wallet.get() == LocalWallet.getInstance() && !LocalWallet.getInstance().isAgeConfirmed();
    }

    // ------------------------------------------------------------- layout

    private int columns() {
        return Math.max(1, Math.min(5, (width - 20 + GAP) / (CARD_W + GAP)));
    }

    private int[] cardPos(int i) {
        int cols = columns();
        int rowW = cols * CARD_W + (cols - 1) * GAP;
        return new int[]{(width - rowW) / 2 + (i % cols) * (CARD_W + GAP), TOP + 6 + (i / cols) * (CARD_H + GAP) + scroll};
    }

    private int cardAt(double mx, double my) {
        if (my < TOP || my > height - 18) return -1;
        List<SlotGame> games = SlotGames.all();
        for (int i = 0; i < games.size(); i++) {
            int[] p = cardPos(i);
            if (inside(mx, my, p[0], p[1], CARD_W, CARD_H)) return i;
        }
        return -1;
    }

    private int[] bonusButton() {
        return new int[]{width / 2 + 70, 50, 92, 16};
    }

    // ------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        velvet(g, width, height);

        // marquee
        int w = Math.min(360, width - 20), x1 = (width - w) / 2;
        g.fillGradient(x1, 4, x1 + w, 44, 0xFF2A0610, 0xFF100206);
        outline(g, x1, 4, x1 + w, 44, GOLD, 1);
        bulbs(g, x1 + 4, 8, x1 + w - 4, 40, 7);
        bigText(g, "HIGH ROLLER LOUNGE", width / 2, 14, Math.min(2f, (w - 30) / (float) width("HIGH ROLLER LOUNGE")), cycle(0));

        // wallet bar
        Wallet wallet = Wallet.get();
        goldPanel(g, width / 2 - 170, 48, width / 2 + 166, 68);
        text(g, "18+", width / 2 - 164, 54, RED);
        text(g, "WALLET", width / 2 - 140, 54, DIM);
        glowText(g, wallet.format(wallet.balance()), width / 2 - 100, 54, GOLD);
        int[] b = bonusButton();
        long cooldown = wallet.bonusCooldown();
        String bonusLabel = cooldown > 0 ? String.format("FREE IN %d:%02d", cooldown / 60000, cooldown / 1000 % 60) : "FREE CHIPS!";
        button(g, bonusLabel, b[0], b[1], b[2], b[3], GREEN, inside(mouseX, mouseY, b[0], b[1], b[2], b[3]), cooldown == 0);

        g.enableScissor(0, TOP, width, height - 16);
        int hover = gated() ? -1 : cardAt(mouseX, mouseY);
        if (hover != -1 && hover != hovered) ArcadeSound.select();
        hovered = hover;
        List<SlotGame> games = SlotGames.all();
        for (int i = 0; i < games.size(); i++) {
            int[] p = cardPos(i);
            renderCard(g, games.get(i), p[0], p[1], i == hover);
        }
        g.disableScissor();

        g.fill(0, height - 14, width, height, 0xE0100206);
        g.fill(0, height - 14, width, height - 13, GOLD);
        centered(g, "PLAY MONEY ONLY  -  NYX COINS HAVE NO REAL VALUE  -  ESC TO LEAVE", width / 2, height - 10, DIM);

        if (toast != null && millis() < toastUntil) {
            int tw = width(toast) + 16;
            goldPanel(g, width / 2 - tw / 2, height / 2 - 10, width / 2 + tw / 2, height / 2 + 10);
            centered(g, toast, width / 2, height / 2 - 3, GOLD);
        }

        if (gated()) renderGate(g, mouseX, mouseY);
    }

    private void renderCard(GuiGraphics g, SlotGame game, int x, int y, boolean hover) {
        if (hover) y -= 2;
        int x2 = x + CARD_W, y2 = y + CARD_H;
        int frame = game.theme.frame();
        g.fill(x + 2, y + 3, x2 + 2, y2 + 3, 0x90000000);
        g.fillGradient(x, y, x2, y2, mix(game.theme.bgTop(), 0xFF000000, 0.2f), mix(game.theme.bgBottom(), 0xFF000000, 0.4f));
        outline(g, x, y, x2, y2, hover ? WHITE : frame, 1);
        if (hover) outline(g, x - 2, y - 2, x2 + 2, y2 + 2, alpha(frame, 120), 1);

        // title plate
        g.fillGradient(x + 3, y + 3, x2 - 3, y + 17, mix(frame, WHITE, 0.2f), mix(frame, 0xFF000000, 0.35f));
        float s = Math.min(1f, (CARD_W - 10) / (float) width(game.title));
        g.pose().pushMatrix();
        g.pose().translate(x + CARD_W / 2f, y + 10 - 4 * s);
        g.pose().scale(s, s);
        text(g, game.title, -width(game.title) / 2 + 1, 1, 0x90000000);
        text(g, game.title, -width(game.title) / 2, 0, WHITE);
        g.pose().popMatrix();

        // mini reel window: three symbols that spin while hovered
        int rx1 = x + 6, ry1 = y + 22, rx2 = x2 - 6, ry2 = y + 58;
        backdrop(g, game.theme, rx1, ry1, rx2, ry2);
        g.fill(rx1, ry1, rx2, ry2, 0x40000000);
        outline(g, rx1, ry1, rx2, ry2, alpha(frame, 200), 1);
        int cell = (rx2 - rx1) / 3;
        for (int c = 0; c < 3; c++) {
            int index = hover ? (int) ((millis() / 90 + c * 3L) % game.symbols.size()) : (game.symbols.size() - 1 - c * 2 + game.symbols.size()) % game.symbols.size();
            SlotSymbol symbol = game.symbols.get(index);
            SlotIcons.draw(g, symbol, rx1 + cell * c + cell / 2f, (ry1 + ry2) / 2f, 18);
        }

        // mechanic tag and tagline
        String tag = switch (game.mechanic) {
            case TUMBLE -> "TUMBLE";
            case LINES -> game.lines.length + " LINES";
            case WAYS -> "243 WAYS";
            case COLLECT -> "MONEY COLLECT";
        };
        int tagW = width(tag) + 6;
        g.fill(x + CARD_W / 2 - tagW / 2, y + 62, x + CARD_W / 2 + tagW / 2, y + 73, alpha(frame, 200));
        centered(g, tag, x + CARD_W / 2, y + 64, 0xFF100408);

        g.pose().pushMatrix();
        g.pose().translate(x + 5, y + 78);
        g.pose().scale(0.5f, 0.5f);
        List<String> lines = wrap(game.tagline, (CARD_W - 10) * 2);
        for (int i = 0; i < Math.min(3, lines.size()); i++) text(g, lines.get(i), 0, i * 10, 0xFFE8D8C8);
        g.pose().popMatrix();

        if (hover && blink(350)) centered(g, "PLAY", x + CARD_W / 2, y2 - 10, GOLD);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
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

    private int[] gateRect() {
        return new int[]{width / 2 - 150, height / 2 - 72, width / 2 + 150, height / 2 + 72};
    }

    private void renderGate(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, 0xD0000000);
        int[] r = gateRect();
        goldPanel(g, r[0], r[1], r[2], r[3]);
        int cx = width / 2;
        disc(g, cx, r[1] + 26, 17, RED);
        disc(g, cx, r[1] + 26, 14, 0xFF100206);
        bigText(g, "18+", cx + 1, r[1] + 19, 1.5f, RED);
        glowCentered(g, "ADULTS ONLY", cx, r[1] + 50, GOLD);
        centered(g, "This lounge contains simulated gambling.", cx, r[1] + 66, WHITE);
        centered(g, "Nyx Coins are play money: they can't be", cx, r[1] + 78, DIM);
        centered(g, "bought, sold or cashed out.", cx, r[1] + 88, DIM);
        int by = r[3] - 26;
        button(g, "I AM 18 OR OLDER", cx - 134, by, 128, 18, GOLD, inside(mouseX, mouseY, cx - 134, by, 128, 18), true);
        button(g, "TAKE ME BACK", cx + 6, by, 128, 18, 0xFF6A6A7A, inside(mouseX, mouseY, cx + 6, by, 128, 18), true);
    }

    // -------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (gated()) {
            int[] r = gateRect();
            int cx = width / 2, by = r[3] - 26;
            if (inside(mx, my, cx - 134, by, 128, 18)) {
                LocalWallet.getInstance().setAgeConfirmed(true);
                NyxUtils.configManager.save();
                ArcadeSound.coin();
            } else if (inside(mx, my, cx + 6, by, 128, 18)) {
                onClose();
            }
            return true;
        }
        int[] b = bonusButton();
        if (inside(mx, my, b[0], b[1], b[2], b[3])) {
            long given = Wallet.get().claimBonus();
            if (given > 0) {
                ArcadeSound.levelUp();
                showToast("+" + Wallet.get().format(given) + " FREE CHIPS!");
                NyxUtils.configManager.save();
            } else {
                ArcadeSound.hit();
            }
            return true;
        }
        int card = cardAt(mx, my);
        if (card != -1 && click.button() == 0) {
            ArcadeSound.coin();
            minecraft.setScreen(new SlotMachineScreen(this, SlotGames.all().get(card)));
        }
        return true;
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = millis() + 1800;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int rows = (SlotGames.all().size() + columns() - 1) / columns();
        int content = rows * (CARD_H + GAP) + 10;
        int max = Math.max(0, content - (height - 16 - TOP));
        scroll = Math.min(0, Math.max(-max, scroll + (vertical > 0 ? 14 : -14)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }
}
