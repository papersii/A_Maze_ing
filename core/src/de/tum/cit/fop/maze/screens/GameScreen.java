package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.effects.FloatingText;
import de.tum.cit.fop.maze.model.*;
import de.tum.cit.fop.maze.model.items.Potion;
import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.ui.GameHUD;
import de.tum.cit.fop.maze.ui.InventoryUI;
import de.tum.cit.fop.maze.ui.ChestInteractUI;
import de.tum.cit.fop.maze.utils.AchievementManager;
import de.tum.cit.fop.maze.utils.BloodParticleSystem;
import de.tum.cit.fop.maze.utils.AchievementUnlockInfo;
import de.tum.cit.fop.maze.utils.MapLoader;
import de.tum.cit.fop.maze.utils.SaveManager;
import de.tum.cit.fop.maze.utils.GameLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Refactored GameScreen using GameWorld Model-View Separation.
 */
public class GameScreen implements Screen, GameWorld.WorldListener {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final Viewport gameViewport;
    private final Stage uiStage;
    private String currentLevelPath;

    // --- Logic / Model ---
    private GameWorld gameWorld;

    // --- View / UI ---
    private GameHUD hud;
    private de.tum.cit.fop.maze.utils.TextureManager textureManager;
    private de.tum.cit.fop.maze.utils.MazeRenderer mazeRenderer;
    private de.tum.cit.fop.maze.utils.AttackRangeRenderer attackRangeRenderer;
    private de.tum.cit.fop.maze.utils.FogRenderer fogRenderer;
    private de.tum.cit.fop.maze.utils.CrosshairRenderer crosshairRenderer;
    private de.tum.cit.fop.maze.utils.PlayerRenderer playerRenderer;
    private BloodParticleSystem bloodParticles;
    private de.tum.cit.fop.maze.utils.DustParticleSystem dustParticles;

    // --- Developer Console ---
    private de.tum.cit.fop.maze.utils.DeveloperConsole developerConsole;
    private de.tum.cit.fop.maze.ui.ConsoleUI consoleUI;
    private boolean isConsoleOpen = false;
    /** 帧保护标志：控制台刚关闭时跳过ESC暂停检测，防止同帧触发暂停菜单 */
    private boolean consoleJustClosed = false;

    private float stateTime = 0f;
    private static final float UNIT_SCALE = 16f;
    private static final float CAMERA_LERP_SPEED = 4.0f;

    private boolean isPaused = false;
    private Table pauseTable;
    // New Settings UI Overlay
    private Table settingsTable;
    private de.tum.cit.fop.maze.ui.SettingsUI settingsUI;

    private Color biomeColor = Color.WHITE;
    private com.badlogic.gdx.graphics.glutils.ShaderProgram grayscaleShader;

    // 宝箱交互UI
    private ChestInteractUI chestInteractUI;
    private TreasureChest activeChest;

    // === 玩家/武器朝向记忆 (队友功能) ===
    private int lastPlayerFacing = 3; // 记录最后水平朝向 (2=左, 3=右)

    // === 背包系统 (Inventory System) ===
    private InventorySystem inventorySystem;
    private InventoryUI inventoryUI;
    private boolean isInventoryOpen = false;

    public GameScreen(MazeRunnerGame game, String saveFilePath) {
        this.game = game;

        camera = new OrthographicCamera();
        gameViewport = new FitViewport(640, 360, camera);
        gameViewport.apply();

        uiStage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        textureManager = new de.tum.cit.fop.maze.utils.TextureManager(game.getAtlas());
        mazeRenderer = new de.tum.cit.fop.maze.utils.MazeRenderer(game.getSpriteBatch(), textureManager);
        fogRenderer = new de.tum.cit.fop.maze.utils.FogRenderer(game.getSpriteBatch());
        attackRangeRenderer = new de.tum.cit.fop.maze.utils.AttackRangeRenderer();
        playerRenderer = new de.tum.cit.fop.maze.utils.PlayerRenderer(game.getSpriteBatch(), textureManager,
                UNIT_SCALE);

        this.currentLevelPath = "maps/level-1.properties";
        boolean isLoaded = false;

        if (saveFilePath != null) {
            if (saveFilePath.endsWith(".properties")) {
                this.currentLevelPath = saveFilePath;
            } else {
                GameState state = SaveManager.loadGame(saveFilePath);
                if (state != null) {
                    loadState(state);
                    isLoaded = true;
                }
            }
        }

        if (!isLoaded) {
            initGameWorld(this.currentLevelPath);
        }

        GameSettings.resetToUserDefaults();
        setupPauseMenu();
        setupDeveloperConsole();
    }

    public GameScreen(MazeRunnerGame game, String mapPath, boolean loadPersistentStats) {
        this.game = game;
        this.currentLevelPath = mapPath;
        this.camera = new OrthographicCamera();
        this.gameViewport = new FitViewport(640, 360, camera);
        this.uiStage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        this.textureManager = new de.tum.cit.fop.maze.utils.TextureManager(game.getAtlas());
        this.mazeRenderer = new de.tum.cit.fop.maze.utils.MazeRenderer(game.getSpriteBatch(), textureManager);
        this.fogRenderer = new de.tum.cit.fop.maze.utils.FogRenderer(game.getSpriteBatch());
        this.attackRangeRenderer = new de.tum.cit.fop.maze.utils.AttackRangeRenderer();
        this.playerRenderer = new de.tum.cit.fop.maze.utils.PlayerRenderer(game.getSpriteBatch(), textureManager,
                UNIT_SCALE);

        initGameWorld(this.currentLevelPath);

        if (loadPersistentStats) {
            // Priority: Load from active save slot first
            String activeSave = game.getCurrentSaveFilePath();
            GameState state = null;

            if (activeSave != null) {
                state = SaveManager.loadGame(activeSave);
                if (state != null) {
                    GameLogger.info("GameScreen", "Loaded persistent stats from active slot: " + activeSave);
                }
            }

            // Fallback: Legacy auto-save
            if (state == null) {
                state = SaveManager.loadGame("auto_save_victory");
                if (state != null) {
                    GameLogger.info("GameScreen", "Loaded persistent stats from legacy auto_save");
                }
            }

            if (state != null) {
                Player p = gameWorld.getPlayer();
                p.setLives(state.getLives());
                p.setHasKey(false); // Key resets between levels
                p.setSkillStats(state.getSkillPoints(),
                        state.getMaxHealthBonus(),
                        state.getDamageBonus(),
                        state.getInvincibilityExtension(),
                        state.getKnockbackMultiplier(),
                        state.getCooldownReduction(),
                        state.getSpeedBonus());
                p.setInventoryFromTypes(state.getInventoryWeaponTypes());

                // Also ensure ShopManager is synced (just in case)
                de.tum.cit.fop.maze.shop.ShopManager.importState(state.getCoins(), state.getPurchasedItemIds());
            }
        }

        GameSettings.resetToUserDefaults();
        setupPauseMenu();
        setupDeveloperConsole();
    }

