package com.sirolf2009.necromancy.entity.goal;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.TraitFlag;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.MinionOrder;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The Work order (docs/REVIEW.md #10): a Minion at its post does the job its body and tools allow, keeps what it gathers
 * in a 9-slot pack and empties the pack into the container it was given (when full or every 30 seconds).
 * <ul>
 * <li>Farmer: a thinking (villager) head, an arm and a hoe. Harvests ripe crops within 8 blocks and replants them.</li>
 * <li>Collector: two Enderman arms. Picks up items within 12 blocks (only the off-hand item's kind, if it holds one).</li>
 * <li>Reaper: a scythe in hand. Keeps two adults of each kind of animal within 8 blocks and culls one extra adult every
 * 30 seconds (never babies, named, leashed or tamed animals), bottling souls with the glass bottles in its pack.</li>
 * </ul>
 */
public class MinionWorkGoal extends Goal {
    public enum Job {
        FARMER, COLLECTOR, REAPER
    }

    private static final int RADIUS = 8;
    private final MinionEntity minion;
    private int cooldown;
    private int depositTimer;
    @Nullable
    private BlockPos targetCrop;

    public MinionWorkGoal(MinionEntity minion) {
        this.minion = minion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable
    public static Job jobOf(MinionEntity minion) {
        if (minion.getMainHandItem().is(NecroTags.SCYTHES)) {
            return Job.REAPER;
        }
        ResourceLocation ender = com.sirolf2009.necromancy.Necromancy.id("enderman");
        var left = minion.part(BodyPartLocation.ARM_LEFT);
        var right = minion.part(BodyPartLocation.ARM_RIGHT);
        if (left != null && right != null && left.id().equals(ender) && right.id().equals(ender)) {
            return Job.COLLECTOR;
        }
        if (minion.hasFlag(BodyPartLocation.HEAD, TraitFlag.THINKS) && minion.hasArms() && minion.getMainHandItem().getItem() instanceof HoeItem) {
            return Job.FARMER;
        }
        return null;
    }

    @Override
    public boolean canUse() {
        return minion.order() == MinionOrder.WORK && minion.getTarget() == null && jobOf(minion) != null && minion.workPost() != null;
    }

    @Override
    public void stop() {
        targetCrop = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(minion.level() instanceof ServerLevel level)) {
            return;
        }
        SimpleContainer pack = minion.pack();
        if (++depositTimer >= 600 || isFull(pack)) {
            if (deposit(level, pack)) {
                depositTimer = 0;
            }
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        Job job = jobOf(minion);
        if (job == null) {
            return;
        }
        switch (job) {
            case FARMER -> farm(level, pack);
            case COLLECTOR -> collect(level, pack);
            case REAPER -> reap(level, pack);
        }
    }

    private void farm(ServerLevel level, SimpleContainer pack) {
        BlockPos post = minion.workPost();
        if (targetCrop == null || !isRipe(level.getBlockState(targetCrop))) {
            targetCrop = BlockPos.betweenClosedStream(post.offset(-RADIUS, -2, -RADIUS), post.offset(RADIUS, 2, RADIUS))
                    .filter(p -> isRipe(level.getBlockState(p))).map(BlockPos::immutable).findFirst().orElse(null);
            cooldown = 20;
            if (targetCrop == null) {
                return;
            }
        }
        if (minion.distanceToSqr(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5) > 4) {
            minion.getNavigation().moveTo(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5, 0.9);
            cooldown = 10;
            return;
        }
        BlockState state = level.getBlockState(targetCrop);
        if (!(state.getBlock() instanceof CropBlock crop)) {
            targetCrop = null;
            return;
        }
        minion.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        List<ItemStack> drops = Block.getDrops(state, level, targetCrop, null, minion, minion.getMainHandItem());
        level.destroyBlock(targetCrop, false, minion);
        ItemStack seed = new ItemStack(crop.getCloneItemStack(level, targetCrop, state).getItem());
        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (!replanted && ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                replanted = true;
            }
            ItemStack rest = pack.addItem(drop);
            if (!rest.isEmpty()) {
                minion.spawnAtLocation(rest);
            }
        }
        if (replanted) {
            level.setBlock(targetCrop, crop.getStateForAge(0), Block.UPDATE_ALL);
        }
        minion.getMainHandItem().hurtAndBreak(1, minion, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        targetCrop = null;
        cooldown = 40;
    }

    private static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private void collect(ServerLevel level, SimpleContainer pack) {
        cooldown = 20;
        ItemStack filter = minion.getOffhandItem();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, minion.getBoundingBox().inflate(12),
                item -> item.isAlive() && !item.hasPickUpDelay() && (filter.isEmpty() || ItemStack.isSameItem(filter, item.getItem())));
        for (ItemEntity item : items) {
            ItemStack rest = pack.addItem(item.getItem().copy());
            if (rest.getCount() != item.getItem().getCount()) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, item.getX(), item.getY(), item.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
                if (rest.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(rest);
                }
            }
            if (isFull(pack)) {
                return;
            }
        }
    }

    private void reap(ServerLevel level, SimpleContainer pack) {
        cooldown = 600;
        BlockPos post = minion.workPost();
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, new net.minecraft.world.phys.AABB(post).inflate(RADIUS),
                a -> a.isAlive() && !a.isBaby() && !a.hasCustomName() && !a.isLeashed()
                        && !(a instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()) && !(a instanceof MinionEntity));
        Map<EntityType<?>, List<Animal>> byType = animals.stream().collect(Collectors.groupingBy(Animal::getType));
        for (List<Animal> herd : byType.values()) {
            if (herd.size() > 2) {
                Animal victim = herd.get(minion.getRandom().nextInt(herd.size()));
                minion.getLookControl().setLookAt(victim);
                minion.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                boolean bottle = takeBottle(pack);
                victim.hurt(minion.damageSources().mobAttack(minion), Float.MAX_VALUE);
                if (bottle) {
                    ItemStack soul = new ItemStack(com.sirolf2009.necromancy.registry.NecroItems.SOUL_IN_A_JAR.get());
                    soul.set(com.sirolf2009.necromancy.registry.NecroComponents.SOUL.get(),
                            com.sirolf2009.necromancy.soul.SoulData.of(victim.getType(), com.sirolf2009.necromancy.soul.SoulPotency.of(victim)));
                    ItemStack rest = pack.addItem(soul);
                    if (!rest.isEmpty()) {
                        minion.spawnAtLocation(rest);
                    }
                }
                minion.getMainHandItem().hurtAndBreak(1, minion, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return;
            }
        }
    }

    private static boolean takeBottle(SimpleContainer pack) {
        for (int i = 0; i < pack.getContainerSize(); i++) {
            if (pack.getItem(i).is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                pack.getItem(i).shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean isFull(SimpleContainer pack) {
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /** Walks to the linked container and puts everything except glass bottles into it. True once done. */
    private boolean deposit(ServerLevel level, SimpleContainer pack) {
        BlockPos chest = minion.workContainer();
        if (chest == null) {
            return true;
        }
        if (minion.distanceToSqr(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5) > 6.25) {
            minion.getNavigation().moveTo(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, 1.0);
            return false;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chest, null);
        if (handler == null) {
            return true;
        }
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && !stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                pack.setItem(i, ItemHandlerHelper.insertItemStacked(handler, stack, false));
            }
        }
        return true;
    }
}
