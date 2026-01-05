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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.effects.FloatingText;
import de.tum.cit.fop.maze.model.*;
import de.tum.cit.fop.maze.model.items.Potion;
import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.ui.GameHUD;
import de.tum.cit.fop.maze.utils.MapLoader;
import de.tum.cit.fop.maze.utils.SaveManager;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    private float stateTime = 0f;
    private static final float UNIT_SCALE = 16f;
    private static final float CAMERA_LERP_SPEED = 4.0f;

    private boolean isPaused = false;
    private Table pauseTable;

    private Color biomeColor = Color.WHITE;

    public GameScreen(MazeRunnerGame game, String saveFilePath) {
        this.game = game;

        camera = new OrthographicCamera();
        gameViewport = new FitViewport(640, 360, camera);
        gameViewport.apply();

        uiStage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        textureManager = new de.tum.cit.fop.maze.utils.TextureManager(game.getAtlas());
        mazeRenderer = new de.tum.cit.fop.maze.utils.MazeRenderer(game.getSpriteBatch(), textureManager);

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
    }

    public GameScreen(MazeRunnerGame game, String mapPath, boolean loadPersistentStats) {
        this.game = game;
        this.currentLevelPath = mapPath;
        this.camera = new OrthographicCamera();
        this.gameViewport = new FitViewport(640, 360, camera);
        this.uiStage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        this.textureManager = new de.tum.cit.fop.maze.utils.TextureManager(game.getAtlas());
        this.mazeRenderer = new de.tum.cit.fop.maze.utils.MazeRenderer(game.getSpriteBatch(), textureManager);

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
    }

    private void initGameWorld(String mapPath) {
        this.currentLevelPath = mapPath;
        GameMap map = MapLoader.loadMap(mapPath);

        // Biome Logic
        if (mapPath.contains("level-1"))
            biomeColor = Color.WHITE;
        else if (mapPath.contains("level-2"))
            biomeColor = new Color(1f, 0.9f, 0.6f, 1f);
        else if (mapPath.contains("level-3"))
            biomeColor = new Color(0.7f, 0.9f, 1f, 1f);
        else if (mapPath.contains("level-4"))
            biomeColor = new Color(0.6f, 0.8f, 0.6f, 1f);
        else if (mapPath.contains("level-5"))
            biomeColor = new Color(0.8f, 0.8f, 0.8f, 1f);
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

        setInputProcessors();
    }

    // --- WorldListener Implementation ---

    @Override
    public void onGameOver(int killCount) {
        System.out.println("Transitioning to GameOver.");
        game.setScreen(new GameOverScreen(game, killCount));
    }

    @Override
    public void onVictory(String currentMapPath) {
        // Auto-save logic
        Player player = gameWorld.getPlayer();
        GameState state = new GameState(player.getX(), player.getY(), currentLevelPath,
                player.getLives(), player.hasKey());
        state.setSkillPoints(player.getSkillPoints());
        state.setMaxHealthBonus(player.getMaxHealthBonus());
        state.setDamageBonus(player.getDamageBonus());
        state.setInvincibilityExtension(player.getInvincibilityExtension());
        state.setKnockbackMultiplier(player.getKnockbackMultiplier());
        state.setCooldownReduction(player.getCooldownReduction());
        state.setSpeedBonus(player.getSpeedBonus());
        state.setInventoryWeaponTypes(player.getInventoryWeaponTypes()); // Save inventory too
        SaveManager.saveGame(state, "auto_save_victory");

        game.setScreen(new VictoryScreen(game, currentLevelPath));
    }

    // --- Main Loop ---

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            togglePause();

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameViewport.apply();
        if (!isPaused) {
            gameWorld.update(delta);
            stateTime += delta;
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
        if (currentLevelPath.contains("level-1"))
            currentFloor = textureManager.floorDungeon;
        else if (currentLevelPath.contains("level-2"))
            currentFloor = textureManager.floorDesert;
        else if (currentLevelPath.contains("level-3"))
            currentFloor = textureManager.floorIce; // Winter/Ice
        else if (currentLevelPath.contains("level-4"))
            currentFloor = textureManager.floorForest;
        else if (currentLevelPath.contains("level-5"))
            currentFloor = textureManager.floorSpace; // Space Ship

        mazeRenderer.render(gameMap, camera, currentFloor);

        // 2. Render Static Dynamic Objects
        for (GameObject obj : gameMap.getDynamicObjects()) {
            if (obj instanceof Enemy || obj instanceof MobileTrap)
                continue;

            TextureRegion reg = null;
            if (obj instanceof Exit)
                reg = textureManager.exitRegion;
            else if (obj instanceof Trap)
                reg = textureManager.trapRegion;
            else if (obj instanceof Key)
                reg = textureManager.keyRegion;
            else if (obj instanceof Potion)
                reg = textureManager.potionRegion;
            else if (obj instanceof Weapon) {
                reg = textureManager.keyRegion;
                game.getSpriteBatch().setColor(Color.CYAN);
            }

            if (reg != null) {
                game.getSpriteBatch().draw(reg, obj.getX() * UNIT_SCALE, obj.getY() * UNIT_SCALE);
            }
            if (obj instanceof Weapon)
                game.getSpriteBatch().setColor(Color.WHITE);
        }

        // 3. Render Enemies
        TextureRegion animatedFrame = textureManager.enemyWalk.getKeyFrame(stateTime, true);
        TextureRegion staticFrame = textureManager.enemyWalk.getKeyFrame(0);

        for (Enemy e : gameWorld.getEnemies()) {
            TextureRegion currentFrame = animatedFrame;
            if (e.isDead()) {
                currentFrame = staticFrame;
                game.getSpriteBatch().setColor(Color.GRAY);
            } else if (e.isHurt()) {
                game.getSpriteBatch().setColor(1f, 0f, 0f, 1f);
            } else if (e.getCurrentEffect() == de.tum.cit.fop.maze.model.weapons.WeaponEffect.FREEZE) {
                game.getSpriteBatch().setColor(0.5f, 0.5f, 1f, 1f);
            } else if (e.getCurrentEffect() == de.tum.cit.fop.maze.model.weapons.WeaponEffect.BURN) {
                game.getSpriteBatch().setColor(1f, 0.5f, 0.5f, 1f);
            } else if (e.getCurrentEffect() == de.tum.cit.fop.maze.model.weapons.WeaponEffect.POISON) {
                game.getSpriteBatch().setColor(0.5f, 1f, 0.5f, 1f);
            }

            if (e.getHealth() > 0 || e.isDead()) {
                game.getSpriteBatch().draw(currentFrame, e.getX() * UNIT_SCALE, e.getY() * UNIT_SCALE);
            }
            game.getSpriteBatch().setColor(Color.WHITE);
        }

        // 4. Mobile Traps
        for (MobileTrap trap : gameWorld.getMobileTraps()) {
            game.getSpriteBatch().draw(animatedFrame, trap.getX() * UNIT_SCALE, trap.getY() * UNIT_SCALE);
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

        game.getSpriteBatch().end();

        hud.getStage().getViewport().apply();
        hud.update(delta);
        hud.render();

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
            game.setScreen(new SettingsScreen(game, this));
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
}