package com.sirolf2009.necromancy.client.model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Something drawn over a part after its own cubes, in the same model space and after the part's animator ran: an
 * emissive eye texture, a second mesh with its own texture (sheep wool, a drowned's outer layer, a slime's jelly), or a
 * block or item stuck to it (the snow golem's pumpkin, a mooshroom's mushrooms).
 */
@FunctionalInterface
public interface PartLayer {
    void render(List<ModelPart> parts, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, int color);

    /** Parts that count for the size of the part (for framing item icons); posed like {@code parts}. */
    default List<ModelPart> boundsParts(List<ModelPart> parts) {
        return List.of();
    }

    /** The same cubes again with another texture. */
    static PartLayer retexture(ResourceLocation texture, Function<ResourceLocation, RenderType> renderType) {
        return (parts, poseStack, buffers, light, overlay, color) -> {
            VertexConsumer consumer = buffers.getBuffer(renderType.apply(texture));
            for (ModelPart part : parts) {
                part.render(poseStack, consumer, light, overlay, color);
            }
        };
    }

    /** Glowing eyes (enderman), like vanilla's eyes layers: full bright, no hurt tint. */
    static PartLayer eyes(ResourceLocation texture) {
        return (parts, poseStack, buffers, light, overlay, color) -> {
            VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(texture));
            for (ModelPart part : parts) {
                part.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
            }
        };
    }

    /**
     * A second mesh drawn over the part: each part listed in {@code pairs} gets its partner drawn, posed like it
     * (vanilla's copyPropertiesTo): sheep wool, outer clothing layers, translucent slime. One layer can serve every part
     * of a mob; each part only draws its own partners.
     */
    static PartLayer mesh(ResourceLocation texture, Function<ResourceLocation, RenderType> renderType, Map<ModelPart, ModelPart> pairs) {
        Map<ModelPart, ModelPart> map = new IdentityHashMap<>(pairs);
        return new PartLayer() {
            @Override
            public void render(List<ModelPart> parts, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, int color) {
                VertexConsumer consumer = null;
                for (ModelPart part : parts) {
                    ModelPart partner = map.get(part);
                    if (partner != null) {
                        partner.copyFrom(part);
                        if (consumer == null) {
                            consumer = buffers.getBuffer(renderType.apply(texture));
                        }
                        partner.render(poseStack, consumer, light, overlay, color);
                    }
                }
            }

            @Override
            public List<ModelPart> boundsParts(List<ModelPart> parts) {
                List<ModelPart> list = new ArrayList<>();
                for (ModelPart part : parts) {
                    ModelPart partner = map.get(part);
                    if (partner != null) {
                        partner.copyFrom(part);
                        list.add(partner);
                    }
                }
                return list;
            }
        };
    }

    /** {@link #mesh} pairing the children of two baked roots that have the same names. */
    static PartLayer mesh(ResourceLocation texture, Function<ResourceLocation, RenderType> renderType, ModelPart root, ModelPart meshRoot, String... names) {
        Map<ModelPart, ModelPart> pairs = new IdentityHashMap<>();
        for (String name : names) {
            pairs.put(root.getChild(name), meshRoot.getChild(name));
        }
        return mesh(texture, renderType, pairs);
    }

    /**
     * A block model fixed to the part. {@code place} moves the pose stack from model space to the block's corner (it may
     * use the posed parts, e.g. to follow the head), in block units.
     */
    static PartLayer block(Supplier<BlockState> state, BiConsumer<List<ModelPart>, PoseStack> place) {
        return (parts, poseStack, buffers, light, overlay, color) -> {
            poseStack.pushPose();
            place.accept(parts, poseStack);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state.get(), poseStack, buffers, light, overlay);
            poseStack.popPose();
        };
    }

    /** An item fixed to the part, drawn like a worn helmet ({@link ItemDisplayContext#HEAD}). */
    static PartLayer item(Supplier<ItemStack> stack, BiConsumer<List<ModelPart>, PoseStack> place) {
        return (parts, poseStack, buffers, light, overlay, color) -> {
            poseStack.pushPose();
            place.accept(parts, poseStack);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack.get(), ItemDisplayContext.HEAD, light, overlay, poseStack, buffers, null, 0);
            poseStack.popPose();
        };
    }
}
