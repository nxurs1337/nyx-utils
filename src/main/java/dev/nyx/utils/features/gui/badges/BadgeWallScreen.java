package dev.nyx.utils.features.gui.badges;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.gui.arcade.ArcadeSound;
import dev.nyx.utils.features.modules.fun.BadgeHudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * A wall of 88x31 badges. Click one and the camera flies in until it fills the screen,
 * then step through the wall with the arrow keys.
 */
public class BadgeWallScreen extends Screen {
    private static final int GAP = 4;
    private static final int TOP = 46;
    private static final int BOTTOM = 36;

    private final Screen parent;
    private final List<Badges.Badge> badges = Badges.ALL;

    private int selected = -1;
    private double overviewFocusY = -1;
    private double focusX, focusY, logScale;
    private boolean cameraReady;
    private long lastFrame;
    private int hovered = -1;
    private String toast;
    private long toastUntil;

    public BadgeWallScreen(Screen parent) {
        super(Component.literal("88x31"));
        this.parent = parent;
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

    // ------------------------------------------------------------ geometry

    private int cols() {
        return width > 520 ? 5 : 3;
    }

    private int rows() {
        return (badges.size() + cols() - 1) / cols();
    }

    private int wallW() {
        return cols() * (Badges.W + GAP) - GAP;
    }

    private int wallH() {
        return rows() * (Badges.H + GAP) - GAP;
    }

    private int badgeX(int i) {
        return (i % cols()) * (Badges.W + GAP);
    }

    private int badgeY(int i) {
        return (i / cols()) * (Badges.H + GAP);
    }

    private double centerX() {
        return width / 2.0;
    }

    private double centerY() {
        return TOP + (height - TOP - BOTTOM) / 2.0;
    }

    private double viewH() {
        return height - TOP - BOTTOM - 8;
    }

    private double overviewScale() {
        double fitW = (width - 40) / (double) wallW();
        return Math.max(0.5, Math.min(3.0, fitW));
    }

    private double zoomScale() {
        return Math.min((width - 60) / (double) Badges.W, (height - TOP - BOTTOM - 30) / (double) Badges.H);
    }

    private double clampOverviewFocus(double fy) {
        double half = viewH() / 2 / overviewScale();
        if (wallH() <= half * 2) return wallH() / 2.0;
        return Math.max(half, Math.min(wallH() - half, fy));
    }

    // -------------------------------------------------------------- camera

    private void updateCamera() {
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;

        if (overviewFocusY < 0) overviewFocusY = clampOverviewFocus(0);
        overviewFocusY = clampOverviewFocus(overviewFocusY);

        double tx, ty, ts;
        if (selected >= 0) {
            tx = badgeX(selected) + Badges.W / 2.0;
            ty = badgeY(selected) + Badges.H / 2.0;
            ts = Math.log(zoomScale());
        } else {
            tx = wallW() / 2.0;
            ty = overviewFocusY;
            ts = Math.log(overviewScale());
        }
        if (!cameraReady) {
            focusX = tx;
            focusY = ty;
            logScale = ts;
            cameraReady = true;
            return;
        }
        double k = 1 - Math.exp(-dt * 7);
        focusX += (tx - focusX) * k;
        focusY += (ty - focusY) * k;
        logScale += (ts - logScale) * k;
    }

    private double scale() {
        return Math.exp(logScale);
    }

    /** 0 = looking at the whole wall, 1 = fully zoomed into a badge. */
    private float zoomAmount() {
        double a = Math.log(overviewScale()), b = Math.log(zoomScale());
        if (b <= a) return 0;
        return (float) Math.max(0, Math.min(1, (logScale - a) / (b - a)));
    }

    private int badgeAt(double mx, double my) {
        if (my < TOP || my > height - BOTTOM) return -1;
        double wx = (mx - centerX()) / scale() + focusX;
        double wy = (my - centerY()) / scale() + focusY;
        for (int i = 0; i < badges.size(); i++) {
            if (wx >= badgeX(i) && wx < badgeX(i) + Badges.W && wy >= badgeY(i) && wy < badgeY(i) + Badges.H) return i;
        }
        return -1;
    }

    // -------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        updateCamera();
        float zoom = zoomAmount();

        g.fillGradient(0, 0, width, height, 0xFF0E0820, 0xFF050210);
        // faint dotted backdrop, like a tiled old homepage
        for (int x = 0; x < width; x += 12)
            for (int y = 0; y < height; y += 12)
                g.fill(x, y, x + 1, y + 1, 0x18FFFFFF);

        int hover = selected < 0 ? badgeAt(mouseX, mouseY) : -1;
        if (hover != -1 && hover != hovered) ArcadeSound.blip();
        hovered = hover;

        double s = scale();
        g.enableScissor(0, TOP, width, height - BOTTOM);
        g.pose().pushMatrix();
        g.pose().translate((float) (centerX() - focusX * s), (float) (centerY() - focusY * s));
        g.pose().scale((float) s, (float) s);
        for (int i = 0; i < badges.size(); i++) {
            int bx = badgeX(i), by = badgeY(i);
            double sx = centerX() + (bx - focusX) * s, sy = centerY() + (by - focusY) * s;
            if (sx > width || sy > height || sx + Badges.W * s < 0 || sy + Badges.H * s < 0) continue;
            boolean lift = i == hovered;
            if (lift) {
                g.fill(bx - 2, by - 2, bx + Badges.W + 2, by + Badges.H + 2, alpha(cycle(0), 160));
            }
            Badges.render(g, badges.get(i), bx, by);
            if (i != selected && zoom > 0) {
                g.fill(bx, by, bx + Badges.W, by + Badges.H, alpha(0xFF000000, (int) (200 * zoom)));
            }
        }
        g.pose().popMatrix();
        g.disableScissor();

        renderHeader(g, zoom);
        renderFooter(g, mouseX, mouseY);

        if (toast != null && millis() < toastUntil) {
            int w = width(toast) + 16;
            int y = height - BOTTOM - 22;
            panel(g, width / 2 - w / 2, y, width / 2 + w / 2, y + 16, GREEN);
            centered(g, toast, width / 2, y + 4, WHITE);
        }
    }

