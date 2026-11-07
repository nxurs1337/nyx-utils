package dev.nyx.utils.features.gui.items.buttons;

import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.items.ItemStyle;
import dev.nyx.utils.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * Number setting. Label and value on top, a wide track below:
 * click anywhere to jump, drag (even outside the row), scroll the wheel or press -/+ for exact steps,
 * right click to type a value. Hold shift for ten steps at a time.
 */
public class Slider extends Button {
    private static final int HEIGHT = 28;
    private static final int STEP_BUTTON = 11;

    private final Number min;
    private final Number max;
    private final double difference;

    public Setting<Number> setting;

    private boolean dragging;
    private boolean isListening = false;
    private String input = "";

    public Slider(Setting<Number> setting) {
        super(setting.getName());
        this.setting = setting;
        this.min = setting.getMin();
        this.max = setting.getMax();
        this.difference = max.doubleValue() - min.doubleValue();
        this.width = 15;
        this.height = HEIGHT;
    }

    // track geometry
    private int left() {
        return Math.round(this.x) + 4;
    }

    private int right() {
        return ItemStyle.right(this) - 4;
    }

    private int trackLeft() {
        return left() + STEP_BUTTON + 4;
    }

    private int trackRight() {
        return right() - STEP_BUTTON - 4;
    }

    private int trackTop() {
        return Math.round(this.y) + 16;
    }

    private boolean isInteger() {
        return setting.getValue() instanceof Integer;
    }

    private double step() {
        if (isInteger()) return 1;
        double step = Math.round(difference / 100.0 * 10.0) / 10.0;
        return Math.max(0.1, step);
    }

    @Override
    public void drawScreen(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // keep following the mouse while the button is held, wherever the cursor goes
        if (dragging) {
            if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
                dragging = false;
            } else {
                setSettingFromX(mouseX);
            }
        }

        boolean hover = dragging || this.isHovering(mouseX, mouseY);
        ItemStyle.row(g, this, HEIGHT, hover);

        String value = isListening ? input + (System.currentTimeMillis() / 400 % 2 == 0 ? "_" : " ") : format(setting.getValue());
        ItemStyle.label(g, this, this.getName(), ItemStyle.TEXT);
        ItemStyle.value(g, this, value, isListening ? ItemStyle.VALUE : 0xFFFFFFFF);

        int top = trackTop();
        stepButton(g, "-", left(), top - 2, mouseX, mouseY);
        stepButton(g, "+", right() - STEP_BUTTON, top - 2, mouseX, mouseY);

