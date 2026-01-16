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
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.utils.SaveManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import de.tum.cit.fop.maze.utils.MapGenerator;
import de.tum.cit.fop.maze.utils.GameLogger;
import de.tum.cit.fop.maze.utils.UIUtils;

public class MenuScreen implements Screen {

    private final Stage stage;
    private final MazeRunnerGame game;
    private Label loadingLabel;
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
                // Go to Story Screen first, passing null to indicate default Level 1
                game.setScreen(new StoryScreen(game, "maps/level-1.properties"));
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

        // 4. "Random Map" Button
        TextButton randomButton = new TextButton("Random Map", game.getSkin());
        table.add(randomButton).width(300).height(60).padBottom(20).row();

        loadingLabel = new Label("Generating 200x200 Map...", game.getSkin());
        loadingLabel.setColor(Color.YELLOW);
        loadingLabel.setVisible(false);
        // 先添加到表格最后，或者在按钮下方
        table.add(loadingLabel).padBottom(10).row();

        randomButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (loadingLabel.isVisible())
                    return;
                showRandomMapDialog();
            }
        });

        // 4. "Load Game" Button
        TextButton loadButton = new TextButton("Load Game", game.getSkin());
        table.add(loadButton).width(300).height(60).padBottom(20).row();

        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showLoadDialog();
            }
        });

        // Shop Button (NEW)
        TextButton shopButton = new TextButton("Shop", game.getSkin());
        table.add(shopButton).width(300).height(60).padBottom(20).row();

        shopButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Shop clicked");
                game.setScreen(new ShopScreen(game));
            }
        });

        // Achievements Button (NEW)
        TextButton achievementsButton = new TextButton("Achievements", game.getSkin());
        table.add(achievementsButton).width(300).height(60).padBottom(20).row();

        achievementsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Achievements clicked");
                game.setScreen(new AchievementScreen(game));
            }
        });

        // Leaderboard Button (NEW)
        TextButton leaderboardButton = new TextButton("Leaderboard", game.getSkin());
        table.add(leaderboardButton).width(300).height(60).padBottom(20).row();

        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Leaderboard clicked");
                game.setScreen(new LeaderboardScreen(game));
            }
        });

        // Help Button (NEW)
        TextButton helpButton = new TextButton("Help", game.getSkin());
        table.add(helpButton).width(300).height(60).padBottom(20).row();

        helpButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Help clicked");
                game.setScreen(new HelpScreen(game));
            }
        });

        // 4. "Settings" Button
        TextButton settingsButton = new TextButton("Settings", game.getSkin());
        table.add(settingsButton).width(300).height(60).padBottom(20).row();

        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SettingsScreen(game, null));
            }
        });

        // 5. "Exit" Button
        TextButton exitButton = new TextButton("Exit", game.getSkin());
        table.add(exitButton).width(300).height(60).row();

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.info("MenuScreen", "Exit clicked");
                Gdx.app.exit();
            }
        });
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
                        game.goToGame(file.name());
                        win.remove();
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

    private void showRandomMapDialog() {
        // 创建一个全屏或大窗口来显示地图列表
        Window win = new Window("Select Random Map", game.getSkin());
        win.setResizable(true);
        win.setModal(true);
        win.getTitleLabel().setAlignment(Align.center);

        // 1. 获取地图列表
        FileHandle mapsDir = Gdx.files.local("maps/random");
        if (!mapsDir.exists()) {
            mapsDir.mkdirs();
        }

        // 过滤 random_*.properties
        FileHandle[] files = mapsDir
                .list((dir, name) -> name.startsWith("random_") && name.endsWith(".properties"));

        // 按最后修改时间降序排序 (最新的在上面)
        Arrays.sort(files, new Comparator<FileHandle>() {
            @Override
            public int compare(FileHandle f1, FileHandle f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        // 2. 构建列表内容
        Table listTable = new Table();
        listTable.top();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (files.length == 0) {
            listTable.add(new Label("No generated maps found.", game.getSkin())).pad(20);
        } else {
            for (FileHandle file : files) {
                Table row = new Table();
                // row.setBackground(game.getSkin().getDrawable("default-rect")); // Removed to
                // avoid crash

                String name = file.nameWithoutExtension();
                String dateStr = sdf.format(new Date(file.lastModified()));

                Label nameLabel = new Label(name + "\n" + dateStr, game.getSkin());
                nameLabel.setFontScale(0.8f);

                TextButton playBtn = new TextButton("Play", game.getSkin());
                playBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.goToGame(file.path());
                        win.remove();
                    }
                });

                TextButton delBtn = new TextButton("Delete", game.getSkin());
                delBtn.setColor(1, 0.3f, 0.3f, 1);
                delBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        file.delete();
                        win.remove();
                        showRandomMapDialog(); // 刷新列表
                    }
                });

                row.add(nameLabel).expandX().left().pad(5);
                row.add(playBtn).width(80).pad(5);
                row.add(delBtn).width(70).pad(5);

                listTable.add(row).growX().padBottom(5).row();
            }
        }

        ScrollPane scroll = new ScrollPane(listTable, game.getSkin());
        scroll.setFadeScrollBars(false);

        // Auto-focus scroll on hover so user doesn't need to click
        UIUtils.enableHoverScrollFocus(scroll, stage);

        win.add(scroll).grow().pad(10).row();

        // 3. 底部按钮
        Table botTable = new Table();

        // === Difficulty Presets (NEW) ===
        Table difficultyTable = new Table();
        difficultyTable.add(new Label("Difficulty: ", game.getSkin())).padRight(10);

        String[] presets = { "Easy", "Normal", "Hard", "Nightmare", "Custom" };
        final int[] selectedPreset = { 1 }; // Default: Normal
        final de.tum.cit.fop.maze.config.RandomMapConfig[] customConfigHolder = { null };

        for (int i = 0; i < presets.length; i++) {
            final int presetIndex = i;
            TextButton presetBtn = new TextButton(presets[i], game.getSkin());
            if (i == selectedPreset[0]) {
                presetBtn.setColor(0.3f, 0.7f, 0.3f, 1f); // Highlight default
            }
            presetBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectedPreset[0] = presetIndex;
                    // Update button colors
                    for (Actor a : difficultyTable.getChildren()) {
                        if (a instanceof TextButton) {
                            ((TextButton) a).setColor(Color.WHITE);
                        }
                    }
                    presetBtn.setColor(0.3f, 0.7f, 0.3f, 1f);

                    // If Custom clicked, open dialog
                    if (presetIndex == 4) {
                        RandomMapConfigDialog dialog = new RandomMapConfigDialog(game,
                                customConfigHolder[0] != null ? customConfigHolder[0]
                                        : new de.tum.cit.fop.maze.config.RandomMapConfig(),
                                (activeConfig) -> {
                                    customConfigHolder[0] = activeConfig;
                                },
                                () -> {
                                    // On cancel
                                });
                        stage.addActor(dialog);
                    }
                }
            });
            difficultyTable.add(presetBtn).width(90).pad(3);
        }

        win.row();
        win.add(difficultyTable).padBottom(15).row();

        TextButton genBtn = new TextButton("Generate New Map", game.getSkin());
        genBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                win.remove();
                String timeSuffix = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String newName = "maps/random/random_" + timeSuffix + ".properties";

                // Get config based on selected preset
                de.tum.cit.fop.maze.config.RandomMapConfig config;
                switch (selectedPreset[0]) {
                    case 0:
                        config = de.tum.cit.fop.maze.config.RandomMapConfig.EASY;
                        break;
                    case 2:
                        config = de.tum.cit.fop.maze.config.RandomMapConfig.HARD;
                        break;
                    case 3:
                        config = de.tum.cit.fop.maze.config.RandomMapConfig.NIGHTMARE;
                        break;
                    case 4: // Custom
                        config = customConfigHolder[0];
                        if (config == null)
                            config = de.tum.cit.fop.maze.config.RandomMapConfig.NORMAL; // Fallback
                        break;
                    default:
                        config = de.tum.cit.fop.maze.config.RandomMapConfig.NORMAL;
                        break;
                }
                startRandomGeneration(newName, config);
            }
        });

        TextButton closeBtn = new TextButton("Cancel", game.getSkin());
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                win.remove();
            }
        });

        botTable.add(genBtn).height(50).padRight(20);
        botTable.add(closeBtn).height(50);

        win.add(botTable).pad(10);

        win.setSize(800, 600);
        win.setPosition(stage.getWidth() / 2 - 400, stage.getHeight() / 2 - 300);
        stage.addActor(win);
    }

    // 重载方法以支持指定文件名
    private void startRandomGeneration(String fileName) {
        startRandomGeneration(fileName, de.tum.cit.fop.maze.config.RandomMapConfig.NORMAL);
    }

    // 使用配置生成随机地图
    private void startRandomGeneration(String fileName, de.tum.cit.fop.maze.config.RandomMapConfig config) {
        loadingLabel.setText("Generating " + config.getWidth() + "x" + config.getHeight() + " Map...");
        loadingLabel.setVisible(true);
        new Thread(() -> {
            MapGenerator gen = new MapGenerator(config);
            gen.generateAndSave(fileName, config);
            // 确保回到主线程切换场景 - 使用 ArmorSelectScreen
            Gdx.app.postRunnable(() -> {
                loadingLabel.setVisible(false);
                // 跳转到护甲选择界面而不是直接进入游戏
                game.setScreen(new ArmorSelectScreen(game, fileName));
            });
        }).start();
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
}