    private void renderHeader(GuiGraphics g, float zoom) {
        g.fill(0, 0, width, TOP, 0xE0080414);
        g.fill(0, TOP - 1, width, TOP, PINK);
        bigText(g, "88x31", width / 2, 6, 2.2f, cycle(0));
        String sub = selected >= 0 ? badges.get(selected).text() : "THE NYX BADGE WALL - CLICK ONE TO TAKE A CLOSER LOOK";
        centered(g, sub, width / 2, 30, selected >= 0 ? YELLOW : DIM);
    }

    private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
        int y = height - BOTTOM;
        g.fill(0, y, width, height, 0xE0080414);
        g.fill(0, y, width, y + 1, PINK);
        if (selected < 0) {
            centered(g, "SCROLL TO BROWSE  -  CLICK A BADGE TO ZOOM  -  ESC TO GO BACK", width / 2, y + 14, DIM);
            return;
        }
        int by = y + 10;
        button(g, "< PREV", 10, by, 54, mouseX, mouseY, CYAN);
        button(g, "NEXT >", width - 64, by, 54, mouseX, mouseY, CYAN);
        int mid = width / 2;
        button(g, "SET AS HUD BADGE", mid - 116, by, 110, mouseX, mouseY, PINK);
        button(g, "SHARE IN CHAT", mid + 6, by, 110, mouseX, mouseY, YELLOW);
        String count = (selected + 1) + " / " + badges.size();
        text(g, count, 70, by + 4, DIM);
    }

    private void button(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 16;
        g.fill(x, y, x + w, y + 16, hover ? alpha(color, 200) : 0xA0000000);
        outline(g, x, y, x + w, y + 16, color, 1);
        centered(g, label, x + w / 2, y + 4, hover ? 0xFF000000 : color);
    }

    private boolean inButton(double mx, double my, int x, int y, int w) {
        return mx >= x && mx < x + w && my >= y && my < y + 16;
    }

    // --------------------------------------------------------------- input

    private void select(int index) {
        selected = Math.floorMod(index, badges.size());
        ArcadeSound.select();
    }

    private void zoomOut() {
        if (selected >= 0) {
            // keep the wall scrolled to where we were looking
            overviewFocusY = clampOverviewFocus(badgeY(selected) + Badges.H / 2.0);
        }
        selected = -1;
        ArcadeSound.blip();
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = millis() + 1800;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (selected >= 0 && my >= height - BOTTOM) {
            int by = height - BOTTOM + 10, mid = width / 2;
            if (inButton(mx, my, 10, by, 54)) select(selected - 1);
            else if (inButton(mx, my, width - 64, by, 54)) select(selected + 1);
            else if (inButton(mx, my, mid - 116, by, 110)) setHudBadge();
            else if (inButton(mx, my, mid + 6, by, 110)) share();
            return true;
        }
        if (click.button() != 0) {
            if (selected >= 0) zoomOut();
            return true;
        }
        int index = badgeAt(mx, my);
        if (selected >= 0) {
            if (index == selected || index == -1) zoomOut();
            else select(index);
        } else if (index != -1) {
            select(index);
            ArcadeSound.coin();
        }
        return true;
    }

    private void setHudBadge() {
        BadgeHudModule module = NyxUtils.moduleManager.getModuleByClass(BadgeHudModule.class);
        if (module == null) return;
        module.badge.setValue(badges.get(selected).id());
        module.cycle.setValue(false);
        if (!module.isEnabled()) module.enable();
        ArcadeSound.levelUp();
        showToast("HUD BADGE SET!");
    }

    private void share() {
        if (minecraft.player == null) {
            showToast("JOIN A WORLD TO SHARE");
            ArcadeSound.hit();
            return;
        }
        minecraft.setScreen(new ChatScreen("[88x31] " + badges.get(selected).text(), false));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (selected >= 0) {
            if (vertical != 0) select(selected + (vertical < 0 ? 1 : -1));
        } else {
            overviewFocusY = clampOverviewFocus(overviewFocusY - vertical * 20 / overviewScale());
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (key == GLFW_KEY_ESCAPE) {
            if (selected >= 0) zoomOut();
            else onClose();
            return true;
        }
        if (selected >= 0) {
            switch (key) {
                case GLFW_KEY_LEFT, GLFW_KEY_A -> select(selected - 1);
                case GLFW_KEY_RIGHT, GLFW_KEY_D -> select(selected + 1);
                case GLFW_KEY_UP, GLFW_KEY_W -> select(selected - cols());
                case GLFW_KEY_DOWN, GLFW_KEY_S -> select(selected + cols());
                default -> {
                }
            }
        } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_RIGHT) {
            select(0);
        }
        return true;
    }
}
