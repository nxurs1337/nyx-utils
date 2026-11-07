package dev.nyx.utils.features.gui.casino.slots;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything one spin produced. Wins are expressed in multiples of the total bet.
 */
public class Outcome {
    /** A grid snapshot. Cluster games produce one per tumble. */
    public static class Step {
        public final int[][] grid;
        public final double[][] values;
        public final boolean[][] winning;
        public double win;

        Step(int[][] grid, double[][] values, boolean[][] winning, double win) {
            this.grid = grid;
            this.values = values;
            this.winning = winning;
            this.win = win;
        }
    }

    public final List<Step> steps = new ArrayList<>();
    /** Sum of the multiplier symbols applied to this spin, 0 when none. */
    public int multiplier;
    /** Money collected by wilds during free spins. */
    public double collected;
    public int wildsCollected;
    public double scatterWin;
    public int freeSpinsAwarded;
    public boolean levelUp;
    public double totalWin;
    public boolean capped;

    public Step last() {
        return steps.getLast();
    }
}
