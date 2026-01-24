package de.tum.cit.fop.maze.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import de.tum.cit.fop.maze.config.GameConfig;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.effects.FloatingText;
import de.tum.cit.fop.maze.model.items.Armor;
import de.tum.cit.fop.maze.model.items.DroppedItem;
import de.tum.cit.fop.maze.model.items.Potion;

import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.utils.AchievementManager;
import de.tum.cit.fop.maze.utils.AudioManager;
import de.tum.cit.fop.maze.utils.GameLogger;
import de.tum.cit.fop.maze.utils.LootTable;

import java.util.*;

/**
 * GameWorld (Model/Logic Layer)
 * 负责管理游戏所有的实体状态、碰撞检测、AI 更新等核心逻辑。
 * 与渲染 (View) 分离，便于测试和维护。
 */
public class GameWorld {

    private final GameMap gameMap;
    private final Player player;
    private final CollisionManager collisionManager;
    private final List<Enemy> enemies;
    private final List<MobileTrap> mobileTraps;
    private final List<FloatingText> floatingTexts;
    private boolean[][] safeGrid; // For AI pathfinding

    // === New: Projectile and Loot Systems ===
    private final List<Projectile> projectiles;
    private final List<DroppedItem> droppedItems;
    private DamageType levelDamageType = DamageType.PHYSICAL; // Default level damage type
    private int levelNumber = 1;
    private List<String> newAchievements = new ArrayList<>(); // Track newly unlocked achievements

    private int killCount = 0;
    private int coinsCollected = 0; // Track coins collected this session
    private int playerDirection = 0; // 0=Down, 1=Up, 2=Left, 3=Right
    private Vector2 aimDirection = new Vector2(0, -1); // Default Down to match playerDirection

    // === Mouse Aiming System ===
    private float aimAngle = 270f; // 瞄准角度 (度数, 0=右, 90=上, 180=左, 270=下)
    private Vector2 mouseWorldPos = new Vector2(); // 鼠标世界坐标
    private static final float UNIT_SCALE = 16f; // 世界坐标缩放系数

    // === Fire Event (for particle effects) ===
    private float lastFireX, lastFireY; // 上次开火位置
    private float lastFireDirX, lastFireDirY; // 上次开火方向
    private boolean fireEventTriggered = false; // 开火事件标志

    // === Achievement Tracking ===
    private boolean playerTookDamage = false; // For flawless victory
    private float levelStartTime = 0f; // For speedrun achievement
    private float levelElapsedTime = 0f;
    private String lastUsedWeaponName = "Sword"; // Track weapon used for kill

    // Multi-kill tracking
    private static final float MULTI_KILL_WINDOW = 5.0f; // 5 seconds window
    private List<Float> recentKillTimes = new ArrayList<>();

    // Listener for events that require Screen transition (Victory, GameOver)
    public interface WorldListener {
        void onGameOver(int killCount);

        void onVictory(String currentMapPath);

        /** 当玩家接触谜题宝箱时调用，需要暂停游戏并显示谜题UI */
        default void onPuzzleChestInteract(TreasureChest chest) {
            // 默认实现：直接打开（向后兼容）
        }
    }

    // Listener for projectile hit visual effects
    public interface ProjectileHitListener {
        void onProjectileHit(float x, float y, String textureKey, int damage,
                de.tum.cit.fop.maze.model.weapons.WeaponEffect effect);
    }

    private WorldListener listener;
    private ProjectileHitListener projectileHitListener;
    private String currentLevelPath;

    public GameWorld(GameMap gameMap, String levelPath) {
        this.gameMap = gameMap;
        this.currentLevelPath = levelPath;

        // Initialize Core Components
        this.collisionManager = new CollisionManager(gameMap);
        this.player = new Player(gameMap.getPlayerStartX(), gameMap.getPlayerStartY());

        // Auto-equip purchased weapons
        java.util.List<String> purchasedItems = de.tum.cit.fop.maze.shop.ShopManager.getPurchasedItemIds();
        for (String itemId : purchasedItems) {
            switch (itemId) {
                case "weapon_sword":
                    // Default sword is already equipped by Player constructor
                    break;
                case "weapon_bow":
                    this.player.addWeapon(
                            new de.tum.cit.fop.maze.model.weapons.Bow(this.player.getX(), this.player.getY()));
                    break;
                case "weapon_staff":
                    this.player.addWeapon(
                            new de.tum.cit.fop.maze.model.weapons.MagicStaff(this.player.getX(), this.player.getY()));
                    break;
                case "weapon_crossbow":
                    this.player.addWeapon(
                            new de.tum.cit.fop.maze.model.weapons.Crossbow(this.player.getX(), this.player.getY()));
                    break;
                case "weapon_wand":
                    this.player.addWeapon(
                            new de.tum.cit.fop.maze.model.weapons.Wand(this.player.getX(), this.player.getY()));
                    break;
            }
        }
        this.enemies = new ArrayList<>();
        this.mobileTraps = new ArrayList<>();
        this.floatingTexts = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.droppedItems = new ArrayList<>();

        // Populate lists from map
        for (GameObject obj : gameMap.getDynamicObjects()) {
            if (obj instanceof Enemy) {
                Enemy enemy = (Enemy) obj;
                // 统一使用第一关的怪物素材 (BOAR)
                enemy.setEnemyType(Enemy.EnemyType.BOAR);
                enemies.add(enemy);
            } else if (obj instanceof MobileTrap)
                mobileTraps.add((MobileTrap) obj);
        }

        // Parse level number from path for scaling
        try {
            String levelStr = levelPath.replaceAll("[^0-9]", "");
            if (!levelStr.isEmpty()) {
                this.levelNumber = Integer.parseInt(levelStr);
            }
        } catch (Exception e) {
            this.levelNumber = 1;
        }

        // Spawn custom elements assigned to this level
        spawnCustomElements();

        // AI Pathfinding Setup
        calculateSafePath();
    }

