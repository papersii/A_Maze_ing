package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.model.items.Armor;
import de.tum.cit.fop.maze.utils.GameLogger;
import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.model.weapons.Sword;
import de.tum.cit.fop.maze.model.weapons.Crossbow;
import de.tum.cit.fop.maze.model.weapons.Wand;
import de.tum.cit.fop.maze.utils.BloodParticleSystem;
import java.util.ArrayList;
import java.util.List;

/*
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  ⚠️  CORE ENTITY FILE - DO NOT MODIFY WITHOUT TEAM LEAD APPROVAL ⚠️      ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  This file implements the PLAYER entity with:                             ║
 * ║  • Smooth continuous movement (no grid snapping)                          ║
 * ║  • Shift key running toggle (via GameSettings speeds)                     ║
 * ║  • Invincibility frames after taking damage                               ║
 * ║  • Attack cooldown system                                                 ║
 * ║  • Hurt timer for red flash VFX                                           ║
 * ║                                                                           ║
 * ║  CRITICAL METHODS (do not change signatures):                             ║
 * ║  - move(deltaX, deltaY): Relative movement used by GameScreen             ║
 * ║  - setPosition(x, y): Absolute positioning for save/load                  ║
 * ║  - damage(amount): Returns true if damage was applied (not invincible)    ║
 * ║  - getSpeed(): Returns walk or run speed from GameSettings                ║
 * ║                                                                           ║
 * ║  If you modify movement logic, test with all 5 maps and ensure:           ║
 * ║  - No clipping through walls                                              ║
 * ║  - Running (Shift) is noticeably faster                                   ║
 * ║  - Save/Load correctly restores position                                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * 代表游戏中的玩家角色。
 * 已回退为：连续平滑移动模式 (无网格锁定)
 */
public class Player extends GameObject {
    private int lives;
    private boolean hasKey;
    private boolean isRunning;

    // 无敌计时器
    private float invincibilityTimer;
    private float attackTimer = 0f;
    private float hurtTimer = 0f;

    // Knockback
    private float knockbackVx = 0f;
    private float knockbackVy = 0f;
    private static final float KNOCKBACK_STRENGTH = 10.0f;
    private static final float KNOCKBACK_FRICTION = 5.0f; // Match enemy knockback friction

    // === Movement Physics (Inertia System) ===
    private float velocityX = 0f; // Current horizontal velocity (tiles/second)
    private float velocityY = 0f; // Current vertical velocity (tiles/second)

    // Physics Constants (tuned for responsive yet smooth movement)
    // Removed 'final' to allow runtime tuning via DeveloperConsole
    private static float ACCELERATION = 45.0f; // How fast to reach max speed (tiles/s²)
    private static float DECELERATION = 35.0f; // How fast to stop when no input (tiles/s²)
    private static float TURN_BOOST = 1.4f; // Acceleration multiplier when reversing direction
    private static float WALL_BOUNCE = 0.25f; // Elasticity when hitting walls at high speed
    private static float WALL_SLIDE_MULT = 0.7f; // Velocity retention when sliding along walls
    private static float VELOCITY_THRESHOLD = 0.1f; // Minimum velocity before snapping to zero

    // Animation State
    private boolean isAttacking = false;
    private float attackAnimTimer = 0f;
    private float attackAnimTotalDuration = 0f;

    // Death Animation
    private boolean isDead = false;
    private float deathTimer = 0f;
    private static final float DEATH_ANIMATION_DURATION = 2.0f;

    // Weapon Inventory
    private List<Weapon> inventory;
    private int currentWeaponIndex;
    private String justSwitchedWeaponName = null;
    private float switchNameTimer = 0f;

    // Skill System Fields
    private int skillPoints;
    private int maxHealthBonus;
    private int damageBonus;
    private float invincibilityExtension; // Added to base invincibility duration

    // New Skills (Phase 2)
    private float knockbackMultiplier = 1.0f; // Multiplier for knockback strength
    private float cooldownReduction = 0.0f; // Reduction in seconds (Max 0.5s)
    private float speedBonus = 0.0f; // Flat speed addition (or multiplier)

    // === Armor System ===
    private Armor equippedArmor = null;

    // === Economy System ===
    private int coins = 0;

