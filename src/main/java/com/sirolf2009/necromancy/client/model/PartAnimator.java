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

    /**
     * Eight spider legs in vanilla's order (right/left pairs from hind to front): vanilla's spread pose (the baked layer
     * has the legs flat) and its scuttle.
     */
    PartAnimator SCUTTLE = (parts, location, pose) -> {
        float[] spread = { Mth.PI / 4F, 0.58119464F, 0.58119464F, Mth.PI / 4F };
        float[] turn = { Mth.PI / 4F, Mth.PI / 8F, -Mth.PI / 8F, -Mth.PI / 4F };
        float[] phase = { 0F, Mth.PI, Mth.HALF_PI, Mth.PI * 1.5F };
        for (int i = 0; i < parts.size(); i++) {
            int row = Math.min(i / 2, 3);
            float side = i % 2 == 0 ? 1 : -1;
            float swingY = -(Mth.cos(pose.limbSwing() * 0.6662F * 2F + phase[row]) * 0.4F) * pose.limbSwingAmount();
            float swingZ = Math.abs(Mth.sin(pose.limbSwing() * 0.6662F + phase[row]) * 0.4F) * pose.limbSwingAmount();
            ModelPart leg = parts.get(i);
            leg.zRot = -side * spread[row] + side * swingZ;
            leg.yRot = side * turn[row] + side * swingY;
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

    /**
     * Arms folded in front like a villager's (the baked pose): they stay crossed, rise to strike and bob a little while
     * walking.
     */
    PartAnimator CROSSED_ARMS = (parts, location, pose) -> {
        for (ModelPart part : parts) {
            if (pose.attack() > 0) {
                part.xRot = -2.0F + 1.5F * triangle(pose.attack(), 10F);
            } else {
                part.xRot += Mth.sin(pose.ageInTicks() * 0.067F) * 0.03F + Mth.cos(pose.limbSwing() * 0.6662F * 2F) * 0.08F * pose.limbSwingAmount();
            }
        }
    };

    /** Iron golem arms: swing stiffly while walking, both rise to strike. */
    PartAnimator GOLEM_ARM = (parts, location, pose) -> {
        boolean right = location == BodyPartLocation.ARM_RIGHT;
        for (ModelPart part : parts) {
            if (pose.attack() > 0) {
                part.xRot = -2.0F + 1.5F * triangle(pose.attack(), 10F);
            } else {
                part.xRot = (-0.2F + 1.5F * triangle(pose.limbSwing() + (right ? 0F : 6.5F), 13F)) * pose.limbSwingAmount();
            }
        }
    };

    /** Squid tentacles used as legs: a slow ripple, spreading and pumping while walking. */
    PartAnimator TENTACLES = (parts, location, pose) -> {
        for (int i = 0; i < parts.size(); i++) {
            float ripple = Mth.sin(pose.ageInTicks() * 0.1F + i * 0.8F) * 0.08F;
            float walk = Mth.cos(pose.limbSwing() * 0.6662F + (i % 2 == 0 ? 0 : Mth.PI)) * 0.5F * pose.limbSwingAmount();
            parts.get(i).xRot += ripple + walk + 0.2F * pose.limbSwingAmount();
        }
    };

    /** A slow wobble for jelly (slime parts): squashes and stretches around each part's pivot. */
    /**
     * Wings: a steady flap, faster while moving, each side mirrored (arms: the side comes from the arm; any other part:
     * even parts are the left wing, odd parts the right).
     */
    PartAnimator FLAP = (parts, location, pose) -> {
        float beat = Mth.sin(pose.ageInTicks() * (0.6F + pose.limbSwingAmount() * 0.6F)) * (0.35F + pose.limbSwingAmount() * 0.25F);
        for (int i = 0; i < parts.size(); i++) {
            boolean right = location == com.sirolf2009.necromancy.api.BodyPartLocation.ARM_RIGHT
                    || location != com.sirolf2009.necromancy.api.BodyPartLocation.ARM_LEFT && i % 2 == 1;
            parts.get(i).zRot += right ? beat : -beat;
        }
    };

    PartAnimator WOBBLE = (parts, location, pose) -> {
        float squash = Mth.sin(pose.ageInTicks() * 0.15F) * 0.03F + Mth.abs(Mth.sin(pose.limbSwing() * 0.6662F)) * 0.08F * pose.limbSwingAmount();
        for (ModelPart part : parts) {
            part.yScale = 1F - squash;
            part.xScale = 1F + squash * 0.5F;
            part.zScale = 1F + squash * 0.5F;
        }
    };

    /**
     * Blaze rods circling like vanilla's: {@code rings} lists, for each ring, how many rods it has (in part order), its
     * radius, its height, its bob speed, its turning speed (radians per tick / pi), its start angle, and the bob phase of its
     * first rod and between rods (vanilla's three rings of four, rod corners on the circle like vanilla).
     */
    static PartAnimator blazeRods(float[]... rings) {
        return (parts, location, pose) -> {
            int index = 0;
            for (float[] ring : rings) {
                int count = (int) ring[0];
                float angle = ring[5] + pose.ageInTicks() * Mth.PI * ring[4];
                for (int i = 0; i < count && index < parts.size(); i++, index++) {
                    ModelPart rod = parts.get(index);
                    rod.x = Mth.cos(angle) * ring[1];
                    rod.z = Mth.sin(angle) * ring[1];
                    rod.y = ring[2] + Mth.cos((ring[6] + i * ring[7] + pose.ageInTicks()) * ring[3]);
                    angle++;
                }
            }
        };
    }

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
