package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.utils.SaveManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import de.tum.cit.fop.maze.utils.GameLogger;
import de.tum.cit.fop.maze.utils.UIUtils;

public class MenuScreen implements Screen {

    private final Stage stage;
    private final MazeRunnerGame game;
    private final Texture backgroundTexture;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;
        var camera = new OrthographicCamera();

        // 加载背景纹理
        backgroundTexture = new Texture(Gdx.files.internal("images/menu_background.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Viewport viewport = new FitViewport(1920, 1080, camera);
        stage = new Stage(viewport, game.getSpriteBatch());

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 1. Title
        table.add(new Label("A-mazeing", game.getSkin(), "title")).padBottom(60).row();

        // 2. "New Game" Button
        TextButton newGameButton = new TextButton("New Game", game.getSkin());
        table.add(newGameButton).width(300).height(60).padBottom(20).row();

        newGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "New Game clicked");
                showNewGameDialog();
            }
        });

        // 3. "Select Level" Button
        TextButton selectLevelButton = new TextButton("Select Level", game.getSkin());
        table.add(selectLevelButton).width(300).height(60).padBottom(20).row();

        selectLevelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LevelSelectScreen(game));
            }
        });

        // 4. "Endless Mode" Button (原 Random Map 位置)
        TextButton endlessButton = new TextButton("Endless Mode", game.getSkin());
        endlessButton.setColor(1f, 0.8f, 0.3f, 1f); // 金黄色高亮
        table.add(endlessButton).width(300).height(60).padBottom(20).row();

        endlessButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Endless Mode clicked");
                // Use LoadingScreen for endless mode too
                game.setScreen(new LoadingScreen(game));
            }
        });

        // 5. "Load Game" Button
        TextButton loadButton = new TextButton("Load Game", game.getSkin());
        table.add(loadButton).width(300).height(60).padBottom(20).row();

        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showLoadDialog();
            }
        });

        // Shop Button
        TextButton shopButton = new TextButton("Shop", game.getSkin());
        table.add(shopButton).width(300).height(60).padBottom(20).row();

        shopButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Shop clicked");
                game.setScreen(new ShopScreen(game));
            }
        });

        // Achievements Button
        TextButton achievementsButton = new TextButton("Achievements", game.getSkin());
        table.add(achievementsButton).width(300).height(60).padBottom(20).row();

        achievementsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Achievements clicked");
                game.setScreen(new AchievementScreen(game));
            }
        });

        // Leaderboard Button
        TextButton leaderboardButton = new TextButton("Leaderboard", game.getSkin());
        table.add(leaderboardButton).width(300).height(60).padBottom(20).row();

        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Leaderboard clicked");
                game.setScreen(new LeaderboardScreen(game));
            }
        });

        // Help Button
        TextButton helpButton = new TextButton("Help", game.getSkin());
        table.add(helpButton).width(300).height(60).padBottom(20).row();

        helpButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Help clicked");
                game.setScreen(new HelpScreen(game));
            }
        });

        // Element Manager Button (Developer Feature)
        TextButton elementCreatorButton = new TextButton("Element Manager", game.getSkin());
        elementCreatorButton.setColor(0.6f, 0.8f, 1f, 1f); // Light blue to indicate dev feature
        table.add(elementCreatorButton).width(300).height(60).padBottom(20).row();

        elementCreatorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Element Creator clicked");
                game.setScreen(new ElementCreatorScreen(game));
            }
        });

        // Player skins are now managed through Element Manager with PLAYER type

        // "Settings" Button
        TextButton settingsButton = new TextButton("Settings", game.getSkin());
        table.add(settingsButton).width(300).height(60).padBottom(20).row();

        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SettingsScreen(game, null));
            }
        });

        // "Exit" Button
        TextButton exitButton = new TextButton("Exit", game.getSkin());
        table.add(exitButton).width(300).height(60).row();

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Exit clicked");
                saveCurrentProfile();
                Gdx.app.exit();
            }
        });
    }

    /**
     * Saves the current active profile's global progression (Coins, Achievements,
     * Levels).
     * Should be called before switching profiles or exiting.
     */
    private void saveCurrentProfile() {
        String currentSave = game.getCurrentSaveFilePath();
        if (currentSave != null && !currentSave.isEmpty()) {
            SaveManager.saveGlobalProgression(currentSave);
        }
    }

    /**
     * 显示读档列表窗口 (动态尺寸版)
     */
    private void showLoadDialog() {
        Window win = new Window("Select Save File", game.getSkin());
        win.setModal(true);
        win.setResizable(true);
        win.getTitleLabel().setAlignment(Align.center);

        FileHandle[] files = SaveManager.getSaveFiles();
        Table listTable = new Table();
        listTable.top();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        if (files.length == 0) {
            listTable.add(new Label("No save files found.", game.getSkin())).pad(20);
        } else {
            for (FileHandle file : files) {
                Table rowTable = new Table();
                String dateStr = sdf.format(new Date(file.lastModified()));
                String infoText = file.nameWithoutExtension() + "\n" + dateStr;

                TextButton loadBtn = new TextButton(infoText, game.getSkin());
                loadBtn.getLabel().setFontScale(0.8f);
                loadBtn.getLabel().setAlignment(Align.left);
                loadBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        // Auto-save current profile before switching
                        saveCurrentProfile();

                        // Pre-load state to sync global managers (Shop, etc)
                        de.tum.cit.fop.maze.model.GameState loaded = SaveManager.loadGame(file.name());
                        if (loaded != null) {
                            de.tum.cit.fop.maze.shop.ShopManager.importState(loaded.getCoins(),
                                    loaded.getPurchasedItemIds());
                            // Sync Level Progress (Force set to what is in the save file)
                            de.tum.cit.fop.maze.config.GameSettings.forceSetUnlockedLevel(loaded.getMaxUnlockedLevel());

                            // Sync Achievements
                            de.tum.cit.fop.maze.utils.AchievementManager.importData(loaded.getAchievementData());

                            game.setCurrentSaveFilePath(file.name());

                            // Instead of going to game, show feedback
                            win.remove();
                            MenuScreen.this.showToast("Profile Loaded: " + file.nameWithoutExtension());
                        } else {
                            GameLogger.error("MenuScreen", "Failed to load save: " + file.name());
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
        scrollPane.setScrollingDisabled(true, false);

        // Auto-focus scroll on hover so user doesn't need to click
        UIUtils.enableHoverScrollFocus(scrollPane, stage);

        // --- 动态尺寸计算 ---
        float screenW = stage.getWidth();
        float screenH = stage.getHeight();

        float dialogW = Math.max(screenW * 0.6f, 350); // 至少350宽，或者屏幕60%
        float dialogH = screenH * 0.7f; // 高度70%

        win.add(scrollPane).grow().pad(10).row();

        TextButton closeBtn = new TextButton("Cancel", game.getSkin());
        closeBtn.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                win.remove();
            }

        });
        win.add(closeBtn).padBottom(10);

        win.setSize(dialogW, dialogH);
        win.setPosition(screenW / 2 - dialogW / 2, screenH / 2 - dialogH / 2);

        stage.addActor(win);
    }

    /**
     * Show dialog for New Game (Input Save Name)
     */
    private void showNewGameDialog() {
        Dialog dialog = new Dialog("New Game", game.getSkin()) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    TextField nameField = findActor("nameField");
                    String saveName = nameField.getText().trim();
                    if (!saveName.isEmpty()) {
                        startNewGame(saveName);
                    } else {
                        // Invalid name
                        GameLogger.error("MenuScreen", "Invalid save name");
                    }
                }
            }
        };

        dialog.text("Enter Save Name:");
        TextField nameField = new TextField("MySave", game.getSkin());
        nameField.setName("nameField");
        dialog.getContentTable().add(nameField).width(200).pad(10).row();

        dialog.button("Start Game", true);
        dialog.button("Cancel", false);

        dialog.show(stage);
    }

    private void startNewGame(String saveName) {
        GameLogger.info("MenuScreen", "Starting new game: " + saveName);

        // Auto-save current profile before switching
        saveCurrentProfile();

        // 1. Reset Global State (Shop & Achievements)
        de.tum.cit.fop.maze.shop.ShopManager.importState(0, new java.util.ArrayList<>());
        de.tum.cit.fop.maze.utils.AchievementManager.resetAll();

        // 2. Create Initial GameState
        // Start at Level 1, default position (will be overwritten by map spawn), 3
        // lives
        de.tum.cit.fop.maze.model.GameState initialState = new de.tum.cit.fop.maze.model.GameState(0, 0,
                "maps/level-1.properties", 3, false);
        initialState.setCoins(0);
        initialState.setPurchasedItemIds(new java.util.ArrayList<>());
        // Reset Skills
        initialState.setSkillPoints(0);
        initialState.setDamageBonus(0);
        initialState.setMaxHealthBonus(0);
        initialState.setMaxUnlockedLevel(1);

        // Reset Global GameSettings for New Game
        de.tum.cit.fop.maze.config.GameSettings.forceSetUnlockedLevel(1);

        // 3. Save it
        SaveManager.saveGame(initialState, saveName);

        // 4. Set Context
        game.setCurrentSaveFilePath(saveName + ".json");

        // 5. Start Game (Go to Story first)
        game.setScreen(new StoryScreen(game, "maps/level-1.properties"));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 绘制背景图片 - 使用整个 GL 视口覆盖整个屏幕（包括 FitViewport 的黑边区域）
        // 关键：使用 glViewport 重置为整个屏幕，然后渲染背景，再恢复 FitViewport
        SpriteBatch batch = game.getSpriteBatch();

        // 获取实际屏幕尺寸 - 使用 backbuffer 尺寸以确保正确
        int screenWidth = Gdx.graphics.getBackBufferWidth();
        int screenHeight = Gdx.graphics.getBackBufferHeight();

        // 重置 GL Viewport 到整个屏幕
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);

        // 设置投影矩阵到屏幕像素坐标系
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenWidth, screenHeight);
        batch.begin();

        // 背景图片原始尺寸
        float texWidth = backgroundTexture.getWidth();
        float texHeight = backgroundTexture.getHeight();

        // 计算Cover模式的缩放比例
        // Cover: 保持宽高比，确保图片覆盖整个屏幕（可能会裁剪）
        float screenRatio = (float) screenWidth / screenHeight;
        float textureRatio = texWidth / texHeight;

        float drawWidth, drawHeight;
        float drawX, drawY;

        if (screenRatio > textureRatio) {
            // 屏幕更宽，以宽度为准，高度可能超出
            drawWidth = screenWidth;
            drawHeight = screenWidth / textureRatio;
            drawX = 0;
            drawY = (screenHeight - drawHeight) / 2; // 垂直居中
        } else {
            // 屏幕更高，以高度为准，宽度可能超出
            drawHeight = screenHeight;
            drawWidth = screenHeight * textureRatio;
            drawX = (screenWidth - drawWidth) / 2; // 水平居中
            drawY = 0;
        }

        batch.draw(backgroundTexture, drawX, drawY, drawWidth, drawHeight);
        batch.end();

        // 恢复 Stage 的 Viewport（这会重新设置正确的 glViewport）
        stage.getViewport().apply();
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        // Play menu background music
        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playMenuBgm();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    private void showToast(String message) {
        Dialog toast = new Dialog("", game.getSkin());
        toast.text(message);
        toast.button("OK");
        toast.show(stage);
        // Auto-hide after 2 seconds?
        // Or just let user click OK.
    }
}