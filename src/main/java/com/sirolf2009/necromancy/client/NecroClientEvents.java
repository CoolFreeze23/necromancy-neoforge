package com.sirolf2009.necromancy.client;

import java.util.ArrayList;
import java.util.List;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.item.BodyPartItem;
import com.sirolf2009.necromancy.network.TearShotPayload;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Necromancy.MOD_ID, value = Dist.CLIENT)
public final class NecroClientEvents {
    /** Pages of the Necronomicon's guide, read from the language file. */
    public static final int GUIDE_PAGES = 12;
    private static long nextShot;

    private NecroClientEvents() {
    }

    /** Isaac's Head: the tear keys fire while it is worn (one tear every third of a second, checked on the server too). */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive() || mc.screen != null) {
            return;
        }
        boolean tear = NecroClient.SHOOT_TEAR.consumeClick() || NecroClient.SHOOT_TEAR.isDown();
        boolean blood = NecroClient.SHOOT_BLOOD_TEAR.consumeClick() || NecroClient.SHOOT_BLOOD_TEAR.isDown();
        if (!(tear || blood) || !player.getItemBySlot(EquipmentSlot.HEAD).is(NecroItems.ISAACS_HEAD.get())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= nextShot) {
            PacketDistributor.sendToServer(new TearShotPayload(blood));
            nextShot = now + 333;
        }
    }

    /** Vanilla mob heads that work as Minion heads get the same tooltip as sewn parts. */
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof BlockItem) {
            List<Component> lines = new ArrayList<>();
            BodyPartItem.addPartTooltip(event.getItemStack(), lines);
            event.getToolTip().addAll(lines);
        }
    }

    /** Opens the Necronomicon's guide (called from the item on the client). */
    public static void openGuide() {
        List<Component> pages = new ArrayList<>();
        for (int i = 1; i <= GUIDE_PAGES; i++) {
            if (i == GUIDE_PAGES && com.sirolf2009.necromancy.compat.OreSpawnCompat.loaded()) {
                // before the last page: the creatures OreSpawn brings
                pages.add(Component.translatable("book.necromancy.guide.orespawn"));
            }
            pages.add(Component.translatable("book.necromancy.guide." + i));
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }
}
