package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.model.weapons.WeaponEffect;

/**
 * 投射物类 (Projectile)
 * 
 * 远程武器发射的飞行物体，具有：
 * - 位置和速度
 * - 伤害值和伤害类型
 * - 生命周期（超时自动消失）
 * - 碰撞检测（命中敌人/墙壁）
 */
public class Projectile extends GameObject {

    // 速度向量
    private float vx, vy;

    // 伤害属性
    private int damage;
    private DamageType damageType;
    private WeaponEffect effect;

    // 生命周期
    private float lifeTime;
    private float maxLifeTime = 3f;
    private boolean expired = false;

    // 归属（区分玩家/敌人发射）
    private boolean playerOwned;

    // 视觉效果
    private String textureKey;
    private float rotation; // 旋转角度（弧度）
    private float size = 1.0f; // 渲染缩放系数 (Render scale multiplier)

    /**
     * 创建投射物
     * 
     * @param x           起始 X 坐标
     * @param y           起始 Y 坐标
     * @param vx          X 方向速度
     * @param vy          Y 方向速度
     * @param damage      伤害值
     * @param damageType  伤害类型
     * @param effect      附加效果
     * @param playerOwned 是否为玩家发射
     * @param textureKey  纹理键名
     * @param size        渲染缩放系数
     */
    public Projectile(float x, float y, float vx, float vy,
            int damage, DamageType damageType, WeaponEffect effect,
            boolean playerOwned, String textureKey, float size) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.damageType = damageType;
        this.effect = effect;
        this.playerOwned = playerOwned;
        this.textureKey = textureKey;
        this.size = size;
        this.lifeTime = maxLifeTime;

        // 投射物碰撞体积较小
        this.width = 0.3f;
        this.height = 0.3f;

        // Create a new field to store start position
        this.startX = x;
        this.startY = y;

        // 计算旋转角度（指向飞行方向）
        this.rotation = (float) Math.atan2(vy, vx);
    }

    // Backward compatible constructor
    public Projectile(float x, float y, float vx, float vy,
            int damage, DamageType damageType, WeaponEffect effect,
            boolean playerOwned, String textureKey) {
        this(x, y, vx, vy, damage, damageType, effect, playerOwned, textureKey, 1.0f);
    }

    private final float startX;
    private final float startY;

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    /**
     * 更新投射物状态
     * 
     * @param delta 帧时间
     * @param cm    碰撞管理器（用于墙壁检测）
     * @return true 如果投射物应该被移除
     */
    public boolean update(float delta, CollisionManager cm) {
        // 更新生命周期
        lifeTime -= delta;
        if (lifeTime <= 0) {
            expired = true;
            return true;
        }

        // 计算新位置
        float newX = x + vx * delta;
        float newY = y + vy * delta;

        // 检查墙壁碰撞 (isWalkable 返回 true 表示可行走，取反表示被阻挡)
        if (cm != null && !cm.isWalkable((int) newX, (int) newY)) {
            expired = true;
            return true;
        }

        // 应用移动
        x = newX;
        y = newY;

        return false;
    }

    /**
     * 检查是否命中目标
     * 
     * @param target 目标对象
     * @return true 如果碰撞
     */
    public boolean hitsTarget(GameObject target) {
        // 简单的圆形碰撞检测
        float dx = target.getX() - x;
        float dy = target.getY() - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float combinedRadius = (width + target.getWidth()) / 2f;
        return distance < combinedRadius;
    }

    /**
     * 标记为已命中（将被移除）
     */
    public void markHit() {
        expired = true;
    }

    // === Getters ===

    public float getVx() {
        return vx;
    }

    public float getVy() {
        return vy;
    }

    public int getDamage() {
        return damage;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public WeaponEffect getEffect() {
        return effect;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    public String getTextureKey() {
        return textureKey;
    }

    public float getRotation() {
        return rotation;
    }

    public float getLifeTimeRemaining() {
        return lifeTime;
    }

    /**
     * 获取速度大小
     */
    public float getSpeed() {
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    /**
     * 获取渲染缩放系数 (Render scale multiplier)
     */
    public float getSize() {
        return size;
    }
}
