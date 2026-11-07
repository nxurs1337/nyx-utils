package dev.nyx.utils.features.modules.render;

import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import dev.nyx.utils.features.modules.render.ModelChangerModule.Model;
import dev.nyx.utils.features.modules.render.model.ModelLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

/**
 * Model changer picker: every mob as a card with its head and name, plus the custom .glb models.
 * Clicking a card turns you (and your chosen targets) into it.
 */
public class ModelPickerScreen extends Screen {
    private static final int CARD_W = 66;
    private static final int CARD_H = 78;
    private static final int GAP = 8;
    private static final int TOP = 44;
    /** Body yaw that makes a mob face the viewer in the picture-in-picture renderer, plus a slight turn. */
    private static final float PORTRAIT_YAW = 90 + 20;

    private enum Kind {MOB, CUSTOM, OPEN_FOLDER, RELOAD}

    private record Card(Kind kind, Model model, String custom, String label) {
    }

    private final Screen parent;
    private final ModelChangerModule module;
    private final Map<Model, LivingEntity> previews = new EnumMap<>(Model.class);
    private List<Card> cards = new ArrayList<>();
    private int scroll;
    private int hovered = -1;

    public ModelPickerScreen(Screen parent, ModelChangerModule module) {
        super(Component.literal("Model Select"));
        this.parent = parent;
        this.module = module;
        rebuild();
    }

