package de.tum.cit.fop.maze.model.weapons;

import de.tum.cit.fop.maze.model.DamageType;

/**
 * A powerful magical staff with burn effect.
 */
public class MagicStaff extends Weapon {

    public MagicStaff(float x, float y) {
        // High damage, medium range, fast cooldown for rapid fire, Burn effect
        // Fully initialized as Ranged: isRanged=true, speed 12
        super(x, y, "Fire Staff", 2, 6.0f, 0.25f, WeaponEffect.BURN, DamageType.MAGICAL, true, 1.2f, 12f);

        // Override with custom values if available
        de.tum.cit.fop.maze.custom.CustomElementManager manager = de.tum.cit.fop.maze.custom.CustomElementManager
                .getInstance();
        de.tum.cit.fop.maze.custom.CustomElementDefinition def = manager.getElementByName("Fire Staff");

        if (def != null && def.getType() == de.tum.cit.fop.maze.custom.ElementType.WEAPON) {
            this.damage = def.getIntProperty("damage");
            this.range = def.getFloatProperty("range");
            this.cooldown = def.getFloatProperty("cooldown");

            String effectName = def.getProperty("effect", String.class);
            if (effectName != null) {
                try {
                    this.effect = WeaponEffect.valueOf(effectName);
                } catch (IllegalArgumentException e) {
                    // Keep default
                }
            }

            Object rangedObj = def.getProperties().get("isRanged");
            if (rangedObj instanceof Boolean) {
                this.isRanged = (Boolean) rangedObj;
            } else if (rangedObj instanceof String) {
                this.isRanged = Boolean.parseBoolean((String) rangedObj);
            }
            if (def.getProperties().containsKey("projectileSpeed")) {
                this.projectileSpeed = def.getFloatProperty("projectileSpeed");
            }
        }
    }

    @Override
    public String getDescription() {
        return "Unleashes the power of fire.";
    }
}
