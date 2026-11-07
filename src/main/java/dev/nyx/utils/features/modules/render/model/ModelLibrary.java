package dev.nyx.utils.features.modules.render.model;

import dev.nyx.utils.NyxUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Custom models for the model changer: the ones shipped inside the mod, plus every .glb file
 * the player drops into .minecraft/nyxutils/models.
 */
public final class ModelLibrary {
    /** Models bundled in assets/nyxutils/models, with their credits (CC-BY needs attribution). */
    private static final Map<String, String> BUILT_IN = Map.of(
            "tung_tung_tung_sahur", "\"Tung Tung Tung Sahur\" by Eks.Art, CC-BY-4.0"
    );

    private static final Map<String, GlbModel> LOADED = new HashMap<>();
    private static final Map<String, String> FAILED = new HashMap<>();

    private ModelLibrary() {
    }

    public static Path folder() {
        return FabricLoader.getInstance().getGameDir().resolve("nyxutils").resolve("models");
    }

    /** Every model name available right now, built-ins first. */
    public static List<String> names() {
        List<String> names = new ArrayList<>(BUILT_IN.keySet());
        names.sort(String::compareTo);
        try {
            Files.createDirectories(folder());
            try (Stream<Path> files = Files.list(folder())) {
                files.map(path -> path.getFileName().toString())
                        .filter(file -> file.toLowerCase(Locale.ROOT).endsWith(".glb"))
                        .map(file -> file.substring(0, file.length() - 4))
                        .filter(name -> !names.contains(name))
                        .sorted()
                        .forEach(names::add);
            }
        } catch (IOException e) {
            NyxUtils.LOGGER.warn("Could not list custom models", e);
        }
        return names;
    }

    public static boolean isBuiltIn(String name) {
        return BUILT_IN.containsKey(name);
    }

    public static String credit(String name) {
        return BUILT_IN.get(name);
    }

    /** Why a model failed to load, or null. */
    public static String error(String name) {
        return FAILED.get(name);
    }

    /** The model, loaded on first use. Returns null when it is missing or broken. */
    public static GlbModel get(String name) {
        if (name == null || name.isEmpty() || FAILED.containsKey(name)) return null;
        GlbModel model = LOADED.get(name);
        if (model != null) return model;
        try {
            byte[] data = read(name);
            if (data == null) {
                FAILED.put(name, "file not found");
                return null;
            }
            model = GlbModel.load(name, data);
            LOADED.put(name, model);
            NyxUtils.LOGGER.info("Loaded custom model {} ({} triangles)", name, model.triangles());
            return model;
        } catch (Exception e) {
            FAILED.put(name, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            NyxUtils.LOGGER.error("Failed to load custom model {}", name, e);
            return null;
        }
    }

    /** Forget failures and reload files on the next use (after the player changed the folder). */
    public static void refresh() {
        FAILED.clear();
        LOADED.keySet().removeIf(name -> !isBuiltIn(name));
    }

    private static byte[] read(String name) throws IOException {
        Path file = folder().resolve(name + ".glb");
        if (Files.isRegularFile(file)) return Files.readAllBytes(file);
        if (isBuiltIn(name)) {
            try (InputStream in = ModelLibrary.class.getResourceAsStream("/assets/nyxutils/models/" + name + ".glb")) {
                if (in != null) return in.readAllBytes();
            }
        }
        return null;
    }
}
