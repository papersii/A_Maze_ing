package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.utils.GameLogger;

/**
 * 宝箱奖励封装类 (Chest Reward)
 * 
 * 封装宝箱打开后获得的各种奖励。
 * 支持关卡模式和无尽模式的不同奖励类型。
 */
public class ChestReward {

    /**
     * 奖励类型枚举
     */
    public enum RewardType {
        // 关卡模式奖励 (Level Mode Rewards)
        WEAPON, // 武器
        COIN, // 金币
        HEALTH, // 生命恢复
        INVINCIBILITY, // 无敌状态

        // 无尽模式奖励 (Endless Mode Rewards)
        MEDKIT, // 急救包 (+1 生命)
        SPEED_BUFF, // 极速 (移速+50%)
        RAGE_BUFF, // 狂暴 (攻击冷却减半)
        SHIELD_BUFF, // 护盾 (抵挡一次伤害)
        EMP // 电磁脉冲 (全屏清场)
    }

    private final RewardType type;
    private final int value; // 数值：金币数量、生命恢复量、Buff持续时间(秒)等
    private final Object payload; // 载荷：武器对象等

    // ========== 静态工厂方法 ==========

    /**
     * 创建武器奖励
     */
    public static ChestReward weapon(Weapon weapon) {
        return new ChestReward(RewardType.WEAPON, 0, weapon);
    }

    /**
     * 创建金币奖励
     */
    public static ChestReward coins(int amount) {
        return new ChestReward(RewardType.COIN, amount, null);
    }

    /**
     * 创建生命恢复奖励
     */
    public static ChestReward health(int amount) {
        return new ChestReward(RewardType.HEALTH, amount, null);
    }

    /**
     * 创建无敌奖励
     * 
     * @param durationSeconds 无敌持续时间（秒）
     */
    public static ChestReward invincibility(int durationSeconds) {
        return new ChestReward(RewardType.INVINCIBILITY, durationSeconds, null);
    }

    /**
     * 创建急救包奖励（无尽模式）
     */
    public static ChestReward medkit() {
        return new ChestReward(RewardType.MEDKIT, 1, null);
    }

    /**
     * 创建速度Buff奖励（无尽模式）
     * 
     * @param durationSeconds Buff持续时间（秒）
     */
    public static ChestReward speedBuff(int durationSeconds) {
        return new ChestReward(RewardType.SPEED_BUFF, durationSeconds, null);
    }

    /**
     * 创建狂暴Buff奖励（无尽模式）
     * 
     * @param durationSeconds Buff持续时间（秒）
     */
    public static ChestReward rageBuff(int durationSeconds) {
        return new ChestReward(RewardType.RAGE_BUFF, durationSeconds, null);
    }

    /**
     * 创建护盾奖励（无尽模式）
     */
    public static ChestReward shield() {
        return new ChestReward(RewardType.SHIELD_BUFF, 1, null);
    }

    /**
     * 创建EMP清场奖励（无尽模式）
     */
    public static ChestReward emp() {
        return new ChestReward(RewardType.EMP, 0, null);
    }

    // ========== 构造函数 ==========

    private ChestReward(RewardType type, int value, Object payload) {
        this.type = type;
        this.value = value;
        this.payload = payload;
    }

    // ========== 核心方法 ==========

    /**
     * 将奖励应用到玩家
     * 
     * @param player 玩家对象
     * @return true 如果成功应用
     */
    public boolean applyToPlayer(Player player) {
        if (player == null) {
            return false;
        }

        GameLogger.info("ChestReward", "Applying reward: " + type + ", value=" + value);

        switch (type) {
            case WEAPON:
                if (payload instanceof Weapon) {
                    return player.pickupWeapon((Weapon) payload);
                }
                return false;

            case COIN:
                player.addCoins(value);
                return true;

            case HEALTH:
                // 提升最大生命值并增加当前生命（突破上限）
                player.upgradeMaxHealth(value);
                return true;

            case INVINCIBILITY:
                player.setInvincible(true);
                player.setInvincibilityTimer(value);
                return true;

            case MEDKIT:
                player.restoreHealth(1);
                return true;

            case SPEED_BUFF:
                player.applySpeedBuff(value);
                return true;

            case RAGE_BUFF:
                player.applyRageBuff(value);
                return true;

            case SHIELD_BUFF:
                player.applyShield();
                return true;

            case EMP:
                // EMP 效果需要在 GameScreen/EndlessGameScreen 中处理
                // 这里只标记已触发
                return true;

            default:
                GameLogger.warn("ChestReward", "Unknown reward type: " + type);
                return false;
        }
    }

    // ========== Getters ==========

    public RewardType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public Object getPayload() {
        return payload;
    }

    /**
     * 获取奖励的显示名称（用于UI）
     */
    public String getDisplayName() {
        switch (type) {
            case WEAPON:
                return payload instanceof Weapon ? ((Weapon) payload).getName() : "Mystery Weapon";
            case COIN:
                return "Coins " + value;
            case HEALTH:
                return "Health " + value;
            case INVINCIBILITY:
                return "Invincibility " + value + " s";
            case MEDKIT:
                return "Medkit";
            case SPEED_BUFF:
                return "Speed " + value + " s";
            case RAGE_BUFF:
                return "Rage " + value + " s";
            case SHIELD_BUFF:
                return "Shield";
            case EMP:
                return "EMP";
            default:
                return "Unknown Reward";
        }
    }

    /**
     * 判断是否为即时效果（无需额外处理）
     */
    public boolean isInstantEffect() {
        return type == RewardType.COIN || type == RewardType.HEALTH || type == RewardType.MEDKIT;
    }

    /**
     * 判断是否为Buff类型
     */
    public boolean isBuffType() {
        return type == RewardType.INVINCIBILITY ||
                type == RewardType.SPEED_BUFF ||
                type == RewardType.RAGE_BUFF ||
                type == RewardType.SHIELD_BUFF;
    }

    @Override
    public String toString() {
        return "ChestReward{" +
                "type=" + type +
                ", value=" + value +
                ", displayName='" + getDisplayName() + '\'' +
                '}';
    }
}
