package dev.nyx.utils.features.gui.arcade;

import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Chiptune-ish sound effects built from note block sounds.
 */
public final class ArcadeSound implements Util {
    private ArcadeSound() {
    }

    private static void play(Holder<SoundEvent> sound, float pitch, float volume) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound.value(), pitch, volume));
    }

    public static void blip() {
        play(SoundEvents.NOTE_BLOCK_BIT, 1.6f, 0.35f);
    }

    public static void select() {
        play(SoundEvents.NOTE_BLOCK_HAT, 1.2f, 0.4f);
    }

    public static void coin() {
        play(SoundEvents.NOTE_BLOCK_CHIME, 1.5f, 0.6f);
        play(SoundEvents.NOTE_BLOCK_BIT, 2.0f, 0.4f);
    }

    public static void score() {
        play(SoundEvents.NOTE_BLOCK_PLING, 1.8f, 0.4f);
    }

    public static void pickup() {
        play(SoundEvents.NOTE_BLOCK_XYLOPHONE, 1.7f, 0.35f);
    }

    public static void shoot() {
        play(SoundEvents.NOTE_BLOCK_BIT, 2.0f, 0.25f);
    }

    public static void hit() {
        play(SoundEvents.NOTE_BLOCK_BASS, 1.4f, 0.5f);
    }

    public static void explode() {
        play(SoundEvents.NOTE_BLOCK_BASS, 0.5f, 0.7f);
    }

    public static void gameOver() {
        play(SoundEvents.NOTE_BLOCK_BIT, 0.6f, 0.6f);
        play(SoundEvents.NOTE_BLOCK_BASS, 0.5f, 0.6f);
    }

    public static void levelUp() {
        play(SoundEvents.NOTE_BLOCK_CHIME, 2.0f, 0.5f);
        play(SoundEvents.NOTE_BLOCK_PLING, 1.5f, 0.4f);
    }
}
