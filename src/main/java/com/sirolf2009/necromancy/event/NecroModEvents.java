package com.sirolf2009.necromancy.event;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.NightCrawlerEntity;
import com.sirolf2009.necromancy.entity.TeddyEntity;
import com.sirolf2009.necromancy.entity.isaac.BloodIsaacEntity;
import com.sirolf2009.necromancy.entity.isaac.IsaacBodyEntity;
import com.sirolf2009.necromancy.entity.isaac.IsaacHeadEntity;
import com.sirolf2009.necromancy.network.TearShotPayload;
import com.sirolf2009.necromancy.registry.NecroEntities;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Necromancy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NecroModEvents {
    private NecroModEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(NecroEntities.MINION.get(), MinionEntity.createAttributes().build());
        event.put(NecroEntities.TEDDY.get(), TeddyEntity.createAttributes().build());
        event.put(NecroEntities.NIGHT_CRAWLER.get(), NightCrawlerEntity.createAttributes().build());
        event.put(NecroEntities.ISAAC.get(), IsaacBodyEntity.createAttributes().build());
        event.put(NecroEntities.ISAAC_BODY.get(), IsaacBodyEntity.createAttributes().build());
        event.put(NecroEntities.BLOOD_ISAAC.get(), BloodIsaacEntity.createAttributes().build());
        event.put(NecroEntities.ISAAC_HEAD.get(), IsaacHeadEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(NecroEntities.NIGHT_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(NecroEntities.ISAAC.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TearShotPayload.TYPE, TearShotPayload.STREAM_CODEC, TearShotPayload::handle);
    }
}
