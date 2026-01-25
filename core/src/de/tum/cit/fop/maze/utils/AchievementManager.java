package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages achievements and unlocked cards.
 * Persists data using LibGDX Preferences.
 * 
 * Achievement Categories:
 * - Kill-based: Novice Hunter, Veteran Slayer, Maze Master, etc.
 * - Weapon Collection: First pickup of each weapon type
 * - Weapon Mastery: Kills per weapon type
 * - Armor Collection: First equip of each armor type
 * - Armor Defense: Damage absorbed milestones
 * - Economy: Coin milestones
 * - Challenge: Flawless victory, speedrun, etc.
 */
public class AchievementManager {
    private static final String PREFS_NAME = "maze_achievements_v1";
    private static final String UNLOCKED_CARDS_KEY = "unlocked_cards";
    private static final String TOTAL_COINS_KEY = "total_coins_earned";

    // === NEW: Statistics Keys ===
    private static final String STATS_PREFIX = "stats_";
    private static final String WEAPON_KILLS_PREFIX = "weapon_kills_";
    private static final String ARMOR_ABSORBED_PREFIX = "armor_absorbed_";
    private static final String LEVELS_COMPLETED_KEY = "levels_completed";
    private static final String FLAWLESS_LEVELS_KEY = "flawless_levels";
    private static final String TOTAL_KILLS_KEY = "total_kills";
    private static final String MAX_COMBO_KEY = "max_combo_kills";
    private static final String ACHIEVEMENT_PROGRESS_PREFIX = "progress_";

    // === NEW: Achievement Definitions ===
    private static Map<String, Achievement> allAchievements;

    static {
        initializeAchievements();
    }

