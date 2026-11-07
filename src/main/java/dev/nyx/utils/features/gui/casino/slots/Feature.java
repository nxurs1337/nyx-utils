package dev.nyx.utils.features.gui.casino.slots;

/**
 * State of a running free spins round.
 */
public class Feature {
    public int spinsLeft;
    public int spinsPlayed;
    public double totalWin;
    /** Cluster games with an accumulating multiplier keep adding to this. */
    public int accumulated;
    /** Collect games: wilds collected so far and the current level (1, 2, 3...). */
    public int wilds;
    public int level = 1;

    public Feature(int spins) {
        this.spinsLeft = spins;
    }
}
