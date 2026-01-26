package de.tum.cit.fop.maze.config;

/**
 * 无尽模式配置类 (Endless Mode Configuration)
 * 
 * 存储无尽模式的所有常量和配置参数。
 * 遵循开闭原则：通过扩展新的配置而非修改现有配置来添加功能。
 */
public final class EndlessModeConfig {

    private EndlessModeConfig() {
        // 防止实例化
    }

    // ========== 地图配置 ==========

    /** 地图宽度（格子数） */
    public static final int MAP_WIDTH = 900;

    /** 地图高度（格子数） */
    public static final int MAP_HEIGHT = 900;

    /** 分块大小（每个Chunk的边长） */
    public static final int CHUNK_SIZE = 64;

    /** 活跃区块范围（玩家周围保持加载的区块数） */
    public static final int ACTIVE_CHUNK_RADIUS = 2; // 5x5 = 25 chunks

    /** 休眠区块范围（敌人停止更新的距离） */
    public static final int DORMANT_CHUNK_RADIUS = 3;

    // ========== 主题区域配置 ==========

    /** 中心Space区域半径 */
    public static final int SPACE_ZONE_RADIUS = 200;

    /** 主题名称常量 */
    public static final String THEME_GRASSLAND = "Grassland";
    public static final String THEME_JUNGLE = "Jungle";
    public static final String THEME_DESERT = "Desert";
    public static final String THEME_ICE = "Ice";
    public static final String THEME_SPACE = "Space";

    // ========== COMBO系统配置 ==========

    /** COMBO衰减时间（秒） */
    public static final float COMBO_DECAY_TIME = 5f;

    /** COMBO倍率阈值 */
    public static final int[] COMBO_THRESHOLDS = { 0, 5, 10, 20, 50 };

    /** 对应的得分倍率 */
    public static final float[] COMBO_MULTIPLIERS = { 1f, 1.5f, 2f, 3f, 5f };

    /** COMBO等级名称 */
    public static final String[] COMBO_NAMES = { "", "NICE!", "GREAT!", "UNSTOPPABLE!", "GODLIKE!" };

    // ========== RAGE系统配置 ==========

    /** RAGE等级阈值 (0-100) */
    public static final int[] RAGE_THRESHOLDS = { 0, 21, 41, 61, 81 };

    /** RAGE等级名称 */
    public static final String[] RAGE_NAMES = { "Calm", "Alert", "Aggressive", "Furious", "Berserk" };

    /** RAGE敌人速度加成 - 优化后降低最高速度 */
    public static final float[] RAGE_SPEED_MULTIPLIERS = { 1.0f, 1.1f, 1.2f, 1.3f, 1.5f };

    /** RAGE敌人伤害加成 - 优化后降低最高伤害 */
    public static final float[] RAGE_DAMAGE_MULTIPLIERS = { 1.0f, 1.0f, 1.0f, 1.2f, 1.5f };

    // ========== 波次系统配置 ==========

    /** 波次时间阈值（秒）- 优化后延长每波时长 */
    public static final int[] WAVE_TIME_THRESHOLDS = { 0, 90, 240, 420, 600, 900 };

    /** 敌人刷新间隔（秒）- 优化后缓和刷新速度 */
    public static final float[] WAVE_SPAWN_INTERVALS = { 4f, 3f, 2.5f, 2f, 1.5f, 1f };

    /** 敌人血量倍率 - 优化后降低后期倍率 */
    public static final float[] WAVE_HEALTH_MULTIPLIERS = { 1.0f, 1.1f, 1.25f, 1.5f, 1.75f, 2.0f };

    /** BOSS刷新间隔（秒） */
    public static final float BOSS_SPAWN_INTERVAL = 120f;

    /** 首次BOSS出现时间（秒） */
    public static final float FIRST_BOSS_TIME = 720f; // 12分钟

    // ========== 敌人配置 ==========

    /** 最大敌人数量 */
    public static final int MAX_ENEMY_COUNT = 200;

    /** 敌人刷新最小距离（格子） */
    public static final int SPAWN_MIN_DISTANCE = 20;

