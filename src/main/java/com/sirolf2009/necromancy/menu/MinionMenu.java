package com.sirolf2009.necromancy.menu;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.registry.NecroMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A Minion's equipment and stats (docs/REVIEW.md #3): armour, both hands (needs arms) and a saddle (needs a torso that
 * takes one). Opened with sneak + empty hand on your own Minion.
 */
public class MinionMenu extends AbstractContainerMenu {
    public static final int DATA_DAMAGE = 0;
    public static final int DATA_FOLLOW = 1;
    public static final int DATA_KNOCKBACK = 2;
    public static final int DATA_SIZE = 3;

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final ResourceLocation[] EMPTY_ARMOR = { InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS };
    public static final int EQUIPMENT_SLOTS = 7;

    @Nullable
    private final MinionEntity minion;
    private final ContainerData data;

    public MinionMenu(int containerId, Inventory inventory, @Nullable MinionEntity minion) {
        this(containerId, inventory, minion, minion == null ? new SimpleContainerData(DATA_SIZE) : statsOf(minion));
    }

    private MinionMenu(int containerId, Inventory inventory, @Nullable MinionEntity minion, ContainerData data) {
        super(NecroMenus.MINION.get(), containerId);
        this.minion = minion;
        this.data = data;
        Container equipment = new Equipment(minion);
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = ARMOR[i];
            addSlot(new Slot(equipment, i, 8, 18 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return minion != null && minion.getEquipmentSlotForItem(stack) == slot && (slot != EquipmentSlot.HEAD || minion.part(BodyPartLocation.HEAD) != null);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            }.setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_ARMOR[i]));
        }
        addSlot(new Slot(equipment, 4, 80, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return minion != null && minion.hasArms();
            }
        });
        addSlot(new Slot(equipment, 5, 80, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return minion != null && minion.hasArms();
            }
        }.setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
        addSlot(new Slot(equipment, 6, 80, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return minion != null && stack.is(Items.SADDLE) && minion.isSaddleable() && !minion.isSaddled();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return minion != null && (minion.isSaddleable() || minion.isSaddled());
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public static MinionMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        MinionEntity minion = inventory.player.level().getEntity(id) instanceof MinionEntity m ? m : null;
        return new MinionMenu(containerId, inventory, minion, new SimpleContainerData(DATA_SIZE));
    }

    private static ContainerData statsOf(MinionEntity minion) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_DAMAGE -> (int) Math.round(minion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 100);
                    case DATA_FOLLOW -> (int) Math.round(minion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) * 10);
                    case DATA_KNOCKBACK -> (int) Math.round(minion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 100);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
    }

    @Nullable
    public MinionEntity minion() {
        return minion;
    }

    public float attackDamage() {
        return data.get(DATA_DAMAGE) / 100F;
    }

    public float followRange() {
        return data.get(DATA_FOLLOW) / 10F;
    }

    public float knockbackResistance() {
        return data.get(DATA_KNOCKBACK) / 100F;
    }

    @Override
    public boolean stillValid(Player player) {
        return minion != null && minion.isAlive() && minion.isOwnedBy(player) && minion.distanceToSqr(player) < 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < EQUIPMENT_SLOTS) {
            if (!moveItemStackTo(stack, EQUIPMENT_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < EQUIPMENT_SLOTS && !moved; i++) {
                Slot target = slots.get(i);
                if (target.isActive() && !target.hasItem() && target.mayPlace(stack) && (i != 4 || !(stack.getItem() instanceof Equipable))) {
                    moved = moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /** The Minion's equipment slots seen as a container: 0-3 armour, 4 main hand, 5 off hand, 6 saddle. */
    private static final class Equipment implements Container {
        @Nullable
        private final MinionEntity minion;

        Equipment(@Nullable MinionEntity minion) {
            this.minion = minion;
        }

        private static EquipmentSlot slot(int index) {
            return switch (index) {
                case 0 -> EquipmentSlot.HEAD;
                case 1 -> EquipmentSlot.CHEST;
                case 2 -> EquipmentSlot.LEGS;
                case 3 -> EquipmentSlot.FEET;
                case 4 -> EquipmentSlot.MAINHAND;
                case 5 -> EquipmentSlot.OFFHAND;
                default -> null;
            };
        }

        @Override
        public int getContainerSize() {
            return EQUIPMENT_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < EQUIPMENT_SLOTS; i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            if (minion == null) {
                return ItemStack.EMPTY;
            }
            if (index == 6) {
                return minion.isSaddled() ? new ItemStack(Items.SADDLE) : ItemStack.EMPTY;
            }
            EquipmentSlot slot = slot(index);
            return slot == null ? ItemStack.EMPTY : minion.getItemBySlot(slot);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack current = getItem(index);
            if (current.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = current.copy();
            setItem(index, ItemStack.EMPTY);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return removeItem(index, 64);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (minion == null) {
                return;
            }
            if (index == 6) {
                if (stack.isEmpty()) {
                    minion.removeSaddle();
                } else {
                    minion.equipSaddle(stack, net.minecraft.sounds.SoundSource.NEUTRAL);
                }
                return;
            }
            EquipmentSlot slot = slot(index);
            if (slot != null) {
                minion.setItemSlot(slot, stack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return minion != null && minion.isAlive();
        }

        @Override
        public void clearContent() {
        }
    }
}
