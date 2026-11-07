package dev.nyx.utils.ducks.render;

import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.MessageSignature;

public interface IChatComponent {
    void nyx$addMessage(GuiMessage message);
    void nyx$removeMessage(MessageSignature signature);
}