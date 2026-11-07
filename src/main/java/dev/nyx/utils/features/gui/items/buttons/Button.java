package dev.nyx.utils.features.gui.items.buttons;

import dev.nyx.utils.features.gui.NyxGui;
import dev.nyx.utils.features.gui.Widget;
import dev.nyx.utils.features.gui.arcade.ArcadeRender;
import dev.nyx.utils.features.gui.items.Item;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class Button
        extends Item {
    private boolean state;

    public Button(String name) {
        super(name);
        this.height = 18;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        int x1 = Math.round(this.x), y1 = Math.round(this.y), x2 = x1 + this.width, y2 = y1 + this.height - 1;
        boolean hovered = this.isHovering(mouseX, mouseY);
        if (this.getState()) {
            context.fillGradient(x1, y1, x2, y2, hovered ? 0xE0FF5AE0 : 0xC0FF2BD6, hovered ? 0xE0B05BFF : 0xC09B4BFF);
            context.fill(x1, y1, x1 + 2, y2, ArcadeRender.CYAN);
        } else {
            context.fill(x1, y1, x2, y2, hovered ? 0x40FFFFFF : 0x30000000);
            context.fill(x1, y1, x1 + 2, y2, hovered ? ArcadeRender.PINK : 0x60FF2BD6);
        }
        drawString(context, this.getName(), this.x + 5f, this.y - 2.0f - (float) NyxGui.getClickGui().getTextOffset(), this.getState() ? -1 : (hovered ? -1 : ArcadeRender.DIM));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.onMouseClick();
        }
    }

    public void onMouseClick() {
        this.state = !this.state;
        this.toggle();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    public void toggle() {
    }

    public boolean getState() {
        return this.state;
    }

    @Override
    public int getHeight() {
        return 18;
    }

    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : NyxGui.getClickGui().getComponents()) {
            if (!widget.drag) continue;
            return false;
        }
        return (float) mouseX >= this.getX() && (float) mouseX <= this.getX() + (float) this.getWidth() + 7.4f
                && (float) mouseY >= this.getY() && (float) mouseY < this.getY() + (float) this.height;
    }
}