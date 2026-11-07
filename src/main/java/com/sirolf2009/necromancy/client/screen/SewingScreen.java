package com.sirolf2009.necromancy.client.screen;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.menu.SewingMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The Sewing Machine screen (original texture and labels). */
public class SewingScreen extends AbstractContainerScreen<SewingMenu> {
    private static final ResourceLocation TEXTURE = Necromancy.id("textures/gui/container/sewing_machine.png");

    public SewingScreen(SewingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
        titleLabelX = 120;
        titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.necromancy.sewing.needle"), 88, 8, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.necromancy.sewing.thread"), 88, 72, 0x404040, false);
        if (!menu.isReady()) {
            graphics.drawString(font, Component.translatable("gui.necromancy.sewing.not_ready"), 120, 58, 0xA03030, false);
        }
    }
}
