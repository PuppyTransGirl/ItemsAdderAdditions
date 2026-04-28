package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.INmsBiomeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public final class NmsBiomeHandler_v1_21_8 implements INmsBiomeHandler {
    /**
     * Cache from Bukkit {@link org.bukkit.block.Biome} -> its NMS {@link Holder}.
     * Populated lazily on first use of each biome.
     */
    private final Map<org.bukkit.block.Biome, Holder<Biome>> holderCache = new HashMap<>();

    private static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    @Override
    public void setBiomes(World world, List<Location> locations, org.bukkit.block.Biome biome) {
        if (locations.isEmpty()) return;

        CraftWorld craftWorld = (CraftWorld) world;
        ServerLevel level = craftWorld.getHandle();

        // Resolve NMS biome holder once for the entire call
        Holder<Biome> holder =
                holderCache.computeIfAbsent(biome, b -> resolveHolder(level, b));

        // Group locations by chunk so we load each LevelChunk exactly once
        Map<Long, ChunkWork> byChunk = new HashMap<>();
        for (Location loc : locations) {
            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            byChunk
                    .computeIfAbsent(chunkKey(chunkX, chunkZ), k -> new ChunkWork(chunkX, chunkZ))
                    .locations.add(loc);
        }

        // One chunk fetch -> write all quanta -> mark dirty -> refresh
        for (ChunkWork work : byChunk.values()) {
            LevelChunk chunk = level.getChunk(work.chunkX, work.chunkZ);

            for (Location loc : work.locations) {
                int blockY = loc.getBlockY();

                // Find the section this Y belongs to
                int sectionIndex = chunk.getSectionIndex(blockY);
                if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
                    Log.debug("NmsBiomeHandler", "  Skipping out-of-bounds Y={} (sectionIndex={})",
                            blockY, sectionIndex);
                    continue;
                }

                LevelChunkSection section = chunk.getSection(sectionIndex);

                // Biome quanta coords - all LOCAL to the section (0-3 each axis).
                // X and Z are local to the chunk (& 15) then shifted to quantum (>> 2).
                // Y must be local to the SECTION (& 15), not absolute, then shifted.
                int localBiomeX = (loc.getBlockX() & 15) >> 2; // 0-3
                int localBiomeY = (blockY & 15) >> 2; // 0-3 within section
                int localBiomeZ = (loc.getBlockZ() & 15) >> 2; // 0-3

                // Biomes are stored in a PalettedContainer<Holder<Biome>> on the section.
                // getBiomes() returns PalettedContainerRO; we cast to the mutable subtype.
                PalettedContainer<Holder<Biome>> biomes =
                        (PalettedContainer<Holder<Biome>>) section.getBiomes();

                biomes.set(localBiomeX, localBiomeY, localBiomeZ, holder);

                Log.debug("NmsBiomeHandler",
                        "  quantum block=({},{},{}) -> section={} local=({},{},{})",
                        loc.getBlockX(), blockY, loc.getBlockZ(),
                        sectionIndex, localBiomeX, localBiomeY, localBiomeZ);
            }

            chunk.markUnsaved();
            world.refreshChunk(work.chunkX, work.chunkZ);

            Log.debug("NmsBiomeHandler", "Wrote {} biome quantum/a in chunk ({}, {}), refreshed",
                    work.locations.size(), work.chunkX, work.chunkZ);
        }
    }

    private Holder<Biome> resolveHolder(ServerLevel level, org.bukkit.block.Biome biome) {
        ResourceKey<Biome> resourceKey = ResourceKey.create(
                Registries.BIOME,
                ResourceLocation.parse(biome.getKey().toString())
        );

        return level.registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .getOrThrow(resourceKey);
    }

    private static final class ChunkWork {
        final int chunkX;
        final int chunkZ;
        final List<Location> locations = new ArrayList<>();

        ChunkWork(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}
