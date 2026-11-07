package com.sirolf2009.necromancy.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.NecroConfig;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NecroLoot {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Necromancy.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<OrganDrops>> ORGAN_DROPS =
            MODIFIERS.register("organ_drops", () -> OrganDrops.CODEC);

    private NecroLoot() {
    }

    public static void register(IEventBus bus) {
        MODIFIERS.register(bus);
    }

    /**
     * Organs from dead creatures (the original death-event roll): brains 1%, heart 1%, muscle 4%, lungs 1%, at most one
     * organ per death. Creatures in #necromancy:bloodless have none.
     */
    public static class OrganDrops extends LootModifier {
        public static final MapCodec<OrganDrops> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, OrganDrops::new));

        public OrganDrops(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (!(entity instanceof LivingEntity) || entity instanceof Player || entity.getType().is(NecroTags.BLOODLESS)) {
                return loot;
            }
            if (!context.getQueriedLootTableId().getPath().startsWith("entities/")) {
                return loot;
            }
            double roll = context.getRandom().nextDouble() * 100.0 / NecroConfig.ORGAN_DROP_CHANCE.get();
            if (roll < 1) {
                loot.add(new ItemStack(NecroItems.BRAINS.get()));
            } else if (roll < 2) {
                loot.add(new ItemStack(NecroItems.HEART.get()));
            } else if (roll < 6) {
                loot.add(new ItemStack(NecroItems.MUSCLE.get()));
            } else if (roll < 7) {
                loot.add(new ItemStack(NecroItems.LUNGS.get()));
            }
            return loot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return ORGAN_DROPS.get();
        }
    }
}
