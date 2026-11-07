package com.sirolf2009.necromancy.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NecroAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Necromancy.MOD_ID);

    /** A necromancer's standing orders for their Minions (/minion). The live Minion count is in MinionCensus. */
    public static final Supplier<AttachmentType<NecromancerData>> NECROMANCER = ATTACHMENTS.register("necromancer",
            () -> AttachmentType.builder(NecromancerData::new).serialize(NecromancerData.CODEC).copyOnDeath().build());

    private NecroAttachments() {
    }

    public static final class NecromancerData {
        public static final Codec<NecromancerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("aggressive", false).forGetter(d -> d.aggressive),
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.BOOL).optionalFieldOf("relations", Map.of()).forGetter(d -> d.relations)
        ).apply(instance, NecromancerData::new));

        private boolean aggressive;
        /** true = friend, false = enemy */
        private final Map<UUID, Boolean> relations;

        public NecromancerData() {
            this(false, Map.of());
        }

        private NecromancerData(boolean aggressive, Map<UUID, Boolean> relations) {
            this.aggressive = aggressive;
            this.relations = new HashMap<>(relations);
        }

        public boolean aggressive() {
            return aggressive;
        }

        public void setAggressive(boolean aggressive) {
            this.aggressive = aggressive;
        }

        public boolean isFriend(UUID player) {
            return Boolean.TRUE.equals(relations.get(player));
        }

        public boolean isEnemy(UUID player) {
            return Boolean.FALSE.equals(relations.get(player));
        }

        public void setFriend(UUID player) {
            relations.put(player, true);
        }

        public void setEnemy(UUID player) {
            relations.put(player, false);
        }

        public void forget(UUID player) {
            relations.remove(player);
        }

        public Map<UUID, Boolean> relations() {
            return relations;
        }
    }
}
