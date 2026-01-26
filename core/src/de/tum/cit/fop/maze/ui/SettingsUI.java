package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.utils.AudioManager;
import de.tum.cit.fop.maze.utils.UIConstants;

/**
 * Settings UI - Clean layout with NO overlapping.
 * Label width is generous to prevent text being covered by controls.
 */
public class SettingsUI {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Skin skin;
    private final Runnable onBackAction;

    private Table contentTable;
    private Label statusLabel;

    // Background for in-game overlay
    private Texture backgroundTexture;

    // Key remap buttons
    private TextButton btnUp, btnDown, btnLeft, btnRight, btnAttack, btnSwitchWeapon;
    private String remappingKeyName = null;

    // 关键！标签列宽度要足够大，避免被控件覆盖
    private static final float LABEL_WIDTH = 230f;
    private static final float SLIDER_WIDTH = 160f;
    private static final float VALUE_WIDTH = 50f;
    private static final float BTN_WIDTH = 100f;
    private static final float KEY_LABEL_WIDTH = 90f;
    private static final float KEY_BTN_WIDTH = 110f;

    public SettingsUI(MazeRunnerGame game, Stage stage, Runnable onBackAction) {
        this.game = game;
        this.stage = stage;
        this.skin = game.getSkin();
        this.onBackAction = onBackAction;
        setupInputProcessor();
    }

    /**
     * Build settings UI without background (for SettingsScreen which handles its
     * own background).
     */
    public Table build() {
        contentTable = new Table();
        contentTable.pad(50, 40, 20, 40); // 上50 左右40 下20

        buildContent();
        return contentTable;
    }

    /**
     * Build settings UI with background (for in-game settings overlay).
     * Uses the same background as SettingsScreen with dim effect.
     */
    public Table buildWithBackground() {
        return buildWithBackground(null);
    }

