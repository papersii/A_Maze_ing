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
        addPauseButton("Save Game", this::saveEndlessGame);
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

        // 更新核心系统
        comboSystem.update(delta);
        rageSystem.update(totalKills, waveSystem.getSurvivalTime());
        waveSystem.update(delta);

        // 更新玩家输入
        updatePlayerInput(delta);
        // 不调用player.update(delta, collisionManager)，因为无尽模式没有GameMap

        // 更新区块加载
        chunkManager.updateActiveChunks(player.getX(), player.getY());

        // 更新敌人
        updateEnemies(delta);

        // 更新浮动文字
        updateFloatingTexts(delta);

        // 更新HUD数据
        hud.setTotalKills(totalKills);
        hud.setCurrentScore(currentScore);
        hud.setCurrentZone(EndlessModeConfig.getThemeForPosition(
                (int) player.getX(), (int) player.getY()));

        // 检查游戏结束
        if (player.getLives() <= 0 && !player.isDead()) {
            triggerGameOver();
        }
    }

    private void updatePlayerInput(float delta) {
        float speed = player.getSpeed();

        float targetVx = 0;
        float targetVy = 0;

        if (Gdx.input.isKeyPressed(GameSettings.KEY_UP))
            targetVy = speed;
        if (Gdx.input.isKeyPressed(GameSettings.KEY_DOWN))
            targetVy = -speed;
        if (Gdx.input.isKeyPressed(GameSettings.KEY_LEFT))
            targetVx = -speed;
        if (Gdx.input.isKeyPressed(GameSettings.KEY_RIGHT))
            targetVx = speed;

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

        // 攻击
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_ATTACK)) {
            player.attack();
            performAttack();
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
        // 生命药水掉落
        if (spawnRandom.nextFloat() < EndlessModeConfig.HEALTH_POTION_DROP_RATE) {
            // TODO: 实现药水掉落
        }
    }

    private void updateEnemies(float delta) {
        // 移除死亡敌人
        enemies.removeIf(e -> e.isDead() && e.isRemovable());

        // 更新存活敌人 - 简化逻辑，手动移动敌人
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                // 简化的敌人AI - 追踪玩家
                float dx = player.getX() - enemy.getX();
                float dy = player.getY() - enemy.getY();
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < 30 && dist > 0.5f) {
                    // 追踪玩家，应用RAGE速度加成
                    float speed = GameSettings.enemyChaseSpeed * rageSystem.getEnemySpeedMultiplier() * delta;
                    float moveX = (dx / dist) * speed;
                    float moveY = (dy / dist) * speed;

                    // 使用setPosition移动敌人
                    enemy.setPosition(enemy.getX() + moveX, enemy.getY() + moveY);
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

        // 渲染敌人
        TextureRegion enemyFrame = textureManager.enemyWalk.getKeyFrame(stateTime, true);
        for (Enemy e : enemies) {
            if (e.isDead()) {
                game.getSpriteBatch().setColor(Color.GRAY);
            } else if (e.isHurt()) {
                game.getSpriteBatch().setColor(Color.RED);
            }
            game.getSpriteBatch().draw(enemyFrame, e.getX() * UNIT_SCALE, e.getY() * UNIT_SCALE);
            game.getSpriteBatch().setColor(Color.WHITE);
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
