package dev.nyx.utils.features.gui;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.gui.arcade.ArcadeGame;
import dev.nyx.utils.features.gui.arcade.ArcadeGames;
import dev.nyx.utils.features.gui.arcade.ArcadeMachineScreen;
import dev.nyx.utils.features.gui.arcade.ArcadeScores;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import dev.nyx.utils.features.gui.badges.BadgeWallScreen;
import dev.nyx.utils.features.gui.casino.CasinoScreen;
import dev.nyx.utils.features.gui.casino.Wallet;
import dev.nyx.utils.features.gui.items.Item;
import dev.nyx.utils.features.gui.items.buttons.ModuleButton;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.util.BuildConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * The click gui, styled as an arcade hall. The game room is full of cabinets (plus the 18+ lounge),
 * every module category is a control deck of light-up arcade buttons.
 */
public class NyxGui extends Screen {
    private enum Nav {
        GAMES("GAME ROOM", YELLOW, Module.Category.GAMES, new String[]{
                "...1...",
                "..111..",
                "...1...",
                "...1...",
                ".11111.",
                "1111111",
                "1111111"}),
        FUN("FUN", PINK, Module.Category.FUN, new String[]{
                "...1...",
                "...1...",
                "1111111",
                ".11111.",
                "..111..",
                ".11.11.",
                "1.....1"}),
        RENDER("RENDER", CYAN, Module.Category.RENDER, new String[]{
                ".......",
                "..111..",
                ".1...1.",
                "1..1..1",
                ".1...1.",
                "..111..",
                "......."}),
        MISC("MISC", GREEN, Module.Category.MISC, new String[]{
                "...1...",
                ".11111.",
                ".1...1.",
                "11.1.11",
                ".1...1.",
                ".11111.",
                "...1..."}),
        CLIENT("CLIENT", ORANGE, Module.Category.CLIENT, new String[]{
                ".1.1.1.",
                "1111111",
                ".1...1.",
                "11.1.11",
                ".1...1.",
                "1111111",
                ".1.1.1."}),
        BADGES("88x31", PURPLE, null, new String[]{
                "1111111",
                "1.....1",
                "1.111.1",
                "1.....1",
                "1.11..1",
                "1.....1",
                "1111111"});

        final String label;
        final int color;
        final Module.Category category;
        final String[] icon;

        Nav(String label, int color, Module.Category category, String[] icon) {
            this.label = label;
            this.color = color;
            this.category = category;
            this.icon = icon;
        }
    }

    private static final int TAB_W = 70;
    private static final int TAB_H = 20;
    private static final int TAB_Y = 56;
    private static final int CAB_W = 72;
    private static final int CAB_H = 122;
    private static final int CAB_GAP = 14;
    private static final int CARD_W = 124;
    private static final int CARD_H = 42;
    private static final int CARD_GAP = 8;
    private static final int SERVICE_W = 190;
    private static final int LOUNGE_H = 64;

    private static NyxGui INSTANCE;
    private static Color colorClipboard = null;

    static {
        INSTANCE = new NyxGui();
    }

    private Screen lastScreen;
    private final ArrayList<Widget> widgets = new ArrayList<>();
    private final Map<Module.Category, List<ModuleButton>> decks = new EnumMap<>(Module.Category.class);
    private Nav nav = Nav.GAMES;
    private ModuleButton service;
    private int scroll;
    private int serviceScroll;
    private int hoveredCabinet = -1;
    private int hoveredTab = -1;
    private int hoveredCard = -1;
    private boolean hoveredLounge;
    private Module pressed;
    private long pressedAt;

    public NyxGui() {
        super(Component.literal("NyxUtils"));
        setInstance();
        load();
    }

