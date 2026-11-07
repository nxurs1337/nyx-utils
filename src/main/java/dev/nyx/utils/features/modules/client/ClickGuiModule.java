package dev.nyx.utils.features.modules.client;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.event.impl.ClientEvent;
import dev.nyx.utils.event.system.Subscribe;
import dev.nyx.utils.features.commands.Command;
import dev.nyx.utils.features.gui.NyxGui;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.settings.Setting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static dev.nyx.utils.features.commands.MessageSignatures.GENERAL;

public class ClickGuiModule extends Module {
    private static ClickGuiModule INSTANCE;

    public final Setting<String> prefix = str("Prefix", ".");
    public final Setting<Color> color = color("Color", 255, 43, 214, 200);
    public final Setting<Color> topColor = color("TopColor", 34, 243, 255, 230);
    public final Setting<Boolean> rainbow = bool("Rainbow", false);
    public final Setting<Integer> rainbowHue = num("Delay", 240, 0, 600);
    public final Setting<Float> rainbowBrightness = num("Brightness", 150.0f, 1.0f, 255.0f);
    public final Setting<Float> rainbowSaturation = num("Saturation", 150.0f, 1.0f, 255.0f);

    public ClickGuiModule() {
        super("ClickGui", "Opens the ClickGui", Module.Category.CLIENT);
        setBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        rainbowHue.setVisibility(v -> rainbow.getValue());
        rainbowBrightness.setVisibility(v -> rainbow.getValue());
        rainbowSaturation.setVisibility(v -> rainbow.getValue());
        INSTANCE = this;
    }

    @Subscribe
    public void onSettingChange(ClientEvent event) {
        if (event.getType() == ClientEvent.Type.SETTING_UPDATE && event.getSetting().getFeature().equals(this)) {
            if (event.getSetting().equals(this.prefix)) {
                NyxUtils.commandManager.setCommandPrefix(this.prefix.getPlannedValue());
                Command.sendMessage("Prefix set to {global} %s", GENERAL, NyxUtils.commandManager.getCommandPrefix());
            }
            if (event.getSetting().equals(this.color)) {
                NyxUtils.colorManager.setColor(this.color.getPlannedValue());
            }
        }
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            return;
        }
        mc.setScreen(NyxGui.getClickGui());
    }

    @Override
    public void onLoad() {
        NyxUtils.colorManager.setColor(this.color.getValue());
        NyxUtils.commandManager.setCommandPrefix(this.prefix.getValue());
    }

    @Override
    public void onTick() {
        if (!(ClickGuiModule.mc.screen instanceof NyxGui)) {
            this.disable();
        }
    }

    public static ClickGuiModule getInstance() {
        return INSTANCE;
    }
}