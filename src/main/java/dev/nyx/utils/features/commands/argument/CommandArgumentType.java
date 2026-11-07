package dev.nyx.utils.features.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.commands.Command;
import dev.nyx.utils.features.commands.CommandExceptions;

import java.util.concurrent.CompletableFuture;

import static dev.nyx.utils.features.commands.ArgumentSuggestions.suggest;

public final class CommandArgumentType implements ArgumentType<Command>, CommandExceptionType {
    @Override
    public Command parse(StringReader reader) throws CommandSyntaxException {
        String value = reader.readString().toLowerCase(java.util.Locale.ROOT);

        for (String alias : NyxUtils.commandManager.getCommandAliases()) {
            if (value.equalsIgnoreCase(alias)) {
                return NyxUtils.commandManager.getCommand(alias);
            }
        }

        throw CommandExceptions.invalidArgument("Invalid command").createWithContext(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggest(NyxUtils.commandManager.getCommandAliases(), builder);
    }

    public static Command getCommand(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Command.class);
    }

    public static CommandArgumentType command() {
        return new CommandArgumentType();
    }
}