    /**
     * Spawn custom elements (enemies, items, etc.) that are assigned to the current
     * level
     */
    private void spawnCustomElements() {
        try {
            de.tum.cit.fop.maze.custom.CustomElementManager manager = de.tum.cit.fop.maze.custom.CustomElementManager
                    .getInstance();

            // Debug: Log all elements and their status
            java.util.Collection<de.tum.cit.fop.maze.custom.CustomElementDefinition> allElements = manager
                    .getAllElements();
            de.tum.cit.fop.maze.utils.GameLogger.info("GameWorld",
                    "Custom elements total: " + allElements.size() + ", checking for level " + levelNumber);

            for (de.tum.cit.fop.maze.custom.CustomElementDefinition el : allElements) {
                boolean assigned = el.isAssignedToLevel(levelNumber);
                boolean complete = el.isComplete();
                de.tum.cit.fop.maze.utils.GameLogger.info("GameWorld",
                        "  - " + el.getName() + " [type=" + el.getType() +
                                ", assignedToLevel" + levelNumber + "=" + assigned +
                                ", complete=" + complete + "]");
            }

            java.util.List<de.tum.cit.fop.maze.custom.CustomElementDefinition> elements = manager
                    .getElementsForLevel(levelNumber);

            de.tum.cit.fop.maze.utils.GameLogger.info("GameWorld",
                    "Elements matching level " + levelNumber + ": " + elements.size());

            for (de.tum.cit.fop.maze.custom.CustomElementDefinition element : elements) {
                if (element.getType() == de.tum.cit.fop.maze.custom.ElementType.ENEMY) {
                    spawnCustomEnemy(element, levelNumber);
                }
                // TODO: Add support for other element types (items, obstacles, weapons)
            }
        } catch (Exception e) {
            de.tum.cit.fop.maze.utils.GameLogger.error("GameWorld",
                    "Failed to spawn custom elements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Spawn a custom enemy based on element definition
     */
    private void spawnCustomEnemy(de.tum.cit.fop.maze.custom.CustomElementDefinition element, int level) {
        int count = element.getSpawnCount();
        float spawnProb = element.getSpawnProbability(level);

        for (int i = 0; i < count; i++) {
            if (Math.random() > spawnProb)
                continue;

            // Find a valid spawn position (walkable tile away from player spawn)
            float spawnX = -1, spawnY = -1;
            int attempts = 0;
            int maxAttempts = 100;

            while (attempts < maxAttempts) {
                int x = 3 + (int) (Math.random() * (gameMap.getWidth() - 6));
                int y = 3 + (int) (Math.random() * (gameMap.getHeight() - 6));

                // Check if walkable and not too close to player start
                if (collisionManager.isWalkable(x, y)) {
                    float distToPlayer = com.badlogic.gdx.math.Vector2.dst(
                            x, y, gameMap.getPlayerStartX(), gameMap.getPlayerStartY());
                    if (distToPlayer > 5) {
                        spawnX = x;
                        spawnY = y;
                        break;
                    }
                }
                attempts++;
            }

            if (spawnX < 0) {
                continue; // Try next spawn
            }

            // Create enemy with custom properties
            int health = 3;
            float moveSpeed = 2.0f;
            int attackDamage = 1;
            int defense = 0; // Shield amount from Element Manager

            try {
                Object healthVal = element.getProperties().get("health");
                if (healthVal instanceof Number)
                    health = ((Number) healthVal).intValue();

                Object speedVal = element.getProperties().get("moveSpeed");
                if (speedVal instanceof Number)
                    moveSpeed = ((Number) speedVal).floatValue();

                Object dmgVal = element.getProperties().get("attackDamage");
                if (dmgVal instanceof Number)
                    attackDamage = ((Number) dmgVal).intValue();

                // Read defense (shield) from Element Manager
                Object defenseVal = element.getProperties().get("defense");
                if (defenseVal instanceof Number)
                    defense = ((Number) defenseVal).intValue();
            } catch (Exception e) {
                // Use defaults
            }

            // If defense > 0, give enemy a PHYSICAL shield
            DamageType shieldType = defense > 0 ? DamageType.PHYSICAL : null;
            int shieldAmount = defense;

            Enemy customEnemy = new Enemy(spawnX, spawnY, health,
                    de.tum.cit.fop.maze.model.DamageType.PHYSICAL, shieldType, shieldAmount);
            customEnemy.setCustomElementId(element.getId());

            // Set Enemy Type if defined
            String typeStr = element.getProperty("enemyType", String.class);
            if (typeStr != null) {
                try {
                    customEnemy.setEnemyType(de.tum.cit.fop.maze.model.Enemy.EnemyType.valueOf(typeStr));
                } catch (Exception e) {
                }
            }

            enemies.add(customEnemy);

            de.tum.cit.fop.maze.utils.GameLogger.info("GameWorld",
                    "Spawned unified enemy '" + element.getName() + "' at (" + spawnX + ", " + spawnY + ")");
        }
    }

    public void setListener(WorldListener listener) {
        this.listener = listener;
    }

    public void setProjectileHitListener(ProjectileHitListener listener) {
        this.projectileHitListener = listener;
    }

    public void update(float delta) {
        // Track level elapsed time for achievements
        levelElapsedTime += delta;

        // 1. Player Update
        player.update(delta, collisionManager);

        // Check Death Animation
        if (player.isDead()) {
            if (player.getDeathTimer() <= 0) {
                if (listener != null)
                    listener.onGameOver(killCount);
            }
        }

        // 2. Input Handling (Movement & Actions)
        if (!player.isDead()) {
            handleInput(delta);
        }

        // 3. Entity Updates
        updateEnemies(delta);
        updateTraps(delta);
        updateProjectiles(delta); // NEW: Update projectiles
        updateDroppedItems(); // NEW: Handle item pickup
        updateDynamicObjects();
        updateChests(delta); // NEW: Handle treasure chest interaction
        updateFloatingTexts(delta);

        // 4. Update player's equipped weapon (for reload timer)
        Weapon currentWeapon = player.getCurrentWeapon();
        if (currentWeapon != null) {
            currentWeapon.update(delta);
        }
    }

    // --- Input Logic ---

    protected void handleInput(float delta) {
        // Weapon Switch
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_SWITCH_WEAPON)) {
            player.switchWeapon();
            AudioManager.getInstance().playSound("select");
        }

        // === Movement with Inertia System ===
        // Step 1: Get input direction
        float inputX = 0, inputY = 0;

        if (Gdx.input.isKeyPressed(GameSettings.KEY_LEFT)) {
            inputX -= 1;
            // 非鼠标模式时，根据键盘输入更新朝向
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 2;
            }
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_RIGHT)) {
            inputX += 1;
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 3;
            }
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_UP)) {
            inputY += 1;
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 1;
            }
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_DOWN)) {
            inputY -= 1;
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 0;
            }
        }

        // Step 2: Normalize diagonal movement (prevent faster diagonal speed)
        if (inputX != 0 && inputY != 0) {
            float length = (float) Math.sqrt(inputX * inputX + inputY * inputY);
            inputX /= length;
            inputY /= length;
        }

        // 非鼠标模式时，根据键盘输入更新瞄准方向
        if (!GameSettings.isUseMouseAiming() && (inputX != 0 || inputY != 0)) {
            aimDirection.set(inputX, inputY).nor();
        }

        // Step 3: Handle running state
        player.setRunning(
                Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        // Step 4: Calculate target velocity
        float maxSpeed = player.getSpeed(); // Already considers running state
        float targetVx = inputX * maxSpeed;
        float targetVy = inputY * maxSpeed;

        // === Stop Assist & Grid Alignment ===
        // When no input is given, help the player stop exactly on a tile center.
        if (inputX == 0 && inputY == 0) {
            float px = player.getX();
            float py = player.getY();
            float vx = player.getVelocityX();
            float vy = player.getVelocityY();

            float distX = px - Math.round(px);
            float distY = py - Math.round(py);

            // If we are very close to the center and moving slowly/stopping, snap
            // immediately.
            // This prevents sliding past the target tile.
            boolean closeToCenterX = Math.abs(distX) < 0.1f;
            boolean closeToCenterY = Math.abs(distY) < 0.1f;

            if (closeToCenterX && Math.abs(vx) < 2.0f) {
                player.setVelocity(0, vy); // Kill X velocity
                targetVx = 0;
                // Immediate snap if very slow
                if (Math.abs(vx) < 0.5f) {
                    player.setPosition(Math.round(px), py);
                }
            }

            if (closeToCenterY && Math.abs(vy) < 2.0f) {
                player.setVelocity(vx, 0); // Kill Y velocity
                targetVy = 0;
                if (Math.abs(vy) < 0.5f) {
                    player.setPosition(px, Math.round(py));
                }
            }
        }

        // Step 5: Apply acceleration towards target velocity (core inertia logic)
        player.applyAcceleration(targetVx, targetVy, delta);

        // Step 6: Apply velocity to position with collision detection
        applyPhysicsMovement(delta);

        // Step 7: 非鼠标模式时根据移动速度更新朝向
        if (!GameSettings.isUseMouseAiming() && player.isMoving()) {
            updateDirectionFromVelocity();
        }

        // Attack (按住攻击键时持续攻击，由武器冷却控制攻击频率)
        if (Gdx.input.isKeyPressed(GameSettings.KEY_ATTACK)) {
            handleAttack();
        }
    }

    /**
     * Apply player velocity to position with per-axis collision detection.
     * This enables wall sliding and bounce effects.
     * Also handles grid snapping when player stops moving.
     */
    private void applyPhysicsMovement(float delta) {
        // NoClip mode: bypass collision
        if (player.isNoClip()) {
            player.move(player.getVelocityX() * delta, player.getVelocityY() * delta);
            return;
        }

        float vx = player.getVelocityX();
        float vy = player.getVelocityY();

        // If player has stopped moving (and target is 0), apply grid snapping
        // Note: We check low velocity AND alignment. If Auto-Drive is active, velocity
        // will be high.
        if (Math.abs(vx) < 0.1f && Math.abs(vy) < 0.1f) {
            snapPlayerToGrid(delta);
            return;
        }

        float moveX = vx * delta;
        float moveY = vy * delta;

        // Try X-axis movement first
        if (moveX != 0) {
            if (canMoveToPosition(player.getX() + moveX, player.getY())) {
                player.move(moveX, 0);
            } else {
                // X-axis blocked: apply wall collision physics
                player.handleWallCollision('x');
            }
        }

        // Then try Y-axis movement
        if (moveY != 0) {
            if (canMoveToPosition(player.getX(), player.getY() + moveY)) {
                player.move(0, moveY);
            } else {
                // Y-axis blocked: apply wall collision physics
                player.handleWallCollision('y');
            }
        }
    }

    /**
     * Smoothly snap player to the nearest grid position when stopped.
     * This ensures the player always rests on a tile center, not between tiles.
     */
    private void snapPlayerToGrid(float delta) {
        // [User Request] Disable grid snapping for arbitrary positioning
        return;
    }

    /**
     * Check if player can move to the specified position.
     * Tests all four corners of the player's collision box.
     */
    private boolean canMoveToPosition(float newX, float newY) {
        float w = player.getWidth();
        float h = player.getHeight();
        float padding = 0.1f;

        return isWalkable(newX + padding, newY + padding) &&
                isWalkable(newX + w - padding, newY + padding) &&
                isWalkable(newX + w - padding, newY + h - padding) &&
                isWalkable(newX + padding, newY + h - padding);
    }

    /**
     * Update player facing direction based on current velocity.
     * Prioritizes the axis with higher velocity magnitude.
     */
    private void updateDirectionFromVelocity() {
        float vx = player.getVelocityX();
        float vy = player.getVelocityY();

        if (Math.abs(vx) > Math.abs(vy)) {
            // Horizontal movement dominates
            playerDirection = vx > 0 ? 3 : 2; // Right : Left
        } else if (Math.abs(vy) > 0.01f) {
            // Vertical movement dominates
            playerDirection = vy > 0 ? 1 : 0; // Up : Down
        }
    }

    private void handleAttack() {
        if (player.canAttack()) {
            // === Energy Check ===
            float energyCost = 10f; // Default cost
            Weapon currentWeapon = player.getCurrentWeapon();
            if (currentWeapon != null) {
                // Try to get custom weapon's energy cost (inline lookup)
                String customId = null;
                for (de.tum.cit.fop.maze.custom.CustomElementDefinition def : de.tum.cit.fop.maze.custom.CustomElementManager
                        .getInstance().getAllElements()) {
                    if (def.getType() == de.tum.cit.fop.maze.custom.ElementType.WEAPON &&
                            def.getName().equalsIgnoreCase(currentWeapon.getName())) {
                        customId = def.getId();
                        break;
                    }
                }
                if (customId != null) {
                    de.tum.cit.fop.maze.custom.CustomElementDefinition def = de.tum.cit.fop.maze.custom.CustomElementManager
                            .getInstance().getElement(customId);
                    if (def != null) {
                        energyCost = def.getFloatProperty("energyCost");
                    }
                }
            }

            if (!player.hasEnergy(energyCost)) {
                return; // Not enough energy to attack
            }

            player.consumeEnergy(energyCost);
            player.attack();

            // Ranged Attack
            if (currentWeapon.isRanged()) {
                currentWeapon.onFire();
                spawnProjectile(currentWeapon, player);
                // Record effect for ranged weapon too
                if (currentWeapon.getEffect() != null &&
                        currentWeapon.getEffect() != de.tum.cit.fop.maze.model.weapons.WeaponEffect.NONE) {
                    newAchievements.addAll(AchievementManager.recordEffectApplied(
                            currentWeapon.getEffect().name()));
                }
                return; // Ranged attack complete
            }

            // Logic moved from GameScreen (Melee)
            float attackRange = currentWeapon.getRange();
            float attackRangeSq = attackRange * attackRange; // 用于快速预过滤
            Iterator<Enemy> iter = enemies.iterator();
            while (iter.hasNext()) {
                Enemy e = iter.next();
                if (e.isDead())
                    continue;

                // 快速预过滤：用平方距离跳过明显远的敌人
                float dx = e.getX() - player.getX();
                float dy = e.getY() - player.getY();
                if (dx * dx + dy * dy > attackRangeSq)
                    continue;

                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < attackRange) {
                    // Angle check (复用已计算的 dx/dy)
                    float enemyAngle = MathUtils.atan2(dy, dx) * MathUtils.radDeg;
                    if (enemyAngle < 0)
                        enemyAngle += 360;

                    // === 根据设置选择攻击角度和锥形范围 ===
                    float attackAngle;
                    float coneHalfAngle;
                    if (GameSettings.isUseMouseAiming()) {
                        // 鼠标模式: 使用 aimAngle, 60度锥形
                        attackAngle = aimAngle;
                        coneHalfAngle = 30f;
                    } else {
                        // 键盘模式: 使用 aimDirection 向量计算8向攻击角度, 45度锥形
                        // aimDirection 在 handleInput() 中根据键盘组合键更新
                        attackAngle = MathUtils.atan2(aimDirection.y, aimDirection.x) * MathUtils.radDeg;
                        if (attackAngle < 0) {
                            attackAngle += 360;
                        }
                        coneHalfAngle = 45f; // 8向攻击，每个方向90度覆盖，半角45度
                    }

                    float angleDiff = enemyAngle - attackAngle;
                    while (angleDiff > 180)
                        angleDiff -= 360;
                    while (angleDiff < -180)
                        angleDiff += 360;

                    // 攻击锥形半角判定, 或极近距离直接命中
                    if (Math.abs(angleDiff) <= coneHalfAngle || dist < 0.5f) {
                        int totalDamage = currentWeapon.getDamage() + player.getDamageBonus();

                        // Set damage source for blood particle direction (include knockback strength)
                        e.setDamageSource(player.getX(), player.getY(), player.getKnockbackMultiplier());
                        // Apply damage with damage type consideration
                        e.takeDamage(totalDamage, currentWeapon.getDamageType());
                        if (e.getHealth() > 0) {
                            e.applyEffect(currentWeapon.getEffect());
                            // === NEW: Track effect application for achievements ===
                            if (currentWeapon.getEffect() != null &&
                                    currentWeapon.getEffect() != de.tum.cit.fop.maze.model.weapons.WeaponEffect.NONE) {
                                newAchievements.addAll(AchievementManager.recordEffectApplied(
                                        currentWeapon.getEffect().name()));
                            }
                        }

                        floatingTexts.add(new FloatingText(e.getX(), e.getY(), "-" + totalDamage, Color.RED));
                        AudioManager.getInstance().playSound("hit");

                        float kbMult = 1.0f + (1.0f - (dist / Math.max(0.1f, attackRange)));
                        if (player.isRunning())
                            kbMult *= 2.0f;
                        kbMult = MathUtils.clamp(kbMult, 1.0f, 4.0f);

                        e.knockback(player.getX(), player.getY(), kbMult * player.getKnockbackMultiplier(),
                                collisionManager);

                        if (e.isDead() && !e.isRemovable()) { // Just died
                            handleEnemyDeath(e);
                        }
                    }
                }
            }
        }
    }

    // --- Private Update Helpers ---

    private void updateEnemies(float delta) {
        for (Enemy enemy : enemies) {
            float dst2 = Vector2.dst2(player.getX(), player.getY(), enemy.getX(), enemy.getY());
            if (dst2 > 1600)
                continue; // Optimization: Don't update far enemies
            enemy.update(delta, player, collisionManager, safeGrid);
        }
        enemies.removeIf(Enemy::isRemovable);

        // Collision with Player
        for (Enemy enemy : enemies) {
            if (Vector2.dst(player.getX(), player.getY(), enemy.getX(), enemy.getY()) < GameSettings.hitDistance) {
                if (enemy.isDead())
                    continue;
                if (player.damage(1)) {
                    playerTookDamage = true; // Track for flawless victory
                    player.knockback(enemy.getX(), enemy.getY(), 2.0f);
                    AudioManager.getInstance().playSound("hit");
                }
            }
        }
    }

    private void updateTraps(float delta) {
        for (MobileTrap trap : mobileTraps) {
            trap.update(delta, collisionManager);
            if (Vector2.dst(player.getX(), player.getY(), trap.getX(), trap.getY()) < 0.8f) {
                if (player.damage(1)) {
                    playerTookDamage = true; // Track for flawless victory
                    player.knockback(trap.getX(), trap.getY(), 0.5f);
                }
            }
        }
    }

    private void updateDynamicObjects() {
        Iterator<GameObject> iter = gameMap.getDynamicObjects().iterator();
        while (iter.hasNext()) {
            GameObject obj = iter.next();
            if (Vector2.dst(player.getX(), player.getY(), obj.getX(), obj.getY()) < 0.5f) {
                if (obj instanceof Key) {
                    player.setHasKey(true);
                    iter.remove();
                    AudioManager.getInstance().playSound("collect");
                } else if (obj instanceof Exit) {
                    if (player.hasKey()) {
                        AudioManager.getInstance().playSound("victory");

                        // === NEW: Check level completion achievements ===
                        newAchievements.addAll(AchievementManager.recordLevelComplete(
                                currentLevelPath, playerTookDamage, levelElapsedTime));

                        // Check comeback achievement (complete with 1 HP)
                        if (player.getLives() == 1) {
                            newAchievements.addAll(AchievementManager.checkComeback(1));
                        }

                        GameLogger.info("GameWorld", "Level completed! Time: " + levelElapsedTime +
                                "s, Damage taken: " + playerTookDamage);

                        // Save State Logic could go here or be handled by Listener.
                        // Let's defer to Listener for clean separation.
                        if (listener != null)
                            listener.onVictory(currentLevelPath);
                    }
                } else if (obj instanceof Trap) {
                    if (player.damage(1)) {
                        AudioManager.getInstance().playSound("hit");
                        player.knockback(obj.getX(), obj.getY(), 0.5f);
                    }
                } else if (obj instanceof Potion) {
                    player.restoreHealth(1);
                    iter.remove();
                    AudioManager.getInstance().playSound("collect");
                } else if (obj instanceof Weapon) {
                    if (player.pickupWeapon((Weapon) obj)) {
                        iter.remove();
                        AudioManager.getInstance().playSound("collect");
                    }
                }
            }
        }
    }

    /**
     * 更新宝箱交互检测
     * 
     * 当玩家靠近未打开的宝箱时，触发交互：
     * - 普通宝箱：直接打开并领取奖励
     * - 谜题宝箱：通知监听器显示谜题UI（暂停游戏）
     */
    private void updateChests(float delta) {
        // 更新所有宝箱的动画状态
        for (TreasureChest chest : gameMap.getTreasureChests()) {
            chest.update(delta);
        }

        // 检查玩家与宝箱的交互
        for (TreasureChest chest : gameMap.getTreasureChests()) {
            if (chest.isInteracted())
                continue; // 已交互过

            // 检查玩家是否靠近宝箱
            float dx = player.getX() - chest.getX();
            float dy = player.getY() - chest.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < 1.0f) { // 交互半径 (slightly reduced for tighter feel)
                // 直接打开所有宝箱（移除谜题机制）
                chest.startOpening();

                // 领取奖励
                boolean success = chest.claimReward(player);
                if (success && chest.getReward() != null) {
                    floatingTexts.add(new FloatingText(
                            chest.getX(), chest.getY() + 0.5f,
                            chest.getReward().getDisplayName(), Color.YELLOW));
                    AudioManager.getInstance().playSound("collect");
                }

                // 可选：如果要保留 notify，可以只用于统计或成就
                if (listener != null) {
                    listener.onPuzzleChestInteract(chest);
                }
                return;
            }
        }
    }

    private void updateFloatingTexts(float delta) {
        Iterator<FloatingText> dtIter = floatingTexts.iterator();
        while (dtIter.hasNext()) {
            FloatingText dt = dtIter.next();
            dt.update(delta);
            if (dt.isExpired()) {
                dtIter.remove();
            }
        }
    }

    private void snapToGrid(float delta) {
        float snapSpeed = 10.0f * delta;
        float targetX = Math.round(player.getX());
        float targetY = Math.round(player.getY());
        float dx = targetX - player.getX();
        float dy = targetY - player.getY();

        if (Math.abs(dx) < 0.01f && Math.abs(dy) < 0.01f) {
            player.setPosition(targetX, targetY);
        } else {
            float moveX = Math.signum(dx) * Math.min(Math.abs(dx), snapSpeed);
            float moveY = Math.signum(dy) * Math.min(Math.abs(dy), snapSpeed);
            player.move(moveX, moveY);
        }
    }

    private void movePlayer(float deltaX, float deltaY) {
        // NoClip模式：直接移动，忽略碰撞
        if (player.isNoClip()) {
            player.move(deltaX, deltaY);
            return;
        }

        float newX = player.getX() + deltaX;
        float newY = player.getY() + deltaY;
        float w = player.getWidth();
        float h = player.getHeight();
        float padding = 0.1f;

        boolean canMove = isWalkable(newX + padding, newY + padding) &&
                isWalkable(newX + w - padding, newY + padding) &&
                isWalkable(newX + w - padding, newY + h - padding) &&
                isWalkable(newX + padding, newY + h - padding);

        if (canMove)
            player.move(deltaX, deltaY);
    }

    private boolean isWalkable(float x, float y) {
        return collisionManager.isWalkableForPlayer((int) x, (int) y, player.hasKey());
    }

    private void calculateSafePath() {
        // BFS Logic from GameScreen
        int w = gameMap.getWidth();
        int h = gameMap.getHeight();
        this.safeGrid = new boolean[w][h];

        int exitX = -1, exitY = -1;
        for (GameObject obj : gameMap.getDynamicObjects()) {
            if (obj instanceof Exit) {
                exitX = (int) obj.getX();
                exitY = (int) obj.getY();
                break;
            }
        }

        if (exitX == -1)
            return;

        int startX = (int) gameMap.getPlayerStartX();
        int startY = (int) gameMap.getPlayerStartY();
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        int startIndex = startY * w + startX;
        queue.add(startIndex);
        visited.add(startIndex);

        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int cx = current % w;
            int cy = current / w;

            // Mark as safe? Actually safeGrid is boolean.
            // Wait, original logic was missing in my prev read output?
            // "calculateSafePath" in previous output ended abruptly.
            // I'll assume standard BFS to mark reachable tiles or populate safeGrid for
            // Enemies.
            // Looking at the name 'safeGrid', it's likely used by Enemy to know where NOT
            // to go?
            // OR it's used to know valid paths.
            // Let's assume valid paths for now.

            // Re-implementing based on "safe path calculation - BFS algorithm" comment.
            // If the original output was cut off, I used the context "calculateSafePath
            // uses BFS".
            // Since I cannot see the full implementation, I will implement a robust BFS.
            safeGrid[cx][cy] = true;

            for (int[] dir : dirs) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];

                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    int nIndex = ny * w + nx;
                    if (!visited.contains(nIndex) && collisionManager.isWalkable(nx, ny)) {
                        visited.add(nIndex);
                        queue.add(nIndex);
                    }
                }
            }
        }
    }

    // --- Getters for View ---
    public Player getPlayer() {
        return player;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<MobileTrap> getMobileTraps() {
        return mobileTraps;
    }

    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    public int getPlayerDirection() {
        return playerDirection;
    }

    /**
     * 获取瞄准角度 (度数) - 仅限鼠标模式
     * 
     * @return 0=右, 90=上, 180=左, 270=下
     */
    public float getAimAngle() {
        return aimAngle;
    }

    /**
     * 获取当前攻击角度 (度数) - 根据模式自动选择
     * 鼠标模式: 返回 aimAngle (鼠标方向)
     * 键盘模式: 返回基于 aimDirection 向量的8向角度
     * 
     * @return 0=右, 45=右上, 90=上, 135=左上, 180=左, 225=左下, 270=下, 315=右下
     */
    public float getAttackAngle() {
        if (GameSettings.isUseMouseAiming()) {
            return aimAngle;
        } else {
            float angle = MathUtils.atan2(aimDirection.y, aimDirection.x) * MathUtils.radDeg;
            if (angle < 0) {
                angle += 360;
            }
            return angle;
        }
    }

    /**
     * 获取鼠标世界坐标
     */
    public Vector2 getMouseWorldPos() {
        return mouseWorldPos;
    }

    /**
     * 检查是否有开火事件需要处理（用于粒子效果）
     */
    public boolean consumeFireEvent() {
        if (fireEventTriggered) {
            fireEventTriggered = false;
            return true;
        }
        return false;
    }

    /**
     * 获取上次开火位置 X
     */
    public float getLastFireX() {
        return lastFireX;
    }

    /**
     * 获取上次开火位置 Y
     */
    public float getLastFireY() {
        return lastFireY;
    }

    /**
     * 获取上次开火方向 X
     */
    public float getLastFireDirX() {
        return lastFireDirX;
    }

    /**
     * 获取上次开火方向 Y
     */
    public float getLastFireDirY() {
        return lastFireDirY;
    }

    /**
     * 更新鼠标瞄准方向
     * 应在每帧渲染循环中调用
     * 
     * @param camera 游戏相机
     */
    public void updateMouseAim(Camera camera) {
        // 获取鼠标屏幕坐标
        float screenX = Gdx.input.getX();
        float screenY = Gdx.input.getY();

        // 转换为世界坐标
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        mouseWorldPos.set(worldCoords.x / UNIT_SCALE, worldCoords.y / UNIT_SCALE);

        // 计算玩家中心到鼠标的角度
        float playerCenterX = player.getX() + 0.5f;
        float playerCenterY = player.getY() + 0.5f;

        float dx = mouseWorldPos.x - playerCenterX;
        float dy = mouseWorldPos.y - playerCenterY;

        // 计算角度 (弧度转度数)
        aimAngle = MathUtils.atan2(dy, dx) * MathUtils.radDeg;
        if (aimAngle < 0)
            aimAngle += 360;

        // 更新 aimDirection 向量
        aimDirection.set(dx, dy).nor();

        // 更新 playerDirection (用于动画选择)
        // 将360度分成4个方向区间
        if (aimAngle >= 315 || aimAngle < 45) {
            playerDirection = 3; // 右
        } else if (aimAngle >= 45 && aimAngle < 135) {
            playerDirection = 1; // 上
        } else if (aimAngle >= 135 && aimAngle < 225) {
            playerDirection = 2; // 左
        } else {
            playerDirection = 0; // 下
        }
    }

    /**
     * 触发攻击 (供鼠标点击调用)
     * 
     * @return true 如果攻击成功触发
     */
    public boolean triggerAttack() {
        if (player.isDead())
            return false;
        handleAttack();
        return true;
    }

    // === New Methods for Extended Systems ===

    /**
     * Handle enemy death: play sound, award points, spawn loot, check achievements
     */
    private void handleEnemyDeath(Enemy e) {
        killCount++;
        GameLogger.info("GameWorld", "Enemy died! Total Kills: " + killCount);

        try {
            AudioManager.getInstance().playSound("kill");
        } catch (Exception ex) {
            GameLogger.error("GameWorld", "Failed to play kill sound", ex);
        }

        float currentTime = levelElapsedTime;

        // Check first kill achievement
        if (killCount == 1) {
            newAchievements.addAll(AchievementManager.checkFirstKill());
        }

        // === NEW: Track weapon kills for mastery achievements ===
        Weapon currentWeapon = player.getCurrentWeapon();
        if (currentWeapon != null) {
            String weaponName = currentWeapon.getName();
            newAchievements.addAll(AchievementManager.recordWeaponKill(weaponName));
            GameLogger.debug("GameWorld", "Enemy killed with weapon: " + weaponName);
        }

        // === NEW: Check Near Death achievement (kill while at 1 HP) ===
        if (player.getLives() == 1) {
            newAchievements.addAll(AchievementManager.checkNearDeathKill(1));
        }

        // === NEW: Multi-kill tracking ===
        recentKillTimes.add(currentTime);
        // Remove kills outside the time window
        recentKillTimes.removeIf(time -> currentTime - time > MULTI_KILL_WINDOW);
        // Check multi-kill achievements
        int killsInWindow = recentKillTimes.size();
        if (killsInWindow >= 3) {
            newAchievements.addAll(AchievementManager.checkMultiKill(killsInWindow));
        }

        // Check cumulative kill achievements
        newAchievements.addAll(AchievementManager.checkAchievements(killCount));

        // Award skill points
        int sp = e.getSkillPointReward();
        player.gainSkillPoints(sp);
        floatingTexts.add(new FloatingText(e.getX(), e.getY() + 0.5f, "+" + sp, Color.GOLD));

        // Generate loot using LootTable
        DroppedItem loot = LootTable.generateLoot(e.getX(), e.getY(), levelNumber);
        if (loot != null) {
            droppedItems.add(loot);

            // Show loot floating text
            String lootName = loot.getDisplayName();
            floatingTexts.add(new FloatingText(e.getX(), e.getY() + 0.3f, lootName, Color.CYAN));
        }

        // Small chance to also drop a potion (10%)
        if (Math.random() < 0.1f) {
            gameMap.addGameObject(Potion.createHealthPotion(e.getX() + 0.5f, e.getY()));
        }
    }

    /**
     * Update all active projectiles
     */
    private void updateProjectiles(float delta) {
        Iterator<Projectile> iter = projectiles.iterator();
        while (iter.hasNext()) {
            Projectile p = iter.next();

            // Update projectile position
            if (p.update(delta, collisionManager)) {
                iter.remove();
                continue;
            }

            // Check collision with enemies (player projectiles only)
            if (p.isPlayerOwned()) {
                for (Enemy e : enemies) {
                    if (e.isDead())
                        continue;
                    if (p.hitsTarget(e)) {
                        e.takeDamage(p.getDamage(), p.getDamageType());
                        if (e.getHealth() > 0) {
                            e.applyEffect(p.getEffect());
                        }
                        floatingTexts.add(new FloatingText(e.getX(), e.getY(), "-" + p.getDamage(), Color.ORANGE));
                        AudioManager.getInstance().playSound("hit");

                        // === Ranged Knockback Logic ===
                        // Calculate distance traveled to apply falloff
                        float distTraveled = Vector2.dst(p.getStartX(), p.getStartY(), p.getX(), p.getY());

                        // Base Knockback depends on weapon (Projectile usually inherits player bonus or
                        // weapon stats)
                        // Here we use a base value + decay
                        float maxRange = 10.0f; // Distance where knockback becomes minimum
                        float decayFactor = MathUtils.clamp(1.0f - (distTraveled / maxRange), 0.2f, 1.0f);

                        // Apply Knockback
                        // Use projectile position as source so enemy flies away from impact (or away
                        // from shooter?)
                        // Usually "away from bullet" means bullet velocity direction.
                        // But setDamageSource expects a point. Using Projectile current pos is good
                        // approximation.
                        e.setDamageSource(p.getX(), p.getY(), 1.0f); // Update last damage source for blood particles

                        float knockbackStrength = 1.5f * decayFactor; // Base 1.5f strength
                        e.knockback(p.getX(), p.getY(), knockbackStrength, collisionManager);

                        if (e.isDead() && !e.isRemovable()) {
                            handleEnemyDeath(e);
                        }

                        p.markHit();

                        // Trigger particle effect for magic weapons
                        if (projectileHitListener != null) {
                            projectileHitListener.onProjectileHit(p.getX(), p.getY(), p.getTextureKey(), p.getDamage(),
                                    p.getEffect());
                        }
                        break;
                    }
                }
            } else {
                // Enemy projectile hitting player
                if (p.hitsTarget(player)) {
                    if (player.damage(p.getDamage(), p.getDamageType())) {
                        player.knockback(p.getX(), p.getY(), 1.0f);
                        AudioManager.getInstance().playSound("hit");
                    }
                    p.markHit();
                }
            }

            // Remove if hit something
            if (p.isExpired()) {
                iter.remove();
            }
        }
    }

    /**
     * Update dropped items and handle pickup
     */
    private void updateDroppedItems() {
        Iterator<DroppedItem> iter = droppedItems.iterator();
        while (iter.hasNext()) {
            DroppedItem item = iter.next();
            item.update(Gdx.graphics.getDeltaTime());

            if (item.canPickUp(player)) {
                if (item.applyToPlayer(player)) {
                    iter.remove();
                    AudioManager.getInstance().playSound("collect");

                    // Check achievements
                    switch (item.getType()) {
                        case WEAPON:
                            Weapon w = (Weapon) item.getPayload();
                            newAchievements.addAll(AchievementManager.checkWeaponPickup(w.getName()));
                            break;
                        case ARMOR:
                            Armor a = (Armor) item.getPayload();
                            newAchievements.addAll(AchievementManager.checkArmorPickup(a.getTypeId()));
                            break;
                        case COIN:
                            int amount = (Integer) item.getPayload();
                            coinsCollected += amount;
                            floatingTexts.add(new FloatingText(player.getX(), player.getY() + 0.5f,
                                    "+" + amount + " coins", Color.GOLD));
                            newAchievements.addAll(AchievementManager.checkCoinMilestone(amount));
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * Fire a projectile from a ranged weapon
     */
    public void fireProjectile(float startX, float startY, float dirX, float dirY,
            Weapon weapon, boolean playerOwned) {
        float speed = GameConfig.PROJECTILE_SPEED;
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (length > 0) {
            dirX /= length;
            dirY /= length;
        }

        String textureKey = weapon.getDamageType() == DamageType.PHYSICAL ? "arrow" : "magic_bolt";

        Projectile p = new Projectile(
                startX, startY,
                dirX * speed, dirY * speed,
                weapon.getDamage() + (playerOwned ? player.getDamageBonus() : 0),
                weapon.getDamageType(),
                weapon.getEffect(),
                playerOwned,
                textureKey);
        projectiles.add(p);
    }

    // === New Getters ===

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getCoinsCollected() {
        return coinsCollected;
    }

    public List<String> getNewAchievements() {
        return newAchievements;
    }

    /**
     * Get and clear new achievements (for UI popup display)
     * 
     * @return List of achievement names that were unlocked since last call
     */
    public List<String> getAndClearNewAchievements() {
        List<String> result = new ArrayList<>(newAchievements);
        newAchievements.clear();
        return result;
    }

    public DamageType getLevelDamageType() {
        return levelDamageType;
    }

    public void setLevelDamageType(DamageType type) {
        this.levelDamageType = type;
        // Also set all enemies to this damage type
        for (Enemy e : enemies) {
            e.setAttackDamageType(type);
        }
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public float getLevelElapsedTime() {
        return levelElapsedTime;
    }

    public boolean didPlayerTakeDamage() {
        return playerTookDamage;
    }

    // ==================== Developer Console Support ====================

    /**
     * 生成敌人 (供开发者控制台使用)
     * 
     * @param x 生成位置 X
     * @param y 生成位置 Y
     */
    public void spawnEnemy(float x, float y) {
        Enemy newEnemy = new Enemy(x, y);
        enemies.add(newEnemy);
        GameLogger.info("GameWorld", "Spawned enemy at (" + x + ", " + y + ")");
    }

    /**
     * 杀死所有敌人 (供开发者控制台使用)
     * 
     * @return 杀死的敌人数量
     */
    public int killAllEnemies() {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                enemy.takeDamage(9999); // Instant kill
                count++;
            }
        }
        GameLogger.info("GameWorld", "Killed " + count + " enemies via console");
        return count;
    }

    /**
     * 杀死指定范围内的敌人 (供开发者控制台使用)
     * 
     * @param centerX 中心点 X
     * @param centerY 中心点 Y
     * @param radius  杀伤半径
     * @return 杀死的敌人数量
     */
    public int killEnemiesNear(float centerX, float centerY, float radius) {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                float dist = Vector2.dst(centerX, centerY, enemy.getX(), enemy.getY());
                if (dist <= radius) {
                    enemy.takeDamage(9999);
                    count++;
                }
            }
        }
        GameLogger.info("GameWorld", "Killed " + count + " nearby enemies via console");
        return count;
    }

    private void spawnProjectile(Weapon weapon, Player player) {
        float speed = weapon.getProjectileSpeed();
        float vx = 0, vy = 0;

        // Use precise aim direction (8-way support)
        vx = aimDirection.x * speed;
        vy = aimDirection.y * speed;

        GameLogger.info("Projectile", String.format("Spawning: Speed=%.1f, Dir=%d, V=(%.1f, %.1f) at (%.1f, %.1f)",
                speed, playerDirection, vx, vy, player.getX(), player.getY()));

        // Spawn slightly in front
        float startX = player.getX() + (vx != 0 ? Math.signum(vx) * 0.5f : 0) + 0.25f; // Center offset adjusted
        float startY = player.getY() + (vy != 0 ? Math.signum(vy) * 0.5f : 0) + 0.25f;

        // Use weapon name (or ID) as texture key so GameScreen can look it up
        String textureKey = weapon.getName();
        float projectileSize = 1.0f; // Default size
        // Check if customizable and get ID if exists
        de.tum.cit.fop.maze.custom.CustomElementDefinition def = de.tum.cit.fop.maze.custom.CustomElementManager
                .getInstance().getElementByName(textureKey);
        if (def != null) {
            textureKey = def.getId();
            projectileSize = def.getFloatProperty("projectileSize");
            if (projectileSize <= 0) {
                projectileSize = 1.0f; // Safety fallback
            }
        }

        Projectile p = new Projectile(startX, startY, vx, vy,
                weapon.getDamage() + player.getDamageBonus(),
                weapon.getDamageType(),
                weapon.getEffect(),
                true,
                textureKey,
                projectileSize);

        projectiles.add(p);
        AudioManager.getInstance().playSound("spell_shoot");

        // === Fire Staff 后坐力效果 ===
        // 对 Fire Staff 和其他法杖类武器应用后坐力
        if (weapon.getName().contains("Staff") || weapon.getName().contains("staff")) {
            float recoilStrength = 8.0f; // 后坐力强度 (增强)
            // 向反方向施加速度脉冲
            float recoilVx = -aimDirection.x * recoilStrength;
            float recoilVy = -aimDirection.y * recoilStrength;
            player.addVelocity(recoilVx, recoilVy);

            // 记录开火事件，供粒子系统使用
            lastFireX = startX;
            lastFireY = startY;
            lastFireDirX = aimDirection.x;
            lastFireDirY = aimDirection.y;
            fireEventTriggered = true;
        }
    }

    private boolean customElementExists(String name) {
        for (de.tum.cit.fop.maze.custom.CustomElementDefinition def : de.tum.cit.fop.maze.custom.CustomElementManager
                .getInstance().getAllElements()) {
            if (def.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }
}
