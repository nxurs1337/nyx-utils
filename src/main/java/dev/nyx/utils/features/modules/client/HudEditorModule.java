package dev.nyx.utils.features.modules.client;

import dev.nyx.utils.features.gui.HudEditorScreen;
import dev.nyx.utils.features.modules.Module;

public class HudEditorModule extends Module {
    public HudEditorModule() {
        super("HudEditor", "Edit HUD element positions", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        mc.setScreen(HudEditorScreen.getInstance());
        disable();
    }
}

