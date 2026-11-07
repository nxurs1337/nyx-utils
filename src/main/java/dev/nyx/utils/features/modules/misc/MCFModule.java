package dev.nyx.utils.features.modules.misc;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.commands.Command;
import dev.nyx.utils.features.modules.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import static dev.nyx.utils.util.chat.SimpleSignature.from;

public class MCFModule extends Module {
    private boolean pressed;

    public MCFModule() {
        super("MCF", "Middle click friend", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.screen != null) {
            pressed = false;
            return;
        }

        if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), 2) == 1) {
            if (!pressed) click();
            pressed = true;
        } else {
            pressed = false;
        }
    }

    private void click() {
        Entity targetedEntity = mc.crosshairPickEntity;
        if (!(targetedEntity instanceof Player)) return;
        String name = ((Player) targetedEntity).getGameProfile().name();

        if (NyxUtils.friendManager.isFriend(name)) {
            NyxUtils.friendManager.removeFriend(name);

            Command.sendMessage("{red} %s has been unfriended.", from(getName()), name);
        } else {
            NyxUtils.friendManager.addFriend(name);
            Command.sendMessage("{aqua} %s has been friended.", from(getName()), name);
        }
    }
}
