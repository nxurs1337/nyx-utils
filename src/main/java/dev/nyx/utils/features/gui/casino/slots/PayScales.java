package dev.nyx.utils.features.gui.casino.slots;

import java.util.Map;

/**
 * Per-machine payout scaling, measured with {@link SlotGame#simulate} so every machine pays back about 96%.
 */
final class PayScales {
    // measured over 1,000,000 simulated spins each (free spins included)
    private static final Map<String, Double> SCALES = Map.ofEntries(
            Map.entry("candy_cascade", 3.1187),
            Map.entry("storm_peak", 1.3825),
            Map.entry("star_sorceress", 1.3825),
            Map.entry("ore_rush", 1.4207),
            Map.entry("fruit_frenzy", 2.5949),
            Map.entry("reel_catch", 1.0137),
            Map.entry("deep_freeze", 0.8683),
            Map.entry("gold_rush", 1.0137),
            Map.entry("lucky_sevens", 1.0947),
            Map.entry("neon_nights", 1.0348),
            Map.entry("desert_tomb", 1.3517),
            Map.entry("wild_west", 1.0348),
            Map.entry("dragons_hoard", 5.1522),
            Map.entry("jungle_ways", 5.1522),
            Map.entry("nether_inferno", 4.9291)
    );

    // bonus buy prices in total bets: the measured average feature value / 0.96 (100,000 buys each)
    private static final Map<String, Integer> BUY_COSTS = Map.ofEntries(
            Map.entry("candy_cascade", 52),
            Map.entry("storm_peak", 21),
            Map.entry("star_sorceress", 21),
            Map.entry("ore_rush", 16),
            Map.entry("fruit_frenzy", 71),
            Map.entry("reel_catch", 76),
            Map.entry("deep_freeze", 99),
            Map.entry("gold_rush", 76),
            Map.entry("neon_nights", 21),
            Map.entry("desert_tomb", 35),
            Map.entry("wild_west", 21),
            Map.entry("dragons_hoard", 30),
            Map.entry("jungle_ways", 30),
            Map.entry("nether_inferno", 37)
    );

    private PayScales() {
    }

    static double of(String id) {
        return SCALES.getOrDefault(id, 1.0);
    }

    static int buyCost(String id) {
        return BUY_COSTS.getOrDefault(id, 0);
    }
}