    // === Energy System (for weapon attacks) ===
    private float energy = 100f;
    private float maxEnergy = 100f;
    private float energyRegenRate = 5f; // Per second

    // === Cheat Flags (for Developer Console) ===
    private boolean godMode = false;
    private boolean noClip = false;

    // Blood particle listener (for visual damage feedback)
    private BloodParticleSystem.DamageListener damageListener = null;

    // 碰撞箱大小 (接近 1.0)
    private static final float SIZE = 0.99f;

    public Player(float x, float y) {
        super(x, y);
        this.lives = GameSettings.playerMaxLives; // 使用配置的初始生命值
        this.hasKey = false;
        this.isRunning = false;

        this.invincibilityTimer = 0;

        // Initialize Inventory with a default sword
        this.inventory = new ArrayList<>();
        this.inventory.add(new Sword(x, y)); // Default weapon
        this.currentWeaponIndex = 0;

        // Skill System Initialization
        this.skillPoints = 0;
        this.maxHealthBonus = 0;
        this.damageBonus = 0;
        this.invincibilityExtension = 0.0f;

        // New Skills Defaults
        this.knockbackMultiplier = 1.0f;
        this.cooldownReduction = 0.0f;
        this.speedBonus = 0.0f;
    }

    public void update(float delta, CollisionManager cm) {
        // Check if player died from any cause (not just damage())
        if (!isDead && lives <= 0) {
            isDead = true;
            deathTimer = DEATH_ANIMATION_DURATION;
            GameLogger.info("Player", "Player died (Update Check). Lives: " + lives);
        }

        if (invincibilityTimer > 0) {
            invincibilityTimer -= delta;
        }
        if (attackTimer > 0) {
            attackTimer -= delta;
        }
        if (hurtTimer > 0) {
            hurtTimer -= delta;
        }
        if (switchNameTimer > 0) {
            switchNameTimer -= delta;
            if (switchNameTimer <= 0) {
                justSwitchedWeaponName = null;
            }
        }

        // ... rest of update

        // Update Physics (Knockback) with Wall Bouncing
        if (Math.abs(knockbackVx) > 0.1f || Math.abs(knockbackVy) > 0.1f) {
            float moveX = knockbackVx * delta;
            float moveY = knockbackVy * delta;

            // Try Move X
            if (!tryMove(moveX, 0, cm)) {
                knockbackVx = -knockbackVx * 0.5f; // Bounce X with energy loss
            }
            // Try Move Y
            if (!tryMove(0, moveY, cm)) {
                knockbackVy = -knockbackVy * 0.5f; // Bounce Y with energy loss
            }

            knockbackVx -= knockbackVx * KNOCKBACK_FRICTION * delta;
            knockbackVy -= knockbackVy * KNOCKBACK_FRICTION * delta;

            if (Math.abs(knockbackVx) < 0.5f)
                knockbackVx = 0;
            if (Math.abs(knockbackVy) < 0.5f)
                knockbackVy = 0;
        }

        // Death Animation Timer
        if (isDead && deathTimer > 0) {
            float oldTimer = deathTimer;
            deathTimer -= delta;
            // GameLogger.debug("Player", "Death timer: " + oldTimer + " -> " + deathTimer);
        }

        // Attack Animation Timer
        if (isAttacking) {
            attackAnimTimer -= delta;
            if (attackAnimTimer <= 0) {
                isAttacking = false;
            }
        }

        // Energy Regeneration
        regenEnergy(delta);

        // Final death check (catches deaths from traps, etc that happen after this
        // update)
        if (!isDead && lives <= 0) {
            isDead = true;
            deathTimer = DEATH_ANIMATION_DURATION;
        }
    }

