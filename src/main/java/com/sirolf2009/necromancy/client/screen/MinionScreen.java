package com.sirolf2009.necromancy.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.menu.MinionMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;

/**
 * A Minion's equipment and what it is made of (docs/REVIEW.md #3): armour on the left, hands and saddle beside the
 * portrait, stats on the right, and the parts, soul, rank, order and stance in the tooltip of the portrait.
 */
public class MinionScreen extends AbstractContainerScreen<MinionMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation SADDLE_SLOT = ResourceLocation.withDefaultNamespace("container/horse/saddle_slot");
    private float mouseX;
    private float mouseY;

    public MinionScreen(MinionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        MinionEntity minion = menu.minion();
        if (minion != null && isHovering(26, 18, 52, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, details(minion), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);
        // cover the horse chest area with plain panel colour, then draw our slots
        graphics.fill(x + 98, y + 17, x + 170, y + 71, 0xFFC6C6C6);
        for (int i = 0; i < 4; i++) {
            graphics.blitSprite(SLOT, x + 7, y + 17 + i * 18, 18, 18);
        }
        graphics.blitSprite(SLOT, x + 79, y + 17, 18, 18);
        graphics.blitSprite(SLOT, x + 79, y + 35, 18, 18);
        if (menu.slots.get(6).isActive()) {
            graphics.blitSprite(SADDLE_SLOT, x + 79, y + 53, 18, 18);
        }
        MinionEntity minion = menu.minion();
        if (minion != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 26, y + 18, x + 78, y + 70, 17, 0.25F, mouseX, mouseY, minion);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        MinionEntity minion = menu.minion();
        if (minion == null) {
            return;
        }
        int x = 100;
        int y = 19;
        line(graphics, x, y, "health", String.format("%.0f/%.0f", minion.getHealth(), minion.getMaxHealth()));
        line(graphics, x, y + 10, "attack", String.format("%.1f", menu.attackDamage()));
        line(graphics, x, y + 20, "armor", String.format("%.0f", minion.getAttributeValue(Attributes.ARMOR)));
        // blocks per second on foot, as the F3 screen counts it
        line(graphics, x, y + 30, "speed", String.format("%.1f", minion.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17));
        line(graphics, x, y + 40, "follow", String.format("%.0f", menu.followRange()));
        graphics.drawString(font, Component.translatable("rank.necromancy." + minion.rank()), x, y + 51,
                minion.rank() >= 5 ? 0x8B0000 : 0x404040, false);
    }

    private void line(GuiGraphics graphics, int x, int y, String key, String value) {
        graphics.drawString(font, Component.translatable("gui.necromancy.minion." + key, value), x, y, 0x404040, false);
    }

    private static List<Component> details(MinionEntity minion) {
        List<Component> lines = new ArrayList<>();
        lines.add(minion.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = minion.part(location);
            if (mob != null) {
                lines.add(Component.translatable("tooltip.necromancy.remains.part", Component.translatable("location.necromancy." + location.getSerializedName()),
                        Component.translatable(mob.translationKey())).withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(minion.soul().description().copy().withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.translatable("gui.necromancy.minion.order", minion.order().displayName()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.necromancy.minion.stance", minion.stance().displayName()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.necromancy.minion.kills", minion.kills()).withStyle(ChatFormatting.DARK_RED));
        if (minion.isBound()) {
            lines.add(Component.translatable("gui.necromancy.minion.bound").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return lines;
    }
}