    /**
     * Initialize all achievement definitions
     */
    private static void initializeAchievements() {
        allAchievements = new HashMap<>();

        // === Weapon Collection Achievements (COMMON) ===
        registerAchievement(new Achievement("sword_collector", "Sword Collector",
                "Pick up your first sword",
                AchievementRarity.COMMON, AchievementCategory.WEAPON));
        registerAchievement(new Achievement("bow_hunter", "Bow Hunter",
                "Pick up your first bow",
                AchievementRarity.COMMON, AchievementCategory.WEAPON));
        registerAchievement(new Achievement("staff_wielder", "Staff Wielder",
                "Pick up your first staff",
                AchievementRarity.COMMON, AchievementCategory.WEAPON));
        registerAchievement(new Achievement("crossbow_expert", "Crossbow Expert",
                "Pick up your first crossbow",
                AchievementRarity.COMMON, AchievementCategory.WEAPON));
        registerAchievement(new Achievement("wand_master", "Wand Master",
                "Pick up your first wand",
                AchievementRarity.COMMON, AchievementCategory.WEAPON));
        registerAchievement(new Achievement("arsenal_complete", "Arsenal Complete",
                "Collect all 5 weapon types",
                AchievementRarity.EPIC, AchievementCategory.WEAPON));

        // === Weapon Mastery Achievements (RARE/EPIC) ===
        registerAchievement(new Achievement("blade_dancer", "Blade Dancer",
                "Kill 25 enemies with Sword",
                AchievementRarity.RARE, AchievementCategory.WEAPON, 25));
        registerAchievement(new Achievement("sword_saint", "Sword Saint",
                "Kill 100 enemies with Sword",
                AchievementRarity.EPIC, AchievementCategory.WEAPON, 100));
        registerAchievement(new Achievement("frozen_heart", "Frozen Heart",
                "Apply freeze effect 50 times with Ice Bow",
                AchievementRarity.RARE, AchievementCategory.WEAPON, 50));
        registerAchievement(new Achievement("pyromaniac", "Pyromaniac",
                "Apply burn effect 50 times",
                AchievementRarity.RARE, AchievementCategory.WEAPON, 50));
        registerAchievement(new Achievement("sharpshooter", "Sharpshooter",
                "Kill 50 enemies with Crossbow",
                AchievementRarity.RARE, AchievementCategory.WEAPON, 50));
        registerAchievement(new Achievement("arcane_apprentice", "Arcane Apprentice",
                "Kill 50 enemies with Magic Wand",
                AchievementRarity.RARE, AchievementCategory.WEAPON, 50));
        registerAchievement(new Achievement("master_of_arms", "Master of Arms",
                "Unlock all weapon achievements",
                AchievementRarity.LEGENDARY, AchievementCategory.WEAPON));

        // === Armor Achievements (COMMON/RARE/EPIC) ===
        registerAchievement(new Achievement("iron_clad", "Iron Clad",
                "Equip your first physical armor",
                AchievementRarity.COMMON, AchievementCategory.ARMOR));
        registerAchievement(new Achievement("arcane_protected", "Arcane Protected",
                "Equip your first magical armor",
                AchievementRarity.COMMON, AchievementCategory.ARMOR));
        registerAchievement(new Achievement("heavy_defender", "Heavy Defender",
                "Equip Knight's Plate",
                AchievementRarity.RARE, AchievementCategory.ARMOR));
        registerAchievement(new Achievement("grand_wizard", "Grand Wizard",
                "Equip Wizard's Cloak",
                AchievementRarity.RARE, AchievementCategory.ARMOR));
        registerAchievement(new Achievement("tank_mode", "Tank Mode",
                "Absorb 50 damage with armor",
                AchievementRarity.RARE, AchievementCategory.ARMOR, 50));
        registerAchievement(new Achievement("indestructible", "Indestructible",
                "Absorb 200 damage with armor",
                AchievementRarity.EPIC, AchievementCategory.ARMOR, 200));

        // === Combat Achievements (COMMON/RARE/EPIC/LEGENDARY) ===
        registerAchievement(new Achievement("first_blood", "First Blood",
                "Kill your first enemy",
                AchievementRarity.COMMON, AchievementCategory.COMBAT));
        registerAchievement(new Achievement("novice_hunter", "Novice Hunter",
                "Kill 1 enemy",
                AchievementRarity.COMMON, AchievementCategory.COMBAT, 1));
        registerAchievement(new Achievement("veteran_slayer", "Veteran Slayer",
                "Kill 5 enemies",
                AchievementRarity.COMMON, AchievementCategory.COMBAT, 5));
        registerAchievement(new Achievement("maze_master", "Maze Master",
                "Kill 10 enemies",
                AchievementRarity.COMMON, AchievementCategory.COMBAT, 10));
        registerAchievement(new Achievement("monster_slayer", "Monster Slayer",
                "Kill 25 enemies",
                AchievementRarity.RARE, AchievementCategory.COMBAT, 25));
        registerAchievement(new Achievement("legendary_hero", "Legendary Hero",
                "Kill 50 enemies",
                AchievementRarity.RARE, AchievementCategory.COMBAT, 50));
        registerAchievement(new Achievement("century_slayer", "Century Slayer",
                "Kill 100 enemies",
                AchievementRarity.EPIC, AchievementCategory.COMBAT, 100));
        registerAchievement(new Achievement("death_incarnate", "Death Incarnate",
                "Kill 500 enemies",
                AchievementRarity.LEGENDARY, AchievementCategory.COMBAT, 500));
        registerAchievement(new Achievement("rampage", "Rampage",
                "Kill 3 enemies within 5 seconds",
                AchievementRarity.RARE, AchievementCategory.COMBAT));
        registerAchievement(new Achievement("overkill", "Overkill",
                "Kill 5 enemies within 10 seconds",
                AchievementRarity.EPIC, AchievementCategory.COMBAT));

        // === Economy Achievements (COMMON/RARE/EPIC/LEGENDARY) ===
        registerAchievement(new Achievement("first_coin", "First Coin",
                "Collect your first coin",
                AchievementRarity.COMMON, AchievementCategory.ECONOMY));
        registerAchievement(new Achievement("coin_collector", "Coin Collector",
                "Collect 50 coins",
                AchievementRarity.COMMON, AchievementCategory.ECONOMY, 50));
        registerAchievement(new Achievement("wealthy_explorer", "Wealthy Explorer",
                "Collect 100 coins",
                AchievementRarity.RARE, AchievementCategory.ECONOMY, 100));
        registerAchievement(new Achievement("rich_adventurer", "Rich Adventurer",
                "Collect 500 coins",
                AchievementRarity.EPIC, AchievementCategory.ECONOMY, 500));
        registerAchievement(new Achievement("treasure_hunter", "Treasure Hunter",
                "Collect 1000 coins",
                AchievementRarity.LEGENDARY, AchievementCategory.ECONOMY, 1000));

        // === Exploration Achievements (COMMON/RARE/EPIC/LEGENDARY) ===
        registerAchievement(new Achievement("grassland_explorer", "Grassland Explorer",
                "Complete all Grassland levels (1-4)",
                AchievementRarity.COMMON, AchievementCategory.EXPLORATION));
        registerAchievement(new Achievement("desert_survivor", "Desert Survivor",
                "Complete all Desert levels (5-8)",
                AchievementRarity.COMMON, AchievementCategory.EXPLORATION));
        registerAchievement(new Achievement("ice_conqueror", "Ice Conqueror",
                "Complete all Ice levels (9-12)",
                AchievementRarity.RARE, AchievementCategory.EXPLORATION));
        registerAchievement(new Achievement("jungle_master", "Jungle Master",
                "Complete all Jungle levels (13-16)",
                AchievementRarity.RARE, AchievementCategory.EXPLORATION));
        registerAchievement(new Achievement("space_commander", "Space Commander",
                "Complete all Spaceship levels (17-20)",
                AchievementRarity.EPIC, AchievementCategory.EXPLORATION));
        registerAchievement(new Achievement("world_champion", "World Champion",
                "Complete all 20 levels",
                AchievementRarity.LEGENDARY, AchievementCategory.EXPLORATION));

        // === Challenge Achievements (EPIC/LEGENDARY) ===
        registerAchievement(new Achievement("flawless_victory", "Flawless Victory",
                "Complete a level without taking damage",
                AchievementRarity.EPIC, AchievementCategory.CHALLENGE));
        registerAchievement(new Achievement("speedrunner", "Speedrunner",
                "Complete a level in under 60 seconds",
                AchievementRarity.EPIC, AchievementCategory.CHALLENGE));
        registerAchievement(new Achievement("near_death", "Near Death",
                "Kill an enemy while at 1 HP",
                AchievementRarity.RARE, AchievementCategory.CHALLENGE));
        registerAchievement(new Achievement("comeback", "Comeback",
                "Complete a level with only 1 HP remaining",
                AchievementRarity.EPIC, AchievementCategory.CHALLENGE));
    }

