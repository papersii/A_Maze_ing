package de.tum.cit.fop.maze.model.weapons;

/**
 * A basic melee weapon.
 */
public class Sword extends Weapon {

    public Sword(float x, float y) {
        super(x, y, "Iron Sword", 1, 2.2f, 0.3f, WeaponEffect.NONE);
    }

    @Override
    public String getDescription() {
        return "A sharp blade. Reliable but plain.";
    }
}
