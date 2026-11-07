package com.sirolf2009.necromancy.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Minion parts described in resource files, {@code assets/<namespace>/necro_parts/<name>.json}, for creatures of other
 * mods (OreSpawn's come with Necromancy). Each file names its mob and either a GeckoLib geometry ({@code "geo"}) or a
 * registered model layer ({@code "layer": "ns:path#layer"}), its texture, and for each part type the bones that form it
 * and the points where it plugs in. Every point is in classic model space: y down, the ground at y = 24, 1/16 block.
 *
 * <pre>{@code
 * { "mob": "orespawn:trex", "geo": "orespawn:geo/entity/trex.geo.json", "texture": "orespawn:textures/entity/trex.png",
 *   "scale": 0.45,
 *   "head":  { "groups": [ { "bones": ["shape1", "jaw"], "pivot": [0, -14, -20] } ], "plug": [0, -10, -22], "animator": "look" },
 *   "torso": { "groups": [ { "bones": ["shape4", "shape5"] } ], "plug": [0, 9, 10], "neck": [0, -10, -22],
 *              "shoulder_left": [5, -4, -12], "shoulder_right": [-5, -4, -12] },
 *   "arm":   { "left": { "bones": ["leftarm"] }, "right": { "bones": ["rightarm"] }, "plug_left": [5, -4, -12], "plug_right": [-5, -4, -12],
 *              "animator": "arm" },
 *   "legs":  { "groups": [ { "bones": ["leftleg", "leftleg2"], "pivot": [6, 2, 11] }, { "bones": ["rightleg", "rightleg2"], "pivot": [-6, 2, 11] } ],
 *              "hip": [0, 2, 11], "animator": "walk_pair" } }
 * }</pre>
 * A group with a {@code pivot} becomes one part turning about that point (its bones re-based onto it); without one,
 * each bone is its own part. A bone brings its child bones along, except children that another part (or the file's
 * {@code "hide"} list) names; a child bone named without its parent keeps its true place (its parents' poses come
 * along, without their cubes). A bone below another bone of the same group comes along with that one. Optional per part: {@code "scale"}, {@code "animator"}; per file: {@code "scale"} for
 * every part (multiplied with a part's own), {@code "translucent": true}, {@code "full_bright": true}.
 */
public final class DataPartModels extends SimpleJsonResourceReloadListener {
    private static final Map<String, PartAnimator> ANIMATORS = new LinkedHashMap<>();

    static {
        ANIMATORS.put("none", PartAnimator.NONE);
        ANIMATORS.put("look", PartAnimator.LOOK);
        ANIMATORS.put("look_first", PartAnimator.LOOK_FIRST);
        ANIMATORS.put("walk_pair", PartAnimator.WALK_PAIR);
        ANIMATORS.put("scuttle", PartAnimator.SCUTTLE);
        ANIMATORS.put("arm", PartAnimator.ARM);
        ANIMATORS.put("zombie_arm", PartAnimator.ZOMBIE_ARM);
        ANIMATORS.put("front_leg", PartAnimator.FRONT_LEG);
        ANIMATORS.put("spin", PartAnimator.SPIN);
        ANIMATORS.put("sway", PartAnimator.SWAY);
        ANIMATORS.put("tentacles", PartAnimator.TENTACLES);
        ANIMATORS.put("wobble", PartAnimator.WOBBLE);
        ANIMATORS.put("golem_arm", PartAnimator.GOLEM_ARM);
    }

    public DataPartModels() {
        super(new com.google.gson.Gson(), "necro_parts");
    }

    public static PartAnimator animator(String name) {
        PartAnimator animator = ANIMATORS.get(name);
        if (animator == null) {
            throw new IllegalArgumentException("Unknown animator '" + name + "', known: " + ANIMATORS.keySet());
        }
        return animator;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        GeoGeometry.invalidate();
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (json.has("requires") && !net.neoforged.fml.ModList.get().isLoaded(json.get("requires").getAsString())) {
                    continue;
                }
                ResourceLocation mob = ResourceLocation.parse(json.get("mob").getAsString());
                NecroMobModels.register(mob, builder -> build(entry.getKey(), json, builder));
                count++;
            } catch (RuntimeException e) {
                Necromancy.LOGGER.error("Minion part file {} is broken: {}", entry.getKey(), e.toString());
            }
        }
        NecroMobModels.invalidate();
        Necromancy.LOGGER.info("Loaded {} Minion part sets from resources", count);
    }

    static void build(ResourceLocation file, JsonObject json, NecroMobModels.Builder builder) {
        ResourceLocation texture = ResourceLocation.parse(json.get("texture").getAsString());
        Map<String, String> parents = new LinkedHashMap<>();
        Map<String, ModelPart> bones = bones(json, parents);
        java.util.Set<String> named = new java.util.HashSet<>();
        for (PartType type : PartType.values()) {
            if (json.has(type.getSerializedName())) {
                collectNames(json.getAsJsonObject(type.getSerializedName()), named);
            }
        }
        if (json.has("hide")) {
            json.getAsJsonArray("hide").forEach(e -> named.add(e.getAsString()));
        }
        Split split = new Split(bones, parents, named);
        boolean translucent = json.has("translucent") && json.get("translucent").getAsBoolean();
        float fileScale = json.has("scale") ? json.get("scale").getAsFloat() : 1F;
        for (PartType type : PartType.values()) {
            JsonObject p = json.has(type.getSerializedName()) ? json.getAsJsonObject(type.getSerializedName()) : null;
            if (p == null) {
                continue;
            }
            PartModel model;
            if (type == PartType.ARM) {
                List<ModelPart> left = group(split, p.getAsJsonObject("left"), file);
                List<ModelPart> right = group(split, p.getAsJsonObject("right"), file);
                float[] l = point(p, "plug_left");
                float[] r = point(p, "plug_right");
                model = PartModel.arms(texture, left, right).plugs(l[0], l[1], l[2], r[0], r[1], r[2]);
            } else {
                List<ModelPart> parts = new ArrayList<>();
                for (JsonElement g : p.getAsJsonArray("groups")) {
                    parts.addAll(group(split, g.getAsJsonObject(), file));
                }
                model = PartModel.of(texture, parts.toArray(ModelPart[]::new));
                if (p.has("plug")) {
                    model.plug(point(p, "plug"));
                }
                if (type == PartType.TORSO) {
                    float[] neck = point(p, "neck");
                    float[] l = point(p, "shoulder_left");
                    float[] r = point(p, "shoulder_right");
                    model.neck(neck[0], neck[1], neck[2]).shoulders(l[0], l[1], l[2], r[0], r[1], r[2]);
                }
                if (type == PartType.LEGS) {
                    float[] hip = point(p, "hip");
                    model.hip(hip[0], hip[1], hip[2]);
                }
            }
            if (p.has("animator")) {
                model.animate(animator(p.get("animator").getAsString()));
            } else {
                model.animate(switch (type) {
                    case HEAD -> PartAnimator.LOOK;
                    case ARM -> PartAnimator.ARM;
                    case LEGS -> PartAnimator.WALK_PAIR;
                    case TORSO -> PartAnimator.NONE;
                });
            }
            float partScale = fileScale * (p.has("scale") ? p.get("scale").getAsFloat() : 1F);
            if (partScale != 1F) {
                model.scale(partScale);
            }
            if (translucent) {
                model.renderType(RenderType::entityTranslucent);
            }
            if (json.has("full_bright") && json.get("full_bright").getAsBoolean()) {
                model.fullBright();
            }
            builder.put(type, model);
        }
    }

    private static void collectNames(JsonObject part, java.util.Set<String> into) {
        for (String key : List.of("groups", "left", "right")) {
            if (!part.has(key)) {
                continue;
            }
            JsonElement e = part.get(key);
            List<JsonObject> groups = new ArrayList<>();
            if (e.isJsonArray()) {
                e.getAsJsonArray().forEach(g -> groups.add(g.getAsJsonObject()));
            } else {
                groups.add(e.getAsJsonObject());
            }
            for (JsonObject g : groups) {
                g.getAsJsonArray("bones").forEach(b -> into.add(b.getAsString()));
            }
        }
    }

    private static Map<String, ModelPart> bones(JsonObject json, Map<String, String> parents) {
        if (json.has("geo")) {
            ResourceLocation geo = ResourceLocation.parse(json.get("geo").getAsString());
            parents.putAll(GeoGeometry.parents(geo));
            return GeoGeometry.bones(geo);
        }
        String[] layer = json.get("layer").getAsString().split("#");
        ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(new ModelLayerLocation(ResourceLocation.parse(layer[0]),
                layer.length > 1 ? layer[1] : "main"));
        Map<String, ModelPart> map = new LinkedHashMap<>();
        collect(root, null, map, parents);
        return map;
    }

    private static void collect(ModelPart part, @javax.annotation.Nullable String name, Map<String, ModelPart> into, Map<String, String> parents) {
        part.children.forEach((childName, child) -> {
            if (into.putIfAbsent(childName, child) == null && name != null) {
                parents.put(childName, name);
            }
            collect(child, childName, into, parents);
        });
    }

    /** Takes bones out of a model: each with its own children except those named elsewhere, at its true place. */
    record Split(Map<String, ModelPart> bones, Map<String, String> parents, java.util.Set<String> named) {
        ModelPart take(String name, java.util.Collection<String> keep) {
            ModelPart bone = prune(bones.get(name), keep);
            String child = name;
            for (String parent = parents.get(name); parent != null; parent = parents.get(parent)) {
                ModelPart wrapper = new ModelPart(List.of(), new java.util.HashMap<>(Map.of(child, bone)));
                wrapper.setInitialPose(bones.get(parent).getInitialPose());
                wrapper.resetPose();
                bone = wrapper;
                child = parent;
            }
            return bone;
        }

        /** A copy of the bone whose children skip the bones other entries of the file name (they draw those). */
        private ModelPart prune(ModelPart bone, java.util.Collection<String> keep) {
            Map<String, ModelPart> children = new LinkedHashMap<>();
            bone.children.forEach((childName, child) -> {
                if (!named.contains(childName) || keep.contains(childName)) {
                    children.put(childName, prune(child, keep));
                }
            });
            ModelPart copy = new ModelPart(bone.cubes, children);
            copy.setInitialPose(bone.getInitialPose());
            copy.resetPose();
            return copy;
        }

        /** Is {@code name} below one of {@code others} in the bone tree? */
        boolean under(String name, java.util.Collection<String> others) {
            for (String parent = parents.get(name); parent != null; parent = parents.get(parent)) {
                if (others.contains(parent)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The bones of one group: one part turning about the group pivot, or each bone as its own part. */
    private static List<ModelPart> group(Split split, JsonObject group, ResourceLocation file) {
        List<ModelPart> parts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        group.getAsJsonArray("bones").forEach(b -> names.add(b.getAsString()));
        for (String name : names) {
            if (!split.bones().containsKey(name)) {
                throw new IllegalArgumentException(file + ": no bone '" + name + "', known: " + split.bones().keySet());
            }
            // a bone below another bone of this group stays inside that one
            if (!split.under(name, names)) {
                parts.add(split.take(name, names));
            }
        }
        if (!group.has("pivot")) {
            return parts;
        }
        float[] pivot = point(group, "pivot");
        Map<String, ModelPart> children = new LinkedHashMap<>();
        int i = 0;
        for (ModelPart bone : parts) {
            PartPose pose = bone.storePose();
            bone.setInitialPose(PartPose.offsetAndRotation(pose.x - pivot[0], pose.y - pivot[1], pose.z - pivot[2], pose.xRot, pose.yRot, pose.zRot));
            bone.resetPose();
            children.put("bone" + i++, bone);
        }
        ModelPart root = new ModelPart(List.of(), children);
        root.setInitialPose(PartPose.offset(pivot[0], pivot[1], pivot[2]));
        root.resetPose();
        return List.of(root);
    }

    private static float[] point(JsonObject o, String key) {
        float[] out = new float[3];
        JsonArray a = o.getAsJsonArray(key);
        if (a == null) {
            throw new IllegalArgumentException("missing point '" + key + "'");
        }
        for (int i = 0; i < 3; i++) {
            out[i] = a.get(i).getAsFloat();
        }
        return out;
    }
}
