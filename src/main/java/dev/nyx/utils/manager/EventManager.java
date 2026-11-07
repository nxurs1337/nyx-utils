package dev.nyx.utils.manager;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.event.impl.entity.DeathEvent;
import dev.nyx.utils.event.impl.entity.player.TickEvent;
import dev.nyx.utils.event.impl.input.KeyInputEvent;
import dev.nyx.utils.event.impl.input.MouseInputEvent;
import dev.nyx.utils.event.impl.network.ChatEvent;
import dev.nyx.utils.event.impl.network.PacketEvent;
import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.event.impl.render.Render3DEvent;
import dev.nyx.utils.event.system.Subscribe;
import dev.nyx.utils.features.Feature;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class EventManager extends Feature {
    private final Set<Integer> deadPlayers = new HashSet<>();

    public void init() {
        EVENT_BUS.register(this);
    }

    public void onUnload() {
        EVENT_BUS.unregister(this);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck())
            return;
        NyxUtils.speedManager.update();
        NyxUtils.moduleManager.onTick();
        Set<Integer> dead = new HashSet<>();
        for (Player player : mc.level.players()) {
            if (player == null || player.getHealth() > 0.0F)
                continue;
            dead.add(player.getId());
            if (!deadPlayers.contains(player.getId()))
                EVENT_BUS.post(new DeathEvent(player));
        }
        deadPlayers.clear();
        deadPlayers.addAll(dead);
    }

    @Subscribe
    public void onPacketReceive(PacketEvent.Receive event) {
        NyxUtils.serverManager.onPacketReceived();
        if (event.getPacket() instanceof ClientboundSetTimePacket)
            NyxUtils.serverManager.update();
        if (event.getPacket() instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)
                && payload instanceof BrandPayload(String brand)) {
            NyxUtils.serverManager.setServerBrand(brand);
        }
    }

    @Subscribe
    public void onWorldRender(Render3DEvent event) {
        NyxUtils.moduleManager.onRender3D(event);
    }

    @Subscribe
    public void onRenderGameOverlayEvent(Render2DEvent event) {
        NyxUtils.moduleManager.onRender2D(event);
    }

    @Subscribe
    public void onKeyInput(KeyInputEvent event) {
        NyxUtils.moduleManager.onKeyPressed(event.getKey());
    }

    @Subscribe
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == 1) {
            NyxUtils.moduleManager.onMouseClicked(event.getButton());
        }
    }

    @Subscribe
    public void onChatSent(ChatEvent event) {
        String message = event.getMessage();
        if (!message.startsWith(NyxUtils.commandManager.getCommandPrefix())) {
            return;
        }
        event.cancel();
        NyxUtils.commandManager.onChatSent(message);
    }
}