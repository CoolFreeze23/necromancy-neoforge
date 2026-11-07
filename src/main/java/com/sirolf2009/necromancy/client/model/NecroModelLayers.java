package com.sirolf2009.necromancy.client.model;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The mod's own models, converted from the 1.7.10 ModelBase classes (tools/convert_legacy_model.py; rotation points,
 * rotations, mirroring and texture offsets as they were). Where a part was re-pivoted to animate cleanly, the cubes
 * were moved by the same amount so nothing changed on screen.
 */
public final class NecroModelLayers {
    public static final ModelLayerLocation TEDDY = layer("teddy");
    public static final ModelLayerLocation NIGHT_CRAWLER = layer("night_crawler");
    public static final ModelLayerLocation ISAAC = layer("isaac");
    public static final ModelLayerLocation ISAAC_BODY = layer("isaac_body");
    public static final ModelLayerLocation ISAAC_HEAD = layer("isaac_head");
    public static final ModelLayerLocation ALTAR = layer("summoning_altar");
    public static final ModelLayerLocation SEWING_MACHINE = layer("sewing_machine");
    public static final ModelLayerLocation BLOOD_SCYTHE = layer("blood_scythe");
    public static final ModelLayerLocation BONE_SCYTHE = layer("bone_scythe");

    private NecroModelLayers() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Necromancy.id(name), "main");
    }

    /** ModelTeddy. */
    public static LayerDefinition teddy() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("paw_front_right", CubeListBuilder.create().texOffs(0, 5).addBox(-2F, 0F, 0F, 2F, 3F, 2F),
                PartPose.offsetAndRotation(-1.5F, 16F, 2.5F, -1.151917F, 0F, 0F));
        root.addOrReplaceChild("paw_front_left", CubeListBuilder.create().texOffs(4, 5).addBox(0F, 0F, 0F, 2F, 3F, 2F),
                PartPose.offsetAndRotation(1.5F, 16F, 2.5F, -1.151917F, 0F, 0F));
        root.addOrReplaceChild("paw_back_right", CubeListBuilder.create().texOffs(0, 14).addBox(0F, 0F, 0F, 2F, 3F, 2F), PartPose.offset(-2.2F, 21F, 2F));
        root.addOrReplaceChild("paw_back_left", CubeListBuilder.create().texOffs(0, 9).addBox(0F, 0F, 0F, 2F, 3F, 2F), PartPose.offset(0.2F, 21F, 2F));
        root.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(10, 0).addBox(0F, 0F, 0F, 4F, 7F, 3F),
                PartPose.offsetAndRotation(-2F, 15F, 1F, 0.1487144F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -3F, -1F, 3F, 3F, 2F),
                PartPose.offset(0.5F, 16F, 1F));
        // the ears turn with the head: their rotation points, relative to the head's
        head.addOrReplaceChild("ear_right", CubeListBuilder.create().texOffs(8, 10).addBox(-0.5F, -1F, 0F, 1F, 1F, 1F),
                PartPose.offsetAndRotation(-1.5F, -2.8F, -0.9F, 0.1115358F, 0F, -0.2230717F));
        head.addOrReplaceChild("ear_left", CubeListBuilder.create().texOffs(8, 12).addBox(-0.5F, -1F, 0F, 1F, 1F, 1F),
                PartPose.offsetAndRotation(0.5F, -2.8F, -0.9F, 0.1115358F, 0F, 0.2230705F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * ModelNightCrawler (by Fabiulu), drawn 1.4 times bigger by its renderer like the original. The arms were pivoted at
     * the shoulders (the original swung them around a point above the head) and the feet were grouped.
     */
    public static LayerDefinition nightCrawler() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("mid_body", CubeListBuilder.create().texOffs(49, 9).addBox(-4F, 0F, -2F, 4F, 5F, 3F),
                PartPose.offsetAndRotation(2F, 14.46667F, -1.133333F, 0.1858931F, 0F, 0F));
        root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(45, 18).addBox(0F, 0F, 0F, 6F, 4F, 3F),
                PartPose.offsetAndRotation(-3F, 12.46667F, -4.666667F, 0.5379539F, 0F, 0F));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(57, 14).addBox(0F, 0F, 0F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-1F, 11.33333F, -4F, 0.669215F, 0F, 0F));
        root.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(53, 23).addBox(0F, 0F, 0F, 2F, 5F, 3F), PartPose.offset(-1F, 19F, -2.4F));
        PartDefinition feet = root.addOrReplaceChild("feet", CubeListBuilder.create(), PartPose.offset(0F, 19F, 0F));
        // the claws and roots around the stalk (Shape1-15), relative to the feet group
        feet.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(54, 27).addBox(0F, 0F, 0F, 1F, 0F, 3F),
                PartPose.offsetAndRotation(0F, 4F, 0F, -0.2602503F, 0F, 0F));
        feet.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(54, 28).addBox(0F, 0F, 0F, 1F, 0F, 2F),
                PartPose.offsetAndRotation(-1F, 4F, 0F, -0.4833219F, 0F, 0F));
        feet.addOrReplaceChild("shape7", CubeListBuilder.create().texOffs(54, 30).addBox(0F, 0F, 0F, 1F, 0F, 1F),
                PartPose.offsetAndRotation(0F, 3F, 0.2666667F, -0.9294653F, 0F, 0F));
        feet.addOrReplaceChild("shape8", CubeListBuilder.create().texOffs(54, 27).addBox(0F, 0F, 0F, 1F, 0F, 3F),
                PartPose.offsetAndRotation(1F, 4F, -1.6F, 0F, 0.2230717F, 0.8179294F));
        feet.addOrReplaceChild("shape9", CubeListBuilder.create().texOffs(54, 28).addBox(0F, 0F, 0F, 1F, 0F, 2F),
                PartPose.offsetAndRotation(-1F, 4F, -1.866667F, 0F, -0.2230717F, 2.249306F));
        feet.addOrReplaceChild("shape10", CubeListBuilder.create().texOffs(55, 27).addBox(0F, 0F, 0F, 1F, 1F, 0F),
                PartPose.offsetAndRotation(-1F, 2F, -0.5333334F, 0F, -2.286485F, 0F));
        feet.addOrReplaceChild("shape11", CubeListBuilder.create().texOffs(54, 28).addBox(0F, 0F, 0F, 1F, 1F, 0F),
                PartPose.offsetAndRotation(1F, 1F, -2F, 0F, -0.9294653F, 0F));
        feet.addOrReplaceChild("shape12", CubeListBuilder.create().texOffs(58, 27).addBox(0F, 0F, 0F, 0F, 1F, 2F),
                PartPose.offsetAndRotation(-1F, 4F, -1F, 0F, -0.1115358F, 0.1858931F));
        feet.addOrReplaceChild("shape13", CubeListBuilder.create().texOffs(58, 27).addBox(0F, 0F, 0F, 0F, 1F, 2F),
                PartPose.offsetAndRotation(1F, 4F, 0F, 0F, 0.1858931F, -0.3717861F));
        feet.addOrReplaceChild("shape14", CubeListBuilder.create().texOffs(56, 28).addBox(0F, 0F, 0F, 0F, 1F, 1F),
                PartPose.offsetAndRotation(1F, 4F, -2F, 0F, 0.9294653F, 0F));
        feet.addOrReplaceChild("shape15", CubeListBuilder.create().texOffs(59, 28).addBox(0F, 0F, 0F, 0F, 1F, 1F),
                PartPose.offsetAndRotation(-1F, 4F, -2F, 0F, -0.8365188F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().mirror()
                .texOffs(17, 0).addBox(-2F, -9F, -4F, 4F, 5F, 5F)
                .texOffs(52, 19).addBox(1F, -4F, 1F, 1F, 2F, 0F)
                .texOffs(52, 19).addBox(-2F, -4F, 1F, 1F, 2F, 0F)
                .texOffs(52, 19).addBox(2F, -4F, 0F, 0F, 2F, 1F)
                .texOffs(52, 19).addBox(-2F, -4F, 0F, 0F, 2F, 1F)
                .texOffs(18, 13).addBox(-2F, -2F, -3F, 4F, 1F, 4F)
                .texOffs(0, 18).addBox(-1.933333F, -2.466667F, -2F, 0F, 1F, 1F)
                .texOffs(0, 18).addBox(1.866667F, -2.466667F, -2F, 0F, 1F, 1F)
                .texOffs(0, 18).addBox(0.4666667F, -2.466667F, -2.933333F, 1F, 1F, 0F)
                .texOffs(0, 18).addBox(-1.466667F, -2.466667F, -2.933333F, 1F, 1F, 0F)
                .texOffs(0, 18).addBox(0.7333333F, -4.8F, -3.533333F, 1F, 1F, 0F)
                .texOffs(0, 18).addBox(-1.666667F, -4.8F, -3.466667F, 1F, 1F, 0F),
                PartPose.offset(0F, 12.26667F, -4.066667F));
        CubeListBuilder arm = CubeListBuilder.create().mirror()
                .texOffs(40, 16).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F)
                .texOffs(0, 0).addBox(0.5F, 9F, -0.5F, 0F, 2F, 1F)
                .texOffs(0, 0).addBox(-0.5F, 9F, -0.5F, 0F, 2F, 1F);
        root.addOrReplaceChild("left_arm", arm, PartPose.offset(3.5F, 12F, -2.5F));
        root.addOrReplaceChild("right_arm", arm, PartPose.offset(-3.5F, 12F, -2.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelIsaacNormal: the biped with Isaac's big head (texture isaac.png or blood_isaac.png, 64x32). */
    public static LayerDefinition isaac() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -8F, -4F, 10F, 9F, 8F), PartPose.offset(0F, 1F, 0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 1F, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelIsaacSevered: the biped without its head, a stump of neck in its place. */
    public static LayerDefinition isaacBody() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1F, 0F, -1F, 2F, 1F, 2F), PartPose.offset(0F, 1F, 0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 1F, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Isaac's severed head with the neck sinews hanging under it (ModelIsaacHead for the entity, NecroEntityIsaac's head
     * part for Minions). Bottom of the head at y = {@code bottom}, centred.
     */
    public static LayerDefinition isaacHead(float bottom) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -9F, -4F, 10F, 9F, 8F),
                PartPose.offset(0F, bottom, 0F));
        head.addOrReplaceChild("neck1", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 1F, 1F), PartPose.offset(0F, 0F, -1F));
        head.addOrReplaceChild("neck2", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 1F, 1F), PartPose.offset(-1F, 0F, 0F));
        head.addOrReplaceChild("neck3", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 1F, 1F), PartPose.offset(-1F, 0F, 1F));
        head.addOrReplaceChild("neck4", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offset(0F, 0F, 1F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelAltar: the whole three-block table. The altar block is centred on the origin, the table runs along +x. */
    public static LayerDefinition altar() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom_altar", CubeListBuilder.create().texOffs(0, 45).addBox(0F, 0F, 0F, 16F, 3F, 16F), PartPose.offset(-8F, 21F, -8F));
        root.addOrReplaceChild("pillar_altar", CubeListBuilder.create().texOffs(0, 41).addBox(0F, 0F, 0F, 11F, 17F, 6F),
                PartPose.offsetAndRotation(-3F, 4F, -3F, 0.0174533F, 0F, 0F));
        root.addOrReplaceChild("base_altar", CubeListBuilder.create().texOffs(64, 46).addBox(0F, 0F, 0F, 16F, 2F, 16F), PartPose.offset(-8F, 2F, -8F));
        root.addOrReplaceChild("backpiece1", CubeListBuilder.create().texOffs(0, 46).addBox(0F, 0F, 0F, 1F, 2F, 16F), PartPose.offset(7F, 0F, -8F));
        root.addOrReplaceChild("backpiece2", CubeListBuilder.create().texOffs(0, 54).addBox(-8F, 0F, 0F, 8F, 2F, 1F),
                PartPose.offsetAndRotation(7F, 0F, 7.1F, 0F, 0F, -0.2617994F));
        root.addOrReplaceChild("backpiece3", CubeListBuilder.create().texOffs(0, 52).addBox(-8F, 0F, 0F, 8F, 2F, 1F),
                PartPose.offsetAndRotation(7F, 0F, -8.1F, 0F, 0F, -0.2617994F));
        root.addOrReplaceChild("connection1", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 13F, 1F), PartPose.offset(7F, 8F, 3F));
        root.addOrReplaceChild("table_bottom", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 32F, 3F, 16F), PartPose.offset(8F, 21F, -8F));
        root.addOrReplaceChild("connection2", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 13F, 1F), PartPose.offset(7F, 8F, -4F));
        root.addOrReplaceChild("table_middle", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 29F, 11F, 10F), PartPose.offset(8F, 10F, -5F));
        root.addOrReplaceChild("table_top", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 32F, 2F, 16F), PartPose.offset(8F, 8F, -8F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** ModelSewing (the crank was never drawn in the original and stays out). */
    public static LayerDefinition sewingMachine() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 12F, 1F, 6F), PartPose.offset(-7F, 23F, -3F));
        root.addOrReplaceChild("needle", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 4F, 1F), PartPose.offset(-6.5F, 18.3F, -0.5F));
        root.addOrReplaceChild("arm1", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 2F, 6F, 2F), PartPose.offset(2F, 17F, -1F));
        root.addOrReplaceChild("arm3", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 2F, 2F, 2F), PartPose.offset(-7F, 17.1F, -1F));
        root.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 7F, 1F, 1F), PartPose.offset(-5F, 17.3F, -0.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelScythe (Blood Scythe). Handle from y = -8 (top, blade) to y = 24. */
    public static LayerDefinition bloodScythe() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("handle_middle", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 11F, 1F),
                PartPose.offsetAndRotation(0F, 1.7F, 0F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("blade_edge", CubeListBuilder.create().texOffs(4, 0).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 10F),
                PartPose.offsetAndRotation(0.5F, -7F, 2F, 0F, 0F, 0.7853982F));
        root.addOrReplaceChild("blade_base1", CubeListBuilder.create().texOffs(40, 0).addBox(0F, 0F, 0F, 1F, 1F, 11F), PartPose.offset(0.2F, -8F, 1F));
        root.addOrReplaceChild("handle_bottom", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 12F, 1F), PartPose.offset(0F, 12F, -2.8F));
        root.addOrReplaceChild("handle_top", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 10F, 1F), PartPose.offset(0F, -8F, 0F));
        root.addOrReplaceChild("joint", CubeListBuilder.create().texOffs(0, 13).addBox(0F, 0F, 0F, 2F, 2F, 2F), PartPose.offset(-0.5F, -8.1F, 0F));
        root.addOrReplaceChild("blade_base2", CubeListBuilder.create().texOffs(40, 0).addBox(0F, 0F, 0F, 1F, 1F, 11F), PartPose.offset(-0.2F, -8F, 1F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelScytheBone (Bone Scythe; its texture is 640x320, ten times the 64x32 layout). */
    public static LayerDefinition boneScythe() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("handle_middle", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 11F, 1F),
                PartPose.offsetAndRotation(0F, 1.7F, 0F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("handle_bottom", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 12F, 1F), PartPose.offset(0F, 12F, -2.8F));
        root.addOrReplaceChild("handle_top", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 1F, 10F, 1F), PartPose.offset(0F, -8F, 0F));
        root.addOrReplaceChild("joint", CubeListBuilder.create().texOffs(34, 0).addBox(0F, 0F, 0F, 2F, 4F, 4F), PartPose.offset(-0.5F, -8.1F, -1F));
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 15).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 15F),
                PartPose.offsetAndRotation(0.5F, -7F, 1F, -0.1F, 0.06F, 0.7F));
        root.addOrReplaceChild("blade_base_left", CubeListBuilder.create().texOffs(0, 15).addBox(0F, 0F, 0F, 1F, 1F, 15F),
                PartPose.offsetAndRotation(0.2F, -8F, 1F, -0.1115358F, 0F, 0F));
        root.addOrReplaceChild("blade_base_right", CubeListBuilder.create().texOffs(0, 15).addBox(0F, 0F, 0F, 1F, 1F, 15F),
                PartPose.offsetAndRotation(-0.2F, -8F, 1F, -0.1115358F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
