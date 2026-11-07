package com.sirolf2009.necromancy.blockentity;

import com.sirolf2009.necromancy.menu.SewingMenu;
import com.sirolf2009.necromancy.registry.NecroBlockEntities;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the Sewing Machine's needle and thread. The 4x4 grid itself belongs to the open screen, like a crafting table. */
public class SewingMachineBlockEntity extends BaseContainerBlockEntity {
    public static final int NEEDLE = 0;
    public static final int THREAD = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    /** Client animation: needle position in ticks, advanced while someone sews. */
    public int stitchTicks;

    public SewingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(NecroBlockEntities.SEWING_MACHINE.get(), pos, state);
    }

    public static boolean isValid(int slot, ItemStack stack) {
        return slot == NEEDLE ? stack.is(NecroItems.BONE_NEEDLE.get()) : stack.is(NecroTags.SEWING_THREAD);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValid(slot, stack);
    }

    public boolean isReady() {
        return items.get(NEEDLE).is(NecroItems.BONE_NEEDLE.get()) && items.get(THREAD).is(NecroTags.SEWING_THREAD);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.necromancy.sewing_machine");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new SewingMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
    }
}
