package com.sirolf2009.necromancy.api;

/**
 * Attribute modifiers a body part adds to its Minion. Every value is relative to the Minion's base value and added on top
 * (operation ADD_MULTIPLIED_BASE), exactly like the original NecroAPI: base values are 20 health, 16 follow range,
 * 0.1 knockback resistance, 0.1 movement speed and 2 attack damage, so a health of 1 adds another 20 health.
 */
public record PartStats(double health, double followRange, double knockbackResistance, double movementSpeed, double attackDamage) {
    public static final PartStats NONE = new PartStats(0, 0, 0, 0, 0);

    public static PartStats of(double health, double followRange, double knockbackResistance, double movementSpeed, double attackDamage) {
        return new PartStats(health, followRange, knockbackResistance, movementSpeed, attackDamage);
    }

    public boolean isEmpty() {
        return health == 0 && followRange == 0 && knockbackResistance == 0 && movementSpeed == 0 && attackDamage == 0;
    }
}
