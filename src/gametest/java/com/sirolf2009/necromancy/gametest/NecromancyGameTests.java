package com.sirolf2009.necromancy.gametest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.block.AltarBlock;
import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.MinionOrder;
import com.sirolf2009.necromancy.item.NecronomiconItem;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroComponents;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroRecipes;
import com.sirolf2009.necromancy.soul.MinionBuild;
import com.sirolf2009.necromancy.soul.Shrine;
import com.sirolf2009.necromancy.soul.SoulData;
import com.sirolf2009.necromancy.soul.SoulPotency;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Necromancy.MOD_ID)
@PrefixGameTestTemplate(false)
public class NecromancyGameTests {
    private static final BlockPos FLOOR = new BlockPos(7, 1, 7);

    private static ResourceLocation id(String path) {
        return Necromancy.id(path);
    }

    private static MinionEntity minion(GameTestHelper h, BlockPos pos, Map<BodyPartLocation, ResourceLocation> parts) {
        MinionEntity minion = h.spawn(NecroEntities.MINION.get(), pos);
        minion.setParts(parts);
        minion.applyPartAttributes(true);
        return minion;
    }

    private static Map<BodyPartLocation, ResourceLocation> body(String head, String torso, String arms, String legs) {
        Map<BodyPartLocation, ResourceLocation> map = new EnumMap<>(BodyPartLocation.class);
        if (head != null) {
            map.put(BodyPartLocation.HEAD, id(head));
        }
        if (torso != null) {
            map.put(BodyPartLocation.TORSO, id(torso));
        }
        if (arms != null) {
            map.put(BodyPartLocation.ARM_LEFT, id(arms));
            map.put(BodyPartLocation.ARM_RIGHT, id(arms));
        }
        if (legs != null) {
            map.put(BodyPartLocation.LEGS, id(legs));
        }
        return map;
    }

    /** Every necro mob part resolves to a registered item, and part items map back to their mob. */
    @GameTest(template = "platform")
    public static void everyPartHasAnItem(GameTestHelper h) {
        int parts = 0;
        for (NecroMob mob : NecroMobRegistry.all()) {
            for (PartType type : PartType.values()) {
                if (mob.hasPart(type)) {
                    ItemStack stack = mob.partStack(type);
                    h.assertFalse(stack.isEmpty(), mob.id() + " " + type + " has no item");
                    var ref = NecroMobRegistry.partOf(stack).orElseThrow(() -> new AssertionError(mob.id() + " " + type + " does not map back"));
                    h.assertTrue(ref.type() == type, "part type of " + stack);
                    parts++;
                }
            }
        }
        h.assertTrue(NecroMobRegistry.all().size() >= 28, "expected 28 necro mobs, got " + NecroMobRegistry.all().size());
        h.assertTrue(parts > 90, "only " + parts + " parts");
        h.succeed();
    }

    /** Every sewn part has a Sewing Machine recipe that makes it. */
    @GameTest(template = "platform")
    public static void everySewnPartHasARecipe(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(NecroRecipes.SEWING.get());
        List<String> missing = new ArrayList<>();
        for (NecroMob mob : NecroMobRegistry.all()) {
            for (PartType type : PartType.values()) {
                if (mob.hasPart(type) && mob.isSewable(type)) {
                    boolean found = recipes.stream().anyMatch(r -> NecroMobRegistry.partOf(r.value().getResultItem(h.getLevel().registryAccess()))
                            .map(p -> p.mob() == mob && p.type() == type).orElse(false));
                    if (!found) {
                        missing.add(mob.id() + "/" + type.getSerializedName());
                    }
                }
            }
        }
        h.assertTrue(missing.isEmpty(), missing.size() + " parts have no sewing recipe, e.g. " + missing.subList(0, Math.min(3, missing.size())));
        h.succeed();
    }

