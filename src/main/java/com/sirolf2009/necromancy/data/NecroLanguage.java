package com.sirolf2009.necromancy.data;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartTrait;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroTags;
import com.sirolf2009.necromancy.registry.NecroVillagers;
import com.sirolf2009.necromancy.soul.SoulPotency;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * en_us.json. Each area has its own method so keys can be added next to their neighbours. The run fails if an item,
 * block, entity, necro mob or part trait has no English name, so new content cannot ship untranslated.
 */
public class NecroLanguage extends LanguageProvider {
    private final Set<String> written = new HashSet<>();

    public NecroLanguage(PackOutput output) {
        super(output, Necromancy.MOD_ID, "en_us");
    }

    @Override
    public void add(String key, String value) {
        written.add(key);
        super.add(key, value);
    }

    @Override
    protected void addTranslations() {
        items();
        bodyParts();
        blocks();
        entities();
        necroMobs();
        traits();
        tooltips();
        souls();
        gui();
        messages();
        commands();
        creativeTabs();
        keyBindings();
        sounds();
        advancements();
        enchantments();
        tags();
        config();
        jei();
        fragments(this::add, "en_us");
        checkComplete();
    }

    // ------------------------------------------------------------------ items

    private void items() {
        addItem(NecroItems.NECRONOMICON, "Necronomicon");
        addItem(NecroItems.BONE_NEEDLE, "Bone Needle");
        addItem(NecroItems.SOUL_IN_A_JAR, "Soul in a Jar");
        addItem(NecroItems.JAR_OF_BLOOD, "Jar of Blood");
        addItem(NecroItems.BRAIN_ON_A_STICK, "Brain on a Stick");
        addItem(NecroItems.BLOOD_SCYTHE, "Blood Scythe");
        addItem(NecroItems.BONE_SCYTHE, "Bone Scythe");
        addItem(NecroItems.BLOOD_BUCKET, "Bucket of Blood");
        addItem(NecroItems.ISAACS_HEAD, "Isaac's Head");
        addItem(NecroItems.SOUL_HEART, "Soul Heart");
        addItem(NecroItems.BRAINS, "Brains");
        addItem(NecroItems.HEART, "Heart");
        addItem(NecroItems.MUSCLE, "Muscle");
        addItem(NecroItems.LUNGS, "Lungs");
        addItem(NecroItems.SKIN, "Skin");
        addItem(NecroItems.TEAR, "Tear");
        addItem(NecroItems.BLOOD_TEAR, "Blood Tear");
        addItem(NecroItems.TEDDY_SPAWN_EGG, "Necromantic Teddy Spawn Egg");
        addItem(NecroItems.NIGHT_CRAWLER_SPAWN_EGG, "Night Crawler Spawn Egg");
        addItem(NecroItems.ISAAC_SPAWN_EGG, "Isaac Spawn Egg");
        addItem(NecroItems.BLOOD_ISAAC_SPAWN_EGG, "Blood Isaac Spawn Egg");
        addItem(NecroItems.ISAAC_HEAD_SPAWN_EGG, "Isaac's Head Spawn Egg");
        addItem(NecroItems.ISAAC_BODY_SPAWN_EGG, "Isaac's Body Spawn Egg");
        // the Sewing Machine item is named by its block
    }

    /** "Cow Torso", "Zombified Piglin Arm", ...: every sewn part is "<mob> <part>". */
    private void bodyParts() {
        NecroItems.BODY_PARTS.forEach((mob, byType) -> byType.forEach((type, item) -> addItem(item, mobName(mob) + " " + partName(type))));
    }

    private static String partName(PartType type) {
        return switch (type) {
            case HEAD -> "Head";
            case TORSO -> "Torso";
            case ARM -> "Arm";
            case LEGS -> "Legs";
        };
    }

    // ------------------------------------------------------------------ blocks and fluids

    private void blocks() {
        addBlock(NecroBlocks.SUMMONING_ALTAR, "Summoning Altar");
        addBlock(NecroBlocks.ALTAR_SIDE, "Summoning Altar Side");
        addBlock(NecroBlocks.SEWING_MACHINE, "Sewing Machine");
        addBlock(NecroBlocks.BLOOD, "Blood");
        add("fluid_type.necromancy.blood", "Blood");
    }

    // ------------------------------------------------------------------ entities and villagers

