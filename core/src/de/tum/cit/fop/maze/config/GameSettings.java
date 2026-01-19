package de.tum.cit.fop.maze.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * 集中管理所有可调整的游戏参数。
 * 
 * 功能说明：
 * - 硬编码默认值 (DEFAULT_*): 系统原始默认值，不可修改
 * - 用户默认值: 玩家在菜单中自定义并保存的默认值
 * - 当前值: 本局游戏中使用的值 (可在游戏中调整)
 * 
 * 使用方法：
 * - 游戏启动时调用 loadUserDefaults() 加载用户自定义默认值
 * - 菜单中调整参数后调用 saveAsUserDefaults() 保存
 * - 新关卡开始时调用 resetToUserDefaults() 重置为用户默认值
 * - 游戏中可以临时调整当前值，但不影响保存的默认值
 */
public class GameSettings {

    private static final String PREFS_NAME = "maze_runner_settings_v1";

    // ==================== 硬编码默认值 (不可修改) ====================

    private static final float DEFAULT_PLAYER_WALK_SPEED = 3.5f;
    private static final float DEFAULT_PLAYER_RUN_SPEED = 7.0f;
    private static final int DEFAULT_PLAYER_MAX_LIVES = 3;
    private static final float DEFAULT_PLAYER_INVINCIBILITY_DURATION = 1.0f;

    private static final float DEFAULT_ENEMY_PATROL_SPEED = 1.5f;
    private static final float DEFAULT_ENEMY_CHASE_SPEED = 2.5f;
    private static final float DEFAULT_ENEMY_DETECT_RANGE = 5.0f;

    private static final float DEFAULT_HIT_DISTANCE = 0.6f;
    private static final float DEFAULT_CAMERA_ZOOM = 0.5f; // Smaller zoom = closer/zoomed-in view
                                                           // player)
                                                           // height)

    // ==================== 用户自定义默认值 (从文件加载/保存) ====================

    private static float userPlayerWalkSpeed = DEFAULT_PLAYER_WALK_SPEED;
    private static float userPlayerRunSpeed = DEFAULT_PLAYER_RUN_SPEED;
    private static int userPlayerMaxLives = DEFAULT_PLAYER_MAX_LIVES;
    private static float userPlayerInvincibilityDuration = DEFAULT_PLAYER_INVINCIBILITY_DURATION;
    private static float userEnemyPatrolSpeed = DEFAULT_ENEMY_PATROL_SPEED;
    private static float userEnemyChaseSpeed = DEFAULT_ENEMY_CHASE_SPEED;
    private static float userEnemyDetectRange = DEFAULT_ENEMY_DETECT_RANGE;
    private static float userHitDistance = DEFAULT_HIT_DISTANCE;
    private static float userCameraZoom = DEFAULT_CAMERA_ZOOM;

    // ==================== 当前值 (本局游戏使用) ====================

    public static float playerWalkSpeed = DEFAULT_PLAYER_WALK_SPEED;
    public static float playerRunSpeed = DEFAULT_PLAYER_RUN_SPEED;
    public static int playerMaxLives = DEFAULT_PLAYER_MAX_LIVES;
    public static float playerInvincibilityDuration = DEFAULT_PLAYER_INVINCIBILITY_DURATION;
    public static float enemyPatrolSpeed = DEFAULT_ENEMY_PATROL_SPEED;
    public static float enemyChaseSpeed = DEFAULT_ENEMY_CHASE_SPEED;
    public static float enemyDetectRange = DEFAULT_ENEMY_DETECT_RANGE;
    public static float hitDistance = DEFAULT_HIT_DISTANCE;
    public static float cameraZoom = DEFAULT_CAMERA_ZOOM;

    // Keys (Default WASD/ARROWS logic handled in game, but here is preferred
    // primary)
    // Actually typically we store int keycodes.
    public static int KEY_UP;
    public static int KEY_DOWN;
    public static int KEY_LEFT;
    public static int KEY_RIGHT;

    public static int KEY_ATTACK;
    public static int KEY_SWITCH_WEAPON;

    // ==================== 保存/加载用户默认值 ====================

