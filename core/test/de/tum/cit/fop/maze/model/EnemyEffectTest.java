package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.model.weapons.WeaponEffect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnemyEffectTest {

    @Test
    public void testApplyFreezeEffect() {
        Enemy enemy = new Enemy(0, 0);
        assertEquals(WeaponEffect.NONE, enemy.getCurrentEffect());

        enemy.applyEffect(WeaponEffect.FREEZE);

        assertEquals(WeaponEffect.FREEZE, enemy.getCurrentEffect());
        // Verify update logic (freeze stops movement, etc. logic is internal but we can
        // check if effect persists)

        enemy.update(1.0f, new Player(0, 0), null, null); // Simulate 1 sec
        assertEquals(WeaponEffect.FREEZE, enemy.getCurrentEffect());

        enemy.update(1.1f, new Player(0, 0), null, null); // Simulate another 1.1 sec (total > 2.0)
        assertEquals(WeaponEffect.NONE, enemy.getCurrentEffect());
    }

    @Test
    public void testApplyBurnEffect() {
        Enemy enemy = new Enemy(0, 0);
        int initialHealth = enemy.getHealth();

        enemy.applyEffect(WeaponEffect.BURN);
        assertEquals(WeaponEffect.BURN, enemy.getCurrentEffect());

        // Simulate 0.5s - No damage yet
        enemy.update(0.5f, new Player(0, 0), null, null);
        assertEquals(initialHealth, enemy.getHealth());

        // Simulate 0.6s - Total 1.1s -> Should take damage
        enemy.update(0.6f, new Player(0, 0), null, null);
        assertEquals(initialHealth - 1, enemy.getHealth());

        // Simulate 2.0s more -> Total 3.1s -> Should be expired
        enemy.update(2.0f, new Player(0, 0), null, null);
        assertEquals(WeaponEffect.NONE, enemy.getCurrentEffect());
    }
}