    private void entities() {
        addEntityType(NecroEntities.MINION, "Minion");
        addEntityType(NecroEntities.TEDDY, "Necromantic Teddy");
        addEntityType(NecroEntities.NIGHT_CRAWLER, "Night Crawler");
        addEntityType(NecroEntities.ISAAC, "Isaac");
        addEntityType(NecroEntities.BLOOD_ISAAC, "Blood Isaac");
        addEntityType(NecroEntities.ISAAC_HEAD, "Isaac's Head");
        addEntityType(NecroEntities.ISAAC_BODY, "Isaac's Body");
        addEntityType(NecroEntities.TEAR, "Tear");
        addEntityType(NecroEntities.BLOOD_TEAR, "Blood Tear");
        // Villager.getTypeName(): entity.minecraft.villager.<namespace>.<profession> for modded professions
        add("entity.minecraft.villager." + NecroVillagers.NECROMANCER.getId().getNamespace() + "." + NecroVillagers.NECROMANCER.getId().getPath(),
                "Necromancer");
    }

    // ------------------------------------------------------------------ necro mobs (tooltips, altar screen)

    /** Display names that plain title case would get wrong. */
    private static final Map<String, String> MOB_NAMES = Map.of(
            "isaac", "Isaac");

    private static String mobName(String id) {
        String name = MOB_NAMES.get(id);
        if (name != null) {
            return name;
        }
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }

    private void necroMobs() {
        for (NecroMob mob : NecroMobRegistry.all()) {
            add(mob.translationKey(), mobName(mob.id().getPath()));
        }
    }

    // ------------------------------------------------------------------ part traits

    private void traits() {
        // passive flags
        trait("climber", "Climber", "Climbs walls like a spider.");
        trait("sunburn", "Sunburn", "Burns in daylight unless it wears a helmet.");
        trait("sun_proof", "Sun-Proof", "Never burns in daylight, whatever else it was sewn from.");
        trait("water_hurts", "Hydrophobic", "Water and rain sear its flesh.");
        trait("fire_proof", "Fireproof", "With a fireproof torso and fireproof legs, the Minion cannot burn.");
        trait("thinker", "Thinker", "A head that still thinks: a Minion with it can learn a trade.");
        trait("heavy", "Heavy", "Too heavy to swim; it sinks like a stone.");
        trait("wings", "Wings", "Flapping wings: falls slowly and takes less fall damage.");
        trait("archer", "Archer", "Looses arrows from a held bow.");
        trait("trident_thrower", "Trident Thrower", "Hurls a held trident at its foes.");
        trait("poison_archer", "Poison Archer", "Arrows from a held bow carry poison.");
        // on hit
        trait("venom", "Venom", "Every hit poisons the target.");
        trait("hunger_bite", "Rotten Bite", "Hits may leave the target sick with Hunger.");
        trait("husk_bite", "Parching Bite", "Every hit leaves the target sick with Hunger.");
        trait("withering", "Withering", "Every hit withers the target.");
        trait("shadow_touch", "Shadow Touch", "Every hit withers the target for a moment.");
        trait("golem_toss", "Golem Toss", "Hits fling the target into the air.");
        trait("pack_hunter", "Pack Hunter", "+1 damage for each other wolf-headed Minion of yours nearby, up to +3.");
        trait("gold_fighter", "Gold Fighter", "+2 damage while wielding a golden sword or axe.");
        trait("ram", "Ram", "Charges: the first hit after a rest deals half again and knocks the target far back.");
        // when hurt
        trait("ink_cloud", "Ink Cloud", "When struck, may blind the attacker with ink.");
        trait("bouncy", "Bouncy", "When struck, may bounce the attacker away.");
        trait("roll_up", "Roll Up", "Near death, curls up and shrugs off harm for a few seconds. Once a minute.");
        // on death
        trait("volatile", "Volatile", "Explodes when the Minion dies.");
        // every tick
        trait("tears", "Tears", "Weeps tears at its target; one in five is a tear of blood.");
        trait("blink", "Blink", "Teleports beside a distant target.");
        trait("fireballs", "Fireballs", "Hurls fireballs at targets 4 to 16 blocks away.");
        trait("snowballs", "Snowballs", "Pelts nearby targets with snowballs.");
        trait("melts", "Melts", "Melts in hot biomes, and water hurts it.");
        trait("potion_thrower", "Potion Thrower", "Throws splash potions of Slowness, Weakness or Poison. Never Harming.");
        trait("dark_brew", "Dark Brew", "Below half health, drinks a potion of Harming, which heals the undead.");
        trait("lays_eggs", "Lays Eggs", "Lays an egg every five to ten minutes.");
        // interactions
        trait("milkable", "Milkable", "Milk it with a bucket, once a minute.");
        trait("stew", "Stew", "A bowl gives mushroom stew, once every five minutes.");
        trait("woolly", "Woolly", "Shear it for wool; it grows back in two minutes.");
    }

