package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.config.EndlessModeConfig;

/**
 * 敌人RAGE仇恨系统 (Enemy Rage System)
 * 
 * 管理无尽模式中敌人的全局仇恨值。
 * 
 * 机制:
 * - Rage = (总击杀数 / 生存时间) × 100
 * - 杀得越快，Rage越高，敌人越强
 * - 慢杀慢打可以控制难度
 * 
 * 遵循单一职责原则：仅处理RAGE逻辑。
 */
public class RageSystem {

    /** 当前RAGE值 (0-100) */
    private float rageLevel;

    /** 当前RAGE等级索引 (0-4) */
    private int rageLevelIndex;

    /** 监听器：RAGE变化时回调 */
    private RageListener listener;

    /**
     * RAGE变化监听器接口
     */
    public interface RageListener {
        /** RAGE等级变化时调用 */
        void onRageLevelChanged(int newLevel, String levelName);
    }

    public RageSystem() {
        reset();
    }

    /**
     * 设置RAGE变化监听器
     */
    public void setListener(RageListener listener) {
        this.listener = listener;
    }

    /**
     * 更新RAGE值
     * 
     * @param totalKills   总击杀数
     * @param survivalTime 生存时间（秒）
     */
    public void update(int totalKills, float survivalTime) {
        // 避免除以零
        if (survivalTime < 1f) {
            survivalTime = 1f;
        }

        // 计算RAGE值: (杀敌数 / 时间) × 100
        // 例如: 30杀/60秒 = 50 RAGE
        float newRage = (totalKills / survivalTime) * 100f;

        // 限制在0-100范围
        newRage = Math.max(0, Math.min(100, newRage));

        this.rageLevel = newRage;

        // 检查等级变化
        int newLevelIndex = EndlessModeConfig.getRageLevel(rageLevel);
        if (newLevelIndex != rageLevelIndex) {
            rageLevelIndex = newLevelIndex;

            if (listener != null) {
                String levelName = EndlessModeConfig.RAGE_NAMES[rageLevelIndex];
                listener.onRageLevelChanged(rageLevelIndex, levelName);
            }
        }
    }

    /**
     * 获取敌人速度倍率
     */
    public float getEnemySpeedMultiplier() {
        return EndlessModeConfig.RAGE_SPEED_MULTIPLIERS[rageLevelIndex];
    }

    /**
     * 获取敌人伤害倍率
     */
    public float getEnemyDamageMultiplier() {
        return EndlessModeConfig.RAGE_DAMAGE_MULTIPLIERS[rageLevelIndex];
    }

    /**
     * 获取当前RAGE等级名称
     */
    public String getRageLevelName() {
        return EndlessModeConfig.RAGE_NAMES[rageLevelIndex];
    }

    /**
     * 获取RAGE百分比 (用于UI显示, 0-100)
     */
    public float getRagePercentage() {
        return rageLevel;
    }

    /**
     * 获取标准化的RAGE值 (0-1, 用于进度条)
     */
    public float getNormalizedRage() {
        return rageLevel / 100f;
    }

    /**
     * 判断是否处于Berserk状态 (最高等级)
     */
    public boolean isBerserk() {
        return rageLevelIndex == EndlessModeConfig.RAGE_THRESHOLDS.length - 1;
    }

    /**
     * 强制重置RAGE
     */
    public void reset() {
        rageLevel = 0;
        rageLevelIndex = 0;
    }

    // ========== Getters ==========

    public float getRageLevel() {
        return rageLevel;
    }

    public int getRageLevelIndex() {
        return rageLevelIndex;
    }

    // ========== Setters (用于存档恢复) ==========

    public void setRageLevel(float rageLevel) {
        this.rageLevel = Math.max(0, Math.min(100, rageLevel));
        this.rageLevelIndex = EndlessModeConfig.getRageLevel(this.rageLevel);
    }

    // ========== Console Command Support ==========

    /**
     * 获取RAGE进度 (0-1, 用于控制台)
     */
    public float getProgress() {
        return rageLevel / 100f;
    }

    /**
     * 设置RAGE进度 (0-1)
     */
    public void setProgress(float progress) {
        setRageLevel(progress * 100f);
    }

    /**
     * 最大化RAGE (直接设置到100)
     */
    public void maxOut() {
        setRageLevel(100f);
    }

    /**
     * 获取当前RAGE等级索引 (用于控制台显示)
     */
    public int getCurrentLevel() {
        return rageLevelIndex;
    }
}
