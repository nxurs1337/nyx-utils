package dev.nyx.utils.util.traits;

import dev.nyx.utils.event.system.EventBus;
import net.minecraft.client.Minecraft;

public interface Util {
    Minecraft mc = Minecraft.getInstance();
    EventBus EVENT_BUS = new EventBus();

    static boolean isLocalPlayer(Object o) {
        return o == mc.player;
    }
}
