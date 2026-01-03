package de.tum.cit.fop.maze.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerAnimationTest {

    @Test
    void testAttackAnimationState() {
        Player player = new Player(0, 0);

        assertFalse(player.isAttacking(), "Player should not be attacking initially");

        player.startAttackAnimation(0.2f);
        assertTrue(player.isAttacking(), "Player should be attacking after startAttackAnimation()");

        // Create a dummy CollisionManager for testing
        // CollisionManager dummyCm = new CollisionManager(null); // Assuming
        // constructor accepts null or we need to mock it properly
        // Ideally we should mock it, but CollisionManager might modify state.
        // For animation tests, update(delta, cm) is just to advance timers.
        // Let's try passing null if the method handles it or create a simple
        // subclass/mock?
        // Player.update accesses CM inside the physics block. Physics block might not
        // run if vx/vy is 0.
        // So passing null might be safe if no knockback is active.
        player.update(0.1f, null);
        assertTrue(player.isAttacking(), "Player should still be attacking after 0.1s (duration is 0.2s)");

        player.update(0.11f, null); // Total 0.21s
        assertFalse(player.isAttacking(), "Player should stop attacking after animation duration expires");
    }
}
