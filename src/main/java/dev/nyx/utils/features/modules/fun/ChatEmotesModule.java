package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.event.impl.network.ChatEvent;
import dev.nyx.utils.event.system.Subscribe;
import dev.nyx.utils.features.modules.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns :shortcodes: in your outgoing messages into text emotes.
 */
public class ChatEmotesModule extends Module {
    private static final Map<String, String> EMOTES = new LinkedHashMap<>();

    static {
        EMOTES.put(":shrug:", "¯\\_(ツ)_/¯");
        EMOTES.put(":tableflip:", "(╯°□°)╯︵ ┻━┻");
        EMOTES.put(":unflip:", "┬─┬ ノ( ゜-゜ノ)");
        EMOTES.put(":lenny:", "( ͡° ͜ʖ ͡°)");
        EMOTES.put(":cool:", "(⌐■_■)");
        EMOTES.put(":bear:", "ʕ•ᴥ•ʔ");
        EMOTES.put(":cat:", "=^.^=");
        EMOTES.put(":hug:", "(づ｡◕‿‿◕｡)づ");
        EMOTES.put(":fight:", "(ง'̀-'́)ง");
        EMOTES.put(":look:", "ಠ_ಠ");
        EMOTES.put(":yay:", "\\(^o^)/");
        EMOTES.put(":cry:", "(╥﹏╥)");
        EMOTES.put(":sparkle:", "✧･ﾟ✧");
        EMOTES.put(":heart:", "❤");
        EMOTES.put(":star:", "★");
        EMOTES.put(":music:", "♫");
        EMOTES.put(":sword:", "⚔");
        EMOTES.put(":sun:", "☀");
        EMOTES.put(":moon:", "☾");
        EMOTES.put(":check:", "✔");
    }

    public ChatEmotesModule() {
        super("ChatEmotes", "Type :shrug:, :tableflip:, :lenny:... in chat", Category.FUN);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        StringBuilder list = new StringBuilder();
        for (String code : EMOTES.keySet()) list.append(code).append(' ');
        mc.gui.getChat().addMessage(Component.literal("[Nyx] Emotes: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(list.toString().trim()).withStyle(ChatFormatting.GRAY)));
    }

    // runs before FancyChat so emotes are not restyled into gibberish
    @Subscribe(priority = 10)
    public void onChat(ChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith(NyxUtils.commandManager.getCommandPrefix()) || message.indexOf(':') < 0) return;
        for (Map.Entry<String, String> emote : EMOTES.entrySet()) {
            message = message.replace(emote.getKey(), emote.getValue());
        }
        event.setMessage(message);
    }
}
