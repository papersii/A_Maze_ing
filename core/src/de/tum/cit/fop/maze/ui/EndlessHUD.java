package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.config.EndlessModeConfig;
import de.tum.cit.fop.maze.model.ComboSystem;
import de.tum.cit.fop.maze.model.Player;
import de.tum.cit.fop.maze.model.RageSystem;
import de.tum.cit.fop.maze.model.WaveSystem;
import de.tum.cit.fop.maze.model.items.Armor;
import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.utils.AchievementRarity;
import de.tum.cit.fop.maze.utils.AchievementUnlockInfo;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.List;

/**
 * 无尽模式专用HUD (Endless Mode HUD)
 * 
 * 基于GameHUD设计，添加无尽模式专有元素：
 * - COMBO计数器和倍率显示
 * - RAGE仪表盘
 * - 生存时间计时器
 * - 击杀计数器
 * - 当前区域指示器
 * 
 * 不包含出口导航箭头（无尽模式无出口）
 */
public class EndlessHUD implements Disposable {

    private final Stage stage;
    private final Player player;
    private final TextureManager textureManager;
    private final Skin skin;

    // === 核心系统引用 ===
    private ComboSystem comboSystem;
    private RageSystem rageSystem;
    private WaveSystem waveSystem;

    // === 基础HUD元素（复用自GameHUD） ===
    private Table livesTable;
    private TextButton menuButton;
    private Label fpsLabel;
    private Label coinLabel;
    private Label armorLabel;
    private Table weaponSlotsTable;
    private ProgressBar reloadBar;

    // === 无尽模式专用元素 ===
    private Label survivalTimeLabel; // 生存时间 MM:SS
    private Label killCountLabel; // 击杀计数
    private Label comboLabel; // COMBO显示
    private Label comboMultiplierLabel; // COMBO倍率
    private ProgressBar comboDecayBar; // COMBO衰减进度条
    private Label rageLabel; // RAGE等级名称
    private ProgressBar rageBar; // RAGE进度条 (0-100%)
    private Label zoneLabel; // 当前区域
    private Label scoreLabel; // 当前得分
    private Label waveLabel; // 当前波次

    // === 缓存UI元素 ===
    private Array<Image> cachedHearts = new Array<>();
    private int lastRenderedLiveCount = -1;
    private int lastWeaponIndex = -1;
    private int lastInventorySize = -1;
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable selectedSlotBg;
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable normalSlotBg;

    // === FPS计时器 ===
    private float fpsUpdateTimer = 0f;

    // === Achievement Popup ===
    private AchievementPopup achievementPopup;

    // === 游戏状态 ===
    private int totalKills = 0;
    private int currentScore = 0;
    private String currentZone = "Space";

    public EndlessHUD(SpriteBatch batch, Player player, Skin skin, TextureManager tm,
            Runnable onMenuClicked) {
        this.player = player;
        this.textureManager = tm;
        this.skin = skin;

        Viewport viewport = new FitViewport(1920, 1080);
        this.stage = new Stage(viewport, batch);

        buildUI(onMenuClicked);

        // Achievement Popup
        achievementPopup = new AchievementPopup(stage, skin);
    }

    /**
     * 设置核心系统引用
     */
    public void setSystems(ComboSystem combo, RageSystem rage, WaveSystem wave) {
        this.comboSystem = combo;
        this.rageSystem = rage;
        this.waveSystem = wave;
    }