    private void trait(String id, String name, String description) {
        add("trait." + Necromancy.MOD_ID + "." + id, name);
        add("trait." + Necromancy.MOD_ID + "." + id + ".desc", description);
    }

    // ------------------------------------------------------------------ tooltips

    private void tooltips() {
        for (PartType type : PartType.values()) {
            add("tooltip.necromancy.part." + type.getSerializedName(), partName(type) + " part: %s");
        }
        add("tooltip.necromancy.stat.health", "%s Health");
        add("tooltip.necromancy.stat.attack", "%s Attack Damage");
        add("tooltip.necromancy.stat.speed", "%s Speed");
        add("tooltip.necromancy.stat.knockback", "%s Knockback Resistance");
        add("tooltip.necromancy.stat.follow", "%s Follow Range");
        add("tooltip.necromancy.per_arm", "%s per arm");
        add("tooltip.necromancy.saddleable", "Can be saddled and ridden");
        add("tooltip.necromancy.weight", "Weight %s");
        add("tooltip.necromancy.isaacs_head", "Still warm. Wear it, and weep tears at your foes.");
        add("tooltip.necromancy.scythe", "A killing blow bottles the victim's soul in an empty Glass Bottle");
        add("tooltip.necromancy.soul_heart", "Set it on the ground, and something will crawl out of it");
        add("tooltip.necromancy.soul.cap", "Can animate a body of weight %s or less");
        add("tooltip.necromancy.soul.legendary", "The soul of a legend: strong enough for any body");
    }

    // ------------------------------------------------------------------ souls

