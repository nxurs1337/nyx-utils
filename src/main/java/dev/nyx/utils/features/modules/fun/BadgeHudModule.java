package dev.nyx.utils.features.modules.fun;

import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.gui.badges.Badges;
import dev.nyx.utils.features.modules.client.HudModule;
import dev.nyx.utils.features.settings.Setting;
import org.joml.Vector2f;

/**
 * Shows an animated 88x31 badge on your HUD. Pick one from the badge wall or let it cycle.
 */
public class BadgeHudModule extends HudModule {
    public final Setting<String> badge = str("Badge", "nyx");
    public final Setting<Boolean> cycle = bool("Cycle", true);
    public final Setting<Integer> interval = num("Interval", 6, 1, 60);
    public final Setting<Float> scale = num("Scale", 1.0f, 0.5f, 3.0f);

    public BadgeHudModule() {
        super("Badge", "Shows an 88x31 badge on your HUD", Badges.W, Badges.H);
        pos.setValueNoEvent(new Vector2f(0.01f, 0.12f));
    }

    private Badges.Badge current() {
        if (cycle.getValue()) {
            long index = System.currentTimeMillis() / (interval.getValue() * 1000L);
            return Badges.ALL.get((int) (index % Badges.ALL.size()));
        }
        return Badges.byId(badge.getValue());
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        float s = scale.getValue();
        e.getContext().pose().pushMatrix();
        e.getContext().pose().translate(getX(), getY());
        e.getContext().pose().scale(s, s);
        Badges.render(e.getContext(), current(), 0, 0);
        e.getContext().pose().popMatrix();
        setWidth(Badges.W * s);
        setHeight(Badges.H * s);
    }
}