    /** The zombie torso shape from the original mod matches on the 4x4 grid. */
    @GameTest(template = "platform")
    public static void sewingGridMatchesTheOriginalShape(GameTestHelper h) {
        TransientCraftingContainer grid = new TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 4, 4);
        String[] rows = { " LL ", "BHUB", "LEEL", "BLLB" };
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                ItemStack stack = switch (rows[y].charAt(x)) {
                    case 'L' -> new ItemStack(NecroItems.SKIN.get());
                    case 'B' -> new ItemStack(Items.BONE);
                    case 'H' -> new ItemStack(NecroItems.HEART.get());
                    case 'U' -> new ItemStack(NecroItems.LUNGS.get());
                    case 'E' -> new ItemStack(Items.ROTTEN_FLESH);
                    default -> ItemStack.EMPTY;
                };
                grid.setItem(x + y * 4, stack);
            }
        }
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(NecroRecipes.SEWING.get(), grid.asCraftInput(), h.getLevel());
        h.assertTrue(recipe.isPresent(), "zombie torso recipe did not match");
        h.assertTrue(recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(NecroItems.part("zombie", PartType.TORSO).get()), "wrong result");
        h.succeed();
    }

    /** The Necronomicon binds planks and two cobblestone into an altar; breaking a side gives the blocks back. */
    @GameTest(template = "platform")
    public static void necronomiconBindsAndUnbindsTheAltar(GameTestHelper h) {
        BlockPos base = FLOOR.above();
        h.setBlock(base, Blocks.SPRUCE_PLANKS);
        h.setBlock(base.east(), Blocks.COBBLESTONE);
        h.setBlock(base.east(2), Blocks.COBBLESTONE);
        NecronomiconItem.bind(h.getLevel(), h.absolutePos(base), Direction.EAST);
        h.assertBlockPresent(NecroBlocks.SUMMONING_ALTAR.get(), base);
        h.assertBlockPresent(NecroBlocks.ALTAR_SIDE.get(), base.east());
        h.assertBlockPresent(NecroBlocks.ALTAR_SIDE.get(), base.east(2));
        h.assertTrue(h.getBlockState(base).getValue(AltarBlock.FACING) == Direction.EAST, "altar faces the sides");
        h.destroyBlock(base.east(2));
        h.assertBlockPresent(Blocks.SPRUCE_PLANKS, base);
        h.assertBlockPresent(Blocks.COBBLESTONE, base.east());
        h.succeed();
    }

    /** A full zombie body summed with the original operation-1 modifiers: 20 x (1 + 1 + 2 + 0.5 + 0.5 + 1) = 120 health. */
    @GameTest(template = "platform")
    public static void partStatsAddUp(GameTestHelper h) {
        MinionEntity zombie = minion(h, FLOOR.above(), body("zombie", "zombie", "zombie", "zombie"));
        h.assertTrue(Math.abs(zombie.getMaxHealth() - 120F) < 0.01F, "zombie max health " + zombie.getMaxHealth());
        h.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) - 6.0) < 0.01, "zombie damage " + zombie.getAttributeValue(Attributes.ATTACK_DAMAGE));
        h.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.26) < 0.001, "zombie speed " + zombie.getAttributeValue(Attributes.MOVEMENT_SPEED));
        h.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.ARMOR) - 2.0) < 0.01, "zombie torso armour");
        MinionEntity golem = minion(h, FLOOR.above().east(3), body("iron_golem", "iron_golem", "iron_golem", "iron_golem"));
        h.assertTrue(Math.abs(golem.getAttributeValue(Attributes.ARMOR) - 12.0) < 0.01, "golem armour " + golem.getAttributeValue(Attributes.ARMOR));
        MinionEntity chicken = minion(h, FLOOR.above().west(3), body("chicken", "chicken", "chicken", "chicken"));
        h.assertTrue(chicken.getAttributeValue(Attributes.GRAVITY) < 0.08 - 0.039, "chicken wings lower gravity");
        h.succeed();
    }

    /** Speed is capped at 0.35 outside classic mode. */
    @GameTest(template = "platform")
    public static void speedIsCapped(GameTestHelper h) {
        MinionEntity fast = minion(h, FLOOR.above(), body("enderman", "enderman", null, "night_crawler"));
        h.assertTrue(fast.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.3501, "speed " + fast.getAttributeValue(Attributes.MOVEMENT_SPEED));
        h.succeed();
    }

    /** A server player for the altar (it needs one), removed again when the test ends. */
    private static net.minecraft.server.level.ServerPlayer raiser(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(h.absoluteVec(FLOOR.above(2).getCenter()));
        CLEANUP.add(player);
        return player;
    }

    private static final List<net.minecraft.server.level.ServerPlayer> CLEANUP = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Mock players left by earlier tests are removed before the next altar test (so they never pile up). */
    private static void removeMockPlayers(GameTestHelper h) {
        for (var player : CLEANUP) {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        CLEANUP.clear();
    }

    private static AltarBlockEntity altar(GameTestHelper h) {
        removeMockPlayers(h);
        BlockPos base = FLOOR.above();
        h.setBlock(base, Blocks.OAK_PLANKS);
        h.setBlock(base.east(), Blocks.COBBLESTONE);
        h.setBlock(base.east(2), Blocks.COBBLESTONE);
        NecronomiconItem.bind(h.getLevel(), h.absolutePos(base), Direction.EAST);
        return (AltarBlockEntity) h.getBlockEntity(base);
    }

    private static void fill(AltarBlockEntity altar, SoulPotency potency, String head, String torso, String legs) {
        altar.setItem(AltarBlockEntity.BLOOD, new ItemStack(NecroItems.JAR_OF_BLOOD.get()));
        ItemStack soul = new ItemStack(NecroItems.SOUL_IN_A_JAR.get());
        soul.set(NecroComponents.SOUL.get(), SoulData.of(EntityType.ZOMBIE, potency));
        altar.setItem(AltarBlockEntity.SOUL, soul);
        altar.setItem(AltarBlockEntity.HEAD, head.equals("zombie") ? new ItemStack(Items.ZOMBIE_HEAD) : new ItemStack(NecroItems.part(head, PartType.HEAD).get()));
        altar.setItem(AltarBlockEntity.TORSO, new ItemStack(NecroItems.part(torso, PartType.TORSO).get()));
        altar.setItem(AltarBlockEntity.LEGS, new ItemStack(NecroItems.part(legs, PartType.LEGS).get()));
    }

    /** Blood, a soul and a body raise a tamed Minion and use the items up. */
    @GameTest(template = "platform")
    public static void altarRaisesAMinion(GameTestHelper h) {
        AltarBlockEntity altar = altar(h);
        fill(altar, SoulPotency.COMMON, "zombie", "zombie", "zombie");
        Player player = raiser(h);
        h.assertTrue(altar.tryRaise(player), "raise failed");
        List<MinionEntity> minions = h.getLevel().getEntitiesOfClass(MinionEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(FLOOR)).inflate(4));
        h.assertTrue(minions.size() == 1, "expected one minion, got " + minions.size());
        MinionEntity minion = minions.get(0);
        h.assertTrue(minion.isOwnedBy(player), "minion is not tamed to the raiser");
        h.assertTrue(minion.parts().size() == 3, "parts " + minion.parts());
        h.assertTrue(altar.getItem(AltarBlockEntity.SOUL).isEmpty() && altar.getItem(AltarBlockEntity.HEAD).isEmpty(), "items not used up");
        h.succeed();
    }

    /** A Faint soul (cap 6) cannot lift three Iron Golem parts (weight 12); nothing is used up. */
    @GameTest(template = "platform")
    public static void weakSoulsCannotLiftHeavyBodies(GameTestHelper h) {
        AltarBlockEntity altar = altar(h);
        fill(altar, SoulPotency.FAINT, "iron_golem", "iron_golem", "iron_golem");
        h.assertTrue(altar.bodyWeight() == 12, "weight " + altar.bodyWeight());
        h.assertFalse(altar.tryRaise(raiser(h)), "a faint soul raised a golem");
        h.assertFalse(altar.getItem(AltarBlockEntity.SOUL).isEmpty(), "soul was used up");
        h.succeed();
    }

    /** Candles, soul lights and heads around the altar raise its tier. */
    @GameTest(template = "platform")
    public static void shrineTiersCountDecor(GameTestHelper h) {
        AltarBlockEntity altar = altar(h);
        BlockPos base = FLOOR.above();
        h.assertTrue(Shrine.scan(h.getLevel(), h.absolutePos(base)).tier() == 1, "bare altar should be tier I");
        for (int i = 0; i < 2; i++) {
            h.setBlock(base.north(2).west(i), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        }
        h.setBlock(base.south(2), Blocks.SOUL_LANTERN);
        h.setBlock(base.south(2).west(), Blocks.SOUL_LANTERN);
        var state = Shrine.scan(h.getLevel(), h.absolutePos(base));
        h.assertTrue(state.points() == 10 && state.tier() == 2, "candles and lanterns: " + state);
        for (int i = 0; i < 7; i++) {
            h.setBlock(base.west(2).north(i - 3), Blocks.WITHER_SKELETON_SKULL);
        }
        state = Shrine.scan(h.getLevel(), h.absolutePos(base));
        h.assertTrue(state.tier() == 3, "skulls should reach tier III: " + state);
        h.assertTrue(altar.shrine().tier() == 3 && altar.shrine().minionCap() == 8, "the altar reads its own shrine: " + altar.shrine());
        h.succeed();
    }

    /** A Phylactery-bound Minion leaves Remains that remember its body, soul and kills. */
    @GameTest(template = "platform")
    public static void boundMinionsLeaveRemains(GameTestHelper h) {
        MinionEntity minion = minion(h, FLOOR.above(), body("zombie", "cow", "skeleton", "spider"));
        minion.setSoul(SoulData.of(EntityType.COW, SoulPotency.LESSER));
        minion.setBound(true);
        for (int i = 0; i < 12; i++) {
            minion.addKill(h.spawn(EntityType.ZOMBIE, FLOOR.above().east(4)));
        }
        h.assertTrue(minion.rank() == 1, "12 kills should be rank 1, got " + minion.rank());
        minion.kill();
        h.runAfterDelay(5, () -> {
            List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(FLOOR)).inflate(4),
                    e -> e.getItem().is(NecroItems.MINION_REMAINS.get()));
            h.assertTrue(items.size() == 1, "expected remains, got " + items.size());
            MinionBuild build = items.get(0).getItem().get(NecroComponents.MINION_BUILD.get());
            h.assertTrue(build != null && build.locations().size() == 5 && build.kills() == 12, "build " + build);
            h.assertTrue(build.soul().potency() == SoulPotency.LESSER, "soul kept");
            h.succeed();
        });
    }

    /** Guard keeps a Minion near its post; Follow lets it go. */
    @GameTest(template = "platform")
    public static void ordersRestrictMovement(GameTestHelper h) {
        MinionEntity minion = minion(h, FLOOR.above(), body("zombie", "zombie", "zombie", "zombie"));
        BlockPos post = h.absolutePos(FLOOR.above().east(3));
        minion.giveOrder(MinionOrder.GUARD, post, List.of());
        h.assertTrue(minion.hasRestriction() && minion.getRestrictCenter().equals(post), "guard post");
        minion.giveOrder(MinionOrder.STAY, null, List.of());
        h.assertTrue(minion.isOrderedToSit(), "stay sits");
        minion.giveOrder(MinionOrder.FOLLOW, null, List.of());
        h.assertFalse(minion.hasRestriction() || minion.isOrderedToSit(), "follow frees it");
        h.succeed();
    }

    /** Remains in the soul slot raise the same Minion, named, with its kills. */
    @GameTest(template = "platform")
    public static void remainsRaiseTheSameMinion(GameTestHelper h) {
        AltarBlockEntity altar = altar(h);
        ItemStack remains = new ItemStack(NecroItems.MINION_REMAINS.get());
        remains.set(NecroComponents.MINION_BUILD.get(), MinionBuild.of(body("wolf", "zombie", "zombie", "zombie"), SoulData.of(EntityType.ZOMBIE, SoulPotency.COMMON),
                java.util.Optional.of(net.minecraft.network.chat.Component.literal("Rex")), 40));
        altar.setItem(AltarBlockEntity.BLOOD, new ItemStack(NecroItems.JAR_OF_BLOOD.get()));
        altar.setItem(AltarBlockEntity.SOUL, remains);
        h.assertTrue(altar.tryRaise(raiser(h)), "raise from remains failed");
        List<MinionEntity> minions = h.getLevel().getEntitiesOfClass(MinionEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(FLOOR)).inflate(4));
        h.assertTrue(minions.size() == 1, "minions " + minions.size());
        MinionEntity rex = minions.get(0);
        h.assertTrue(rex.hasCustomName() && rex.getCustomName().getString().equals("Rex"), "name kept");
        h.assertTrue(rex.kills() == 40 && rex.rank() == 2, "kills " + rex.kills() + " rank " + rex.rank());
        h.succeed();
    }

    /** Souls are weighed by the creature they came from. */
    @GameTest(template = "platform")
    public static void soulPotencyFollowsHealth(GameTestHelper h) {
        h.assertTrue(SoulPotency.ofHealth(4) == SoulPotency.FAINT, "chicken");
        h.assertTrue(SoulPotency.ofHealth(20) == SoulPotency.COMMON, "zombie");
        h.assertTrue(SoulPotency.ofHealth(40) == SoulPotency.POTENT, "enderman");
        h.assertTrue(SoulPotency.ofHealth(100) == SoulPotency.MIGHTY, "golem");
        h.assertTrue(SoulData.get(new ItemStack(NecroItems.SOUL_IN_A_JAR.get())).potency() == SoulPotency.COMMON, "a jar without data is common");
        h.succeed();
    }
}