    /**
     * 仅更新定时器（用于无尽模式，无需碰撞管理器）
     * Update timers only, without collision management - used for Endless Mode
     */
    public void updateTimers(float delta) {
        // 死亡检查
        if (!isDead && lives <= 0) {
            isDead = true;
            deathTimer = DEATH_ANIMATION_DURATION;
            GameLogger.info("Player", "Player died (Timer Update Check). Lives: " + lives);
        }

        // 无敌计时器
        if (invincibilityTimer > 0) {
            invincibilityTimer -= delta;
        }
        // 攻击冷却计时器
        if (attackTimer > 0) {
            attackTimer -= delta;
        }
        // 受伤红闪计时器
        if (hurtTimer > 0) {
            hurtTimer -= delta;
        }
        // 武器切换提示计时器
        if (switchNameTimer > 0) {
            switchNameTimer -= delta;
            if (switchNameTimer <= 0) {
                justSwitchedWeaponName = null;
            }
        }

        // 攻击动画计时器
        if (isAttacking) {
            attackAnimTimer -= delta;
            if (attackAnimTimer <= 0) {
                isAttacking = false;
            }
        }

        // 死亡动画计时器
        if (isDead && deathTimer > 0) {
            deathTimer -= delta;
        }
    }

    // Death Animation Getters
    public boolean isDead() {
        return isDead;
    }

    public float getDeathTimer() {
        return deathTimer;
    }

    public float getDeathProgress() {
        return 1.0f - (deathTimer / DEATH_ANIMATION_DURATION); // 0.0 to 1.0
    }

    private boolean tryMove(float deltaX, float deltaY, CollisionManager cm) {
        float newX = this.x + deltaX;
        float newY = this.y + deltaY;
        float padding = 0.1f;

        boolean canMove = cm.isWalkableForPlayer((int) (newX + padding), (int) (newY + padding), hasKey) &&
                cm.isWalkableForPlayer((int) (newX + SIZE - padding), (int) (newY + padding), hasKey) &&
                cm.isWalkableForPlayer((int) (newX + SIZE - padding), (int) (newY + SIZE - padding), hasKey) &&
                cm.isWalkableForPlayer((int) (newX + padding), (int) (newY + SIZE - padding), hasKey);

        if (canMove) {
            this.x = newX;
            this.y = newY;
            return true;
        }
        return false;
    }

    public void knockback(float sourceX, float sourceY, float strengthMultiplier) {
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
    }

    public float getKnockbackVx() {
        return knockbackVx;
    }

    public float getKnockbackVy() {
        return knockbackVy;
    }

