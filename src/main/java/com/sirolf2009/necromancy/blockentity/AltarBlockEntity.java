package com.sirolf2009.necromancy.blockentity;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.block.AltarBlock;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.menu.AltarMenu;
import com.sirolf2009.necromancy.entity.MinionCensus;
import com.sirolf2009.necromancy.registry.NecroBlockEntities;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroSounds;

import com.sirolf2009.necromancy.soul.Shrine;
import com.sirolf2009.necromancy.soul.SoulData;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Summoning Altar's inventory: blood, soul and five body parts (the original slot order). Raising a Minion uses up one
 * of each. The parts are synced to clients so the altar can show the Minion lying on its table.
 */
public class AltarBlockEntity extends BaseContainerBlockEntity {
    public static final int BLOOD = 0;
    public static final int SOUL = 1;
    public static final int HEAD = 2;
    public static final int TORSO = 3;
    public static final int LEGS = 4;
    public static final int ARM_RIGHT = 5;
    public static final int ARM_LEFT = 6;
    public static final int SIZE = 7;

    public static final int INFO_WEIGHT = 0;
    public static final int INFO_SOUL_CAP = 1;
    public static final int INFO_POINTS = 2;
    public static final int INFO_TIER = 3;
    public static final int INFO_MINIONS = 4;
    public static final int INFO_MINION_CAP = 5;
    public static final int INFO_SIZE = 6;

