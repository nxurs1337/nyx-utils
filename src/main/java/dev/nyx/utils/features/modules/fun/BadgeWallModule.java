package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.features.gui.badges.BadgeWallScreen;
import dev.nyx.utils.features.modules.Module;

/**
 * Opens the 88x31 badge wall. Works like a launcher: it switches itself back off.
 */
public class BadgeWallModule extends Module {
    public BadgeWallModule() {
        super("BadgeWall", "Opens the 88x31 badge wall", Category.FUN);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new BadgeWallScreen(mc.screen));
        disable();
    }
}
