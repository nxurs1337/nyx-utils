package dev.nyx.utils.features.gui.casino;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Where the lounge's chips live. Today this is {@link LocalWallet}, a play-money balance on this
 * computer. A server backed wallet (shared balances, transfers between players) can replace it
 * later through {@link #set(Wallet)} without touching the machines.
 */
public abstract class Wallet {
    private static Wallet active;
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    public static Wallet get() {
        // resolved lazily: LocalWallet extends Wallet, so a static default here could capture null
        return active != null ? active : LocalWallet.getInstance();
    }

    public static void set(Wallet wallet) {
        active = wallet;
    }

    public abstract long balance();

    /** Takes chips for a bet. @return false when the balance is too low. */
    public abstract boolean withdraw(long amount, String reason);

    public abstract void deposit(long amount, String reason);

    /** Milliseconds until free chips can be claimed again, 0 when claimable. */
    public abstract long bonusCooldown();

    /** Hands out free chips. @return the amount given, 0 when still cooling down. */
    public abstract long claimBonus();

    public String currency() {
        return "NC";
    }

    public String format(long amount) {
        return FORMAT.format(amount) + " " + currency();
    }
}
