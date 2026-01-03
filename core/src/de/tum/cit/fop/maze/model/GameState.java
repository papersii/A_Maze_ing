package de.tum.cit.fop.maze.model;

/**
 * 这个类用于存储需要持久化的游戏状态。
 * 包括玩家坐标、当前关卡、生命值等。
 */
public class GameState {
    private float playerX;
    private float playerY;
    private String currentLevel;
    private int lives;
    private boolean hasKey;

    // 必须有一个无参构造函数供 JSON 反序列化使用
    public GameState() {
    }

    public GameState(float playerX, float playerY, String currentLevel, int lives, boolean hasKey) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.currentLevel = currentLevel;
        this.lives = lives;
        this.hasKey = hasKey;
    }

    // --- Getters & Setters ---

    public float getPlayerX() {
        return playerX;
    }

    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public boolean isHasKey() {
        return hasKey;
    }

    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
    }

    // Skill System Fields
    private int skillPoints;
    private int maxHealthBonus;
    private int damageBonus;
    private float invincibilityExtension;

    // Phase 2 Skills
    private float knockbackMultiplier = 1.0f;
    private float cooldownReduction = 0.0f;
    private float speedBonus = 0.0f;

    public float getKnockbackMultiplier() {
        return knockbackMultiplier;
    }

    public void setKnockbackMultiplier(float knockbackMultiplier) {
        this.knockbackMultiplier = knockbackMultiplier;
    }

    public float getCooldownReduction() {
        return cooldownReduction;
    }

    public void setCooldownReduction(float cooldownReduction) {
        this.cooldownReduction = cooldownReduction;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }

    public void setSpeedBonus(float speedBonus) {
        this.speedBonus = speedBonus;
    }

    // We could store inventory here too if needed, but let's stick to requested
    // Skill/Stats first.
    // User asked "Weapons, Health, Skill Points kept". So yes, inventory.
    // Let's store inventory as a list of class names or simple IDs.
    // For simplicity, let's store Weapon simple class names? Or Types.
    // We have Sword, Bow, MagicStaff, etc.
    private java.util.List<String> inventoryWeaponTypes;

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int getMaxHealthBonus() {
        return maxHealthBonus;
    }

    public void setMaxHealthBonus(int maxHealthBonus) {
        this.maxHealthBonus = maxHealthBonus;
    }

    public int getDamageBonus() {
        return damageBonus;
    }

    public void setDamageBonus(int damageBonus) {
        this.damageBonus = damageBonus;
    }

    public float getInvincibilityExtension() {
        return invincibilityExtension;
    }

    public void setInvincibilityExtension(float invincibilityExtension) {
        this.invincibilityExtension = invincibilityExtension;
    }

    public java.util.List<String> getInventoryWeaponTypes() {
        return inventoryWeaponTypes;
    }

    public void setInventoryWeaponTypes(java.util.List<String> inventoryWeaponTypes) {
        this.inventoryWeaponTypes = inventoryWeaponTypes;
    }
}