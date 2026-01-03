package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.model.weapons.WeaponEffect;
import java.util.Random;

/*
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  ⚠️  CORE AI FILE - DO NOT MODIFY WITHOUT TEAM LEAD APPROVAL ⚠️          ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  This file implements ENEMY AI with:                                      ║
 * ║  • State machine (PATROL / CHASE states)                                  ║
 * ║  • Axis-aligned pathfinding (simple but efficient)                        ║
 * ║  • Collision-aware movement (uses CollisionManager)                       ║
 * ║  • Knockback and stun mechanics                                           ║
 * ║                                                                           ║
 * ║  STATE MACHINE LOGIC (handlePatrol / handleChase):                        ║
 * ║  - PATROL: Random direction, changes every 2-4 seconds or on wall hit    ║
 * ║  - CHASE: Axis-aligned pursuit (X-first if X-diff > Y-diff)              ║
 * ║                                                                           ║
 * ║  PERFORMANCE: The simple chase algorithm is O(1) per enemy per frame.     ║
 * ║  Do NOT replace with A* or BFS unless you have profiled performance.      ║
 * ║                                                                           ║
 * ║  If you modify AI, test with level-4.properties (many enemies):           ║
 * ║  - Enemies should not clip through walls                                  ║
 * ║  - Enemies should chase player when in range                              ║
 * ║  - Frame rate should stay above 30 FPS on mid-range hardware              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * 代表一个可以巡逻和追逐玩家的敌人。
 */
public class Enemy extends GameObject {
    public enum EnemyState {
        PATROL,
        CHASE,
        DEAD
    }

    private EnemyState state;
    private Random random;

    // Stun logic
    private float stunTimer = 0f;
    private float hurtTimer = 0f; // Red flash timer
    private float deathTimer = 5.0f; // Dead body persists for 5s

    // Knockback
    private float knockbackVx = 0f;
    private float knockbackVy = 0f;
    private static final float KNOCKBACK_STRENGTH = 10.0f;
    private static final float KNOCKBACK_FRICTION = 5.0f;

    // Status Effects
    private WeaponEffect currentEffect = WeaponEffect.NONE;
    private float effectTimer = 0f;
    private float dotTimer = 0f; // Damage Over Time timer

    // 巡逻逻辑
    private float patrolDirX;
    private float patrolDirY;
    private float changeDirTimer;

    // 碰撞箱大小 (接近 1.0，但稍微内缩以避免卡住)
    private static final float SIZE = 0.99f;

    // 出生点 (领地中心)
    private final float homeX;
    private final float homeY;

    private int health = 3;

    public Enemy(float x, float y) {
        super(x, y);
        // 记录出生点
        this.homeX = x;
        this.homeY = y;

        this.state = EnemyState.PATROL;
        this.random = new Random();
        this.changeDirTimer = 0;
        pickRandomDirection();
    }

    public boolean takeDamage(int amount) {
        if (state == EnemyState.DEAD)
            return false;
        this.health -= amount;
        this.hurtTimer = 0.2f; // Flash red for 0.2s
        if (this.health <= 0) {
            this.state = EnemyState.DEAD;
            this.deathTimer = 5.0f;
            // Clear status effects
            this.currentEffect = WeaponEffect.NONE;
        }
        return false; // Never return true for immediate removal, GameScreen checks isRemovable()
    }

    public boolean isDead() {
        return state == EnemyState.DEAD;
    }

    public boolean isRemovable() {
        return state == EnemyState.DEAD && deathTimer <= 0;
    }

    public boolean isHurt() {
        return hurtTimer > 0;
    }

    public void applyEffect(WeaponEffect effect) {
        if (effect == WeaponEffect.NONE)
            return;

        this.currentEffect = effect;
        switch (effect) {
            case FREEZE:
                this.effectTimer = 2.0f; // Freeze for 2 seconds
                break;
            case BURN:
                this.effectTimer = 3.0f; // Burn for 3 seconds
                break;
            case POISON:
                this.effectTimer = 5.0f; // Poison for 5 seconds
                break;
            default:
                break;
        }
    }

    public WeaponEffect getCurrentEffect() {
        return currentEffect;
    }

    public int getHealth() {
        return health;
    }

    public int getSkillPointReward() {
        return 10; // Default reward
    }

    public void knockback(float sourceX, float sourceY, float strengthMultiplier, CollisionManager cm) {
        float dx = this.x - sourceX;
        float dy = this.y - sourceY;

        // Normalize
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length != 0) {
            dx /= length;
            dy /= length;
        }

