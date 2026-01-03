package de.tum.cit.fop.maze.model.weapons;

/**
 * A ranged weapon with freeze effect (Ice Bow).
 */
public class Bow extends Weapon {

    public Bow(float x, float y) {
        // Lower damage, longer range, freeze effect
        super(x, y, "Ice Bow", 1, 5.0f, 0.8f, WeaponEffect.FREEZE);
    }

    @Override
    public String getDescription() {
        return "Chills enemies to the bone.";
    }
}
