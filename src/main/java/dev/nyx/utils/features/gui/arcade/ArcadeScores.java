package dev.nyx.utils.features.gui.arcade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nyx.utils.util.traits.Jsonable;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent hi-scores for every arcade machine, stored in nyxutils/arcade.json.
 */
public class ArcadeScores implements Jsonable {
    private static final ArcadeScores INSTANCE = new ArcadeScores();

    private final Map<String, Integer> scores = new HashMap<>();

    public static ArcadeScores getInstance() {
        return INSTANCE;
    }

    public int get(String game) {
        return scores.getOrDefault(game, 0);
    }

    /** @return true when this is a new hi-score */
    public boolean submit(String game, int score) {
        if (score <= get(game)) return false;
        scores.put(game, score);
        return true;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        scores.forEach(object::addProperty);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                scores.put(entry.getKey(), entry.getValue().getAsInt());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getFileName() {
        return "arcade.json";
    }
}
