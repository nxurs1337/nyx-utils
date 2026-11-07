package dev.nyx.utils.features.modules.hud;

import dev.nyx.utils.event.impl.render.Render2DEvent;
import dev.nyx.utils.features.modules.client.HudModule;
import dev.nyx.utils.features.settings.Setting;
import dev.nyx.utils.util.BuildConfig;
import dev.nyx.utils.util.TextUtil;
import net.minecraft.network.chat.Component;

public class WatermarkHudModule extends HudModule {
    public final Setting<String> text = str("Text", BuildConfig.NAME);
    public final Setting<Boolean> fullVersion = new Setting<>("FullVersion", false);

    public WatermarkHudModule() {
        super("Watermark", "Display watermark", 100, 10);
        if (BuildConfig.USING_GIT) {
            register(fullVersion);
        }
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        String watermarkString = "{global} %s {} %s";
        if (fullVersion.getValue() && BuildConfig.USING_GIT) {
            watermarkString += "/" + BuildConfig.BRANCH + "-" + BuildConfig.HASH;
        }

        Component component = TextUtil.text(watermarkString, text.getValue(), BuildConfig.VERSION);
        e.getContext().drawString(mc.font, component, (int) getX(), (int) getY(), -1);

        setWidth(mc.font.width(component));
        setHeight(mc.font.lineHeight);
    }
}

