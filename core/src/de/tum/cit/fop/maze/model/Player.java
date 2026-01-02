package de.tum.cit.fop.maze.model;

/**
 * 代表游戏中的玩家角色。
 * 处理基于网格的移动、生命值计数和钥匙清单。
 */
public class Player extends GameObject {
    private int lives;
    private boolean hasKey;
    private boolean isRunning;

    // 移动冷却计时器 (防止按住键时移动过快)
    private float moveCooldown;
    private static final float WALK_COOLDOWN = 0.2f; // 行走时每 0.2 秒移动一格
    private static final float RUN_COOLDOWN = 0.1f; // 跑步时每 0.1 秒移动一格

    // 无敌时间 (受伤后短暂无敌，防止连续扣血)
    private float invincibilityTimer;
    private static final float INVINCIBILITY_DURATION = 1.0f; // 受伤后无敌 1 秒

    public Player(float x, float y) {
        super(x, y);
        this.lives = 3; // 初始3条命
        this.hasKey = false;
        this.isRunning = false;
        this.moveCooldown = 0;
        this.invincibilityTimer = 0;
    }

    /**
     * 更新玩家状态。
     * 
     * @param delta 上一帧以来的时间
     */
    public void update(float delta) {
        // 减少移动冷却计时器
        if (moveCooldown > 0) {
            moveCooldown -= delta;
        }
        // 减少无敌计时器
        if (invincibilityTimer > 0) {
            invincibilityTimer -= delta;
        }
    }

    /**
     * 检查玩家是否可以移动 (冷却完成)。
     * 
     * @return 如果可以移动则返回 true
     */
    public boolean canMove() {
        return moveCooldown <= 0;
    }

    /**
     * 在网格上移动一格 (整数移动)。
     * 此方法不检查 collisions；调用此方法前必须检查 collision。
     * 
     * @param deltaX X 轴移动方向 (-1, 0, 或 1)
     * @param deltaY Y 轴移动方向 (-1, 0, 或 1)
     */
    public void moveGrid(int deltaX, int deltaY) {
        this.x += deltaX;
        this.y += deltaY;
        // 重置冷却
        this.moveCooldown = isRunning ? RUN_COOLDOWN : WALK_COOLDOWN;
    }

    /**
     * 减少玩家指定数量的生命值。
     * 只有在非无敌状态下才会受伤。
     * 
     * @param amount 要移除的生命值数量。
     * @return 如果实际受伤则返回 true，如果处于无敌状态则返回 false
     */
    public boolean damage(int amount) {
        if (invincibilityTimer > 0) {
            return false; // 无敌中，不受伤
        }

        this.lives -= amount;
        if (this.lives < 0) {
            this.lives = 0;
        }

        // 触发无敌时间
        this.invincibilityTimer = INVINCIBILITY_DURATION;
        return true;
    }

    /**
     * 检查玩家是否处于无敌状态。
     * 
     * @return 如果无敌则返回 true
     */
    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    /**
     * 设置玩家是否正在跑步（按住 Shift 键）。
     * 
     * @param running 如果正在跑步则为 true，否则为 false。
     */
    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    // --- Getters 和 Setters ---

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

    /**
     * 获取当前移动冷却时间。
     */
    public float getMoveCooldown() {
        return moveCooldown;
    }
}
