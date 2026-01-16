package de.tum.cit.fop.maze.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 无尽模式存档状态类 (Endless Game State)
 * 
 * 与关卡模式的 GameState 分开存储，用于无尽模式的存档和读档。
 * 支持JSON序列化（LibGDX Json）。
 */
public class EndlessGameState {

    // ========== 玩家状态 ==========

    /** 玩家X坐标 */
    public float playerX;

    /** 玩家Y坐标 */
    public float playerY;

    /** 玩家生命值 */
    public int playerLives;

    /** 玩家最大生命值 */
    public int playerMaxLives;

    /** 玩家当前护甲类型 (null = 无护甲) */
    public String armorType;

    /** 护甲剩余耐久 */
    public int armorDurability;

    // ========== 游戏进度 ==========

    /** 生存时间（秒） */
    public float survivalTime;

    /** 总击杀数 */
    public int totalKills;

    /** 当前COMBO */
    public int currentCombo;

    /** 历史最高COMBO */
    public int maxCombo;

    /** 当前RAGE值 (0-100) */
    public float rageLevel;

    /** 当前波次 */
    public int currentWave;

    /** 当前得分 */
    public int score;

    // ========== 武器状态 ==========

    /** 当前装备的武器类型 */
    public String equippedWeapon;

    /** 已解锁的武器列表 */
    public List<String> unlockedWeapons;

    /** 武器经验/临时加成 */
    public int weaponBonus;

    // ========== 位置信息 ==========

    /** 当前所在区域主题 */
    public String currentZone;

    /** 当前区块X */
    public int currentChunkX;

    /** 当前区块Y */
    public int currentChunkY;

    // ========== 收集物 ==========

    /** 已收集的金币 */
    public int collectedCoins;

    /** 库存中的药水数量 */
    public int potionCount;

    // ========== 元数据 ==========

    /** 存档时间戳 */
    public long saveTimestamp;

    /** 存档版本（用于兼容性检查） */
    public int saveVersion = 1;

    /**
     * 默认构造函数 (JSON反序列化需要)
     */
    public EndlessGameState() {
        this.unlockedWeapons = new ArrayList<>();
    }

    /**
     * 从当前游戏状态创建存档
     */
    public static EndlessGameState createFromGame(
            Player player,
            float survivalTime,
            int totalKills,
            int currentCombo,
            int maxCombo,
            float rageLevel,
            int currentWave,
            int score,
            String currentZone) {

        EndlessGameState state = new EndlessGameState();

        // 玩家状态
        state.playerX = player.getX();
        state.playerY = player.getY();
        state.playerLives = player.getLives();
        state.playerMaxLives = player.getMaxHealth();

        // 护甲状态
        if (player.getEquippedArmor() != null) {
            state.armorType = player.getEquippedArmor().getClass().getSimpleName();
            state.armorDurability = player.getEquippedArmor().getCurrentShield();
        }

        // 武器状态
        if (player.getCurrentWeapon() != null) {
            state.equippedWeapon = player.getCurrentWeapon().getName();
        }
        state.unlockedWeapons = new ArrayList<>();
        for (var weapon : player.getInventory()) {
            state.unlockedWeapons.add(weapon.getName());
        }

        // 游戏进度
        state.survivalTime = survivalTime;
        state.totalKills = totalKills;
        state.currentCombo = currentCombo;
        state.maxCombo = maxCombo;
        state.rageLevel = rageLevel;
        state.currentWave = currentWave;
        state.score = score;
        state.currentZone = currentZone;

        // 收集物
        state.collectedCoins = player.getCoins();

        // 元数据
        state.saveTimestamp = System.currentTimeMillis();

        return state;
    }

    /**
     * 获取格式化的生存时间 (MM:SS)
     */
    public String getFormattedSurvivalTime() {
        int minutes = (int) (survivalTime / 60);
        int seconds = (int) (survivalTime % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 获取格式化的存档时间
     */
    public String getFormattedSaveTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new java.util.Date(saveTimestamp));
    }

    /**
     * 获取存档摘要（用于UI显示）
     */
    public String getSummary() {
        return String.format("Time: %s | Kills: %d | Score: %,d",
                getFormattedSurvivalTime(), totalKills, score);
    }

    @Override
    public String toString() {
        return "EndlessGameState{" +
                "survivalTime=" + getFormattedSurvivalTime() +
                ", kills=" + totalKills +
                ", score=" + score +
                ", zone=" + currentZone +
                '}';
    }
}