    /**
     * 从文件加载用户自定义的默认值。
     * 应在游戏启动时调用一次。
     */
    public static void loadUserDefaults() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        userPlayerWalkSpeed = prefs.getFloat("playerWalkSpeed", DEFAULT_PLAYER_WALK_SPEED);
        userPlayerRunSpeed = prefs.getFloat("playerRunSpeed", DEFAULT_PLAYER_RUN_SPEED);
        userPlayerMaxLives = prefs.getInteger("playerMaxLives", DEFAULT_PLAYER_MAX_LIVES);
        userPlayerInvincibilityDuration = prefs.getFloat("playerInvincibilityDuration",
                DEFAULT_PLAYER_INVINCIBILITY_DURATION);
        userEnemyPatrolSpeed = prefs.getFloat("enemyPatrolSpeed", DEFAULT_ENEMY_PATROL_SPEED);
        userEnemyChaseSpeed = prefs.getFloat("enemyChaseSpeed", DEFAULT_ENEMY_CHASE_SPEED);
        userEnemyDetectRange = prefs.getFloat("enemyDetectRange", DEFAULT_ENEMY_DETECT_RANGE);
        userHitDistance = prefs.getFloat("hitDistance", DEFAULT_HIT_DISTANCE);
        userHitDistance = prefs.getFloat("hitDistance", DEFAULT_HIT_DISTANCE);
        userHitDistance = prefs.getFloat("hitDistance", DEFAULT_HIT_DISTANCE);
        userCameraZoom = prefs.getFloat("cameraZoom", DEFAULT_CAMERA_ZOOM);
        userFogEnabled = prefs.getBoolean("fogEnabled", false);

        KEY_UP = prefs.getInteger("key_up", com.badlogic.gdx.Input.Keys.UP);
        KEY_DOWN = prefs.getInteger("key_down", com.badlogic.gdx.Input.Keys.DOWN);
        KEY_LEFT = prefs.getInteger("key_left", com.badlogic.gdx.Input.Keys.LEFT);
        KEY_RIGHT = prefs.getInteger("key_right", com.badlogic.gdx.Input.Keys.RIGHT);

        KEY_ATTACK = prefs.getInteger("key_attack", com.badlogic.gdx.Input.Keys.SPACE);
        KEY_SWITCH_WEAPON = prefs.getInteger("key_switch_weapon", com.badlogic.gdx.Input.Keys.TAB);

