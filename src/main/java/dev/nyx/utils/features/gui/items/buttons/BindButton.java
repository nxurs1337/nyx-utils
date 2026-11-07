package dev.nyx.utils.features.gui.items.buttons;

import dev.nyx.utils.features.settings.Bind;
import dev.nyx.utils.features.settings.Setting;
import dev.nyx.utils.util.KeyboardUtil;
import dev.nyx.utils.features.gui.items.ItemStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class BindButton
        extends Button {
    private final Setting<Bind> setting;
    public boolean isListening;

    public BindButton(Setting<Bind> setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        boolean hover = this.isHovering(mouseX, mouseY);
        ItemStyle.row(context, this, this.height, hover);
        ItemStyle.label(context, this, this.setting.getName(), ItemStyle.TEXT);
        String key = this.isListening ? "PRESS A KEY" : KeyboardUtil.getKeyName(setting.getValue()).toUpperCase(java.util.Locale.ROOT);
        ItemStyle.keycap(context, this, key, this.isListening);
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isListening) {
            if (mouseButton != 0 && mouseButton != 1) {
                this.setting.setValue(new Bind(-mouseButton - 2));
                this.onMouseClick();
            }
        } else if (this.isHovering(mouseX, mouseY)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (this.isListening) {
            Bind bind = new Bind(key);
            if (key == GLFW.GLFW_KEY_DELETE
                    || key == GLFW.GLFW_KEY_BACKSPACE
                    || key == GLFW.GLFW_KEY_ESCAPE) {
                bind = new Bind(-1);
            }
            this.setting.setValue(bind);
            this.onMouseClick();
        }
    }

    @Override
    public void toggle() {
        this.isListening = !this.isListening;
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }
}