    private static void registerAchievement(Achievement achievement) {
        allAchievements.put(achievement.getId(), achievement);
    }

    // === Original Methods (Backward Compatible) ===

    /**
     * Checks if new achievements are unlocked based on game stats.
     * 
     * @param killCount The number of enemies killed in the session.
     * @return A list of newly unlocked card names.
     */
    public static List<String> checkAchievements(int killCount) {
        List<String> newUnlocks = new ArrayList<>();

        if (killCount >= 1) {
            if (unlockCard("Novice Hunter"))
                newUnlocks.add("Novice Hunter");
        }
        if (killCount >= 5) {
            if (unlockCard("Veteran Slayer"))
                newUnlocks.add("Veteran Slayer");
        }
        if (killCount >= 10) {
            if (unlockCard("Maze Master"))
                newUnlocks.add("Maze Master");
        }
        if (killCount >= 25) {
            if (unlockCard("Monster Slayer"))
                newUnlocks.add("Monster Slayer");
        }
        if (killCount >= 50) {
            if (unlockCard("Legendary Hero"))
                newUnlocks.add("Legendary Hero");
        }

        return newUnlocks;
    }

    /**
     * Check achievements for weapon pickup.
     * 
     * @param weaponName The name of the picked up weapon
     * @return List of newly unlocked achievements
     */
    public static List<String> checkWeaponPickup(String weaponName) {
        List<String> newUnlocks = new ArrayList<>();

        String achievementName = null;
        switch (weaponName) {
            case "Steel Sword":
            case "Sword":
            case "Iron Sword":
                achievementName = "Sword Collector";
                break;
            case "Ice Bow":
            case "Bow":
                achievementName = "Bow Hunter";
                break;
            case "Fire Staff":
            case "MagicStaff":
                achievementName = "Staff Wielder";
                break;
            case "Crossbow":
                achievementName = "Crossbow Expert";
                break;
            case "Magic Wand":
            case "Wand":
                achievementName = "Wand Master";
                break;
        }

        if (achievementName != null && unlockCard(achievementName)) {
            newUnlocks.add(achievementName);
            GameLogger.info("AchievementManager", "Achievement unlocked: " + achievementName);
        }

        // Check if player has collected all weapons
        if (hasAllWeaponAchievements()) {
            if (unlockCard("Arsenal Complete"))
                newUnlocks.add("Arsenal Complete");
        }

        return newUnlocks;
    }

