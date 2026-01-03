package de.tum.cit.fop.maze.model.weapons;

/**
 * A powerful magical staff with burn effect.
 */
public class MagicStaff extends Weapon {

    public MagicStaff(float x, float y) {
        // High damage, medium range, slow cooldown, Burn effect
        super(x, y, "Fire Staff", 2, 3.0f, 1.2f, WeaponEffect.BURN);
    }

    @Override
    public String getDescription() {
        return "Unleashes the power of fire.";
    }
}
