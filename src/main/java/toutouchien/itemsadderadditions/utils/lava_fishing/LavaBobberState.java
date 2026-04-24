package toutouchien.itemsadderadditions.utils.lava_fishing;

import org.bukkit.entity.FishHook;

/**
 * Represents a FishHook (Bobber) for a Lava Fishing Hook.
 */
public class LavaBobberState {

    private final FishHook hook;
    private final int minBiteTicks;
    private final int maxBiteTicks;

    private State state = State.FLYING;

    private int biteTimer = -1;
    private int reelWindowTimer = -1;
    private boolean hasCatch = false;

    public LavaBobberState(FishHook hook, int minBiteTicks, int maxBiteTicks) {
        this.hook = hook;
        this.minBiteTicks = minBiteTicks;
        this.maxBiteTicks = maxBiteTicks;
    }

    /**
     * Gets the FishHook Entity currently associated with this LavaBobberState.
     * @return The FishHook entity of this LavaBobberState.
     */
    public FishHook getHook() {
        return hook;
    }

    /**
     * Gets the min number of ticks it takes for a "fish" to appear and approach the Hook.
     * @return Minimum number of ticks it takes for a "fish" to appear.
     */
    public int getMinBiteTicks() {
        return minBiteTicks;
    }

    /**
     * Gets the max number of ticks it takes for a "fish" to appear and approach the Hook.
     * @return Maximum number of ticks it takes for a "fish" to appear.
     */
    public int getMaxBiteTicks() {
        return maxBiteTicks;
    }

    /**
     * Gets the current {@link State state} the FishHook is currently in.
     * @return Current FishHook state.
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the current number of ticks until a "fish" bites.
     * @return Number of ticks until a "fish" bites.
     */
    public int getBiteTimer() {
        return biteTimer;
    }

    /**
     * Gets the number of ticks the player has left to catch a "fish".
     * @return Number of ticks left to catch a "fish".
     */
    public int getReelWindowTimer() {
        return reelWindowTimer;
    }

    /**
     * Returns whether the FishHook has caught something.
     * @return Whether the FishHook has caught something.
     */
    public boolean hasCatch(){
        return hasCatch;
    }

    /**
     * Sets the new {@link State state} for the FishHook.
     * @param state New State for the FishHook to have.
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Sets the number of ticks for the "fish" to bite.
     * @param biteTimer Number of ticks for the "fish" to bite.
     */
    public void setBiteTimer(int biteTimer) {
        this.biteTimer = biteTimer;
    }

    /**
     * Sets the number of ticks the player has to catch a "fish" when it bites.
     * @param reelWindowTimer Number of ticks for catching something.
     */
    public void setReelWindowTimer(int reelWindowTimer) {
        this.reelWindowTimer = reelWindowTimer;
    }

    /**
     * Sets whether this FishHook has caught something.
     * @param hasCatch Whether this FishHook caught something.
     */
    public void setHasCatch(boolean hasCatch) {
        this.hasCatch = hasCatch;
    }

    /**
     * Decreases the Bite Timer by 1 unless it's less than 0.
     */
    public void decreaseBiteTimer() {
        if (biteTimer > 0) {
            biteTimer--;
        }
    }

    /**
     * Decreases the Reel Window Timer by 1 unless it's less than 0.
     */
    public void decreaseReelWindowTimer() {
        if (reelWindowTimer > 0) {
            reelWindowTimer--;
        }
    }

    /**
     * Represents the different states this FishHook can be in.
     */
    public enum State {
        /**
         * The Fishing bobber has been cast.
         */
        FLYING,
        /**
         * The Fishing bobber is in Lava.
         */
        IN_LAVA,
        /**
         * A "fish" starts to aproach the Fishing bobber.
         */
        NIBBLE,
        /**
         * A "fish" has been catched by the Fishing bobber.
         */
        BITE,
        /**
         * Player didn't catch in time and the "fish" escaped.
         */
        EXPIRED
    }
}