    /**
     * Check achievements for armor equip.
     * 
     * @param armorType "PHYSICAL" or "MAGICAL"
     * @return List of newly unlocked achievements
     */
    public static List<String> checkArmorPickup(String armorType) {
        List<String> newUnlocks = new ArrayList<>();

        String achievementName = null;
        if ("PHYSICAL".equals(armorType) || armorType.contains("Physical") || "PHYSICAL_ARMOR".equals(armorType)) {
            achievementName = "Iron Clad";
        } else if ("MAGICAL".equals(armorType) || armorType.contains("Magical") || "MAGICAL_ARMOR".equals(armorType)) {
            achievementName = "Arcane Protected";
        }

        if (achievementName != null && unlockCard(achievementName)) {
            newUnlocks.add(achievementName);
            GameLogger.info("AchievementManager", "Achievement unlocked: " + achievementName);
        }

        return newUnlocks;
    }

    /**
     * Check achievements for coin milestones.
     * 
     * @param coinsEarned Coins earned in current session
     * @return List of newly unlocked achievements
     */
    public static List<String> checkCoinMilestone(int coinsEarned) {
        List<String> newUnlocks = new ArrayList<>();

        // Update total coins
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        int totalCoins = prefs.getInteger(TOTAL_COINS_KEY, 0) + coinsEarned;
        prefs.putInteger(TOTAL_COINS_KEY, totalCoins);
        prefs.flush();

        if (totalCoins >= 1) {
            if (unlockCard("First Coin"))
                newUnlocks.add("First Coin");
        }
        if (totalCoins >= 50) {
            if (unlockCard("Coin Collector"))
                newUnlocks.add("Coin Collector");
        }
        if (totalCoins >= 100) {
            if (unlockCard("Wealthy Explorer"))
                newUnlocks.add("Wealthy Explorer");
        }
        if (totalCoins >= 500) {
            if (unlockCard("Rich Adventurer"))
                newUnlocks.add("Rich Adventurer");
        }
        if (totalCoins >= 1000) {
            if (unlockCard("Treasure Hunter"))
                newUnlocks.add("Treasure Hunter");
        }

        return newUnlocks;
    }

    /**
     * Check if player first kills an enemy.
     */
    public static List<String> checkFirstKill() {
        List<String> newUnlocks = new ArrayList<>();
        if (unlockCard("First Blood")) {
            newUnlocks.add("First Blood");
            GameLogger.info("AchievementManager", "Achievement unlocked: First Blood");
        }
        return newUnlocks;
    }