        this.knockbackVx = dx * KNOCKBACK_STRENGTH * strengthMultiplier;
        this.knockbackVy = dy * KNOCKBACK_STRENGTH * strengthMultiplier;

        this.stunTimer = 0.5f;
    }

    /**
     * 更新敌人：基于状态的连续移动
     * 
     * @param safeGrid 安全路径网格 [x][y]
     */
    public void update(float delta, Player player, CollisionManager collisionManager, boolean[][] safeGrid) {
        // 0. Update Physics (Knockback) - Always runs to allow "flying corpses"
        if (Math.abs(knockbackVx) > 0.1f || Math.abs(knockbackVy) > 0.1f) {
            float moveX = knockbackVx * delta;
            float moveY = knockbackVy * delta;

            // Split movement to handle bounces on axis independently
            if (!tryMove(moveX, 0, collisionManager)) {
                // X Axis Collision
                if (Math.abs(knockbackVx) > 5.0f) {
                    takeDamage(1); // Small impact damage
                    // Visual/Audio could be added here
                    System.out.println("Enemy hit wall hard! (X)");
                }
                knockbackVx = -knockbackVx * 0.5f; // Bounce X (0.5 elasticity)
            }
            if (!tryMove(0, moveY, collisionManager)) {
                // Y Axis Collision
                if (Math.abs(knockbackVy) > 5.0f) {
                    takeDamage(1); // Small impact damage
                    System.out.println("Enemy hit wall hard! (Y)");
                }
                knockbackVy = -knockbackVy * 0.5f; // Bounce Y (0.5 elasticity)
            }

            // Friction
            knockbackVx -= knockbackVx * KNOCKBACK_FRICTION * delta;
            knockbackVy -= knockbackVy * KNOCKBACK_FRICTION * delta;

            if (Math.abs(knockbackVx) < 0.5f)
                knockbackVx = 0;
            if (Math.abs(knockbackVy) < 0.5f)
                knockbackVy = 0;
        }

        if (state == EnemyState.DEAD) {
            deathTimer -= delta;
            return; // No AI updates if dead
        }

        if (stunTimer > 0) {
            stunTimer -= delta;
            // Don't run AI if stunned, but allow physics to continue above
            return;
        }
        if (hurtTimer > 0) {
            hurtTimer -= delta;
        }

        // Handle Status Effects
        if (currentEffect != WeaponEffect.NONE) {
            effectTimer -= delta;

            if (currentEffect == WeaponEffect.FREEZE) {
                // Freeze: Stop movement entirely
                if (effectTimer <= 0) {
                    currentEffect = WeaponEffect.NONE;
                }
                return;
            } else if (currentEffect == WeaponEffect.BURN || currentEffect == WeaponEffect.POISON) {
                // DOT Logic
                dotTimer += delta;
                if (dotTimer >= 1.0f) { // Damage every 1 second
                    takeDamage(1);
                    dotTimer = 0f;
                    System.out.println("Enemy takes DOT from " + currentEffect);
                }
            }

            if (effectTimer <= 0) {
                currentEffect = WeaponEffect.NONE;
                dotTimer = 0f;
            }

            // Check death from DOT
            if (this.health <= 0) {
                // GameScreen handles removal based on health check usually,
                // but we need to ensure it's removed if it dies during update.
                // However, GameScreen usually checks enemies list in its update loop.
                // We will let GameScreen handle removal for now.
            }
        }
        // 1. 状态判断
        // Check distance to ENEMY itself, not Home (Chase on Sight vs Territorial)
        // Also removed !isPlayerSafe check so enemies chase even if on optimal path.
        float distToSelf = distanceToPoint(this.getX(), this.getY(), player.getX(), player.getY());

        // Use a larger range if needed, or stick to settings.
        // Assuming settings range is "Visual Range".
        if (distToSelf < GameSettings.enemyDetectRange) {
            state = EnemyState.CHASE;
        } else {
            // Stop chasing if far away
            state = EnemyState.PATROL;
        }

        // 2. 执行行为
        float moveAmount = (state == EnemyState.CHASE ? GameSettings.enemyChaseSpeed : GameSettings.enemyPatrolSpeed)
                * delta;
        boolean moved = false;

        switch (state) {
            case PATROL:
                moved = handlePatrol(moveAmount, delta, collisionManager);
                break;
            case CHASE:
                moved = handleChase(moveAmount, player, collisionManager);
                break;
            case DEAD:
                // Do nothing
                break;
        }

        // 3. 如果没有移动，自动对齐到最近的整数格
        if (!moved) {
            snapToGrid(delta);
        }
    }

    /**
     * 平滑对齐到整数格
     */
    private void snapToGrid(float delta) {
        float snapSpeed = 5.0f * delta;

        float targetX = Math.round(this.x);
        float targetY = Math.round(this.y);

        float dx = targetX - this.x;
        float dy = targetY - this.y;

        if (Math.abs(dx) < 0.01f && Math.abs(dy) < 0.01f) {
            this.x = targetX;
            this.y = targetY;
        } else {
            this.x += Math.signum(dx) * Math.min(Math.abs(dx), snapSpeed);
            this.y += Math.signum(dy) * Math.min(Math.abs(dy), snapSpeed);
        }
    }

    private boolean handlePatrol(float moveAmount, float delta, CollisionManager collisionManager) {
        // 计时器：每隔一段时间随机换个方向
        changeDirTimer -= delta;
        if (changeDirTimer <= 0) {
            pickRandomDirection();
            changeDirTimer = 2.0f + random.nextFloat() * 2.0f; // 2~4秒换一次
        }

        // 尝试沿当前方向移动
        boolean moved = tryMove(patrolDirX * moveAmount, patrolDirY * moveAmount, collisionManager);
        if (!moved) {
            // 如果撞墙了，立即换个方向
            pickRandomDirection();
        }
        return moved;
    }

    private boolean handleChase(float moveAmount, Player player, CollisionManager collisionManager) {
        // 计算与玩家的距离差
        float dx = player.getX() - this.x;
        float dy = player.getY() - this.y;

        // 简单的轴对齐追逐算法：
        // 优先在距离差较大的轴上移动。如果那个方向堵住了，就试另一个方向。

        boolean moved = false;

        if (Math.abs(dx) > Math.abs(dy)) {
            // X轴距离更远，优先尝试水平移动
            if (Math.abs(dx) > 0.1f) {
                float sign = Math.signum(dx);
                moved = tryMove(sign * moveAmount, 0, collisionManager);
            }
            // 如果水平走不通，尝试垂直
            if (!moved && Math.abs(dy) > 0.1f) {
                float sign = Math.signum(dy);
                moved = tryMove(0, sign * moveAmount, collisionManager);
            }
        } else {
            // Y轴距离更远，优先尝试垂直移动
            if (Math.abs(dy) > 0.1f) {
                float sign = Math.signum(dy);
                moved = tryMove(0, sign * moveAmount, collisionManager);
            }
            // 如果垂直走不通，尝试水平
            if (!moved && Math.abs(dx) > 0.1f) {
                float sign = Math.signum(dx);
                moved = tryMove(sign * moveAmount, 0, collisionManager);
            }
        }
        return moved;
    }

    /**
     * 尝试移动。如果目标位置没有碰撞，则应用移动并返回 true。
     */
    private boolean tryMove(float deltaX, float deltaY, CollisionManager cm) {
        float newX = this.x + deltaX;
        float newY = this.y + deltaY;

        // 碰撞检测：检查自身的四个角落
        float padding = 0.1f; // 内缩一点，防止卡住

        boolean canMove = isWalkable(newX + padding, newY + padding, cm) &&
                isWalkable(newX + SIZE - padding, newY + padding, cm) &&
                isWalkable(newX + SIZE - padding, newY + SIZE - padding, cm) &&
                isWalkable(newX + padding, newY + SIZE - padding, cm);

        if (canMove) {
            this.x = newX;
            this.y = newY;
            return true;
        }
        return false;
    }

    private boolean isWalkable(float x, float y, CollisionManager cm) {
        return cm.isWalkableForEnemy((int) x, (int) y);
    }

    private void pickRandomDirection() {
        int dir = random.nextInt(4);
        switch (dir) {
            case 0:
                patrolDirX = 1;
                patrolDirY = 0;
                break; // 右
            case 1:
                patrolDirX = -1;
                patrolDirY = 0;
                break; // 左
            case 2:
                patrolDirX = 0;
                patrolDirY = 1;
                break; // 上
            case 3:
                patrolDirX = 0;
                patrolDirY = -1;
                break; // 下
        }
    }

    public EnemyState getState() {
        return state;
    }

    // 兼容旧代码的 getter，防止其他地方报错
    public int getTargetX() {
        return (int) x;
    }

    public int getTargetY() {
        return (int) y;
    }

    private float distanceToPoint(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}