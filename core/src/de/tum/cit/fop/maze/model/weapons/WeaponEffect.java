package de.tum.cit.fop.maze.model.weapons;

/**
 * Combat styling and status effects for weapons.
 */
public enum WeaponEffect {
    NONE,
    FREEZE, // Slows or stops enemy
    BURN, // Damage over time
    POISON // Damage over time, maybe slower than burn
}