    // === NEW: Weapon Mastery Tracking ===

    /**
     * Record a kill with a specific weapon and check mastery achievements
     * 
     * @param weaponName Name of the weapon used
     * @return List of newly unlocked achievements
     */
    public static List<String> recordWeaponKill(String weaponName) {
        List<String> newUnlocks = new ArrayList<>();
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Normalize weapon name to key
        String weaponKey = normalizeWeaponKey(weaponName);
        String statsKey = WEAPON_KILLS_PREFIX + weaponKey;

        // Increment kill count
        int kills = prefs.getInteger(statsKey, 0) + 1;
        prefs.putInteger(statsKey, kills);
        prefs.flush();

        GameLogger.debug("AchievementManager",
                "Weapon kill recorded: " + weaponName + " -> " + kills + " total kills");

        // Check mastery achievements based on weapon type
        switch (weaponKey) {
            case "sword":
                if (kills >= 25 && unlockCard("Blade Dancer")) {
                    newUnlocks.add("Blade Dancer");
                }
                if (kills >= 100 && unlockCard("Sword Saint")) {
                    newUnlocks.add("Sword Saint");
                }
                break;
            case "crossbow":
                if (kills >= 50 && unlockCard("Sharpshooter")) {
                    newUnlocks.add("Sharpshooter");
                }
                break;
            case "wand":
                if (kills >= 50 && unlockCard("Arcane Apprentice")) {
                    newUnlocks.add("Arcane Apprentice");
                }
                break;
        }

        // Check if all weapon mastery achievements are unlocked
        if (hasAllWeaponMasteryAchievements() && unlockCard("Master of Arms")) {
            newUnlocks.add("Master of Arms");
        }

        return newUnlocks;
    }

    /**
     * Record effect application (freeze, burn)
     * 
     * @param effectName Name of the effect (FREEZE, BURN)
     * @return List of newly unlocked achievements
     */
    public static List<String> recordEffectApplied(String effectName) {
        List<String> newUnlocks = new ArrayList<>();
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        String statsKey = STATS_PREFIX + "effect_" + effectName.toLowerCase();
        int count = prefs.getInteger(statsKey, 0) + 1;
        prefs.putInteger(statsKey, count);
        prefs.flush();

        if ("FREEZE".equalsIgnoreCase(effectName) && count >= 50 && unlockCard("Frozen Heart")) {
            newUnlocks.add("Frozen Heart");
        }
        if ("BURN".equalsIgnoreCase(effectName) && count >= 50 && unlockCard("Pyromaniac")) {
            newUnlocks.add("Pyromaniac");
        }

        return newUnlocks;
    }

    // === NEW: Armor Defense Tracking ===

    /**
     * Record damage absorbed by armor
     * 
     * @param armorType      Type of armor (PHYSICAL or MAGICAL)
     * @param damageAbsorbed Amount of damage absorbed
     * @return List of newly unlocked achievements
     */
    public static List<String> recordArmorAbsorbed(String armorType, int damageAbsorbed) {
        List<String> newUnlocks = new ArrayList<>();
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Track per armor type
        String statsKey = ARMOR_ABSORBED_PREFIX + armorType.toLowerCase();
        int total = prefs.getInteger(statsKey, 0) + damageAbsorbed;
        prefs.putInteger(statsKey, total);

        // Track global armor absorbed
        String globalKey = ARMOR_ABSORBED_PREFIX + "total";
        int globalTotal = prefs.getInteger(globalKey, 0) + damageAbsorbed;
        prefs.putInteger(globalKey, globalTotal);
        prefs.flush();

        GameLogger.debug("AchievementManager",
                "Armor absorbed: " + armorType + " +" + damageAbsorbed + " (total: " + total + ")");

        // Check defense achievements
        if (globalTotal >= 50 && unlockCard("Tank Mode")) {
            newUnlocks.add("Tank Mode");
        }
        if (globalTotal >= 200 && unlockCard("Indestructible")) {
            newUnlocks.add("Indestructible");
        }

        return newUnlocks;
    }

