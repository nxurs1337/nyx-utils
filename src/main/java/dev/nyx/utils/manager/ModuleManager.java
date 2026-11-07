package dev.nyx.utils.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nyx.utils.NyxUtils;
import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.gui.NyxGui;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import dev.nyx.utils.event.impl.render.Render3DEvent;
import dev.nyx.utils.features.Feature;
import dev.nyx.utils.features.commands.ModuleCommand;
import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.modules.client.ClickGuiModule;
import dev.nyx.utils.features.modules.fun.*;
import dev.nyx.utils.features.modules.client.HudEditorModule;
import dev.nyx.utils.features.modules.client.NotificationsModule;
import dev.nyx.utils.features.modules.hud.CoordinatesHudModule;
import dev.nyx.utils.features.modules.hud.WatermarkHudModule;
import dev.nyx.utils.features.modules.misc.MCFModule;
import dev.nyx.utils.features.modules.render.BlockHighlightModule;
import dev.nyx.utils.features.modules.render.ModelChangerModule;
import dev.nyx.utils.util.traits.Jsonable;
import dev.nyx.utils.util.traits.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class ModuleManager implements Jsonable, Util {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModuleManager");

    private final Map<Class<? extends Module>, Module> fastRegistry = new HashMap<>();
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        register(new WatermarkHudModule());
        register(new CoordinatesHudModule());
        register(new HudEditorModule());
        register(new ClickGuiModule());
        register(new NotificationsModule());
        register(new MCFModule());
        register(new BlockHighlightModule());
        register(new ModelChangerModule());
        register(new FancyChatModule());
        register(new ChatEmotesModule());
        register(new BouncingLogoModule());
        register(new ScreenWeatherModule());
        register(new ScreenPetModule());
        register(new RainbowCatModule());
        register(new BadgeWallModule());
        register(new BadgeHudModule());

        LOGGER.info("Registered {} modules", modules.size());

        // Create a command for each module for modules to be configurable via command line
        for (Module module : modules) {
            NyxUtils.commandManager.register(new ModuleCommand(module));
        }

        NyxUtils.configManager.addConfig(this);
    }

    public void register(Module module) {
        getModules().add(module);
        fastRegistry.put(module.getClass(), module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Stream<Module> stream() {
        return getModules().stream();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        return (T) fastRegistry.get(clazz);
    }

    public Module getModuleByName(String name) {
        return stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Module getModuleByDisplayName(String display) {
        return stream().filter(m -> m.getDisplayName().equalsIgnoreCase(display)).findFirst().orElse(null);
    }

    public List<Module> getModulesByCategory(Module.Category category) {
        return stream().filter(m -> m.getCategory() == category).toList();
    }

    public List<Module.Category> getCategories() {
        return Arrays.asList(Module.Category.values());
    }

    public void onLoad() {
        getModules().forEach(Module::onLoad);
    }

    public void onTick() {
        stream().filter(Feature::isEnabled).forEach(Module::onTick);
    }

    public void onRender2D(Render2DEvent event) {
        stream().filter(Feature::isEnabled).forEach(module -> module.onRender2D(event));
    }

    public void onRender3D(Render3DEvent event) {
        stream().filter(Feature::isEnabled).forEach(module -> module.onRender3D(event));
    }

    public void onUnload() {
        getModules().forEach(EVENT_BUS::unregister);
        getModules().forEach(Module::onUnload);
    }

    public void onKeyPressed(int key) {
        if (key <= 0) return;
        if (mc.screen instanceof TitleScreen || mc.screen instanceof JoinMultiplayerScreen) {
            if (ClickGuiModule.getInstance().getBind().getKey() == key) {
                NyxGui.open(mc.screen);
            }
            return;
        }
        if (mc.screen != null) return;
        stream().filter(module -> module.getBind().getKey() == key).forEach(Module::toggle);
    }

    public void onMouseClicked(int button) {
        if (mc.screen != null) return;
        int key = -button - 2;
        stream().filter(module -> module.getBind().getKey() == key).forEach(Module::toggle);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        for (Module module : getModules()) {
            object.add(module.getName(), module.toJson());
        }
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        for (Module module : getModules()) {
            module.fromJson(element.getAsJsonObject().get(module.getName()));
        }
    }

    @Override
    public String getFileName() {
        return "modules.json";
    }
}