        int x1 = trackLeft(), x2 = trackRight();
        float fill = (float) Math.max(0, Math.min(1, partialMultiplier()));
        int knobX = x1 + Math.round((x2 - x1) * fill);
        g.fill(x1, top + 1, x2, top + 6, 0xFF0A1A0E);
        ArcadeRender.outline(g, x1, top + 1, x2, top + 6, ItemStyle.MUTED, 1);
        g.fillGradient(x1 + 1, top + 2, knobX, top + 5, 0xFF7CFF9A, 0xFF1FA845);
        // tick marks every 10%
        for (int i = 1; i < 10; i++) {
            int tx = x1 + (x2 - x1) * i / 10;
            g.fill(tx, top + 6, tx + 1, top + 8, ItemStyle.MUTED);
        }
        int knobColor = dragging ? ItemStyle.VALUE : hover ? 0xFFFFFFFF : 0xFFB8FFC8;
        g.fill(knobX - 3, top - 2, knobX + 4, top + 9, 0xFF051008);
        g.fill(knobX - 2, top - 1, knobX + 3, top + 8, knobColor);
        g.fill(knobX - 1, top, knobX + 2, top + 1, 0x80FFFFFF);
    }

    private void stepButton(GuiGraphics g, String label, int x, int y, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + STEP_BUTTON && mouseY >= y && mouseY < y + STEP_BUTTON;
        g.fill(x, y, x + STEP_BUTTON, y + STEP_BUTTON, hover ? 0xFF1F5A2C : 0xFF0F2A16);
        ArcadeRender.outline(g, x, y, x + STEP_BUTTON, y + STEP_BUTTON, hover ? ItemStyle.VALUE : ItemStyle.MUTED, 1);
        ArcadeRender.text(g, label, x + (STEP_BUTTON - ArcadeRender.width(label)) / 2 + 1, y + 2, hover ? ItemStyle.VALUE : ItemStyle.TEXT);
    }

    private boolean inStepButton(int mouseX, int mouseY, int x) {
        int y = trackTop() - 2;
        return mouseX >= x && mouseX < x + STEP_BUTTON && mouseY >= y && mouseY < y + STEP_BUTTON;
    }

    private String format(Number value) {
        if (value instanceof Integer) return String.valueOf(value);
        double v = value.doubleValue();
        return step() < 1 ? String.format(java.util.Locale.US, "%.1f", v) : String.valueOf(Math.round(v));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.isHovering(mouseX, mouseY)) {
            if (isListening && mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) enterNumber();
            return;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            isListening = true;
            input = format(setting.getValue());
            return;
        }
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (isListening) enterNumber();

        if (inStepButton(mouseX, mouseY, left())) {
            nudge(-1);
        } else if (inStepButton(mouseX, mouseY, right() - STEP_BUTTON)) {
            nudge(1);
        } else {
            dragging = true;
            setSettingFromX(mouseX);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) dragging = false;
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double amount) {
        if (!this.isHovering(mouseX, mouseY) || amount == 0) return false;
        nudge(amount > 0 ? 1 : -1);
        return true;
    }

    private void nudge(int direction) {
        boolean shift = GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        setValue(setting.getValue().doubleValue() + direction * step() * (shift ? 10 : 1));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
    }

    @Override
    public void onKeyTyped(String typedChar, int keyCode) {
        if (!isListening) return;
        for (char c : typedChar.toCharArray()) {
            if (Character.isDigit(c)) {
                input += c;
            } else if (c == '-' && input.isEmpty() && min.doubleValue() < 0) {
                input += c;
            } else if ((c == '.' || c == ',') && !isInteger() && !input.contains(".")) {
                input += '.';
            }
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (!isListening) return;
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> enterNumber();
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                isListening = false;
                input = "";
            }
            default -> {
            }
        }
    }

    private void enterNumber() {
        try {
            if (!input.isEmpty() && !input.equals("-") && !input.equals(".")) {
                double value = Double.parseDouble(input);
                if (Double.isFinite(value)) setValue(value);
            }
        } catch (NumberFormatException ignored) {
            return;
        }
        isListening = false;
        input = "";
    }

    /** Clamps, snaps to the step and stores the value with the setting's own number type. */
    private void setValue(double value) {
        value = Math.max(min.doubleValue(), Math.min(max.doubleValue(), value));
        if (isInteger()) {
            setting.setValue((int) Math.round(value));
            return;
        }
        double snapped = Math.round(value * 10.0) / 10.0;
        if (setting.getValue() instanceof Float) setting.setValue((float) snapped);
        else setting.setValue(snapped);
    }

    private void setSettingFromX(int mouseX) {
        double percent = Math.clamp((mouseX - trackLeft()) / (double) Math.max(1, trackRight() - trackLeft()), 0.0, 1.0);
        double before = setting.getValue().doubleValue();
        setValue(min.doubleValue() + difference * percent);
        if (setting.getValue().doubleValue() != before && isInteger()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9f, 0.08f));
        }
    }

    private float partialMultiplier() {
        if (difference == 0) return 0;
        return (float) ((setting.getValue().doubleValue() - min.doubleValue()) / difference);
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= ItemStyle.right(this)
                && mouseY >= this.getY() && mouseY < this.getY() + HEIGHT;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }
}