    // === NEW: Level Completion Tracking ===

    /**
     * Record level completion and check related achievements
     * 
     * @param levelPath      Path to the completed level
     * @param tookDamage     Whether the player took damage during the level
     * @param completionTime Time to complete the level in seconds
     * @return List of newly unlocked achievements
     */
    public static List<String> recordLevelComplete(String levelPath, boolean tookDamage, float completionTime) {
        List<String> newUnlocks = new ArrayList<>();
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Extract level number from path
        int levelNumber = extractLevelNumber(levelPath);
        if (levelNumber <= 0) {
            return newUnlocks;
        }

        // Track completed levels
        String completed = prefs.getString(LEVELS_COMPLETED_KEY, "");
        String levelKey = "level_" + levelNumber;
        if (!completed.contains(levelKey + ";")) {
            completed += levelKey + ";";
            prefs.putString(LEVELS_COMPLETED_KEY, completed);
            prefs.flush();
            GameLogger.info("AchievementManager", "Level " + levelNumber + " completed!");
        }

        // Check theme completion achievements
        newUnlocks.addAll(checkThemeCompletion(completed));

        // Check flawless victory
        if (!tookDamage) {
            if (unlockCard("Flawless Victory")) {
                newUnlocks.add("Flawless Victory");
            }
            // Track flawless levels
            String flawless = prefs.getString(FLAWLESS_LEVELS_KEY, "");
            if (!flawless.contains(levelKey + ";")) {
                flawless += levelKey + ";";
                prefs.putString(FLAWLESS_LEVELS_KEY, flawless);
                prefs.flush();
            }
        }

        // Check speedrun achievement
        if (completionTime < 60.0f && unlockCard("Speedrunner")) {
            newUnlocks.add("Speedrunner");
        }

        return newUnlocks;
    }

    private static List<String> checkThemeCompletion(String completedLevels) {
        List<String> newUnlocks = new ArrayList<>();

        // Grassland (1-4)
        if (hasLevels(completedLevels, 1, 4) && unlockCard("Grassland Explorer")) {
            newUnlocks.add("Grassland Explorer");
        }
        // Desert (5-8)
        if (hasLevels(completedLevels, 5, 8) && unlockCard("Desert Survivor")) {
            newUnlocks.add("Desert Survivor");
        }
        // Ice (9-12)
        if (hasLevels(completedLevels, 9, 12) && unlockCard("Ice Conqueror")) {
            newUnlocks.add("Ice Conqueror");
        }
        // Jungle (13-16)
        if (hasLevels(completedLevels, 13, 16) && unlockCard("Jungle Master")) {
            newUnlocks.add("Jungle Master");
        }
        // Spaceship (17-20)
        if (hasLevels(completedLevels, 17, 20) && unlockCard("Space Commander")) {
            newUnlocks.add("Space Commander");
        }
        // World Champion
        if (hasLevels(completedLevels, 1, 20) && unlockCard("World Champion")) {
            newUnlocks.add("World Champion");
        }

        return newUnlocks;
    }

    private static boolean hasLevels(String completed, int start, int end) {
        for (int i = start; i <= end; i++) {
            if (!completed.contains("level_" + i + ";")) {
                return false;
            }
        }
        return true;
    }

    // === NEW: Challenge Achievement Tracking ===

    /**
     * Record a kill while player is at low HP
     * 
     * @param playerHP Player's current HP when the kill happened
     * @return List of newly unlocked achievements
     */
    public static List<String> checkNearDeathKill(int playerHP) {
        List<String> newUnlocks = new ArrayList<>();
        if (playerHP == 1 && unlockCard("Near Death")) {
            newUnlocks.add("Near Death");
            GameLogger.info("AchievementManager", "Achievement unlocked: Near Death");
        }
        return newUnlocks;
    }

