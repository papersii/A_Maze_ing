package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.config.EndlessModeConfig;

/**
 * COMBO击杀连击系统 (Combo Kill System)
 * 
 * 管理无尽模式中的连续击杀奖励机制。
 * 
 * 机制:
 * - 每次击杀敌人 COMBO +1
 * - 5秒内无击杀 COMBO 重置为 0
 * - 高COMBO带来得分倍率加成
 * 
 * 遵循单一职责原则：仅处理COMBO逻辑，不涉及其他系统。
 */
public class ComboSystem {

    /** 当前COMBO计数 */
    private int currentCombo;

    /** 历史最高COMBO */
    private int maxCombo;

    /** COMBO衰减计时器（秒） */
    private float decayTimer;

    /** COMBO是否激活（防止初始状态误判） */
    private boolean isActive;

    /** 监听器：COMBO变化时回调 */
    private ComboListener listener;

    /**
     * COMBO变化监听器接口
     */
    public interface ComboListener {
        /** COMBO增加时调用 */
        void onComboIncreased(int newCombo, float multiplier);

        /** COMBO重置时调用 */
        void onComboReset(int finalCombo);

        /** 达到里程碑时调用 (5, 10, 20, 50) */
        void onMilestoneReached(int combo, String milestoneName);
    }

    public ComboSystem() {
        reset();
    }

    /**
     * 设置COMBO变化监听器
     */
    public void setListener(ComboListener listener) {
        this.listener = listener;
    }

    /**
     * 每帧更新
     * 
     * @param delta 帧间隔时间（秒）
     */
    public void update(float delta) {
        if (!isActive || currentCombo == 0) {
            return;
        }

        decayTimer -= delta;

        if (decayTimer <= 0) {
            // COMBO超时，重置
            int finalCombo = currentCombo;
            currentCombo = 0;
            isActive = false;

            if (listener != null) {
                listener.onComboReset(finalCombo);
            }
        }
    }

    /**
     * 击杀敌人时调用
     * 
     * @return 当前COMBO倍率
     */
    public float onKill() {
        currentCombo++;
        isActive = true;
        decayTimer = EndlessModeConfig.COMBO_DECAY_TIME;

        // 更新最高COMBO
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo;
        }

        float multiplier = getMultiplier();

        // 回调监听器
        if (listener != null) {
            listener.onComboIncreased(currentCombo, multiplier);

            // 检查里程碑
            checkMilestone();
        }

        return multiplier;
    }

    /**
     * 检查是否达到里程碑
     */
    private void checkMilestone() {
        String milestoneName = null;

        // 只在刚好达到阈值时触发
        for (int i = EndlessModeConfig.COMBO_THRESHOLDS.length - 1; i > 0; i--) {
            if (currentCombo == EndlessModeConfig.COMBO_THRESHOLDS[i]) {
                milestoneName = EndlessModeConfig.COMBO_NAMES[i];
                break;
            }
        }

        if (milestoneName != null && listener != null) {
            listener.onMilestoneReached(currentCombo, milestoneName);
        }
    }

    /**
     * 获取当前得分倍率
     */
    public float getMultiplier() {
        return EndlessModeConfig.getComboMultiplier(currentCombo);
    }

    /**
     * 获取当前COMBO等级名称
     */
    public String getComboName() {
        return EndlessModeConfig.getComboName(currentCombo);
    }

    /**
     * 获取COMBO衰减进度 (0-1, 1表示刚击杀, 0表示即将重置)
     */
    public float getDecayProgress() {
        if (!isActive || currentCombo == 0) {
            return 0;
        }
        return decayTimer / EndlessModeConfig.COMBO_DECAY_TIME;
    }

    /**
     * 延长COMBO衰减时间（道具效果）
     */
    public void extendDecayTime(float extraSeconds) {
        if (isActive) {
            decayTimer += extraSeconds;
        }
    }

    /**
     * 强制重置COMBO
     */
    public void reset() {
        currentCombo = 0;
        maxCombo = 0;
        decayTimer = 0;
        isActive = false;
    }

    /**
     * 保留最高记录的重置（用于继续游戏）
     */
    public void softReset() {
        currentCombo = 0;
        decayTimer = 0;
        isActive = false;
    }

    // ========== Getters ==========

    public int getCurrentCombo() {
        return currentCombo;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public boolean isActive() {
        return isActive;
    }

    public float getDecayTimer() {
        return decayTimer;
    }

    // ========== Setters (用于存档恢复) ==========

    public void setCurrentCombo(int combo) {
        this.currentCombo = combo;
        if (combo > 0) {
            this.isActive = true;
            this.decayTimer = EndlessModeConfig.COMBO_DECAY_TIME;
        }
    }

    public void setMaxCombo(int maxCombo) {
        this.maxCombo = maxCombo;
    }
}