    private void rebuild() {
        List<Card> list = new ArrayList<>();
        for (Model model : Model.values()) {
            if (model == Model.CUSTOM) continue;
            list.add(new Card(Kind.MOB, model, null, model.name().replace('_', ' ')));
        }
        for (String name : ModelLibrary.names()) {
            list.add(new Card(Kind.CUSTOM, Model.CUSTOM, name, name.replace('_', ' ').toUpperCase(Locale.ROOT)));
        }
        list.add(new Card(Kind.OPEN_FOLDER, null, null, "OPEN FOLDER"));
        list.add(new Card(Kind.RELOAD, null, null, "RELOAD"));
        cards = list;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    // ---------------------------------------------------------------- layout

    private int columns() {
        return Math.max(1, (width - 20 + GAP) / (CARD_W + GAP));
    }

    /** Mobs fill the first rows, custom models start on a fresh row under their own header. */
    private int[] cardPos(int index) {
        int cols = columns();
        int mobs = (int) cards.stream().filter(c -> c.kind == Kind.MOB).count();
        int mobRows = (mobs + cols - 1) / cols;
        int row, col;
        int extra = 0;
        if (index < mobs) {
            row = index / cols;
            col = index % cols;
        } else {
            int i = index - mobs;
            row = mobRows + i / cols;
            col = i % cols;
            extra = 22;
        }
        int rowW = cols * CARD_W + (cols - 1) * GAP;
        int x = (width - rowW) / 2 + col * (CARD_W + GAP);
        int y = TOP + 16 + row * (CARD_H + GAP) + extra + scroll;
        return new int[]{x, y};
    }

    private int cardAt(double mx, double my) {
        if (my < TOP) return -1;
        for (int i = 0; i < cards.size(); i++) {
            int[] p = cardPos(i);
            if (mx >= p[0] && mx < p[0] + CARD_W && my >= p[1] && my < p[1] + CARD_H) return i;
        }
        return -1;
    }

    private int contentHeight() {
        int[] last = cardPos(cards.size() - 1);
        return last[1] - scroll + CARD_H + 10 - TOP;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        synthwave(g, width, height, CYAN);
        g.fill(0, 0, width, height, 0x90000000);

        int hover = cardAt(mouseX, mouseY);
        if (hover != -1 && hover != hovered) ArcadeSound.blip();
        hovered = hover;

        g.enableScissor(0, TOP, width, height - 14);
        int mobs = (int) cards.stream().filter(c -> c.kind == Kind.MOB).count();
        glowText(g, "MOBS", cardPos(0)[0], cardPos(0)[1] - 13, CYAN);
        if (mobs < cards.size()) {
            int[] p = cardPos(mobs);
            glowText(g, "CUSTOM MODELS  (.GLB)", p[0], p[1] - 13, PINK);
        }
        for (int i = 0; i < cards.size(); i++) {
            int[] p = cardPos(i);
            if (p[1] > height || p[1] + CARD_H < TOP) continue;
            renderCard(g, cards.get(i), p[0], p[1], i == hover);
        }
        g.disableScissor();

        // header and footer drawn last so cards scroll underneath
        g.fill(0, 0, width, TOP, 0xE0060214);
        g.fill(0, TOP - 1, width, TOP, CYAN);
        bigText(g, "MODEL SELECT", width / 2, 6, 2f, cycle(0));
        String current = module.model.getValue() == Model.CUSTOM
                ? "NOW: " + module.custom.getValue().replace('_', ' ').toUpperCase(Locale.ROOT)
                : "NOW: " + module.model.getValue().name().replace('_', ' ');
        centered(g, current + "   -   CLICK A CARD TO TRANSFORM   -   ESC TO GO BACK", width / 2, 28, DIM);

        g.fill(0, height - 14, width, height, 0xE0060214);
        g.fill(0, height - 14, width, height - 13, CYAN);
        String footer = footer(hover);
        centered(g, footer, width / 2, height - 10, footer.startsWith("ERROR") ? RED : DIM);
    }

    private String footer(int hover) {
        if (hover >= 0) {
            Card card = cards.get(hover);
            if (card.kind == Kind.CUSTOM) {
                String error = ModelLibrary.error(card.custom);
                if (error != null) return "ERROR: " + error.toUpperCase(Locale.ROOT);
                String credit = ModelLibrary.credit(card.custom);
                if (credit != null) return credit.toUpperCase(Locale.ROOT);
                return "FROM YOUR MODELS FOLDER";
            }
            if (card.kind == Kind.OPEN_FOLDER) return "DROP .GLB FILES INTO .MINECRAFT/NYXUTILS/MODELS";
            if (card.kind == Kind.RELOAD) return "RESCAN THE MODELS FOLDER";
        }
        return "CUSTOM MODELS: .GLB FILES IN .MINECRAFT/NYXUTILS/MODELS";
    }

    private boolean isSelected(Card card) {
        if (card.kind == Kind.MOB) return module.model.getValue() == card.model;
        if (card.kind == Kind.CUSTOM) return module.model.getValue() == Model.CUSTOM && card.custom.equals(module.custom.getValue());
        return false;
    }

    private void renderCard(GuiGraphics g, Card card, int x, int y, boolean hover) {
        boolean selected = isSelected(card);
        int accent = card.kind == Kind.MOB ? CYAN : card.kind == Kind.CUSTOM ? PINK : YELLOW;
        int x2 = x + CARD_W, y2 = y + CARD_H;
        if (hover) y -= 2;
        g.fill(x + 2, y + 3, x2 + 2, y2 + 3, 0x80000000);
        g.fillGradient(x, y, x2, y2, hover ? 0xFF2A1C48 : 0xFF1C1234, 0xFF0C0718);
        if (selected) {
            neonBox(g, x, y, x2, y2, YELLOW);
        } else {
            outline(g, x, y, x2, y2, hover ? accent : alpha(accent, 110), 1);
        }

        // portrait window
        int px1 = x + 4, py1 = y + 4, px2 = x2 - 4, py2 = y + CARD_H - 20;
        g.fillGradient(px1, py1, px2, py2, mix(accent, 0xFF000000, 0.75f), 0xFF05020C);
        switch (card.kind) {
            case MOB -> portrait(g, card.model, px1, py1, px2, py2, hover);
            case CUSTOM -> customIcon(g, card, px1, py1, px2, py2, hover);
            case OPEN_FOLDER -> folderIcon(g, (px1 + px2) / 2, (py1 + py2) / 2);
            case RELOAD -> bigText(g, "@", (px1 + px2) / 2 + 1, (py1 + py2) / 2 - 10, 2.5f, YELLOW);
        }
        outline(g, px1, py1, px2, py2, alpha(accent, 80), 1);

        // name plate
        String label = card.label;
        float s = Math.min(0.75f, (CARD_W - 6) / (float) width(label));
        g.pose().pushMatrix();
        g.pose().translate(x + CARD_W / 2f, y2 - 13);
        g.pose().scale(s, s);
        if (selected) glowText(g, label, -width(label) / 2, 0, YELLOW);
        else text(g, label, -width(label) / 2, 0, hover ? WHITE : mix(accent, WHITE, 0.5f));
        g.pose().popMatrix();
        if (selected) {
            g.fill(x2 - 9, y + 2, x2 - 2, y + 9, YELLOW);
            text(g, "!", x2 - 7, y + 2, 0xFF201000);
        }
    }

    /** The mob's head, rendered live, slowly turning when hovered. */
    private void portrait(GuiGraphics g, Model model, int x1, int y1, int x2, int y2, boolean hover) {
        LivingEntity entity = preview(model);
        if (entity == null) {
            ItemStack egg = spawnEgg(model);
            g.pose().pushMatrix();
            g.pose().translate((x1 + x2) / 2f - 16, (y1 + y2) / 2f - 16);
            g.pose().scale(2, 2);
            g.renderItem(egg, 0, 0);
            g.pose().popMatrix();
            return;
        }
        float sway = hover ? (float) Math.sin(millis() / 400.0) * 35 : 0;
        float yaw = PORTRAIT_YAW + sway;
        entity.yBodyRot = entity.yBodyRotO = yaw;
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.yHeadRot = entity.yHeadRotO = yaw;
        entity.setXRot(0);
        entity.xRotO = 0;

        @SuppressWarnings("unchecked")
        EntityRenderer<Entity, EntityRenderState> renderer = (EntityRenderer<Entity, EntityRenderState>) minecraft.getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, 1.0f);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        // frame the head: focus on the eyes, zoom so roughly the head fills the window
        float head = Math.max(0.45f, Math.min(3f, Math.min(entity.getBbHeight(), entity.getBbWidth()) * 0.9f));
        float scale = (y2 - y1) * 0.85f / head;
        Vector3f focus = new Vector3f(0, entity.getEyeHeight(), 0);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        g.submitEntityRenderState(state, scale, focus, rotation, null, x1, y1, x2, y2);
    }

