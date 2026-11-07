package dev.nyx.utils.features.gui.items.buttons;

import dev.nyx.utils.features.settings.Setting;
import dev.nyx.utils.features.gui.items.ItemStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class EnumButton
        extends Button {
    public Setting<Enum<?>> setting;

    public EnumButton(Setting<Enum<?>> setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        boolean hover = this.isHovering(mouseX, mouseY);
        ItemStyle.row(context, this, this.height, hover);
        ItemStyle.label(context, this, this.setting.getName(), ItemStyle.TEXT);
        ItemStyle.value(context, this, "< " + this.setting.currentEnumName().toUpperCase(java.util.Locale.ROOT) + " >", ItemStyle.VALUE);
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!this.isHovering(mouseX, mouseY)) return;
        // left click: next mode, right click: previous mode (or the setting's own chooser)
        if (mouseButton == 1) {
            if (this.setting.getPicker() != null) {
                this.setting.getPicker().run();
                return;
            }
            step(-1);
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double amount) {
        if (!this.isHovering(mouseX, mouseY) || amount == 0) return false;
        step(amount < 0 ? 1 : -1);
        return true;
    }

    private void step(int direction) {
        Enum<?>[] constants = this.setting.getValue().getDeclaringClass().getEnumConstants();
        int index = Math.floorMod(this.setting.getValue().ordinal() + direction, constants.length);
        this.setting.setValue(constants[index]);
    }

    @Override
    public void toggle() {
        this.setting.increaseEnum();
    }

    @Override
    public boolean getState() {
        return true;
    }
}