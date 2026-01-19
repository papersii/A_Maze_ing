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
import de.tum.cit.fop.maze.ui.ChestInteractUI;
import de.tum.cit.fop.maze.utils.AchievementManager;
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
    private de.tum.cit.fop.maze.utils.FogRenderer fogRenderer;

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
    private int lastWeaponFacing = 3;

    public GameScreen(MazeRunnerGame game, String saveFilePath) {
        this.game = game;

        camera = new OrthographicCamera();
        gameViewport = new FitViewport(640, 360, camera);
        gameViewport.apply();

        uiStage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        textureManager = new de.tum.cit.fop.maze.utils.TextureManager(game.getAtlas());
        mazeRenderer = new de.tum.cit.fop.maze.utils.MazeRenderer(game.getSpriteBatch(), textureManager);
        fogRenderer = new de.tum.cit.fop.maze.utils.FogRenderer(game.getSpriteBatch());

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

        initGameWorld(this.currentLevelPath);

        if (loadPersistentStats) {
            GameState state = SaveManager.loadGame("auto_save_victory");
            if (state != null) {
                Player p = gameWorld.getPlayer();
                p.setLives(state.getLives());
                p.setHasKey(false);
                p.setSkillStats(state.getSkillPoints(),
                        state.getMaxHealthBonus(),
                        state.getDamageBonus(),
                        state.getInvincibilityExtension(),
                        state.getKnockbackMultiplier(),
                        state.getCooldownReduction(),
                        state.getSpeedBonus());
                p.setInventoryFromTypes(state.getInventoryWeaponTypes());
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

        if (hud != null)
            hud.dispose();
        hud = new GameHUD(game.getSpriteBatch(), gameWorld.getPlayer(), gameViewport, game.getSkin(), textureManager,
                this::togglePause);

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

        setInputProcessors();
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
        SaveManager.saveGame(state, "auto_save_victory");

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
        // 暂停游戏
        isPaused = true;
        activeChest = chest;

        // 创建并显示宝箱交互UI
        chestInteractUI = new ChestInteractUI(chest, game.getSkin(), new ChestInteractUI.ChestUIListener() {
            @Override
            public void onChestOpened(ChestReward reward) {
                // 领取奖励
                if (reward != null) {
                    reward.applyToPlayer(gameWorld.getPlayer());
                    gameWorld.getFloatingTexts().add(new FloatingText(
                            chest.getX(), chest.getY() + 0.5f,
                            reward.getDisplayName(), Color.CYAN));
                    de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("collect");
                }
                chest.startOpening();
                chest.update(0.5f);
            }

            @Override
            public void onChestFailed() {
                // 谜题失败，给安慰奖
                gameWorld.getPlayer().addCoins(1);
                chest.setInteracted(true); // 标记为已交互
            }

            @Override
            public void onUIClose() {
                // 关闭UI，恢复游戏
                if (chestInteractUI != null) {
                    chestInteractUI.remove();
                    chestInteractUI = null;
                }
                activeChest = null;
                isPaused = false;
            }
        });

        uiStage.addActor(chestInteractUI);
        GameLogger.info("GameScreen", "Puzzle chest interaction started");
    }

    // --- Main Loop ---

    @Override
    public void render(float delta) {
        // Developer Console Toggle - 用 ~ 或 F3 打开/关闭
        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE) || Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
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
            togglePause();
        }

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameViewport.apply();
        if (!isPaused) {
            // Apply time scale from developer console
            float effectiveDelta = delta * developerConsole.getTimeScale();
            gameWorld.update(effectiveDelta);
            stateTime += effectiveDelta;
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

        mazeRenderer.render(gameMap, camera, currentFloor, stateTime);
        game.getSpriteBatch().setColor(Color.WHITE); // Reset

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
        for (Enemy e : gameWorld.getEnemies()) {
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

            // === Render Health Bar (OPTIMIZED) ===
            if (!e.isDead() && e.getHealth() > 0) {
                float barWidth = 14f;
                float barHeight = 2f;
                float barX = e.getX() * UNIT_SCALE + 1f;
                float barY = e.getY() * UNIT_SCALE + 17f; // Above enemy

                // Background (dark gray)
                game.getSpriteBatch().setColor(0.2f, 0.2f, 0.2f, 0.8f);
                game.getSpriteBatch().draw(textureManager.whitePixel, barX, barY, barWidth, barHeight);

                // Shield bar (if has shield) - avoid new Color() allocation
                if (e.hasShield()) {
                    float shieldPercent = e.getShieldPercentage();
                    if (e.getShieldType() == DamageType.PHYSICAL) {
                        game.getSpriteBatch().setColor(0.3f, 0.5f, 0.8f, 1f); // Blue for physical
                    } else {
                        game.getSpriteBatch().setColor(0.7f, 0.3f, 0.9f, 1f); // Purple for magical
                    }
                    game.getSpriteBatch().draw(textureManager.whitePixel, barX, barY + barHeight,
                            barWidth * shieldPercent, barHeight);
                }

                // Health bar - avoid new Color() allocation
                float healthPercent = e.getHealthPercentage();
                game.getSpriteBatch().setColor(1f - healthPercent * 0.5f, healthPercent, 0.2f, 1f);
                game.getSpriteBatch().draw(textureManager.whitePixel, barX, barY,
                        barWidth * healthPercent, barHeight);

                game.getSpriteBatch().setColor(Color.WHITE);
            }
        }

        // 4. Mobile Traps (use legacy slime animation)
        for (MobileTrap trap : gameWorld.getMobileTraps()) {
            TextureRegion trapFrame = textureManager.enemyWalk.getKeyFrame(stateTime, true);
            game.getSpriteBatch().draw(trapFrame, trap.getX() * UNIT_SCALE, trap.getY() * UNIT_SCALE);
        }

        // 5. Floating Texts
        com.badlogic.gdx.graphics.g2d.BitmapFont font = game.getSkin().getFont("font");
        font.getData().setScale(0.5f);
        for (FloatingText ft : gameWorld.getFloatingTexts()) {
            font.setColor(ft.color);
            font.draw(game.getSpriteBatch(), ft.text, ft.x * UNIT_SCALE, ft.y * UNIT_SCALE + 16);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);

        // 6. Render Player
        renderPlayer(player);

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
                float width = 8f;
                float height = 8f;
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

        // 7. Render Fog of War (渐变迷雾效果)
        // 必须在所有游戏元素渲染完成后、batch.end() 之前渲染
        // 迷雾会覆盖在游戏画面上，但不影响 HUD
        // 注意：迷雾可见半径固定，不随相机缩放变化，防止作弊
        game.getSpriteBatch().setColor(Color.WHITE);
        float playerCenterX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float playerCenterY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;
        fogRenderer.render(playerCenterX, playerCenterY, camera);

        game.getSpriteBatch().end();

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
        TextureRegion playerFrame;
        int dir = gameWorld.getPlayerDirection();

        boolean isRunning = player.isRunning(); // Visually relevant? current code doesn't change sprite for run.
        boolean isMoving = Gdx.input.isKeyPressed(GameSettings.KEY_UP) || Gdx.input.isKeyPressed(GameSettings.KEY_DOWN)
                ||
                Gdx.input.isKeyPressed(GameSettings.KEY_LEFT) || Gdx.input.isKeyPressed(GameSettings.KEY_RIGHT);

        if (player.isAttacking()) {
            float total = player.getAttackAnimTotalDuration();
            if (total <= 0)
                total = 0.2f;
            float elapsed = total - player.getAttackAnimTimer();
            float progress = (elapsed / total) * 0.2f;

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

        Color oldC = game.getSpriteBatch().getColor();
        if (player.isDead())
            game.getSpriteBatch().setColor(0.5f, 0.5f, 0.5f, 1f);
        else if (player.isHurt())
            game.getSpriteBatch().setColor(1f, 0f, 0f, 1f);

        float drawX = player.getX() * UNIT_SCALE;
        float drawY = player.getY() * UNIT_SCALE;
        if (playerFrame.getRegionWidth() > 16) {
            drawX -= (playerFrame.getRegionWidth() - 16) / 2f;
        }

        if (player.isDead()) {
            float rotation = player.getDeathProgress() * 90f;
            game.getSpriteBatch().draw(playerFrame, drawX, drawY,
                    playerFrame.getRegionWidth() / 2f, playerFrame.getRegionHeight() / 2f,
                    playerFrame.getRegionWidth(), playerFrame.getRegionHeight(),
                    1f, 1f, rotation, false);
        } else {
            game.getSpriteBatch().draw(playerFrame, drawX, drawY);
        }
        game.getSpriteBatch().setColor(oldC);

        // === 渲染装备的武器 (队友功能) ===
        if (!player.isDead()) {
            renderEquippedWeapon(player, dir);
        }
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

        String action = player.isAttacking() ? "Attack" : "Idle";
        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> weaponAnim = de.tum.cit.fop.maze.custom.CustomElementManager
                .getInstance()
                .getAnimation(weaponId, action);

        if (weaponAnim == null)
            return;

        TextureRegion weaponFrame = weaponAnim.getKeyFrame(stateTime, !player.isAttacking());

        // 更新武器朝向记忆
        if (dir == 2 || dir == 3) {
            lastWeaponFacing = dir;
        }

        float playerCenterX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float playerCenterY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;

        float weaponSize = UNIT_SCALE * 1.2f;
        float offsetX = (lastWeaponFacing == 2) ? -UNIT_SCALE * 0.4f : UNIT_SCALE * 0.4f;

        float weaponX = playerCenterX + offsetX - weaponSize / 2;
        float weaponY = playerCenterY - weaponSize / 2;

        boolean flipX = (lastWeaponFacing == 2);
        if (flipX) {
            game.getSpriteBatch().draw(weaponFrame, weaponX + weaponSize, weaponY, -weaponSize, weaponSize);
        } else {
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

        if (mapW > viewW) {
            camera.position.x = MathUtils.clamp(camera.position.x, viewW / 2, mapW - viewW / 2);
        }
        if (mapH > viewH) {
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
            Gdx.input.setInputProcessor(uiStage);
        } else {
            consoleUI.hide();
            // 设置帧保护标志，防止同帧ESC被再次检测并触发暂停菜单
            consoleJustClosed = true;
            setInputProcessors();
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

    private void showSettingsOverlay() {
        if (settingsTable == null) {
            settingsUI = new de.tum.cit.fop.maze.ui.SettingsUI(game, uiStage, () -> {
                // On Back -> Hide settings, show pause menu
                settingsTable.setVisible(false);
                pauseTable.setVisible(true);
            });
            settingsTable = settingsUI.build();
            settingsTable.setVisible(false);
            // Center it
            settingsTable.setFillParent(true);
            uiStage.addActor(settingsTable);
        }
        settingsTable.setVisible(true);
        settingsTable.toFront();
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
                GameState s = new GameState(p.getX(), p.getY(), currentLevelPath, p.getLives(), p.hasKey());

                s.setSkillPoints(p.getSkillPoints());
                s.setMaxHealthBonus(p.getMaxHealthBonus());
                s.setDamageBonus(p.getDamageBonus());
                s.setInvincibilityExtension(p.getInvincibilityExtension());
                s.setKnockbackMultiplier(p.getKnockbackMultiplier());
                s.setCooldownReduction(p.getCooldownReduction());
                s.setSpeedBonus(p.getSpeedBonus());

                s.setInventoryWeaponTypes(p.getInventoryWeaponTypes());
                SaveManager.saveGame(s, name);
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

    private void setInputProcessors() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        if (isPaused) {
            multiplexer.addProcessor(uiStage);
        } else {
            if (hud != null)
                multiplexer.addProcessor(hud.getStage());
        }
        Gdx.input.setInputProcessor(multiplexer);
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
        if (grayscaleShader != null)
            grayscaleShader.dispose();
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
}