    private LivingEntity preview(Model model) {
        if (minecraft.level == null || model.type == null) return null;
        return previews.computeIfAbsent(model, m -> {
            Entity entity = m.type.create(minecraft.level, EntitySpawnReason.LOAD);
            return entity instanceof LivingEntity living ? living : null;
        });
    }

    private static ItemStack spawnEgg(Model model) {
        Identifier id = Identifier.withDefaultNamespace(model.name().toLowerCase(Locale.ROOT) + "_spawn_egg");
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }

    /** A spinning wireframe cube for custom models (they can't be previewed in the gui yet). */
    private void customIcon(GuiGraphics g, Card card, int x1, int y1, int x2, int y2, boolean hover) {
        int cx = (x1 + x2) / 2, cy = (y1 + y2) / 2 - 2;
        double a = millis() / (hover ? 300.0 : 900.0);
        float r = 13;
        float[][] corners = new float[8][2];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -1 : 1, y = (i & 2) == 0 ? -1 : 1, z = (i & 4) == 0 ? -1 : 1;
            double rx = x * Math.cos(a) - z * Math.sin(a), rz = x * Math.sin(a) + z * Math.cos(a);
            double ry = y * Math.cos(0.5) - rz * Math.sin(0.5);
            corners[i][0] = (float) (cx + rx * r * 0.75);
            corners[i][1] = (float) (cy + ry * r * 0.75);
        }
        int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        boolean broken = ModelLibrary.error(card.custom) != null;
        int color = broken ? RED : PINK;
        for (int[] e : edges) line(g, corners[e[0]], corners[e[1]], color);
        String tag = broken ? "ERROR" : "3D";
        centered(g, tag, cx, y2 - 9, broken ? RED : WHITE);
    }

    private static void line(GuiGraphics g, float[] a, float[] b, int color) {
        int steps = (int) Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])) + 1;
        for (int i = 0; i <= steps; i++) {
            int x = Math.round(a[0] + (b[0] - a[0]) * i / steps), y = Math.round(a[1] + (b[1] - a[1]) * i / steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void folderIcon(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 14, cy - 10, cx - 3, cy - 6, 0xFFE0A830);
        g.fill(cx - 14, cy - 7, cx + 14, cy + 11, 0xFFFFC23A);
        g.fill(cx - 14, cy - 3, cx + 14, cy - 2, 0xFFE0A830);
        centered(g, "+", cx + 1, cy + 1, 0xFF6A3A00);
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int index = cardAt(click.x(), click.y());
        if (index < 0 || click.button() != 0) return true;
        Card card = cards.get(index);
        switch (card.kind) {
            case MOB -> {
                module.select(card.model);
                ArcadeSound.coin();
                onClose();
            }
            case CUSTOM -> {
                if (ModelLibrary.get(card.custom) == null) {
                    ArcadeSound.hit();
                    return true;
                }
                module.selectCustom(card.custom);
                ArcadeSound.coin();
                onClose();
            }
            case OPEN_FOLDER -> {
                try {
                    java.nio.file.Files.createDirectories(ModelLibrary.folder());
                } catch (java.io.IOException ignored) {
                }
                Util.getPlatform().openPath(ModelLibrary.folder());
                ArcadeSound.select();
            }
            case RELOAD -> {
                ModelLibrary.refresh();
                rebuild();
                ArcadeSound.levelUp();
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int max = Math.max(0, contentHeight() - (height - TOP - 14));
        scroll = Math.min(0, Math.max(-max, scroll + (vertical > 0 ? 16 : -16)));
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
