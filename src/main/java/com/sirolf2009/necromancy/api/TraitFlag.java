package com.sirolf2009.necromancy.api;

/** Passive properties a Minion gets from its parts. */
public enum TraitFlag {
    /** Climbs walls like a spider. */
    CLIMBS,
    /** Burns in daylight unless it wears a helmet (undead rule). */
    BURNS_IN_SUN,
    /** Hurt by water and rain (endermen, blazes, snow golems). */
    HURT_BY_WATER,
    /** Counts towards fire immunity (a Minion is fire immune when its torso and legs both have this). */
    FIRE_PROOF,
    /** Never takes sunburn even with other undead parts (husk head). */
    SUN_PROOF,
    /** Can take a job when its head has this (villager head). */
    THINKS,
    /** Sinks instead of swimming (iron golem). */
    SINKS
}
