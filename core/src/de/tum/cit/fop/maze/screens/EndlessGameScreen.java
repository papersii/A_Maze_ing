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
import de.tum.cit.fop.maze.model.weapons.WeaponEffect;
import de.tum.cit.fop.maze.ui.EndlessHUD;
import de.tum.cit.fop.maze.ui.ChestInteractUI;
import de.tum.cit.fop.maze.utils.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private de.tum.cit.fop.maze.utils.PlayerRenderer playerRenderer;

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
    private SpatialHashGrid<Enemy> enemyGrid; // Spatial hash for O(1) neighbor queries
    private List<Trap> traps;
    private List<FloatingText> floatingTexts;
    private List<Potion> potions; // 掉落的药水

    // === 宝箱系统 ===
    private Map<String, List<TreasureChest>> chunkChests; // chunkId -> chests
    private ChestInteractUI chestUI;
    private TreasureChest activeChest; // 当前交互的宝箱
    private boolean isChestUIActive = false;

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
    private boolean consoleJustOpened = false; // Prevents double-toggle in same frame

    // === 常量 ===
    private static final float UNIT_SCALE = 16f;
    private static final float CAMERA_LERP_SPEED = 4.0f;
    private static final int MAX_ENEMIES = EndlessModeConfig.MAX_ENEMY_COUNT;

    // === 玩家/武器朝向记忆 (队友功能) ===
    private int lastPlayerFacing = 3;

    // === 敌人刷新 ===
    private Random spawnRandom;

    // === 灰度Shader (对齐关卡模式死亡效果) ===
    private ShaderProgram grayscaleShader;
    private com.badlogic.gdx.graphics.glutils.ShapeRenderer shapeRenderer;

    // === 溅血粒子系统 ===
    private BloodParticleSystem bloodParticles;
    private de.tum.cit.fop.maze.utils.DustParticleSystem dustParticles;

    // === 鼠标瞄准系统 ===
    private float aimAngle = 270f; // 瞄准角度 (度数, 0=右, 90=上, 180=左, 270=下)
    private com.badlogic.gdx.math.Vector2 mouseWorldPos = new com.badlogic.gdx.math.Vector2();
    private int playerDirection = 0; // 0=下, 1=上, 2=左, 3=右
    private de.tum.cit.fop.maze.utils.CrosshairRenderer crosshairRenderer;

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
        playerRenderer = new de.tum.cit.fop.maze.utils.PlayerRenderer(game.getSpriteBatch(), textureManager,
                UNIT_SCALE);
        shapeRenderer = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();

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
        enemyGrid = new SpatialHashGrid<>(16f); // Cell size matches typical view radius
        traps = new ArrayList<>();
        floatingTexts = new ArrayList<>();
        potions = new ArrayList<>();
        spawnRandom = new Random();

        // 宝箱系统
        chunkChests = new HashMap<>();

        // 溅血粒子系统
        bloodParticles = new BloodParticleSystem();
        dustParticles = new de.tum.cit.fop.maze.utils.DustParticleSystem();

        // 准星渲染器
        crosshairRenderer = new de.tum.cit.fop.maze.utils.CrosshairRenderer();

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
        // 绑定玩家溅血粒子监听器
        // 绑定玩家溅血粒子监听器
        player.setDamageListener(
                (x, y, amount, dirX, dirY, knockback) -> bloodParticles.spawn(x, y, amount, dirX, dirY, knockback,
                        new Color(0.0f, 0.0f, 0.5f, 1.0f)));

        // 加载初始区块
        chunkManager.updateActiveChunks(player.getX(), player.getY());
    }

    private void loadState(EndlessGameState state) {
        player = new Player(state.playerX, state.playerY);
        player.setLives(state.playerLives);
        // 绑定玩家溅血粒子监听器
        player.setDamageListener(
                (x, y, amount, dirX, dirY, knockback) -> bloodParticles.spawn(x, y, amount, dirX, dirY, knockback));

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

        // 注册无尽模式数据提供器
        developerConsole.setEndlessMode(true, new DeveloperConsole.EndlessModeData() {
            @Override
            public int getTotalKills() {
                return totalKills;
            }

            @Override
            public int getCurrentScore() {
                return currentScore;
            }

            @Override
            public float getSurvivalTime() {
                return stateTime;
            }

            @Override
            public int getCurrentCombo() {
                return comboSystem != null ? comboSystem.getCurrentCombo() : 0;
            }

            @Override
            public int getCurrentWave() {
                return waveSystem != null ? waveSystem.getCurrentWave() : 0;
            }

            @Override
            public String getCurrentZone() {
                return EndlessModeConfig.getThemeForPosition((int) player.getX(), (int) player.getY());
            }

            @Override
            public void setScore(int score) {
                currentScore = score;
                hud.setCurrentScore(score);
            }

            @Override
            public void setCombo(int combo) {
                if (comboSystem != null)
                    comboSystem.setCurrentCombo(combo);
            }

            @Override
            public void addKills(int kills) {
                totalKills += kills;
                hud.setTotalKills(totalKills);
            }

            // === Rage System ===
            @Override
            public float getRageProgress() {
                return rageSystem != null ? rageSystem.getProgress() * 100f : 0;
            }

            @Override
            public int getRageLevel() {
                return rageSystem != null ? rageSystem.getCurrentLevel() : 0;
            }

            @Override
            public void setRageProgress(float progress) {
                if (rageSystem != null)
                    rageSystem.setProgress(progress / 100f);
            }

            @Override
            public void maxRage() {
                if (rageSystem != null)
                    rageSystem.maxOut();
            }

            // === Enemy Control ===
            @Override
            public void spawnEnemies(int count) {
                for (int i = 0; i < count; i++) {
                    spawnEnemyNearPlayer();
                }
            }

            @Override
            public void spawnBoss() {
                spawnBossNearPlayer();
            }

            @Override
            public int clearAllEnemies() {
                int count = enemies.size();
                for (Enemy e : enemies) {
                    enemyGrid.remove(e);
                }
                enemies.clear();
                return count;
            }

            @Override
            public int getEnemyCount() {
                return enemies.size();
            }

            // === Teleport ===
            @Override
            public void teleportPlayer(float x, float y) {
                x = com.badlogic.gdx.math.MathUtils.clamp(x, 5, EndlessModeConfig.MAP_WIDTH - 5);
                y = com.badlogic.gdx.math.MathUtils.clamp(y, 5, EndlessModeConfig.MAP_HEIGHT - 5);
                player.setPosition(x, y);
            }

            @Override
            public float getPlayerX() {
                return player.getX();
            }

            @Override
            public float getPlayerY() {
                return player.getY();
            }
        });
    }

    private void setInputProcessors() {
        // Gameplay Mode Input Chain (Aligned with GameScreen)
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();

        // 1. Global Keys (Console Toggle) - HIGHEST PRIORITY
        multiplexer.addProcessor(getConsoleKeyProcessor());

        // 2. UI Stage (Popups, Menus)
        multiplexer.addProcessor(uiStage);

        // 3. HUD Stage (On-screen controls)
        multiplexer.addProcessor(hud.getStage());

        // 4. Mouse Input for Aiming and Attack
        multiplexer.addProcessor(getMouseInputProcessor());

        Gdx.input.setInputProcessor(multiplexer);
    }

    /**
     * 鼠标输入处理器 - 处理攻击和武器切换
     */
    private com.badlogic.gdx.InputProcessor getMouseInputProcessor() {
        return new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // 控制台/暂停/游戏结束时不处理输入
                if (isConsoleOpen || isPaused || isGameOver)
                    return false;

                if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                    // 左键攻击 - 仅在鼠标模式启用时生效
                    if (!GameSettings.isUseMouseAiming())
                        return false;
                    if (player.canAttack()) {
                        player.attack();
                        performAttack();
                        if (crosshairRenderer != null) {
                            crosshairRenderer.triggerAttackFeedback();
                        }
                    }
                    return true;
                } else if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
                    // 右键切换武器 - 仅在鼠标模式启用时生效
                    if (!GameSettings.isUseMouseAiming())
                        return false;
                    player.switchWeapon();
                    AudioManager.getInstance().playSound("select");
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * 更新鼠标瞄准方向
     */
    private void updateMouseAim() {
        // 获取鼠标屏幕坐标
        float screenX = Gdx.input.getX();
        float screenY = Gdx.input.getY();

        // 转换为世界坐标
        com.badlogic.gdx.math.Vector3 worldCoords = camera
                .unproject(new com.badlogic.gdx.math.Vector3(screenX, screenY, 0));
        mouseWorldPos.set(worldCoords.x / UNIT_SCALE, worldCoords.y / UNIT_SCALE);

        // 计算玩家中心到鼠标的角度
        float playerCenterX = player.getX() + 0.5f;
        float playerCenterY = player.getY() + 0.5f;

        float dx = mouseWorldPos.x - playerCenterX;
        float dy = mouseWorldPos.y - playerCenterY;

        // 计算角度
        aimAngle = MathUtils.atan2(dy, dx) * MathUtils.radDeg;
        if (aimAngle < 0)
            aimAngle += 360;

        // 更新 playerDirection (用于动画选择)
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
     * Shared InputProcessor for toggling the console.
     * Uses keyDown (Physical Key) for reliability across OS/keyboard layouts.
     */
    private com.badlogic.gdx.InputProcessor getConsoleKeyProcessor() {
        return new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == GameSettings.KEY_CONSOLE || keycode == GameSettings.KEY_CONSOLE_ALT) {
                    toggleConsole();
                    return true; // Consume event
                }
                return false;
            }
        };
    }

    // === 主循环 ===

    @Override
    public void render(float delta) {
        // Developer Console Toggle - handled in InputProcessor for '~'/'`'
        // (GameSettings.KEY_CONSOLE)
        // F3 here as backup/alternative (aligned with GameScreen)
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_CONSOLE_ALT)) {
            toggleConsole();
        }

        if (isConsoleOpen) {
            // Clear the "just opened" flag after one frame
            if (consoleJustOpened) {
                consoleJustOpened = false;
            } else {
                // Allow closing with ESC, `, or F3 (only after first frame)
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                        Gdx.input.isKeyJustPressed(GameSettings.KEY_CONSOLE) ||
                        Gdx.input.isKeyJustPressed(GameSettings.KEY_CONSOLE_ALT)) {
                    toggleConsole();
                    consoleJustClosed = true;
                }
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

        // === 更新鼠标瞄准 (仅在鼠标模式开启时) ===
        if (GameSettings.isUseMouseAiming()) {
            updateMouseAim();
            if (crosshairRenderer != null) {
                crosshairRenderer.update(delta);
            }
        }

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

        // 更新宝箱交互
        updateChests(delta);

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
            // 非鼠标模式时根据键盘更新方向
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 1;
                aimAngle = 90f;
            }
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_DOWN)) {
            targetVy = -speed;
            hasInput = true;
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 0;
                aimAngle = 270f;
            }
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_LEFT)) {
            targetVx = -speed;
            hasInput = true;
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 2;
                aimAngle = 180f;
            }
        }
        if (Gdx.input.isKeyPressed(GameSettings.KEY_RIGHT)) {
            targetVx = speed;
            hasInput = true;
            if (!GameSettings.isUseMouseAiming()) {
                playerDirection = 3;
                aimAngle = 0f;
            }
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

        // 键盘攻击已移至鼠标输入处理器 (getMouseInputProcessor)
        // 保留键盘攻击作为备选
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_ATTACK)) {
            if (player.canAttack()) {
                player.attack();
                performAttack();
                if (crosshairRenderer != null) {
                    crosshairRenderer.triggerAttackFeedback();
                }
            }
        }
    }

    /**
     * 平滑地将玩家对齐到最近的整数格位置
     * 当玩家停止移动时调用，确保玩家不会停在两个格子之间
     */
    private void snapPlayerToGrid(float delta) {
        // 使用Player类中的统一实现，传入碰撞检测回调
        player.snapToGrid(delta, this::canPlayerMoveTo);
    }

    private void performAttack() {
        Weapon weapon = player.getCurrentWeapon();
        if (weapon == null)
            return;

        float attackRange = weapon.getRange();
        float attackRangeSq = attackRange * attackRange; // 用于快速预过滤
        float attackDamage = weapon.getDamage() + player.getDamageBonus();

        for (Enemy enemy : enemies) {
            if (enemy.isDead())
                continue;

            // 快速预过滤：用平方距离跳过明显远的敌人
            float dx = enemy.getX() - player.getX();
            float dy = enemy.getY() - player.getY();
            if (dx * dx + dy * dy > attackRangeSq)
                continue;

            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= attackRange) {
                // === 使用鼠标瞄准角度进行60度锥形攻击判定 ===
                float enemyAngle = MathUtils.atan2(dy, dx) * MathUtils.radDeg;
                if (enemyAngle < 0)
                    enemyAngle += 360;

                float angleDiff = enemyAngle - aimAngle;
                while (angleDiff > 180)
                    angleDiff -= 360;
                while (angleDiff < -180)
                    angleDiff += 360;

                // 攻击锥形半角 = 30度, 或极近距离直接命中
                if (Math.abs(angleDiff) <= 30 || dist < 0.5f) {
                    int damage = (int) attackDamage;
                    // Set damage source for blood particle direction (include knockback strength)
                    enemy.setDamageSource(player.getX(), player.getY(), player.getKnockbackMultiplier());
                    // Apply damage with damage type consideration
                    enemy.takeDamage(damage, weapon.getDamageType());

                    boolean killed = enemy.isDead();

                    // === Hit Feedback: Damage Number (伤害数值显示) ===
                    floatingTexts.add(new FloatingText(enemy.getX(), enemy.getY(), "-" + damage, Color.RED));
                    AudioManager.getInstance().playSound("hit");

                    // === Hit Feedback: Knockback ===
                    if (!killed) {
                        enemy.knockback(player.getX(), player.getY(), 2.0f, null);
                    }
                    // === Hit Feedback: Weapon Effect ===
                    enemy.applyEffect(weapon.getEffect());

                    if (killed) {
                        onEnemyKilled(enemy);
                    }
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
            Potion potion = Potion.createHealthPotion(enemy.getX(), enemy.getY());
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
        // 移除死亡敌人（同时从空间网格中移除）
        enemies.removeIf(e -> {
            if (e.isDead() && e.isRemovable()) {
                enemyGrid.remove(e);
                return true;
            }
            return false;
        });

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
                        float newX = enemy.getX() + moveX;
                        float newY = enemy.getY() + moveY;
                        enemy.setPosition(newX, newY);
                        // 更新空间网格
                        enemyGrid.update(enemy, newX, newY);
                    }
                }

                // 攻击玩家
                if (dist < 0.8f) {
                    int baseDamage = 1;
                    int damage = (int) (baseDamage * rageSystem.getEnemyDamageMultiplier());
                    if (player.damage(damage, enemy.getAttackDamageType())) {
                        // === Hit Feedback: Player Knockback + Sound ===
                        player.knockback(enemy.getX(), enemy.getY(), 1.5f);
                        AudioManager.getInstance().playSound("hit");
                    }
                }
            }
            // === Update enemy timers (knockback physics, status effects, hurt flash) ===
            enemy.updateTimers(delta);
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

        // 使用与关卡模式一致的血量（基础3HP），然后乘以波次倍率
        int baseHealth = (int) (3 * waveSystem.getEnemyHealthMultiplier());
        if (baseHealth < 1)
            baseHealth = 1; // 最少1HP
        Enemy enemy = new Enemy(spawnX, spawnY, baseHealth, DamageType.PHYSICAL, null, 0);

        // 根据生成位置的主题分配敌人类型
        String theme = EndlessModeConfig.getThemeForPosition((int) spawnX, (int) spawnY);
        enemy.setType(getEnemyTypeForTheme(theme));

        enemies.add(enemy);
        enemyGrid.insert(enemy, spawnX, spawnY); // 插入空间网格
        // 绑定溅血粒子监听器
        enemy.setDamageListener(
                (x, y, amount, dirX, dirY, knockback) -> bloodParticles.spawn(x, y, amount, dirX, dirY, knockback));
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
        enemyGrid.insert(boss, spawnX, spawnY); // 插入空间网格
        // 绑定溅血粒子监听器
        boss.setDamageListener(
                (x, y, amount, dirX, dirY, knockback) -> bloodParticles.spawn(x, y, amount, dirX, dirY, knockback));

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
     * 检查指定区块内某格子是否被墙体占用（用于渲染时的高效检查）
     * 
     * @param chunk  区块
     * @param worldX 世界坐标 X
     * @param worldY 世界坐标 Y
     * @return 是否被墙体占用
     */
    private boolean isWallAtInChunk(MapChunk chunk, int worldX, int worldY) {
        for (WallEntity wall : chunk.getWalls()) {
            // [FIX] Use collision height, not visual grid height, to determine if this tile
            // should stand as a "wall base". This allows visuals to extend over walkable
            // floor.
            if (worldX >= wall.getOriginX() && worldX < wall.getOriginX() + wall.getGridWidth() &&
                    worldY >= wall.getOriginY() && worldY < wall.getOriginY() + wall.getCollisionHeight()) {
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

        // 1. 渲染地板 (背景层)
        String currentTheme = EndlessModeConfig.getThemeForPosition((int) player.getX(), (int) player.getY());
        TextureRegion floor = getFloorTextureForTheme(currentTheme);

        for (MapChunk chunk : chunkManager.getLoadedChunks()) {
            int startX = chunk.getWorldStartX();
            int startY = chunk.getWorldStartY();
            int endX = startX + chunk.getSize();
            int endY = startY + chunk.getSize();

            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    game.getSpriteBatch().draw(floor, x * UNIT_SCALE, y * UNIT_SCALE, UNIT_SCALE, UNIT_SCALE);
                }
            }
        }

        // === Render Dust Particles (Behind entities, on top of floor) ===
        game.getSpriteBatch().end();
        dustParticles.update(Gdx.graphics.getDeltaTime());
        if (player.isMoving() && !isPaused) {
            // Spawn dust occasionally
            if (Math.random() < 0.3f) {
                // Endless mode default dirt color
                Color themeColor = new Color(0.5f, 0.45f, 0.35f, 1f);
                dustParticles.spawn(player.getX(), player.getY(), themeColor);
            }
        }
        dustParticles.render(camera.combined);
        game.getSpriteBatch().begin();

        // 2. 渲染实体 (Entities) - 玩家和敌人都在墙下层
        // 敌人
        for (Enemy e : enemies) {
            renderEnemy(e);
        }
        // 玩家
        renderPlayer();

        // 3. 渲染墙体 (Foreground/Cover)
        // 用户要求：玩家全程在墙图层下方 (被墙遮挡)
        for (MapChunk chunk : chunkManager.getLoadedChunks()) {
            for (WallEntity wall : chunk.getWalls()) {
                renderWall(wall);
            }
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
        font.getData().setScale(0.3f);
        for (FloatingText ft : floatingTexts) {
            font.setColor(ft.color);
            font.draw(game.getSpriteBatch(), ft.text, ft.x * UNIT_SCALE, ft.y * UNIT_SCALE + 16);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);

        // 6. Overlay Pass: Health Bars (Always on top of walls)
        for (Enemy e : enemies) {
            if (!e.isDead()) {
                float x = e.getX() * UNIT_SCALE;
                float y = e.getY() * UNIT_SCALE;
                float w = e.getWidth() * UNIT_SCALE;
                float h = e.getHeight() * UNIT_SCALE;
                renderHealthBar(e, x, y, w, h);
            }
        }

        // 迷雾效果
        game.getSpriteBatch().setColor(Color.WHITE);
        float pcX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float pcY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;
        fogRenderer.render(pcX, pcY, camera);

        game.getSpriteBatch().end();

        // === 渲染溅血粒子 ===
        bloodParticles.update(Gdx.graphics.getDeltaTime());
        bloodParticles.render(camera.combined);

        // === 渲染准星 (Crosshair) - 仅在鼠标模式启用时显示 ===
        if (crosshairRenderer != null && GameSettings.isUseMouseAiming() && !isPaused && !isConsoleOpen
                && !isGameOver) {
            crosshairRenderer.render(camera, mouseWorldPos.x * UNIT_SCALE, mouseWorldPos.y * UNIT_SCALE);
        }
    }

    // [Helper] Render a single wall (Full render)
    private void renderWall(WallEntity wall) {
        String theme = EndlessModeConfig.getThemeForPosition(wall.getOriginX(), wall.getOriginY());
        TextureRegion region = textureManager.getWallRegion(
                theme,
                wall.getGridWidth(),
                wall.getGridHeight(),
                wall.getOriginX(),
                wall.getOriginY());

        if (region != null) {
            float drawX = wall.getOriginX() * UNIT_SCALE;
            float drawY = wall.getOriginY() * UNIT_SCALE;
            float wallW = wall.getGridWidth() * UNIT_SCALE;
            float wallH = wall.getGridHeight() * UNIT_SCALE;

            boolean isGrassland = "grassland".equalsIgnoreCase(theme);

            if (isGrassland && region.getRegionHeight() >= 32) {
                // Split Rendering: Draw Body then Top (Visual correctness)
                int topH = 16;
                int bodyH = region.getRegionHeight() - topH;
                TextureRegion bodyReg = new TextureRegion(region, 0, topH, region.getRegionWidth(), bodyH);
                TextureRegion topReg = new TextureRegion(region, 0, 0, region.getRegionWidth(), topH);

                // Draw Body
                game.getSpriteBatch().draw(bodyReg, drawX, drawY, wallW, wallH);
                // Draw Top (at wallY + wallH)
                game.getSpriteBatch().draw(topReg, drawX, drawY + wallH, wallW, UNIT_SCALE);
            } else {
                // Standard Rendering
                float drawHeight = wallH;
                if (region.getRegionWidth() > 0) {
                    drawHeight = region.getRegionHeight() * (wallW / region.getRegionWidth());
                }
                game.getSpriteBatch().draw(region, drawX, drawY, wallW, drawHeight);
            }
        }
    }

    // [Added Helper Method] Render a single enemy - 对齐关卡模式
    private void renderEnemy(Enemy e) {
        // 使用固定尺寸，与关卡模式一致
        float drawWidth = 16f;
        float drawHeight = 16f;
        float drawX = e.getX() * UNIT_SCALE - (drawWidth - UNIT_SCALE) / 2;
        float drawY = e.getY() * UNIT_SCALE - (drawHeight - UNIT_SCALE) / 2;

        // 1. Custom Element Support
        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> enemyAnim = null;
        boolean isCustom = false;

        if (e.getCustomElementId() != null) {
            String action = e.isDead() ? "Death" : "Move";
            enemyAnim = de.tum.cit.fop.maze.custom.CustomElementManager.getInstance()
                    .getAnimation(e.getCustomElementId(), action);
            if (enemyAnim != null)
                isCustom = true;
        }

        if (enemyAnim == null) {
            enemyAnim = textureManager.getEnemyAnimation(e.getType(), e.getVelocityX(), e.getVelocityY());
        }

        TextureRegion enemyFrame = enemyAnim.getKeyFrame(stateTime, true);

        // 2. Grayscale Shader for Dead Enemies
        if (e.isDead()) {
            game.getSpriteBatch().setShader(grayscaleShader);
        }

        // 状态着色（受伤、中毒、冰冻、燃烧）
        if (e.isHurt()) {
            game.getSpriteBatch().setColor(1, 0, 0, 1); // Red flash
        } else if (e.getCurrentEffect() == WeaponEffect.POISON) {
            game.getSpriteBatch().setColor(0, 1, 0, 1); // Green tint
        } else if (e.getCurrentEffect() == WeaponEffect.FREEZE) {
            game.getSpriteBatch().setColor(0, 0.5f, 1, 1); // Blue tint
        } else if (e.getCurrentEffect() == WeaponEffect.BURN) {
            game.getSpriteBatch().setColor(1, 0.5f, 0, 1); // Orange tint
        }

        // Flip if moving left (only for custom elements)
        boolean flipX = isCustom && e.getVelocityX() < 0;

        if (flipX) {
            game.getSpriteBatch().draw(enemyFrame, drawX + drawWidth, drawY, -drawWidth, drawHeight);
        } else {
            game.getSpriteBatch().draw(enemyFrame, drawX, drawY, drawWidth, drawHeight);
        }

        // 重要：在恢复 shader 之前先恢复颜色
        game.getSpriteBatch().setColor(Color.WHITE);
        game.getSpriteBatch().setShader(null);
    }

    // [Helper] Render Health Bar - 对齐关卡模式，始终显示血条
    private void renderHealthBar(Enemy e, float x, float y, float w, float h) {
        // 使用固定尺寸，与关卡模式一致
        float drawWidth = 16f;
        float drawX = e.getX() * UNIT_SCALE - (drawWidth - UNIT_SCALE) / 2;
        float drawY = e.getY() * UNIT_SCALE - (drawWidth - UNIT_SCALE) / 2;

        float barWidth = drawWidth;
        float barHeight = 4;
        float barX = drawX;
        float barY = drawY + drawWidth + 2;

        game.getSpriteBatch().end();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

        // Background
        shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.RED);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // Health - 使用 float 除法确保正确的百分比
        float healthPercent = (float) e.getHealth() / (float) e.getMaxHealth();
        shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.GREEN);
        shapeRenderer.rect(barX, barY, barWidth * healthPercent, barHeight);

        shapeRenderer.end();
        game.getSpriteBatch().begin();
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
        // 基于速度确定方向 - 与关卡模式对齐
        int dir = 0;
        if (Math.abs(player.getVelocityY()) > Math.abs(player.getVelocityX())) {
            dir = player.getVelocityY() > 0 ? 1 : 0;
        } else if (player.getVelocityX() != 0) {
            dir = player.getVelocityX() < 0 ? 2 : 3;
        }

        boolean isMoving = player.isMoving();

        // 使用统一的 PlayerRenderer 工具类进行渲染
        // 武器渲染回调确保武器在正确的层级（玩家前/后）渲染
        playerRenderer.render(player, dir, stateTime, isMoving,
                (p, d, t) -> renderEquippedWeapon(p, d));
    }

    /**
     * 渲染玩家装备的武器精灵 (队友功能)
     */
    private void renderEquippedWeapon(Player player, int dir) {
        if (player.isDead())
            return;

        de.tum.cit.fop.maze.model.weapons.Weapon weapon = player.getCurrentWeapon();
        if (weapon == null)
            return;

        String weaponId = findCustomWeaponId(weapon.getName());
        if (weaponId == null)
            return;

        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> weaponAnim = null;
        boolean useRotation = false;

        if (player.isAttacking()) {
            // 攻击时只使用Attack动画，通过旋转处理方向
            weaponAnim = de.tum.cit.fop.maze.custom.CustomElementManager
                    .getInstance()
                    .getAnimation(weaponId, "Attack");
            useRotation = true;
        } else {
            // 待机时尝试使用方向性动画
            String directionSuffix = "";
            if (dir == 1) {
                directionSuffix = "Up";
            } else if (dir == 0) {
                directionSuffix = "Down";
            }

            if (!directionSuffix.isEmpty()) {
                weaponAnim = de.tum.cit.fop.maze.custom.CustomElementManager
                        .getInstance()
                        .getAnimation(weaponId, "Idle" + directionSuffix);
            }
            // 回退到默认Idle
            if (weaponAnim == null) {
                weaponAnim = de.tum.cit.fop.maze.custom.CustomElementManager
                        .getInstance()
                        .getAnimation(weaponId, "Idle");
            }
        }

        if (weaponAnim == null)
            return;

        TextureRegion weaponFrame = weaponAnim.getKeyFrame(stateTime, !player.isAttacking());

        float playerCenterX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float playerCenterY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;

        float weaponSize = UNIT_SCALE * 1.2f;
        float offsetX;
        float offsetY;
        float rotation = 0f;
        boolean flipX = false;

        // 根据玩家朝向调整武器位置
        switch (dir) {
            case 1: // 朝上
                offsetX = UNIT_SCALE * 0.5f;
                offsetY = UNIT_SCALE * 0.1f;
                if (useRotation)
                    rotation = 90f;
                else
                    flipX = true;
                break;
            case 0: // 朝下
                offsetX = -UNIT_SCALE * 0.4f;
                offsetY = UNIT_SCALE * 0.1f;
                if (useRotation)
                    rotation = -90f;
                break;
            case 2: // 朝左
                offsetX = -UNIT_SCALE * 0.25f;
                offsetY = UNIT_SCALE * 0.05f;
                flipX = true;
                break;
            case 3: // 朝右
            default:
                offsetX = UNIT_SCALE * 0.25f;
                offsetY = UNIT_SCALE * 0.05f;
                break;
        }

        float weaponX = playerCenterX + offsetX - weaponSize / 2;
        float weaponY = playerCenterY + offsetY - weaponSize / 2;

        if (rotation != 0f) {
            // 带旋转绘制（攻击时）
            game.getSpriteBatch().draw(weaponFrame,
                    weaponX, weaponY,
                    weaponSize / 2f, weaponSize / 2f,
                    weaponSize, weaponSize,
                    1f, 1f, rotation);
        } else if (flipX) {
            // 水平翻转绘制
            game.getSpriteBatch().draw(weaponFrame, weaponX + weaponSize, weaponY, -weaponSize, weaponSize);
        } else {
            // 正常绘制
            game.getSpriteBatch().draw(weaponFrame, weaponX, weaponY, weaponSize, weaponSize);
        }
    }

    /**
     * 根据武器名称查找自定义武器元素ID
     */
    private String findCustomWeaponId(String weaponName) {
        for (de.tum.cit.fop.maze.custom.CustomElementDefinition def : de.tum.cit.fop.maze.custom.CustomElementManager
                .getInstance().getAllElements()) {
            if (def.getType() == de.tum.cit.fop.maze.custom.ElementType.WEAPON &&
                    def.getName().equalsIgnoreCase(weaponName)) {
                return def.getId();
            }
        }
        return null;
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
            consoleJustOpened = true;
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

    // === 设置界面截图背景 ===
    private Texture settingsScreenshotTexture;

    private void showSettingsOverlay() {
        // 截取当前游戏画面作为设置界面背景
        if (settingsScreenshotTexture != null) {
            settingsScreenshotTexture.dispose();
        }
        settingsScreenshotTexture = captureScreenshot();

        // 每次重新创建设置界面以使用新截图
        if (settingsTable != null) {
            settingsTable.remove();
            if (settingsUI != null) {
                settingsUI.dispose();
            }
        }

        settingsUI = new de.tum.cit.fop.maze.ui.SettingsUI(game, uiStage, () -> {
            // On Back -> Hide settings, show pause menu
            settingsTable.setVisible(false);
            pauseTable.setVisible(true);
        });
        settingsTable = settingsUI.buildWithBackground(settingsScreenshotTexture);
        settingsTable.setVisible(true);
        settingsTable.setFillParent(true);
        uiStage.addActor(settingsTable);
        settingsTable.toFront();
    }

    /**
     * 截取当前游戏画面
     */
    private Texture captureScreenshot() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        // 读取当前帧缓冲区的像素
        byte[] pixels = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);

        // 创建 Pixmap
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        com.badlogic.gdx.utils.BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);

        // 创建纹理
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return texture;
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

    // === 宝箱更新 ===

    /**
     * 更新宝箱交互检测
     * 
     * 当玩家靠近未打开的宝箱时，自动触发交互：
     * - 普通宝箱：直接打开并领取奖励
     * - 谜题宝箱：暂停游戏并显示谜题UI（待实现）
     */
    private void updateChests(float delta) {
        if (isChestUIActive)
            return; // 正在与宝箱交互中，不处理新碰撞

        // 遍历已加载区块的宝箱
        for (MapChunk chunk : chunkManager.getLoadedChunks()) {
            String chunkId = chunk.getId();
            List<TreasureChest> chests = chunkChests.get(chunkId);
            if (chests == null)
                continue;

            for (TreasureChest chest : chests) {
                if (chest.isInteracted())
                    continue; // 已交互过

                // 检查玩家是否靠近宝箱
                float dx = player.getX() - chest.getX();
                float dy = player.getY() - chest.getY();
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < 1.2f) { // 交互半径
                    // 玩家接触到宝箱
                    if (chest.getType() == TreasureChest.ChestType.NORMAL) {
                        // 普通宝箱：直接打开
                        chest.startOpening();
                        chest.update(0.5f); // 快速完成开启动画

                        // 领取奖励（claimReward 内部会应用到玩家）
                        boolean success = chest.claimReward(player);
                        if (success && chest.getReward() != null) {
                            floatingTexts.add(new FloatingText(
                                    chest.getX(), chest.getY() + 0.5f,
                                    chest.getReward().getDisplayName(), Color.YELLOW));
                            AudioManager.getInstance().playSound("pickup");
                        }
                    } else {
                        // 谜题宝箱：暂停游戏并显示谜题UI
                        isPaused = true;
                        isChestUIActive = true;
                        activeChest = chest;

                        chestUI = new ChestInteractUI(chest, game.getSkin(), new ChestInteractUI.ChestUIListener() {
                            @Override
                            public void onChestOpened(ChestReward reward) {
                                // 领取奖励
                                if (reward != null) {
                                    reward.applyToPlayer(player);
                                    floatingTexts.add(new FloatingText(
                                            chest.getX(), chest.getY() + 0.5f,
                                            reward.getDisplayName(), Color.CYAN));
                                    AudioManager.getInstance().playSound("collect");
                                }
                                chest.startOpening();
                                chest.update(0.5f);
                            }

                            @Override
                            public void onChestFailed() {
                                // 谜题失败，给安慰奖
                                player.addCoins(1);
                                chest.setInteracted(true);
                            }

                            @Override
                            public void onUIClose() {
                                // 关闭UI，恢复游戏
                                if (chestUI != null) {
                                    chestUI.remove();
                                    chestUI = null;
                                }
                                activeChest = null;
                                isChestUIActive = false;
                                isPaused = false;
                            }
                        });

                        uiStage.addActor(chestUI);
                        GameLogger.info("EndlessGameScreen", "Puzzle chest interaction started");
                    }
                    return; // 一次只处理一个宝箱
                }
            }
        }
    }

    // === 辅助方法 ===

    /**
     * 根据主题获取对应的敌人类型 - 统一使用第一关怪物素材
     */
    private Enemy.EnemyType getEnemyTypeForTheme(String theme) {
        // 统一使用第一关的怪物素材 (BOAR)
        return Enemy.EnemyType.BOAR;
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
        if (textureManager != null)
            textureManager.dispose();
        uiStage.dispose();
        if (hud != null)
            hud.dispose();
        if (mazeRenderer != null)
            mazeRenderer.dispose();
        if (fogRenderer != null)
            fogRenderer.dispose();
        if (shapeRenderer != null)
            shapeRenderer.dispose();
        if (grayscaleShader != null)
            grayscaleShader.dispose();
        if (bloodParticles != null)
            bloodParticles.dispose();
        if (dustParticles != null)
            dustParticles.dispose();
        // 清理设置界面相关资源
        if (settingsScreenshotTexture != null)
            settingsScreenshotTexture.dispose();
        if (settingsUI != null)
            settingsUI.dispose();
    }

    private Color getThemeColor(String theme) {
        if (theme == null)
            return Color.GRAY;
        switch (theme.toLowerCase()) {
            case "grassland":
                return new Color(0.1f, 0.3f, 0.1f, 1f); // Darker Green
            case "desert":
                return new Color(0.8f, 0.7f, 0.4f, 1f); // Sand
            case "ice":
                return new Color(0.8f, 0.9f, 1.0f, 1f); // White/Blue
            case "jungle":
                return new Color(0.05f, 0.15f, 0.05f, 1f); // Very Dark Forest Green (Near Black)
            case "space":
                return new Color(0.2f, 0.1f, 0.4f, 1f); // Purple
            case "dungeon":
            default:
                return new Color(0.4f, 0.4f, 0.4f, 1f); // Gray
        }
    }
}
