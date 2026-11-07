package dev.nyx.utils;

import dev.nyx.utils.features.gui.arcade.ArcadeScores;
import dev.nyx.utils.features.gui.casino.LocalWallet;
import dev.nyx.utils.manager.*;
import dev.nyx.utils.util.BuildConfig;
import dev.nyx.utils.util.TextUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NyxUtils implements ModInitializer, ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("NyxUtils");
    public static ServerManager serverManager;
    public static ColorManager colorManager;
    public static EventManager eventManager;
    public static SpeedManager speedManager;
    public static CommandManager commandManager;
    public static FriendManager friendManager;
    public static ModuleManager moduleManager;
    public static ConfigManager configManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Pre-initializing {} v{}",
                BuildConfig.NAME, BuildConfig.VERSION);
        configManager = new ConfigManager();
        eventManager = new EventManager();
        serverManager = new ServerManager();
        friendManager = new FriendManager();
        colorManager = new ColorManager();
        commandManager = new CommandManager();
        moduleManager = new ModuleManager();
        speedManager = new SpeedManager();

        TextUtil.init();
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", BuildConfig.NAME);

        long startTime = System.nanoTime();

        eventManager.init();
        commandManager.init();
        moduleManager.init();
        friendManager.init();
        configManager.addConfig(ArcadeScores.getInstance());
        configManager.addConfig(LocalWallet.getInstance());

        configManager.load();
        colorManager.init();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> configManager.save()));

        long endTime = System.nanoTime();

        LOGGER.info("Initialized {} in {}ms",
                BuildConfig.NAME, (endTime - startTime) / 1000000.0);
    }
}
