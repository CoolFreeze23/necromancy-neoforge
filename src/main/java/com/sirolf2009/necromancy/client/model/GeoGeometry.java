package com.sirolf2009.necromancy.client.model;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Turns a Bedrock / GeckoLib {@code .geo.json} model into plain {@link ModelPart}s, so creatures drawn by GeckoLib can
 * lend Minion parts without a GeckoLib dependency.
 *
 * <p>The geometry follows the convention of a classic model converted to Bedrock: a cube's x stays the ModelPart x,
 * y is measured up from the 24-unit datum, z is unchanged, and rotations are the classic degrees on all three axes. So
 * a bone pivot {@code (px, py, pz)} is the ModelPart point {@code (px, 24 - py, pz)}, a cube {@code origin (ox, oy, oz)
 * size (sx, sy, sz)} spans {@code [ox, ox+sx] x [24-oy-sy, 24-oy] x [oz, oz+sz]}, and a per-face uv lands on the
 * classic face whose normal is the Bedrock face's mirrored in x and y: Bedrock "up" is the classic DOWN (the y-down
 * top), "west" the classic EAST. Each face's rectangle is {@code uv} to {@code uv + uv_size} (a negative size flips it),
 * which reproduces the vanilla box mapping for converted cubes. Box-uv cubes ({@code "uv": [u, v]}) use the vanilla
 * box layout directly.
 */
public final class GeoGeometry {
    private static final Map<ResourceLocation, JsonObject> CACHE = new HashMap<>();

    private GeoGeometry() {
    }

    public static void invalidate() {
        CACHE.clear();
    }

    /**
     * Every bone of the model by name, each a fresh ModelPart in model space with its children attached (fresh on every
     * call, so callers may re-base them freely). Root bones sit at their absolute pivots.
     */
    public static Map<String, ModelPart> bones(ResourceLocation geo) {
        JsonObject geometry = geometry(geo);
        JsonObject description = geometry.getAsJsonObject("description");
        float texW = description.get("texture_width").getAsFloat();
        float texH = description.get("texture_height").getAsFloat();
        return build(geometry.getAsJsonArray("bones"), texW, texH);
    }

    private static synchronized JsonObject geometry(ResourceLocation geo) {
        return CACHE.computeIfAbsent(geo, id -> {
            var resource = Minecraft.getInstance().getResourceManager().getResource(id)
                    .orElseThrow(() -> new IllegalArgumentException("No geometry " + id));
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + id, e);
            }
        });
    }

    private record Bone(String name, @Nullable String parent, float[] pivot, float[] rotation, List<ModelPart.Cube> cubes) {
    }

    private static Map<String, ModelPart> build(JsonArray json, float texW, float texH) {
        Map<String, Bone> bones = new LinkedHashMap<>();
        for (JsonElement element : json) {
            JsonObject b = element.getAsJsonObject();
            String name = b.get("name").getAsString();
            float[] pivot = classicPoint(vec(b, "pivot"));
            float[] rotation = vec(b, "rotation");
            List<ModelPart.Cube> cubes = new ArrayList<>();
            if (b.has("cubes")) {
                for (JsonElement c : b.getAsJsonArray("cubes")) {
                    cubes.add(cube(c.getAsJsonObject(), pivot, texW, texH));
                }
            }
            bones.put(name, new Bone(name, b.has("parent") ? b.get("parent").getAsString() : null, pivot, rotation, cubes));
        }
        Map<String, ModelPart> parts = new LinkedHashMap<>();
        for (Bone bone : bones.values()) {
            build(bone, bones, parts);
        }
        return parts;
    }

    private static ModelPart build(Bone bone, Map<String, Bone> bones, Map<String, ModelPart> parts) {
        ModelPart existing = parts.get(bone.name);
        if (existing != null) {
            return existing;
        }
        Map<String, ModelPart> children = new LinkedHashMap<>();
        for (Bone other : bones.values()) {
            if (bone.name.equals(other.parent)) {
                children.put(other.name, build(other, bones, parts));
            }
        }
        // cubes were built relative to this bone's own pivot; children are re-based onto it below
        ModelPart part = new ModelPart(bone.cubes, children);
        float[] base = bone.parent != null && bones.containsKey(bone.parent) ? bones.get(bone.parent).pivot : new float[3];
        part.setPos(bone.pivot[0] - base[0], bone.pivot[1] - base[1], bone.pivot[2] - base[2]);
        part.setRotation(bone.rotation[0] * Mth.DEG_TO_RAD, bone.rotation[1] * Mth.DEG_TO_RAD, bone.rotation[2] * Mth.DEG_TO_RAD);
        part.setInitialPose(part.storePose());
        parts.put(bone.name, part);
        return part;
    }

    /** Model-space position of a bone's pivot (useful for plugs): the root bones' pivots are absolute. */
    public static float[] classicPoint(float[] bedrock) {
        return new float[] { bedrock[0], 24F - bedrock[1], bedrock[2] };
    }

    private static ModelPart.Cube cube(JsonObject c, float[] pivot, float texW, float texH) {
        float[] origin = vec(c, "origin");
        float[] size = vec(c, "size");
        float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0F;
        boolean mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
        // classic min corner, relative to the bone pivot
        float x = origin[0] - pivot[0];
        float y = 24F - origin[1] - size[1] - pivot[1];
        float z = origin[2] - pivot[2];
        JsonElement uv = c.get("uv");
        if (uv == null || uv.isJsonArray()) {
            int u = uv == null ? 0 : uv.getAsJsonArray().get(0).getAsInt();
            int v = uv == null ? 0 : uv.getAsJsonArray().get(1).getAsInt();
            return new ModelPart.Cube(u, v, x, y, z, size[0], size[1], size[2], inflate, inflate, inflate, mirror, texW, texH, EnumSet.allOf(Direction.class));
        }
        JsonObject faces = uv.getAsJsonObject();
        Set<Direction> visible = EnumSet.noneOf(Direction.class);
        for (Direction classic : Direction.values()) {
            if (faces.has(bedrockFace(classic))) {
                visible.add(classic);
            }
        }
        ModelPart.Cube cube = new ModelPart.Cube(0, 0, x, y, z, size[0], size[1], size[2], inflate, inflate, inflate, false, texW, texH, visible);
        // the cube fills its polygons in DOWN, UP, WEST, NORTH, EAST, SOUTH order, skipping the hidden faces
        int i = 0;
        for (Direction classic : new Direction[] { Direction.DOWN, Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH }) {
            if (!visible.contains(classic)) {
                continue;
            }
            JsonObject face = faces.getAsJsonObject(bedrockFace(classic));
            float[] at = vec(face, "uv");
            float[] span = face.has("uv_size") ? vec(face, "uv_size") : new float[] { 0, 0, 0 };
            ModelPart.Polygon old = cube.polygons[i];
            cube.polygons[i] = new ModelPart.Polygon(old.vertices, at[0], at[1], at[0] + span[0], at[1] + span[1], texW, texH, false, classic);
            i++;
        }
        return cube;
    }

    /** The Bedrock face that lands on a classic face (normal mirrored in x and y). */
    private static String bedrockFace(Direction classic) {
        return switch (classic) {
            case DOWN -> "up";
            case UP -> "down";
            case WEST -> "east";
            case EAST -> "west";
            case NORTH -> "north";
            case SOUTH -> "south";
        };
    }

    private static float[] vec(JsonObject o, String key) {
        float[] out = new float[3];
        if (o.has(key)) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < Math.min(3, a.size()); i++) {
                out[i] = a.get(i).getAsFloat();
            }
        }
        return out;
    }
}
