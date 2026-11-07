package com.sirolf2009.necromancy.entity.isaac;

import com.sirolf2009.necromancy.entity.BloodTearEntity;
import com.sirolf2009.necromancy.entity.TearEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Isaac's severed Head: 40 health, still crying blood. Drops Isaac's Head (loot table). */
public class IsaacHeadEntity extends IsaacEntity {
    public IsaacHeadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IsaacBodyEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected TearEntity createTear() {
        return new BloodTearEntity(level(), this);
    }

    @Override
    protected EntityType<? extends Mob>[] nextStages() {
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob>[] none = new EntityType[0];
        return none;
    }
}
