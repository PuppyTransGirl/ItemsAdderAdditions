package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class StorageSoundPlayer {
    @Nullable private final Sound openSound;
    @Nullable private final Sound closeSound;

    StorageSoundPlayer(@Nullable Sound openSound, @Nullable Sound closeSound) {
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    private static void play(Location location, @Nullable Sound sound) {
        if (sound != null && location.getWorld() != null) {
            location.getWorld().playSound(sound, location.x(), location.y(), location.z());
        }
    }

    void playOpen(Location location) {
        play(location, openSound);
    }

    void playClose(Location location, boolean enabled) {
        if (enabled) {
            play(location, closeSound);
        }
    }
}