    private void initGameWorld(String mapPath) {
        this.currentLevelPath = mapPath;
        GameMap map = MapLoader.loadMap(mapPath);

        // Biome Logic - Theme order: 草原, 丛林, 荒漠, 冰原, 太空船
        GameLogger.info("GameScreen", "Initializing GameWorld with map: " + mapPath);
        if (mapPath.contains("level-1"))
            biomeColor = Color.WHITE; // 草原 (Grassland)
        else if (mapPath.contains("level-2"))
            biomeColor = new Color(0.6f, 0.8f, 0.6f, 1f); // 丛林 (Jungle - Dark Green)
        else if (mapPath.contains("level-3"))
            biomeColor = new Color(1f, 0.9f, 0.6f, 1f); // 荒漠 (Desert - Sand)
        else if (mapPath.contains("level-4"))
            biomeColor = new Color(0.7f, 0.9f, 1f, 1f); // 冰原 (Ice - Cyan)
        else if (mapPath.contains("level-5"))
            biomeColor = new Color(0.8f, 0.8f, 0.8f, 1f); // 太空船 (Spaceship - Grey)
        else
            biomeColor = Color.WHITE;

        this.gameWorld = new GameWorld(map, mapPath);
        this.gameWorld.setListener(this);

        // === 注册投射物命中粒子效果监听器 ===
        this.gameWorld.setProjectileHitListener((x, y, textureKey, damage, effect) -> {
            // 基于效果类型生成不同颜色的粒子
            if (effect == de.tum.cit.fop.maze.model.weapons.WeaponEffect.SLOW) {
                // Magic Wand: 蓝色爆炸粒子
                Color blueColor = new Color(0.3f, 0.5f, 1.0f, 1.0f);
                bloodParticles.spawn(x, y, 8, 0, 0, 1.5f, blueColor);
            } else if (effect == de.tum.cit.fop.maze.model.weapons.WeaponEffect.FREEZE) {
                // Ice Bow: 冰蓝色爆炸粒子
                Color iceColor = new Color(0.6f, 0.9f, 1.0f, 1.0f);
                bloodParticles.spawn(x, y, 12, 0, 0, 2.0f, iceColor);
            } else if (effect == de.tum.cit.fop.maze.model.weapons.WeaponEffect.BURN) {
                // Fire Staff: 橙红色粒子
                Color fireColor = new Color(1.0f, 0.4f, 0.1f, 1.0f);
                bloodParticles.spawn(x, y, 5, 0, 0, 0.8f, fireColor);
            }
        });

        if (hud != null)
            hud.dispose();

        // === 初始化背包系统 ===
        inventorySystem = new InventorySystem(gameWorld.getPlayer());
        inventoryUI = new InventoryUI(uiStage, game.getSkin(), textureManager, inventorySystem);
        inventoryUI.setOnCloseCallback(this::onInventoryClosed);

        hud = new GameHUD(game.getSpriteBatch(), gameWorld.getPlayer(), gameViewport, game.getSkin(), textureManager,
                this::togglePause, this::toggleInventory);

        // Find Exit for HUD
        for (GameObject obj : map.getDynamicObjects()) {
            if (obj instanceof Exit) {
                hud.setTarget(obj.getX(), obj.getY());
                break;
            }
        }

        // === Play theme-appropriate BGM ===
        // Level 20 gets special boss music, otherwise use theme-based BGM
        int levelNumber = extractLevelNumber(mapPath);
        if (levelNumber == 20) {
            de.tum.cit.fop.maze.utils.AudioManager.getInstance().playBgm(
                    de.tum.cit.fop.maze.utils.AudioManager.BGM_BOSS);
        } else {
            de.tum.cit.fop.maze.utils.AudioManager.getInstance().playThemeBgm(map.getTheme());
        }

        // === Initialize Blood Particle System ===
        if (bloodParticles == null) {
            this.bloodParticles = new BloodParticleSystem();
            this.dustParticles = new de.tum.cit.fop.maze.utils.DustParticleSystem();
            this.crosshairRenderer = new de.tum.cit.fop.maze.utils.CrosshairRenderer();
        } else {
            bloodParticles.clear();
        }
        // Wire up damage listeners
        // Wire up damage listeners (Player) [Correct Dark Blue Color]
        gameWorld.getPlayer().setDamageListener(
                (x, y, amount, dirX, dirY, knockback) -> bloodParticles.spawn(x, y, amount, dirX, dirY, knockback,
                        new Color(0.0f, 0.0f, 0.5f, 1.0f)));

        // Wire up damage listeners (Enemies)
        for (Enemy enemy : gameWorld.getEnemies()) {
            enemy.setDamageListener(
                    (x, y, amount, dirX, dirY, knockback) -> bloodParticles.spawn(x, y, amount, dirX, dirY, knockback));
        }

        setInputProcessors();
    }

    private void setInputProcessors() {
        // Gameplay Mode Input Chain
        InputMultiplexer multiplexer = new InputMultiplexer();

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
                // 控制台/暂停/背包打开时不处理游戏输入
                if (isConsoleOpen || isPaused || isInventoryOpen)
                    return false;

                if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                    // 左键攻击 - 仅在鼠标模式启用时生效
                    if (!GameSettings.isUseMouseAiming())
                        return false;
                    if (gameWorld.triggerAttack()) {
                        if (crosshairRenderer != null) {
                            crosshairRenderer.triggerAttackFeedback();
                        }
                    }
                    return true;
                } else if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
                    // 右键切换武器 - 仅在鼠标模式启用时生效
                    if (!GameSettings.isUseMouseAiming())
                        return false;
                    gameWorld.getPlayer().switchWeapon();
                    de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Shared InputProcessor for toggling the console.
     * Uses keyDown (Physical Key) instead of keyTyped (Character) for reliability.
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