    /**
     * Check for level completion with 1 HP
     * 
     * @param playerHP Player's HP at level completion
     * @return List of newly unlocked achievements
     */
    public static List<String> checkComeback(int playerHP) {
        List<String> newUnlocks = new ArrayList<>();
        if (playerHP == 1 && unlockCard("Comeback")) {
            newUnlocks.add("Comeback");
            GameLogger.info("AchievementManager", "Achievement unlocked: Comeback");
        }
        return newUnlocks;
    }

    /**
     * Record multi-kill and check combo achievements
     * 
     * @param killsInWindow Number of kills in the time window
     * @return List of newly unlocked achievements
     */
    public static List<String> checkMultiKill(int killsInWindow) {
        List<String> newUnlocks = new ArrayList<>();
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Update max combo
        int maxCombo = prefs.getInteger(MAX_COMBO_KEY, 0);
        if (killsInWindow > maxCombo) {
            prefs.putInteger(MAX_COMBO_KEY, killsInWindow);
            prefs.flush();
        }

        if (killsInWindow >= 3 && unlockCard("Rampage")) {
            newUnlocks.add("Rampage");
            GameLogger.info("AchievementManager", "Achievement unlocked: Rampage");
        }
        if (killsInWindow >= 5 && unlockCard("Overkill")) {
            newUnlocks.add("Overkill");
            GameLogger.info("AchievementManager", "Achievement unlocked: Overkill");
        }

        return newUnlocks;
    }

    // === Helper Methods ===

    private static String normalizeWeaponKey(String weaponName) {
        if (weaponName == null)
            return "unknown";
        String lower = weaponName.toLowerCase();
        if (lower.contains("sword"))
            return "sword";
        if (lower.contains("bow"))
            return "bow";
        if (lower.contains("staff"))
            return "staff";
        if (lower.contains("crossbow"))
            return "crossbow";
        if (lower.contains("wand"))
            return "wand";
        return lower.replace(" ", "_");
    }

    private static int extractLevelNumber(String levelPath) {
        if (levelPath == null)
            return 0;
        try {
            String numbers = levelPath.replaceAll("[^0-9]", "");
            if (!numbers.isEmpty()) {
                return Integer.parseInt(numbers);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    /**
     * Check if player has all weapon achievements.
     */
    private static boolean hasAllWeaponAchievements() {
        List<String> unlocked = getUnlockedCards();
        return unlocked.contains("Sword Collector") &&
                unlocked.contains("Bow Hunter") &&
                unlocked.contains("Staff Wielder") &&
                unlocked.contains("Crossbow Expert") &&
                unlocked.contains("Wand Master");
    }

    /**
     * Check if player has all weapon mastery achievements.
     */
    private static boolean hasAllWeaponMasteryAchievements() {
        List<String> unlocked = getUnlockedCards();
        return unlocked.contains("Blade Dancer") &&
                unlocked.contains("Sharpshooter") &&
                unlocked.contains("Arcane Apprentice");
    }

    /**
     * Unlocks a card if it hasn't been unlocked yet.
     * 
     * @param cardName The name of the card.
     * @return true if the card was newly unlocked, false if already unlocked.
     */
    public static boolean unlockCard(String cardName) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String current = prefs.getString(UNLOCKED_CARDS_KEY, "");

        if (current.contains(cardName + ";")) {
            return false;
        }

        current += cardName + ";";
        prefs.putString(UNLOCKED_CARDS_KEY, current);
        prefs.flush();

        GameLogger.info("AchievementManager", "Achievement unlocked: " + cardName);
        return true;
    }

    public static List<String> getUnlockedCards() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String current = prefs.getString(UNLOCKED_CARDS_KEY, "");
        List<String> list = new ArrayList<>();
        if (!current.isEmpty()) {
            for (String s : current.split(";")) {
                if (!s.trim().isEmpty())
                    list.add(s);
            }
        }
        return list;
    }

    /**
     * Get count of unlocked achievements.
     */
    public static int getUnlockedCount() {
        return getUnlockedCards().size();
    }

    /**
     * Get total count of all achievements.
     */
    public static int getTotalCount() {
        return allAchievements.size();
    }

    /**
     * Get total coins ever earned.
     */
    public static int getTotalCoinsEarned() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getInteger(TOTAL_COINS_KEY, 0);
    }

