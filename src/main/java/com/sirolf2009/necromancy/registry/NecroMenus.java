package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.menu.AltarMenu;
import com.sirolf2009.necromancy.menu.SewingMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Necromancy.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AltarMenu>> ALTAR = MENUS.register("summoning_altar",
            () -> IMenuTypeExtension.create(AltarMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<SewingMenu>> SEWING = MENUS.register("sewing_machine",
            () -> IMenuTypeExtension.create(SewingMenu::fromNetwork));

    private NecroMenus() {
    }
}
