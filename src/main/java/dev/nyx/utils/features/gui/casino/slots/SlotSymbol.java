package dev.nyx.utils.features.gui.casino.slots;

/**
 * One reel symbol. {@code item} is a Minecraft item id used as artwork, {@code label} replaces it
 * with big text when set. Pays are indexed by match count: cluster games use [8-9, 10-11, 12+]
 * (times total bet), line and ways games use the match count itself (times line bet).
 */
public record SlotSymbol(String item, String label, Kind kind, int weight, double[] pays) {
    public enum Kind {NORMAL, WILD, SCATTER, MULTIPLIER, MONEY, WILD_SCATTER}

    public static SlotSymbol normal(String item, int weight, double... pays) {
        return new SlotSymbol(item, null, Kind.NORMAL, weight, pays);
    }

    public static SlotSymbol label(String label, String item, int weight, double... pays) {
        return new SlotSymbol(item, label, Kind.NORMAL, weight, pays);
    }

    public static SlotSymbol special(String item, Kind kind, int weight, double... pays) {
        return new SlotSymbol(item, null, kind, weight, pays);
    }

    public boolean isWild() {
        return kind == Kind.WILD || kind == Kind.WILD_SCATTER;
    }

    public boolean isScatter() {
        return kind == Kind.SCATTER || kind == Kind.WILD_SCATTER;
    }

    public double pay(int index) {
        return index >= 0 && index < pays.length ? pays[index] : 0;
    }
}