    public static NyxGui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NyxGui();
        }
        return INSTANCE;
    }

    public static NyxGui getClickGui() {
        return NyxGui.getInstance();
    }

    public static void open(Screen previous) {
        NyxGui gui = getInstance();
        gui.lastScreen = previous;
        Minecraft.getInstance().setScreen(gui);
    }

    @Override
    public void onClose() {
        Screen previous = this.lastScreen;
        this.lastScreen = null;
        this.minecraft.setScreen(previous);
    }

    private void setInstance() {
        INSTANCE = this;
    }

    private void load() {
        for (Nav entry : Nav.values()) {
            if (entry.category == null || entry.category == Module.Category.GAMES) continue;
            List<ModuleButton> deck = new ArrayList<>(NyxUtils.moduleManager.stream()
                    .filter(m -> m.getCategory() == entry.category && !m.hidden)
                    .map(ModuleButton::new)
                    .toList());
            deck.sort(Comparator.comparing(button -> button.getModule().getName()));
            decks.put(entry.category, deck);
        }
    }

    private List<ModuleButton> deck() {
        return nav.category == null ? List.of() : decks.getOrDefault(nav.category, List.of());
    }

    private int contentTop() {
        return TAB_Y + TAB_H + 10;
    }

    private int contentBottom() {
        return height - 16;
    }

    // ----------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        synthwave(g, width, height, nav.color);
        g.fill(0, 0, width, height, 0x40000000);

        renderMarquee(g);
        renderTabs(g, mouseX, mouseY);

        g.enableScissor(0, contentTop() - 4, width, contentBottom());
        if (nav == Nav.GAMES) {
            renderGameRoom(g, mouseX, mouseY);
        } else {
            renderDeck(g, mouseX, mouseY);
        }
        g.disableScissor();
        if (nav != Nav.GAMES && service != null) renderService(g, mouseX, mouseY, delta);

        renderTicker(g);
    }

    private void renderMarquee(GuiGraphics g) {
        int w = Math.min(380, width - 20);
        int x1 = (width - w) / 2, x2 = x1 + w, y1 = 6, y2 = 50;
        g.fillGradient(x1, y1, x2, y2, 0xFF3A0B5A, 0xFF12031F);
        neonBox(g, x1, y1, x2, y2, PINK);
        bulbs(g, x1 + 4, y1 + 4, x2 - 4, y2 - 4, 8);
        String title = BuildConfig.NAME.toUpperCase(java.util.Locale.ROOT) + " ARCADE";
        float scale = Math.min(2.4f, (w - 30) / (float) width(title));
        bigText(g, title, width / 2, y1 + 11, scale, cycle(0));
        if (blink(600)) centered(g, "- INSERT COIN -", width / 2, y2 - 14, YELLOW);
        else centered(g, "FUN  UTILITY  CLIENT", width / 2, y2 - 14, DIM);
    }

    private int tabsX() {
        return (width - Nav.values().length * (TAB_W + 6) + 6) / 2;
    }

    private int tabAt(int mouseX, int mouseY) {
        if (mouseY < TAB_Y - 2 || mouseY >= TAB_Y + TAB_H + 2) return -1;
        for (int i = 0; i < Nav.values().length; i++) {
            int x = tabsX() + i * (TAB_W + 6);
            if (mouseX >= x && mouseX < x + TAB_W) return i;
        }
        return -1;
    }

    /** Chunky arcade push buttons: the active one is pressed down and lit. */
    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int hovered = tabAt(mouseX, mouseY);
        for (int i = 0; i < Nav.values().length; i++) {
            Nav entry = Nav.values()[i];
            int x = tabsX() + i * (TAB_W + 6);
            boolean active = entry == nav, hover = i == hovered;
            int color = entry.color;
            int depth = active ? 1 : hover ? 2 : 3;
            int top = TAB_Y + (3 - depth);

            // base / shadow under the cap
            g.fill(x + 1, TAB_Y + 4, x + TAB_W + 1, TAB_Y + TAB_H + 4, 0xA0000000);
            g.fill(x, top + depth, x + TAB_W, top + TAB_H + depth, mix(color, 0xFF000000, 0.65f));
            // cap
            int face = active ? color : hover ? mix(color, 0xFF000000, 0.35f) : mix(color, 0xFF000000, 0.6f);
            g.fillGradient(x, top, x + TAB_W, top + TAB_H, mix(face, WHITE, 0.15f), face);
            g.fill(x + 1, top + 1, x + TAB_W - 1, top + 2, alpha(WHITE, active ? 140 : 60));
            outline(g, x, top, x + TAB_W, top + TAB_H, active ? mix(color, WHITE, 0.5f) : alpha(0xFF000000, 160), 1);
            if (active) {
                outline(g, x - 2, top - 2, x + TAB_W + 2, top + TAB_H + 2, alpha(color, 90), 1);
                outline(g, x - 3, top - 3, x + TAB_W + 3, top + TAB_H + 3, alpha(color, 40), 1);
            }
            int ink = active ? 0xFF140820 : hover ? WHITE : mix(color, WHITE, 0.4f);
            sprite(g, x + 5, top + 6, 1, entry.icon, ink);
            int space = TAB_W - 19;
            float fit = Math.min(1f, space / (float) width(entry.label));
            g.pose().pushMatrix();
            g.pose().translate(x + 15 + (space - width(entry.label) * fit) / 2f, top + 6 + (1 - fit) * 3.5f);
            g.pose().scale(fit, fit);
            text(g, entry.label, 0, 0, ink);
            g.pose().popMatrix();
        }
        if (hovered != -1 && hovered != hoveredTab) ArcadeSound.blip();
        hoveredTab = hovered;
    }

    // ------------------------------------------------------------- game room

    private int perRow() {
        return Math.max(1, Math.min(ArcadeGames.all().size(), (width - 20) / (CAB_W + CAB_GAP)));
    }

    private int cabinetRows() {
        return (ArcadeGames.all().size() + perRow() - 1) / perRow();
    }

    private int[] cabinetPos(int index) {
        int perRow = perRow();
        int count = ArcadeGames.all().size();
        int row = index / perRow, col = index % perRow;
        int inRow = Math.min(perRow, count - row * perRow);
        int rowW = inRow * CAB_W + (inRow - 1) * CAB_GAP;
        int x = (width - rowW) / 2 + col * (CAB_W + CAB_GAP);
        int y = contentTop() + 8 + row * (CAB_H + 22) + scroll;
        return new int[]{x, y};
    }

    private int cabinetAt(int mouseX, int mouseY) {
        if (mouseY < contentTop() || mouseY > contentBottom()) return -1;
        for (int i = 0; i < ArcadeGames.all().size(); i++) {
            int[] p = cabinetPos(i);
            if (mouseX >= p[0] && mouseX < p[0] + CAB_W && mouseY >= p[1] - 4 && mouseY < p[1] + CAB_H) return i;
        }
        return -1;
    }

    private int[] loungeRect() {
        int w = Math.min(340, width - 30);
        int x = (width - w) / 2;
        int y = contentTop() + 8 + cabinetRows() * (CAB_H + 22) + 4 + scroll;
        return new int[]{x, y, x + w, y + LOUNGE_H};
    }

    private boolean loungeAt(int mouseX, int mouseY) {
        int[] r = loungeRect();
        return mouseY >= contentTop() && mouseY <= contentBottom()
                && mouseX >= r[0] && mouseX < r[2] && mouseY >= r[1] && mouseY < r[3];
    }

    private void renderGameRoom(GuiGraphics g, int mouseX, int mouseY) {
        int hovered = cabinetAt(mouseX, mouseY);
        if (hovered != -1 && hovered != hoveredCabinet) ArcadeSound.select();
        hoveredCabinet = hovered;

        List<ArcadeGame> games = ArcadeGames.all();
        for (int i = 0; i < games.size(); i++) {
            int[] p = cabinetPos(i);
            renderCabinet(g, games.get(i), p[0], p[1], i == hovered);
        }

        boolean lounge = loungeAt(mouseX, mouseY);
        if (lounge && !hoveredLounge) ArcadeSound.select();
        hoveredLounge = lounge;
        renderLounge(g, lounge);
    }

    /** The neon sign leading to the 18+ slot lounge. */
    private void renderLounge(GuiGraphics g, boolean hover) {
        int[] r = loungeRect();
        int x1 = r[0], y1 = r[1], x2 = r[2], y2 = r[3];
        // velvet rope posts
        for (int px : new int[]{x1 - 12, x2 + 8}) {
            g.fill(px, y1 + 18, px + 4, y2, 0xFFB8912F);
            disc(g, px + 2, y1 + 16, 3, YELLOW);
        }
        g.fill(x1 - 8, y2 - 18, x2 + 8, y2 - 15, 0xFF8A1030);

        g.fillGradient(x1, y1, x2, y2, 0xFF3A0610, 0xFF12020A);
        neonBox(g, x1, y1, x2, y2, hover ? YELLOW : RED);
        bulbs(g, x1 + 4, y1 + 4, x2 - 4, y2 - 4, 7);

        // 18+ badge
        int bx = x1 + 30, by = (y1 + y2) / 2;
        disc(g, bx, by, 15, hover ? YELLOW : RED);
        disc(g, bx, by, 12, 0xFF12020A);
        bigText(g, "18+", bx + 1, by - 6, 1.3f, hover ? YELLOW : RED);

        boolean flicker = (millis() / 90) % 37 == 0;
        int title = flicker ? alpha(PINK, 80) : (hover ? cycle(0) : PINK);
        int textX = (x1 + 52 + x2) / 2;
        bigText(g, "HIGH ROLLER LOUNGE", textX, y1 + 14, 1.4f, title);
        centered(g, "15 SLOT MACHINES  -  PLAY MONEY ONLY", textX, y1 + 33, YELLOW);
        String balance = "WALLET " + Wallet.get().format(Wallet.get().balance());
        centered(g, hover && blink(400) ? "STEP INSIDE >" : balance, textX, y1 + 45, hover ? WHITE : DIM);
    }

    private void renderCabinet(GuiGraphics g, ArcadeGame game, int x, int y, boolean hover) {
        int accent = game.getAccent();
        if (hover) y -= 3;
        int x2 = x + CAB_W, y2 = y + CAB_H;

        g.fillGradient(x - 6, y2 + 2 + (hover ? 3 : 0), x2 + 6, y2 + 10 + (hover ? 3 : 0), alpha(accent, hover ? 120 : 50), 0x00000000);

        g.fillGradient(x, y, x2, y2, 0xFF22163A, 0xFF0C0718);
        g.fill(x, y + 16, x + 4, y2, mix(accent, 0xFF000000, 0.4f));
        g.fill(x2 - 4, y + 16, x2, y2, mix(accent, 0xFF000000, 0.4f));
        if (hover) neonBox(g, x, y, x2, y2, accent);
        else outline(g, x, y, x2, y2, alpha(accent, 120), 1);

        g.fillGradient(x + 2, y + 2, x2 - 2, y + 15, mix(accent, 0xFF000000, 0.2f), mix(accent, 0xFF000000, 0.6f));
        String title = game.getTitle();
        float scale = Math.min(1f, (CAB_W - 8) / (float) width(title));
        g.pose().pushMatrix();
        g.pose().translate(x + CAB_W / 2f, y + 9 - 4 * scale);
        g.pose().scale(scale, scale);
        glowText(g, title, -width(title) / 2, 0, WHITE);
        g.pose().popMatrix();

        int sx1 = x + 7, sy1 = y + 20, sx2 = x2 - 7, sy2 = y + 70;
        g.fill(sx1 - 2, sy1 - 2, sx2 + 2, sy2 + 2, 0xFF050308);
        g.fill(sx1, sy1, sx2, sy2, 0xFF02010A);
        g.enableScissor(sx1, sy1, sx2, sy2);
        game.renderPreview(g, sx1, sy1, sx2 - sx1, sy2 - sy1);
        if (hover && blink(350)) {
            g.fill(sx1, sy2 - 12, sx2, sy2, 0xB0000000);
            centered(g, "PLAY!", (sx1 + sx2) / 2, sy2 - 10, YELLOW);
        }
        crt(g, sx1, sy1, sx2, sy2);
        g.disableScissor();

        int cy = y + 76;
        g.fillGradient(x + 3, cy, x2 - 3, cy + 14, 0xFF3A2860, 0xFF1A1030);
        disc(g, x + 18, cy + 7, 3, RED);
        g.fill(x + 17, cy + 7, x + 19, cy + 12, 0xFF9A9AAA);
        disc(g, x + 42, cy + 7, 2, PINK);
        disc(g, x + 50, cy + 5, 2, CYAN);
        disc(g, x + 58, cy + 7, 2, YELLOW);

        g.fill(x + 22, y + 96, x2 - 22, y + 110, 0xFF100A1A);
        outline(g, x + 22, y + 96, x2 - 22, y + 110, 0xFF3A3050, 1);
        int slot = hover && blink(250) ? RED : 0xFF6B1A24;
        g.fill(x + 30, y + 99, x + 32, y + 107, slot);
        g.fill(x2 - 32, y + 99, x2 - 30, y + 107, slot);
        String hi = "HI " + ArcadeScores.getInstance().get(game.getId());
        float hs = Math.min(0.75f, (CAB_W - 6) / (float) width(hi));
        g.pose().pushMatrix();
        g.pose().translate(x + CAB_W / 2f, y + 113);
        g.pose().scale(hs, hs);
        centered(g, hi, 0, 0, alpha(accent, 220));
        g.pose().popMatrix();
    }

    // ------------------------------------------------------------ module deck

    private int deckRight() {
        return service != null ? width - SERVICE_W - 16 : width;
    }

    private int deckColumns() {
        int avail = deckRight() - 20;
        return Math.max(1, Math.min(4, (avail + CARD_GAP) / (CARD_W + CARD_GAP)));
    }

    private int[] cardPos(int index) {
        int cols = deckColumns();
        int rowW = cols * CARD_W + (cols - 1) * CARD_GAP;
        int x0 = (deckRight() - rowW) / 2;
        return new int[]{x0 + (index % cols) * (CARD_W + CARD_GAP),
                contentTop() + 26 + (index / cols) * (CARD_H + CARD_GAP) + scroll};
    }

    private int cardAt(int mouseX, int mouseY) {
        if (mouseY < contentTop() || mouseY > contentBottom() || mouseX >= deckRight()) return -1;
        List<ModuleButton> deck = deck();
        for (int i = 0; i < deck.size(); i++) {
            int[] p = cardPos(i);
            if (mouseX >= p[0] && mouseX < p[0] + CARD_W && mouseY >= p[1] && mouseY < p[1] + CARD_H) return i;
        }
        return -1;
    }

    private void renderDeck(GuiGraphics g, int mouseX, int mouseY) {
        List<ModuleButton> deck = deck();
        int cx = deckRight() / 2;
        if (deck.isEmpty()) {
            int cy = (contentTop() + contentBottom()) / 2;
            panel(g, cx - 120, cy - 40, cx + 120, cy + 40, nav.color);
            bigText(g, "COMING SOON", cx, cy - 22, 2f, cycle(0));
            if (blink(500)) centered(g, "THIS DECK IS BEING WIRED UP", cx, cy + 4, YELLOW);
            centered(g, "VISIT THE GAME ROOM!", cx, cy + 18, DIM);
            return;
        }

        // deck header plate
        String header = nav.label + " DECK";
        int on = (int) deck.stream().filter(b -> b.getModule().isEnabled()).count();
        int hy = contentTop() + 4 + scroll;
        glowCentered(g, header, cx, hy, nav.color);
        centered(g, on + " / " + deck.size() + " LIT   -   CLICK: TOGGLE   RIGHT CLICK: SETTINGS", cx, hy + 11, DIM);

        int hovered = cardAt(mouseX, mouseY);
        if (hovered != -1 && hovered != hoveredCard) ArcadeSound.blip();
        hoveredCard = hovered;
        for (int i = 0; i < deck.size(); i++) {
            int[] p = cardPos(i);
            renderCard(g, deck.get(i), p[0], p[1], i == hovered);
        }
    }

    private void renderCard(GuiGraphics g, ModuleButton button, int x, int y, boolean hover) {
        Module module = button.getModule();
        boolean on = module.isEnabled();
        boolean selected = service == button;
        int color = nav.color;
        int x2 = x + CARD_W, y2 = y + CARD_H;

        // brushed steel plate with screws
        g.fill(x + 2, y + 3, x2 + 2, y2 + 3, 0x80000000);
        g.fillGradient(x, y, x2, y2, hover ? 0xFF33264E : 0xFF281C40, 0xFF120A22);
        g.fill(x + 1, y + 1, x2 - 1, y + 2, 0x30FFFFFF);
        outline(g, x, y, x2, y2, selected ? YELLOW : on ? alpha(color, 200) : 0xFF3A2C58, 1);
        if (selected) outline(g, x - 2, y - 2, x2 + 2, y2 + 2, alpha(YELLOW, 80), 1);
        for (int[] s : new int[][]{{x + 3, y + 3}, {x2 - 5, y + 3}, {x + 3, y2 - 5}, {x2 - 5, y2 - 5}}) {
            g.fill(s[0], s[1], s[0] + 2, s[1] + 2, 0xFF6A5A8A);
        }

        // the big arcade button
        int bx = x + 20, by = y + CARD_H / 2;
        boolean down = module == pressed && millis() - pressedAt < 140;
        disc(g, bx, by + 2, 12, 0xFF07040E);
        disc(g, bx, by + 1, 11, 0xFF3A3A48);
        if (on) disc(g, bx, by, 13, alpha(color, 50));
        int dome = on ? color : mix(color, 0xFF000000, 0.7f);
        int lift = down ? 1 : 0;
        disc(g, bx, by - 1 + lift, 9, mix(dome, 0xFF000000, 0.3f));
        disc(g, bx, by - 2 + lift, 8, dome);
        disc(g, bx - 3, by - 5 + lift, 2, alpha(WHITE, on ? 200 : 60));

        // label plate
        int tx = x + 38;
        String name = module.getName();
        float s = Math.min(1f, (x2 - tx - 6) / (float) width(name));
        g.pose().pushMatrix();
        g.pose().translate(tx, y + 8);
        g.pose().scale(s, s);
        if (on) glowText(g, name, 0, 0, mix(color, WHITE, 0.5f));
        else text(g, name, 0, 0, 0xFFB8B0D0);
        g.pose().popMatrix();

        String description = module.getDescription();
        g.pose().pushMatrix();
        g.pose().translate(tx, y + 20);
        g.pose().scale(0.5f, 0.5f);
        List<String> lines = wrap(description, (x2 - tx - 6) * 2);
        for (int i = 0; i < Math.min(2, lines.size()); i++) text(g, lines.get(i), 0, i * 10, DIM);
        g.pose().popMatrix();

        // status LED
        int led = on ? GREEN : 0xFF5A1018;
        g.fill(x2 - 10, y2 - 8, x2 - 6, y2 - 4, led);
        if (on) g.fill(x2 - 11, y2 - 9, x2 - 5, y2 - 3, alpha(GREEN, 60));
    }

    private List<String> wrap(String text, int maxWidth) {
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

    // ---------------------------------------------------------- service panel

    private int[] serviceRect() {
        int x1 = width - SERVICE_W - 8, y1 = contentTop();
        return new int[]{x1, y1, width - 8, contentBottom() - 4};
    }

    /** Settings of the selected module, shown like a cabinet's CRT service menu. */
    private void renderService(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int[] r = serviceRect();
        int x1 = r[0], y1 = r[1], x2 = r[2], y2 = r[3];
        g.fill(x1, y1, x2, y2, 0xF0020A04);
        neonBox(g, x1, y1, x2, y2, GREEN);
        Module module = service.getModule();
        glowText(g, "* SERVICE MODE *", x1 + 6, y1 + 5, GREEN);
        text(g, "X", x2 - 10, y1 + 5, mouseX >= x2 - 14 && mouseX < x2 && mouseY >= y1 && mouseY < y1 + 16 ? WHITE : GREEN);
        bigText(g, module.getName().toUpperCase(java.util.Locale.ROOT), (x1 + x2) / 2, y1 + 18, 1.25f, mix(GREEN, WHITE, 0.4f));
        g.fill(x1 + 4, y1 + 32, x2 - 4, y1 + 33, alpha(GREEN, 120));

        int listTop = y1 + 37;
        g.enableScissor(x1 + 2, listTop, x2 - 2, y2 - 2);
        float y = listTop + 2 + serviceScroll;
        for (Item item : service.getSettingItems()) {
            item.update();
            if (item.isHidden()) continue;
            item.setLocation(x1 + 6, y);
            item.setWidth(x2 - x1 - 20);
            item.drawScreen(g, mouseX, mouseY, delta);
            y += item.getHeight() + 2;
        }
        g.disableScissor();
        crt(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1);
    }

    private int serviceContentHeight() {
        int h = 0;
        for (Item item : service.getSettingItems()) {
            if (!item.isHidden()) h += item.getHeight() + 2;
        }
        return h + 6;
    }

    private boolean inService(double mx, double my) {
        if (service == null || nav == Nav.GAMES) return false;
        int[] r = serviceRect();
        return mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
    }

    private List<Item> serviceItems() {
        if (service == null || nav == Nav.GAMES) return List.of();
        return service.getSettingItems().stream().filter(item -> !item.isHidden()).toList();
    }

    // ----------------------------------------------------------------- ticker

    private void renderTicker(GuiGraphics g) {
        int y = height - 13;
        g.fill(0, y, width, height, 0xE0050210);
        g.fill(0, y, width, y + 1, PINK);
        StringBuilder text = new StringBuilder("*** HI-SCORES ***   ");
        for (ArcadeGame game : ArcadeGames.all()) {
            text.append(game.getTitle()).append(' ').append(ArcadeScores.getInstance().get(game.getId())).append("   *   ");
        }
        text.append("WALLET ").append(Wallet.get().format(Wallet.get().balance())).append("   *   ");
        text.append("RIGHT CLICK A MODULE FOR SETTINGS   *   ESC TO LEAVE THE ARCADE   ***   ");
        String line = text.toString();
        int lineW = width(line);
        int offset = (int) ((millis() / 25) % lineW);
        for (int x = -offset; x < width; x += lineW) {
            text(g, line, x, y + 3, YELLOW);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int button = click.button();

        int tab = tabAt(mx, my);
        if (tab != -1) {
            Nav entry = Nav.values()[tab];
            if (entry == Nav.BADGES) {
                ArcadeSound.coin();
                minecraft.setScreen(new BadgeWallScreen(this));
            } else if (entry != nav) {
                nav = entry;
                scroll = 0;
                service = null;
                ArcadeSound.coin();
            }
            return true;
        }

        if (nav == Nav.GAMES) {
            int index = cabinetAt(mx, my);
            if (index != -1 && button == 0) {
                ArcadeSound.coin();
                minecraft.setScreen(new ArcadeMachineScreen(this, ArcadeGames.all().get(index)));
                return true;
            }
            if (loungeAt(mx, my) && button == 0) {
                ArcadeSound.coin();
                minecraft.setScreen(new CasinoScreen(this));
                return true;
            }
            return true;
        }

        if (inService(mx, my)) {
            int[] r = serviceRect();
            if (button == 0 && mx >= r[2] - 14 && my < r[1] + 16) {
                service = null;
                ArcadeSound.blip();
                return true;
            }
            for (Item item : serviceItems()) item.mouseClicked(mx, my, button);
            return true;
        }

        int card = cardAt(mx, my);
        if (card != -1) {
            ModuleButton moduleButton = deck().get(card);
            if (button == 0) {
                moduleButton.getModule().toggle();
                pressed = moduleButton.getModule();
                pressedAt = millis();
                ArcadeSound.select();
            } else if (button == 1) {
                service = service == moduleButton ? null : moduleButton;
                serviceScroll = 0;
                ArcadeSound.coin();
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        for (Item item : serviceItems()) item.mouseReleased((int) click.x(), (int) click.y(), click.button());
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = verticalAmount > 0 ? 12 : verticalAmount < 0 ? -12 : 0;
        if (inService(mouseX, mouseY)) {
            for (Item item : serviceItems()) {
                if (item.mouseScrolled((int) mouseX, (int) mouseY, verticalAmount)) return true;
            }
            int[] r = serviceRect();
            int max = Math.max(0, serviceContentHeight() - (r[3] - r[1] - 40));
            serviceScroll = Math.min(0, Math.max(-max, serviceScroll + step));
            return true;
        }
        scroll = Math.min(0, Math.max(-maxScroll(), scroll + step));
        return true;
    }

    private int maxScroll() {
        int contentHeight;
        if (nav == Nav.GAMES) {
            contentHeight = cabinetRows() * (CAB_H + 22) + LOUNGE_H + 24;
        } else {
            int rows = (deck().size() + deckColumns() - 1) / deckColumns();
            contentHeight = 30 + rows * (CARD_H + CARD_GAP);
        }
        return Math.max(0, contentHeight - (contentBottom() - contentTop()));
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        for (Item item : serviceItems()) item.onKeyPressed(input.input());
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        for (Item item : serviceItems()) item.onKeyTyped(input.codepointAsString(), input.modifiers());
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }//ignore 1.21.8 blur thing

    public final ArrayList<Widget> getComponents() {
        return this.widgets;
    }

    public int getTextOffset() {
        return -6;
    }

    public static Color getColorClipboard() {
        return colorClipboard;
    }

    public static void setColorClipboard(Color color) {
        colorClipboard = color;
    }
}
