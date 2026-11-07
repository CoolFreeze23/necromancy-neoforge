package com.sirolf2009.necromancy.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Every creature that can lend its body to a Minion. Other mods add theirs with {@link #register} during mod
 * construction; the lookup by item is built lazily once items exist.
 */
public final class NecroMobRegistry {
    private static final Map<ResourceLocation, NecroMob> MOBS = new LinkedHashMap<>();
    private static Map<Item, PartRef> byItem;

    private NecroMobRegistry() {
    }

    public static synchronized NecroMob register(NecroMob mob) {
        if (MOBS.putIfAbsent(mob.id(), mob) != null) {
            throw new IllegalStateException("Necro mob " + mob.id() + " is registered twice");
        }
        byItem = null;
        return mob;
    }

    public static Collection<NecroMob> all() {
        return Collections.unmodifiableCollection(MOBS.values());
    }

    @Nullable
    public static NecroMob get(@Nullable ResourceLocation id) {
        return id == null ? null : MOBS.get(id);
    }

    /** Reads a creature part stack's mob and type (set by the component registry). */
    public static java.util.function.Function<ItemStack, PartRef> CREATURE_PART_READER = stack -> null;

    public static Optional<PartRef> partOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        PartRef creature = CREATURE_PART_READER.apply(stack);
        if (creature != null) {
            return Optional.of(creature);
        }
        return Optional.ofNullable(index().get(stack.getItem()));
    }

    private static synchronized Map<Item, PartRef> index() {
        if (byItem == null) {
            Map<Item, PartRef> map = new LinkedHashMap<>();
            for (NecroMob mob : MOBS.values()) {
                mob.parts().forEach((type, item) -> {
                    if (!mob.isCreaturePart(type)) {
                        map.putIfAbsent(item.get(), new PartRef(mob, type));
                    }
                });
            }
            byItem = map;
        }
        return byItem;
    }

    /** A body part item: which mob it comes from and what kind of part it is. */
    public record PartRef(NecroMob mob, PartType type) {
        public boolean fits(BodyPartLocation location) {
            return location.type() == type;
        }
    }
}
