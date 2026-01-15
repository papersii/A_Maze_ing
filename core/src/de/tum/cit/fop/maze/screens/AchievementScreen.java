package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.utils.*;

import java.util.List;
import java.util.Map;

/**
 * Achievement List Screen - 成就列表界面
 * 
 * 显示所有成就及其进度，支持分类筛选和统计数据展示。
 */
public class AchievementScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Skin skin;

    // UI Elements
    private Table rootTable;
    private Table achievementListTable;
    private ScrollPane scrollPane;
    private Label progressLabel;
    private Label totalGoldLabel;

    // Filter state
    private AchievementCategory currentFilter = null; // null = ALL

    // Cached drawables for rarity backgrounds
    private TextureRegionDrawable commonBg, rareBg, epicBg, legendaryBg;
    private Texture backgroundTexture;

    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.skin = game.getSkin();

        OrthographicCamera camera = new OrthographicCamera();
        Viewport viewport = new FitViewport(1920, 1080, camera);
        stage = new Stage(viewport, game.getSpriteBatch());

        // Load background
        try {
            backgroundTexture = new Texture(Gdx.files.internal("achievement_bg.jpg"));
        } catch (Exception e) {
            backgroundTexture = null;
        }

        createRarityBackgrounds();
        buildUI();
        refreshAchievementList();
    }

    private void createRarityBackgrounds() {
        commonBg = createColorDrawable(new Color(0.18f, 0.18f, 0.21f, 0.95f));
        rareBg = createColorDrawable(new Color(0.1f, 0.17f, 0.29f, 0.95f));
        epicBg = createColorDrawable(new Color(0.23f, 0.1f, 0.29f, 0.95f));
        legendaryBg = createColorDrawable(new Color(0.29f, 0.23f, 0.1f, 0.95f));
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
        pm.dispose();
        return drawable;
    }

    private void buildUI() {
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();
        stage.addActor(rootTable);

        // === Header ===
        buildHeader();

        // === Category Tabs ===
        buildCategoryTabs();

        // === Achievement List (ScrollPane) ===
        achievementListTable = new Table();
        achievementListTable.top();
        scrollPane = new ScrollPane(achievementListTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // Vertical scrolling only

        // Auto-focus scroll on hover so user doesn't need to click
        UIUtils.enableHoverScrollFocus(scrollPane, stage);

        rootTable.add(scrollPane).width(1600).minHeight(0).expandY().fillY().pad(10).row();

        // === Statistics Panel ===
        buildStatsPanel();

        // === Back Button ===
        TextButton backBtn = new TextButton("Back to Menu", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        rootTable.add(backBtn).width(200).height(50).pad(20);
    }

    private void buildHeader() {
        Table headerTable = new Table();
        headerTable.pad(20);

        // Title
        Label titleLabel = new Label("ACHIEVEMENTS", skin, "title");
        headerTable.add(titleLabel).left().expandX();

        // Progress
        int unlocked = AchievementManager.getUnlockedCount();
        int total = AchievementManager.getTotalCount();
        progressLabel = new Label("[" + unlocked + " / " + total + " Unlocked]", skin);
        progressLabel.setColor(Color.GOLD);
        headerTable.add(progressLabel).right();

        rootTable.add(headerTable).width(1600).row();

        // Sub stats row
        Table statsRow = new Table();
        int totalGold = AchievementManager.getTotalCoinsEarned();
        totalGoldLabel = new Label("Total Gold Earned: " + totalGold, skin);
        totalGoldLabel.setColor(Color.YELLOW);
        statsRow.add(totalGoldLabel).padRight(50);

        rootTable.add(statsRow).padBottom(10).row();
    }

    private void buildCategoryTabs() {
        Table tabsTable = new Table();
        tabsTable.pad(10);

        // Calculate button width: 1600px total width / 7 buttons - padding
        // Need to fit: ALL + 6 categories = 7 buttons
        float buttonWidth = 180f; // Compact width for each button
        float buttonHeight = 45f;

        // ALL tab
        TextButton allBtn = createCategoryButton("ALL", null);
        tabsTable.add(allBtn).width(buttonWidth).height(buttonHeight).padRight(8);

        // Category tabs - use shorter labels to fit
        for (AchievementCategory cat : AchievementCategory.values()) {
            // Use icon + abbreviated name (max 4 chars) for compact display
            String abbrev = cat.getDisplayName().substring(0,
                    Math.min(4, cat.getDisplayName().length()));
            String label = cat.getIcon() + " " + abbrev;
            TextButton catBtn = createCategoryButton(label, cat);
            tabsTable.add(catBtn).width(buttonWidth).height(buttonHeight).padRight(8);
        }

        // Wrap in a horizontal scroll pane as fallback for very small screens
        ScrollPane tabsScrollPane = new ScrollPane(tabsTable, skin);
        tabsScrollPane.setScrollingDisabled(false, true); // Horizontal scrolling only
        tabsScrollPane.setFadeScrollBars(true);
        tabsScrollPane.setScrollbarsVisible(false);

        rootTable.add(tabsScrollPane).width(1600).height(70).row();
    }

    private TextButton createCategoryButton(String label, AchievementCategory category) {
        TextButton btn = new TextButton(label, skin);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentFilter = category;
                refreshAchievementList();
            }
        });
        return btn;
    }

    private void buildStatsPanel() {
        Table statsPanel = new Table();
        statsPanel.pad(15);
        statsPanel.setBackground(createColorDrawable(new Color(0.15f, 0.15f, 0.18f, 0.9f)));

        Label statsTitle = new Label("=== STATISTICS ===", skin);
        statsTitle.setColor(Color.CYAN);
        statsPanel.add(statsTitle).colspan(4).center().padBottom(10).row();

        // Row 1
        addStatItem(statsPanel, "Total Kills", String.valueOf(getTotalKills()));
        addStatItem(statsPanel, "Max Combo", String.valueOf(getMaxCombo()));
        statsPanel.row();

        // Row 2
        addStatItem(statsPanel, "Sword Kills", String.valueOf(AchievementManager.getWeaponKills("Sword")));
        addStatItem(statsPanel, "Bow Kills", String.valueOf(AchievementManager.getWeaponKills("Bow")));
        statsPanel.row();

        // Row 3
        addStatItem(statsPanel, "Damage Absorbed", String.valueOf(AchievementManager.getTotalArmorAbsorbed()));
        addStatItem(statsPanel, "Levels Done", getLevelsCompleted());

        rootTable.add(statsPanel).width(1600).pad(20).row();
    }

    private void addStatItem(Table table, String name, String value) {
        Label nameLabel = new Label(name + ":", skin);
        nameLabel.setColor(Color.LIGHT_GRAY);
        Label valueLabel = new Label(value, skin);
        valueLabel.setColor(Color.WHITE);
        table.add(nameLabel).left().padRight(10);
        table.add(valueLabel).left().padRight(40);
    }

    private int getTotalKills() {
        // Sum up all weapon kills as approximation
        int total = 0;
        String[] weapons = { "Sword", "Bow", "Staff", "Crossbow", "Wand" };
        for (String w : weapons) {
            total += AchievementManager.getWeaponKills(w);
        }
        return total;
    }

    private int getMaxCombo() {
        return Gdx.app.getPreferences("maze_achievements").getInteger("max_combo_kills", 0);
    }

    private String getLevelsCompleted() {
        String completed = Gdx.app.getPreferences("maze_achievements").getString("levels_completed", "");
        int count = 0;
        if (!completed.isEmpty()) {
            count = completed.split(";").length - 1;
        }
        return count + "/20";
    }

    private void refreshAchievementList() {
        achievementListTable.clearChildren();

        Map<String, Achievement> allAchievements = AchievementManager.getAllAchievements();
        List<String> unlockedNames = AchievementManager.getUnlockedCards();

        for (Achievement achievement : allAchievements.values()) {
            // Apply filter
            if (currentFilter != null && achievement.getCategory() != currentFilter) {
                continue;
            }

            // Sync unlock status
            if (unlockedNames.contains(achievement.getName())) {
                achievement.setUnlocked(true);
            }

            // Build card
            Table card = buildAchievementCard(achievement);
            achievementListTable.add(card).width(1550).padBottom(8).row();
        }

        // Update header stats
        int unlocked = AchievementManager.getUnlockedCount();
        int total = currentFilter == null ? AchievementManager.getTotalCount()
                : AchievementManager.getAchievementsByCategory(currentFilter).size();
        progressLabel.setText("[" + unlocked + " / " + total + " Unlocked]");
    }

    private Table buildAchievementCard(Achievement achievement) {
        Table card = new Table();
        card.pad(12);

        // Set background based on rarity
        switch (achievement.getRarity()) {
            case COMMON:
                card.setBackground(commonBg);
                break;
            case RARE:
                card.setBackground(rareBg);
                break;
            case EPIC:
                card.setBackground(epicBg);
                break;
            case LEGENDARY:
                card.setBackground(legendaryBg);
                break;
        }

        // Left column: Icon + Name + Description
        Table leftCol = new Table();

        // Rarity icon
        String rarityIcon = achievement.getRarity().getIcon();
        Label iconLabel = new Label(rarityIcon, skin);
        iconLabel.setFontScale(1.0f);
        leftCol.add(iconLabel).padRight(15).top();

        // Name and description
        Table textCol = new Table();

        String displayName = achievement.isHidden() && !achievement.isUnlocked()
                ? "???"
                : achievement.getName();
        Label nameLabel = new Label(displayName, skin);
        nameLabel.setColor(getRarityTextColor(achievement.getRarity()));
        nameLabel.setFontScale(1.0f);
        nameLabel.setWrap(true);
        nameLabel.setAlignment(Align.left);
        textCol.add(nameLabel).left().width(800).row();

        String displayDesc = achievement.isHidden() && !achievement.isUnlocked()
                ? "???"
                : achievement.getDescription();
        Label descLabel = new Label(displayDesc, skin);
        descLabel.setColor(Color.LIGHT_GRAY);
        descLabel.setFontScale(0.7f);
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.left);
        textCol.add(descLabel).left().width(800);

        leftCol.add(textCol).left();
        card.add(leftCol).left().expandX();

        // Right column: Status + Reward
        Table rightCol = new Table();

        // Status or Progress
        if (achievement.isUnlocked()) {
            Label statusLabel = new Label("[OK] Unlocked", skin);
            statusLabel.setColor(Color.GREEN);
            rightCol.add(statusLabel).right().row();
        } else if (achievement.isOneTimeAchievement()) {
            Label statusLabel = new Label("[X] Locked", skin);
            statusLabel.setColor(Color.GRAY);
            rightCol.add(statusLabel).right().row();
        } else {
            // Progress bar
            Table progressTable = new Table();
            float percent = achievement.getProgressPercentage();
            int filled = (int) (percent * 10);
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < 10; i++) {
                bar.append(i < filled ? "#" : "-");
            }
            bar.append("] ");
            bar.append(achievement.getProgressString());
            Label progressLabel = new Label(bar.toString(), skin);
            progressLabel.setColor(Color.CYAN);
            progressLabel.setFontScale(0.8f);
            rightCol.add(progressLabel).right().row();
        }

        // Reward
        String rewardText = "+" + achievement.getGoldReward() + " Gold ("
                + achievement.getRarity().getDisplayName() + ")";
        Label rewardLabel = new Label(rewardText, skin);
        rewardLabel.setColor(Color.YELLOW);
        rewardLabel.setFontScale(0.7f);
        rightCol.add(rewardLabel).right();

        card.add(rightCol).right().padLeft(20);

        return card;
    }

    private Color getRarityTextColor(AchievementRarity rarity) {
        switch (rarity) {
            case COMMON:
                return Color.WHITE;
            case RARE:
                return new Color(0.4f, 0.7f, 1.0f, 1.0f);
            case EPIC:
                return new Color(0.8f, 0.4f, 1.0f, 1.0f);
            case LEGENDARY:
                return new Color(1.0f, 0.85f, 0.2f, 1.0f);
            default:
                return Color.WHITE;
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        refreshAchievementList(); // Refresh on show in case data changed
        // Initial scroll focus
        if (scrollPane != null) {
            stage.setScrollFocus(scrollPane);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background at full screen size (before viewport is applied)
        if (backgroundTexture != null) {
            game.getSpriteBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight());
            game.getSpriteBatch().begin();
            game.getSpriteBatch().setColor(0.4f, 0.4f, 0.4f, 1f); // Dim
            game.getSpriteBatch().draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            game.getSpriteBatch().setColor(1, 1, 1, 1);
            game.getSpriteBatch().end();
        }

        // Apply viewport for UI
        stage.getViewport().apply();
        stage.act(Math.min(delta, 1 / 30f));
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
