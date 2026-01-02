package de.tum.cit.fop.maze.model;

import java.util.Random;

/**
 * 代表一个可以巡逻和追逐玩家的敌人。
 * 使用平滑移动：视觉位置(x,y)插值到目标网格位置(targetX,targetY)。
 */
public class Enemy extends GameObject {
    public enum EnemyState {
        PATROL,
        CHASE
    }

    private EnemyState state;
    private Random random;

    // 目标网格位置
    private int targetX;
    private int targetY;

    // 移动速度 (每秒移动的格数)
    private static final float PATROL_SPEED = 3.0f;
    private static final float CHASE_SPEED = 4.5f;

    // AI 决策冷却 (防止每帧都做决策)
    private float decisionCooldown;
    private static final float PATROL_DECISION_COOLDOWN = 0.3f;
    private static final float CHASE_DECISION_COOLDOWN = 0.2f;

    // 巡逻方向改变计时器
    private int patrolDirX;
    private int patrolDirY;
    private float changeDirTimer;

    public Enemy(float x, float y) {
        super(x, y);
        this.targetX = Math.round(x);
        this.targetY = Math.round(y);
        this.state = EnemyState.PATROL;
        this.random = new Random();
        this.decisionCooldown = 0;
        this.changeDirTimer = 0;
        pickRandomDirection();
    }

    /**
     * 更新敌人：平滑移动 + AI 决策。
     */
    public void update(float delta, Player player, CollisionManager collisionManager) {
        // 1. 平滑移动视觉位置到目标位置
        float speed = (state == EnemyState.CHASE ? CHASE_SPEED : PATROL_SPEED) * delta;

        if (Math.abs(this.x - targetX) > 0.01f) {
            if (this.x < targetX) {
                this.x = Math.min(this.x + speed, targetX);
            } else {
                this.x = Math.max(this.x - speed, targetX);
            }
        } else {
            this.x = targetX;
        }

        if (Math.abs(this.y - targetY) > 0.01f) {
            if (this.y < targetY) {
                this.y = Math.min(this.y + speed, targetY);
            } else {
                this.y = Math.max(this.y - speed, targetY);
            }
        } else {
            this.y = targetY;
        }

        // 2. 减少决策冷却
        if (decisionCooldown > 0) {
            decisionCooldown -= delta;
            return; // 冷却中不做新决策
        }

        // 3. 只有到达目标格后才做新决策
        if (isMoving()) {
            return;
        }

        // 4. 状态转换
        float dist = distanceTo(player);
        if (dist < 5.0f) {
            state = EnemyState.CHASE;
        } else {
            state = EnemyState.PATROL;
        }

        // 5. 执行行为
        switch (state) {
            case PATROL:
                handlePatrol(delta, collisionManager);
                break;
            case CHASE:
                handleChase(player, collisionManager);
                break;
        }
    }

    private boolean isMoving() {
        return Math.abs(this.x - targetX) > 0.01f || Math.abs(this.y - targetY) > 0.01f;
    }

    private void handlePatrol(float delta, CollisionManager collisionManager) {
        changeDirTimer -= delta;
        if (changeDirTimer <= 0) {
            pickRandomDirection();
            changeDirTimer = 2.0f + random.nextFloat() * 2.0f;
        }

        int nextX = targetX + patrolDirX;
        int nextY = targetY + patrolDirY;

        if (collisionManager.isWalkable(nextX, nextY)) {
            targetX = nextX;
            targetY = nextY;
        } else {
            pickRandomDirection();
        }

        decisionCooldown = PATROL_DECISION_COOLDOWN;
    }

    private void handleChase(Player player, CollisionManager collisionManager) {
        int playerX = player.getTargetX();
        int playerY = player.getTargetY();

        int dx = Integer.compare(playerX, targetX);
        int dy = Integer.compare(playerY, targetY);

        boolean moved = false;

        if (dx != 0) {
            int nextX = targetX + dx;
            if (collisionManager.isWalkable(nextX, targetY)) {
                targetX = nextX;
                moved = true;
            }
        }

        if (!moved && dy != 0) {
            int nextY = targetY + dy;
            if (collisionManager.isWalkable(targetX, nextY)) {
                targetY = nextY;
            }
        }

        decisionCooldown = CHASE_DECISION_COOLDOWN;
    }

    private void pickRandomDirection() {
        int direction = random.nextInt(4);
        switch (direction) {
            case 0:
                patrolDirX = 1;
                patrolDirY = 0;
                break;
            case 1:
                patrolDirX = -1;
                patrolDirY = 0;
                break;
            case 2:
                patrolDirX = 0;
                patrolDirY = 1;
                break;
            case 3:
                patrolDirX = 0;
                patrolDirY = -1;
                break;
        }
    }

    private float distanceTo(GameObject other) {
        float dx = this.targetX - other.getX();
        float dy = this.targetY - other.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public EnemyState getState() {
        return state;
    }
}