    /**
     * Build settings UI with screenshot background (for in-game settings overlay).
     * Uses the provided screenshot texture as background with dim effect.
     * 
     * @param screenshotTexture 游戏暂停时的截图纹理，如果为null则使用默认背景
     */
    public Table buildWithBackground(Texture screenshotTexture) {
        contentTable = new Table();

        // 优先使用传入的截图作为背景
        if (screenshotTexture != null) {
            // 使用游戏暂停时的截图作为背景
            TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(screenshotTexture));
            contentTable.setBackground(bgDrawable);
            contentTable.setColor(0.5f, 0.5f, 0.5f, 1f); // 轻微暗化以突出设置内容
        } else {
            // Fallback: 加载固定背景图片
            try {
                backgroundTexture = new Texture(Gdx.files.internal("settings_bg.png"));
            } catch (Exception e) {
                backgroundTexture = null;
            }

            if (backgroundTexture != null) {
                TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(backgroundTexture));
                contentTable.setBackground(bgDrawable);
                contentTable.setColor(0.4f, 0.4f, 0.4f, 1f);
            } else {
                // Fallback to dark semi-transparent background
                contentTable.setBackground(skin.newDrawable("white", 0.04f, 0.04f, 0.06f, 0.95f));
            }
        }

        contentTable.pad(50, 40, 20, 40); // 上50 左右40 下20

        buildContent();
        return contentTable;
    }

    /**
     * Dispose of background texture resources.
     */
    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }

    private void buildContent() {
        // ===== Title =====
        Label title = new Label("Settings", skin, "title");
        title.setFontScale(1.2f);
        contentTable.add(title).colspan(3).padBottom(25).row();

        // ===== Audio Section =====
        addSectionHeader("Audio");
        addVolumeRow();
        addMusicRow();

        // ===== Gameplay Section =====
        addSectionHeader("Gameplay");
        addWalkSpeedRow();
        addRunSpeedRow();
        addCameraRow();
        addFogRow();
        addAttackRangeRow();
        addMouseAimingRow(); // 添加鼠标瞄准开关
        addHint();

        // ===== Controls Section =====
        addSectionHeader("Controls");
        addControlsSection();

        // ===== Back Button =====
        addBackButton();
    }

    private void addSectionHeader(String text) {
        Table header = new Table();

        Image leftLine = new Image(skin.newDrawable("white", 0.5f, 0.45f, 0.3f, 0.6f));
        header.add(leftLine).width(100).height(2).padRight(20);

        Label label = new Label(text, skin);
        label.setColor(UIConstants.SETTINGS_SECTION_TITLE);
        label.setFontScale(1.1f);
        header.add(label);

        Image rightLine = new Image(skin.newDrawable("white", 0.5f, 0.45f, 0.3f, 0.6f));
        header.add(rightLine).width(100).height(2).padLeft(20);

        contentTable.add(header).colspan(3).padTop(20).padBottom(15).row();
    }

    // ==================== Audio Section ====================

    private void addVolumeRow() {
        Label label = new Label("Volume:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final Slider slider = new Slider(0, 1, 0.1f, false, skin);
        slider.setValue(0.5f);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                AudioManager.getInstance().setVolume(slider.getValue());
            }
        });
        contentTable.add(slider).width(SLIDER_WIDTH).height(12).colspan(2).left();
        contentTable.row().padBottom(10);
    }

    private void addMusicRow() {
        Label label = new Label("Music:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        boolean musicOn = AudioManager.getInstance().isMusicEnabled();
        final TextButton btn = new TextButton(musicOn ? "ON" : "OFF", skin);
        btn.getLabel().setFontScale(0.9f);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                boolean on = !AudioManager.getInstance().isMusicEnabled();
                AudioManager.getInstance().setMusicEnabled(on);
                btn.setText(on ? "ON" : "OFF");
            }
        });
        contentTable.add(btn).width(BTN_WIDTH).height(32).colspan(2).left();
        contentTable.row().padBottom(8);
    }

    // ==================== Gameplay Section ====================

    private void addWalkSpeedRow() {
        Label label = new Label("Walk Speed:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final Label valueLabel = new Label(String.format("%.1f", GameSettings.playerWalkSpeed), skin);
        valueLabel.setFontScale(0.95f);

        Slider slider = new Slider(1f, 15f, 0.5f, false, skin);
        slider.setValue(GameSettings.playerWalkSpeed);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                float v = ((Slider) a).getValue();
                GameSettings.playerWalkSpeed = v;
                valueLabel.setText(String.format("%.1f", v));
            }
        });
        contentTable.add(slider).width(SLIDER_WIDTH).height(12);
        contentTable.add(valueLabel).width(VALUE_WIDTH).padLeft(10);
        contentTable.row().padBottom(10);
    }

    private void addRunSpeedRow() {
        Label label = new Label("Run Speed:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final Label valueLabel = new Label(String.format("%.1f", GameSettings.playerRunSpeed), skin);
        valueLabel.setFontScale(0.95f);

        Slider slider = new Slider(5f, 20f, 0.5f, false, skin);
        slider.setValue(GameSettings.playerRunSpeed);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                float v = ((Slider) a).getValue();
                GameSettings.playerRunSpeed = v;
                valueLabel.setText(String.format("%.1f", v));
            }
        });
        contentTable.add(slider).width(SLIDER_WIDTH).height(12);
        contentTable.add(valueLabel).width(VALUE_WIDTH).padLeft(10);
        contentTable.row().padBottom(10);
    }

    private void addCameraRow() {
        Label label = new Label("Camera Zoom:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final Label valueLabel = new Label(String.format("%.2f", GameSettings.cameraZoom), skin);
        valueLabel.setFontScale(0.95f);

        Slider slider = new Slider(0.3f, 1.5f, 0.05f, false, skin);
        slider.setValue(GameSettings.cameraZoom);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                float v = ((Slider) a).getValue();
                GameSettings.cameraZoom = v;
                valueLabel.setText(String.format("%.2f", v));
            }
        });
        contentTable.add(slider).width(SLIDER_WIDTH).height(12);
        contentTable.add(valueLabel).width(VALUE_WIDTH).padLeft(10);
        contentTable.row().padBottom(10);
    }

    private void addFogRow() {
        Label label = new Label("Fog of War:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final TextButton btn = new TextButton(GameSettings.isFogEnabled() ? "ON" : "OFF", skin);
        btn.getLabel().setFontScale(0.9f);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                boolean on = !GameSettings.isFogEnabled();
                GameSettings.setFogEnabled(on);
                btn.setText(on ? "ON" : "OFF");
            }
        });
        contentTable.add(btn).width(BTN_WIDTH).height(32).colspan(2).left();
        contentTable.row().padBottom(6);
    }

    private void addAttackRangeRow() {
        Label label = new Label("Attack Range:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final TextButton btn = new TextButton(GameSettings.isShowAttackRange() ? "ON" : "OFF", skin);
        btn.getLabel().setFontScale(0.9f);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                boolean on = !GameSettings.isShowAttackRange();
                GameSettings.setShowAttackRange(on);
                btn.setText(on ? "ON" : "OFF");
            }
        });
        contentTable.add(btn).width(BTN_WIDTH).height(32).colspan(2).left();
        contentTable.row().padBottom(6);
    }

    private void addMouseAimingRow() {
        Label label = new Label("Mouse Aiming:", skin);
        label.setFontScale(1.0f);
        contentTable.add(label).width(LABEL_WIDTH).right().padRight(30);

        final TextButton btn = new TextButton(GameSettings.isUseMouseAiming() ? "ON" : "OFF", skin);
        btn.getLabel().setFontScale(0.9f);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                boolean on = !GameSettings.isUseMouseAiming();
                GameSettings.setUseMouseAiming(on);
                btn.setText(on ? "ON" : "OFF");
            }
        });
        contentTable.add(btn).width(BTN_WIDTH).height(32).colspan(2).left();
        contentTable.row().padBottom(6);
    }

    private void addHint() {
        Label hint = new Label("* Fog mode: vision range is fixed", skin);
        hint.setColor(0.6f, 0.6f, 0.6f, 1f);
        hint.setFontScale(0.8f);
        contentTable.add(hint).colspan(3).padBottom(8).row();
    }

    // ==================== Controls Section ====================

    private void addControlsSection() {
        // Status label
        statusLabel = new Label("Click button → Press new key", skin);
        statusLabel.setColor(Color.YELLOW);
        statusLabel.setFontScale(0.9f);
        contentTable.add(statusLabel).colspan(3).padBottom(15).row();

        // Create key buttons
        btnUp = makeKeyButton("UP");
        btnDown = makeKeyButton("DOWN");
        btnLeft = makeKeyButton("LEFT");
        btnRight = makeKeyButton("RIGHT");
        btnAttack = makeKeyButton("ATTACK");
        btnSwitchWeapon = makeKeyButton("SWITCH_WEAPON");

        // Row 1: Up, Down, Left
        Table keyRow1 = new Table();
        addKeyBinding(keyRow1, "Up:", btnUp);
        addKeyBinding(keyRow1, "Down:", btnDown);
        addKeyBinding(keyRow1, "Left:", btnLeft);
        contentTable.add(keyRow1).colspan(3).padBottom(12).row();

        // Row 2: Right, Attack, Switch
        Table keyRow2 = new Table();
        addKeyBinding(keyRow2, "Right:", btnRight);
        addKeyBinding(keyRow2, "Attack:", btnAttack);
        addKeyBinding(keyRow2, "Switch:", btnSwitchWeapon);
        contentTable.add(keyRow2).colspan(3).padBottom(10).row();
    }

    private TextButton makeKeyButton(final String keyName) {
        final TextButton btn = new TextButton(getKeyName(keyName), skin);
        btn.getLabel().setFontScale(0.85f);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                startRemap(keyName, btn);
            }
        });
        return btn;
    }

    private void addKeyBinding(Table table, String labelText, Actor btn) {
        Label label = new Label(labelText, skin);
        label.setFontScale(0.85f);
        table.add(label).width(KEY_LABEL_WIDTH).right().padRight(15);
        table.add(btn).width(KEY_BTN_WIDTH).height(34).padRight(30);
    }

    private void addBackButton() {
        TextButton backBtn = new TextButton("Back / Save", skin);
        backBtn.getLabel().setFontScale(0.9f);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                GameSettings.saveKeyBindingsOnly();
                if (onBackAction != null)
                    onBackAction.run();
            }
        });
        contentTable.add(backBtn).colspan(3).width(220).height(45).padTop(20);
    }

    // ===== Key Remapping Logic =====

    private String getKeyName(String keyName) {
        int code = -1;
        switch (keyName) {
            case "UP":
                code = GameSettings.KEY_UP;
                break;
            case "DOWN":
                code = GameSettings.KEY_DOWN;
                break;
            case "LEFT":
                code = GameSettings.KEY_LEFT;
                break;
            case "RIGHT":
                code = GameSettings.KEY_RIGHT;
                break;
            case "ATTACK":
                code = GameSettings.KEY_ATTACK;
                break;
            case "SWITCH_WEAPON":
                code = GameSettings.KEY_SWITCH_WEAPON;
                break;
        }
        return Input.Keys.toString(code);
    }

    private void startRemap(String keyName, TextButton btn) {
        remappingKeyName = keyName;
        statusLabel.setText("Press key for " + keyName);
        btn.setText("...");
    }

    private void updateButtons() {
        btnUp.setText(getKeyName("UP"));
        btnDown.setText(getKeyName("DOWN"));
        btnLeft.setText(getKeyName("LEFT"));
        btnRight.setText(getKeyName("RIGHT"));
        btnAttack.setText(getKeyName("ATTACK"));
        btnSwitchWeapon.setText(getKeyName("SWITCH_WEAPON"));
    }

    public boolean handleKeyDown(int keycode) {
        if (remappingKeyName != null) {
            if (keycode == Input.Keys.ESCAPE) {
                remappingKeyName = null;
                updateButtons();
                statusLabel.setText("Cancelled");
                return true;
            }
            switch (remappingKeyName) {
                case "UP":
                    GameSettings.KEY_UP = keycode;
                    break;
                case "DOWN":
                    GameSettings.KEY_DOWN = keycode;
                    break;
                case "LEFT":
                    GameSettings.KEY_LEFT = keycode;
                    break;
                case "RIGHT":
                    GameSettings.KEY_RIGHT = keycode;
                    break;
                case "ATTACK":
                    GameSettings.KEY_ATTACK = keycode;
                    break;
                case "SWITCH_WEAPON":
                    GameSettings.KEY_SWITCH_WEAPON = keycode;
                    break;
            }
            remappingKeyName = null;
            updateButtons();
            statusLabel.setText("Saved");
            return true;
        }
        return false;
    }

    private void setupInputProcessor() {
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                return handleKeyDown(keycode);
            }
        });
    }
}
