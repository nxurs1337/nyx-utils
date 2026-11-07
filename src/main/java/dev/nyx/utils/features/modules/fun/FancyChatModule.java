package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.event.impl.network.ChatEvent;
import dev.nyx.utils.event.system.Subscribe;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.settings.Setting;

import java.util.Random;

/**
 * Restyles the chat messages you send.
 */
public class FancyChatModule extends Module {
    public enum Style {SMALL_CAPS, FULLWIDTH, BUBBLE, UWU, LEET, MOCKING, REVERSE, SPARKLE}

    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    private static final String[] UWU_FACES = {" uwu", " owo", " >w<", " :3", " ^w^", " (◕ᴗ◕✿)"};

    public final Setting<Style> style = mode("Style", Style.SMALL_CAPS);

    private final Random random = new Random();

    public FancyChatModule() {
        super("FancyChat", "Restyles the messages you send", Category.FUN);
    }

    @Subscribe
    public void onChat(ChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith(NyxUtils.commandManager.getCommandPrefix())) return;
        event.setMessage(apply(style.getValue(), message));
    }

    public String apply(Style style, String text) {
        return switch (style) {
            case SMALL_CAPS -> map(text, c -> c >= 'a' && c <= 'z' ? SMALL_CAPS.charAt(c - 'a')
                    : c >= 'A' && c <= 'Z' ? SMALL_CAPS.charAt(c - 'A') : c);
            case FULLWIDTH -> map(text, c -> c == ' ' ? '　' : c > 0x20 && c < 0x7F ? (char) (c + 0xFEE0) : c);
            case BUBBLE -> map(text, c -> c >= 'a' && c <= 'z' ? (char) (0x24D0 + c - 'a')
                    : c >= 'A' && c <= 'Z' ? (char) (0x24B6 + c - 'A')
                    : c >= '1' && c <= '9' ? (char) (0x2460 + c - '1')
                    : c == '0' ? '⓪' : c);
            case LEET -> map(text, c -> switch (Character.toLowerCase(c)) {
                case 'a' -> '4';
                case 'e' -> '3';
                case 'i' -> '1';
                case 'o' -> '0';
                case 's' -> '5';
                case 't' -> '7';
                default -> c;
            });
            case MOCKING -> {
                StringBuilder builder = new StringBuilder();
                boolean upper = false;
                for (char c : text.toCharArray()) {
                    if (Character.isLetter(c)) {
                        builder.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                        upper = !upper;
                    } else {
                        builder.append(c);
                    }
                }
                yield builder.toString();
            }
            case REVERSE -> new StringBuilder(text).reverse().toString();
            case UWU -> text.replaceAll("[rl]", "w").replaceAll("[RL]", "W")
                    .replaceAll("n([aeiou])", "ny$1").replaceAll("N([aeiouAEIOU])", "Ny$1")
                    + UWU_FACES[random.nextInt(UWU_FACES.length)];
            case SPARKLE -> "✧･ﾟ: *✧ " + text + " ✧*:･ﾟ✧";
        };
    }

    private interface CharMapper {
        char map(char c);
    }

    private static String map(String text, CharMapper mapper) {
        StringBuilder builder = new StringBuilder(text.length());
        for (char c : text.toCharArray()) builder.append(mapper.map(c));
        return builder.toString();
    }

    @Override
    public String getDisplayInfo() {
        return style.getValue().name();
    }
}
