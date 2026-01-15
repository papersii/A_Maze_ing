package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.config.RandomMapConfig;
import de.tum.cit.fop.maze.model.LevelSummaryData;
import de.tum.cit.fop.maze.utils.AchievementManager;
import de.tum.cit.fop.maze.utils.AchievementUnlockInfo;
import de.tum.cit.fop.maze.utils.MapGenerator;

/**
 * Level Summary Screen - Redesigned
 * 
 * Features a modern card-based layout with strong visual hierarchy.
 */
public class LevelSummaryScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final LevelSummaryData data;
    private final Skin skin;

    // Background
    private Texture backgroundTexture;

    // Styling Constants
    private static final Color COLOR_VICTORY = new Color(1f, 0.84f, 0f, 1f); // Gold
    private static final Color COLOR_DEFEAT = new Color(0.8f, 0.2f, 0.2f, 1f); // Red
    private static final Color COLOR_TEXT_DIM = new Color(0.8f, 0.8f, 0.8f, 1f);

    public LevelSummaryScreen(MazeRunnerGame game, LevelSummaryData data) {
        this.game = game;
        this.data = data;
        this.skin = game.getSkin();
        this.stage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        // === 金币同步到商店系统 ===
        // 将本关卡收集的金币持久化到 Preferences，供商店使用
        de.tum.cit.fop.maze.shop.ShopManager.syncCoinsFromGame(data.getCoinsCollected());

        loadBackground();
        buildUI();
    }

    private void loadBackground() {
        String bgPath = "Grass.png"; // Default
        String theme = data.getThemeName();
        switch (theme) {
            case "Grassland":
                bgPath = "Grass.png";
                break;
            case "Desert":
                bgPath = "sand.png";
                break;
            case "Ice":
                bgPath = "waterland.png";
                break;
            case "Jungle":
                bgPath = "jungle.png";
                break;
            case "Space":
                bgPath = "space.png";
                break;
        }

        try {
            backgroundTexture = new Texture(Gdx.files.internal(bgPath));
        } catch (Exception e) {
            // Fallback to solid color
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(data.isVictory() ? new Color(0.1f, 0.2f, 0.1f, 1) : new Color(0.2f, 0.1f, 0.1f, 1));
            pm.fill();
            backgroundTexture = new Texture(pm);
            pm.dispose();
        }
    }

    private void buildUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        // Add a dark overlay to dim the background image for better text readability
        rootTable.setBackground(createColorDrawable(new Color(0f, 0f, 0f, 0.6f)));
        rootTable.pad(50);
        stage.addActor(rootTable);

        // 1. Header Section
        buildHeader(rootTable);

        // 2. Main Content Card
        buildMainCard(rootTable);

        // 3. Footer Buttons
        buildFooter(rootTable);
    }

    private void buildHeader(Table root) {
        String titleText = data.isVictory() ? "VICTORY" : "DEFEAT";
        Label titleLabel = new Label(titleText, skin, "title");
        Color titleColor = data.isVictory() ? COLOR_VICTORY : COLOR_DEFEAT;
        titleLabel.setColor(titleColor);
        titleLabel.setFontScale(1.5f);

        root.add(titleLabel).padBottom(10).row();

        String subtitle = getSubtitle();
        Label subtitleLabel = new Label(subtitle, skin);
        // Ensure subtitle is bright white and fully opaque
        subtitleLabel.setColor(new Color(1f, 1f, 1f, 1f));
        subtitleLabel.setFontScale(1.1f);
        root.add(subtitleLabel).padBottom(40).row();
    }

    private void buildMainCard(Table root) {
        Table card = new Table();

        // Use NinePatch for perfect border scaling
        Color borderColor = data.isVictory() ? COLOR_VICTORY : Color.GRAY;
        Color bgColor = new Color(0.1f, 0.1f, 0.15f, 0.95f);
        card.setBackground(createBorderNinePatch(bgColor, borderColor, 4));

        // Padding to keep content away from borders
        card.pad(50);
        // Extra top padding as requested to avoid overcrowding the top area
        card.padTop(80);

        // Split Layout: Stats (Left) | Rank/Summary (Right)
        if (data.isVictory()) {
            // Left Column: Detailed Stats
            Table statsCol = buildStatsTable();
            card.add(statsCol).expand().fill().padRight(60);

            // Vertical Separator
            Image separator = new Image(createColorDrawable(new Color(1f, 1f, 1f, 0.1f)));
            card.add(separator).width(2).growY().padRight(60);

            // Right Column: Rank & Achievements
            Table rankCol = buildRankColumn();
            card.add(rankCol).width(450).top();
        } else {
            Table statsCol = buildStatsTable();
            card.add(statsCol).growX();
        }

        root.add(card).width(1400).height(600).padBottom(50).row();
    }

    /**
     * Creates a NinePatchDrawable with a bordered style.
     * This ensures the border thickness remains constant regardless of resizing.
     */
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createBorderNinePatch(Color bgColor, Color borderColor,
            int borderThickness) {
        // Create a 3x3 grid for the NinePatch
        // We need enough resolution to represent the border.
        // Actually, simplest is 3x3 pixels: Corners, Edges, Center.
        // But for a thick border (4px), we might want a slightly larger pixmap or
        // handle it via scaling?
        // Standard NinePatch logic:
        // 1. Create a texture where the border pixels are 'borderThickness' wide/high.
        // 2. Define the split points.

        int size = 10 + 2 * borderThickness; // Small texture size
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Fill pure background first
        pm.setColor(bgColor);
        pm.fill();

        // Draw borders
        pm.setColor(borderColor);
        // Top
        pm.fillRectangle(0, 0, size, borderThickness);
        // Bottom
        pm.fillRectangle(0, size - borderThickness, size, borderThickness);
        // Left
        pm.fillRectangle(0, 0, borderThickness, size);
        // Right
        pm.fillRectangle(size - borderThickness, 0, borderThickness, size);

        Texture texture = new Texture(pm);
        pm.dispose();

        // Configure splits so the corners (borderThickness size) are not stretched,
        // and the center is stretched.
        // NinePatch(texture, left, right, top, bottom)
        com.badlogic.gdx.graphics.g2d.NinePatch patch = new com.badlogic.gdx.graphics.g2d.NinePatch(texture,
                borderThickness, borderThickness, borderThickness, borderThickness);

        return new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(patch);
    }

    // ... existing cleanup ...

    private Table buildStatsTable() {
        Table table = new Table();
        table.top();

        // Section Title
        Label sectionTitle = new Label("PERFORMANCE", skin);
        sectionTitle.setColor(Color.LIGHT_GRAY);
        sectionTitle.setFontScale(1.1f); // Larger font
        table.add(sectionTitle).left().padBottom(30).colspan(2).row();

        // Stat Rows
        addModernStatRow(table, "Enemies Defeated", String.valueOf(data.getKillCount()), "icon_skull");
        addModernStatRow(table, "Coins Collected", String.valueOf(data.getCoinsCollected()), "icon_coin");
        addModernStatRow(table, "Time Taken", data.getFormattedTime(), "icon_time");

        if (data.isVictory()) {
            String hpText = data.getPlayerHP() + "/" + data.getMaxHP();
            addModernStatRow(table, "HP Remaining", hpText, "icon_heart");

            String flawless = data.tookDamage() ? "No" : "Yes (Bonus!)";
            addModernStatRow(table, "Flawless Clear", flawless, "icon_shield");
        }

        return table;
    }

    private void addModernStatRow(Table table, String label, String value, String icon) {
        // Just text for now, icons can be added later if assets exist
        Label nameLabel = new Label(label, skin);
        nameLabel.setColor(COLOR_TEXT_DIM);
        nameLabel.setFontScale(1.1f);

        Label valueLabel = new Label(value, skin);
        valueLabel.setColor(Color.WHITE);
        valueLabel.setAlignment(Align.right);
        valueLabel.setFontScale(1.1f);

        table.add(nameLabel).left().padBottom(20).expandX();
        table.add(valueLabel).right().padBottom(20).row();

        // Dotted line separator
        Image line = new Image(createColorDrawable(new Color(1f, 1f, 1f, 0.05f)));
        table.add(line).height(1).colspan(2).growX().padBottom(20).row();
    }

    private Table buildRankColumn() {
        Table table = new Table();
        table.top();

        // Rank Display
        Label rankTitle = new Label("RANK", skin);
        rankTitle.setColor(Color.LIGHT_GRAY);
        rankTitle.setFontScale(1.1f);
        table.add(rankTitle).center().padBottom(20).row();

        String rank = data.getRank();
        Label rankLabel = new Label(rank, skin, "title"); // Use Title font for rank
        rankLabel.setColor(getRankColor(rank));
        rankLabel.setFontScale(4.5f); // Massive Rank (increased from 3.0)
        table.add(rankLabel).center().padBottom(40).row();

        // New Achievements Section
        if (!data.getNewAchievements().isEmpty()) {
            Label achTitle = new Label("UNLOCKED", skin);
            achTitle.setColor(Color.LIGHT_GRAY);
            achTitle.setFontScale(1.0f);
            table.add(achTitle).center().padBottom(15).row();

            Table achList = new Table();
            for (String ach : data.getNewAchievements()) {
                AchievementUnlockInfo info = AchievementManager.getAchievementInfo(ach);
                Label badge = new Label(info.getName(), skin);
                badge.setColor(Color.CYAN);
                badge.setFontScale(0.9f);
                achList.add(badge).padBottom(8).row();
            }
            ScrollPane scroll = new ScrollPane(achList, skin);
            scroll.setFadeScrollBars(false);

            // Auto-focus scroll on hover so user doesn't need to click
            final ScrollPane sp = scroll;
            scroll.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                        com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    stage.setScrollFocus(sp);
                }

                @Override
                public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                        com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    // Keep focus for better UX
                }
            });

            table.add(scroll).height(150).growX();
        }

        return table;
    }

    private void buildFooter(Table root) {
        Table footer = new Table();

        // Primary Action (Next Level) - Only on Victory
        if (data.isVictory() && !data.isRandomMode()) {
            TextButton nextBtn = new TextButton("NEXT LEVEL >>", skin);
            nextBtn.getLabel().setFontScale(1.2f);
            nextBtn.setColor(COLOR_VICTORY);
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    goToNextLevel();
                }
            });
            // Increased primary button size
            footer.add(nextBtn).width(320).height(85).padRight(40);
        }

        // Secondary Actions Group
        Table secondary = new Table();

        TextButton retryBtn = new TextButton("Retry", skin);
        retryBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ArmorSelectScreen(game, data.getMapPath()));
            }
        });
        // Increased secondary button sizes
        secondary.add(retryBtn).width(200).height(65).padRight(20);

        TextButton skillBtn = new TextButton("Skill Tree", skin);
        skillBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SkillScreen(game, data.getMapPath()));
            }
        });
        secondary.add(skillBtn).width(200).height(65).padRight(20);

        TextButton menuBtn = new TextButton("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        secondary.add(menuBtn).width(200).height(65);

        // Submit Score Button (Only on Victory)
        if (data.isVictory()) {
            TextButton submitBtn = new TextButton("Submit Score", skin);
            submitBtn.setColor(Color.CYAN);
            submitBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showScoreSubmitDialog();
                }
            });
            secondary.add(submitBtn).width(200).height(65).padLeft(20);
        }

        footer.add(secondary);

        root.add(footer).padTop(30);
    }

    /**
     * 显示分数提交对话框
     */
    private void showScoreSubmitDialog() {
        // 计算分数
        int score = de.tum.cit.fop.maze.utils.LeaderboardManager.calculateScore(
                data.getCompletionTime(),
                data.getKillCount(),
                data.getCoinsCollected(),
                data.tookDamage());

        Dialog dialog = new Dialog("Submit Your Score", skin);
        dialog.setModal(true);

        Table content = new Table();
        content.pad(20);

        // 显示分数
        Label scoreLabel = new Label("Your Score: " + score, skin);
        scoreLabel.setColor(Color.GOLD);
        scoreLabel.setFontScale(1.5f);
        content.add(scoreLabel).padBottom(30).row();

        // 显示排名预览
        int rank = de.tum.cit.fop.maze.utils.LeaderboardManager.getInstance().getRank(score);
        Label rankLabel = new Label("Rank #" + rank, skin);
        rankLabel.setColor(Color.CYAN);
        content.add(rankLabel).padBottom(20).row();

        // 玩家名称输入
        content.add(new Label("Enter Your Name:", skin)).padBottom(10).row();
        final TextField nameField = new TextField("Player", skin);
        nameField.setMaxLength(15);
        content.add(nameField).width(300).padBottom(20).row();

        dialog.getContentTable().add(content);

        // 按钮
        TextButton submitBtn = new TextButton("Submit", skin);
        submitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String playerName = nameField.getText().trim();
                if (playerName.isEmpty())
                    playerName = "Anonymous";

                de.tum.cit.fop.maze.utils.LeaderboardManager.getInstance().submitScore(
                        playerName,
                        score,
                        data.getMapPath(),
                        data.getKillCount(),
                        data.getCompletionTime());

                dialog.hide();

                // 显示确认
                Dialog confirmDialog = new Dialog("Score Submitted!", skin);
                confirmDialog.text("Your score has been saved to the leaderboard!");
                confirmDialog.button("OK");
                confirmDialog.show(stage);
            }
        });

        TextButton cancelBtn = new TextButton("Cancel", skin);
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        dialog.getButtonTable().add(submitBtn).width(120).padRight(20);
        dialog.getButtonTable().add(cancelBtn).width(120);

        dialog.show(stage);
    }

    private String getSubtitle() {
        if (data.isRandomMode()) {
            return data.isVictory() ? "Random Map Completed!" : "You were defeated...";
        }
        int level = data.getLevelNumber();
        String theme = data.getThemeName();
        if (data.isVictory()) {
            return "Level " + level + " - " + theme + " Conquered";
        } else {
            return "Level " + level + " Failed";
        }
    }

    private Color getRankColor(String rank) {
        switch (rank) {
            case "S":
                return COLOR_VICTORY;
            case "A":
                return Color.CYAN;
            case "B":
                return Color.GREEN;
            case "C":
                return Color.YELLOW;
            case "D":
                return Color.ORANGE;
            default:
                return Color.GRAY;
        }
    }

    private void goToNextLevel() {
        int currentLevel = data.getLevelNumber();
        int nextLevel = currentLevel + 1;
        String nextMapPath = "maps/level-" + nextLevel + ".properties";

        GameSettings.unlockLevel(nextLevel);

        if (!Gdx.files.internal(nextMapPath).exists() && !Gdx.files.local(nextMapPath).exists()) {
            RandomMapConfig config = RandomMapConfig.NORMAL.copy();
            // Theme order: 草原, 丛林, 荒漠, 冰原, 太空船
            String nextTheme = "Grassland"; // 1-4: 草原
            if (nextLevel >= 5)
                nextTheme = "Jungle"; // 5-8: 丛林
            if (nextLevel >= 9)
                nextTheme = "Desert"; // 9-12: 荒漠
            if (nextLevel >= 13)
                nextTheme = "Ice"; // 13-16: 冰原
            if (nextLevel >= 17)
                nextTheme = "Space"; // 17-20: 太空船

            config.setTheme(nextTheme);
            config.setDifficulty(Math.min(5, (nextLevel / 4) + 1));
            new MapGenerator(config).generateAndSave(nextMapPath);
        }
        game.setScreen(new ArmorSelectScreen(game, nextMapPath));
    }

    /**
     * Creates a solid color drawable.
     */
    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
        pm.dispose();
        return drawable;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // Clear screen with dark background
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background image
        if (backgroundTexture != null) {
            game.getSpriteBatch().begin();
            game.getSpriteBatch().setColor(0.6f, 0.6f, 0.6f, 1f); // Dim it down
            game.getSpriteBatch().draw(backgroundTexture, 0, 0, stage.getWidth(), stage.getHeight());
            game.getSpriteBatch().setColor(Color.WHITE);
            game.getSpriteBatch().end();
        }

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
        if (backgroundTexture != null)
            backgroundTexture.dispose();
    }
}
