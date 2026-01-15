package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.utils.AchievementManager;

import java.util.List;
import de.tum.cit.fop.maze.utils.UIUtils;

public class VictoryScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final de.tum.cit.fop.maze.utils.SimpleParticleSystem particleSystem;
    private final Texture backgroundTexture;

    // Statistics
    private int killCount = 0;
    private int coinsCollected = 0;
    private List<String> newAchievements = null;

    /**
     * Original constructor (backward compatible)
     */
    public VictoryScreen(MazeRunnerGame game, String lastMapPath) {
        this(game, lastMapPath, 0, 0, null);
    }

    /**
     * Extended constructor with statistics and achievements
     */
    public VictoryScreen(MazeRunnerGame game, String lastMapPath, int killCount,
            int coinsCollected, List<String> newAchievements) {
        this.game = game;
        this.stage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());
        this.killCount = killCount;
        this.coinsCollected = coinsCollected;
        this.newAchievements = newAchievements;

        // Determine theme
        de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme theme = getTheme(lastMapPath);
        this.particleSystem = new de.tum.cit.fop.maze.utils.SimpleParticleSystem(theme);

        // Load Background
        this.backgroundTexture = new Texture(Gdx.files.internal(getBackgroundPath(theme)));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("VICTORY!", game.getSkin(), "title");
        table.add(title).padBottom(30).row();

        // Dynamic Story Text
        String storyText = getStoryText(lastMapPath);
        Label subtitle = new Label(storyText, game.getSkin());
        table.add(subtitle).padBottom(20).row();

        // === Statistics Display ===
        if (killCount > 0 || coinsCollected > 0) {
            Table statsTable = new Table();
            statsTable.pad(10);

            if (killCount > 0) {
                Label killLabel = new Label("Enemies Defeated: " + killCount, game.getSkin());
                killLabel.setColor(Color.RED);
                statsTable.add(killLabel).padRight(30);
            }

            if (coinsCollected > 0) {
                Label coinLabel = new Label("Coins Collected: " + coinsCollected, game.getSkin());
                coinLabel.setColor(Color.GOLD);
                statsTable.add(coinLabel);
            }

            table.add(statsTable).padBottom(20).row();
        }

        // === New Achievements Display ===
        if (newAchievements != null && !newAchievements.isEmpty()) {
            Label achievementTitle = new Label("New Achievements!", game.getSkin());
            achievementTitle.setColor(Color.CYAN);
            table.add(achievementTitle).padBottom(10).row();

            HorizontalGroup achievementGroup = new HorizontalGroup();
            achievementGroup.space(15);

            for (String achievement : newAchievements) {
                Table card = createAchievementCard(achievement);
                achievementGroup.addActor(card);
            }

            ScrollPane scrollPane = new ScrollPane(achievementGroup, game.getSkin());
            scrollPane.setScrollingDisabled(false, true);
            scrollPane.setFadeScrollBars(false);

            // Auto-focus scroll on hover so user doesn't need to click
            UIUtils.enableHoverScrollFocus(scrollPane, stage);

            table.add(scrollPane).width(600).padBottom(20).row();
        }

        // Calculate Next Level Logic
        final String nextMapPath = calculateNextLevel(lastMapPath);

        // Unlock Logic
        try {
            String clean = nextMapPath.replace("maps/level-", "").replace(".properties", "");
            int nextLevelNum = Integer.parseInt(clean);
            de.tum.cit.fop.maze.config.GameSettings.unlockLevel(nextLevelNum);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextButton nextBtn = new TextButton("Next Region", game.getSkin());
        nextBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Check if map exists, if not generate it
                if (!Gdx.files.internal(nextMapPath).exists() && !Gdx.files.local(nextMapPath).exists()) {
                    de.tum.cit.fop.maze.config.RandomMapConfig config = de.tum.cit.fop.maze.config.RandomMapConfig.NORMAL
                            .copy();

                    int nextLevel = getLevelNumber(nextMapPath);
                    // Theme order: 草原, 丛林, 荒漠, 冰原, 太空船
                    String nextTheme = "Grassland"; // 1-4: 草原
                    if (nextLevel >= 5 && nextLevel <= 8)
                        nextTheme = "Jungle"; // 5-8: 丛林
                    else if (nextLevel >= 9 && nextLevel <= 12)
                        nextTheme = "Desert"; // 9-12: 荒漠
                    else if (nextLevel >= 13 && nextLevel <= 16)
                        nextTheme = "Ice"; // 13-16: 冰原
                    else if (nextLevel >= 17 && nextLevel <= 20)
                        nextTheme = "Space"; // 17-20: 太空船

                    config.setTheme(nextTheme);

                    // Increase difficulty slightly
                    config.setDifficulty(Math.min(5, (nextLevel / 4) + 1));

                    new de.tum.cit.fop.maze.utils.MapGenerator(config).generateAndSave(nextMapPath);
                }
                game.setScreen(new GameScreen(game, nextMapPath, true));
            }
        });
        table.add(nextBtn).width(300).height(60).padBottom(15).row();

        TextButton skillBtn = new TextButton("Open Skill Tree", game.getSkin());
        skillBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SkillScreen(game, lastMapPath));
            }
        });
        table.add(skillBtn).width(300).height(60).padBottom(15).row();

        TextButton menuBtn = new TextButton("Back to Menu", game.getSkin());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        table.add(menuBtn).width(300).height(60);
    }

    /**
     * Create a simple achievement card display
     */
    private Table createAchievementCard(String achievementName) {
        Table card = new Table();
        card.pad(8);
        card.setBackground(game.getSkin().newDrawable("white", new Color(0.2f, 0.4f, 0.6f, 0.9f)));

        Label nameLabel = new Label(achievementName, game.getSkin());
        nameLabel.setColor(Color.WHITE);
        nameLabel.setAlignment(Align.center);
        nameLabel.setFontScale(0.9f);
        card.add(nameLabel).width(120).center();

        return card;
    }

    private String getStoryText(String mapPath) {
        if (mapPath == null)
            return "You retrieved the Chip!";

        int level = getLevelNumber(mapPath);
        if (level >= 1 && level <= 4) {
            return "You explored the peaceful grasslands!";
        } else if (level >= 5 && level <= 8) {
            return "You navigated the deadly jungle!";
        } else if (level >= 9 && level <= 12) {
            return "You survived the scorching sands!";
        } else if (level >= 13 && level <= 16) {
            return "You conquered the frozen wasteland!";
        } else if (level >= 17 && level <= 20) {
            return "You escaped the alien station!";
        } else {
            return "You retrieved the Chip!";
        }
    }

    private de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme getTheme(String mapPath) {
        if (mapPath == null)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.FOREST;

        int level = getLevelNumber(mapPath);
        if (level >= 1 && level <= 4)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.FOREST; // 草原
        if (level >= 5 && level <= 8)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.JUNGLE; // 丛林
        if (level >= 9 && level <= 12)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.DESERT; // 荒漠
        if (level >= 13 && level <= 16)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.RAIN; // 冰原
        if (level >= 17 && level <= 20)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.SPACE; // 太空船

        return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.FOREST;
    }

    private int getLevelNumber(String mapPath) {
        try {
            String clean = mapPath.replace("maps/level-", "").replace(".properties", "");
            // Handle local path case just in case
            if (clean.contains("level-")) {
                // Regex fallback if simple replace failed due to different path structure
                clean = clean.replaceAll(".*level-", "");
            }
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 1;
        }
    }

    private String getBackgroundPath(de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme theme) {
        switch (theme) {
            case FOREST:
                return "Grass.png";
            case DESERT:
                return "sand.png";
            case RAIN:
                return "waterland.png";
            case JUNGLE:
                return "jungle.png";
            case SPACE:
                return "space.png";
            default:
                return "Grass.png";
        }
    }

    private String calculateNextLevel(String current) {
        // format: "maps/level-1.properties"
        try {
            String clean = current.replace("maps/level-", "").replace(".properties", "");
            int num = Integer.parseInt(clean);
            return "maps/level-" + (num + 1) + ".properties";
        } catch (Exception e) {
            return "maps/level-1.properties"; // Fallback
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0.2f, 0, 1); // Dark Green Background (Fallback)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw Background
        game.getSpriteBatch().begin();
        // Crop bottom-right 10% by using UV coordinates 0.0 to 0.9
        game.getSpriteBatch().draw(backgroundTexture,
                0, 0, stage.getViewport().getScreenWidth(), stage.getViewport().getScreenHeight(),
                0, 0.9f, 0.9f, 0);
        game.getSpriteBatch().end();

        // Render Particles
        // particleSystem.updateAndDrawRefactored(delta,
        // stage.getViewport().getScreenWidth(), stage.getViewport().getScreenHeight());

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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

    @Override
    public void dispose() {
        stage.dispose();
        particleSystem.dispose();
        backgroundTexture.dispose();
    }
}
