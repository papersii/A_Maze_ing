package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.model.GameState;
import de.tum.cit.fop.maze.utils.SaveManager;
import de.tum.cit.fop.maze.utils.UIUtils;

public class SkillScreen implements Screen {

    private final MazeRunnerGame game;
    private final String currentLevel;
    private final boolean isVictory; // 追踪玩家是胜利还是失败进入的
    private Stage stage;
    private GameState gameState;

    /**
     * 构造函数 - 需要知道玩家是胜利还是失败进入 Skill Tree
     * @param game 游戏实例
     * @param currentLevel 当前关卡路径
     * @param isVictory true=胜利后进入, false=失败后进入
     */
    public SkillScreen(MazeRunnerGame game, String currentLevel, boolean isVictory) {
        this.game = game;
        this.currentLevel = currentLevel;
        this.isVictory = isVictory;

        // Load the victory state
        this.gameState = SaveManager.loadGame("auto_save_victory.json");
        if (this.gameState == null) {
            // Fallback if no victory save found
            this.gameState = SaveManager.loadGame("auto_save.json");
        }
        if (this.gameState == null) {
            // New fallback state if nothing exists
            this.gameState = new GameState(0, 0, currentLevel, 3, false);
            this.gameState.setSkillPoints(0);
            this.gameState.setMaxHealthBonus(0);
            this.gameState.setDamageBonus(0);
            this.gameState.setInvincibilityExtension(0.0f);
            this.gameState.setKnockbackMultiplier(1.0f);
            this.gameState.setCooldownReduction(0.0f);
            this.gameState.setSpeedBonus(0.0f);
        }
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Skin skin = game.getSkin();

        // 1. Title
        Label title = new Label("SKILL TREE", skin, "title");
        rootTable.add(title).padTop(20).padBottom(20).row();

        // 2. SP Display
        Label spLabel = new Label("Skill Points: " + gameState.getSkillPoints(), skin);
        spLabel.setFontScale(1.2f);
        rootTable.add(spLabel).padBottom(20).row();

        // 3. Scrollable Skill List
        Table skillTable = new Table();
        skillTable.pad(20);

        // --- Health ---
        addSkillRow(skillTable, skin, "Max Health +1",
                "Current Bonus: +" + gameState.getMaxHealthBonus(),
                getHealthCost(), () -> {
                    int cost = getHealthCost();
                    if (gameState.getSkillPoints() >= cost) {
                        gameState.setSkillPoints(gameState.getSkillPoints() - cost);
                        gameState.setMaxHealthBonus(gameState.getMaxHealthBonus() + 1);
                        gameState.setLives(gameState.getLives() + 1);
                        updateLabels(spLabel, rootTable);
                        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    }
                });

        // --- Damage ---
        addSkillRow(skillTable, skin, "Damage +1",
                "Current Bonus: +" + gameState.getDamageBonus(),
                getDamageCost(), () -> {
                    int cost = getDamageCost();
                    if (gameState.getSkillPoints() >= cost) {
                        gameState.setSkillPoints(gameState.getSkillPoints() - cost);
                        gameState.setDamageBonus(gameState.getDamageBonus() + 1);
                        updateLabels(spLabel, rootTable);
                        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    }
                });

        // --- Invincibility ---
        addSkillRow(skillTable, skin, "I-Frame +0.2s",
                String.format("Ext: +%.1fs", gameState.getInvincibilityExtension()),
                getInvincibilityCost(), () -> {
                    int cost = getInvincibilityCost();
                    if (gameState.getSkillPoints() >= cost) {
                        gameState.setSkillPoints(gameState.getSkillPoints() - cost);
                        gameState.setInvincibilityExtension(gameState.getInvincibilityExtension() + 0.2f);
                        updateLabels(spLabel, rootTable);
                        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    }
                });

        // --- Knockback ---
        addSkillRow(skillTable, skin, "Knockback +10%",
                String.format("Mult: %.1fx", gameState.getKnockbackMultiplier()),
                getKnockbackCost(), () -> {
                    int cost = getKnockbackCost();
                    if (gameState.getSkillPoints() >= cost) {
                        gameState.setSkillPoints(gameState.getSkillPoints() - cost);
                        gameState.setKnockbackMultiplier(gameState.getKnockbackMultiplier() + 0.1f);
                        updateLabels(spLabel, rootTable);
                        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    }
                });

        // --- Cooldown ---
        addSkillRow(skillTable, skin, "Cooldown -0.1s",
                String.format("Reduc: %.1fs", gameState.getCooldownReduction()),
                getCooldownCost(), () -> {
                    int cost = getCooldownCost();
                    if (gameState.getSkillPoints() >= cost && gameState.getCooldownReduction() < 0.5f) {
                        gameState.setSkillPoints(gameState.getSkillPoints() - cost);
                        gameState.setCooldownReduction(gameState.getCooldownReduction() + 0.1f);
                        updateLabels(spLabel, rootTable);
                        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    }
                });

        // --- Speed ---
        addSkillRow(skillTable, skin, "Speed +5%",
                String.format("Bonus: %.0f%%", gameState.getSpeedBonus() * 100),
                getSpeedCost(), () -> {
                    int cost = getSpeedCost();
                    if (gameState.getSkillPoints() >= cost) {
                        gameState.setSkillPoints(gameState.getSkillPoints() - cost);
                        gameState.setSpeedBonus(gameState.getSpeedBonus() + 0.05f);
                        updateLabels(spLabel, rootTable);
                        de.tum.cit.fop.maze.utils.AudioManager.getInstance().playSound("select");
                    }
                });

        ScrollPane scrollPane = new ScrollPane(skillTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // Vertical scrolling only

        // Auto-focus scroll on hover so user doesn't need to click
        UIUtils.enableHoverScrollFocus(scrollPane, stage);

        rootTable.add(scrollPane).expand().fill().pad(40).row();

        // 4. Done Button - 根据isVictory返回正确界面
        TextButton doneBtn = new TextButton("Done / Continue", skin);
        doneBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SaveManager.saveGame(gameState, "auto_save_victory");
                SaveManager.saveGame(gameState, "auto_save");
                
                // 根据进入时的状态返回正确界面
                if (isVictory) {
                    // 胜利后返回 VictoryScreen
                    game.setScreen(new VictoryScreen(game, currentLevel));
                } else {
                    // 失败后返回 LevelSummaryScreen (Defeat 模式)
                    de.tum.cit.fop.maze.model.LevelSummaryData defeatData = 
                        new de.tum.cit.fop.maze.model.LevelSummaryData(
                            de.tum.cit.fop.maze.model.LevelSummaryData.Result.DEFEAT, 
                            currentLevel
                        );
                    game.setScreen(new LevelSummaryScreen(game, defeatData));
                }
            }
        });
        rootTable.add(doneBtn).width(300).height(60).padBottom(20);
    }

    private void addSkillRow(Table table, Skin skin, String name, String statInfo, int cost, Runnable onBuy) {
        // Increased widths and padding for better spacing
        table.add(new Label(name, skin)).width(250).align(Align.left).padRight(40);

        Label infoLabel = new Label(statInfo, skin);
        infoLabel.setName("info_" + name);
        table.add(infoLabel).width(350).align(Align.center).padRight(40);

        TextButton buyBtn = new TextButton("Buy (Cost: " + cost + ")", skin);
        buyBtn.setName("btn_" + name);
        buyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onBuy.run();
            }
        });
        table.add(buyBtn).width(260).height(60).padBottom(40); // 增加宽度确保文字不超出
        table.row();
    }

    // Cost Formulas
    private int getHealthCost() {
        return 10 + (gameState.getMaxHealthBonus() * 10);
    }

    // Damage costs: +1 base, then scales
    private int getDamageCost() {
        return 15 + (gameState.getDamageBonus() * 15);
    }

    private int getInvincibilityCost() {
        return 20 + ((int) (gameState.getInvincibilityExtension() * 50));
    }

    private int getKnockbackCost() {
        return 20 + (int) ((gameState.getKnockbackMultiplier() - 1.0f) * 100);
    }

    private int getCooldownCost() {
        return 50 + (int) (gameState.getCooldownReduction() * 500); // 50, 100, 150...
    }

    private int getSpeedCost() {
        return 30 + (int) (gameState.getSpeedBonus() * 200);
    }

    private void updateLabels(Label spLabel, Table table) {
        spLabel.setText("Skill Points: " + gameState.getSkillPoints());

        // Rebuild or update specific cells is hard in LibGDX Table without references.
        // But we tagged them or can just refresh the screen.
        // Simplest: Close and Reopen or just clear and rebuild.
        // Let's clear and rebuild for simplicity.
        stage.clear();
        show(); // Rebuild UI
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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
    }
}