    public void startAttackAnimation(float duration) {
        this.isAttacking = true;
        this.attackAnimTotalDuration = duration;
        this.attackAnimTimer = duration;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public float getAttackAnimTimer() {
        return attackAnimTimer;
    }

    public float getAttackAnimTotalDuration() {
        return attackAnimTotalDuration;
    }

    public boolean canAttack() {
        return attackTimer <= 0;
    }

    public void resetAttackCooldown(float cooldown) {
        this.attackTimer = cooldown;
    }

    /**
     * 相对移动：在当前坐标基础上增加 delta
     */
    public void move(float deltaX, float deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }

    /**
     * 【修复点】绝对定位：直接设置 X 坐标 (用于读档)
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * 【修复点】绝对定位：直接设置 Y 坐标 (用于读档)
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * 绝对定位：同时设置 X 和 Y
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Original damage method (backward compatible, treats as PHYSICAL).
     */
    public boolean damage(int amount) {
        return damage(amount, DamageType.PHYSICAL);
    }

    /**
     * Take damage with type consideration for armor system.
     * 
     * @param amount 伤害量
     * @param type   伤害类型
     * @return true 如果伤害被应用
     */
    public boolean damage(int amount, DamageType type) {
        // God Mode: 无敌模式下不受伤害
        if (godMode) {
            return false;
        }

        if (invincibilityTimer > 0 || isDead) {
            return false;
        }

        int remainingDamage = amount;

        // Check if armor absorbs this damage
        if (equippedArmor != null) {
            int beforeShield = equippedArmor.getCurrentShield();
            remainingDamage = equippedArmor.absorbDamage(amount, type);
            int absorbed = amount - remainingDamage;

            // === NEW: Track armor absorbed for achievements ===
            if (absorbed > 0) {
                de.tum.cit.fop.maze.utils.AchievementManager.recordArmorAbsorbed(
                        equippedArmor.getResistType().name(), absorbed);
                GameLogger.debug("Player", "Armor absorbed " + absorbed + " " + type + " damage");
            }
        }

        if (remainingDamage > 0) {
            this.lives -= remainingDamage;
            if (this.lives < 0)
                this.lives = 0;
            // Trigger blood particle effect - 随机方向（玩家受伤无明确方向）
            if (damageListener != null) {
                float angle = (float) (Math.random() * Math.PI * 2);
                damageListener.onDamage(x + 0.5f, y + 0.5f, remainingDamage,
                        (float) Math.cos(angle), (float) Math.sin(angle), 1.0f);
            }
        }

        // Trigger death animation if lives reach 0
        if (this.lives <= 0) {
            isDead = true;
            deathTimer = DEATH_ANIMATION_DURATION;
            GameLogger.info("Player", "Player died (Damage Check)! Amount: " + amount + " Remaining Lives: " + lives);
        }

        this.invincibilityTimer = GameSettings.playerInvincibilityDuration + invincibilityExtension;
        this.hurtTimer = 0.5f; // Red flash for 0.5s
        return true;
    }

    public void setDamageListener(BloodParticleSystem.DamageListener listener) {
        this.damageListener = listener;
    }

    public void restoreHealth(int amount) {
        this.lives += amount;
        if (this.lives > getMaxHealth()) {
            this.lives = getMaxHealth();
        }
    }

    public float getSpeed() {
        // Base Speed + Bonus
        float base = isRunning ? GameSettings.playerRunSpeed : GameSettings.playerWalkSpeed;
        return base + (base * speedBonus);
    }

    public void attack() {
        if (attackTimer <= 0) {
            Weapon currentWeapon = inventory.get(currentWeaponIndex);

            // Calculate cooldown with reduction
            float baseCooldown = currentWeapon.getCooldown();
            float reducedDisplay = Math.max(0.2f, baseCooldown - cooldownReduction); // Cap at 0.2s minimum

            attackTimer = reducedDisplay;
            isAttacking = true;

            // Fix: Decouple visual animation duration from weapon cooldown
            // The attack animation is fast (0.2s). Stretching it over a long cooldown (e.g.
            // 1.2s) causes "lag".
            // We set the animation to play at its native speed (0.2s) regardless of
            // cooldown.
            attackAnimTotalDuration = 0.2f;

            attackAnimTimer = attackAnimTotalDuration;

            de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("attack");
        }
    }

    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
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

    // 获取碰撞箱大小
    public float getWidth() {
        return SIZE;
    }

    public float getHeight() {
        return SIZE;
    }

    public boolean isHurt() {
        return hurtTimer > 0;
    }

    // ==================== Weapon System ====================

    /**
     * Directly add weapon to inventory (bypassing size limit).
     * Used for initial auto-equip of purchased items.
     */
    public void addWeapon(Weapon weapon) {
        inventory.add(weapon);
    }

    public boolean pickupWeapon(Weapon weapon) {
        if (inventory.size() < 4) { // Increased to 4 slots
            inventory.add(weapon);
            GameLogger.info("Player", "Picked up weapon: " + weapon.getName());
            return true;
        }
        return false; // Inventory full
    }

    public void switchToWeapon(int index) {
        if (index >= 0 && index < inventory.size()) {
            currentWeaponIndex = index;
            justSwitchedWeaponName = getCurrentWeapon().getName();
            switchNameTimer = 2.0f;
        }
    }

    public int getCurrentWeaponIndex() {
        return currentWeaponIndex;
    }

    public List<Weapon> getInventory() {
        return inventory;
    }

    public void switchWeapon() {
        if (inventory.isEmpty())
            return;
        currentWeaponIndex = (currentWeaponIndex + 1) % inventory.size();

        // Trigger UI notification
        justSwitchedWeaponName = getCurrentWeapon().getName();
        switchNameTimer = 2.0f; // Show for 2 seconds
    }

    public Weapon getCurrentWeapon() {
        if (inventory.isEmpty())
            return null;
        return inventory.get(currentWeaponIndex);
    }

    public String getJustSwitchedWeaponName() {
        return justSwitchedWeaponName;
    }

    // ==================== Skill System ====================

    public void gainSkillPoints(int amount) {
        this.skillPoints += amount;
    }

    public boolean spendSkillPoints(int amount) {
        if (this.skillPoints >= amount) {
            this.skillPoints -= amount;
            return true;
        }
        return false;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void upgradeMaxHealth(int amount) {
        this.maxHealthBonus += amount;
        this.lives += amount; // Heal/Increase current HP by the new bonus
    }

    public int getMaxHealth() {
        return GameSettings.playerMaxLives + maxHealthBonus;
    }

    public void upgradeDamageBonus(int amount) {
        this.damageBonus += amount;
    }

    public int getDamageBonus() {
        return damageBonus;
    }

    public void upgradeInvincibilityExtension(float amount) {
        this.invincibilityExtension += amount;
    }

    public float getInvincibilityExtension() {
        return invincibilityExtension;
    }

    // Getters for persistence
    public int getMaxHealthBonus() {
        return maxHealthBonus;
    }

    public void setSkillStats(int sp, int hpBonus, int dmgBonus, float invincExt, float knockbackMult,
            float cooldownRed, float speedBon) {
        this.skillPoints = sp;
        this.maxHealthBonus = hpBonus;
        this.damageBonus = dmgBonus;
        this.invincibilityExtension = invincExt;

        this.knockbackMultiplier = knockbackMult;
        this.cooldownReduction = cooldownRed;
        this.speedBonus = speedBon;
    }

    public float getKnockbackMultiplier() {
        return knockbackMultiplier;
    }

    public float getCooldownReduction() {
        return cooldownReduction;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }

    public List<String> getInventoryWeaponTypes() {
        List<String> types = new ArrayList<>();
        for (Weapon w : inventory) {
            types.add(w.getClass().getSimpleName()); // Stores "Sword", "Bow", "MagicStaff"
        }
        return types;
    }

    public void setInventoryFromTypes(List<String> types) {
        if (types == null)
            return;
        inventory.clear();
        for (String type : types) {
            Weapon w = null;
            if (type.equals("Sword"))
                w = new Sword(x, y);
            else if (type.equals("Bow"))
                w = new de.tum.cit.fop.maze.model.weapons.Bow(x, y);
            else if (type.equals("MagicStaff"))
                w = new de.tum.cit.fop.maze.model.weapons.MagicStaff(x, y);
            else if (type.equals("Crossbow"))
                w = new Crossbow(x, y);
            else if (type.equals("Wand"))
                w = new Wand(x, y);
            /* Legacy/Other types if any */

            if (w != null)
                inventory.add(w);
        }
        // Ensure at least one weapon
        if (inventory.isEmpty()) {
            inventory.add(new Sword(x, y));
        }
        currentWeaponIndex = 0;
    }

    // ==================== Armor System ====================

    public void equipArmor(Armor armor) {
        this.equippedArmor = armor;
    }

    public Armor getEquippedArmor() {
        return equippedArmor;
    }

    public boolean hasArmor() {
        return equippedArmor != null && equippedArmor.hasShield();
    }

    public float getArmorShieldPercentage() {
        if (equippedArmor == null)
            return 0f;
        return equippedArmor.getShieldPercentage();
    }

    // ==================== Economy System ====================

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    // ==================== Energy System ====================

    public float getEnergy() {
        return energy;
    }

    public float getMaxEnergy() {
        return maxEnergy;
    }

    public float getEnergyPercentage() {
        return energy / maxEnergy;
    }

    public boolean hasEnergy(float amount) {
        return energy >= amount;
    }

    public void consumeEnergy(float amount) {
        energy = Math.max(0, energy - amount);
    }

    public void restoreEnergy(float amount) {
        energy = Math.min(maxEnergy, energy + amount);
    }

    public void regenEnergy(float delta) {
        if (energy < maxEnergy) {
            energy = Math.min(maxEnergy, energy + energyRegenRate * delta);
        }
    }

    // ==================== Cheat System ====================

    public boolean isGodMode() {
        return godMode;
    }

    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    public boolean isNoClip() {
        return noClip;
    }

    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
    }

    // ==================== Movement Physics System ====================

    /**
     * Get current horizontal velocity.
     * 
     * @return velocityX in tiles/second
     */
    public float getVelocityX() {
        return velocityX;
    }

    /**
     * Get current vertical velocity.
     * 
     * @return velocityY in tiles/second
     */
    public float getVelocityY() {
        return velocityY;
    }

    /**
     * Set velocity directly (used for knockback override, wall collision, etc.)
     */
    public void setVelocity(float vx, float vy) {
        this.velocityX = vx;
        this.velocityY = vy;
    }

    /**
     * Add velocity (for recoil effects, knockback, etc.)
     */
    public void addVelocity(float dvx, float dvy) {
        this.velocityX += dvx;
        this.velocityY += dvy;
    }

    /**
     * Apply acceleration towards target velocity.
     * This is the core physics method for smooth movement.
     * 
     * @param targetVx Target horizontal velocity (based on input)
     * @param targetVy Target vertical velocity (based on input)
     * @param delta    Frame time
     */
    public void applyAcceleration(float targetVx, float targetVy, float delta) {
        // Calculate velocity difference
        float diffX = targetVx - velocityX;
        float diffY = targetVy - velocityY;

        // Determine if accelerating or decelerating
        boolean isAcceleratingX = (targetVx != 0);
        boolean isAcceleratingY = (targetVy != 0);

        float accelX = isAcceleratingX ? ACCELERATION : DECELERATION;
        float accelY = isAcceleratingY ? ACCELERATION : DECELERATION;

        // Turn boost: increase acceleration when reversing direction
        if (Math.signum(targetVx) != Math.signum(velocityX) && velocityX != 0 && targetVx != 0) {
            accelX *= TURN_BOOST;
        }
        if (Math.signum(targetVy) != Math.signum(velocityY) && velocityY != 0 && targetVy != 0) {
            accelY *= TURN_BOOST;
        }

        // Apply acceleration (clamped to velocity difference)
        float maxChangeX = accelX * delta;
        float maxChangeY = accelY * delta;

        velocityX += clamp(diffX, -maxChangeX, maxChangeX);
        velocityY += clamp(diffY, -maxChangeY, maxChangeY);

        // Snap to zero if below threshold and no input
        if (Math.abs(velocityX) < VELOCITY_THRESHOLD && targetVx == 0) {
            velocityX = 0;
        }
        if (Math.abs(velocityY) < VELOCITY_THRESHOLD && targetVy == 0) {
            velocityY = 0;
        }
    }

    /**
     * Handle wall collision with bounce/slide effect.
     * Called when movement on an axis is blocked.
     * 
     * @param axis 'x' or 'y'
     */
    public void handleWallCollision(char axis) {
        if (axis == 'x') {
            float speed = Math.abs(velocityX);
            if (speed > 3.0f) {
                // High speed: bounce back
                velocityX = -velocityX * WALL_BOUNCE;
            } else {
                // Low speed: just stop
                velocityX = 0;
            }
            // Apply wall slide to perpendicular axis
            velocityY *= WALL_SLIDE_MULT;
        } else if (axis == 'y') {
            float speed = Math.abs(velocityY);
            if (speed > 3.0f) {
                velocityY = -velocityY * WALL_BOUNCE;
            } else {
                velocityY = 0;
            }
            velocityX *= WALL_SLIDE_MULT;
        }
    }

    /**
     * Check if player is currently moving (has velocity).
     */
    public boolean isMoving() {
        return Math.abs(velocityX) > VELOCITY_THRESHOLD || Math.abs(velocityY) > VELOCITY_THRESHOLD;
    }

    /**
     * Get current speed magnitude (for animation speed, etc.)
     */
    public float getCurrentSpeedMagnitude() {
        return (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
    }

    /**
     * Reset velocity to zero (for teleport, respawn, etc.)
     */
    public void resetVelocity() {
        velocityX = 0;
        velocityY = 0;
    }

    // Physics constant getters (for debugging/console)
    public static float getAcceleration() {
        return ACCELERATION;
    }

    public static float getDeceleration() {
        return DECELERATION;
    }

    public static float getTurnBoost() {
        return TURN_BOOST;
    }

    public static float getWallBounce() {
        return WALL_BOUNCE;
    }

    // Physics constant setters (for console tuning)
    public static void setAcceleration(float val) {
        ACCELERATION = val;
    }

    public static void setDeceleration(float val) {
        DECELERATION = val;
    }

    public static void setTurnBoost(float val) {
        TURN_BOOST = val;
    }

    public static void setWallBounce(float val) {
        WALL_BOUNCE = val;
    }

    // Helper method
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== Buff System (for Treasure Chest rewards)
    // ====================

    // Buff 状态
    private float speedBuffTimer = 0f; // 速度Buff剩余时间
    private float speedBuffMultiplier = 0f; // 速度Buff倍率 (0.5 = +50%)
    private float rageBuffTimer = 0f; // 狂暴Buff剩余时间
    private boolean hasShieldBuff = false; // 是否有护盾（抵挡一次伤害）

    /**
     * 应用速度Buff
     * 
     * @param durationSeconds Buff持续时间（秒）
     */
    public void applySpeedBuff(float durationSeconds) {
        this.speedBuffTimer = durationSeconds;
        this.speedBuffMultiplier = 0.5f; // +50% 移速
        GameLogger.info("Player", "Speed buff applied for " + durationSeconds + "s");
    }

    /**
     * 应用狂暴Buff（攻击冷却减半）
     * 
     * @param durationSeconds Buff持续时间（秒）
     */
    public void applyRageBuff(float durationSeconds) {
        this.rageBuffTimer = durationSeconds;
        GameLogger.info("Player", "Rage buff applied for " + durationSeconds + "s");
    }

    /**
     * 应用护盾（抵挡下一次伤害）
     */
    public void applyShield() {
        this.hasShieldBuff = true;
        GameLogger.info("Player", "Shield buff applied");
    }

    /**
     * 设置无敌状态（用于宝箱奖励）
     * 
     * @param invincible 是否无敌
     */
    public void setInvincible(boolean invincible) {
        if (invincible) {
            // 设置一个较长的无敌时间，实际时间由 setInvincibilityTimer 控制
            this.invincibilityTimer = 999f;
        } else {
            this.invincibilityTimer = 0f;
        }
    }

    /**
     * 设置无敌计时器（秒）
     * 
     * @param seconds 无敌持续时间
     */
    public void setInvincibilityTimer(float seconds) {
        this.invincibilityTimer = seconds;
        GameLogger.info("Player", "Invincibility set for " + seconds + "s");
    }

    /**
     * 更新Buff状态（每帧调用）
     * 应在 updateTimers 中调用
     */
    public void updateBuffs(float delta) {
        if (speedBuffTimer > 0) {
            speedBuffTimer -= delta;
            if (speedBuffTimer <= 0) {
                speedBuffTimer = 0;
                speedBuffMultiplier = 0;
                GameLogger.info("Player", "Speed buff expired");
            }
        }
        if (rageBuffTimer > 0) {
            rageBuffTimer -= delta;
            if (rageBuffTimer <= 0) {
                rageBuffTimer = 0;
                GameLogger.info("Player", "Rage buff expired");
            }
        }
    }

    /**
     * 尝试消耗护盾抵挡伤害
     * 
     * @return true 如果护盾成功抵挡
     */
    public boolean consumeShieldBuff() {
        if (hasShieldBuff) {
            hasShieldBuff = false;
            GameLogger.info("Player", "Shield buff consumed - damage blocked");
            return true;
        }
        return false;
    }

    /**
     * 获取带Buff的速度（覆盖原 getSpeed 逻辑）
     */
    public float getSpeedWithBuff() {
        float baseSpeed = getSpeed();
        if (speedBuffTimer > 0) {
            return baseSpeed * (1f + speedBuffMultiplier);
        }
        return baseSpeed;
    }

    /**
     * 获取带狂暴Buff的攻击冷却倍率
     * 
     * @return 冷却倍率（正常=1.0，狂暴=0.5）
     */
    public float getRageCooldownMultiplier() {
        return rageBuffTimer > 0 ? 0.5f : 1.0f;
    }

    // Buff 状态查询
    public boolean hasSpeedBuff() {
        return speedBuffTimer > 0;
    }

    public boolean hasRageBuff() {
        return rageBuffTimer > 0;
    }

    public boolean hasShield() {
        return hasShieldBuff;
    }

    public float getSpeedBuffTimer() {
        return speedBuffTimer;
    }

    public float getRageBuffTimer() {
        return rageBuffTimer;
    }
}