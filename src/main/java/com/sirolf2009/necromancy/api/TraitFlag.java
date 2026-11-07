package com.sirolf2009.necromancy.api;

/** Passive properties a Minion gets from its parts. */
public enum TraitFlag {
    /** Climbs walls like a spider. */
    CLIMBS,
    /** Burns in daylight unless it wears a helmet (undead rule). */
    BURNS_IN_SUN,
    /** Hurt by water and rain (endermen, blazes, snow golems). */
    HURT_BY_WATER,
    /**
     * Counts towards fire immunity: a Minion is fire immune when its torso and legs both have this, or its torso has it
     * and it has no legs (a snake or fish body).
     */
    FIRE_PROOF,
    /** Never takes sunburn even with other undead parts (husk head). */
    SUN_PROOF,
    /** Can take a job when its head has this (villager head). */
    THINKS,
    /** Sinks instead of swimming (iron golem). */
    SINKS,
    /** Never runs out of air when its head or torso has this (gills). */
    BREATHES_WATER,
    /** Ignores cactus and sweet berry bush damage (OreSpawn's hides). */
    CACTUS_PROOF,
    /** Ignores suffocation in walls (burrowers). */
    IN_WALL_PROOF,
    /** A killing blow bottles the victim's soul for the owner, like a scythe (arms). */
    REAPS,
    /** Bobs on the water surface; stronger than {@link #SINKS}. */
    FLOATS
}