    private void buildUI(Runnable onMenuClicked) {
        // Root Table
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();
        stage.addActor(rootTable);

        // === TOP BAR ===
        Table topBar = new Table();
        rootTable.add(topBar).growX().top().pad(15);

        // --- Left Section: Lives + Zone ---
        Table topLeft = new Table();
        topBar.add(topLeft).left().expandX();

        livesTable = new Table();
        topLeft.add(livesTable).left().row();

        // Zone Indicator
        Label.LabelStyle zoneStyle = new Label.LabelStyle(skin.getFont("font"), Color.CYAN);
        zoneLabel = new Label("Zone: Space", zoneStyle);
        topLeft.add(zoneLabel).left().padTop(5);

        // --- Center Section: Time + Kills + Wave ---
        Table topCenter = new Table();
        topBar.add(topCenter).center();

        // Survival Time (Big)
        Label.LabelStyle timeStyle = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        survivalTimeLabel = new Label("00:00", timeStyle);
        survivalTimeLabel.setFontScale(1.5f);
        topCenter.add(survivalTimeLabel).padBottom(5).row();

        // Wave Label
        Label.LabelStyle waveStyle = new Label.LabelStyle(skin.getFont("font"), Color.YELLOW);
        waveLabel = new Label("Wave 1", waveStyle);
        topCenter.add(waveLabel).padBottom(5).row();

        // Kill Count
        Label.LabelStyle killStyle = new Label.LabelStyle(skin.getFont("font"), Color.RED);
        killCountLabel = new Label("Kills: 0", killStyle);
        topCenter.add(killCountLabel);

        // --- Right Section: Score + FPS + Menu ---
        Table topRight = new Table();
        topBar.add(topRight).right().expandX();

        // Score (Big)
        Label.LabelStyle scoreStyle = new Label.LabelStyle(skin.getFont("font"), Color.GOLD);
        scoreLabel = new Label("Score: 0", scoreStyle);
        scoreLabel.setFontScale(1.2f);
        topRight.add(scoreLabel).right().padRight(20).row();

        // Coins
        Label.LabelStyle coinStyle = new Label.LabelStyle(skin.getFont("font"), Color.GOLD);
        coinLabel = new Label("Coins: 0", coinStyle);
        topRight.add(coinLabel).right().padRight(20).row();

        // FPS
        Label.LabelStyle fpsStyle = new Label.LabelStyle(skin.getFont("font"), Color.YELLOW);
        fpsLabel = new Label("FPS: --", fpsStyle);
        fpsLabel.setFontScale(0.8f);
        topRight.add(fpsLabel).right().padRight(20).row();

        // Menu Button
        menuButton = new TextButton("Menu", skin);
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (onMenuClicked != null)
                    onMenuClicked.run();
            }
        });
        topRight.add(menuButton).right().width(120).height(50);

        // === COMBO AND RAGE SECTION (Below Top Bar) ===
        rootTable.row();
        Table comboRageBar = new Table();
        rootTable.add(comboRageBar).growX().padTop(10).padLeft(20).padRight(20);

        // --- COMBO Section (Left) ---
        Table comboSection = new Table();
        comboRageBar.add(comboSection).left().expandX();

        Label.LabelStyle comboStyle = new Label.LabelStyle(skin.getFont("font"), Color.ORANGE);
        comboLabel = new Label("COMBO: 0", comboStyle);
        comboLabel.setFontScale(1.3f);
        comboSection.add(comboLabel).left().row();

        Label.LabelStyle multStyle = new Label.LabelStyle(skin.getFont("font"), Color.YELLOW);
        comboMultiplierLabel = new Label("x1.0", multStyle);
        comboSection.add(comboMultiplierLabel).left().row();

        // COMBO Decay Bar
        ProgressBar.ProgressBarStyle comboBarStyle = new ProgressBar.ProgressBarStyle();
        comboBarStyle.background = skin.newDrawable("white", new Color(0.3f, 0.2f, 0.1f, 0.8f));
        comboBarStyle.knobBefore = skin.newDrawable("white", new Color(1f, 0.6f, 0f, 1f));
        comboDecayBar = new ProgressBar(0f, 1f, 0.01f, false, comboBarStyle);
        comboDecayBar.setValue(0f);
        comboSection.add(comboDecayBar).width(200).height(10).left();

        // --- RAGE Section (Right) ---
        Table rageSection = new Table();
        comboRageBar.add(rageSection).right().expandX();

        Label.LabelStyle rageStyle = new Label.LabelStyle(skin.getFont("font"), Color.RED);
        rageLabel = new Label("RAGE: Calm", rageStyle);
        rageLabel.setFontScale(1.1f);
        rageSection.add(rageLabel).right().row();

        // RAGE Progress Bar
        ProgressBar.ProgressBarStyle rageBarStyle = new ProgressBar.ProgressBarStyle();
        rageBarStyle.background = skin.newDrawable("white", new Color(0.2f, 0.1f, 0.1f, 0.8f));
        rageBarStyle.knobBefore = skin.newDrawable("white", new Color(1f, 0.2f, 0.1f, 1f));
        rageBar = new ProgressBar(0f, 100f, 1f, false, rageBarStyle);
        rageBar.setValue(0f);
        rageSection.add(rageBar).width(200).height(15).right();

        // === BOTTOM SECTION: Weapon Slots + Armor ===
        rootTable.row();
        rootTable.add().growY(); // Push to bottom

        rootTable.row();

        // Armor Status
        Label.LabelStyle armorStyle = new Label.LabelStyle(skin.getFont("font"), Color.CYAN);
        armorLabel = new Label("", armorStyle);
        rootTable.add(armorLabel).bottom().padBottom(10);

        rootTable.row();

        // Weapon Slots
        weaponSlotsTable = new Table();
        weaponSlotsTable.pad(5);
        rootTable.add(weaponSlotsTable).bottom().padBottom(10);

        // Reload Bar
        ProgressBar.ProgressBarStyle reloadStyle = new ProgressBar.ProgressBarStyle();
        reloadStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f));
        reloadStyle.knobBefore = skin.newDrawable("white", new Color(0.3f, 0.7f, 1f, 1f));
        reloadBar = new ProgressBar(0f, 1f, 0.01f, false, reloadStyle);
        reloadBar.setVisible(false);
        rootTable.row();
        rootTable.add(reloadBar).width(200).height(15).bottom().padBottom(20);
    }

    /**
     * 更新HUD
     */
    public void update(float delta) {
        // FPS更新
        fpsUpdateTimer += delta;
        if (fpsUpdateTimer >= 1.0f) {
            fpsLabel.setText("FPS: " + Gdx.graphics.getFramesPerSecond());
            fpsUpdateTimer = 0f;
        }

        // === 生命条 ===
        updateLivesDisplay();

        // === 金币 ===
        coinLabel.setText("Coins: " + player.getCoins());

        // === 分数 ===
        scoreLabel.setText("Score: " + String.format("%,d", currentScore));

        // === 生存时间 ===
        if (waveSystem != null) {
            survivalTimeLabel.setText(waveSystem.getFormattedTime());
            waveLabel.setText(waveSystem.getWaveName());
        }

        // === 击杀数 ===
        killCountLabel.setText("Kills: " + totalKills);

        // === COMBO显示 ===
        if (comboSystem != null) {
            int combo = comboSystem.getCurrentCombo();
            comboLabel.setText("COMBO: " + combo);

            float multiplier = comboSystem.getMultiplier();
            comboMultiplierLabel.setText("x" + String.format("%.1f", multiplier));

            // 根据COMBO等级改变颜色
            if (combo >= 50) {
                comboLabel.setColor(Color.MAGENTA);
            } else if (combo >= 20) {
                comboLabel.setColor(Color.RED);
            } else if (combo >= 10) {
                comboLabel.setColor(Color.ORANGE);
            } else if (combo >= 5) {
                comboLabel.setColor(Color.YELLOW);
            } else {
                comboLabel.setColor(Color.WHITE);
            }

            // COMBO衰减进度条
            comboDecayBar.setValue(comboSystem.getDecayProgress());
            comboDecayBar.setVisible(comboSystem.isActive());

            // COMBO名称显示
            String comboName = comboSystem.getComboName();
            if (!comboName.isEmpty()) {
                comboMultiplierLabel.setText(comboName + " x" + String.format("%.1f", multiplier));
            }
        }

        // === RAGE显示 ===
        if (rageSystem != null) {
            rageLabel.setText("RAGE: " + rageSystem.getRageLevelName());
            rageBar.setValue(rageSystem.getRagePercentage());

            // 根据RAGE等级改变颜色
            int rageLevel = rageSystem.getRageLevelIndex();
            if (rageLevel >= 4) {
                rageLabel.setColor(Color.MAGENTA);
            } else if (rageLevel >= 3) {
                rageLabel.setColor(Color.RED);
            } else if (rageLevel >= 2) {
                rageLabel.setColor(Color.ORANGE);
            } else if (rageLevel >= 1) {
                rageLabel.setColor(Color.YELLOW);
            } else {
                rageLabel.setColor(Color.GREEN);
            }
        }

        // === 区域指示器 ===
        zoneLabel.setText("Zone: " + currentZone);

        // 根据区域设置颜色
        switch (currentZone) {
            case "Grassland":
                zoneLabel.setColor(Color.GREEN);
                break;
            case "Jungle":
                zoneLabel.setColor(Color.PURPLE);
                break;
            case "Desert":
                zoneLabel.setColor(Color.GOLD);
                break;
            case "Ice":
                zoneLabel.setColor(Color.CYAN);
                break;
            case "Space":
                zoneLabel.setColor(Color.BLUE);
                break;
            default:
                zoneLabel.setColor(Color.WHITE);
                break;
        }

        // === 护甲状态 ===
        Armor armor = player.getEquippedArmor();
        if (armor != null && armor.hasShield()) {
            armorLabel.setText(armor.getName() + " [" + armor.getCurrentShield() + "/" + armor.getMaxShield() + "]");
            armorLabel.setVisible(true);
        } else if (armor != null) {
            armorLabel.setText(armor.getName() + " [BROKEN]");
            armorLabel.setColor(Color.GRAY);
            armorLabel.setVisible(true);
        } else {
            armorLabel.setVisible(false);
        }

        // === 武器栏 ===
        updateWeaponSlots();

        // === 装弹进度条 ===
        Weapon currentWeapon = player.getCurrentWeapon();
        if (currentWeapon != null && currentWeapon.isRanged() && currentWeapon.isReloading()) {
            reloadBar.setVisible(true);
            float reloadProgress = 1f - (currentWeapon.getCurrentReloadTimer() / currentWeapon.getReloadTime());
            reloadBar.setValue(reloadProgress);
        } else {
            reloadBar.setVisible(false);
        }

        stage.act(delta);
    }

    private void updateLivesDisplay() {
        int lives = player.getLives();
        if (lives != lastRenderedLiveCount && textureManager.heartRegion != null) {
            livesTable.clearChildren();
            cachedHearts.clear();

            for (int i = 0; i < lives; i++) {
                Image heart = new Image(textureManager.heartRegion);
                cachedHearts.add(heart);
                livesTable.add(heart).size(50, 50).pad(4);
            }
            lastRenderedLiveCount = lives;
        }
    }

    private void updateWeaponSlots() {
        int currentWeaponIdx = player.getCurrentWeaponIndex();
        List<Weapon> inventory = player.getInventory();

        boolean needRebuild = lastInventorySize != inventory.size() || weaponSlotsTable.getChildren().isEmpty();

        if (selectedSlotBg == null) {
            selectedSlotBg = skin.newDrawable("white", new Color(0.3f, 0.5f, 0.8f, 0.8f));
            normalSlotBg = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.6f));
        }

        if (needRebuild) {
            weaponSlotsTable.clearChildren();
            for (int i = 0; i < inventory.size(); i++) {
                Weapon w = inventory.get(i);
                Table slot = new Table();
                slot.setBackground(i == currentWeaponIdx ? selectedSlotBg : normalSlotBg);

                Label slotLabel = new Label(
                        (i + 1) + ": " + w.getName().substring(0, Math.min(3, w.getName().length())), skin);
                slotLabel.setFontScale(0.7f);
                slot.add(slotLabel).pad(5);

                weaponSlotsTable.add(slot).width(70).height(40).pad(3);
            }
            lastInventorySize = inventory.size();
            lastWeaponIndex = currentWeaponIdx;
        } else if (currentWeaponIdx != lastWeaponIndex) {
            for (int i = 0; i < weaponSlotsTable.getChildren().size; i++) {
                Table slot = (Table) weaponSlotsTable.getChildren().get(i);
                slot.setBackground(i == currentWeaponIdx ? selectedSlotBg : normalSlotBg);
            }
            lastWeaponIndex = currentWeaponIdx;
        }
    }

    // === 游戏状态更新方法 ===

    public void setTotalKills(int kills) {
        this.totalKills = kills;
    }

    public void setCurrentScore(int score) {
        this.currentScore = score;
    }

    public void setCurrentZone(String zone) {
        this.currentZone = zone;
    }

    public void incrementKills() {
        this.totalKills++;
    }

    public void addScore(int amount) {
        this.currentScore += amount;
    }

    // === Achievement Methods ===

    public void showAchievement(AchievementUnlockInfo info) {
        if (achievementPopup != null && info != null) {
            achievementPopup.queueAchievement(info.getName(), info.getRarity(), info.getGoldReward());
        }
    }

    public void showAchievement(String name, AchievementRarity rarity, int goldReward) {
        if (achievementPopup != null) {
            achievementPopup.queueAchievement(name, rarity, goldReward);
        }
    }

    // === Standard Methods ===

    public void render() {
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
