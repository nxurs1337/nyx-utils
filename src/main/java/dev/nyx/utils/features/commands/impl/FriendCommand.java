package dev.nyx.utils.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.features.commands.Command;
import dev.nyx.utils.manager.CommandManager;

import java.util.List;
import java.util.StringJoiner;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "friends", "f");
        setDescription("Manages your friends list");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(literal("list")
                        .executes((ctx) -> {
                            List<String> friends = NyxUtils.friendManager.getFriends();
                            if (friends.isEmpty()) {
                                return success("You have no friends :(");
                            }
                            StringJoiner joiner = new StringJoiner(",");
                            friends.forEach(joiner::add);
                            return success("Friends (%s): %s", friends.size(), joiner);
                        }))
                .then(literal("clear")
                        .executes((ctx) -> {
                            NyxUtils.friendManager.getFriends().clear();
                            return success("Cleared friends list");
                        }))
                .then(literal("add")
                        .then(argument("username", word())
                                .executes((ctx) -> {
                                    String username = getString(ctx, "username");
                                    if (NyxUtils.friendManager.isFriend(username)) {
                                        return success("{green} %s {reset} is already on your friends list.", username);
                                    }
                                    NyxUtils.friendManager.addFriend(username);
                                    return success("Added {green} %s {reset} to your friends list", username);
                                })))
                .then(literal("remove")
                        .then(argument("username", word())
                                .executes((ctx) -> {
                                    String username = getString(ctx, "username");
                                    if (!NyxUtils.friendManager.isFriend(username)) {
                                        return success("{green} %s {reset} is not on your friends list.", username);
                                    }
                                    NyxUtils.friendManager.removeFriend(username);
                                    return success("Removed {green} %s {reset} from your friends list", username);
                                })));
    }
}
