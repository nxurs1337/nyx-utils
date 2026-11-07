package dev.nyx.utils.features.gui.casino;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nyx.utils.util.traits.Jsonable;

/**
 * Play-money wallet stored in nyxutils/wallet.json. Nyx Coins have no real-world value.
 */
public class LocalWallet extends Wallet implements Jsonable {
    public static final long STARTING_BALANCE = 10_000;
    public static final long BONUS = 2_500;
    public static final long BONUS_COOLDOWN = 15 * 60 * 1000L;

    private static final LocalWallet INSTANCE = new LocalWallet();

    private long balance = STARTING_BALANCE;
    private long lastBonus;
    private boolean ageConfirmed;

    public static LocalWallet getInstance() {
        return INSTANCE;
    }

    @Override
    public long balance() {
        return balance;
    }

    @Override
    public boolean withdraw(long amount, String reason) {
        if (amount < 0 || amount > balance) return false;
        balance -= amount;
        return true;
    }

    @Override
    public void deposit(long amount, String reason) {
        if (amount > 0) balance += amount;
    }

    @Override
    public long bonusCooldown() {
        return Math.max(0, lastBonus + BONUS_COOLDOWN - System.currentTimeMillis());
    }

    @Override
    public long claimBonus() {
        if (bonusCooldown() > 0) return 0;
        lastBonus = System.currentTimeMillis();
        balance += BONUS;
        return BONUS;
    }

    /** Whether the player confirmed they are 18+ before entering the lounge. */
    public boolean isAgeConfirmed() {
        return ageConfirmed;
    }

    public void setAgeConfirmed(boolean ageConfirmed) {
        this.ageConfirmed = ageConfirmed;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("balance", balance);
        object.addProperty("lastBonus", lastBonus);
        object.addProperty("ageConfirmed", ageConfirmed);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        if (object.has("balance")) balance = Math.max(0, object.get("balance").getAsLong());
        if (object.has("lastBonus")) lastBonus = object.get("lastBonus").getAsLong();
        if (object.has("ageConfirmed")) ageConfirmed = object.get("ageConfirmed").getAsBoolean();
    }

    @Override
    public String getFileName() {
        return "wallet.json";
    }
}
