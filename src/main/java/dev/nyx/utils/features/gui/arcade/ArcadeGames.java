package dev.nyx.utils.features.gui.arcade;

import dev.nyx.utils.features.gui.arcade.games.*;

import java.util.List;

/**
 * Every machine standing in the game room, in display order.
 */
public final class ArcadeGames {
    private static List<ArcadeGame> games;

    private ArcadeGames() {
    }

    public static List<ArcadeGame> all() {
        if (games == null) {
            games = List.of(
                    new DotDashGame(),
                    new SnakeGame(),
                    new BlockDropGame(),
                    new GateHopperGame(),
                    new MineFieldGame(),
                    new StarDefenderGame(),
                    new BrickBlasterGame()
            );
        }
        return games;
    }
}
