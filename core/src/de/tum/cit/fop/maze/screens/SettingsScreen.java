package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.utils.AudioManager;

public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final Screen parentScreen;
    private final Stage stage;

    // UI Elements
    private Label statusLabel;
    private TextButton btnUp, btnDown, btnLeft, btnRight, btnAttack, btnSwitchWeapon;
    private Slider volumeSlider;
    private TextButton muteBtn;

    // Remapping State
    private String remappingKeyName = null;

    public SettingsScreen(MazeRunnerGame game, Screen parentScreen) {
        this.game = game;
        this.parentScreen = parentScreen;
        this.stage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Skin skin = game.getSkin();

        // Title
        root.add(new Label("Settings", skin, "title")).colspan(2).padBottom(30).row();

        // --- Audio Settings ---
        root.add(new Label("Audio", skin)).colspan(2).padBottom(5).row();

        // Volume
        Table audioTable = new Table();
        audioTable.add(new Label("Vol:", skin)).right().padRight(10);
        volumeSlider = new Slider(0, 1, 0.1f, false, skin);
        volumeSlider.setValue(0.5f); // Should get from AudioManager
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                AudioManager.getInstance().setVolume(volumeSlider.getValue());
            }
        });
        audioTable.add(volumeSlider).width(200).padRight(20);

        // Mute
        boolean isEnabled = AudioManager.getInstance().isMusicEnabled();
        muteBtn = new TextButton("Music: " + (isEnabled ? "ON" : "OFF"), skin);
        muteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean newState = !AudioManager.getInstance().isMusicEnabled();
                AudioManager.getInstance().setMusicEnabled(newState);
                muteBtn.setText("Music: " + (newState ? "ON" : "OFF"));
            }
        });
        audioTable.add(muteBtn).width(120);
        root.add(audioTable).colspan(2).padBottom(20).row();

        // --- Gameplay Settings (Merged) ---
        root.add(new Label("Gameplay", skin)).colspan(2).padBottom(5).row();
        Table gameplayTable = new Table();

        // Walk Speed
        gameplayTable.add(new Label("Walk Spd:", skin)).right().padRight(10);
        final Label walkLabel = new Label(String.format("%.1f", GameSettings.playerWalkSpeed), skin);
        Slider walkSlider = new Slider(1f, 15f, 0.5f, false, skin);
        walkSlider.setValue(GameSettings.playerWalkSpeed);
        walkSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.playerWalkSpeed = ((Slider) actor).getValue();
                walkLabel.setText(String.format("%.1f", GameSettings.playerWalkSpeed));
            }
        });
        gameplayTable.add(walkSlider).width(150);
        gameplayTable.add(walkLabel).width(40).padLeft(5).padRight(20);

        // Run Speed
        gameplayTable.add(new Label("Run Spd:", skin)).right().padRight(10);
        final Label runLabel = new Label(String.format("%.1f", GameSettings.playerRunSpeed), skin);
        Slider runSlider = new Slider(5f, 20f, 0.5f, false, skin);
        runSlider.setValue(GameSettings.playerRunSpeed);
        runSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.playerRunSpeed = ((Slider) actor).getValue();
                runLabel.setText(String.format("%.1f", GameSettings.playerRunSpeed));
            }
        });
        gameplayTable.add(runSlider).width(150);
        gameplayTable.add(runLabel).width(40).padLeft(5);

        root.add(gameplayTable).colspan(2).padBottom(20).row();

        // --- Key Bindings ---
        root.add(new Label("Controls", skin)).colspan(2).padBottom(5).row();
        statusLabel = new Label("Click button -> Press key", skin);
        statusLabel.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
        root.add(statusLabel).colspan(2).padBottom(10).row();

        btnUp = createKeyButton("Up", "UP", skin);
        btnDown = createKeyButton("Down", "DOWN", skin);
        btnLeft = createKeyButton("Left", "LEFT", skin);
        btnRight = createKeyButton("Right", "RIGHT", skin);
        btnAttack = createKeyButton("Attack", "ATTACK", skin);
        btnSwitchWeapon = createKeyButton("Switch Weapon", "SWITCH_WEAPON", skin);

        Table keyTable = new Table();
        addToKeyTable(keyTable, "Up:", btnUp);
        addToKeyTable(keyTable, "Down:", btnDown);
        addToKeyTable(keyTable, "Left:", btnLeft);
        addToKeyTable(keyTable, "Right:", btnRight);
        addToKeyTable(keyTable, "Atk:", btnAttack);
        addToKeyTable(keyTable, "Switch:", btnSwitchWeapon);
        root.add(keyTable).colspan(2).padBottom(20).row();

        // --- Navigation ---
        TextButton backBtn = new TextButton(parentScreen != null ? "Resume" : "Back / Save", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Only save settings when accessed from Main Menu (not in-game)
                // In-game changes are session-only and reset on next level
                if (parentScreen == null) {
                    GameSettings.saveAsUserDefaults();
                }
                // Also always save key bindings (they are persistent)
                GameSettings.saveKeyBindingsOnly();

                if (parentScreen != null) {
                    game.setScreen(parentScreen);
                } else {
                    game.goToMenu();
                }
            }
        });
        root.add(backBtn).colspan(2).width(200).height(50).padTop(10);

        // Input Processor
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (remappingKeyName != null) {
                    handleRemap(keycode);
                    return true;
                }
                return false;
            }
        });
    }

    private TextButton createKeyButton(String label, String keyName, Skin skin) {
        TextButton btn = new TextButton(getKeyName(keyName), skin);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startRemap(keyName, btn);
            }
        });
        return btn;
    }

    private void addToKeyTable(Table table, String label, Actor actor) {
        table.add(new Label(label, game.getSkin())).right().pad(5);
        table.add(actor).left().width(100).height(35).pad(5);
        if (table.getCells().size >= 4 && table.getCells().size % 4 == 0)
            table.row(); // Wrap ? No.
        // Let's do 3 columns? Or just flow.
        // The prompt uses simpler adds.
    }

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

    private void handleRemap(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            remappingKeyName = null;
            updateButtons();
            statusLabel.setText("Cancelled");
            return;
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
    }

    private void updateButtons() {
        btnUp.setText(getKeyName("UP"));
        btnDown.setText(getKeyName("DOWN"));
        btnLeft.setText(getKeyName("LEFT"));
        btnRight.setText(getKeyName("RIGHT"));
        btnAttack.setText(getKeyName("ATTACK"));
        btnSwitchWeapon.setText(getKeyName("SWITCH_WEAPON"));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
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
