package com.sirolf2009.necromancy.menu;

import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Summoning Altar screen: blood and soul on the sides, the body in a cross in the middle (original layout). */
public class AltarMenu extends AbstractContainerMenu {
    public static final int RAISE_BUTTON = 0;
    private final Container altar;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

    public AltarMenu(int containerId, Inventory inventory, Container altar, BlockPos pos) {
        super(NecroMenus.ALTAR.get(), containerId);
        checkContainerSize(altar, AltarBlockEntity.SIZE);
        this.altar = altar;
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        altar.startOpen(inventory.player);

        addSlot(new FilteredSlot(altar, AltarBlockEntity.BLOOD, 26, 40));
        addSlot(new FilteredSlot(altar, AltarBlockEntity.SOUL, 134, 39));
        addSlot(new FilteredSlot(altar, AltarBlockEntity.HEAD, 80, 19));
        addSlot(new FilteredSlot(altar, AltarBlockEntity.TORSO, 80, 36));
        addSlot(new FilteredSlot(altar, AltarBlockEntity.LEGS, 80, 53));
        addSlot(new FilteredSlot(altar, AltarBlockEntity.ARM_RIGHT, 63, 36));
        addSlot(new FilteredSlot(altar, AltarBlockEntity.ARM_LEFT, 97, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static AltarMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Container container = inventory.player.level().getBlockEntity(pos) instanceof AltarBlockEntity altar ? altar : new SimpleContainer(AltarBlockEntity.SIZE);
        return new AltarMenu(containerId, inventory, container, pos);
    }

    public BlockPos pos() {
        return pos;
    }

    public Container altar() {
        return altar;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == RAISE_BUTTON && altar instanceof AltarBlockEntity be) {
            be.tryRaise(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, NecroBlocks.SUMMONING_ALTAR.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        altar.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int altarEnd = AltarBlockEntity.SIZE;
        if (index < altarEnd) {
            if (!moveItemStackTo(stack, altarEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < altarEnd && !moved; i++) {
                if (AltarBlockEntity.isValid(i, stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                int invEnd = altarEnd + 27;
                if (index < invEnd) {
                    if (!moveItemStackTo(stack, invEnd, slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, altarEnd, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    private static class FilteredSlot extends Slot {
        FilteredSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AltarBlockEntity.isValid(getContainerSlot(), stack);
        }
    }
}
