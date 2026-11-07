package com.sirolf2009.necromancy.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Blocks a Minion trait puts down for a while (cobwebs): saved with the level and taken away when their time is up, or
 * when their chunk is next loaded if the time ran out meanwhile. A block is only removed while it is still the one placed.
 */
public class PlacedBlocks extends SavedData {
    private static final String NAME = "necromancy_placed_blocks";
    private static final SavedData.Factory<PlacedBlocks> FACTORY = new SavedData.Factory<>(PlacedBlocks::new, PlacedBlocks::load, null);
    private final List<Entry> entries = new ArrayList<>();

    record Entry(BlockPos pos, Block block, long until) {
    }

    public static PlacedBlocks get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Places the block and remembers to take it away after {@code lifetime} ticks. */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int lifetime) {
        PlacedBlocks data = get(level);
        if (data.entries.size() > 512 || data.entries.stream().anyMatch(e -> e.pos.equals(pos))) {
            return false;
        }
        if (!level.setBlock(pos, state, 3)) {
            return false;
        }
        data.entries.add(new Entry(pos.immutable(), state.getBlock(), level.getGameTime() + lifetime));
        data.setDirty();
        return true;
    }

    /** Called once a second for each level. */
    public static void tick(ServerLevel level) {
        PlacedBlocks data = level.getDataStorage().get(FACTORY, NAME);
        if (data == null || data.entries.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Entry> it = data.entries.iterator(); it.hasNext();) {
            Entry e = it.next();
            if (e.until > now || !level.isLoaded(e.pos)) {
                continue;
            }
            if (level.getBlockState(e.pos).is(e.block)) {
                level.setBlock(e.pos, Blocks.AIR.defaultBlockState(), 3);
            }
            it.remove();
            data.setDirty();
        }
    }

    private static PlacedBlocks load(CompoundTag tag, HolderLookup.Provider registries) {
        PlacedBlocks data = new PlacedBlocks();
        for (Tag t : tag.getList("Blocks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(c.getString("Block")));
            data.entries.add(new Entry(BlockPos.of(c.getLong("Pos")), block, c.getLong("Until")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", e.pos.asLong());
            c.putString("Block", BuiltInRegistries.BLOCK.getKey(e.block).toString());
            c.putLong("Until", e.until);
            list.add(c);
        }
        tag.put("Blocks", list);
        return tag;
    }
}
