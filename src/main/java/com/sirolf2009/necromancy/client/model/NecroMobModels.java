package com.sirolf2009.necromancy.client.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.resources.ResourceLocation;

/**
 * The look of every necro mob's parts, by mob id. Built lazily (and again after a resource reload) from the vanilla
 * model layer definitions, so the parts always match the vanilla textures.
 */
public final class NecroMobModels {
    private static final Map<ResourceLocation, Consumer<Builder>> FACTORIES = new HashMap<>();
    private static Map<ResourceLocation, Map<PartType, PartModel>> baked;

    private NecroMobModels() {
    }

    /** Registers the parts of one mob. Call during client setup. */
    public static void register(ResourceLocation mob, Consumer<Builder> factory) {
        FACTORIES.put(mob, factory);
        baked = null;
    }

    @Nullable
    public static PartModel get(@Nullable ResourceLocation mob, PartType type) {
        if (mob == null) {
            return null;
        }
        Map<PartType, PartModel> parts = bake().get(mob);
        return parts == null ? null : parts.get(type);
    }

    public static void invalidate() {
        baked = null;
    }

    private static synchronized Map<ResourceLocation, Map<PartType, PartModel>> bake() {
        if (baked == null) {
            Map<ResourceLocation, Map<PartType, PartModel>> map = new HashMap<>();
            FACTORIES.forEach((id, factory) -> {
                Builder builder = new Builder();
                try {
                    factory.accept(builder);
                } catch (RuntimeException e) {
                    Necromancy.LOGGER.error("Could not build the Minion parts of {}", id, e);
                }
                map.put(id, builder.parts);
            });
            baked = map;
        }
        return baked;
    }

    public static final class Builder {
        private final Map<PartType, PartModel> parts = new EnumMap<>(PartType.class);

        public Builder put(PartType type, PartModel model) {
            parts.put(type, model);
            return this;
        }
    }
}