    /** 敌人刷新最大距离（格子） */
    public static final int SPAWN_MAX_DISTANCE = 50;

    /** 敌人休眠距离（格子） */
    public static final int ENEMY_DORMANT_DISTANCE = 50;

    // ========== 安全期配置（新增）==========

    /** 开局安全期时长（秒）- 无敌人刷新 */
    public static final float SAFE_PERIOD_DURATION = 15f;

    /** 初期敌人刷新距离（格子）- 比正常更远 */
    public static final int INITIAL_SPAWN_DISTANCE = 30;

    // ========== 掉落配置 ==========

    /** 生命药水掉落概率 - 优化后提升 */
    public static final float HEALTH_POTION_DROP_RATE = 0.15f;

    /** 精英敌人武器升级掉落概率 */
    public static final float WEAPON_UPGRADE_DROP_RATE = 0.30f;

    /** 护甲碎片刷新间隔（秒）- 优化后缩短 */
    public static final float ARMOR_SHARD_SPAWN_INTERVAL = 20f;

    /** COMBO延长剂掉落概率（在10+ COMBO时）- 优化后提升 */
    public static final float COMBO_EXTENDER_DROP_RATE = 0.08f;

    // ========== 得分配置 ==========

    /** 每次击杀基础分数 */
    public static final int SCORE_PER_KILL = 100;

    /** 时间加成最大倍率 */
    public static final float MAX_TIME_BONUS = 3.0f;

    /** 达到最大时间加成所需时间（秒） */
    public static final float TIME_FOR_MAX_BONUS = 300f; // 5分钟

    // ========== 存档配置 ==========

    /** 无尽模式存档目录 */
    public static final String ENDLESS_SAVE_DIR = "saves/endless/";

    /** 排行榜文件 */
    public static final String LEADERBOARD_FILE = "endless_leaderboard.json";

    /** 排行榜最大条目数 */
    public static final int MAX_LEADERBOARD_ENTRIES = 100;

    // ========== 辅助方法 ==========

    /**
     * 根据COMBO值获取倍率
     */
    public static float getComboMultiplier(int combo) {
        for (int i = COMBO_THRESHOLDS.length - 1; i >= 0; i--) {
            if (combo >= COMBO_THRESHOLDS[i]) {
                return COMBO_MULTIPLIERS[i];
            }
        }
        return 1f;
    }

    /**
     * 根据COMBO值获取等级名称
     */
    public static String getComboName(int combo) {
        for (int i = COMBO_THRESHOLDS.length - 1; i >= 0; i--) {
            if (combo >= COMBO_THRESHOLDS[i]) {
                return COMBO_NAMES[i];
            }
        }
        return "";
    }

    /**
     * 根据RAGE值获取等级索引 (0-4)
     */
    public static int getRageLevel(float rage) {
        for (int i = RAGE_THRESHOLDS.length - 1; i >= 0; i--) {
            if (rage >= RAGE_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 根据生存时间获取波次索引 (0-5)
     */
    public static int getWaveIndex(float survivalTime) {
        for (int i = WAVE_TIME_THRESHOLDS.length - 1; i >= 0; i--) {
            if (survivalTime >= WAVE_TIME_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 根据坐标获取主题
     * 
     * 布局:
     * - 中心圆形: Space (半径200)
     * - 西北: Grassland
     * - 东北: Jungle
     * - 西南: Desert
     * - 东南: Ice
     */
    public static String getThemeForPosition(int x, int y) {
        int centerX = MAP_WIDTH / 2;
        int centerY = MAP_HEIGHT / 2;

        // 检查是否在Space区域（中心圆形）
        float distFromCenter = (float) Math.sqrt(
                Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
        if (distFromCenter <= SPACE_ZONE_RADIUS) {
            return THEME_SPACE;
        }

        // 根据象限分配主题
        boolean isWest = x < centerX;
        boolean isNorth = y < centerY;

        if (isWest && isNorth) {
            return THEME_GRASSLAND;
        } else if (!isWest && isNorth) {
            return THEME_JUNGLE;
        } else if (isWest && !isNorth) {
            return THEME_DESERT;
        } else {
            return THEME_ICE;
        }
    }
}
