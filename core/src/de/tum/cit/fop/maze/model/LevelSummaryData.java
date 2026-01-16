package de.tum.cit.fop.maze.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Level Summary Data - 关卡结算数据封装
 * 
 * 统一胜利/失败关卡模式的结算数据。
 */
public class LevelSummaryData {

    public enum Result {
        VICTORY, DEFEAT
    }

    // === Core ===
    private Result result;
    private String mapPath;

    // === Statistics ===
    private int killCount;
    private int coinsCollected;
    private float completionTime; // seconds
    private boolean tookDamage;
    private int playerHP;
    private int maxHP;

    // === Achievements ===
    private List<String> newAchievements;

    // === Builder Pattern ===
    public LevelSummaryData(Result result, String mapPath) {
        this.result = result;
        this.mapPath = mapPath;
        this.newAchievements = new ArrayList<>();
    }

    // === Getters ===
    public Result getResult() {
        return result;
    }

    public String getMapPath() {
        return mapPath;
    }

    public boolean isVictory() {
        return result == Result.VICTORY;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getCoinsCollected() {
        return coinsCollected;
    }

    public float getCompletionTime() {
        return completionTime;
    }

    public boolean tookDamage() {
        return tookDamage;
    }

    public int getPlayerHP() {
        return playerHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public List<String> getNewAchievements() {
        return newAchievements;
    }

    // === Setters (Builder Style) ===
    public LevelSummaryData setKillCount(int killCount) {
        this.killCount = killCount;
        return this;
    }

    public LevelSummaryData setCoinsCollected(int coins) {
        this.coinsCollected = coins;
        return this;
    }

    public LevelSummaryData setCompletionTime(float time) {
        this.completionTime = time;
        return this;
    }

    public LevelSummaryData setTookDamage(boolean tookDamage) {
        this.tookDamage = tookDamage;
        return this;
    }

    public LevelSummaryData setPlayerHP(int hp, int maxHP) {
        this.playerHP = hp;
        this.maxHP = maxHP;
        return this;
    }

    public LevelSummaryData setNewAchievements(List<String> achievements) {
        this.newAchievements = achievements != null ? achievements : new ArrayList<>();
        return this;
    }

    // === Utility ===
    public String getFormattedTime() {
        int minutes = (int) (completionTime / 60);
        int seconds = (int) (completionTime % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public int getLevelNumber() {
        if (mapPath == null)
            return 0;
        try {
            String clean = mapPath.replace("maps/level-", "").replace(".properties", "");
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getThemeName() {
        int level = getLevelNumber();
        if (level >= 1 && level <= 4)
            return "Grassland";
        if (level >= 5 && level <= 8)
            return "Desert";
        if (level >= 9 && level <= 12)
            return "Ice";
        if (level >= 13 && level <= 16)
            return "Jungle";
        if (level >= 17 && level <= 20)
            return "Space";
        return "Unknown";
    }

    /**
     * Calculate rank based on performance (S/A/B/C/D)
     */
    public String getRank() {
        if (!isVictory())
            return "F";

        int score = 0;

        // Kill bonus (up to 30 points)
        score += Math.min(30, killCount * 2);

        // Time bonus (up to 30 points, under 60s = max)
        if (completionTime < 60)
            score += 30;
        else if (completionTime < 120)
            score += 20;
        else if (completionTime < 180)
            score += 10;

        // Flawless bonus (20 points)
        if (!tookDamage)
            score += 20;

        // HP bonus (up to 20 points)
        if (maxHP > 0) {
            score += (int) (20.0 * playerHP / maxHP);
        }

        // Rank thresholds
        if (score >= 90)
            return "S";
        if (score >= 70)
            return "A";
        if (score >= 50)
            return "B";
        if (score >= 30)
            return "C";
        return "D";
    }
}
