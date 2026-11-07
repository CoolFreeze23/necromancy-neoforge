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
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation SADDLE_SLOT = ResourceLocation.withDefaultNamespace("container/horse/saddle_slot");
    private float mouseX;
    private float mouseY;

    public MinionScreen(MinionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 184;
        inventoryLabelY = MinionMenu.INVENTORY_Y - 11;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        MinionEntity minion = menu.minion();
        if (minion != null && isHovering(26, 17, 52, 72, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, details(minion), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        panel(graphics, x, y, imageWidth, imageHeight);
        for (int i = 0; i < 4; i++) {
            graphics.blitSprite(SLOT, x + 7, y + 17 + i * 18, 18, 18);
        }
        graphics.blitSprite(SLOT, x + 79, y + 17, 18, 18);
        graphics.blitSprite(SLOT, x + 79, y + 35, 18, 18);
        if (menu.slots.get(6).isActive()) {
            graphics.blitSprite(SADDLE_SLOT, x + 79, y + 53, 18, 18);
        }
        // portrait window
        graphics.fill(x + 26, y + 17, x + 78, y + 89, 0xFF373737);
        graphics.fill(x + 27, y + 18, x + 77, y + 88, 0xFF000000);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                graphics.blitSprite(SLOT, x + 7 + col * 18, y + MinionMenu.INVENTORY_Y - 1 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            graphics.blitSprite(SLOT, x + 7 + col * 18, y + MinionMenu.INVENTORY_Y + 57, 18, 18);
        }
        MinionEntity minion = menu.minion();
        if (minion != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 27, y + 18, x + 77, y + 88, 20, 0.25F, mouseX, mouseY, minion);
        }
    }

    /** A plain vanilla-style window: light grey with the bevelled border. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
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
        // blocks per second on foot: a mob's walk input is its speed too, so it moves at about 43.17 x speed^2 (a zombie: 2.3)
        double speed = minion.getAttributeValue(Attributes.MOVEMENT_SPEED);
        line(graphics, x, y + 30, "speed", String.format("%.1f", 43.17 * speed * speed));
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