    /**
     * Get weapon kills for a specific weapon
     */
    public static int getWeaponKills(String weaponName) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String weaponKey = normalizeWeaponKey(weaponName);
        return prefs.getInteger(WEAPON_KILLS_PREFIX + weaponKey, 0);
    }

    /**
     * Get total armor damage absorbed
     */
    public static int getTotalArmorAbsorbed() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getInteger(ARMOR_ABSORBED_PREFIX + "total", 0);
    }

    /**
     * Get all achievement definitions
     */
    public static Map<String, Achievement> getAllAchievements() {
        return new HashMap<>(allAchievements);
    }

    /**
     * Get achievements by category
     */
    public static List<Achievement> getAchievementsByCategory(AchievementCategory category) {
        List<Achievement> result = new ArrayList<>();
        for (Achievement a : allAchievements.values()) {
            if (a.getCategory() == category) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Reset all achievements (for debugging).
     */
    public static void resetAll() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.clear();
        prefs.flush();
        GameLogger.info("AchievementManager", "All achievements and statistics reset.");
    }

    /**
     * Get achievement unlock info by name.
     * Used by UI to display popup with full achievement details.
     * 
     * @param achievementName The display name of the achievement
     * @return AchievementUnlockInfo or null if not found
     */
    public static AchievementUnlockInfo getAchievementInfo(String achievementName) {
        for (Achievement a : allAchievements.values()) {
            if (a.getName().equals(achievementName)) {
                return new AchievementUnlockInfo(a);
            }
        }
        // Legacy fallback for old achievement names
        return new AchievementUnlockInfo(
                achievementName,
                "Achievement unlocked!",
                AchievementRarity.COMMON,
                AchievementCategory.COMBAT,
                10);
    }

    /**
     * Get achievement unlock info for multiple achievements.
     * 
     * @param achievementNames List of achievement names
     * @return List of AchievementUnlockInfo
     */
    public static List<AchievementUnlockInfo> getAchievementInfoList(List<String> achievementNames) {
        List<AchievementUnlockInfo> result = new ArrayList<>();
        for (String name : achievementNames) {
            result.add(getAchievementInfo(name));
        }
        return result;
    }

    // === Import/Export Logic for Save System ===

    /**
     * Export all achievement data (progress, unlocks, stats) to a Map.
     */
    public static Map<String, Object> exportData() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        Map<String, Object> data = new HashMap<>();
        data.putAll(prefs.get());
        return data;
    }

    /**
     * Import achievement data from a Map.
     * Replaces current state.
     */
    public static void importData(Map<String, Object> data) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.clear();
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object v = entry.getValue();
                if (v instanceof String)
                    prefs.putString(entry.getKey(), (String) v);
                else if (v instanceof Integer)
                    prefs.putInteger(entry.getKey(), (Integer) v);
                else if (v instanceof Float)
                    prefs.putFloat(entry.getKey(), (Float) v);
                else if (v instanceof Boolean)
                    prefs.putBoolean(entry.getKey(), (Boolean) v);
                else if (v instanceof Long)
                    prefs.putLong(entry.getKey(), (Long) v);
                // Fallback catch-all
                else
                    prefs.putString(entry.getKey(), v.toString());
            }
        }
        prefs.flush();
        GameLogger.info("AchievementManager", "Imported achievement data.");
    }
}
