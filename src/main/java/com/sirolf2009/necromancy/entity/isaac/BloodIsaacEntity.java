package com.sirolf2009.necromancy.entity.isaac;

import com.sirolf2009.necromancy.entity.BloodTearEntity;
import com.sirolf2009.necromancy.entity.TearEntity;
import com.sirolf2009.necromancy.registry.NecroEntities;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Blood Isaac: 75 health, blood tears, a boss bar. Splits into Isaac's Head and Isaac's Body on death. */
public class BloodIsaacEntity extends IsaacEntity {
    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);

    public BloodIsaacEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IsaacBodyEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 75.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected TearEntity createTear() {
        return new BloodTearEntity(level(), this);
    }

    @Override
    protected EntityType<? extends Mob>[] nextStages() {
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob>[] next = new EntityType[] { NecroEntities.ISAAC_BODY.get(), NecroEntities.ISAAC_HEAD.get() };
        return next;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }
}
