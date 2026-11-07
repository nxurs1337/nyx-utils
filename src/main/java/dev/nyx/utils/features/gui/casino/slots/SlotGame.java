package dev.nyx.utils.features.gui.casino.slots;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A slot machine: its symbols, its rules and the maths that turns a random draw into a win.
 * Plain Java on purpose, so the maths can be simulated outside the game.
 */
public class SlotGame {
    public enum Mechanic {
        /** Pay anywhere with 8+ matching symbols, winners tumble away and new symbols fall in. */
        TUMBLE,
        /** Classic paylines, left to right. */
        LINES,
        /** 243 ways: matching symbols on adjacent reels from the left, any row. */
        WAYS,
        /** Paylines, plus money symbols that wild collectors grab during free spins. */
        COLLECT
    }

    public static final double MAX_WIN = 5000;
    private static final int[] COLLECT_LEVELS = {1, 2, 3, 10};

    public final String id;
    public final String title;
    public final String tagline;
    public final Mechanic mechanic;
    public final int cols;
    public final int rows;
    public final SlotTheme theme;
    public final List<SlotSymbol> symbols = new ArrayList<>();

    public int[][] lines = {};
    public int waysDivisor = 25;
    public int freeSpins = 10;
    public double freeSpinWinMultiplier = 1;
    public boolean multipliersInBase;
    public boolean accumulate;
    public double[] multiplierValues = {2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 50, 100};
    public int[] multiplierWeights = {300, 220, 150, 110, 80, 60, 45, 30, 20, 12, 8, 3, 1};
    public double[] moneyValues = {2, 3, 4, 5, 10, 15, 20, 25, 50};
    public int[] moneyWeights = {300, 220, 160, 120, 70, 40, 25, 15, 4};
    /** Scales every payout so the machine returns roughly 96% (tuned by simulation). */
    public double payScale = 1;
    /** Price of a bonus buy in total bets, 0 when the machine has none (tuned by simulation). */
    public int buyCost;

    public SlotGame(String id, String title, String tagline, Mechanic mechanic, int cols, int rows, SlotTheme theme) {
        this.id = id;
        this.title = title;
        this.tagline = tagline;
        this.mechanic = mechanic;
        this.cols = cols;
        this.rows = rows;
        this.theme = theme;
    }

    public SlotGame add(SlotSymbol symbol) {
        symbols.add(symbol);
        return this;
    }

