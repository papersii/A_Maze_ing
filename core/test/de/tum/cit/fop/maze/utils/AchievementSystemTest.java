package de.tum.cit.fop.maze.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the Achievement System.
 * Tests data models, rarity, category and progress tracking logic.
 * Note: Tests that require LibGDX Preferences are marked for manual testing.
 */
public class AchievementSystemTest {

    @BeforeEach
    public void setUp() {
        // Reset all achievements before each test
        // Note: In actual LibGDX environment, this would require mocking
        // Gdx.app.getPreferences()
    }

    // === Achievement Model Tests ===

    @Test
    public void testAchievementCreation() {
        Achievement achievement = new Achievement(
                "test_achievement",
                "Test Achievement",
                "This is a test",
                AchievementRarity.RARE,
                AchievementCategory.WEAPON);

        assertEquals("test_achievement", achievement.getId());
        assertEquals("Test Achievement", achievement.getName());
        assertEquals(AchievementRarity.RARE, achievement.getRarity());
        assertEquals(AchievementCategory.WEAPON, achievement.getCategory());
        assertFalse(achievement.isUnlocked());
        assertEquals(0, achievement.getCurrentProgress());
    }

    @Test
    public void testAchievementProgress() {
        Achievement achievement = new Achievement(
                "mastery_test",
                "Mastery Test",
                "Kill 25 enemies",
                AchievementRarity.RARE,
                AchievementCategory.COMBAT,
                25 // Required count
        );

        assertFalse(achievement.isUnlocked());
        assertEquals("0/25", achievement.getProgressString());
        assertEquals(0.0f, achievement.getProgressPercentage(), 0.01f);

        // Add progress
        achievement.addProgress(10);
        assertEquals(10, achievement.getCurrentProgress());
        assertEquals(0.4f, achievement.getProgressPercentage(), 0.01f);
        assertFalse(achievement.isUnlocked());

        // Complete
        boolean unlocked = achievement.addProgress(15);
        assertTrue(unlocked);
        assertTrue(achievement.isUnlocked());
        assertEquals(1.0f, achievement.getProgressPercentage(), 0.01f);
    }

    @Test
    public void testAchievementUnlockOnce() {
        Achievement achievement = new Achievement(
                "one_time",
                "One Time",
                "First blood",
                AchievementRarity.COMMON,
                AchievementCategory.COMBAT);

        // First unlock should return true
        assertTrue(achievement.unlock());
        assertTrue(achievement.isUnlocked());

        // Second unlock should return false (already unlocked)
        assertFalse(achievement.unlock());
    }

    // === Rarity Tests ===

    @Test
    public void testRarityGoldRewards() {
        assertEquals(10, AchievementRarity.COMMON.getGoldReward());
        assertEquals(30, AchievementRarity.RARE.getGoldReward());
        assertEquals(100, AchievementRarity.EPIC.getGoldReward());
        assertEquals(300, AchievementRarity.LEGENDARY.getGoldReward());
    }

    @Test
    public void testRarityDisplayName() {
        assertEquals("Common", AchievementRarity.COMMON.getDisplayName());
        assertEquals("Rare", AchievementRarity.RARE.getDisplayName());
        assertEquals("Epic", AchievementRarity.EPIC.getDisplayName());
        assertEquals("Legendary", AchievementRarity.LEGENDARY.getDisplayName());
    }

    // === Category Tests ===

    @Test
    public void testCategoryProperties() {
        assertEquals("Weapons", AchievementCategory.WEAPON.getDisplayName());
        assertEquals("[W]", AchievementCategory.WEAPON.getIcon());

        assertEquals("Armor", AchievementCategory.ARMOR.getDisplayName());
        assertEquals("[A]", AchievementCategory.ARMOR.getIcon());

        assertEquals("Combat", AchievementCategory.COMBAT.getDisplayName());
        assertEquals("[C]", AchievementCategory.COMBAT.getIcon());
    }

    // === Achievement Definitions Tests ===

