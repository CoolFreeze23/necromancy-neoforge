package com.sirolf2009.necromancy.client.model;

import java.util.List;
import java.util.function.Function;

import org.joml.Vector3f;

import net.minecraft.client.model.geom.ModelPart;
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
