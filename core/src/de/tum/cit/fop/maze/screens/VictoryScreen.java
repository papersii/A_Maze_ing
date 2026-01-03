package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class VictoryScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final de.tum.cit.fop.maze.utils.SimpleParticleSystem particleSystem;
    private final Texture backgroundTexture;

    public VictoryScreen(MazeRunnerGame game, String lastMapPath) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        // Determine theme
        de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme theme = getTheme(lastMapPath);
        this.particleSystem = new de.tum.cit.fop.maze.utils.SimpleParticleSystem(theme);

        // Load Background
        this.backgroundTexture = new Texture(Gdx.files.internal(getBackgroundPath(theme)));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("VICTORY!", game.getSkin(), "title");
        table.add(title).padBottom(50).row();

        // Dynamic Story Text
        String storyText = getStoryText(lastMapPath);
        Label subtitle = new Label(storyText, game.getSkin());
        table.add(subtitle).padBottom(50).row();

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
                    new de.tum.cit.fop.maze.utils.MapGenerator().generateAndSave(nextMapPath);
                }
                game.setScreen(new GameScreen(game, nextMapPath, true));
            }
        });
        table.add(nextBtn).width(300).height(60).padBottom(20).row();

        TextButton skillBtn = new TextButton("Open Skill Tree", game.getSkin());
        skillBtn.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SkillScreen(game, lastMapPath));
            }
        });
        table.add(skillBtn).width(300).height(60).padBottom(20).row();

        TextButton menuBtn = new TextButton("Back to Menu", game.getSkin());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        table.add(menuBtn).width(300).height(60);
    }

    private String getStoryText(String mapPath) {
        if (mapPath == null)
            return "You retrieved the Chip!";

        if (mapPath.contains("level-1")) {
            return "You found the ancient key in the forest!";
        } else if (mapPath.contains("level-2")) {
            return "You survived the scorching sands!";
        } else if (mapPath.contains("level-3")) {
            return "You conquered the frozen wasteland!";
        } else if (mapPath.contains("level-4")) {
            return "You navigated the deadly jungle!";
        } else if (mapPath.contains("level-5")) {
            return "You escaped the alien station!";
        } else {
            return "You retrieved the Chip!";
        }
    }

    private de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme getTheme(String mapPath) {
        if (mapPath == null)
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.FOREST;
        if (mapPath.contains("level-1"))
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.FOREST;
        if (mapPath.contains("level-2"))
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.DESERT;
        if (mapPath.contains("level-3"))
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.RAIN; // Ice/Wet
        if (mapPath.contains("level-4"))
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.JUNGLE;
        if (mapPath.contains("level-5"))
            return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.SPACE;
        return de.tum.cit.fop.maze.utils.SimpleParticleSystem.Theme.FOREST;
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