    /**
     * Extract level number from map path.
     * e.g., "maps/level-5.properties" -> 5
     */
    private int extractLevelNumber(String mapPath) {
        try {
            String levelStr = mapPath.replaceAll("[^0-9]", "");
            if (!levelStr.isEmpty()) {
                return Integer.parseInt(levelStr);
            }
        } catch (Exception e) {
            GameLogger.warn("GameScreen", "Failed to extract level number from: " + mapPath);
        }
        return 1; // Default to level 1
    }

    // --- WorldListener Implementation ---

    @Override
    public void onGameOver(int killCount) {
        GameLogger.info("GameScreen", "Game Over triggered! Kill count: " + killCount);

        // Build summary data
        LevelSummaryData summaryData = new LevelSummaryData(
                LevelSummaryData.Result.DEFEAT, currentLevelPath)
                .setKillCount(gameWorld.getKillCount())
                .setCoinsCollected(gameWorld.getCoinsCollected())
                .setCompletionTime(gameWorld.getLevelElapsedTime())
                .setTookDamage(true)
                .setPlayerHP(0, gameWorld.getPlayer().getMaxHealth())
                .setNewAchievements(gameWorld.getNewAchievements());

        game.setScreen(new LevelSummaryScreen(game, summaryData));
    }

    @Override
    public void onVictory(String currentMapPath) {
        Player player = gameWorld.getPlayer();

        // Auto-save logic
        // === 修复：进入下一关时恢复满血 ===
        // 保存满血状态而非当前血量，确保下一关开始时血量恢复
        GameState state = new GameState(player.getX(), player.getY(), currentLevelPath,
                player.getMaxHealth(), player.hasKey()); // 使用 getMaxHealth() 而非 getLives()
        state.setSkillPoints(player.getSkillPoints());
        state.setMaxHealthBonus(player.getMaxHealthBonus());
        state.setDamageBonus(player.getDamageBonus());
        state.setInvincibilityExtension(player.getInvincibilityExtension());
        state.setKnockbackMultiplier(player.getKnockbackMultiplier());
        state.setCooldownReduction(player.getCooldownReduction());
        state.setSpeedBonus(player.getSpeedBonus());
        state.setInventoryWeaponTypes(player.getInventoryWeaponTypes());
        // Auto-save logic
        // Use current save context if available, otherwise fallback
        String saveFile = game.getCurrentSaveFilePath();
        if (saveFile == null)
            saveFile = "auto_save_victory.json";

        // === PERSIST SHOP STATE ===
        state.setCoins(de.tum.cit.fop.maze.shop.ShopManager.getPlayerCoins());
        state.setPurchasedItemIds(de.tum.cit.fop.maze.shop.ShopManager.getPurchasedItemIds());

        SaveManager.saveGame(state, saveFile);

        // Build summary data
        LevelSummaryData summaryData = new LevelSummaryData(
                LevelSummaryData.Result.VICTORY, currentMapPath)
                .setKillCount(gameWorld.getKillCount())
                .setCoinsCollected(gameWorld.getCoinsCollected())
                .setCompletionTime(gameWorld.getLevelElapsedTime())
                .setTookDamage(gameWorld.didPlayerTakeDamage())
                .setPlayerHP(player.getLives(), player.getMaxHealth())
                .setNewAchievements(gameWorld.getNewAchievements());

        game.setScreen(new LevelSummaryScreen(game, summaryData));
        GameLogger.info("GameScreen", "Victory achieved! Level: " + currentLevelPath);
    }

    @Override
    public void onPuzzleChestInteract(TreasureChest chest) {
        // Feature removed: No UI interaction for chests.
        // This method left empty to satisfy interface or for logging.
        GameLogger.info("GameScreen", "Chest interaction triggered (No UI).");
    }

    // --- Main Loop ---

