package dev.nyx.utils.features.gui.casino;

import dev.nyx.utils.features.gui.casino.slots.SlotSymbol;
import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static dev.nyx.utils.features.gui.arcade.ArcadeRender.*;

/**
 * Draws slot symbols using Minecraft items as artwork.
 */
public final class SlotIcons implements Util {
    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private SlotIcons() {
    }

    public static ItemStack stack(String id) {
        return CACHE.computeIfAbsent(id, key -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(key))));
    }

    /** Draws a symbol centered on (cx, cy) at roughly {@code size} pixels. */
    public static void draw(GuiGraphics g, SlotSymbol symbol, float cx, float cy, float size) {
        if (symbol.label() != null) {
            float scale = size / 9f;
            g.pose().pushMatrix();
            g.pose().translate(cx, cy);
            g.pose().scale(scale, scale);
            int w = mc.font.width(symbol.label());
            g.drawString(mc.font, symbol.label(), -w / 2 + 1, -3, 0xFF5A0000, false);
            g.drawString(mc.font, symbol.label(), -w / 2, -4, RED, false);
            g.pose().popMatrix();
            return;
        }
        float scale = size / 16f;
        g.pose().pushMatrix();
        g.pose().translate(cx - 8 * scale, cy - 8 * scale);
        g.pose().scale(scale, scale);
        g.renderItem(stack(symbol.item()), 0, 0);
        g.pose().popMatrix();
    }
}
