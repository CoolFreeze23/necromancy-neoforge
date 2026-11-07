package com.sirolf2009.necromancy.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * How many Minions each player has alive, kept per world so a Minion that dies while its master is offline still counts
 * down (the original stored this on the player and lost track).
 */
public class MinionCensus extends SavedData {
    private static final String NAME = "necromancy_minions";
    private final Map<UUID, Integer> counts = new HashMap<>();

    public static MinionCensus get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(MinionCensus::new, MinionCensus::load), NAME);
    }

    private static MinionCensus load(CompoundTag tag, HolderLookup.Provider registries) {
        MinionCensus census = new MinionCensus();
        CompoundTag counts = tag.getCompound("Counts");
        for (String key : counts.getAllKeys()) {
            try {
                census.counts.put(UUID.fromString(key), counts.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // skip a broken entry
            }
        }
        return census;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag counts = new CompoundTag();
        this.counts.forEach((uuid, count) -> counts.putInt(uuid.toString(), count));
        tag.put("Counts", counts);
        return tag;
    }

    public int count(UUID owner) {
        return counts.getOrDefault(owner, 0);
    }

    public void add(UUID owner) {
        counts.merge(owner, 1, Integer::sum);
        setDirty();
    }

    public void remove(UUID owner) {
        counts.computeIfPresent(owner, (k, v) -> v <= 1 ? null : v - 1);
        setDirty();
    }
}
