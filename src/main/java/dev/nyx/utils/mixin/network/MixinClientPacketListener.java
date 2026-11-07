package dev.nyx.utils.mixin.network;

import dev.nyx.utils.event.impl.network.ChatEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.nyx.utils.util.traits.Util.EVENT_BUS;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Unique
    private static boolean nyx$resending;

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sendChat(String content, CallbackInfo ci) {
        if (nyx$resending) return;
        ChatEvent event = new ChatEvent(content);
        if (EVENT_BUS.post(event)) {
            ci.cancel();
            return;
        }
        String message = event.getMessage();
        if (message == null || message.equals(content)) return;
        ci.cancel();
        message = StringUtil.trimChatMessage(message);
        if (message.isBlank()) return;
        nyx$resending = true;
        try {
            ((ClientPacketListener) (Object) this).sendChat(message);
        } finally {
            nyx$resending = false;
        }
    }
}
