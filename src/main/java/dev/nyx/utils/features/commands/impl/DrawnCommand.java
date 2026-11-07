package dev.nyx.utils.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nyx.utils.features.commands.Command;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.manager.CommandManager;

import static dev.nyx.utils.features.commands.argument.ModuleArgumentType.getModule;
import static dev.nyx.utils.features.commands.argument.ModuleArgumentType.module;

public class DrawnCommand extends Command {
    public DrawnCommand() {
        super("drawn");
        setDescription("Sets a module to be drawn to the arraylist or not");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("module", module())
                .executes((ctx) -> {
                    Module module = getModule(ctx, "module");
                    module.setDrawn(!module.isDrawn());
                    boolean drawn = module.isDrawn();
                    return success("{gray} %s {reset} is now %s %s",
                            module.getDisplayName(),
                            drawn ? "{green}" : "{red}",
                            drawn ? "drawn" : "hidden");
                }));
    }
}
