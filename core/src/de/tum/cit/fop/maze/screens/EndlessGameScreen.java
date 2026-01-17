package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.EndlessModeConfig;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.effects.FloatingText;
import de.tum.cit.fop.maze.model.*;
import de.tum.cit.fop.maze.model.items.Potion;
import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.ui.EndlessHUD;
import de.tum.cit.fop.maze.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 无尽模式主游戏画面 (Endless Game Screen)
 * 
 * 与GameScreen的主要区别:
 * - 使用ChunkManager动态加载900×900地图
 * - 集成ComboSystem, RageSystem, WaveSystem
 * - 敌人持续刷新而非预设
 * - 无钥匙和出口逻辑
 * - 使用EndlessHUD
 */
public class EndlessGameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final Viewport gameViewport;
    private final Stage uiStage;

    // === 渲染 ===
    private TextureManager textureManager;
    private MazeRenderer mazeRenderer;
    private FogRenderer fogRenderer;

    // === 地图系统 ===
    private ChunkManager chunkManager;
    private EndlessMapGenerator mapGenerator;

    // === 核心系统 ===
    private ComboSystem comboSystem;
    private RageSystem rageSystem;
    private WaveSystem waveSystem;

    // === 游戏对象 ===
    private Player player;
    private List<Enemy> enemies;
    private List<Trap> traps;
    private List<FloatingText> floatingTexts;
    private List<Potion> potions; // 掉落的药水

    // === HUD ===
    private EndlessHUD hud;

    // === 游戏状态 ===
    private float stateTime = 0f;
    private int totalKills = 0;
    private int currentScore = 0;
    private boolean isPaused = false;
    private boolean isGameOver = false;

    // === 暂停菜单 ===
    private Table pauseTable;
    private de.tum.cit.fop.maze.ui.SettingsUI settingsUI;
    private Table settingsTable;

    // === 控制台 ===
    private DeveloperConsole developerConsole;
    private de.tum.cit.fop.maze.ui.ConsoleUI consoleUI;
    private boolean isConsoleOpen = false;
    private boolean consoleJustClosed = false;

    // === 常量 ===
    private static final float UNIT_SCALE = 16f;
    private static final float CAMERA_LERP_SPEED = 4.0f;
    private static final int MAX_ENEMIES = EndlessModeConfig.MAX_ENEMY_COUNT;

    // === 敌人刷新 ===
    private Random spawnRandom;

    public EndlessGameScreen(MazeRunnerGame game) {
        this(game, null);
    }

    public EndlessGameScreen(MazeRunnerGame game, EndlessGameState savedState) {
        this.game = game;

        camera = new OrthographicCamera();
        gameViewport = new FitViewport(640, 360, camera);
        gameViewport.apply();

        uiStage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        textureManager = new TextureManager(game.getAtlas());
        mazeRenderer = new MazeRenderer(game.getSpriteBatch(), textureManager);
        fogRenderer = new FogRenderer(game.getSpriteBatch());

        initializeSystems();

        if (savedState != null) {
            loadState(savedState);
        } else {
            initializeNewGame();
        }

        setupHUD();
        setupPauseMenu();
        setupDeveloperConsole();
        setInputProcessors();

        // 播放无尽模式BGM
        AudioManager.getInstance().playBgm(AudioManager.BGM_BOSS);
    }

    private void initializeSystems() {
        // 地图系统
        mapGenerator = new EndlessMapGenerator();
        chunkManager = new ChunkManager();

        // 核心系统
        comboSystem = new ComboSystem();
        rageSystem = new RageSystem();
        waveSystem = new WaveSystem();

        // 游戏对象
        enemies = new ArrayList<>();
        traps = new ArrayList<>();
        floatingTexts = new ArrayList<>();
        potions = new ArrayList<>();
        spawnRandom = new Random();

        // 设置系统监听器
        setupSystemListeners();
    }

    private void setupSystemListeners() {
        // COMBO监听器
        comboSystem.setListener(new ComboSystem.ComboListener() {
            @Override
            public void onComboIncreased(int newCombo, float multiplier) {
                // 可以在这里添加视觉效果
            }

            @Override
            public void onComboReset(int finalCombo) {
                // COMBO断了
            }

            @Override
            public void onMilestoneReached(int combo, String milestoneName) {
                // 显示里程碑特效
                floatingTexts.add(new FloatingText(
                        player.getX(), player.getY() + 1,
                        milestoneName, Color.GOLD));
            }
        });

        // RAGE监听器
        rageSystem.setListener((newLevel, levelName) -> {
            floatingTexts.add(new FloatingText(
                    player.getX(), player.getY() + 1.5f,
                    "RAGE: " + levelName, Color.RED));
        });

        // 波次监听器
        waveSystem.setListener(new WaveSystem.WaveListener() {
            @Override
            public void onWaveChanged(int newWave, float spawnInterval, float healthMultiplier) {
                floatingTexts.add(new FloatingText(
                        player.getX(), player.getY() + 2,
                        "Wave " + (newWave + 1), Color.YELLOW));
            }

            @Override
            public void onSpawnEnemy() {
                spawnEnemyNearPlayer();
            }

            @Override
            public void onSpawnBoss() {
                spawnBossNearPlayer();
            }
        });
    }

    private void initializeNewGame() {
        // 玩家出生在地图中心
        Vector2 spawnPoint = mapGenerator.getPlayerSpawnPoint();
        player = new Player(spawnPoint.x, spawnPoint.y);

        // 加载初始区块
        chunkManager.updateActiveChunks(player.getX(), player.getY());
    }

    private void loadState(EndlessGameState state) {
        player = new Player(state.playerX, state.playerY);
        player.setLives(state.playerLives);

        // 恢复系统状态
        comboSystem.setCurrentCombo(state.currentCombo);
        comboSystem.setMaxCombo(state.maxCombo);
        rageSystem.setRageLevel(state.rageLevel);
        waveSystem.setSurvivalTime(state.survivalTime);

        totalKills = state.totalKills;
        currentScore = state.score;

        // 加载区块
        chunkManager.updateActiveChunks(player.getX(), player.getY());
    }

    private void setupHUD() {
        hud = new EndlessHUD(game.getSpriteBatch(), player, game.getSkin(), textureManager,
                this::togglePause);
        hud.setSystems(comboSystem, rageSystem, waveSystem);
    }

    private void setupPauseMenu() {
        pauseTable = new Table();
        pauseTable.setFillParent(true);

        Pixmap bg = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bg.setColor(0, 0, 0, 0.7f);
        bg.fill();
        pauseTable.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(bg))));

        Label title = new Label("PAUSED", game.getSkin(), "title");
        pauseTable.add(title).padBottom(40).row();

        addPauseButton("Resume", this::togglePause);
        addPauseButton("Settings", () -> {
            pauseTable.setVisible(false);
            showSettingsOverlay();
        });
        addPauseButton("Save Game", this::saveEndlessGame);
        addPauseButton("Load Game", () -> {
            pauseTable.setVisible(false);
            showLoadDialog();
        });
        addPauseButton("Main Menu", () -> {
            game.goToMenu();
        });

        pauseTable.setVisible(false);
        uiStage.addActor(pauseTable);
    }

    private void addPauseButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, game.getSkin());
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        pauseTable.add(btn).width(200).padBottom(20).row();
    }

    private void setupDeveloperConsole() {
        developerConsole = new DeveloperConsole();
        consoleUI = new de.tum.cit.fop.maze.ui.ConsoleUI(uiStage, game.getSkin());
        consoleUI.setConsole(developerConsole);
        // 无尽模式没有GameWorld，设置为null
        developerConsole.setGameWorld(null);
    }

    private void setInputProcessors() {
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputMultiplexer(
                uiStage, hud.getStage()));
    }

    // === 主循环 ===

    @Override
    public void render(float delta) {
        // 控制台切换
        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE) || Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            toggleConsole();
        }

        if (isConsoleOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                toggleConsole();
                consoleJustClosed = true;
            }
            renderConsole(delta);
            return;
        }

        if (consoleJustClosed) {
            consoleJustClosed = false;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!isPaused && !isGameOver) {
            updateGame(delta);
        }

        renderGame(delta);
        renderHUD(delta);

        if (isPaused) {
            uiStage.act(delta);
            uiStage.getViewport().apply();
            uiStage.draw();
        }
    }

    private void updateGame(float delta) {
        stateTime += delta;

        // 更新玩家定时器（攻击动画、受伤闪烁等）
        player.updateTimers(delta);

        // 更新核心系统
        comboSystem.update(delta);
        rageSystem.update(totalKills, waveSystem.getSurvivalTime());
        waveSystem.update(delta);

        // 更新玩家输入
        updatePlayerInput(delta);

        // 更新区块加载
        chunkManager.updateActiveChunks(player.getX(), player.getY());

        // 更新敌人
        updateEnemies(delta);

        // 更新陷阱碰撞检测
        updateTraps(delta);

        // 更新浮动文字
        updateFloatingTexts(delta);

        // 更新药水拾取
        updatePotions(delta);

        // 更新HUD数据
        hud.setTotalKills(totalKills);
        hud.setCurrentScore(currentScore);
        hud.setCurrentZone(EndlessModeConfig.getThemeForPosition(
                (int) player.getX(), (int) player.getY()));

        // 检查游戏结束 - 当玩家死亡且游戏尚未结束时触发
        // Fix: 之前的条件 `player.getLives() <= 0 && !player.isDead()` 有逻辑错误
        // 因为 Player.damage() 在生命值 <= 0 时会立即设置 isDead = true
        // 所以该条件永远无法满足，导致 game over 从不触发
        if (player.isDead() && !isGameOver) {
            triggerGameOver();
        }
    }

    private void updatePlayerInput(float delta) {
        float speed = player.getSpeed();

        float targetVx = 0;
        float targetVy = 0;

        boolean hasInput = false;

        if (Gdx.input.isKeyPressed(GameSettings.KEY_UP)) {
            targetVy = speed;
            hasInput = true;
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_DOWN)) {
            targetVy = -speed;
            hasInput = true;
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_LEFT)) {
            targetVx = -speed;
            hasInput = true;
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_RIGHT)) {
            targetVx = speed;
            hasInput = true;
        }

        player.setRunning(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT));
        player.applyAcceleration(targetVx, targetVy, delta);

        // 应用移动
        float moveX = player.getVelocityX() * delta;
        float moveY = player.getVelocityY() * delta;

        if (!player.isNoClip()) {
            // X轴移动 - 使用内联碰撞检查
            if (!canPlayerMoveTo(player.getX() + moveX, player.getY())) {
                player.handleWallCollision('x');
                moveX = 0;
            }
            // Y轴移动
            if (!canPlayerMoveTo(player.getX(), player.getY() + moveY)) {
                player.handleWallCollision('y');
                moveY = 0;
            }
        }

        player.move(moveX, moveY);

        // 玩家停止时对齐到整数格
        // 当没有输入且速度接近零时，平滑对齐到最近的整数格位置
        if (!hasInput && !player.isMoving()) {
            snapPlayerToGrid(delta);
        }

        // 攻击
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_ATTACK)) {
            player.attack();
            performAttack();
        }
    }

    /**
     * 平滑地将玩家对齐到最近的整数格位置
     * 当玩家停止移动时调用，确保玩家不会停在两个格子之间
     */
    private void snapPlayerToGrid(float delta) {
        float snapSpeed = 10.0f * delta; // 对齐速度

        float targetX = Math.round(player.getX());
        float targetY = Math.round(player.getY());

        float dx = targetX - player.getX();
        float dy = targetY - player.getY();

        // 如果已经在整数格上,不需要调整
        if (Math.abs(dx) < 0.01f && Math.abs(dy) < 0.01f) {
            player.setPosition(targetX, targetY);
            return;
        }

        // 平滑移动到目标位置
        float moveX = Math.signum(dx) * Math.min(Math.abs(dx), snapSpeed);
        float moveY = Math.signum(dy) * Math.min(Math.abs(dy), snapSpeed);

        // 检查碰撞后再移动
        if (Math.abs(moveX) > 0.001f) {
            if (canPlayerMoveTo(player.getX() + moveX, player.getY())) {
                player.move(moveX, 0);
            }
        }
        if (Math.abs(moveY) > 0.001f) {
            if (canPlayerMoveTo(player.getX(), player.getY() + moveY)) {
                player.move(0, moveY);
            }
        }
    }

    private void performAttack() {
        Weapon weapon = player.getCurrentWeapon();
        if (weapon == null)
            return;

        float attackRange = weapon.getRange();
        float attackDamage = weapon.getDamage() + player.getDamageBonus();

        for (Enemy enemy : enemies) {
            if (enemy.isDead())
                continue;

            float dist = Vector2.dst(player.getX(), player.getY(), enemy.getX(), enemy.getY());
            if (dist <= attackRange) {
                boolean killed = enemy.takeDamage((int) attackDamage, weapon.getDamageType());

                if (killed) {
                    onEnemyKilled(enemy);
                }
            }
        }
    }

    private void onEnemyKilled(Enemy enemy) {
        totalKills++;

        // COMBO加分
        float multiplier = comboSystem.onKill();
        int baseScore = EndlessModeConfig.SCORE_PER_KILL;
        int earnedScore = (int) (baseScore * multiplier);
        currentScore += earnedScore;

        // 浮动分数显示
        floatingTexts.add(new FloatingText(
                enemy.getX(), enemy.getY() + 0.5f,
                "+" + earnedScore, Color.GOLD));

        // 掉落物品
        spawnDrops(enemy);
    }

    private void spawnDrops(Enemy enemy) {
        // 生命药水掉落 (10%)
        if (spawnRandom.nextFloat() < EndlessModeConfig.HEALTH_POTION_DROP_RATE) {
            Potion potion = new Potion(enemy.getX(), enemy.getY());
            potions.add(potion);
            GameLogger.debug("EndlessGameScreen", "Potion dropped at " + enemy.getX() + ", " + enemy.getY());
        }
    }

    /**
     * 更新药水拾取逻辑
     */
    private void updatePotions(float delta) {
        for (int i = potions.size() - 1; i >= 0; i--) {
            Potion potion = potions.get(i);

            // 检查玩家是否拾取
            float dx = player.getX() - potion.getX();
            float dy = player.getY() - potion.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < 1.0f) { // 拾取半径
                // 恢复生命
                if (player.getLives() < player.getMaxHealth()) {
                    player.restoreHealth(1);
                    floatingTexts.add(new FloatingText(
                            player.getX(), player.getY() + 0.5f,
                            "+1 HP", Color.GREEN));
                    AudioManager.getInstance().playSound("pickup");
                } else {
                    // 满血时转化为分数
                    currentScore += 50;
                    floatingTexts.add(new FloatingText(
                            player.getX(), player.getY() + 0.5f,
                            "+50", Color.GOLD));
                }
                potions.remove(i);
            }
        }
    }

    private void updateEnemies(float delta) {
        // 移除死亡敌人
        enemies.removeIf(e -> e.isDead() && e.isRemovable());

        // 更新存活敌人 - 使用带碰撞检测的寻路逻辑
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                // 敌人AI - 追踪玩家，带碰撞检测
                float dx = player.getX() - enemy.getX();
                float dy = player.getY() - enemy.getY();
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < 30 && dist > 0.5f) {
                    // 追踪玩家，应用RAGE速度加成
                    float speed = GameSettings.enemyChaseSpeed * rageSystem.getEnemySpeedMultiplier() * delta;

                    // 轴对齐寻路：优先沿主轴移动，被阻挡时尝试次轴
                    float moveX = 0;
                    float moveY = 0;

                    // 确定主轴和次轴
                    boolean preferX = Math.abs(dx) > Math.abs(dy);

                    if (preferX) {
                        // 主轴X：尝试水平移动
                        moveX = Math.signum(dx) * speed;
                        if (!canEnemyMoveTo(enemy.getX() + moveX, enemy.getY())) {
                            // X轴被阻挡，尝试Y轴
                            moveX = 0;
                            if (Math.abs(dy) > 0.1f) {
                                moveY = Math.signum(dy) * speed;
                                if (!canEnemyMoveTo(enemy.getX(), enemy.getY() + moveY)) {
                                    moveY = 0; // 两个方向都被阻挡
                                }
                            }
                        }
                    } else {
                        // 主轴Y：尝试垂直移动
                        moveY = Math.signum(dy) * speed;
                        if (!canEnemyMoveTo(enemy.getX(), enemy.getY() + moveY)) {
                            // Y轴被阻挡，尝试X轴
                            moveY = 0;
                            if (Math.abs(dx) > 0.1f) {
                                moveX = Math.signum(dx) * speed;
                                if (!canEnemyMoveTo(enemy.getX() + moveX, enemy.getY())) {
                                    moveX = 0; // 两个方向都被阻挡
                                }
                            }
                        }
                    }

                    // 应用移动（只有在可以移动时才更新位置）
                    if (moveX != 0 || moveY != 0) {
                        enemy.setPosition(enemy.getX() + moveX, enemy.getY() + moveY);
                    }
                }

                // 攻击玩家
                if (dist < 1.5f) {
                    int baseDamage = 1;
                    int damage = (int) (baseDamage * rageSystem.getEnemyDamageMultiplier());
                    player.damage(damage, enemy.getAttackDamageType());
                }
            }
        }
    }

    /**
     * 检查敌人是否可以移动到指定位置（碰撞检测）
     */
    private boolean canEnemyMoveTo(float x, float y) {
        float size = 0.9f; // 敌人碰撞箱大小
        float padding = 0.05f;

        // 检查四个角
        return !isWallAt((int) (x + padding), (int) (y + padding)) &&
                !isWallAt((int) (x + size - padding), (int) (y + padding)) &&
                !isWallAt((int) (x + size - padding), (int) (y + size - padding)) &&
                !isWallAt((int) (x + padding), (int) (y + size - padding));
    }

    private void spawnEnemyNearPlayer() {
        if (enemies.size() >= MAX_ENEMIES)
            return;

        float angle = spawnRandom.nextFloat() * 360f * MathUtils.degreesToRadians;
        float distance = EndlessModeConfig.SPAWN_MIN_DISTANCE +
                spawnRandom.nextFloat() * (EndlessModeConfig.SPAWN_MAX_DISTANCE - EndlessModeConfig.SPAWN_MIN_DISTANCE);

        float spawnX = player.getX() + MathUtils.cos(angle) * distance;
        float spawnY = player.getY() + MathUtils.sin(angle) * distance;

        // 边界检查
        spawnX = MathUtils.clamp(spawnX, 5, EndlessModeConfig.MAP_WIDTH - 5);
        spawnY = MathUtils.clamp(spawnY, 5, EndlessModeConfig.MAP_HEIGHT - 5);

        // 检查是否在墙内
        if (isWallAt((int) spawnX, (int) spawnY))
            return;

        // 使用正确的构造函数：Enemy(x, y, health, attackType, shieldType, shieldAmount)
        int baseHealth = 50 + (int) (waveSystem.getEnemyHealthMultiplier() * 20);
        Enemy enemy = new Enemy(spawnX, spawnY, baseHealth, DamageType.PHYSICAL, null, 0);

        // 根据生成位置的主题分配敌人类型
        String theme = EndlessModeConfig.getThemeForPosition((int) spawnX, (int) spawnY);
        enemy.setType(getEnemyTypeForTheme(theme));

        enemies.add(enemy);
    }

    private void spawnBossNearPlayer() {
        // 与普通敌人相同的刷新位置逻辑
        float angle = spawnRandom.nextFloat() * 360f * MathUtils.degreesToRadians;
        float distance = EndlessModeConfig.SPAWN_MAX_DISTANCE;

        float spawnX = player.getX() + MathUtils.cos(angle) * distance;
        float spawnY = player.getY() + MathUtils.sin(angle) * distance;

        spawnX = MathUtils.clamp(spawnX, 5, EndlessModeConfig.MAP_WIDTH - 5);
        spawnY = MathUtils.clamp(spawnY, 5, EndlessModeConfig.MAP_HEIGHT - 5);

        if (isWallAt((int) spawnX, (int) spawnY))
            return;

        // BOSS有更高的血量，带护盾
        int bossHealth = 300 + (int) (waveSystem.getEnemyHealthMultiplier() * 100);
        Enemy boss = new Enemy(spawnX, spawnY, bossHealth, DamageType.MAGICAL, DamageType.PHYSICAL, 50);

        // 根据生成位置的主题分配敌人类型
        String theme = EndlessModeConfig.getThemeForPosition((int) spawnX, (int) spawnY);
        boss.setType(getEnemyTypeForTheme(theme));

        enemies.add(boss);

        floatingTexts.add(new FloatingText(
                spawnX, spawnY + 1, "BOSS!", Color.RED));
    }

    private void updateFloatingTexts(float delta) {
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update(delta);
            if (ft.isExpired()) {
                floatingTexts.remove(i);
            }
        }
    }

    private boolean isWallAt(int x, int y) {
        MapChunk chunk = chunkManager.getChunkAtWorld(x, y);
        if (chunk == null)
            return true;

        for (WallEntity wall : chunk.getWalls()) {
            if (x >= wall.getOriginX() && x < wall.getOriginX() + wall.getGridWidth() &&
                    y >= wall.getOriginY() && y < wall.getOriginY() + wall.getGridHeight()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否可以移动到指定位置
     */
    private boolean canPlayerMoveTo(float x, float y) {
        float size = player.getWidth();
        float padding = 0.05f;

        // 检查四个角
        return !isWallAt((int) (x + padding), (int) (y + padding)) &&
                !isWallAt((int) (x + size - padding), (int) (y + padding)) &&
                !isWallAt((int) (x + size - padding), (int) (y + size - padding)) &&
                !isWallAt((int) (x + padding), (int) (y + size - padding));
    }

    private void renderGame(float delta) {
        gameViewport.apply();
        updateCamera(delta);

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // 渲染地板（基于当前区域主题）
        String currentTheme = EndlessModeConfig.getThemeForPosition((int) player.getX(), (int) player.getY());
        TextureRegion floor = getFloorTextureForTheme(currentTheme);

        // 渲染已加载区块的墙
        for (MapChunk chunk : chunkManager.getLoadedChunks()) {
            renderChunk(chunk, floor);
        }

        // 渲染敌人 (使用主题敌人动画)
        for (Enemy e : enemies) {
            // 获取敌人类型对应的方向性动画
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> enemyAnim = textureManager
                    .getEnemyAnimation(e.getType(), e.getVelocityX(), e.getVelocityY());
            TextureRegion enemyFrame = enemyAnim.getKeyFrame(stateTime, true);

            if (e.isDead()) {
                game.getSpriteBatch().setColor(Color.GRAY);
            } else if (e.isHurt()) {
                game.getSpriteBatch().setColor(Color.RED);
            }

            // 渲染敌人 (与关卡模式渲染方式一致)
            float drawWidth = 16f;
            float drawHeight = 16f;
            float drawX = e.getX() * UNIT_SCALE - (drawWidth - UNIT_SCALE) / 2;
            float drawY = e.getY() * UNIT_SCALE - (drawHeight - UNIT_SCALE) / 2;

            // 移动方向向左时翻转
            boolean flipX = e.getVelocityX() < 0;
            if (flipX) {
                game.getSpriteBatch().draw(enemyFrame, drawX + drawWidth, drawY, -drawWidth, drawHeight);
            } else {
                game.getSpriteBatch().draw(enemyFrame, drawX, drawY, drawWidth, drawHeight);
            }

            game.getSpriteBatch().setColor(Color.WHITE);
        }

        // 渲染药水掉落物
        for (Potion potion : potions) {
            TextureRegion potionTex = textureManager.potionRegion;
            if (potionTex != null) {
                game.getSpriteBatch().draw(potionTex,
                        potion.getX() * UNIT_SCALE, potion.getY() * UNIT_SCALE,
                        UNIT_SCALE, UNIT_SCALE);
            }
        }

        // 渲染浮动文字
        com.badlogic.gdx.graphics.g2d.BitmapFont font = game.getSkin().getFont("font");
        font.getData().setScale(0.5f);
        for (FloatingText ft : floatingTexts) {
            font.setColor(ft.color);
            font.draw(game.getSpriteBatch(), ft.text, ft.x * UNIT_SCALE, ft.y * UNIT_SCALE + 16);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);

        // 渲染玩家
        renderPlayer();

        // 迷雾效果
        game.getSpriteBatch().setColor(Color.WHITE);
        float pcX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float pcY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;
        fogRenderer.render(pcX, pcY, camera);

        game.getSpriteBatch().end();
    }

    private void renderChunk(MapChunk chunk, TextureRegion floor) {
        int startX = chunk.getWorldStartX();
        int startY = chunk.getWorldStartY();
        int size = chunk.getSize();

        // 获取区块主题对应的地砖纹理
        TextureRegion chunkFloor = getFloorTextureForTheme(chunk.getTheme());

        // 渲染地砖 - 只渲染可见范围内的地砖以优化性能
        float camX = camera.position.x / UNIT_SCALE;
        float camY = camera.position.y / UNIT_SCALE;
        float viewW = camera.viewportWidth * camera.zoom / UNIT_SCALE / 2 + 2;
        float viewH = camera.viewportHeight * camera.zoom / UNIT_SCALE / 2 + 2;

        int minLX = Math.max(0, (int) (camX - viewW - startX));
        int maxLX = Math.min(size, (int) (camX + viewW - startX));
        int minLY = Math.max(0, (int) (camY - viewH - startY));
        int maxLY = Math.min(size, (int) (camY + viewH - startY));

        for (int lx = minLX; lx < maxLX; lx++) {
            for (int ly = minLY; ly < maxLY; ly++) {
                float worldX = (startX + lx) * UNIT_SCALE;
                float worldY = (startY + ly) * UNIT_SCALE;
                game.getSpriteBatch().draw(chunkFloor, worldX, worldY, UNIT_SCALE, UNIT_SCALE);
            }
        }

        // 渲染墙壁
        for (WallEntity wall : chunk.getWalls()) {
            TextureRegion wallTex = textureManager.getWallRegion(chunk.getTheme(),
                    wall.getGridWidth(), wall.getGridHeight(), wall.getOriginX(), wall.getOriginY());
            game.getSpriteBatch().draw(wallTex,
                    wall.getOriginX() * UNIT_SCALE,
                    wall.getOriginY() * UNIT_SCALE,
                    wall.getGridWidth() * UNIT_SCALE,
                    wall.getGridHeight() * UNIT_SCALE);
        }

        // 渲染陷阱
        for (Vector2 trapPos : chunk.getTraps()) {
            // 尝试获取动画
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> trapAnim = textureManager
                    .getTrapAnimation(chunk.getTheme());
            if (trapAnim != null) {
                // 绘制静态底座
                TextureRegion base = textureManager.getTrapRegion(chunk.getTheme());
                if (base != null) {
                    game.getSpriteBatch().draw(base, trapPos.x * UNIT_SCALE, trapPos.y * UNIT_SCALE,
                            UNIT_SCALE, UNIT_SCALE);
                }
                // 绘制动画覆盖层
                TextureRegion currentFrame = trapAnim.getKeyFrame(stateTime, true);
                float overlayY = trapPos.y * UNIT_SCALE + (UNIT_SCALE / 2f);
                game.getSpriteBatch().draw(currentFrame, trapPos.x * UNIT_SCALE, overlayY,
                        UNIT_SCALE, UNIT_SCALE);
            } else {
                // 无动画时使用静态纹理
                TextureRegion trapTex = textureManager.getTrapRegion(chunk.getTheme());
                if (trapTex != null) {
                    game.getSpriteBatch().draw(trapTex, trapPos.x * UNIT_SCALE, trapPos.y * UNIT_SCALE,
                            UNIT_SCALE, UNIT_SCALE);
                }
            }
        }
    }

    private TextureRegion getFloorTextureForTheme(String theme) {
        switch (theme) {
            case "Grassland":
                return textureManager.floorGrassland;
            case "Jungle":
                return textureManager.floorJungle;
            case "Desert":
                return textureManager.floorDesert;
            case "Ice":
                return textureManager.floorIce;
            case "Space":
                return textureManager.floorSpace;
            default:
                return textureManager.floorDungeon;
        }
    }

    private void renderPlayer() {
        TextureRegion playerFrame;
        int dir = 0; // 默认向下

        // 基于速度确定方向
        if (Math.abs(player.getVelocityY()) > Math.abs(player.getVelocityX())) {
            dir = player.getVelocityY() > 0 ? 1 : 0;
        } else if (player.getVelocityX() != 0) {
            dir = player.getVelocityX() < 0 ? 2 : 3;
        }

        boolean isMoving = player.isMoving();

        if (player.isAttacking()) {
            float progress = (player.getAttackAnimTotalDuration() - player.getAttackAnimTimer()) /
                    player.getAttackAnimTotalDuration() * 0.2f;
            switch (dir) {
                case 1:
                    playerFrame = textureManager.playerAttackUp.getKeyFrame(progress, false);
                    break;
                case 2:
                    playerFrame = textureManager.playerAttackLeft.getKeyFrame(progress, false);
                    break;
                case 3:
                    playerFrame = textureManager.playerAttackRight.getKeyFrame(progress, false);
                    break;
                default:
                    playerFrame = textureManager.playerAttackDown.getKeyFrame(progress, false);
                    break;
            }
        } else if (isMoving) {
            switch (dir) {
                case 1:
                    playerFrame = textureManager.playerUp.getKeyFrame(stateTime, true);
                    break;
                case 2:
                    playerFrame = textureManager.playerLeft.getKeyFrame(stateTime, true);
                    break;
                case 3:
                    playerFrame = textureManager.playerRight.getKeyFrame(stateTime, true);
                    break;
                default:
                    playerFrame = textureManager.playerDown.getKeyFrame(stateTime, true);
                    break;
            }
        } else {
            switch (dir) {
                case 1:
                    playerFrame = textureManager.playerUpStand;
                    break;
                case 2:
                    playerFrame = textureManager.playerLeftStand;
                    break;
                case 3:
                    playerFrame = textureManager.playerRightStand;
                    break;
                default:
                    playerFrame = textureManager.playerDownStand;
                    break;
            }
        }

        if (player.isDead()) {
            game.getSpriteBatch().setColor(0.5f, 0.5f, 0.5f, 1f);
        } else if (player.isHurt()) {
            game.getSpriteBatch().setColor(1f, 0f, 0f, 1f);
        }

        float drawX = player.getX() * UNIT_SCALE;
        float drawY = player.getY() * UNIT_SCALE;
        game.getSpriteBatch().draw(playerFrame, drawX, drawY);
        game.getSpriteBatch().setColor(Color.WHITE);
    }

    private void updateCamera(float delta) {
        float targetX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float targetY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;
        camera.position.x += (targetX - camera.position.x) * CAMERA_LERP_SPEED * delta;
        camera.position.y += (targetY - camera.position.y) * CAMERA_LERP_SPEED * delta;

        // Z/X 按键控制相机缩放 (与关卡模式一致)
        if (Gdx.input.isKeyPressed(Input.Keys.Z))
            GameSettings.cameraZoom -= 0.02f;
        if (Gdx.input.isKeyPressed(Input.Keys.X))
            GameSettings.cameraZoom += 0.02f;
        GameSettings.cameraZoom = MathUtils.clamp(GameSettings.cameraZoom, 0.2f, 2.0f);

        camera.zoom = GameSettings.cameraZoom;
        camera.update();
    }

    private void renderHUD(float delta) {
        hud.getStage().getViewport().apply();
        hud.update(delta);
        hud.render();
    }

    private void renderConsole(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiStage.act(delta);
        uiStage.getViewport().apply();
        uiStage.draw();
    }

    // === 游戏流程 ===

    private void togglePause() {
        isPaused = !isPaused;
        pauseTable.setVisible(isPaused);
    }

    private void toggleConsole() {
        isConsoleOpen = !isConsoleOpen;
        if (isConsoleOpen) {
            consoleUI.show();
            Gdx.input.setInputProcessor(uiStage);
        } else {
            consoleUI.hide();
            consoleJustClosed = true;
            setInputProcessors();
        }
    }

    private void triggerGameOver() {
        isGameOver = true;

        // 切换到结算画面
        EndlessGameState finalState = EndlessGameState.createFromGame(
                player,
                waveSystem.getSurvivalTime(),
                totalKills,
                comboSystem.getCurrentCombo(),
                comboSystem.getMaxCombo(),
                rageSystem.getRageLevel(),
                waveSystem.getCurrentWave(),
                currentScore,
                EndlessModeConfig.getThemeForPosition((int) player.getX(), (int) player.getY()));

        game.setScreen(new EndlessGameOverScreen(game, finalState));
    }

    private void saveEndlessGame() {
        EndlessGameState state = EndlessGameState.createFromGame(
                player,
                waveSystem.getSurvivalTime(),
                totalKills,
                comboSystem.getCurrentCombo(),
                comboSystem.getMaxCombo(),
                rageSystem.getRageLevel(),
                waveSystem.getCurrentWave(),
                currentScore,
                EndlessModeConfig.getThemeForPosition((int) player.getX(), (int) player.getY()));

        // TODO: 调用SaveManager保存无尽模式存档
        GameLogger.info("EndlessGameScreen", "Game saved: " + state);
    }

    // === 设置和加载对话框 ===

    private void showSettingsOverlay() {
        if (settingsTable == null) {
            settingsUI = new de.tum.cit.fop.maze.ui.SettingsUI(game, uiStage, () -> {
                // On Back -> Hide settings, show pause menu
                settingsTable.setVisible(false);
                pauseTable.setVisible(true);
            });
            settingsTable = settingsUI.build();
            settingsTable.setVisible(false);
            settingsTable.setFillParent(true);
            uiStage.addActor(settingsTable);
        }
        settingsTable.setVisible(true);
        settingsTable.toFront();
    }

    private void showLoadDialog() {
        Window win = new Window("Select Endless Save", game.getSkin());
        win.setModal(true);
        win.setResizable(true);
        win.getTitleLabel().setAlignment(com.badlogic.gdx.utils.Align.center);

        com.badlogic.gdx.files.FileHandle[] files = SaveManager.getEndlessSaveFiles();
        Table listTable = new Table();
        listTable.top();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

        if (files.length == 0) {
            listTable.add(new Label("No endless saves found.", game.getSkin())).pad(20);
        } else {
            for (com.badlogic.gdx.files.FileHandle file : files) {
                Table rowTable = new Table();
                String dateStr = sdf.format(new java.util.Date(file.lastModified()));
                TextButton loadBtn = new TextButton(file.nameWithoutExtension() + "\n" + dateStr, game.getSkin());
                loadBtn.getLabel().setFontScale(0.8f);
                loadBtn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
                loadBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        EndlessGameState state = SaveManager.loadEndlessGame(file.name());
                        if (state != null) {
                            win.remove();
                            game.setScreen(new EndlessGameScreen(game, state));
                        }
                    }
                });
                TextButton deleteBtn = new TextButton("X", game.getSkin());
                deleteBtn.setColor(Color.RED);
                deleteBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        SaveManager.deleteEndlessSave(file.name());
                        win.remove();
                        showLoadDialog();
                    }
                });
                rowTable.add(loadBtn).expandX().fillX().height(50).padRight(5);
                rowTable.add(deleteBtn).width(50).height(50);
                listTable.add(rowTable).expandX().fillX().padBottom(5).row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);

        final ScrollPane sp = scrollPane;
        scrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                    Actor fromActor) {
                uiStage.setScrollFocus(sp);
            }

            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                    Actor toActor) {
                // Keep focus for better UX
            }
        });

        win.add(scrollPane).grow().pad(10).row();
        TextButton closeBtn = new TextButton("Close", game.getSkin());
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                win.remove();
                pauseTable.setVisible(true);
            }
        });
        win.add(closeBtn).padBottom(10);
        float w = MathUtils.clamp(uiStage.getWidth() * 0.6f, 300, 600);
        float h = uiStage.getHeight() * 0.7f;
        win.setSize(w, h);
        win.setPosition(uiStage.getWidth() / 2 - w / 2, uiStage.getHeight() / 2 - h / 2);
        uiStage.addActor(win);
    }

    // === 陷阱更新 ===

    /**
     * 更新陷阱碰撞检测
     */
    private void updateTraps(float delta) {
        // 遍历已加载区块的陷阱
        for (MapChunk chunk : chunkManager.getLoadedChunks()) {
            for (Vector2 trapPos : chunk.getTraps()) {
                // 检查玩家是否踩到陷阱
                float dx = player.getX() - trapPos.x;
                float dy = player.getY() - trapPos.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < 0.8f) { // 碰撞半径
                    // 造成陷阱伤害
                    boolean damaged = player.damage(1, DamageType.PHYSICAL);
                    if (damaged) {
                        floatingTexts.add(new FloatingText(
                                player.getX(), player.getY() + 0.5f,
                                "-1", Color.RED));
                    }
                }
            }
        }
    }

    // === 辅助方法 ===

    /**
     * 根据主题获取对应的敌人类型
     */
    private Enemy.EnemyType getEnemyTypeForTheme(String theme) {
        switch (theme) {
            case "Grassland":
                return Enemy.EnemyType.BOAR;
            case "Desert":
                return Enemy.EnemyType.SCORPION;
            case "Jungle":
                return Enemy.EnemyType.JUNGLE_CREATURE;
            case "Ice":
                return Enemy.EnemyType.YETI;
            case "Space":
                return Enemy.EnemyType.SPACE_DRONE;
            default:
                return Enemy.EnemyType.SLIME;
        }
    }

    // === Screen生命周期 ===

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        uiStage.getViewport().update(width, height, true);
        hud.resize(width, height);
    }

    @Override
    public void show() {
        setInputProcessors();
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
        isPaused = true;
        pauseTable.setVisible(true);
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        uiStage.dispose();
        hud.dispose();
        chunkManager.dispose();
    }
}