    private void souls() {
        add("soul.necromancy.of", "Soul of %s (%s)");
        add("soul.necromancy.nameless", "A nameless soul (%s)");
        for (SoulPotency potency : SoulPotency.values()) {
            String name = potency.getSerializedName();
            add("soul.necromancy.potency." + name, name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
        }
    }

    // ------------------------------------------------------------------ screens

    private void gui() {
        add("container.necromancy.summoning_altar", "Summoning Altar");
        add("container.necromancy.sewing_machine", "Sewing Machine");
        add("gui.necromancy.altar.blood", "Blood");
        add("gui.necromancy.altar.soul", "Soul");
        add("gui.necromancy.altar.raise", "Raise");
        add("gui.necromancy.altar.raise.tooltip", "Bind the soul to the body and raise your Minion. The body, the blood and the soul are used up.");
        add("gui.necromancy.altar.weight", "Weight %s / %s");
        add("gui.necromancy.altar.shrine", "Shrine %s (%s pts)");
        add("gui.necromancy.altar.tier.1", "I");
        add("gui.necromancy.altar.tier.2", "II");
        add("gui.necromancy.altar.tier.3", "III");
        add("gui.necromancy.altar.minions", "Minions %s / %s");
        add("gui.necromancy.sewing.needle", "Needle");
        add("gui.necromancy.sewing.thread", "Thread");
        add("gui.necromancy.sewing.not_ready", "Needs needle and thread");
    }

    // ------------------------------------------------------------------ chat and action bar messages

    private void messages() {
        add("message.necromancy.altar.needs_blood_and_soul", "The altar thirsts: it needs a Jar of Blood and a Soul in a Jar");
        add("message.necromancy.altar.needs_body", "Lay at least a head, a torso and legs on the altar");
        add("message.necromancy.altar.too_heavy", "This body weighs %s; the soul can only carry %s");
        add("message.necromancy.altar.too_many", "The dead refuse you: you hold as many Minions as your shrine allows");
        add("message.necromancy.minion.bidding", "%s: Your bidding, master?");
        add("message.necromancy.minion.obey_only", "%s obeys only %s");
        add("message.necromancy.minion.following", "%s follows you");
        add("message.necromancy.minion.staying", "%s stays here");
        add("message.necromancy.need_bottle", "You need an empty Glass Bottle to catch the blood");
        add("message.necromancy.teddy.walking", "Teddy follows you");
        add("message.necromancy.teddy.defending", "Teddy stands guard");
        add("message.necromancy.teddy.sitting", "Teddy sits still");
    }

    // ------------------------------------------------------------------ /minion

    private void commands() {
        add("commands.necromancy.minion.aggressive", "Your Minions now attack every player who is not your friend");
        add("commands.necromancy.minion.passive", "Your Minions now leave other players alone, unless they are your enemies");
        add("commands.necromancy.minion.friend", "Your Minions will never attack %s");
        add("commands.necromancy.minion.enemy", "Your Minions will hunt %s on sight");
        add("commands.necromancy.minion.forget", "Your Minions have forgotten %s");
        add("commands.necromancy.minion.list.header", "You have %s Minions (%s)");
        add("commands.necromancy.minion.mode.aggressive", "aggressive");
        add("commands.necromancy.minion.mode.passive", "passive");
        add("commands.necromancy.minion.list.friend", "Friend: %s");
        add("commands.necromancy.minion.list.enemy", "Enemy: %s");
        add("commands.necromancy.minion.sit", "%s Minions sit and wait");
        add("commands.necromancy.minion.follow", "%s Minions follow you");
    }

    // ------------------------------------------------------------------ creative tabs, keys, sounds

    private void creativeTabs() {
        add("itemGroup.necromancy", "Necromancy");
        add("itemGroup.necromancy.body_parts", "Body Parts");
    }

    private void keyBindings() {
        add("key.categories.necromancy", "Necromancy");
        add("key.necromancy.shoot_tear", "Weep a Tear (Isaac's Head)");
        add("key.necromancy.shoot_blood_tear", "Weep a Blood Tear (Isaac's Head)");
    }

    private void sounds() {
        add("subtitles.necromancy.spawn", "Minion rises");
        add("subtitles.necromancy.tear", "Tear splashes");
        add("subtitles.necromancy.nightcrawler.howl", "Night Crawler howls");
        add("subtitles.necromancy.nightcrawler.scream", "Night Crawler screams");
    }

    // ------------------------------------------------------------------ advancements (NecroAdvancements)

    private void advancements() {
        advancement("root", "Forbidden Knowledge", "Bind leather, blood and ink into a Necronomicon");
        advancement("bound_in_blood", "Bound in Blood", "Use the Necronomicon on planks with two cobblestone in a row beside them");
        advancement("its_alive", "It's Alive!", "Raise a Minion on the Summoning Altar");
        advancement("stitches", "Stitches", "Build a Sewing Machine");
        advancement("blood_bank", "Blood Bank", "Fill a bucket with blood");
        advancement("soul_reaper", "Soul Reaper", "Catch a soul in a Glass Bottle with a killing blow of a scythe");
        advancement("night_terror", "Night Terror", "Kill a Night Crawler");
        advancement("crying_shame", "Crying Shame", "Put an end to Blood Isaac's tears");
        advancement("basement_dweller", "Basement Dweller", "Claim Isaac's Head");
    }

    private void advancement(String name, String title, String description) {
        add("advancements.necromancy." + name + ".title", title);
        add("advancements.necromancy." + name + ".description", description);
    }

    // ------------------------------------------------------------------ enchantments

    private void enchantments() {
        add(Util.makeDescriptionId("enchantment", NecroEnchantments.REAPING.location()), "Reaping");
        add(Util.makeDescriptionId("enchantment", NecroEnchantments.REAPING.location()) + ".desc",
                "Creatures slain with the scythe give up their organs more often.");
    }

    // ------------------------------------------------------------------ tag names (recipe viewers)

    private void tags() {
        add(NecroTags.SCYTHES, "Scythes");
        add(NecroTags.BODY_PARTS, "Body Parts");
        add(NecroTags.ORGANS, "Organs");
        add(NecroTags.SEWING_THREAD, "Sewing Thread");
    }

    // ------------------------------------------------------------------ config (NeoForge config screen)

    private void config() {
        add("necromancy.configuration.title", "Necromancy Configuration");
        add("necromancy.configuration.spawning", "Spawning");
        add("necromancy.configuration.rarityNightCrawlers", "Night Crawler Rarity");
        add("necromancy.configuration.rarityIsaacs", "Isaac Rarity");
        add("necromancy.configuration.minions", "Minions");
        add("necromancy.configuration.useShrineTiers", "Shrine Tiers");
        add("necromancy.configuration.shrineTierCaps", "Minions per Shrine Tier");
        add("necromancy.configuration.maxMinionsPerPlayer", "Max Minions per Player");
        add("necromancy.configuration.soulPotency", "Soul Potency");
        add("necromancy.configuration.minionChat", "Minion Chat");
        add("necromancy.configuration.balance", "Balance");
        add("necromancy.configuration.classicStats", "Classic 1.7.1 Stats");
        add("necromancy.configuration.creeperTorsoBreaksBlocks", "Creeper Torso Breaks Blocks");
        add("necromancy.configuration.drops", "Drops");
        add("necromancy.configuration.organDropMultiplier", "Organ Drop Multiplier");
    }

    // ------------------------------------------------------------------ JEI (compat.jei)

    private void jei() {
        add("jei.necromancy.category.sewing", "Sewing");
        add("jei.necromancy.sewing.needle", "Needs a Bone Needle in the needle slot; each stitch wears it down");
        add("jei.necromancy.sewing.thread", "Uses up one String per stitch");
        add("jei.necromancy.info.altar",
                "To build a Summoning Altar, place any planks with two cobblestone in a row beside them, then use the Necronomicon on the planks. "
                        + "The book is consumed and the three blocks become the altar.\n\n"
                        + "Lay a head, a torso, arms and legs on it, add a Jar of Blood and a Soul in a Jar, and raise your Minion. "
                        + "Candles, soul lights and mob heads around the altar let you keep more Minions.");
        add("jei.necromancy.info.organs",
                "Organs come from creatures killed by you or your Minions: brains, hearts, muscle and lungs. "
                        + "Skeletons, golems, slimes and spirits have none. "
                        + "Some creatures are richer in one organ than others: villagers and illagers in brains, hoglins in hearts, ghasts in lungs.");
        add("jei.necromancy.info.skin", "Skin is cut from leather on the Sewing Machine: one leather gives eight pieces.");
        add("jei.necromancy.info.soul_in_a_jar",
                "Carry an empty Glass Bottle and land the killing blow with a scythe: the creature's soul is caught in the bottle. "
                        + "Stronger creatures give stronger souls, which can animate heavier bodies.");
        add("jei.necromancy.info.jar_of_blood", "Use a Bone Needle on a living creature while carrying an empty Glass Bottle to draw a Jar of Blood.");
    }

    // ------------------------------------------------------------------ completeness check

    /**
     * Adds the hand-written key files in {@code src/main/lang-src/<lang>/} (the guide book and later features). The data
     * run passes the folder as the {@code necromancy.langsrc} system property.
     */
    static void fragments(java.util.function.BiConsumer<String, String> add, String lang) {
        java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("necromancy.langsrc", "src/main/lang-src"), lang);
        if (!java.nio.file.Files.isDirectory(dir)) {
            throw new IllegalStateException("Language fragments not found: " + dir.toAbsolutePath());
        }
        try (var files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                json.entrySet().forEach(e -> add.accept(e.getKey(), e.getValue().getAsString()));
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** Another language built only from its fragment files (Brazilian Portuguese). */
    public static class Fragments extends LanguageProvider {
        private final String lang;

        public Fragments(PackOutput output, String lang) {
            super(output, Necromancy.MOD_ID, lang);
            this.lang = lang;
        }

        @Override
        protected void addTranslations() {
            fragments(this::add, lang);
        }
    }

    private void checkComplete() {
        Set<String> missing = new TreeSet<>();
        for (Holder<Item> item : NecroItems.ITEMS.getEntries()) {
            need(missing, item.value().getDescriptionId());
        }
        for (Holder<Block> block : NecroBlocks.BLOCKS.getEntries()) {
            need(missing, block.value().getDescriptionId());
        }
        for (Holder<EntityType<?>> type : NecroEntities.ENTITIES.getEntries()) {
            need(missing, type.value().getDescriptionId());
        }
        for (NecroMob mob : NecroMobRegistry.all()) {
            need(missing, mob.translationKey());
            for (PartType type : PartType.values()) {
                for (PartTrait trait : mob.traits(type)) {
                    String key = "trait." + trait.id().getNamespace() + "." + trait.id().getPath();
                    need(missing, key);
                    need(missing, key + ".desc");
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing English text for " + List.copyOf(missing));
        }
    }

    private void need(Set<String> missing, String key) {
        if (!written.contains(key)) {
            missing.add(key);
        }
    }
}