    public int find(SlotSymbol.Kind kind) {
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).kind() == kind) return i;
        }
        return -1;
    }

    public int scatterIndex() {
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).isScatter()) return i;
        }
        return -1;
    }

    public int scatterTrigger() {
        return mechanic == Mechanic.TUMBLE ? 4 : 3;
    }

    public int collectMultiplier(int level) {
        return COLLECT_LEVELS[Math.min(level, COLLECT_LEVELS.length) - 1];
    }

    // ------------------------------------------------------------------ draws

    private boolean allowed(SlotSymbol symbol, boolean free) {
        if (symbol.kind() == SlotSymbol.Kind.MULTIPLIER && !free && !multipliersInBase) return false;
        return !(symbol.kind() == SlotSymbol.Kind.WILD && mechanic == Mechanic.COLLECT && !free);
    }

    private int draw(Random random, boolean free, boolean allowScatter) {
        int total = 0;
        for (SlotSymbol symbol : symbols) {
            if (allowed(symbol, free) && (allowScatter || !symbol.isScatter())) total += symbol.weight();
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < symbols.size(); i++) {
            SlotSymbol symbol = symbols.get(i);
            if (!allowed(symbol, free) || (!allowScatter && symbol.isScatter())) continue;
            roll -= symbol.weight();
            if (roll < 0) return i;
        }
        return 0;
    }

    private static double weighted(Random random, double[] values, int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        int roll = random.nextInt(total);
        for (int i = 0; i < values.length; i++) {
            roll -= weights[i];
            if (roll < 0) return values[i];
        }
        return values[0];
    }

    private void place(Random random, int[][] grid, double[][] values, int c, int r, boolean free, boolean allowScatter) {
        int s = draw(random, free, allowScatter);
        grid[c][r] = s;
        SlotSymbol.Kind kind = symbols.get(s).kind();
        values[c][r] = kind == SlotSymbol.Kind.MULTIPLIER ? weighted(random, multiplierValues, multiplierWeights)
                : kind == SlotSymbol.Kind.MONEY ? weighted(random, moneyValues, moneyWeights) : 0;
    }

    private void fill(Random random, int[][] grid, double[][] values, boolean free) {
        boolean oneScatterPerReel = mechanic != Mechanic.TUMBLE;
        for (int c = 0; c < cols; c++) {
            boolean scatterOnReel = false;
            for (int r = 0; r < rows; r++) {
                place(random, grid, values, c, r, free, !(oneScatterPerReel && scatterOnReel));
                if (symbols.get(grid[c][r]).isScatter()) scatterOnReel = true;
            }
        }
    }

    private void forceScatters(Random random, int[][] grid, double[][] values) {
        int scatter = scatterIndex();
        // clear any natural scatters, then drop exactly the trigger amount on distinct reels
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                while (grid[c][r] == scatter) place(random, grid, values, c, r, false, false);
            }
        }
        List<Integer> reels = new ArrayList<>();
        for (int c = 0; c < cols; c++) reels.add(c);
        java.util.Collections.shuffle(reels, random);
        for (int i = 0; i < scatterTrigger(); i++) {
            int c = reels.get(i % cols);
            int r = random.nextInt(rows);
            while (grid[c][r] == scatter) r = (r + 1) % rows;
            grid[c][r] = scatter;
            values[c][r] = 0;
        }
    }

    private static int[][] copy(int[][] grid) {
        int[][] out = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) out[i] = grid[i].clone();
        return out;
    }

    private static double[][] copy(double[][] grid) {
        double[][] out = new double[grid.length][];
        for (int i = 0; i < grid.length; i++) out[i] = grid[i].clone();
        return out;
    }

    // ------------------------------------------------------------------- spin

    /** Plays one spin. Pass the running feature during free spins, null in the base game. */
    public Outcome spin(Random random, Feature feature) {
        return spin(random, feature, false);
    }

    /** Whether this machine has free spins that can be bought. */
    public boolean canBuyBonus() {
        return scatterIndex() >= 0 && buyCost > 0;
    }

    /** A bonus buy: a base spin that is guaranteed to land exactly enough scatters to trigger. */
    public Outcome buySpin(Random random) {
        return spin(random, null, true);
    }

    private Outcome spin(Random random, Feature feature, boolean forceTrigger) {
        boolean free = feature != null;
        Outcome outcome = new Outcome();
        int[][] grid = new int[cols][rows];
        double[][] values = new double[cols][rows];
        fill(random, grid, values, free);
        if (forceTrigger) forceScatters(random, grid, values);

        double raw = switch (mechanic) {
            case TUMBLE -> tumble(random, grid, values, feature, outcome);
            case LINES, COLLECT -> lines(grid, values, feature, outcome);
            case WAYS -> ways(grid, values, feature, outcome);
        };

        // scatters pay anywhere and start (or extend) the free spins
        int scatter = scatterIndex();
        if (scatter >= 0) {
            Outcome.Step last = outcome.last();
            int count = 0;
            for (int c = 0; c < cols; c++)
                for (int r = 0; r < rows; r++)
                    if (last.grid[c][r] == scatter) count++;
            if (count >= scatterTrigger() || (free && count >= 3)) {
                outcome.freeSpinsAwarded += free ? (mechanic == Mechanic.TUMBLE ? 5 : freeSpins) : freeSpins;
                outcome.scatterWin = symbols.get(scatter).pay(Math.min(count, symbols.get(scatter).pays().length - 1));
                for (int c = 0; c < cols; c++)
                    for (int r = 0; r < rows; r++)
                        if (last.grid[c][r] == scatter) last.winning[c][r] = true;
                raw += outcome.scatterWin;
            }
        }

        outcome.scatterWin *= payScale;
        for (Outcome.Step step : outcome.steps) step.win *= payScale;
        outcome.collected *= payScale;
        outcome.totalWin = raw * payScale;
        if (outcome.totalWin > MAX_WIN) {
            outcome.totalWin = MAX_WIN;
            outcome.capped = true;
        }
        return outcome;
    }

    private double tumble(Random random, int[][] grid, double[][] values, Feature feature, Outcome outcome) {
        boolean free = feature != null;
        double total = 0;
        while (true) {
            int[] counts = new int[symbols.size()];
            for (int c = 0; c < cols; c++)
                for (int r = 0; r < rows; r++)
                    counts[grid[c][r]]++;
            boolean[][] winning = new boolean[cols][rows];
            double win = 0;
            for (int s = 0; s < symbols.size(); s++) {
                SlotSymbol symbol = symbols.get(s);
                if (symbol.kind() != SlotSymbol.Kind.NORMAL || counts[s] < 8) continue;
                win += symbol.pay(counts[s] >= 12 ? 2 : counts[s] >= 10 ? 1 : 0);
                for (int c = 0; c < cols; c++)
                    for (int r = 0; r < rows; r++)
                        if (grid[c][r] == s) winning[c][r] = true;
            }
            outcome.steps.add(new Outcome.Step(copy(grid), copy(values), winning, win));
            if (win == 0) break;
            total += win;
            // winners vanish, everything above falls down, new symbols drop in from the top
            for (int c = 0; c < cols; c++) {
                int[] column = new int[rows];
                double[] columnValues = new double[rows];
                int write = rows - 1;
                for (int r = rows - 1; r >= 0; r--) {
                    if (winning[c][r]) continue;
                    column[write] = grid[c][r];
                    columnValues[write] = values[c][r];
                    write--;
                }
                grid[c] = column;
                values[c] = columnValues;
                for (int r = write; r >= 0; r--) place(random, grid, values, c, r, free, true);
            }
        }

        int multiplier = 0;
        int mult = find(SlotSymbol.Kind.MULTIPLIER);
        if (mult >= 0) {
            for (int c = 0; c < cols; c++)
                for (int r = 0; r < rows; r++)
                    if (grid[c][r] == mult) multiplier += (int) values[c][r];
        }
        if (total > 0 && multiplier > 0) {
            if (accumulate && free) {
                feature.accumulated += multiplier;
                multiplier = feature.accumulated;
            }
            outcome.multiplier = multiplier;
            total *= multiplier;
        }
        return total;
    }

    private double lineWin(int[][] grid, int[] line, boolean[][] winning) {
        int target = -1, count = 0, leadingWilds = 0;
        boolean onlyWilds = true;
        for (int c = 0; c < cols; c++) {
            SlotSymbol symbol = symbols.get(grid[c][line[c]]);
            if (symbol.isWild()) {
                count++;
                if (onlyWilds) leadingWilds++;
                continue;
            }
            if (symbol.kind() != SlotSymbol.Kind.NORMAL && symbol.kind() != SlotSymbol.Kind.MONEY) break;
            onlyWilds = false;
            if (target == -1) {
                target = grid[c][line[c]];
                count++;
            } else if (grid[c][line[c]] == target) {
                count++;
            } else {
                break;
            }
        }
        double symbolPay = target >= 0 ? symbols.get(target).pay(count) : 0;
        int wild = find(SlotSymbol.Kind.WILD);
        double wildPay = wild >= 0 ? symbols.get(wild).pay(leadingWilds) : 0;
        double pay = Math.max(symbolPay, wildPay);
        int length = symbolPay >= wildPay ? count : leadingWilds;
        if (pay > 0) {
            for (int c = 0; c < length; c++) winning[c][line[c]] = true;
        }
        return pay;
    }

    private double lines(int[][] grid, double[][] values, Feature feature, Outcome outcome) {
        boolean free = feature != null;
        boolean[][] winning = new boolean[cols][rows];
        double lineBet = 1.0 / lines.length;
        double total = 0;
        for (int[] line : lines) total += lineWin(grid, line, winning) * lineBet;
        if (free) total *= freeSpinWinMultiplier;

        if (mechanic == Mechanic.COLLECT && free) {
            int wild = find(SlotSymbol.Kind.WILD), money = find(SlotSymbol.Kind.MONEY);
            int wilds = 0;
            double cash = 0;
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    if (grid[c][r] == wild) wilds++;
                    if (grid[c][r] == money) cash += values[c][r];
                }
            }
            if (wilds > 0 && cash > 0) {
                outcome.collected = cash * wilds * collectMultiplier(feature.level);
                outcome.wildsCollected = wilds;
                total += outcome.collected;
                for (int c = 0; c < cols; c++)
                    for (int r = 0; r < rows; r++)
                        if (grid[c][r] == wild || grid[c][r] == money) winning[c][r] = true;
            }
            if (wilds > 0) {
                int before = feature.wilds / 4;
                feature.wilds += wilds;
                int levels = Math.min(feature.wilds / 4, 3) - Math.min(before, 3);
                if (levels > 0) {
                    feature.level += levels;
                    outcome.freeSpinsAwarded += 10 * levels;
                    outcome.levelUp = true;
                }
            }
        }
        Outcome.Step step = new Outcome.Step(copy(grid), copy(values), winning, total);
        outcome.steps.add(step);
        return total;
    }

    private double ways(int[][] grid, double[][] values, Feature feature, Outcome outcome) {
        boolean[][] winning = new boolean[cols][rows];
        double unit = 1.0 / waysDivisor;
        double total = 0;
        for (int s = 0; s < symbols.size(); s++) {
            SlotSymbol symbol = symbols.get(s);
            if (symbol.kind() != SlotSymbol.Kind.NORMAL) continue;
            int reels = 0;
            long ways = 1;
            for (int c = 0; c < cols; c++) {
                int hits = 0;
                for (int r = 0; r < rows; r++) {
                    if (grid[c][r] == s || symbols.get(grid[c][r]).isWild()) hits++;
                }
                if (hits == 0) break;
                reels++;
                ways *= hits;
            }
            double pay = symbol.pay(reels);
            if (reels >= 3 && pay > 0) {
                total += pay * ways * unit;
                for (int c = 0; c < reels; c++)
                    for (int r = 0; r < rows; r++)
                        if (grid[c][r] == s || symbols.get(grid[c][r]).isWild()) winning[c][r] = true;
            }
        }
        if (feature != null) total *= freeSpinWinMultiplier;
        outcome.steps.add(new Outcome.Step(copy(grid), copy(values), winning, total));
        return total;
    }

    // ------------------------------------------------------------- simulation

    /** Average return of one bonus buy (trigger spin plus the free spins) in total bets. */
    public double simulateBuy(long buys, long seed) {
        Random random = new Random(seed);
        double paid = 0;
        for (long i = 0; i < buys; i++) {
            Outcome outcome = buySpin(random);
            Feature feature = new Feature(outcome.freeSpinsAwarded);
            while (feature.spinsLeft > 0 && feature.spinsPlayed < 200) {
                feature.spinsLeft--;
                feature.spinsPlayed++;
                Outcome free = spin(random, feature);
                feature.totalWin += free.totalWin;
                feature.spinsLeft += free.freeSpinsAwarded;
            }
            paid += Math.min(MAX_WIN, outcome.totalWin + feature.totalWin);
        }
        return paid / buys;
    }

    /** Return to player over {@code spins} base spins, free spins included. */
    public double simulate(long spins, long seed) {
        Random random = new Random(seed);
        double paid = 0;
        for (long i = 0; i < spins; i++) {
            Outcome outcome = spin(random, null);
            double win = outcome.totalWin;
            if (outcome.freeSpinsAwarded > 0) {
                Feature feature = new Feature(outcome.freeSpinsAwarded);
                while (feature.spinsLeft > 0 && feature.spinsPlayed < 200) {
                    feature.spinsLeft--;
                    feature.spinsPlayed++;
                    Outcome free = spin(random, feature);
                    feature.totalWin += free.totalWin;
                    feature.spinsLeft += free.freeSpinsAwarded;
                }
                win += feature.totalWin;
            }
            paid += Math.min(MAX_WIN, win);
        }
        return paid / spins;
    }
}
