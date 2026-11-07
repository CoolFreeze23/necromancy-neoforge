package com.sirolf2009.necromancy.client.model;

import java.util.List;

import com.sirolf2009.necromancy.api.BodyPartLocation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Poses a part before it is drawn. Parts are shared between Minions, so an animator always starts from the baked pose
 * ({@link ModelPart#resetPose()} is called first by the renderer).
 */
@FunctionalInterface
public interface PartAnimator {
    void animate(List<ModelPart> parts, BodyPartLocation location, Pose pose);

    /** What a Minion is doing this frame. */
    record Pose(float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float attack, boolean sitting) {
        public static final Pose STILL = new Pose(0, 0, 0, 0, 0, 0, false);
    }

    PartAnimator NONE = (parts, location, pose) -> {
    };

    /** The whole part turns with the head (every listed part, e.g. a chicken's head, beak and wattle). */
    PartAnimator LOOK = (parts, location, pose) -> {
        for (ModelPart part : parts) {
            part.yRot += pose.headYaw() * Mth.DEG_TO_RAD;
            part.xRot += pose.headPitch() * Mth.DEG_TO_RAD;
        }
    };

    /** Only the first part turns (the rest are its children or decorations). */
    PartAnimator LOOK_FIRST = (parts, location, pose) -> {
        if (!parts.isEmpty()) {
            parts.get(0).yRot += pose.headYaw() * Mth.DEG_TO_RAD;
            parts.get(0).xRot += pose.headPitch() * Mth.DEG_TO_RAD;
        }
    };

    /** Two legs walking: part 0 and part 1 swing in opposite phase (biped and quadruped hind legs). */
    PartAnimator WALK_PAIR = (parts, location, pose) -> {
        for (int i = 0; i < parts.size(); i++) {
            float phase = i % 2 == 0 ? 0 : Mth.PI;
            parts.get(i).xRot += Mth.cos(pose.limbSwing() * 0.6662F + phase) * 1.4F * pose.limbSwingAmount();
        }
    };

    /** Four or more legs (spiders): alternating pairs, swinging sideways like vanilla's spider. */
    PartAnimator SCUTTLE = (parts, location, pose) -> {
        for (int i = 0; i < parts.size(); i++) {
            float phase = (i / 2) * Mth.HALF_PI + (i % 2 == 0 ? 0 : Mth.PI);
            float side = (i % 2 == 0) ? 1 : -1;
            parts.get(i).yRot += side * Mth.cos(pose.limbSwing() * 0.6662F * 2F + phase) * 0.4F * pose.limbSwingAmount();
            parts.get(i).zRot += side * Math.abs(Mth.sin(pose.limbSwing() * 0.6662F + phase) * 0.4F) * pose.limbSwingAmount();
        }
    };

    /** A hanging arm: swings while walking, rises to strike (the original's attack animation). */
    PartAnimator ARM = (parts, location, pose) -> {
        boolean right = location == BodyPartLocation.ARM_RIGHT;
        float swing = Mth.cos(pose.limbSwing() * 0.6662F + (right ? Mth.PI : 0)) * 2.0F * pose.limbSwingAmount() * 0.5F;
        for (ModelPart part : parts) {
            if (pose.attack() > 0) {
                part.xRot = -2.0F + 1.5F * triangle(pose.attack(), 10F);
            } else {
                part.xRot += swing;
                part.zRot += (right ? 1 : -1) * (Mth.cos(pose.ageInTicks() * 0.09F) * 0.05F + 0.05F);
                part.xRot += Mth.sin(pose.ageInTicks() * 0.067F) * 0.05F;
            }
        }
    };

    /** Arms held out in front like a zombie's, still rising to strike. */
    PartAnimator ZOMBIE_ARM = (parts, location, pose) -> {
        for (ModelPart part : parts) {
            if (pose.attack() > 0) {
                part.xRot = -2.0F + 1.5F * triangle(pose.attack(), 10F);
            } else {
                part.xRot = -Mth.HALF_PI + Mth.sin(pose.ageInTicks() * 0.067F) * 0.05F;
            }
        }
    };

    /** A front leg used as an arm: walks like a quadruped's front leg, paws at the target to strike. */
    PartAnimator FRONT_LEG = (parts, location, pose) -> {
        boolean right = location == BodyPartLocation.ARM_RIGHT;
        for (ModelPart part : parts) {
            if (pose.attack() > 0) {
                part.xRot = -1.2F + 0.8F * triangle(pose.attack(), 10F);
            } else {
                part.xRot += Mth.cos(pose.limbSwing() * 0.6662F + (right ? 0 : Mth.PI)) * 1.4F * pose.limbSwingAmount();
            }
        }
    };

    /** Slow idle spin (blaze rods). */
    PartAnimator SPIN = (parts, location, pose) -> {
        float t = pose.ageInTicks() * 0.05F;
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).y += Mth.cos((i * 2 + pose.ageInTicks()) * 0.25F);
        }
        if (!parts.isEmpty()) {
            for (ModelPart part : parts) {
                float angle = t;
                float x = part.x;
                float z = part.z;
                part.x = x * Mth.cos(angle) - z * Mth.sin(angle);
                part.z = x * Mth.sin(angle) + z * Mth.cos(angle);
            }
        }
    };

    /** Tentacles that sway (squid). */
    PartAnimator SWAY = (parts, location, pose) -> {
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).xRot += Mth.sin(pose.ageInTicks() * 0.1F + i) * 0.15F + pose.limbSwingAmount() * 0.4F;
        }
    };

    static float triangle(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }

    /** Runs two animators in turn. */
    default PartAnimator then(PartAnimator next) {
        return (parts, location, pose) -> {
            animate(parts, location, pose);
            next.animate(parts, location, pose);
        };
    }
}
