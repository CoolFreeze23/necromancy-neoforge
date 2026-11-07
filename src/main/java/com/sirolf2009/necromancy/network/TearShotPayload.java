package com.sirolf2009.necromancy.network;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.entity.BloodTearEntity;
import com.sirolf2009.necromancy.entity.TearEntity;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroSounds;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the wearer of Isaac's Head pressed a tear key. */
public record TearShotPayload(boolean blood) implements CustomPacketPayload {
    public static final Type<TearShotPayload> TYPE = new Type<>(Necromancy.id("tear_shot"));
    public static final StreamCodec<ByteBuf, TearShotPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(TearShotPayload::new, TearShotPayload::blood);

    /** Server-side rate limit, matching the client's 1/3 second. */
    private static final int COOLDOWN_TICKS = 6;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TearShotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(NecroItems.ISAACS_HEAD.get())) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong("necromancy:last_tear");
        if (now - last < COOLDOWN_TICKS && now >= last) {
            return;
        }
        player.getPersistentData().putLong("necromancy:last_tear", now);
        if (payload.blood() && !player.getAbilities().instabuild) {
            player.hurt(player.damageSources().magic(), 1F);
        }
        TearEntity tear = payload.blood() ? new BloodTearEntity(player.level(), player) : new TearEntity(player.level(), player);
        tear.shootFromRotation(player, player.getXRot(), player.getYHeadRot(), 0F, 1.5F, 1F);
        player.level().addFreshEntity(tear);
        player.level().playSound(null, player, NecroSounds.TEAR.get(), SoundSource.PLAYERS, 1F, 1F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
