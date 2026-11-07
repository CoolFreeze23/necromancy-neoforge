package com.sirolf2009.necromancy.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * One body part ready to draw: baked {@link ModelPart}s in the source mob's own model space (y down, ground at y = 24,
 * units of 1/16 block) with that mob's texture, and the points where it plugs into the rest of the body.
 *
 * <ul>
 * <li>{@code plug}: the point of this part that sits on its socket: bottom-centre of a head, the waist (bottom-centre) of
 * a torso, the shoulder of an arm. Legs stand on the ground, so their plug is unused.</li>
 * <li>Torso sockets: {@code neck} (where a head's plug goes) and the two shoulders.</li>
 * <li>Legs socket: {@code hip}, where the torso's waist goes.</li>
 * </ul>
 * Arms have a left and a right list (vanilla left_arm / right_arm); every other part only uses {@link #parts}.
 */
public final class PartModel {
    private final ResourceLocation texture;
    private final List<ModelPart> parts;
    private final List<ModelPart> rightParts;
    private Vector3f plug = new Vector3f();
    private Vector3f plugRight = new Vector3f();
    private Vector3f neck = new Vector3f(0, 0, 0);
    private Vector3f shoulderLeft = new Vector3f(5, 2, 0);
    private Vector3f shoulderRight = new Vector3f(-5, 2, 0);
    private Vector3f hip = new Vector3f(0, 12, 0);
    private PartAnimator animator = PartAnimator.NONE;
    private Function<ResourceLocation, RenderType> renderType = RenderType::entityCutoutNoCull;
    private float scale = 1F;
    private final List<PartLayer> layers = new ArrayList<>();
    private boolean fullBright;
    private AABB bounds;
    private AABB boundsRight;

    private PartModel(ResourceLocation texture, List<ModelPart> parts, List<ModelPart> rightParts) {
        this.texture = texture;
        this.parts = parts;
        this.rightParts = rightParts;
    }

    public static PartModel of(ResourceLocation texture, ModelPart... parts) {
        return new PartModel(texture, List.of(parts), List.of());
    }

    /** An arm pair: {@code left} for ARM_LEFT, {@code right} for ARM_RIGHT. */
    public static PartModel arms(ResourceLocation texture, List<ModelPart> left, List<ModelPart> right) {
        return new PartModel(texture, List.copyOf(left), List.copyOf(right));
    }

    public PartModel plug(float x, float y, float z) {
        this.plug = new Vector3f(x, y, z);
        return this;
    }

    public PartModel plug(float[] point) {
        return plug(point[0], point[1], point[2]);
    }

    /** Arm shoulders: left plug and right plug. */
    public PartModel plugs(float lx, float ly, float lz, float rx, float ry, float rz) {
        this.plug = new Vector3f(lx, ly, lz);
        this.plugRight = new Vector3f(rx, ry, rz);
        return this;
    }

    public PartModel neck(float x, float y, float z) {
        this.neck = new Vector3f(x, y, z);
        return this;
    }

    public PartModel shoulders(float lx, float ly, float lz, float rx, float ry, float rz) {
        this.shoulderLeft = new Vector3f(lx, ly, lz);
        this.shoulderRight = new Vector3f(rx, ry, rz);
        return this;
    }

    public PartModel hip(float x, float y, float z) {
        this.hip = new Vector3f(x, y, z);
        return this;
    }

    public PartModel animate(PartAnimator animator) {
        this.animator = animator;
        return this;
    }

    public PartModel renderType(Function<ResourceLocation, RenderType> renderType) {
        this.renderType = renderType;
        return this;
    }

    /** Draw this part bigger or smaller around its plug (for parts whose mob is much bigger or smaller). */
    public PartModel scale(float scale) {
        this.scale = scale;
        return this;
    }

    /** Something drawn over the part (eyes, wool, an outer layer, a block). */
    public PartModel layer(PartLayer layer) {
        this.layers.add(layer);
        return this;
    }

    /** Lit like a light source (blaze). */
    public PartModel fullBright() {
        this.fullBright = true;
        return this;
    }

    public List<PartLayer> layers() {
        return layers;
    }

    public boolean isFullBright() {
        return fullBright;
    }

    /** Frames the item icon around this box instead of the cubes (when a layer draws something big, like a pumpkin). */
    public PartModel iconBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        this.boundsRight = this.bounds;
        return this;
    }

    /** The box around the part in its baked pose, layers included, in model units (for framing item icons). */
    public AABB bounds(boolean right) {
        if (right && rightParts.isEmpty()) {
            right = false;
        }
        AABB cached = right ? boundsRight : bounds;
        if (cached == null) {
            List<ModelPart> list = parts(right);
            for (ModelPart part : list) {
                part.resetPose();
            }
            List<ModelPart> all = new ArrayList<>(list);
            for (PartLayer layer : layers) {
                all.addAll(layer.boundsParts(list));
            }
            cached = boundsOf(all);
            if (right) {
                boundsRight = cached;
            } else {
                bounds = cached;
            }
        }
        return cached;
    }

    /** The box around the cubes of these parts as they are posed now, in model units (1/16 block, y down). */
    public static AABB boundsOf(List<ModelPart> parts) {
        float[] box = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
        PoseStack poseStack = new PoseStack();
        Vector3f corner = new Vector3f();
        for (ModelPart part : parts) {
            if (!part.visible) {
                continue;
            }
            part.visit(poseStack, (pose, path, index, cube) -> {
                for (int i = 0; i < 8; i++) {
                    corner.set((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY, (i & 4) == 0 ? cube.minZ : cube.maxZ);
                    corner.div(16F);
                    pose.pose().transformPosition(corner);
                    corner.mul(16F);
                    box[0] = Math.min(box[0], corner.x);
                    box[1] = Math.min(box[1], corner.y);
                    box[2] = Math.min(box[2], corner.z);
                    box[3] = Math.max(box[3], corner.x);
                    box[4] = Math.max(box[4], corner.y);
                    box[5] = Math.max(box[5], corner.z);
                }
            });
        }
        if (box[0] > box[3]) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        return new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    public ResourceLocation texture() {
        return texture;
    }

    public List<ModelPart> parts() {
        return parts;
    }

    public List<ModelPart> rightParts() {
        return rightParts;
    }

    public List<ModelPart> parts(boolean right) {
        return right && !rightParts.isEmpty() ? rightParts : parts;
    }

    public Vector3f plug(boolean right) {
        return right ? plugRight : plug;
    }

    public Vector3f neck() {
        return neck;
    }

    public Vector3f shoulder(boolean right) {
        return right ? shoulderRight : shoulderLeft;
    }

    public Vector3f hip() {
        return hip;
    }

    public PartAnimator animator() {
        return animator;
    }

    public RenderType renderType(ResourceLocation texture) {
        return renderType.apply(texture);
    }

    public float scale() {
        return scale;
    }
}
