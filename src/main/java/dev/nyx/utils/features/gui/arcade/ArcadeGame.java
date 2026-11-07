package dev.nyx.utils.features.gui.arcade;

import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for an arcade machine game. Games simulate at a fixed 60 updates per second and
 * render into their own low resolution canvas of {@link #getWidth()} x {@link #getHeight()} pixels.
 */
public abstract class ArcadeGame implements Util {
    public enum State {
        ATTRACT, PLAYING, PAUSED, GAME_OVER
    }

    private final String id;
    private final String title;
    private final int accent;
    private final int width;
    private final int height;

    protected final Set<Integer> held = new HashSet<>();
    protected State state = State.ATTRACT;
    protected int score;
    protected boolean won;
    private boolean newHiScore;

    protected ArcadeGame(String id, String title, int accent, int width, int height) {
        this.id = id;
        this.title = title;
        this.accent = accent;
        this.width = width;
        this.height = height;
    }

    /** Resets everything for a fresh run. */
    protected abstract void reset();

    /** One fixed 1/60s simulation step while playing. */
    protected abstract void update();

    /** Renders the game in canvas coordinates. */
    public abstract void render(GuiGraphics g);

    /** Tiny attract-mode artwork for the cabinet screen in the game room. */
    public abstract void renderPreview(GuiGraphics g, int x, int y, int w, int h);

    /** Short lines shown on the machine's instruction card. */
    public abstract String[] controls();

    protected void onKey(int key) {
    }

    protected void onMouse(int x, int y, int button) {
    }

    /** Called by the machine screen at 60Hz. */
    public void step() {
        if (state == State.PLAYING) update();
    }

    public void start() {
        score = 0;
        won = false;
        newHiScore = false;
        held.clear();
        reset();
        state = State.PLAYING;
        ArcadeSound.coin();
    }

    protected void gameOver(boolean won) {
        if (state != State.PLAYING) return;
        this.won = won;
        state = State.GAME_OVER;
        newHiScore = ArcadeScores.getInstance().submit(id, score);
        if (won) ArcadeSound.levelUp();
        else ArcadeSound.gameOver();
    }

    public void keyPressed(int key) {
        held.add(key);
        if (state == State.PLAYING) onKey(key);
    }

    public void keyReleased(int key) {
        held.remove(key);
    }

    public void mouseClicked(int x, int y, int button) {
        if (state == State.PLAYING) onMouse(x, y, button);
    }

    public void togglePause() {
        if (state == State.PLAYING) state = State.PAUSED;
        else if (state == State.PAUSED) state = State.PLAYING;
    }

    protected boolean isHeld(int... keys) {
        for (int key : keys) {
            if (held.contains(key)) return true;
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getAccent() {
        return accent;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public State getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isNewHiScore() {
        return newHiScore;
    }

    public int getHiScore() {
        return Math.max(score, ArcadeScores.getInstance().get(id));
    }

    /** Extra line for the side panel (lives, level...). */
    public String getStatus() {
        return "";
    }

    /** Whether the joystick should animate from the arrow keys. */
    public boolean usesMouse() {
        return false;
    }
}
