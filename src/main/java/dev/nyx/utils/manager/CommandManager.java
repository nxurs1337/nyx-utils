package dev.nyx.utils.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.Feature;
import dev.nyx.utils.features.commands.Command;
import dev.nyx.utils.features.commands.impl.*;
import dev.nyx.utils.util.traits.Jsonable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static dev.nyx.utils.features.commands.Command.SINGLE_FAILURE;
import static dev.nyx.utils.features.commands.MessageSignatures.GENERAL;
import static dev.nyx.utils.features.commands.MessageSignatures.SUCCESS;

public class CommandManager extends Feature implements Jsonable {
    private static final Logger LOGGER = LogManager.getLogger("Commands");

    private final CommandDispatcher<CommandManager> dispatcher = new CommandDispatcher<>();
    private final Map<String, Command> commandAliasMap = new LinkedHashMap<>();
    private final List<Command> commandList = new LinkedList<>();

    private String commandPrefix = ".";

    public CommandManager() {
        super("Commands");
    }

    public void init() {
        register(new BindCommand());
        register(new DrawnCommand());
        register(new FriendCommand());
        register(new HelpCommand());
        register(new PrefixCommand());
        register(new ToggleCommand());

        LOGGER.info("Registered {} commands", commandList.size());
        NyxUtils.configManager.addConfig(this);
    }

    public void onChatSent(String message) {
        try {
            int result = dispatcher.execute(message.substring(getCommandPrefix().length()).trim(), this);

            if (result == SINGLE_FAILURE) {
                Command.sendMessage("{red} Failed to execute command", SUCCESS);
            }
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to execute command", e);
            Command.sendMessage("{red} %s", GENERAL, e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error while executing command", e);
            Command.sendMessage("{red} Failed to execute command", GENERAL);
        }
    }

    public void register(Command command) {
        commandList.add(command);
        for (String alias : command.getAliases()) {
            commandAliasMap.put(alias, command);
            LiteralArgumentBuilder<CommandManager> builder = Command.literal(alias);
            command.createArgumentBuilder(builder);
            dispatcher.register(builder);
        }
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public Command getCommand(String alias) {
        return commandAliasMap.get(alias);
    }

    public Set<String> getCommandAliases() {
        return commandAliasMap.keySet();
    }

    public List<Command> getCommands() {
        return commandList;
    }

    public CommandDispatcher<CommandManager> getDispatcher() {
        return dispatcher;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("prefix", commandPrefix);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return;

        JsonObject object = element.getAsJsonObject();

        if (object.has("prefix")) {
            setCommandPrefix(object.get("prefix").getAsString());
        }
    }

    @Override
    public String getFileName() {
        return "commands.json";
    }
}
