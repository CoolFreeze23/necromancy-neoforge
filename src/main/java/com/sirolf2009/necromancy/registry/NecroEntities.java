package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.entity.BloodTearEntity;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.NightCrawlerEntity;
import com.sirolf2009.necromancy.entity.TeddyEntity;
import com.sirolf2009.necromancy.entity.TearEntity;
import com.sirolf2009.necromancy.entity.isaac.BloodIsaacEntity;
import com.sirolf2009.necromancy.entity.isaac.IsaacBodyEntity;
import com.sirolf2009.necromancy.entity.isaac.IsaacEntity;
import com.sirolf2009.necromancy.entity.isaac.IsaacHeadEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Necromancy.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MinionEntity>> MINION = ENTITIES.register("minion",
            () -> EntityType.Builder.of(MinionEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10)
                    .build(Necromancy.MOD_ID + ":minion"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.sirolf2009.necromancy.entity.AbominationEntity>> ABOMINATION = ENTITIES.register("abomination",
            () -> EntityType.Builder.of(com.sirolf2009.necromancy.entity.AbominationEntity::new, MobCategory.MONSTER).sized(0.8F, 2.0F).eyeHeight(1.7F)
                    .clientTrackingRange(10).fireImmune().build(Necromancy.MOD_ID + ":abomination"));
    public static final DeferredHolder<EntityType<?>, EntityType<TeddyEntity>> TEDDY = ENTITIES.register("teddy",
            () -> EntityType.Builder.of(TeddyEntity::new, MobCategory.CREATURE).sized(0.6F, 0.8F).clientTrackingRange(10)
                    .build(Necromancy.MOD_ID + ":teddy"));
    public static final DeferredHolder<EntityType<?>, EntityType<NightCrawlerEntity>> NIGHT_CRAWLER = ENTITIES.register("night_crawler",
            () -> EntityType.Builder.of(NightCrawlerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.0F).clientTrackingRange(8)
                    .build(Necromancy.MOD_ID + ":night_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<IsaacEntity>> ISAAC = ENTITIES.register("isaac",
            () -> EntityType.Builder.of(IsaacEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(Necromancy.MOD_ID + ":isaac"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodIsaacEntity>> BLOOD_ISAAC = ENTITIES.register("blood_isaac",
            () -> EntityType.Builder.of(BloodIsaacEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(Necromancy.MOD_ID + ":blood_isaac"));
    public static final DeferredHolder<EntityType<?>, EntityType<IsaacHeadEntity>> ISAAC_HEAD = ENTITIES.register("isaac_head",
            () -> EntityType.Builder.of(IsaacHeadEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8)
                    .build(Necromancy.MOD_ID + ":isaac_head"));
    public static final DeferredHolder<EntityType<?>, EntityType<IsaacBodyEntity>> ISAAC_BODY = ENTITIES.register("isaac_body",
            () -> EntityType.Builder.of(IsaacBodyEntity::new, MobCategory.MONSTER).sized(0.6F, 1.3F).clientTrackingRange(8)
                    .build(Necromancy.MOD_ID + ":isaac_body"));
    public static final DeferredHolder<EntityType<?>, EntityType<TearEntity>> TEAR = ENTITIES.register("tear",
            () -> EntityType.Builder.<TearEntity>of(TearEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build(Necromancy.MOD_ID + ":tear"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodTearEntity>> BLOOD_TEAR = ENTITIES.register("blood_tear",
            () -> EntityType.Builder.<BloodTearEntity>of(BloodTearEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build(Necromancy.MOD_ID + ":blood_tear"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.sirolf2009.necromancy.entity.PartBoltEntity>> PART_BOLT = ENTITIES.register("part_bolt",
            () -> EntityType.Builder.<com.sirolf2009.necromancy.entity.PartBoltEntity>of(com.sirolf2009.necromancy.entity.PartBoltEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10).build(Necromancy.MOD_ID + ":part_bolt"));

    private NecroEntities() {
    }
}
