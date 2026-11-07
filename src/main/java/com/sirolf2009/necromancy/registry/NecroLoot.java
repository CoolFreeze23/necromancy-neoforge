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
     * Organs from creatures killed by a player or a player's Minion (docs/REVIEW.md #8). Each organ rolls on its own:
     * brains 2%, heart 2%, lungs 2%, muscle 5%, 1-2 skin 15%; creatures in the {@code necromancy:organ_rich/*} tags are
     * richer in one organ (ghasts are mostly lung). Every level of Looting or Reaping adds 1%. Babies and creatures in
     * #necromancy:bloodless have none. The config multiplier scales every chance.
     */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> REAPING =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, Necromancy.id("reaping"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> RICH_BRAINS = richTag("brains");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> RICH_HEART = richTag("heart");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> RICH_MUSCLE = richTag("muscle");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> RICH_LUNGS = richTag("lungs");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> RICH_LUNGS_PLENTY = richTag("lungs_plenty");

    private static net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> richTag(String organ) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Necromancy.id("organ_rich/" + organ));
    }

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
            if (entity instanceof LivingEntity living && living.isBaby()) {
                return loot;
            }
            Entity killer = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
            Player player = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
            if (player == null && !(killer instanceof Player)) {
                return loot;
            }
            int bonus = 0;
            if (killer instanceof LivingEntity attacker) {
                var enchantments = context.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                ItemStack weapon = attacker.getMainHandItem();
                bonus += net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), weapon);
                bonus += enchantments.get(REAPING).map(h -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h, weapon)).orElse(0);
            }
            double scale = NecroConfig.ORGAN_DROP_CHANCE.get();
            var type = entity.getType();
            var random = context.getRandom();
            roll(loot, random, NecroItems.BRAINS.get(), (type.is(RICH_BRAINS) ? 6 : 2) + bonus, scale, 1);
            roll(loot, random, NecroItems.HEART.get(), (type.is(RICH_HEART) ? 5 : 2) + bonus, scale, 1);
            roll(loot, random, NecroItems.LUNGS.get(), (type.is(RICH_LUNGS_PLENTY) ? 25 : type.is(RICH_LUNGS) ? 10 : 2) + bonus, scale, 1);
            roll(loot, random, NecroItems.MUSCLE.get(), (type.is(RICH_MUSCLE) ? 12 : 5) + bonus, scale, 1);
            roll(loot, random, NecroItems.SKIN.get(), 15 + bonus, scale, 1 + random.nextInt(2));
            return loot;
        }

        private static void roll(ObjectArrayList<ItemStack> loot, net.minecraft.util.RandomSource random, net.minecraft.world.item.Item item,
                double percent, double scale, int count) {
            if (random.nextDouble() * 100 < percent * scale) {
                loot.add(new ItemStack(item, count));
            }
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return ORGAN_DROPS.get();
        }
    }
}
