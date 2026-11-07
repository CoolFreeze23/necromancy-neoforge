package com.sirolf2009.necromancy.menu;

import java.util.Optional;

import com.sirolf2009.necromancy.blockentity.SewingMachineBlockEntity;
import com.sirolf2009.necromancy.recipe.SewingRecipe;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroMenus;
import com.sirolf2009.necromancy.registry.NecroRecipes;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * The Sewing Machine screen: a 4x4 grid, the needle and thread slots and the result. Every stitch uses up one String
 * and wears the Bone Needle (10 stitches per needle; the original broke a needle on every craft).
 */
public class SewingMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int GRID_START = 1;
    public static final int GRID_END = GRID_START + 16;
    public static final int NEEDLE_SLOT = GRID_END;
    public static final int THREAD_SLOT = GRID_END + 1;
    public static final int INV_START = THREAD_SLOT + 1;

    private final TransientCraftingContainer grid = new TransientCraftingContainer(this, 4, 4);
    private final ResultContainer result = new ResultContainer();
    private final Container machine;
    private final ContainerLevelAccess access;
    private final Player player;
    private final BlockPos pos;

    public SewingMenu(int containerId, Inventory inventory, Container machine, BlockPos pos) {
        super(NecroMenus.SEWING.get(), containerId);
        checkContainerSize(machine, 2);
        this.machine = machine;
        this.player = inventory.player;
        this.pos = pos;
        this.access = ContainerLevelAccess.create(player.level(), pos);

        addSlot(new SewingResultSlot(145, 35));
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                addSlot(new Slot(grid, x + y * 4, 8 + x * 18, 8 + y * 18));
            }
        }
        addSlot(new MachineSlot(machine, SewingMachineBlockEntity.NEEDLE, 95, 17));
        addSlot(new MachineSlot(machine, SewingMachineBlockEntity.THREAD, 95, 54));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static SewingMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Container container = inventory.player.level().getBlockEntity(pos) instanceof SewingMachineBlockEntity machine ? machine : new SimpleContainer(2);
        return new SewingMenu(containerId, inventory, container, pos);
    }

    public BlockPos pos() {
        return pos;
    }

    public boolean isReady() {
        return machine.getItem(SewingMachineBlockEntity.NEEDLE).is(NecroItems.BONE_NEEDLE.get())
                && machine.getItem(SewingMachineBlockEntity.THREAD).is(NecroTags.SEWING_THREAD);
    }

    public TransientCraftingContainer grid() {
        return grid;
    }

    @Override
    public void slotsChanged(Container container) {
        access.execute((level, p) -> updateResult(level));
    }

    private void updateResult(Level level) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = ItemStack.EMPTY;
        if (isReady()) {
            Optional<RecipeHolder<SewingRecipe>> recipe = findRecipe(level, grid.asCraftInput());
            if (recipe.isPresent() && result.setRecipeUsed(level, serverPlayer, recipe.get())) {
                stack = recipe.get().value().assemble(grid.asCraftInput(), level.registryAccess());
            }
        }
        result.setItem(0, stack);
        setRemoteSlot(RESULT_SLOT, stack);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, stack));
    }

    public static Optional<RecipeHolder<SewingRecipe>> findRecipe(Level level, CraftingInput input) {
        return level.getRecipeManager().getRecipeFor(NecroRecipes.SEWING.get(), input, level);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, NecroBlocks.SEWING_MACHINE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, p) -> clearContainer(player, grid));
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int end = slots.size();
        if (index == RESULT_SLOT) {
            access.execute((level, p) -> stack.getItem().onCraftedBy(stack, level, player));
            if (!moveItemStackTo(stack, INV_START, end, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index >= INV_START) {
            if (SewingMachineBlockEntity.isValid(SewingMachineBlockEntity.NEEDLE, stack) && moveItemStackTo(stack, NEEDLE_SLOT, NEEDLE_SLOT + 1, false)) {
                // moved into the needle slot
            } else if (SewingMachineBlockEntity.isValid(SewingMachineBlockEntity.THREAD, stack) && moveItemStackTo(stack, THREAD_SLOT, THREAD_SLOT + 1, false)) {
                // moved into the thread slot
            } else if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
                int hotbar = end - 9;
                if (index < hotbar) {
                    if (!moveItemStackTo(stack, hotbar, end, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, INV_START, hotbar, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!moveItemStackTo(stack, INV_START, end, false)) {
            return ItemStack.EMPTY;
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
        if (index == RESULT_SLOT) {
            player.drop(stack, false);
        }
        return copy;
    }

    /** Needle and thread: changing them re-checks the grid. */
    private class MachineSlot extends Slot {
        MachineSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SewingMachineBlockEntity.isValid(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return getContainerSlot() == SewingMachineBlockEntity.NEEDLE ? 1 : super.getMaxStackSize();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(grid);
        }
    }

    private class SewingResultSlot extends Slot {
        private int removeCount;

        SewingResultSlot(int x, int y) {
            super(result, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            if (hasItem()) {
                removeCount += Math.min(amount, getItem().getCount());
            }
            return super.remove(amount);
        }

        @Override
        protected void onQuickCraft(ItemStack stack, int amount) {
            removeCount += amount;
            checkTakeAchievements(stack);
        }

        @Override
        protected void checkTakeAchievements(ItemStack stack) {
            if (removeCount > 0) {
                stack.onCraftedBy(player.level(), player, removeCount);
                net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player, stack, grid);
            }
            removeCount = 0;
            result.awardUsedRecipes(player, grid.getItems());
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            checkTakeAchievements(stack);
            CraftingInput input = grid.asCraftInput();
            NonNullList<ItemStack> remainders = findRecipe(taker.level(), input)
                    .map(r -> r.value().getRemainingItems(input))
                    .orElseGet(() -> NonNullList.withSize(input.size(), ItemStack.EMPTY));
            CraftingInput.Positioned positioned = grid.asPositionedCraftInput();
            int left = positioned.left();
            int top = positioned.top();
            for (int y = 0; y < input.height(); y++) {
                for (int x = 0; x < input.width(); x++) {
                    int slotIndex = x + left + (y + top) * 4;
                    ItemStack inGrid = grid.getItem(slotIndex);
                    ItemStack remainder = remainders.get(x + y * input.width());
                    if (!inGrid.isEmpty()) {
                        grid.removeItem(slotIndex, 1);
                        inGrid = grid.getItem(slotIndex);
                    }
                    if (!remainder.isEmpty()) {
                        if (inGrid.isEmpty()) {
                            grid.setItem(slotIndex, remainder);
                        } else if (ItemStack.isSameItemSameComponents(inGrid, remainder)) {
                            remainder.grow(inGrid.getCount());
                            grid.setItem(slotIndex, remainder);
                        } else if (!taker.getInventory().add(remainder)) {
                            taker.drop(remainder, false);
                        }
                    }
                }
            }
            useThread(taker);
            slotsChanged(grid);
        }

        private void useThread(Player taker) {
            ItemStack thread = machine.getItem(SewingMachineBlockEntity.THREAD);
            if (!thread.isEmpty()) {
                thread.shrink(1);
            }
            ItemStack needle = machine.getItem(SewingMachineBlockEntity.NEEDLE);
            if (!needle.isEmpty() && !taker.level().isClientSide) {
                needle.hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) taker.level(), taker instanceof ServerPlayer sp ? sp : null, item -> {});
            }
            machine.setChanged();
            taker.level().playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.4F);
        }
    }
}
