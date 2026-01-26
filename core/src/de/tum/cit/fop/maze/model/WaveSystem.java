package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.config.EndlessModeConfig;

/**
 * 时间波次系统 (Wave System)
 * 
 * 管理无尽模式中的时间压力和难度递增。
 * 
 * 机制:
 * - 根据生存时间划分波次
 * - 每个波次有不同的敌人刷新率和血量倍率
 * - 12分钟后开始出现BOSS级敌人
 * 
 * 遵循单一职责原则：仅处理波次逻辑。
 */
public class WaveSystem {

    /** 当前生存时间（秒） */
    private float survivalTime;

    /** 当前波次索引 (0-5) */
    private int currentWaveIndex;

    /** 下一次敌人刷新时间 */
    private float nextSpawnTime;

    /** 下一次BOSS刷新时间 */
    private float nextBossTime;

    /** 监听器：波次变化时回调 */
    private WaveListener listener;

    /**
     * 波次变化监听器接口
     */
    public interface WaveListener {
        /** 波次变化时调用 */
        void onWaveChanged(int newWave, float spawnInterval, float healthMultiplier);

        /** 需要刷新敌人时调用 */
        void onSpawnEnemy();

        /** 需要刷新BOSS时调用 */
        void onSpawnBoss();
    }

    public WaveSystem() {
        reset();
    }

    /**
     * 设置波次变化监听器
     */
    public void setListener(WaveListener listener) {
        this.listener = listener;
    }

    /**
     * 每帧更新
     * 
     * @param delta 帧间隔时间（秒）
     */
    public void update(float delta) {
        survivalTime += delta;

        // 检查波次变化
        int newWaveIndex = EndlessModeConfig.getWaveIndex(survivalTime);
        if (newWaveIndex != currentWaveIndex) {
            currentWaveIndex = newWaveIndex;

            if (listener != null) {
                listener.onWaveChanged(
                        currentWaveIndex,
                        getSpawnInterval(),
                        getEnemyHealthMultiplier());
            }
        }

        // 检查敌人刷新（安全期内不刷新）
        if (survivalTime >= EndlessModeConfig.SAFE_PERIOD_DURATION &&
                survivalTime >= nextSpawnTime) {
            nextSpawnTime = survivalTime + getSpawnInterval();

            if (listener != null) {
                listener.onSpawnEnemy();
            }
        }

        // 检查BOSS刷新 (12分钟后每2分钟一个)
        if (survivalTime >= EndlessModeConfig.FIRST_BOSS_TIME &&
                survivalTime >= nextBossTime) {
            nextBossTime = survivalTime + EndlessModeConfig.BOSS_SPAWN_INTERVAL;

            if (listener != null) {
                listener.onSpawnBoss();
            }
        }
    }

    /**
     * 获取当前敌人刷新间隔（秒）
     */
    public float getSpawnInterval() {
        return EndlessModeConfig.WAVE_SPAWN_INTERVALS[currentWaveIndex];
    }

    /**
     * 获取当前敌人血量倍率
     */
    public float getEnemyHealthMultiplier() {
        return EndlessModeConfig.WAVE_HEALTH_MULTIPLIERS[currentWaveIndex];
    }

    /**
     * 获取当前波次索引 (0-5)
     */
    public int getCurrentWave() {
        return currentWaveIndex;
    }

    /**
     * 获取当前波次显示名称 (Wave 1-6)
     */
    public String getWaveName() {
        return "Wave " + (currentWaveIndex + 1);
    }

    /**
     * 获取下一波次开始时间（秒）
     * 
     * @return 下一波次时间，如果已是最后波次返回-1
     */
    public float getNextWaveTime() {
        if (currentWaveIndex >= EndlessModeConfig.WAVE_TIME_THRESHOLDS.length - 1) {
            return -1;
        }
        return EndlessModeConfig.WAVE_TIME_THRESHOLDS[currentWaveIndex + 1];
    }

    /**
     * 获取到下一波次的剩余时间（秒）
     */
    public float getTimeToNextWave() {
        float nextTime = getNextWaveTime();
        if (nextTime < 0) {
            return -1;
        }
        return Math.max(0, nextTime - survivalTime);
    }

    /**
     * 是否已进入BOSS刷新阶段
     */
    public boolean isBossPhase() {
        return survivalTime >= EndlessModeConfig.FIRST_BOSS_TIME;
    }

    /**
     * 获取到下一次BOSS的剩余时间（秒）
     */
    public float getTimeToNextBoss() {
        if (!isBossPhase()) {
            return EndlessModeConfig.FIRST_BOSS_TIME - survivalTime;
        }
        return Math.max(0, nextBossTime - survivalTime);
    }

    /**
     * 获取格式化的生存时间 (MM:SS)
     */
    public String getFormattedTime() {
        int minutes = (int) (survivalTime / 60);
        int seconds = (int) (survivalTime % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 强制重置
     */
    public void reset() {
        survivalTime = 0;
        currentWaveIndex = 0;
        nextSpawnTime = EndlessModeConfig.WAVE_SPAWN_INTERVALS[0];
        nextBossTime = EndlessModeConfig.FIRST_BOSS_TIME;
    }

    // ========== Getters ==========

    public float getSurvivalTime() {
        return survivalTime;
    }

    // ========== Setters (用于存档恢复) ==========

    public void setSurvivalTime(float time) {
        this.survivalTime = time;
        this.currentWaveIndex = EndlessModeConfig.getWaveIndex(time);
        this.nextSpawnTime = time + getSpawnInterval();

        // 计算下一次BOSS时间
        if (time >= EndlessModeConfig.FIRST_BOSS_TIME) {
            float timeSinceFirstBoss = time - EndlessModeConfig.FIRST_BOSS_TIME;
            int bossesSpawned = (int) (timeSinceFirstBoss / EndlessModeConfig.BOSS_SPAWN_INTERVAL);
            this.nextBossTime = EndlessModeConfig.FIRST_BOSS_TIME +
                    (bossesSpawned + 1) * EndlessModeConfig.BOSS_SPAWN_INTERVAL;
        } else {
            this.nextBossTime = EndlessModeConfig.FIRST_BOSS_TIME;
        }
    }
}
