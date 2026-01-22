package de.tum.cit.fop.maze.model.weapons;

/**
 * A basic melee weapon.
 */
public class Sword extends Weapon {

    public Sword(float x, float y) {
        super(x, y, "Iron Sword", 10, 2.2f, 0.3f, WeaponEffect.NONE);

        // Override with custom values if available
        de.tum.cit.fop.maze.custom.CustomElementManager manager = de.tum.cit.fop.maze.custom.CustomElementManager
                .getInstance();
        de.tum.cit.fop.maze.custom.CustomElementDefinition def = manager.getElementByName("Iron Sword");

        if (def != null && def.getType() == de.tum.cit.fop.maze.custom.ElementType.WEAPON) {
            this.damage = def.getIntProperty("damage");
            this.range = def.getFloatProperty("range");
            this.cooldown = def.getFloatProperty("cooldown");

            String effectName = def.getProperty("effect", String.class);
            if (effectName != null) {
                try {
                    this.effect = WeaponEffect.valueOf(effectName);
                } catch (IllegalArgumentException e) {
                    // Start with default
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
        return "A sharp blade. Reliable but plain.";
    }
}
