package com.sirolf2009.necromancy.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.sirolf2009.necromancy.menu.AltarMenu;

import net.minecraft.network.chat.Component;

/** The text in the altar's side panel. */
public final class AltarInfo {
    private AltarInfo() {
    }

    public static List<Component> lines(AltarMenu menu) {
        return new ArrayList<>(menu.infoLines());
    }
}
