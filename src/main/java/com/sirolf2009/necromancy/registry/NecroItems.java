package com.sirolf2009.necromancy.registry;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.item.BodyPartItem;
import com.sirolf2009.necromancy.item.BoneNeedleItem;
import com.sirolf2009.necromancy.item.BrainOnAStickItem;
import com.sirolf2009.necromancy.item.IsaacsHeadItem;
import com.sirolf2009.necromancy.item.NecronomiconItem;
import com.sirolf2009.necromancy.item.ScytheItem;
import com.sirolf2009.necromancy.item.SoulHeartItem;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Necromancy.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Necromancy.MOD_ID);

    public static final Holder<ArmorMaterial> ISAAC_MATERIAL = ARMOR_MATERIALS.register("isaac", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 0);
                }
            }),
            0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
            List.of(new ArmorMaterial.Layer(Necromancy.id("isaac"))), 0F, 0F));

    /** The original scythe materials: 666 uses, speed 7, +2 (blood) / +4 (bone) damage, enchantability 9. */
    public static final SimpleTier BLOOD_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 666, 7F, 2F, 9, () -> Ingredient.of(Items.OBSIDIAN));
    public static final SimpleTier BONE_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 666, 7F, 4F, 9, () -> Ingredient.of(Items.BONE_BLOCK));

    private static final FoodProperties ORGAN_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
            .build();

    // Tools of the trade
    public static final DeferredItem<NecronomiconItem> NECRONOMICON = ITEMS.register("necronomicon", () -> new NecronomiconItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BoneNeedleItem> BONE_NEEDLE = ITEMS.register("bone_needle", () -> new BoneNeedleItem(new Item.Properties().durability(10)));
    public static final DeferredItem<com.sirolf2009.necromancy.item.SoulJarItem> SOUL_IN_A_JAR = ITEMS.register("soul_in_a_jar",
            () -> new com.sirolf2009.necromancy.item.SoulJarItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> JAR_OF_BLOOD = ITEMS.registerSimpleItem("jar_of_blood", new Item.Properties().craftRemainder(Items.GLASS_BOTTLE));
    public static final DeferredItem<BrainOnAStickItem> BRAIN_ON_A_STICK = ITEMS.register("brain_on_a_stick",
            () -> new BrainOnAStickItem(new Item.Properties().durability(25)));
    public static final DeferredItem<ScytheItem> BLOOD_SCYTHE = ITEMS.register("blood_scythe", () -> new ScytheItem(BLOOD_TIER,
            new Item.Properties().attributes(ScytheItem.createAttributes(BLOOD_TIER, 4, -2.8F))));
    public static final DeferredItem<ScytheItem> BONE_SCYTHE = ITEMS.register("bone_scythe", () -> new ScytheItem(BONE_TIER,
            new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ScytheItem.createAttributes(BONE_TIER, 4, -2.8F))));
    public static final DeferredItem<BucketItem> BLOOD_BUCKET = ITEMS.register("blood_bucket",
            () -> new BucketItem(NecroFluids.BLOOD.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<IsaacsHeadItem> ISAACS_HEAD = ITEMS.register("isaacs_head",
            () -> new IsaacsHeadItem(ISAAC_MATERIAL, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<SoulHeartItem> SOUL_HEART = ITEMS.register("soul_heart", () -> new SoulHeartItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Organs: edible, but not good for you
    public static final DeferredItem<Item> BRAINS = organ("brains");
    public static final DeferredItem<Item> HEART = organ("heart");
    public static final DeferredItem<Item> MUSCLE = organ("muscle");
    public static final DeferredItem<Item> LUNGS = organ("lungs");
    public static final DeferredItem<Item> SKIN = organ("skin");

    // Projectile sprites (not in the creative tab)
    public static final DeferredItem<Item> TEAR = ITEMS.registerSimpleItem("tear");
    public static final DeferredItem<Item> BLOOD_TEAR = ITEMS.registerSimpleItem("blood_tear");

    public static final DeferredItem<BlockItem> SEWING_MACHINE = ITEMS.registerSimpleBlockItem(NecroBlocks.SEWING_MACHINE);

    public static final DeferredItem<DeferredSpawnEggItem> TEDDY_SPAWN_EGG = egg("teddy", NecroEntities.TEDDY, 0x63451D, 0xFF0000);
    public static final DeferredItem<DeferredSpawnEggItem> NIGHT_CRAWLER_SPAWN_EGG = egg("night_crawler", NecroEntities.NIGHT_CRAWLER, 0x060606, 0x0D0D0D);
    public static final DeferredItem<DeferredSpawnEggItem> ISAAC_SPAWN_EGG = egg("isaac", NecroEntities.ISAAC, 0x060606, 0xCC9999);
    public static final DeferredItem<DeferredSpawnEggItem> BLOOD_ISAAC_SPAWN_EGG = egg("blood_isaac", NecroEntities.BLOOD_ISAAC, 0x100606, 0xD69999);
    public static final DeferredItem<DeferredSpawnEggItem> ISAAC_HEAD_SPAWN_EGG = egg("isaac_head", NecroEntities.ISAAC_HEAD, 0x1A0606, 0xD69999);
    public static final DeferredItem<DeferredSpawnEggItem> ISAAC_BODY_SPAWN_EGG = egg("isaac_body", NecroEntities.ISAAC_BODY, 0x240606, 0xD69999);

    /** mob name -> part type -> item */
    public static final Map<String, Map<PartType, DeferredItem<BodyPartItem>>> BODY_PARTS = new LinkedHashMap<>();

    static {
        for (Map.Entry<String, Set<PartType>> entry : BodyPartTable.SEWN.entrySet()) {
            Map<PartType, DeferredItem<BodyPartItem>> byType = new EnumMap<>(PartType.class);
            for (PartType type : entry.getValue()) {
                String mob = entry.getKey();
                byType.put(type, ITEMS.register(BodyPartTable.itemName(mob, type), () -> new BodyPartItem(new Item.Properties())));
            }
            BODY_PARTS.put(entry.getKey(), byType);
        }
    }

    private NecroItems() {
    }

    public static DeferredItem<BodyPartItem> part(String mob, PartType type) {
        Map<PartType, DeferredItem<BodyPartItem>> byType = BODY_PARTS.get(mob);
        DeferredItem<BodyPartItem> item = byType == null ? null : byType.get(type);
        if (item == null) {
            throw new IllegalArgumentException("No " + type.getSerializedName() + " part for " + mob);
        }
        return item;
    }

    private static DeferredItem<Item> organ(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().food(ORGAN_FOOD));
    }

    private static <T extends net.minecraft.world.entity.Mob> DeferredItem<DeferredSpawnEggItem> egg(String name,
            DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<T>> type, int back, int front) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, back, front, new Item.Properties()));
    }
}
