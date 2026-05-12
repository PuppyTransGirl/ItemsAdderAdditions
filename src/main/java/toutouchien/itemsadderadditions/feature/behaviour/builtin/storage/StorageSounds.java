package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import net.kyori.adventure.sound.Sound;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record StorageSounds(@Nullable Sound open, @Nullable Sound close) {}