    @Override
    public void render(float delta) {
        // Developer Console Toggle - handled in InputProcessor for '~'/'`', keeping
        // F3/GRAVE here as backup/alternative
        // Developer Console Toggle - handled in InputProcessor for '~'/'`'
        // (GameSettings.KEY_CONSOLE)
        // F3/GRAVE here as backup/alternative -> Only enabling ALT (F3) in loop to
        // avoid double-toggle with keyTyped
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_CONSOLE_ALT)) {
            toggleConsole();
        }

        // If console is open, handle console-specific input and render
        if (isConsoleOpen) {
            // ESC 专门用于关闭控制台
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                toggleConsole(); // 关闭控制台
                consoleJustClosed = true; // 防止下一帧触发暂停
            }

            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            uiStage.act(delta);
            uiStage.getViewport().apply();
            uiStage.draw();
            return;
        }

        // 控制台刚关闭的帧，跳过ESC暂停检测
        if (consoleJustClosed) {
            consoleJustClosed = false;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isInventoryOpen) {
                toggleInventory(); // 关闭背包
            } else {
                togglePause();
            }
        }

        // 背包快捷键检测
        if (Gdx.input.isKeyJustPressed(GameSettings.KEY_INVENTORY)) {
            toggleInventory();
        }

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameViewport.apply();

        // === 更新鼠标瞄准 (仅在鼠标模式开启时) ===
        if (GameSettings.isUseMouseAiming()) {
            gameWorld.updateMouseAim(camera);
            if (crosshairRenderer != null) {
                crosshairRenderer.update(delta);
            }
        }

        if (!isPaused) {
            // Apply time scale from developer console
            float effectiveDelta = delta * developerConsole.getTimeScale();
            gameWorld.update(effectiveDelta);
            stateTime += effectiveDelta;

            // === 鼠标模式下按住左键连续攻击 ===
            if (GameSettings.isUseMouseAiming() && !isConsoleOpen && !isPaused && !isInventoryOpen) {
                if (Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                    if (gameWorld.triggerAttack()) {
                        if (crosshairRenderer != null) {
                            crosshairRenderer.triggerAttackFeedback();
                        }
                    }
                }
            }

            // === Fire Staff 开火粒子效果 ===
            if (gameWorld.consumeFireEvent()) {
                // 使用橙红色火焰粒子
                Color fireColor = new Color(1.0f, 0.4f, 0.1f, 1.0f);
                bloodParticles.spawn(
                        gameWorld.getLastFireX(),
                        gameWorld.getLastFireY(),
                        5, // 中等粒子数量
                        gameWorld.getLastFireDirX(),
                        gameWorld.getLastFireDirY(),
                        0.8f, // 扩散强度
                        fireColor);
            }
        } else {
            updateCamera(0); // Still update camera if needed (e.g. initial frame)
        }

        Player player = gameWorld.getPlayer();
        GameMap gameMap = gameWorld.getGameMap();

        updateCamera(delta); // Camera follows player from World

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // 1. Render Map
        TextureRegion currentFloor = textureManager.floorRegion; // Default
        String theme = gameMap.getTheme();

        switch (theme) {
            case "Grassland":
                currentFloor = textureManager.floorGrassland;
                break;
            case "Desert":
                currentFloor = textureManager.floorDesert;
                break;
            case "Ice":
                currentFloor = textureManager.floorIce;
                break;
            case "Jungle":
                currentFloor = textureManager.floorJungle;
                break;
            case "Space":
                currentFloor = textureManager.floorSpace;
                break;
            default:
                currentFloor = textureManager.floorDungeon; // Default to Dungeon
                break;
        }

        // Apply Jungle Tint if needed - now leveraging the actual jungle texture,
        // but we can still bump the atmosphere if we want.
        // For now, let's keep it clean since we are generating a specific texture.
        game.getSpriteBatch().setColor(Color.WHITE);

        mazeRenderer.renderFloor(gameMap, camera, currentFloor);
        game.getSpriteBatch().setColor(Color.WHITE); // Reset

        game.getSpriteBatch().end();
        // === Render Dust Particles (Behind entities, on top of floor) ===
        dustParticles.update(delta);
        if (player.isMoving() && !isPaused) {
            // Spawn dust occasionally (random chance per frame)
            if (Math.random() < 0.3f) {
                String currentTheme = gameWorld.getGameMap().getTheme();
                Color themeColor = getThemeColor(currentTheme);
                dustParticles.spawn(player.getX(), player.getY(), themeColor);
            }
        }
        dustParticles.render(camera.combined);
        game.getSpriteBatch().begin();

        // 2. Render Static Dynamic Objects
        for (GameObject obj : gameMap.getDynamicObjects()) {
            if (obj instanceof Enemy || obj instanceof MobileTrap)
                continue;

            TextureRegion reg = null;
            if (obj instanceof Exit)
                reg = textureManager.exitRegion;
            else if (obj instanceof Trap) {
                // Check for animation (Overlay)
                Animation<TextureRegion> trapAnim = textureManager.getTrapAnimation(theme);
                if (trapAnim != null) {
                    // 1. Draw Static Base
                    TextureRegion base = textureManager.getTrapRegion(theme);
                    if (base != null) {
                        game.getSpriteBatch().draw(base, obj.getX() * UNIT_SCALE, obj.getY() * UNIT_SCALE,
                                UNIT_SCALE, UNIT_SCALE);
                    }
                    // 2. Draw Animation Overlay (Offset to center)
                    // We shift Y by UNIT_SCALE / 2 so the bottom of the effect starts at the
                    // geometric center
                    TextureRegion currentFrame = trapAnim.getKeyFrame(stateTime, true);
                    float overlayY = obj.getY() * UNIT_SCALE + (UNIT_SCALE / 2f);

                    game.getSpriteBatch().draw(currentFrame, obj.getX() * UNIT_SCALE, overlayY,
                            UNIT_SCALE, UNIT_SCALE);
                    continue; // Skip generic rendering for this object
                }
                reg = textureManager.getTrapRegion(theme);
            } else if (obj instanceof Key)
                reg = textureManager.keyRegion;
            else if (obj instanceof Potion)
                reg = textureManager.potionRegion;
            else if (obj instanceof Weapon) {
                reg = textureManager.keyRegion;
                game.getSpriteBatch().setColor(Color.CYAN);
            }

            if (reg != null) {
                game.getSpriteBatch().draw(reg, obj.getX() * UNIT_SCALE, obj.getY() * UNIT_SCALE, UNIT_SCALE,
                        UNIT_SCALE);
            }
            if (obj instanceof Weapon)
                game.getSpriteBatch().setColor(Color.WHITE);
        }

        // 2.5 Render Treasure Chests (底部对齐)
        for (TreasureChest chest : gameMap.getTreasureChests()) {
            TextureRegion chestTex = textureManager.getChestFrame(chest.getState());
            if (chestTex != null) {
                // 底部对齐渲染：宝箱底边与格子底边对齐
                float renderHeight = textureManager.getChestRenderHeight(chest.getState(), UNIT_SCALE);
                float drawX = chest.getX() * UNIT_SCALE;
                float drawY = chest.getY() * UNIT_SCALE; // 底边对齐
                game.getSpriteBatch().draw(chestTex, drawX, drawY,
                        UNIT_SCALE, UNIT_SCALE * renderHeight);
            }
        }

        // 3. Render Enemies with Health Bars
        // Note: Now using boar animations with directional support
        float renderRadius = de.tum.cit.fop.maze.config.GameConfig.ENTITY_RENDER_RADIUS;
        float renderRadiusSq = renderRadius * renderRadius;
        float pX = gameWorld.getPlayer().getX();
        float pY = gameWorld.getPlayer().getY();
        for (Enemy e : gameWorld.getEnemies()) {
            // 距离过滤：只渲染玩家渲染半径内的敌人
            float dx = e.getX() - pX;
            float dy = e.getY() - pY;
            if (dx * dx + dy * dy > renderRadiusSq)
                continue;

            // Get directional animation based on enemy velocity
            float vx = e.getVelocityX();
            float vy = e.getVelocityY();

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
                enemyAnim = textureManager.getEnemyAnimation(e.getType(), vx, vy);
            }

            TextureRegion currentFrame;
            if (isCustom && e.isDead()) {
                currentFrame = enemyAnim.getKeyFrame(stateTime, false);
                game.getSpriteBatch().setColor(Color.WHITE);
            } else if (e.isDead()) {
                currentFrame = enemyAnim.getKeyFrame(0); // Static frame for dead
                game.getSpriteBatch().setColor(Color.GRAY);
            } else if (e.isHurt()) {
                currentFrame = enemyAnim.getKeyFrame(stateTime, true);
                game.getSpriteBatch().setColor(1f, 0f, 0f, 1f);
            } else if (e.getCurrentEffect() == de.tum.cit.fop.maze.model.weapons.WeaponEffect.FREEZE) {
                currentFrame = enemyAnim.getKeyFrame(0); // Frozen = static
                game.getSpriteBatch().setColor(0.5f, 0.5f, 1f, 1f);
            } else if (e.getCurrentEffect() == de.tum.cit.fop.maze.model.weapons.WeaponEffect.BURN) {
                currentFrame = enemyAnim.getKeyFrame(stateTime, true);
                game.getSpriteBatch().setColor(1f, 0.5f, 0.5f, 1f);
            } else if (e.getCurrentEffect() == de.tum.cit.fop.maze.model.weapons.WeaponEffect.POISON) {
                currentFrame = enemyAnim.getKeyFrame(stateTime, true);
                game.getSpriteBatch().setColor(0.5f, 1f, 0.5f, 1f);
            } else {
                currentFrame = enemyAnim.getKeyFrame(stateTime, true);
            }

            if (e.getHealth() > 0 || e.isDead()) {
                // Grayscale for dead enemies
                if (e.isDead()) {
                    if (grayscaleShader == null) {
                        String vertexShader = "attribute vec4 "
                                + com.badlogic.gdx.graphics.glutils.ShaderProgram.POSITION_ATTRIBUTE + ";\n"
                                + "attribute vec4 " + com.badlogic.gdx.graphics.glutils.ShaderProgram.COLOR_ATTRIBUTE
                                + ";\n"
                                + "attribute vec2 " + com.badlogic.gdx.graphics.glutils.ShaderProgram.TEXCOORD_ATTRIBUTE
                                + "0;\n"
                                + "uniform mat4 u_projTrans;\n"
                                + "varying vec4 v_color;\n"
                                + "varying vec2 v_texCoords;\n"
                                + "\n"
                                + "void main()\n"
                                + "{\n"
                                + "   v_color = " + com.badlogic.gdx.graphics.glutils.ShaderProgram.COLOR_ATTRIBUTE
                                + ";\n"
                                + "   v_color.a = v_color.a * (255.0/254.0);\n"
                                + "   v_texCoords = "
                                + com.badlogic.gdx.graphics.glutils.ShaderProgram.TEXCOORD_ATTRIBUTE
                                + "0;\n"
                                + "   gl_Position =  u_projTrans * "
                                + com.badlogic.gdx.graphics.glutils.ShaderProgram.POSITION_ATTRIBUTE + ";\n"
                                + "}\n";
                        String fragmentShader = "#ifdef GL_ES\n"
                                + "precision mediump float;\n"
                                + "#endif\n"
                                + "varying vec4 v_color;\n"
                                + "varying vec2 v_texCoords;\n"
                                + "uniform sampler2D u_texture;\n"
                                + "void main()\n"
                                + "{\n"
                                + "  vec4 c = v_color * texture2D(u_texture, v_texCoords);\n"
                                + "  float gray = dot(c.rgb, vec3(0.299, 0.587, 0.114));\n"
                                + "  gl_FragColor = vec4(gray, gray, gray, c.a);\n"
                                + "}";
                        grayscaleShader = new com.badlogic.gdx.graphics.glutils.ShaderProgram(vertexShader,
                                fragmentShader);
                        if (!grayscaleShader.isCompiled()) {
                            GameLogger.error("GameScreen", "Shader compile failed: " + grayscaleShader.getLog());
                        }
                    }
                    if (grayscaleShader.isCompiled()) {
                        game.getSpriteBatch().setShader(grayscaleShader);
                    }
                }

                // Render enemy centered (scale to fit 16px tile)
                float drawWidth = 16f;
                float drawHeight = 16f;
                float drawX = e.getX() * UNIT_SCALE - (drawWidth - UNIT_SCALE) / 2;
                float drawY = e.getY() * UNIT_SCALE - (drawHeight - UNIT_SCALE) / 2;

                // Flip if moving left, BUT ONLY for custom elements (Standard mobs have
                // directional sprites)
                boolean flipX = isCustom && e.getVelocityX() < 0;

                if (flipX) {
                    game.getSpriteBatch().draw(currentFrame, drawX + drawWidth, drawY, -drawWidth, drawHeight);
                } else {
                    game.getSpriteBatch().draw(currentFrame, drawX, drawY, drawWidth, drawHeight);
                }

                if (e.isDead()) {
                    game.getSpriteBatch().setShader(null);
                }
            }
            game.getSpriteBatch().setColor(Color.WHITE);

        }

        // 4. Mobile Traps (use legacy slime animation)
        for (MobileTrap trap : gameWorld.getMobileTraps()) {
            TextureRegion trapFrame = textureManager.enemyWalk.getKeyFrame(stateTime, true);
            game.getSpriteBatch().draw(trapFrame, trap.getX() * UNIT_SCALE, trap.getY() * UNIT_SCALE);
        }

        // 6. Render Player
        renderPlayer(player);

        // 6.1 Render Attack Range Indicator (攻击范围可视化)
        if (player.isAttacking() && GameSettings.isShowAttackRange()) {
            game.getSpriteBatch().end(); // 暂停 SpriteBatch 以使用 ShapeRenderer
            Weapon currentWeapon = player.getCurrentWeapon();
            if (currentWeapon != null && !currentWeapon.isRanged()) {
                float total = player.getAttackAnimTotalDuration();
                if (total <= 0)
                    total = 0.2f;
                float elapsed = total - player.getAttackAnimTimer();
                float progress = elapsed / total;
                // 使用统一的 getAttackAngle() 方法，支持鼠标和8向键盘攻击
                attackRangeRenderer.render(camera, player.getX(), player.getY(),
                        gameWorld.getAttackAngle(), currentWeapon.getRange(),
                        currentWeapon.isRanged(), progress);
            }
            game.getSpriteBatch().begin(); // 恢复 SpriteBatch
            game.getSpriteBatch().setProjectionMatrix(camera.combined);
        }

        // 6.5 Render Projectiles (队友功能: 弹道渲染)
        for (de.tum.cit.fop.maze.model.Projectile p : gameWorld.getProjectiles()) {
            TextureRegion projRegion = null;
            String key = p.getTextureKey();

            // 1. Try Custom Element (by ID)
            if (key != null) {
                com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = de.tum.cit.fop.maze.custom.CustomElementManager
                        .getInstance()
                        .getAnimation(key, "Projectile");
                if (anim != null) {
                    projRegion = anim.getKeyFrame(stateTime, true);
                }
            }

            // 2. Fallback to generic textures
            if (projRegion == null && key != null) {
                String lowerKey = key.toLowerCase();
                if (lowerKey.contains("crossbow") || lowerKey.contains("arrow")) {
                    projRegion = textureManager.arrowRegion;
                } else if (lowerKey.contains("wand") || lowerKey.contains("magic")) {
                    projRegion = textureManager.arrowRegion;
                    game.getSpriteBatch().setColor(Color.CYAN);
                } else {
                    projRegion = textureManager.arrowRegion;
                }
            }

            if (projRegion != null) {
                float baseWidth = 8f;
                float baseHeight = 8f;
                float width = baseWidth * p.getSize();
                float height = baseHeight * p.getSize();
                float rotation = p.getRotation() * com.badlogic.gdx.math.MathUtils.radDeg;

                game.getSpriteBatch().draw(projRegion,
                        p.getX() * UNIT_SCALE, p.getY() * UNIT_SCALE,
                        width / 2, height / 2,
                        width, height,
                        1f, 1f,
                        rotation);

                game.getSpriteBatch().setColor(Color.WHITE);
            }
        }

        // 6.8 Render Walls (Strict Layering: Always above players)
        game.getSpriteBatch().setColor(Color.WHITE); // Defensive reset
        game.getSpriteBatch().setShader(null); // Defensive reset
        mazeRenderer.renderWalls(gameMap, camera, stateTime);

        // 6.9 UI Overlay Pass (Health Bars & Floating Texts - Always on top of Walls)
        // 1. Health Bars
        for (Enemy e : gameWorld.getEnemies()) {
            renderRadius = de.tum.cit.fop.maze.config.GameConfig.ENTITY_RENDER_RADIUS;
            float dx = e.getX() - gameWorld.getPlayer().getX();
            float dy = e.getY() - gameWorld.getPlayer().getY();
            if (dx * dx + dy * dy > renderRadius * renderRadius)
                continue;

            if (!e.isDead() && e.getHealth() > 0) {
                float barWidth = 14f;
                float barHeight = 2f;
                float barX = e.getX() * UNIT_SCALE + 1f;
                float barY = e.getY() * UNIT_SCALE + 17f; // Above enemy

                game.getSpriteBatch().setColor(0.2f, 0.2f, 0.2f, 0.8f);
                game.getSpriteBatch().draw(textureManager.whitePixel, barX, barY, barWidth, barHeight);

                if (e.hasShield()) {
                    float shieldPercent = e.getShieldPercentage();
                    if (e.getShieldType() == DamageType.PHYSICAL) {
                        game.getSpriteBatch().setColor(0.3f, 0.5f, 0.8f, 1f);
                    } else {
                        game.getSpriteBatch().setColor(0.7f, 0.3f, 0.9f, 1f);
                    }
                    game.getSpriteBatch().draw(textureManager.whitePixel, barX, barY + barHeight,
                            barWidth * shieldPercent, barHeight);
                }

                float healthPercent = e.getHealthPercentage();
                game.getSpriteBatch().setColor(1f - healthPercent * 0.5f, healthPercent, 0.2f, 1f);
                game.getSpriteBatch().draw(textureManager.whitePixel, barX, barY,
                        barWidth * healthPercent, barHeight);

                game.getSpriteBatch().setColor(Color.WHITE);
            }
        }

        // 2. Floating Texts
        com.badlogic.gdx.graphics.g2d.BitmapFont font = game.getSkin().getFont("font");
        font.getData().setScale(0.3f);
        for (FloatingText ft : gameWorld.getFloatingTexts()) {
            font.setColor(ft.color);
            font.draw(game.getSpriteBatch(), ft.text, ft.x * UNIT_SCALE, ft.y * UNIT_SCALE + 16);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);

        // 7. Render Fog of War (渐变迷雾效果)
        // 必须在所有游戏元素渲染完成后、batch.end() 之前渲染
        // 迷雾会覆盖在游戏画面上，但不影响 HUD
        // 注意：迷雾可见半径固定，不随相机缩放变化，防止作弊
        game.getSpriteBatch().setColor(Color.WHITE);
        float playerCenterX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float playerCenterY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;
        fogRenderer.render(playerCenterX, playerCenterY, camera);

        game.getSpriteBatch().end();

        // === Render Blood Particles (after main batch, before HUD) ===
        bloodParticles.update(delta);
        bloodParticles.render(camera.combined);

        // === 渲染准星 (Crosshair) - 仅在鼠标模式启用时显示 ===
        if (crosshairRenderer != null && GameSettings.isUseMouseAiming() && !isPaused && !isConsoleOpen
                && !isInventoryOpen) {
            com.badlogic.gdx.math.Vector2 mousePos = gameWorld.getMouseWorldPos();
            crosshairRenderer.render(camera, mousePos.x * UNIT_SCALE, mousePos.y * UNIT_SCALE);
        }

        hud.getStage().getViewport().apply();
        hud.update(delta);
        hud.render();

        // === NEW: Display achievement popups ===
        List<String> newAchievements = gameWorld.getAndClearNewAchievements();
        for (String achievementName : newAchievements) {
            AchievementUnlockInfo info = AchievementManager.getAchievementInfo(achievementName);
            hud.showAchievement(info);
            // Add gold reward to player
            player.addCoins(info.getGoldReward());
            GameLogger.info("GameScreen",
                    "Achievement popup: " + achievementName + " +" + info.getGoldReward() + " gold");
        }

        if (isPaused) {
            uiStage.act(delta);
            uiStage.getViewport().apply();
            uiStage.draw();
        }
    }

    private void renderPlayer(Player player) {
        int dir = gameWorld.getPlayerDirection();
        boolean isMoving = !isPaused && (Gdx.input.isKeyPressed(GameSettings.KEY_UP)
                || Gdx.input.isKeyPressed(GameSettings.KEY_DOWN)
                || Gdx.input.isKeyPressed(GameSettings.KEY_LEFT) || Gdx.input.isKeyPressed(GameSettings.KEY_RIGHT));

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
        Player player = gameWorld.getPlayer();
        GameMap gameMap = gameWorld.getGameMap();

        float targetX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float targetY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;
        camera.position.x += (targetX - camera.position.x) * CAMERA_LERP_SPEED * delta;
        camera.position.y += (targetY - camera.position.y) * CAMERA_LERP_SPEED * delta;

        float mapW = gameMap.getWidth() * UNIT_SCALE;
        float mapH = gameMap.getHeight() * UNIT_SCALE;
        float viewW = camera.viewportWidth * camera.zoom;
        float viewH = camera.viewportHeight * camera.zoom;

        // 当视野大于地图时居中相机，否则 clamp 到地图边界
        if (mapW <= viewW) {
            camera.position.x = mapW / 2;
        } else {
            camera.position.x = MathUtils.clamp(camera.position.x, viewW / 2, mapW - viewW / 2);
        }
        if (mapH <= viewH) {
            camera.position.y = mapH / 2;
        } else {
            camera.position.y = MathUtils.clamp(camera.position.y, viewH / 2, mapH - viewH / 2);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.Z))
            GameSettings.cameraZoom -= 0.02f;
        if (Gdx.input.isKeyPressed(Input.Keys.X))
            GameSettings.cameraZoom += 0.02f;
        GameSettings.cameraZoom = MathUtils.clamp(GameSettings.cameraZoom, 0.2f, 2.0f);

        camera.zoom = GameSettings.cameraZoom;
        camera.update();
    }

    // --- Utils ---

    private void loadState(GameState state) {
        initGameWorld(state.getCurrentLevel());
        Player player = gameWorld.getPlayer();
        player.setPosition(state.getPlayerX(), state.getPlayerY());
        player.setLives(state.getLives());
        player.setHasKey(state.isHasKey());

        player.setSkillStats(state.getSkillPoints(),
                state.getMaxHealthBonus(),
                state.getDamageBonus(),
                state.getInvincibilityExtension(),
                state.getKnockbackMultiplier(),
                state.getCooldownReduction(),
                state.getSpeedBonus());
        player.setInventoryFromTypes(state.getInventoryWeaponTypes());

        // === RESTORE GLOBAL SHOP STATE FOR THIS SAVE SLOT ===
        de.tum.cit.fop.maze.shop.ShopManager.importState(state.getCoins(), state.getPurchasedItemIds());
    }

    private void saveGameContext(String filename) {
        Player p = gameWorld.getPlayer();
        GameState s = new GameState(p.getX(), p.getY(), currentLevelPath, p.getLives(), p.hasKey());

        s.setSkillPoints(p.getSkillPoints());
        s.setMaxHealthBonus(p.getMaxHealthBonus());
        s.setDamageBonus(p.getDamageBonus());
        s.setInvincibilityExtension(p.getInvincibilityExtension());
        s.setKnockbackMultiplier(p.getKnockbackMultiplier());
        s.setCooldownReduction(p.getCooldownReduction());
        s.setSpeedBonus(p.getSpeedBonus());
        s.setInventoryWeaponTypes(p.getInventoryWeaponTypes());

        // === PERSIST SHOP STATE ===
        s.setCoins(de.tum.cit.fop.maze.shop.ShopManager.getPlayerCoins());
        s.setPurchasedItemIds(de.tum.cit.fop.maze.shop.ShopManager.getPurchasedItemIds());

        SaveManager.saveGame(s, filename);
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

        addMenuButton("Resume", this::togglePause);
        addMenuButton("Settings", () -> {
            pauseTable.setVisible(false);
            showSettingsOverlay();
        });
        addMenuButton("Save Game", () -> {
            pauseTable.setVisible(false);
            showSaveDialog();
        });
        addMenuButton("Load Game", () -> {
            pauseTable.setVisible(false);
            showLoadDialog();
        });
        // Important: Dispose current screen when going to menu?
        // Typically MazeRunnerGame manages screens.
        addMenuButton("Main Menu", game::goToMenu);

        pauseTable.setVisible(false);
        uiStage.addActor(pauseTable);
    }

    /**
     * 初始化开发者控制台
     */
    private void setupDeveloperConsole() {
        developerConsole = new de.tum.cit.fop.maze.utils.DeveloperConsole();
        consoleUI = new de.tum.cit.fop.maze.ui.ConsoleUI(uiStage, game.getSkin());
        consoleUI.setCloseCallback(this::toggleConsole);
        consoleUI.setConsole(developerConsole);
        developerConsole.setGameWorld(gameWorld);

        // 设置关卡控制监听器，实现 level/restart/skip/win 命令
        developerConsole.setLevelChangeListener(new de.tum.cit.fop.maze.utils.DeveloperConsole.LevelChangeListener() {
            @Override
            public void onLevelChange(int levelNumber) {
                // 跳转到指定关卡
                String newLevelPath = "maps/level-" + levelNumber + ".properties";
                toggleConsole(); // 关闭控制台
                initGameWorld(newLevelPath);
                setupDeveloperConsole(); // 重新初始化控制台
            }

            @Override
            public void onRestart() {
                // 重新开始当前关卡
                toggleConsole();
                initGameWorld(currentLevelPath);
                setupDeveloperConsole();
            }

            @Override
            public void onSkip() {
                // 跳到下一关
                int currentLevel = 1;
                try {
                    String levelStr = currentLevelPath.replaceAll("[^0-9]", "");
                    if (!levelStr.isEmpty()) {
                        currentLevel = Integer.parseInt(levelStr);
                    }
                } catch (Exception e) {
                    currentLevel = 1;
                }
                int nextLevel = Math.min(currentLevel + 1, 5);
                String newLevelPath = "maps/level-" + nextLevel + ".properties";
                toggleConsole();
                initGameWorld(newLevelPath);
                setupDeveloperConsole();
            }

            @Override
            public void onWin() {
                // Directly trigger victory
                toggleConsole();
                onVictory(currentLevelPath);
            }
        });
    }

    /**
     * 切换开发者控制台显示状态
     */
    private void toggleConsole() {
        isConsoleOpen = !isConsoleOpen;
        if (isConsoleOpen) {
            consoleUI.show();
            // Update game world reference in case it changed
            developerConsole.setGameWorld(gameWorld);

            // Console Input Mode:
            // 1. Global Keys (to Close Console)
            // 2. UI Stage (to Type commands)
            // Note: HUD and Player Control are excluded
            InputMultiplexer consoleMultiplexer = new InputMultiplexer();
            consoleMultiplexer.addProcessor(getConsoleKeyProcessor());
            consoleMultiplexer.addProcessor(uiStage);

            Gdx.input.setInputProcessor(consoleMultiplexer);
        } else {
            consoleUI.hide();
            // 设置帧保护标志，防止同帧ESC被再次检测并触发暂停菜单
            consoleJustClosed = true;
            setInputProcessors(); // Restore Gameplay Chain
        }
    }

    private void addMenuButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, game.getSkin());
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        pauseTable.add(btn).width(200).padBottom(20).row();
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

    // --- Popups (Keep logic in Screen for now) ---
    private void showLoadDialog() {
        // (Copied existing logic mostly as is, but adapting SaveManager usage)
        Window win = new Window("Select Save File", game.getSkin());
        win.setModal(true);
        win.setResizable(true);
        win.getTitleLabel().setAlignment(Align.center);
        FileHandle[] files = SaveManager.getSaveFiles();
        Table listTable = new Table();
        listTable.top();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        if (files.length == 0)
            listTable.add(new Label("No save files found.", game.getSkin())).pad(20);
        else {
            for (FileHandle file : files) {
                Table rowTable = new Table();
                String dateStr = sdf.format(new Date(file.lastModified()));
                TextButton loadBtn = new TextButton(file.nameWithoutExtension() + "\n" + dateStr, game.getSkin());
                loadBtn.getLabel().setFontScale(0.8f);
                loadBtn.getLabel().setAlignment(Align.left);
                loadBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        GameState s = SaveManager.loadGame(file.name());
                        if (s != null) {
                            loadState(s);
                            win.remove();
                            togglePause();
                        }
                    }
                });
                TextButton deleteBtn = new TextButton("X", game.getSkin());
                deleteBtn.setColor(Color.RED);
                deleteBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        SaveManager.deleteSave(file.name());
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

        // Auto-focus scroll on hover so user doesn't need to click
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

    private void showSaveDialog() {
        Window win = new Window("Save Game As...", game.getSkin());
        win.setModal(true);
        win.getTitleLabel().setAlignment(Align.center);
        TextField nameField = new TextField("MySave", game.getSkin());
        GameLogger.debug("GameScreen", "Opening Save Dialog");
        Table content = new Table();
        content.add(new Label("Name:", game.getSkin())).padRight(10);
        content.add(nameField).growX();
        win.add(content).growX().pad(20).row();
        Table btns = new Table();
        TextButton saveBtn = new TextButton("Save", game.getSkin());
        TextButton cancelBtn = new TextButton("Cancel", game.getSkin());
        btns.add(saveBtn).width(80).padRight(10);
        btns.add(cancelBtn).width(80);
        win.add(btns).padBottom(10);

        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText();
                if (name.isEmpty())
                    name = "unnamed";
                Player p = gameWorld.getPlayer();
                saveGameContext(name);
                GameLogger.info("GameScreen", "Game saved as: " + name);
                win.remove();
                pauseTable.setVisible(true);
            }
        });
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                win.remove();
                pauseTable.setVisible(true);
            }
        });
        win.pack();
        win.setWidth(Math.max(uiStage.getWidth() * 0.4f, 300));
        win.setPosition(uiStage.getWidth() / 2 - win.getWidth() / 2, uiStage.getHeight() / 2 - win.getHeight() / 2);
        uiStage.addActor(win);
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseTable.setVisible(isPaused);
        setInputProcessors();
    }

    /**
     * 切换背包界面显示状态
     */
    private void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryUI.setVisible(isInventoryOpen);
        if (isInventoryOpen) {
            isPaused = true; // 打开背包时暂停游戏
        } else {
            isPaused = false;
        }
    }

    /**
     * 背包关闭回调
     */
    private void onInventoryClosed() {
        isInventoryOpen = false;
        isPaused = false;
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        uiStage.getViewport().update(width, height, true);
        if (hud != null)
            hud.resize(width, height);
    }

    @Override
    public void dispose() {
        if (textureManager != null)
            textureManager.dispose();
        uiStage.dispose();
        if (hud != null)
            hud.dispose();
        if (fogRenderer != null)
            fogRenderer.dispose();
        if (attackRangeRenderer != null)
            attackRangeRenderer.dispose();
        if (grayscaleShader != null)
            grayscaleShader.dispose();
        if (dustParticles != null)
            dustParticles.dispose();
        // 清理设置界面相关资源
        if (settingsScreenshotTexture != null)
            settingsScreenshotTexture.dispose();
        if (settingsUI != null)
            settingsUI.dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void show() {
        if (isPaused)
            togglePause();
        else
            setInputProcessors();
    }

    @Override
    public void hide() {
    }

    /**
     * Get the game world for external access (e.g., armor selection screen)
     */
    public GameWorld getGameWorld() {
        return gameWorld;
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