package com.sirolf2009.necromancy.client.screen;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.client.model.PartAnimator;
import com.sirolf2009.necromancy.client.render.MinionBodyRenderer;
import com.sirolf2009.necromancy.menu.AltarMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The Summoning Altar: the original screen, plus a side panel with the Minion taking shape and a Raise button. */
public class AltarScreen extends AbstractContainerScreen<AltarMenu> {
    private static final ResourceLocation TEXTURE = Necromancy.id("textures/gui/container/summoning_altar.png");
    private static final int PANEL_WIDTH = 92;
    private Button raise;

    public AltarScreen(AltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth - PANEL_WIDTH) / 2;
        raise = addRenderableWidget(Button.builder(Component.translatable("gui.necromancy.altar.raise"), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AltarMenu.RAISE_BUTTON);
            }
        }).bounds(leftPos + imageWidth + 6, topPos + imageHeight - 26, PANEL_WIDTH - 12, 20).build());
        raise.setTooltip(Tooltip.create(Component.translatable("gui.necromancy.altar.raise.tooltip")));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        raise.active = canRaise();
    }

    private boolean canRaise() {
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        var altar = menu.altar();
        boolean ritual = altar.getItem(AltarBlockEntity.BLOOD).getCount() > 0 && altar.getItem(AltarBlockEntity.SOUL).getCount() > 0;
        Map<BodyPartLocation, ResourceLocation> parts = parts();
        return (creative || ritual) && parts.containsKey(BodyPartLocation.HEAD) && parts.containsKey(BodyPartLocation.TORSO) && parts.containsKey(BodyPartLocation.LEGS);
    }

    private Map<BodyPartLocation, ResourceLocation> parts() {
        Map<BodyPartLocation, ResourceLocation> map = new EnumMap<>(BodyPartLocation.class);
        var altar = menu.altar();
        for (int slot = AltarBlockEntity.HEAD; slot <= AltarBlockEntity.ARM_LEFT; slot++) {
            BodyPartLocation location = AltarBlockEntity.locationOf(slot);
            ItemStack stack = altar.getItem(slot);
            NecroMobRegistry.partOf(stack).filter(ref -> ref.fits(location)).ifPresent(ref -> map.put(location, ref.mob().id()));
        }
        return map;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int px = leftPos + imageWidth + 2;
        // side panel in the vanilla container style
        graphics.fill(px, topPos, px + PANEL_WIDTH, topPos + imageHeight, 0xFF000000);
        graphics.fill(px + 1, topPos + 1, px + PANEL_WIDTH - 1, topPos + imageHeight - 1, 0xFF2B1B1B);
        graphics.fill(px + 3, topPos + 3, px + PANEL_WIDTH - 3, topPos + imageHeight - 30, 0xFF120A0A);
        renderPreview(graphics, px + PANEL_WIDTH / 2, topPos + imageHeight - 40, partialTick);
        drawInfo(graphics, px + 5, topPos + 6);
    }

    /** Lines the panel shows above the preview; filled in by the soul and shrine systems through {@link AltarInfo}. */
    protected void drawInfo(GuiGraphics graphics, int x, int y) {
        for (Component line : AltarInfo.lines(menu)) {
            graphics.drawString(font, line, x, y, 0xFFD8C8C8, false);
            y += 10;
        }
    }

    private void renderPreview(GuiGraphics graphics, int cx, int groundY, float partialTick) {
        Map<BodyPartLocation, ResourceLocation> parts = parts();
        if (parts.isEmpty() || minecraft == null || minecraft.player == null) {
            return;
        }
        float age = minecraft.player.tickCount + partialTick;
        float size = 26F;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, groundY - 1.5F * size, 120);
        pose.scale(size, size, -size);
        pose.mulPose(Axis.YP.rotationDegrees(age * 2F % 360F));
        Lighting.setupForEntityInInventory();
        MinionBodyRenderer.render(MinionBodyRenderer.of(parts), new PartAnimator.Pose(0, 0, age, 0, 0, 0, false), pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.necromancy.altar.blood"), 21, 60, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.necromancy.altar.soul"), 132, 60, 0xFFFFFF, false);
    }
}