    private Shrine.State shrine = new Shrine.State(0, 1);
    private long shrineScannedAt = Long.MIN_VALUE;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private BlockState originalBase = Blocks.OAK_PLANKS.defaultBlockState();
    private BlockState originalSide1 = Blocks.COBBLESTONE.defaultBlockState();
    private BlockState originalSide2 = Blocks.COBBLESTONE.defaultBlockState();
    /** Client side: ticks since the parts last changed, for the preview's fade-in. */
    public int previewAge;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(NecroBlockEntities.ALTAR.get(), pos, state);
    }

    public static BodyPartLocation locationOf(int slot) {
        return switch (slot) {
            case HEAD -> BodyPartLocation.HEAD;
            case TORSO -> BodyPartLocation.TORSO;
            case LEGS -> BodyPartLocation.LEGS;
            case ARM_RIGHT -> BodyPartLocation.ARM_RIGHT;
            case ARM_LEFT -> BodyPartLocation.ARM_LEFT;
            default -> null;
        };
    }

    public static boolean isValid(int slot, ItemStack stack) {
        if (slot == BLOOD) {
            return stack.is(NecroItems.JAR_OF_BLOOD.get());
        }
        if (slot == SOUL) {
            return stack.is(NecroItems.SOUL_IN_A_JAR.get()) || stack.is(net.minecraft.world.item.Items.OMINOUS_BOTTLE) || stack.is(NecroItems.MINION_REMAINS.get()) && stack.has(com.sirolf2009.necromancy.registry.NecroComponents.MINION_BUILD.get());
        }
        BodyPartLocation location = locationOf(slot);
        return location != null && NecroMobRegistry.partOf(stack).map(ref -> ref.fits(location)).orElse(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValid(slot, stack);
    }

    /** The mob each body slot currently holds a part of, for the preview and the raise. */
    /** The Remains in the soul slot, if any: they bring their own body. */
    @Nullable
    public com.sirolf2009.necromancy.soul.MinionBuild remains() {
        return items.get(SOUL).get(com.sirolf2009.necromancy.registry.NecroComponents.MINION_BUILD.get());
    }

    public Map<BodyPartLocation, ResourceLocation> parts() {
        com.sirolf2009.necromancy.soul.MinionBuild remains = remains();
        if (remains != null) {
            return remains.locations();
        }
        Map<BodyPartLocation, ResourceLocation> map = new EnumMap<>(BodyPartLocation.class);
        for (int slot = HEAD; slot <= ARM_LEFT; slot++) {
            BodyPartLocation location = locationOf(slot);
            ItemStack stack = items.get(slot);
            NecroMobRegistry.partOf(stack).filter(ref -> ref.fits(location)).ifPresent(ref -> map.put(location, ref.mob().id()));
        }
        return map;
    }

    /** Total weight of the body on the altar: each part counts its mob's weight (both arms count). */
    public int bodyWeight() {
        int weight = 0;
        for (ResourceLocation mob : parts().values()) {
            NecroMob necro = NecroMobRegistry.get(mob);
            weight += necro == null ? 0 : necro.weight();
        }
        return weight;
    }

    /** The heaviest body the soul in the soul slot can animate, or 0 with no soul. */
    public int soulCap() {
        ItemStack soul = items.get(SOUL);
        if (remains() != null) {
            return remains().soul().potency().weightCap();
        }
        return soul.is(NecroItems.SOUL_IN_A_JAR.get()) ? SoulData.get(soul).potency().weightCap() : 0;
    }

    /** The shrine around this altar, rescanned at most once a second. */
    public Shrine.State shrine() {
        if (level != null && level.getGameTime() - shrineScannedAt >= 20) {
            shrine = Shrine.scan(level, worldPosition);
            shrineScannedAt = level.getGameTime();
        }
        return shrine;
    }

    /** Values for the altar screen's side panel, read for one player. */
    public ContainerData infoData(Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case INFO_WEIGHT -> bodyWeight();
                    case INFO_SOUL_CAP -> NecroConfig.SOUL_POTENCY.get() ? soulCap() : 0;
                    case INFO_POINTS -> shrine().points();
                    case INFO_TIER -> shrine().tier();
                    case INFO_MINIONS -> level != null && level.getServer() != null ? MinionCensus.get(level.getServer()).count(player.getUUID()) : 0;
                    case INFO_MINION_CAP -> shrine().minionCap();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return INFO_SIZE;
            }
        };
    }

    public boolean hasRitualItems() {
        boolean blood = items.get(BLOOD).is(NecroItems.JAR_OF_BLOOD.get()) || remains() != null && shrine().tier() >= 3;
        return blood && isValid(SOUL, items.get(SOUL));
    }

    public boolean hasBody() {
        Map<BodyPartLocation, ResourceLocation> parts = parts();
        return parts.containsKey(BodyPartLocation.HEAD) && parts.containsKey(BodyPartLocation.TORSO) && parts.containsKey(BodyPartLocation.LEGS);
    }

    public int comparatorSignal() {
        int score = 0;
        for (int i = 0; i < SIZE; i++) {
            if (!items.get(i).isEmpty()) {
                score += i < 2 ? 3 : 2;
            }
        }
        return Math.min(15, score - (score > 0 ? 1 : 0));
    }

    // --- the Rite of the Abomination (docs/REVIEW.md #17) ---

    private static final int RITE_TICKS = 100;
    private int riteTicks;
    private long lastRiteDay = -1;

    private boolean isRite() {
        return items.get(SOUL).is(net.minecraft.world.item.Items.OMINOUS_BOTTLE);
    }

    /** Starts the Rite: an Ominous Bottle and a Jar of Blood on an empty altar, at a tier III shrine, at night or in a storm. */
    private boolean tryRite(Player player, ServerLevel server) {
        if (riteTicks > 0) {
            return false;
        }
        if (!parts().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.necromancy.rite.empty_altar"), true);
            return false;
        }
        if (!items.get(BLOOD).is(NecroItems.JAR_OF_BLOOD.get())) {
            player.displayClientMessage(Component.translatable("message.necromancy.altar.needs_blood_and_soul"), true);
            return false;
        }
        shrineScannedAt = Long.MIN_VALUE;
        if (shrine().tier() < 3) {
            player.displayClientMessage(Component.translatable("message.necromancy.rite.needs_tier_3"), true);
            return false;
        }
        if (server.isDay() && !server.isThundering()) {
            player.displayClientMessage(Component.translatable("message.necromancy.rite.needs_night"), true);
            return false;
        }
        long day = server.getDayTime() / 24000L;
        if (day == lastRiteDay) {
            player.displayClientMessage(Component.translatable("message.necromancy.rite.once_a_day"), true);
            return false;
        }
        lastRiteDay = day;
        if (!player.getAbilities().instabuild) {
            removeItem(SOUL, 1);
            removeItem(BLOOD, 1);
        }
        riteTicks = RITE_TICKS;
        server.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.necromancy.rite.begun"));
        setChanged();
        return true;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, AltarBlockEntity altar) {
        if (altar.riteTicks <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        altar.riteTicks--;
        Vec3 top = Vec3.atBottomCenterOf(pos.above());
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, top.x, top.y + 0.2, top.z, 4, 0.6, 0.2, 0.6, 0.02);
        server.sendParticles(ParticleTypes.SCULK_SOUL, top.x, top.y + 1, top.z, 2, 0.8, 0.8, 0.8, 0.02);
        if (altar.riteTicks % 25 == 0) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(top.add(server.random.nextGaussian() * 3, 0, server.random.nextGaussian() * 3));
                bolt.setVisualOnly(true);
                server.addFreshEntity(bolt);
            }
        }
        if (altar.riteTicks == 0) {
            var boss = com.sirolf2009.necromancy.registry.NecroEntities.ABOMINATION.get().create(server);
            if (boss != null) {
                boss.setParts(com.sirolf2009.necromancy.entity.AbominationEntity.randomBody(server.random));
                boss.moveTo(top.x, top.y, top.z, server.random.nextFloat() * 360F, 0);
                boss.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                boss.applyPartAttributes(true);
                boss.setPersistenceRequired();
                server.addFreshEntity(boss);
                server.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5F, 0.6F);
            }
            altar.setChanged();
        }
    }

    /** Raises the Minion. Called on sneak-use and by the Raise button. Returns true if a Minion rose. */
    public boolean tryRaise(Player player) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (isRite()) {
            return tryRite(player, server);
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative && !hasRitualItems()) {
            player.displayClientMessage(Component.translatable("message.necromancy.altar.needs_blood_and_soul"), true);
            return false;
        }
        if (!hasBody()) {
            player.displayClientMessage(Component.translatable("message.necromancy.altar.needs_body"), true);
            return false;
        }
        if (!creative && NecroConfig.SOUL_POTENCY.get() && bodyWeight() > soulCap()) {
            player.displayClientMessage(Component.translatable("message.necromancy.altar.too_heavy", bodyWeight(), soulCap()), true);
            return false;
        }
        MinionCensus census = MinionCensus.get(server.getServer());
        shrineScannedAt = Long.MIN_VALUE;
        int max = shrine().minionCap();
        if (max >= 0 && census.count(player.getUUID()) >= max && !creative) {
            player.sendSystemMessage(Component.translatable("message.necromancy.altar.too_many"));
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(worldPosition));
                bolt.setVisualOnly(true);
                server.addFreshEntity(bolt);
            }
            return false;
        }
        MinionEntity minion = NecroEntities.MINION.get().create(server);
        if (minion == null) {
            return false;
        }
        com.sirolf2009.necromancy.soul.MinionBuild remains = remains();
        minion.setParts(parts());
        minion.setSoul(remains != null ? remains.soul() : SoulData.get(items.get(SOUL)));
        if (remains != null) {
            remains.name().ifPresent(minion::setCustomName);
            minion.restoreKills(remains.kills());
        }
        minion.tame(player);
        BlockPos spawn = findSpawnPos();
        minion.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot() + 180F, 0F);
        minion.finalizeSpawn(server, server.getCurrentDifficultyAt(spawn), MobSpawnType.MOB_SUMMONED, null);
        minion.applyPartAttributes(true);
        server.addFreshEntityWithPassengers(minion);
        census.add(player.getUUID());
        CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, minion);
        if (NecroConfig.MINION_GRIEF_MESSAGES.get()) {
            player.sendSystemMessage(Component.translatable("message.necromancy.minion.bidding", minion.getDisplayName()));
        }
        server.playSound(null, worldPosition, NecroSounds.SPAWN.get(), SoundSource.PLAYERS, 1F, 1F / (server.random.nextFloat() * 0.4F + 0.8F));
        server.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 1, minion.getZ(), 30, 0.4, 0.8, 0.4, 0.03);
        server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, minion.getX(), minion.getY() + 0.5, minion.getZ(), 10, 0.4, 0.3, 0.4, 0.1);
        if (!creative) {
            if (remains != null) {
                removeItem(SOUL, 1);
                removeItem(BLOOD, 1);
            } else {
                for (int i = 0; i < SIZE; i++) {
                    removeItem(i, 1);
                }
            }
        }
        setChanged();
        return true;
    }

    /** On top of the altar, or beside it if something is in the way. */
    private BlockPos findSpawnPos() {
        BlockPos above = worldPosition.above();
        if (level.noCollision(new AABB(above).inflate(-0.2, 0, -0.2).expandTowards(0, 1, 0))) {
            return above;
        }
        for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos side = worldPosition.relative(dir);
            if (level.noCollision(new AABB(side).inflate(-0.2, 0, -0.2).expandTowards(0, 1, 0))) {
                return side;
            }
        }
        return above;
    }

    public void setOriginalBlocks(BlockState base, BlockState side1, BlockState side2) {
        this.originalBase = base;
        this.originalSide1 = side1;
        this.originalSide2 = side2;
        setChanged();
    }

    public BlockState originalBase() {
        return originalBase;
    }

    public BlockState originalSide(int distance) {
        return distance == 2 ? originalSide2 : originalSide1;
    }

    // --- container plumbing ---

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.necromancy.summoning_altar");
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
        return SIZE;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AltarMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        if (tag.contains("OriginalBase")) {
            originalBase = NbtUtils.readBlockState(blocks, tag.getCompound("OriginalBase"));
            originalSide1 = NbtUtils.readBlockState(blocks, tag.getCompound("OriginalSide1"));
            originalSide2 = NbtUtils.readBlockState(blocks, tag.getCompound("OriginalSide2"));
        }
        previewAge = 0;
        riteTicks = tag.getInt("RiteTicks");
        lastRiteDay = tag.contains("LastRiteDay") ? tag.getLong("LastRiteDay") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        tag.putInt("RiteTicks", riteTicks);
        tag.putLong("LastRiteDay", lastRiteDay);
        tag.put("OriginalBase", NbtUtils.writeBlockState(originalBase));
        tag.put("OriginalSide1", NbtUtils.writeBlockState(originalSide1));
        tag.put("OriginalSide2", NbtUtils.writeBlockState(originalSide2));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public net.minecraft.core.Direction facing() {
        return getBlockState().getValue(AltarBlock.FACING);
    }

    @Nullable
    public static NecroMob mob(@Nullable ResourceLocation id) {
        return NecroMobRegistry.get(id);
    }
}
