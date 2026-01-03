package de.tum.cit.fop.maze.model.weapons;

import de.tum.cit.fop.maze.model.GameObject;

/**
 * Abstract base class for all weapons.
 */
public abstract class Weapon extends GameObject {
    protected String name;
    protected int damage;
    protected float range;
    protected float cooldown;
    protected WeaponEffect effect;

    public Weapon(float x, float y, String name, int damage, float range, float cooldown, WeaponEffect effect) {
        super(x, y);
        this.name = name;
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.effect = effect;
        // Weapon items (pickups) are small
        this.width = 0.5f;
        this.height = 0.5f;
    }

    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    public float getRange() {
        return range;
    }

    public float getCooldown() {
        return cooldown;
    }

    public WeaponEffect getEffect() {
        return effect;
    }

    // Abstract method to allow unique behavior if needed,
    // though for now stats might be enough.
    public abstract String getDescription();
}