        // 同时设置当前值
        resetToUserDefaults();
    }

    /**
     * 将当前值保存为用户自定义默认值。
     * 应在菜单设置中点击 "Save" 时调用。
     */
    public static void saveAsUserDefaults() {
        // 更新用户默认值
        userPlayerWalkSpeed = playerWalkSpeed;
        userPlayerRunSpeed = playerRunSpeed;
        userPlayerMaxLives = playerMaxLives;
        userPlayerInvincibilityDuration = playerInvincibilityDuration;
        userEnemyPatrolSpeed = enemyPatrolSpeed;
        userEnemyChaseSpeed = enemyChaseSpeed;
        userEnemyDetectRange = enemyDetectRange;
        userHitDistance = hitDistance;
        userCameraZoom = cameraZoom;
        userFogEnabled = fogEnabled;

        // 保存到文件
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat("playerWalkSpeed", userPlayerWalkSpeed);
        prefs.putFloat("playerRunSpeed", userPlayerRunSpeed);
        prefs.putInteger("playerMaxLives", userPlayerMaxLives);
        prefs.putFloat("playerInvincibilityDuration", userPlayerInvincibilityDuration);
        prefs.putFloat("enemyPatrolSpeed", userEnemyPatrolSpeed);
        prefs.putFloat("enemyChaseSpeed", userEnemyChaseSpeed);
        prefs.putFloat("enemyDetectRange", userEnemyDetectRange);
        prefs.putFloat("hitDistance", userHitDistance);
        prefs.putFloat("cameraZoom", userCameraZoom);
        prefs.putBoolean("fogEnabled", userFogEnabled);
        prefs.flush();
    }

    /**
     * 仅保存按键绑定设置（这些始终是持久化的）。
     * 用于游戏内设置界面，不保存速度等会话设置。
     */
    public static void saveKeyBindingsOnly() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putInteger("key_up", KEY_UP);
        prefs.putInteger("key_down", KEY_DOWN);
        prefs.putInteger("key_left", KEY_LEFT);
        prefs.putInteger("key_right", KEY_RIGHT);

        prefs.putInteger("key_attack", KEY_ATTACK);
        prefs.putInteger("key_switch_weapon", KEY_SWITCH_WEAPON);
        prefs.flush();
    }

    /**
     * 重置当前值为用户自定义默认值。
     * 应在每次新关卡开始时调用。
     */
    public static void resetToUserDefaults() {
        playerWalkSpeed = userPlayerWalkSpeed;
        playerRunSpeed = userPlayerRunSpeed;
        playerMaxLives = userPlayerMaxLives;
        playerInvincibilityDuration = userPlayerInvincibilityDuration;
        enemyPatrolSpeed = userEnemyPatrolSpeed;
        enemyChaseSpeed = userEnemyChaseSpeed;
        enemyDetectRange = userEnemyDetectRange;
        hitDistance = userHitDistance;
        fogEnabled = userFogEnabled;
        cameraZoom = DEFAULT_CAMERA_ZOOM; // Force new default zoom (0.67 for 15x15 view)
    }

    // ==================== 迷雾模式 (Fog of War) ====================
    public static boolean fogEnabled = false;
    private static boolean userFogEnabled = false;

    public static void setFogEnabled(boolean enabled) {
        fogEnabled = enabled;
        userFogEnabled = enabled;
    }

    public static boolean isFogEnabled() {
        return fogEnabled;
    }

    /**
     * 重置用户默认值为硬编码默认值，并保存。
     */
    public static void resetUserDefaultsToHardcoded() {
        userPlayerWalkSpeed = DEFAULT_PLAYER_WALK_SPEED;
        userPlayerRunSpeed = DEFAULT_PLAYER_RUN_SPEED;
        userPlayerMaxLives = DEFAULT_PLAYER_MAX_LIVES;
        userPlayerInvincibilityDuration = DEFAULT_PLAYER_INVINCIBILITY_DURATION;
        userEnemyPatrolSpeed = DEFAULT_ENEMY_PATROL_SPEED;
        userEnemyChaseSpeed = DEFAULT_ENEMY_CHASE_SPEED;
        userEnemyDetectRange = DEFAULT_ENEMY_DETECT_RANGE;
        userHitDistance = DEFAULT_HIT_DISTANCE;
        userFogEnabled = false;

        // 同时更新当前值
        resetToUserDefaults();

        // 保存到文件
        saveAsUserDefaults();
    }

    // ==================== 关卡解锁系统 ====================

    public static final String DEV_PASSWORD = "111"; // 开发者模式密码
    private static final String PREF_UNLOCKED_LEVEL = "max_unlocked_level";

    public static int getUnlockedLevel() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getInteger(PREF_UNLOCKED_LEVEL, 1); // 默认解锁第 1 关
    }

    public static void unlockLevel(int level) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        int current = getUnlockedLevel();
        if (level > current) {
            prefs.putInteger(PREF_UNLOCKED_LEVEL, level);
            prefs.flush();
            System.out.println("New Level Unlocked: " + level);
        }
    }

    // ==================== 旧方法 (保持兼容) ====================

    /**
     * @deprecated 使用 resetToUserDefaults() 代替
     */
    public static void resetToDefaults() {
        resetToUserDefaults();
    }

    // ==================== Getters for Default Values ====================

    public static float getDefaultPlayerWalkSpeed() {
        return DEFAULT_PLAYER_WALK_SPEED;
    }

    public static float getDefaultPlayerRunSpeed() {
        return DEFAULT_PLAYER_RUN_SPEED;
    }

    public static int getDefaultPlayerMaxLives() {
        return DEFAULT_PLAYER_MAX_LIVES;
    }

    public static float getDefaultEnemyPatrolSpeed() {
        return DEFAULT_ENEMY_PATROL_SPEED;
    }

    public static float getDefaultEnemyChaseSpeed() {
        return DEFAULT_ENEMY_CHASE_SPEED;
    }

    public static float getDefaultEnemyDetectRange() {
        return DEFAULT_ENEMY_DETECT_RANGE;
    }
}
