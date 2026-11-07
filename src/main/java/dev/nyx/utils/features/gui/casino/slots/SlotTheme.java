package dev.nyx.utils.features.gui.casino.slots;

/**
 * Colours and backdrop of a slot machine.
 */
public record SlotTheme(int bgTop, int bgBottom, int frame, int accent, int reelBg, Pattern pattern) {
    public enum Pattern {RAYS, BUBBLES, STARS, SPARKS, STRIPES, WAVES, FLAMES, SNOW, DIAMONDS}
}