    @Test
    public void testAllAchievementsInitialized() {
        Map<String, Achievement> achievements = AchievementManager.getAllAchievements();

        // Should have many achievements defined
        assertTrue(achievements.size() >= 30, "Should have at least 30 achievements");

        // Check some key achievements exist
        assertTrue(achievements.containsKey("sword_collector"), "Should have sword_collector");
        assertTrue(achievements.containsKey("arsenal_complete"), "Should have arsenal_complete");
        assertTrue(achievements.containsKey("first_blood"), "Should have first_blood");
        assertTrue(achievements.containsKey("world_champion"), "Should have world_champion");
        assertTrue(achievements.containsKey("flawless_victory"), "Should have flawless_victory");
    }

    @Test
    public void testAchievementsByCategory() {
        List<Achievement> weaponAchievements = AchievementManager.getAchievementsByCategory(AchievementCategory.WEAPON);
        List<Achievement> combatAchievements = AchievementManager.getAchievementsByCategory(AchievementCategory.COMBAT);

        // Should have several achievements in each category
        assertTrue(weaponAchievements.size() >= 5, "Should have weapon achievements");
        assertTrue(combatAchievements.size() >= 5, "Should have combat achievements");
    }

    @Test
    public void testAchievementRaritiesDistributed() {
        Map<String, Achievement> achievements = AchievementManager.getAllAchievements();

        int commonCount = 0;
        int rareCount = 0;
        int epicCount = 0;
        int legendaryCount = 0;

        for (Achievement a : achievements.values()) {
            switch (a.getRarity()) {
                case COMMON:
                    commonCount++;
                    break;
                case RARE:
                    rareCount++;
                    break;
                case EPIC:
                    epicCount++;
                    break;
                case LEGENDARY:
                    legendaryCount++;
                    break;
            }
        }

        // Should have a good distribution
        assertTrue(commonCount > 0, "Should have common achievements");
        assertTrue(rareCount > 0, "Should have rare achievements");
        assertTrue(epicCount > 0, "Should have epic achievements");
        assertTrue(legendaryCount > 0, "Should have legendary achievements");

        // Common should be the most common
        assertTrue(commonCount >= rareCount, "Common should be most frequent");
    }

    // === Progress Tracking Logic Tests (without Preferences) ===

    @Test
    public void testAchievementProgressString() {
        Achievement countBased = new Achievement(
                "count_based", "Count Based", "Do 50 things",
                AchievementRarity.RARE, AchievementCategory.COMBAT, 50);

        assertEquals("0/50", countBased.getProgressString());

        countBased.addProgress(25);
        assertEquals("25/50", countBased.getProgressString());

        countBased.addProgress(25);
        assertEquals("Complete", countBased.getProgressString());
    }

    @Test
    public void testOneTimeAchievementNoProgress() {
        Achievement oneTime = new Achievement(
                "one_time", "One Time", "Do something once",
                AchievementRarity.COMMON, AchievementCategory.COMBAT);

        assertTrue(oneTime.isOneTimeAchievement());
        assertEquals("Not Started", oneTime.getProgressString());

        oneTime.unlock();
        assertEquals("Complete", oneTime.getProgressString());
    }

    @Test
    public void testAchievementProgressPercentage() {
        Achievement percentTest = new Achievement(
                "percent", "Percent", "Progress test",
                AchievementRarity.EPIC, AchievementCategory.EXPLORATION, 100);

        assertEquals(0.0f, percentTest.getProgressPercentage(), 0.001f);

        percentTest.addProgress(50);
        assertEquals(0.5f, percentTest.getProgressPercentage(), 0.001f);

        percentTest.addProgress(50);
        assertEquals(1.0f, percentTest.getProgressPercentage(), 0.001f);
    }

    @Test
    public void testAchievementToString() {
        Achievement test = new Achievement(
                "test", "Test Name", "Test Description",
                AchievementRarity.EPIC, AchievementCategory.WEAPON);

        String str = test.toString();
        assertTrue(str.contains("(E)"), "Should contain rarity icon");
        assertTrue(str.contains("Test Name"), "Should contain name");
        assertTrue(str.contains("🔒"), "Should contain lock symbol");
    }

    @Test
    public void testGoldRewardFromAchievement() {
        Achievement common = new Achievement(
                "common", "Common", "desc",
                AchievementRarity.COMMON, AchievementCategory.COMBAT);
        Achievement legendary = new Achievement(
                "legendary", "Legendary", "desc",
                AchievementRarity.LEGENDARY, AchievementCategory.COMBAT);

        assertEquals(10, common.getGoldReward());
        assertEquals(300, legendary.getGoldReward());
    }
}
