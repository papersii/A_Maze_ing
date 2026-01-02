package de.tum.cit.fop.maze.model;

/**
 * 代表游戏中的玩家角色。
 * 使用平滑移动：视觉位置(x,y)插值到目标网格位置(targetX,targetY)。
 */
public class Player extends GameObject {
    private int lives;
    private boolean hasKey;
    private boolean isRunning;

    // 目标网格位置 (用于碰撞检测)
    private int targetX;
    private int targetY;

    // 移动速度 (每秒移动的格数)
    private static final float WALK_SPEED = 5.0f; // 行走速度
    private static final float RUN_SPEED = 8.0f; // 跑步速度

    // 输入冷却 (防止按住键时立即连续移动)
    private float inputCooldown;
    private static final float WALK_INPUT_COOLDOWN = 0.15f;
    private static final float RUN_INPUT_COOLDOWN = 0.08f;

    // 无敌时间 (受伤后短暂无敌，防止连续扣血)
    private float invincibilityTimer;
    private static final float INVINCIBILITY_DURATION = 1.0f;

    public Player(float x, float y) {
        super(x, y);
        this.targetX = Math.round(x);
        this.targetY = Math.round(y);
        this.lives = 3;
        this.hasKey = false;
        this.isRunning = false;
        this.inputCooldown = 0;
        this.invincibilityTimer = 0;
    }

    /**
     * 更新玩家状态：平滑移动视觉位置到目标位置。
     * 
     * @param delta 上一帧以来的时间
     */
    public void update(float delta) {
        // 减少输入冷却计时器
        if (inputCooldown > 0) {
            inputCooldown -= delta;
        }

        // 减少无敌计时器
        if (invincibilityTimer > 0) {
            invincibilityTimer -= delta;
        }

        // 平滑移动视觉位置到目标位置
        float speed = (isRunning ? RUN_SPEED : WALK_SPEED) * delta;

        // X 轴插值
        if (Math.abs(this.x - targetX) > 0.01f) {
            if (this.x < targetX) {
                this.x = Math.min(this.x + speed, targetX);
            } else {
                this.x = Math.max(this.x - speed, targetX);
            }
        } else {
            this.x = targetX; // 对齐到整数
        }

        // Y 轴插值
        if (Math.abs(this.y - targetY) > 0.01f) {
            if (this.y < targetY) {
                this.y = Math.min(this.y + speed, targetY);
            } else {
                this.y = Math.max(this.y - speed, targetY);
            }
        } else {
            this.y = targetY; // 对齐到整数
        }
    }

    /**
     * 检查是否可以接受新的移动输入。
     */
    public boolean canAcceptInput() {
        return inputCooldown <= 0;
    }

    /**
     * 检查玩家是否正在移动中 (视觉位置未到达目标)。
     */
    public boolean isMoving() {
        return Math.abs(this.x - targetX) > 0.01f || Math.abs(this.y - targetY) > 0.01f;
    }

    /**
     * 设置新的目标网格位置 (不直接改变视觉位置)。
     * 调用此方法前必须检查 collision。
     * 
     * @param deltaX X 轴移动方向 (-1, 0, 或 1)
     * @param deltaY Y 轴移动方向 (-1, 0, 或 1)
     */
    public void moveGrid(int deltaX, int deltaY) {
        this.targetX += deltaX;
        this.targetY += deltaY;
        // 重置输入冷却
        this.inputCooldown = isRunning ? RUN_INPUT_COOLDOWN : WALK_INPUT_COOLDOWN;
    }

    /**
     * 减少玩家指定数量的生命值。
     * 只有在非无敌状态下才会受伤。
     */
    public boolean damage(int amount) {
        if (invincibilityTimer > 0) {
            return false;
        }

        this.lives -= amount;
        if (this.lives < 0) {
            this.lives = 0;
        }

        this.invincibilityTimer = INVINCIBILITY_DURATION;
        return true;
    }

    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    // --- Getters ---

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public boolean hasKey() {
        return hasKey;
    }

    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
    }
}
