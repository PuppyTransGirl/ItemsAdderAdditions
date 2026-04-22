package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.entity.Player;

/**
 * NMS operations required by {@code BedBehaviour}.
 *
 * <p>Two sleep modes are supported:
 * <ul>
 *   <li><b>Functional</b> - vanilla path ({@link Player#sleep}) handles everything;
 *       this interface only provides {@link #forceWake} so the behaviour can kick a
 *       player out when the furniture is broken or the plugin unloads.</li>
 *   <li><b>Decorative</b> - the player is put into the {@code SLEEPING} entity pose
 *       via direct entity-data manipulation without entering the vanilla sleep state
 *       machine.  {@link #startDecorativeSleep} and {@link #stopDecorativeSleep}
 *       handle the full lifecycle.</li>
 * </ul>
 */
public interface INmsBedHandler {
    /**
     * Puts the player into the visual {@code SLEEPING} pose at the given block
     * coordinates without going through vanilla sleep logic.
     *
     * <p>Concretely this means:
     * <ol>
     *   <li>Sets the player's synched sleeping-position entity data to
     *       {@code (x, y, z)} so the client renders the lying-down anchor.</li>
     *   <li>Overrides the entity pose to {@code Pose.SLEEPING}.</li>
     *   <li>Broadcasts an {@code ClientboundSetEntityDataPacket} to all players
     *       tracking this entity (including the sleeping player themselves).</li>
     * </ol>
     *
     * <p>The caller is responsible for:
     * <ul>
     *   <li>calling {@link Player#setSleepingIgnored(boolean) setSleepingIgnored(true)}
     *       so the player is excluded from the "all asleep → skip night" count;</li>
     *   <li>teleporting the player to the correct lying position beforehand.</li>
     * </ul>
     *
     * @param player the player to put to sleep
     * @param x      block X of the bed / furniture
     * @param y      block Y of the bed / furniture
     * @param z      block Z of the bed / furniture
     */
    void startDecorativeSleep(Player player, int x, int y, int z);

    /**
     * Removes the decorative {@code SLEEPING} pose and clears the sleeping-position
     * entity data, then broadcasts the update.
     *
     * <p>The caller is responsible for calling
     * {@link Player#setSleepingIgnored(boolean) setSleepingIgnored(false)} afterwards
     * (it is deliberately left to the caller to keep this interface thin and testable).
     *
     * @param player the decoratively sleeping player to wake
     */
    void stopDecorativeSleep(Player player);

    /**
     * Force-wakes a player who is sleeping via the <em>functional</em> (vanilla) path.
     *
     * <p>Equivalent to the server calling {@code player.stopSleepInBed(true, false)},
     * which fires {@link org.bukkit.event.player.PlayerBedLeaveEvent} and clears all
     * vanilla sleep state.  Use this only when the temp-bed block has already been (or
     * is about to be) removed, so the vanilla wakeup does not try to interact with the
     * block afterwards.
     *
     * @param player the functionally sleeping player to wake
     */
    void forceWake(Player player);
}
