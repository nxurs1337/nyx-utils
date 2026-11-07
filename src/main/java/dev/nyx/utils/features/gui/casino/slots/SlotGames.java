package dev.nyx.utils.features.gui.casino.slots;

import dev.nyx.utils.features.gui.casino.slots.SlotGame.Mechanic;
import dev.nyx.utils.features.gui.casino.slots.SlotSymbol.Kind;
import dev.nyx.utils.features.gui.casino.slots.SlotTheme.Pattern;

import java.util.List;

import static dev.nyx.utils.features.gui.casino.slots.SlotSymbol.*;

/**
 * Every machine in the lounge. Symbols are listed from lowest to highest paying.
 * payScale values come from {@link SlotGame#simulate} runs and keep each machine near 96%.
 */
public final class SlotGames {
    private static final int[][] LINES_5 = {
            {1, 1, 1}, {0, 0, 0}, {2, 2, 2}, {0, 1, 2}, {2, 1, 0}
    };
    private static final int[][] LINES_10 = {
            {1, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {2, 2, 2, 2, 2}, {0, 1, 2, 1, 0}, {2, 1, 0, 1, 2},
            {0, 0, 1, 2, 2}, {2, 2, 1, 0, 0}, {1, 0, 0, 0, 1}, {1, 2, 2, 2, 1}, {1, 0, 1, 2, 1}
    };
    private static final int[][] LINES_20 = {
            {1, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {2, 2, 2, 2, 2}, {0, 1, 2, 1, 0}, {2, 1, 0, 1, 2},
            {0, 0, 1, 2, 2}, {2, 2, 1, 0, 0}, {1, 0, 0, 0, 1}, {1, 2, 2, 2, 1}, {1, 0, 1, 2, 1},
            {1, 2, 1, 0, 1}, {0, 1, 0, 1, 0}, {2, 1, 2, 1, 2}, {0, 1, 1, 1, 0}, {2, 1, 1, 1, 2},
            {1, 1, 0, 1, 1}, {1, 1, 2, 1, 1}, {0, 0, 2, 0, 0}, {2, 2, 0, 2, 2}, {0, 2, 2, 2, 0}
    };

    // cluster pays for [8-9, 10-11, 12+] in total bets, lowest to highest symbol
    private static final double[][] CLUSTER = {
            {0.25, 0.75, 2}, {0.4, 0.9, 4}, {0.5, 1, 5}, {0.8, 1.2, 8},
            {1, 1.5, 10}, {1.5, 2, 12}, {2, 5, 15}, {2.5, 10, 25}, {10, 25, 50}
    };
    private static final int[] CLUSTER_WEIGHTS = {20, 19, 18, 17, 15, 13, 11, 9, 7};

    // line pays by match count in line bets
    private static final double[][] LINE_PAYS = {
            {0, 0, 0, 5, 15, 50}, {0, 0, 0, 5, 20, 75}, {0, 0, 0, 10, 25, 100}, {0, 0, 0, 10, 30, 125},
            {0, 0, 0, 15, 50, 200}, {0, 0, 0, 20, 75, 300}, {0, 0, 2, 30, 100, 500}, {0, 0, 5, 50, 250, 1000}
    };
    private static final int[] LINE_WEIGHTS = {30, 28, 26, 24, 18, 14, 10, 7};

    // ways pays by reel count in units of bet / 25
    private static final double[][] WAYS_PAYS = {
            {0, 0, 0, 0.5, 1, 4}, {0, 0, 0, 0.5, 1.5, 5}, {0, 0, 0, 1, 2, 6}, {0, 0, 0, 1, 3, 8},
            {0, 0, 0, 2, 5, 15}, {0, 0, 0, 3, 8, 25}, {0, 0, 0, 5, 15, 50}
    };
    private static final int[] WAYS_WEIGHTS = {26, 24, 22, 20, 16, 12, 9};

    private static final double[] TUMBLE_SCATTER = {0, 0, 0, 0, 3, 5, 100};
    private static final double[] LINE_SCATTER = {0, 0, 0, 2, 10, 50};

    private static List<SlotGame> games;

    private SlotGames() {
    }

    public static List<SlotGame> all() {
        if (games == null) games = build();
        return games;
    }

    private static SlotGame tumble(String id, String title, String tagline, SlotTheme theme, String[] items,
                                   String scatter, String multiplier, boolean inBase, boolean accumulate) {
        SlotGame game = new SlotGame(id, title, tagline, Mechanic.TUMBLE, 6, 5, theme);
        for (int i = 0; i < items.length; i++) game.add(normal(items[i], CLUSTER_WEIGHTS[i], CLUSTER[i]));
        game.add(special(scatter, Kind.SCATTER, 3, TUMBLE_SCATTER));
        if (multiplier != null) game.add(special(multiplier, Kind.MULTIPLIER, inBase ? 2 : 5));
        game.multipliersInBase = inBase;
        game.accumulate = accumulate;
        return game;
    }

    private static SlotGame lines(String id, String title, String tagline, SlotTheme theme, int[][] lines,
                                  String[] items, String wild, String scatter, double freeMultiplier) {
        SlotGame game = new SlotGame(id, title, tagline, Mechanic.LINES, 5, 3, theme);
        int offset = LINE_PAYS.length - items.length;
        for (int i = 0; i < items.length; i++) game.add(normal(items[i], LINE_WEIGHTS[offset + i], LINE_PAYS[offset + i]));
        game.add(special(wild, Kind.WILD, 4, 0, 0, 10, 100, 500, 2000));
        if (scatter != null) game.add(special(scatter, Kind.SCATTER, 4, LINE_SCATTER));
        game.lines = lines;
        game.freeSpinWinMultiplier = freeMultiplier;
        return game;
    }

    private static SlotGame ways(String id, String title, String tagline, SlotTheme theme, String[] items,
                                 String wild, String scatter, double freeMultiplier) {
        SlotGame game = new SlotGame(id, title, tagline, Mechanic.WAYS, 5, 3, theme);
        for (int i = 0; i < items.length; i++) game.add(normal(items[i], WAYS_WEIGHTS[i], WAYS_PAYS[i]));
        game.add(special(wild, Kind.WILD, 5));
        game.add(special(scatter, Kind.SCATTER, 4, LINE_SCATTER));
        game.freeSpinWinMultiplier = freeMultiplier;
        return game;
    }

    private static SlotGame collect(String id, String title, String tagline, SlotTheme theme, String[] items,
                                    String money, String wild, String scatter, double[] moneyValues, int[] moneyWeights) {
        SlotGame game = new SlotGame(id, title, tagline, Mechanic.COLLECT, 5, 3, theme);
        int offset = LINE_PAYS.length - items.length;
        for (int i = 0; i < items.length; i++) game.add(normal(items[i], LINE_WEIGHTS[offset + i], LINE_PAYS[offset + i]));
        game.add(special(money, Kind.MONEY, 10, 0, 0, 0, 10, 50, 200));
        // the angler only shows up in free spins, and rarely: every catch is a moment
        game.add(special(wild, Kind.WILD, 2, 0, 0, 10, 100, 500, 2000));
        game.add(special(scatter, Kind.SCATTER, 3, LINE_SCATTER));
        game.lines = LINES_10;
        game.moneyValues = moneyValues;
        game.moneyWeights = moneyWeights;
        return game;
    }

    private static List<SlotGame> build() {
        SlotGame candy = tumble("candy_cascade", "CANDY CASCADE", "Tumbling treats, TNT multipliers in free spins",
                new SlotTheme(0xFFFF8AD8, 0xFF8A2BE2, 0xFFFF4FB0, 0xFFFFE0F4, 0xE0FFF0FA, Pattern.BUBBLES),
                new String[]{"sweet_berries", "glow_berries", "apple", "melon_slice", "cookie", "pumpkin_pie", "cake", "golden_apple", "enchanted_golden_apple"},
                "nether_star", "tnt", false, false);

        SlotGame storm = tumble("storm_peak", "STORM PEAK", "Lightning orbs strike on any spin and stack in free spins",
                new SlotTheme(0xFF1A2A6C, 0xFF0A0A2A, 0xFFE0B040, 0xFFFFE08A, 0xE0101436, Pattern.RAYS),
                new String[]{"amethyst_shard", "lapis_lazuli", "emerald", "redstone", "prismarine_crystals", "gold_ingot", "diamond", "trident", "golden_helmet"},
                "lightning_rod", "end_crystal", true, true);

        SlotGame star = tumble("star_sorceress", "STAR SORCERESS", "A moonlit tumble with ever-growing star multipliers",
                new SlotTheme(0xFF5A2A9A, 0xFF1A0A3A, 0xFFFF9AE0, 0xFFE0C0FF, 0xE0200C40, Pattern.STARS),
                new String[]{"glow_ink_sac", "prismarine_shard", "echo_shard", "ender_pearl", "ender_eye", "firework_star", "heart_of_the_sea", "totem_of_undying", "nether_star"},
                "beacon", "firework_rocket", true, true);

        SlotGame ore = tumble("ore_rush", "ORE RUSH", "Dig deep: ores tumble and TNT blows up your wins",
                new SlotTheme(0xFF4A4A52, 0xFF16161A, 0xFF9AA0A8, 0xFF4BFF6B, 0xE0202024, Pattern.DIAMONDS),
                new String[]{"coal", "raw_copper", "raw_iron", "lapis_lazuli", "redstone", "raw_gold", "emerald", "diamond", "netherite_ingot"},
                "diamond_pickaxe", "tnt", true, false);

        SlotGame fruit = tumble("fruit_frenzy", "FRUIT FRENZY", "Farm fresh tumbles, bell multipliers keep climbing",
                new SlotTheme(0xFF6ACF3A, 0xFF1E6A1A, 0xFFFFC23A, 0xFFFFF4B0, 0xE0F4FFE8, Pattern.SPARKS),
                new String[]{"carrot", "beetroot", "potato", "apple", "melon_slice", "glow_berries", "golden_carrot", "golden_apple", "enchanted_golden_apple"},
                "honey_bottle", "bell", false, true);

        double[] fishValues = {2, 3, 4, 5, 10, 15, 20, 25, 50};
        int[] fishWeights = {300, 220, 160, 120, 70, 40, 25, 15, 4};
        SlotGame reel = collect("reel_catch", "REEL CATCH", "Land the boat, then your angler reels in every fish",
                new SlotTheme(0xFF2AB0E0, 0xFF0A3A6A, 0xFFFFD040, 0xFFE0FAFF, 0xE0E8F8FF, Pattern.WAVES),
                new String[]{"kelp", "lily_pad", "bucket", "fishing_rod", "pufferfish", "salmon"},
                "tropical_fish", "cod_bucket", "oak_boat", fishValues, fishWeights);

        SlotGame freeze = collect("deep_freeze", "DEEP FREEZE", "Ice fishing with bigger fish and colder nerves",
                new SlotTheme(0xFFB0E8FF, 0xFF2A5A9A, 0xFFFFFFFF, 0xFF8AE0FF, 0xE0E0F4FF, Pattern.SNOW),
                new String[]{"snowball", "ice", "packed_ice", "blue_ice", "cod", "pufferfish"},
                "salmon", "salmon_bucket", "spruce_boat",
                new double[]{2, 5, 10, 15, 20, 25, 50, 100, 250}, new int[]{320, 220, 120, 80, 50, 30, 12, 4, 1});

        SlotGame gold = collect("gold_rush", "GOLD RUSH", "Minecarts haul every nugget in the mine",
                new SlotTheme(0xFF8A5A2A, 0xFF2A1408, 0xFFFFC23A, 0xFFFFE08A, 0xE03A2010, Pattern.STRIPES),
                new String[]{"rail", "powered_rail", "torch", "lantern", "iron_pickaxe", "chest_minecart"},
                "gold_nugget", "minecart", "tnt_minecart", fishValues, fishWeights);

        SlotGame sevens = new SlotGame("lucky_sevens", "LUCKY SEVENS", "Three reels, five lines, pure old-school",
                Mechanic.LINES, 3, 3,
                new SlotTheme(0xFFC81E1E, 0xFF3A0606, 0xFFFFD040, 0xFFFFF0A0, 0xE0FFF8E8, Pattern.RAYS));
        sevens.add(normal("sweet_berries", 30, 0, 0, 0, 5))
                .add(normal("melon_slice", 25, 0, 0, 0, 10))
                .add(normal("gold_ingot", 20, 0, 0, 0, 20))
                .add(normal("bell", 14, 0, 0, 0, 30))
                .add(normal("diamond", 9, 0, 0, 0, 50))
                .add(label("7", "redstone", 6, 0, 0, 0, 100))
                .add(special("nether_star", Kind.WILD, 4, 0, 0, 0, 250));
        sevens.lines = LINES_5;

        SlotGame neon = lines("neon_nights", "NEON NIGHTS", "Twenty glowing lines, double wins in free spins",
                new SlotTheme(0xFF2A0A4A, 0xFF05010C, 0xFF22F3FF, 0xFFFF2BD6, 0xE0100420, Pattern.STRIPES),
                LINES_20, new String[]{"glowstone_dust", "glow_ink_sac", "prismarine_crystals", "glow_berries", "sea_lantern", "shroomlight", "beacon"},
                "end_rod", "jukebox", 2);

        SlotGame tomb = new SlotGame("desert_tomb", "DESERT TOMB", "The enchanted book is wild AND scatter. Triple free spins.",
                Mechanic.LINES, 5, 3,
                new SlotTheme(0xFFE0B060, 0xFF6A3A10, 0xFF3AD0C0, 0xFFFFE8B0, 0xE0F8E8C8, Pattern.RAYS));
        String[] tombItems = {"bone", "arrow", "rabbit_foot", "gold_nugget", "golden_sword", "golden_chestplate", "totem_of_undying", "diamond"};
        for (int i = 0; i < tombItems.length; i++) tomb.add(normal(tombItems[i], LINE_WEIGHTS[i], LINE_PAYS[i]));
        tomb.add(special("enchanted_book", Kind.WILD_SCATTER, 5, 0, 0, 0, 2, 20, 200));
        tomb.lines = LINES_10;
        tomb.freeSpinWinMultiplier = 3;

        SlotGame west = lines("wild_west", "WILD WEST BOUNTY", "Saddle up for twenty lines of frontier loot",
                new SlotTheme(0xFFD0702A, 0xFF4A1A08, 0xFFFFD040, 0xFFFFE0B0, 0xE0F0D8B8, Pattern.RAYS),
                LINES_20, new String[]{"wheat", "leather", "lead", "saddle", "crossbow", "gold_ingot", "emerald"},
                "golden_horse_armor", "target", 2);

        SlotGame dragon = ways("dragons_hoard", "DRAGON'S HOARD", "243 ways to raid the End",
                new SlotTheme(0xFF2A0A3A, 0xFF05000A, 0xFFB04BFF, 0xFFE0A0FF, 0xE0140820, Pattern.STARS),
                new String[]{"gold_nugget", "gold_ingot", "amethyst_shard", "dragon_breath", "end_crystal", "elytra", "dragon_head"},
                "dragon_egg", "ender_eye", 2);

        SlotGame jungle = ways("jungle_ways", "JUNGLE WAYS", "243 ways through the canopy",
                new SlotTheme(0xFF2A8A3A, 0xFF0A2A10, 0xFFFFD040, 0xFFC8FFB0, 0xE0E8FFE0, Pattern.SPARKS),
                new String[]{"cocoa_beans", "bamboo", "vine", "jungle_sapling", "turtle_scute", "turtle_helmet", "emerald"},
                "sniffer_egg", "spyglass", 2);

        SlotGame nether = ways("nether_inferno", "NETHER INFERNO", "243 blazing ways, triple wins in free spins",
                new SlotTheme(0xFFAA2A0A, 0xFF2A0402, 0xFFFF8A1F, 0xFFFFC080, 0xE0200804, Pattern.FLAMES),
                new String[]{"quartz", "nether_wart", "magma_cream", "blaze_powder", "ghast_tear", "blaze_rod", "netherite_ingot"},
                "fire_charge", "respawn_anchor", 3);

        List<SlotGame> list = List.of(candy, storm, star, ore, fruit, reel, freeze, gold, sevens, neon, tomb, west, dragon, jungle, nether);
        for (SlotGame game : list) {
            game.payScale = PayScales.of(game.id);
            game.buyCost = PayScales.buyCost(game.id);
        }
        return list;
    }
}
