package com.sirolf2009.necromancy.client.render;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.client.model.NecroMobModels;
import com.sirolf2009.necromancy.client.model.PartAnimator;
import com.sirolf2009.necromancy.client.model.PartLayer;
import com.sirolf2009.necromancy.client.model.PartModel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a body stitched from different creatures. Same order as the original ModelMinion: legs stand on the ground, the
 * torso's waist sits on the legs' hip, and the head and arms plug into the torso's neck and shoulders. Coordinates are
 * model units (1/16 block, y down, ground at y = 24), after the usual living-entity flip.
 */
public final class MinionBodyRenderer {
    private static final Vector3f DEFAULT_HIP = new Vector3f(0, 12, 0);
    private static final Vector3f GROUND = new Vector3f(0, 24, 0);

    private MinionBodyRenderer() {
    }

    /** A body to draw: the mob of each location (missing locations are left out). */
    public interface Body {
        @Nullable
        ResourceLocation mob(BodyPartLocation location);

        /** Called after a head or arm is drawn, with the pose stack at that part's model space (held items, helmets). */
        default void decorate(BodyPartLocation location, ModelPart part, PoseStack poseStack, MultiBufferSource buffers, int light) {
        }

        default boolean saddled() {
            return false;
        }

        @Nullable
        default ResourceLocation saddleTexture(ResourceLocation torsoMob) {
            return null;
        }
    }

    private static boolean legless(@javax.annotation.Nullable ResourceLocation torsoMob) {
        com.sirolf2009.necromancy.api.NecroMob mob = com.sirolf2009.necromancy.api.NecroMobRegistry.get(torsoMob);
        return mob != null && !mob.hasPart(com.sirolf2009.necromancy.api.PartType.LEGS);
    }

    public static Body of(Map<BodyPartLocation, ResourceLocation> parts) {
        return parts::get;
    }

    public static void render(Body body, PartAnimator.Pose pose, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, int color) {
        PartModel legs = model(body, BodyPartLocation.LEGS);
        PartModel torso = model(body, BodyPartLocation.TORSO);

        Vector3f hip = DEFAULT_HIP;
        if (legs == null && legless(body.mob(BodyPartLocation.TORSO))) {
            // a creature that has no legs at all (a worm, a snake, a fish) rests its body on the ground
            hip = GROUND;
        }
        if (legs != null) {
            // legs are scaled around the ground under them, so they keep standing on it
            poseStack.pushPose();
            scaleAround(poseStack, legs.scale(), GROUND);
            draw(legs, false, BodyPartLocation.LEGS, pose, poseStack, buffers, light, overlay, color, null);
            poseStack.popPose();
            hip = scaled(legs.hip(), GROUND, legs.scale());
        }
        poseStack.pushPose();
        if (torso != null) {
            Vector3f waist = torso.plug(false);
            translate(poseStack, hip.x - waist.x, hip.y - waist.y, hip.z - waist.z);
            ResourceLocation torsoMob = body.mob(BodyPartLocation.TORSO);
            ResourceLocation saddle = body.saddled() && torsoMob != null ? body.saddleTexture(torsoMob) : null;
            poseStack.pushPose();
            scaleAround(poseStack, torso.scale(), waist);
            draw(torso, false, BodyPartLocation.TORSO, pose, poseStack, buffers, light, overlay, color, saddle);
            poseStack.popPose();
            float s = torso.scale();
            attach(body, BodyPartLocation.HEAD, scaled(torso.neck(), waist, s), false, pose, poseStack, buffers, light, overlay, color);
            attach(body, BodyPartLocation.ARM_LEFT, scaled(torso.shoulder(false), waist, s), false, pose, poseStack, buffers, light, overlay, color);
            attach(body, BodyPartLocation.ARM_RIGHT, scaled(torso.shoulder(true), waist, s), true, pose, poseStack, buffers, light, overlay, color);
        } else {
            // a floating head (wolf, goat or Isaac head alone, in the altar preview)
            translate(poseStack, 0, hip.y - 12, 0);
            attach(body, BodyPartLocation.HEAD, new Vector3f(0, 12, 0), false, pose, poseStack, buffers, light, overlay, color);
        }
        poseStack.popPose();
    }

