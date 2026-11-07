package com.sirolf2009.necromancy.compat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartStats;
import com.sirolf2009.necromancy.api.PartTrait;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.trait.DataTraits;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;

/**
 * OreSpawn's creatures as Minion parts: 97 of them, from {@code data/necromancy/compat/orespawn.json} (made from
 * docs/orespawn_parts_spec.json by tools/gen_orespawn.py). They are registered only when OreSpawn is installed; their
 * parts are the generic creature part item, sewn with OreSpawn's drops. Their names ({@code necromob.orespawn.*}) are
 * OreSpawn's English names and the Brazilian Portuguese names of the CrazyCraft pack.
 */
public final class OreSpawnCompat {
    public static final String MOD_ID = "orespawn";
    private static final String DATA = "/data/necromancy/compat/orespawn.json";

    private OreSpawnCompat() {
    }

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    public static void register() {
        if (!loaded()) {
            return;
        }
        JsonObject data = data();
        int parts = 0;
        for (JsonElement element : data.getAsJsonArray("mobs")) {
            NecroMob mob = build(element.getAsJsonObject());
            NecroMobRegistry.register(mob);
            parts += mob.parts().size();
        }
        Necromancy.LOGGER.info("OreSpawn found: {} creatures with {} body parts can be raised", data.getAsJsonArray("mobs").size(), parts);
    }

    /** The creature file, read from the mod jar. */
    public static JsonObject data() {
        try (InputStream in = OreSpawnCompat.class.getResourceAsStream(DATA)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + DATA);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read " + DATA, e);
        }
    }

    /** Entity tag additions for the data generator: tag id (path under necromancy) to OreSpawn entity ids. */
    public static Map<String, List<ResourceLocation>> tags() {
        Map<String, List<ResourceLocation>> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : data().getAsJsonObject("tags").entrySet()) {
            List<ResourceLocation> ids = new ArrayList<>();
            e.getValue().getAsJsonArray().forEach(id -> ids.add(ResourceLocation.parse(id.getAsString())));
            out.put(e.getKey().substring(e.getKey().indexOf(':') + 1), ids);
        }
        return out;
    }

    static NecroMob build(JsonObject json) {
        String name = json.get("mob").getAsString();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
        NecroMob mob = new NecroMob(id).weight(json.get("weight").getAsInt());
        var shape = json.getAsJsonArray("shape");
        mob.shape(shape.get(0).getAsFloat(), shape.get(1).getAsFloat(), shape.get(2).getAsFloat());
        JsonObject essence = json.getAsJsonObject("essence");
        mob.essence(ingredient(essence.get("default").getAsString()));
        JsonObject parts = json.getAsJsonObject("parts");
        boolean mounted = false;
        for (PartType type : PartType.values()) {
            JsonObject part = parts.has(type.getSerializedName()) ? parts.getAsJsonObject(type.getSerializedName()) : null;
            if (part == null) {
                continue;
            }
            mob.creaturePart(type);
            if (essence.has(type.getSerializedName())) {
                mob.essence(type, ingredient(essence.get(type.getSerializedName()).getAsString()));
            }
            var s = part.getAsJsonArray("stats");
            PartStats stats = PartStats.of(s.get(0).getAsDouble(), s.get(1).getAsDouble(), s.get(2).getAsDouble(), s.get(3).getAsDouble(), s.get(4).getAsDouble());
            switch (type) {
                case HEAD -> mob.stats(BodyPartLocation.HEAD, stats);
                case TORSO -> mob.stats(BodyPartLocation.TORSO, stats);
                case ARM -> mob.armStats(stats);
                case LEGS -> mob.stats(BodyPartLocation.LEGS, stats);
            }
            for (JsonElement a : part.getAsJsonArray("attributes")) {
                JsonObject attribute = a.getAsJsonObject();
                ResourceLocation attributeId = ResourceLocation.parse(attribute.get("attribute").getAsString());
                var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId)
                        .orElseThrow(() -> new IllegalArgumentException(id + ": unknown attribute " + attributeId));
                mob.attribute(type, holder, attribute.get("amount").getAsDouble());
            }
            for (PartTrait trait : DataTraits.parse(id, type, part.getAsJsonArray("traits"))) {
                mob.trait(type, trait);
            }
            for (JsonElement t : part.getAsJsonArray("traits")) {
                if (type == PartType.TORSO && t.getAsJsonObject().get("type").getAsString().equals("mount")) {
                    mob.mount(json.get("rider_offset").getAsFloat(), DataTraits.mount(t.getAsJsonObject()));
                    mounted = true;
                }
            }
        }
        if (!mounted && json.get("saddleable").getAsBoolean()) {
            mob.saddleable(json.get("rider_offset").getAsFloat());
        }
        JsonObject sounds = json.getAsJsonObject("sounds");
        mob.sounds(sound(sounds, "ambient"), sound(sounds, "hurt"), sound(sounds, "death"));
        return mob;
    }

    private static Supplier<Ingredient> ingredient(String id) {
        if (id.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1)));
            return () -> Ingredient.of(tag);
        }
        ResourceLocation item = ResourceLocation.parse(id);
        return () -> {
            Item found = BuiltInRegistries.ITEM.get(item);
            return found == Items.AIR ? Ingredient.EMPTY : Ingredient.of(found);
        };
    }

    private static Supplier<SoundEvent> sound(JsonObject sounds, String key) {
        if (!sounds.has(key) || sounds.get(key).isJsonNull()) {
            return () -> null;
        }
        ResourceLocation id = ResourceLocation.parse(sounds.get(key).getAsString());
        return () -> BuiltInRegistries.SOUND_EVENT.get(id);
    }
}
