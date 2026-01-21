package de.tum.cit.fop.maze.config;

/**
 * 存放游戏设计的静态配置常量，特别是与文件格式、游戏规则相关的不可变常量。
 * 区别于 GameSettings (后者可能包含用户可调整的偏好设置)。
 */
public class GameConfig {

    // ==================== Map Object IDs ====================
    // 对应 .properties 地图文件中的 value

    public static final int OBJECT_ID_WALL = 0;
    public static final int OBJECT_ID_ENTRY = 1; // Start Point
    public static final int OBJECT_ID_EXIT = 2;
    public static final int OBJECT_ID_TRAP = 3;
    public static final int OBJECT_ID_ENEMY = 4;
    public static final int OBJECT_ID_KEY = 5;
    public static final int OBJECT_ID_MOBILE_TRAP = 6;

    // Multi-tile Wall IDs (7 sizes: squares + horizontal + vertical)
    // Format: WxH (Width x Height in grid cells)
    public static final int OBJECT_ID_WALL_2X2 = 10; // 2x2 square
    public static final int OBJECT_ID_WALL_3X2 = 11; // 3x2 horizontal
    public static final int OBJECT_ID_WALL_2X3 = 12; // 2x3 vertical
    public static final int OBJECT_ID_WALL_2X4 = 13; // 2x4 vertical
    public static final int OBJECT_ID_WALL_4X2 = 14; // 4x2 horizontal
    public static final int OBJECT_ID_WALL_3X3 = 15; // 3x3 square
    public static final int OBJECT_ID_WALL_4X4 = 16; // 4x4 square

    public static final int OBJECT_ID_COIN = 7;
    public static final int OBJECT_ID_WEAPON_DROP = 8;
    public static final int OBJECT_ID_ARMOR_DROP = 9;
    public static final int OBJECT_ID_POTION = 17;

    // ==================== Texture Paths ====================

    public static final String TEXTURE_ATLAS_PATH = "character.atlas";

    // ==================== Default Stats ====================

    public static final int PLAYER_DEFAULT_MAX_LIVES = 3;
    public static final int ENEMY_DEFAULT_HEALTH = 3;
    public static final int KEY_DEFAULT_SKILL_POINTS = 10;

    // ==================== Weapon Type IDs ====================

    public static final String WEAPON_SWORD = "Sword";
    public static final String WEAPON_BOW = "Ice Bow";
    public static final String WEAPON_STAFF = "Fire Staff";
    public static final String WEAPON_CROSSBOW = "Crossbow";
    public static final String WEAPON_WAND = "Magic Wand";

    // ==================== Armor Type IDs ====================

    public static final String ARMOR_PHYSICAL = "PHYSICAL_ARMOR";
    public static final String ARMOR_MAGICAL = "MAGICAL_ARMOR";

    // ==================== Projectile Settings ====================

    public static final float PROJECTILE_SPEED = 8.0f;
    public static final float PROJECTILE_LIFETIME = 3.0f;

    // ==================== Treasure Chest Settings ====================

    /** 宝箱 Object ID */
    public static final int OBJECT_ID_CHEST = 20;

    /** 谜题宝箱概率 (30%) */
    public static final float CHEST_PUZZLE_PROBABILITY = 0.3f;

    /** 宝箱密度：约陷阱数量的 1/20 */
    public static final float CHEST_DENSITY_RATIO = 0.05f;

    /** 每张地图最少宝箱数 */
    public static final int CHEST_MIN_COUNT = 3;

    // ==================== 实时渲染优化配置 ====================

    /** 实体渲染半径（格子单位）- 只渲染玩家附近的实体 */
    public static final float ENTITY_RENDER_RADIUS = 8f;

    /** 敌人更新距离阈值平方 - 超过此距离跳过更新 (40^2 = 1600) */
    public static final float ENEMY_UPDATE_DISTANCE_SQUARED = 1600f;
}