    /** Draws one part alone in its own model space (item icons): baked pose, no animation. */
    public static void renderPart(PartModel part, boolean right, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, int color) {
        List<ModelPart> parts = part.parts(right);
        for (ModelPart p : parts) {
            p.resetPose();
        }
        drawPosed(part, parts, poseStack, buffers, light, overlay, color, null);
    }

    private static Vector3f scaled(Vector3f point, Vector3f origin, float scale) {
        if (scale == 1F) {
            return point;
        }
        return new Vector3f(point).sub(origin).mul(scale).add(origin);
    }

    private static void scaleAround(PoseStack poseStack, float scale, Vector3f origin) {
        if (scale != 1F) {
            translate(poseStack, origin.x, origin.y, origin.z);
            poseStack.scale(scale, scale, scale);
            translate(poseStack, -origin.x, -origin.y, -origin.z);
        }
    }

    private static void attach(Body body, BodyPartLocation location, Vector3f socket, boolean right, PartAnimator.Pose pose, PoseStack poseStack,
            MultiBufferSource buffers, int light, int overlay, int color) {
        PartModel part = model(body, location);
        if (part == null) {
            return;
        }
        Vector3f plug = part.plug(right);
        poseStack.pushPose();
        translate(poseStack, socket.x - plug.x, socket.y - plug.y, socket.z - plug.z);
        if (part.scale() != 1F) {
            translate(poseStack, plug.x, plug.y, plug.z);
            poseStack.scale(part.scale(), part.scale(), part.scale());
            translate(poseStack, -plug.x, -plug.y, -plug.z);
        }
        draw(part, right, location, pose, poseStack, buffers, light, overlay, color, null);
        List<ModelPart> drawn = part.parts(right);
        if (!drawn.isEmpty()) {
            body.decorate(location, drawn.get(0), poseStack, buffers, light);
        }
        poseStack.popPose();
    }

    private static void draw(PartModel part, boolean right, BodyPartLocation location, PartAnimator.Pose pose, PoseStack poseStack,
            MultiBufferSource buffers, int light, int overlay, int color, @Nullable ResourceLocation overlayTexture) {
        List<ModelPart> parts = part.parts(right);
        for (ModelPart p : parts) {
            p.resetPose();
        }
        part.animator().animate(parts, location, pose);
        drawPosed(part, parts, poseStack, buffers, light, overlay, color, overlayTexture);
    }

    private static void drawPosed(PartModel part, List<ModelPart> parts, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
            int color, @Nullable ResourceLocation overlayTexture) {
        if (part.isFullBright()) {
            light = LightTexture.FULL_BRIGHT;
        }
        VertexConsumer consumer = buffers.getBuffer(part.renderType(part.texture()));
        for (ModelPart p : parts) {
            p.render(poseStack, consumer, light, overlay, color);
        }
        for (PartLayer layer : part.layers()) {
            layer.render(parts, poseStack, buffers, light, overlay, color);
        }
        if (overlayTexture != null) {
            VertexConsumer saddle = buffers.getBuffer(part.renderType(overlayTexture));
            for (ModelPart p : parts) {
                p.render(poseStack, saddle, light, overlay, color);
            }
        }
    }

    @Nullable
    private static PartModel model(Body body, BodyPartLocation location) {
        ResourceLocation mob = body.mob(location);
        return NecroMobModels.get(mob, location.type() == PartType.ARM ? PartType.ARM : location.type());
    }

    private static void translate(PoseStack poseStack, float x, float y, float z) {
        poseStack.translate(x / 16F, y / 16F, z / 16F);
    }
}
