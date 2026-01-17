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
import de.tum.cit.fop.maze.custom.CustomElementDefinition;
import de.tum.cit.fop.maze.custom.CustomElementManager;
import de.tum.cit.fop.maze.custom.ElementType;
import de.tum.cit.fop.maze.utils.UIConstants;

/**
 * Element Creator Screen - Multi-step wizard for creating custom game elements.
 * 
 * Steps:
 * 1. Element Type Selection
 * 2. Frame Count Selection (4 or 16)
 * 3. Sprite Upload
 * 4. Property Configuration
 * 5. Level Assignment
 * 6. Confirmation
 */
public class ElementCreatorScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Skin skin;

    private int currentStep = 1;
    private static final int TOTAL_STEPS = 6;

    // Wizard state
    private CustomElementDefinition currentElement;
    private String currentAction; // For sprite upload step

    // UI Components
    private Table contentTable;
    private Label stepLabel;
    private TextButton prevBtn;
    private TextButton nextBtn;

    public ElementCreatorScreen(MazeRunnerGame game) {
        this.game = game;
        this.skin = game.getSkin();
        this.stage = new Stage(new FitViewport(UIConstants.VIEWPORT_WIDTH, UIConstants.VIEWPORT_HEIGHT),
                game.getSpriteBatch());

        buildUI();
        showStep(1);
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(skin.newDrawable("white", new Color(0.08f, 0.08f, 0.12f, 1f)));
        stage.addActor(root);

        // Header
        Table header = new Table();
        Label title = new Label("ELEMENT CREATOR", skin, "title");
        title.setColor(Color.CYAN);
        header.add(title).padTop(20).padBottom(10).row();

        stepLabel = new Label("Step 1 / " + TOTAL_STEPS + ": Select Element Type", skin);
        stepLabel.setColor(Color.LIGHT_GRAY);
        header.add(stepLabel).padBottom(20);

        root.add(header).row();

        // Content Area
        contentTable = new Table();
        contentTable.setBackground(skin.newDrawable("white", new Color(0.12f, 0.12f, 0.18f, 0.95f)));
        contentTable.pad(30);
        root.add(contentTable).grow().pad(20).row();

        // Footer with navigation
        Table footer = new Table();

        TextButton backBtn = new TextButton("Cancel", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        footer.add(backBtn).width(150).height(50).padRight(30);

        prevBtn = new TextButton("< Previous", skin);
        prevBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (currentStep > 1) {
                    showStep(currentStep - 1);
                }
            }
        });
        footer.add(prevBtn).width(150).height(50).padRight(30);

        nextBtn = new TextButton("Next >", skin);
        nextBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (validateCurrentStep()) {
                    if (currentStep < TOTAL_STEPS) {
                        showStep(currentStep + 1);
                    } else {
                        saveAndFinish();
                    }
                }
            }
        });
        footer.add(nextBtn).width(200).height(50);

        root.add(footer).padTop(20).padBottom(30);
    }

    private void showStep(int step) {
        currentStep = step;
        contentTable.clearChildren();
        stepLabel.setText("Step " + step + " / " + TOTAL_STEPS + ": " + getStepName(step));

        prevBtn.setVisible(step > 1);
        nextBtn.setText(step == TOTAL_STEPS ? "Create Element" : "Next >");
        nextBtn.setColor(step == TOTAL_STEPS ? Color.GREEN : Color.WHITE);

        switch (step) {
            case 1:
                buildTypeSelectionStep();
                break;
            case 2:
                buildFrameCountStep();
                break;
            case 3:
                buildSpriteUploadStep();
                break;
            case 4:
                buildPropertyStep();
                break;
            case 5:
                buildLevelAssignmentStep();
                break;
            case 6:
                buildConfirmationStep();
                break;
        }
    }

    private String getStepName(int step) {
        switch (step) {
            case 1:
                return "Select Element Type";
            case 2:
                return "Choose Frame Count";
            case 3:
                return "Upload Sprites";
            case 4:
                return "Configure Properties";
            case 5:
                return "Assign to Levels";
            case 6:
                return "Confirm & Create";
            default:
                return "";
        }
    }

    // ==================== Step 1: Type Selection ====================

    private void buildTypeSelectionStep() {
        contentTable.add(new Label("Select the type of element you want to create:", skin)).padBottom(30).row();

        Table typeGrid = new Table();
        for (ElementType type : ElementType.values()) {
            Table typeCard = createTypeCard(type);
            typeGrid.add(typeCard).size(350, 200).pad(15);
            if (type.ordinal() % 2 == 1) {
                typeGrid.row();
            }
        }
        contentTable.add(typeGrid).row();

        // Name input
        contentTable.add(new Label("Element Name:", skin)).padTop(30).left().row();
        TextField nameField = new TextField("My Custom Element", skin);
        nameField.setName("nameField");
        contentTable.add(nameField).width(400).padTop(10);
    }

    private Table createTypeCard(ElementType type) {
        Table card = new Table();
        card.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.25f, 1f)));
        card.pad(15);

        Label nameLabel = new Label(type.getDisplayName(), skin);
        nameLabel.setFontScale(1.3f);
        nameLabel.setColor(Color.GOLD);
        card.add(nameLabel).padBottom(10).row();

        Label descLabel = new Label(getTypeDescription(type), skin);
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        descLabel.setColor(Color.LIGHT_GRAY);
        card.add(descLabel).width(300).row();

        TextButton selectBtn = new TextButton("Select", skin);
        selectBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Highlight selected
                for (Actor a : card.getParent().getChildren()) {
                    if (a instanceof Table) {
                        ((Table) a).setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.25f, 1f)));
                    }
                }
                card.setBackground(skin.newDrawable("white", new Color(0.2f, 0.4f, 0.3f, 1f)));
                card.setUserObject(type);
            }
        });
        card.add(selectBtn).padTop(15).width(120).height(40);

        // Store type in card for retrieval
        card.setUserObject(type);
        return card;
    }

    private String getTypeDescription(ElementType type) {
        switch (type) {
            case ENEMY:
                return "Hostile creatures that patrol and attack the player.";
            case WEAPON:
                return "Equipable items that the player can use to attack.";
            case OBSTACLE:
                return "Static or moving obstacles that damage on contact.";
            case ITEM:
                return "Collectibles that provide health or effects.";
            default:
                return "";
        }
    }

    // ==================== Step 2: Frame Count ====================

    private void buildFrameCountStep() {
        contentTable.add(new Label("Choose the number of animation frames:", skin)).padBottom(30).row();

        Table options = new Table();

        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);

        TextButton btn4 = new TextButton("4 Frames (Simple)", skin, "toggle");
        btn4.setChecked(true);
        btn4.setUserObject(4);
        group.add(btn4);
        options.add(btn4).size(250, 80).pad(20);

        TextButton btn16 = new TextButton("16 Frames (Detailed)", skin, "toggle");
        btn16.setUserObject(16);
        group.add(btn16);
        options.add(btn16).size(250, 80).pad(20);

        contentTable.add(options).row();

        // Preview info
        Label infoLabel = new Label(
                "4 frames: Recommended for simple animations.\n16 frames: For smooth, detailed animations.", skin);
        infoLabel.setColor(Color.GRAY);
        infoLabel.setAlignment(Align.center);
        contentTable.add(infoLabel).padTop(30);

        // Store group for retrieval
        contentTable.setUserObject(group);
    }

    // ==================== Step 3: Sprite Upload ====================

    private void buildSpriteUploadStep() {
        if (currentElement == null)
            return;

        String[] actions = currentElement.getType().getActions();
        if (currentAction == null) {
            currentAction = actions[0];
        }

        // Action tabs
        Table tabs = new Table();
        for (String action : actions) {
            TextButton tabBtn = new TextButton(action, skin);
            if (action.equals(currentAction)) {
                tabBtn.setColor(Color.CYAN);
            }
            tabBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    currentAction = action;
                    buildSpriteUploadStep(); // Rebuild
                }
            });
            tabs.add(tabBtn).width(120).height(40).padRight(10);
        }
        contentTable.add(tabs).padBottom(20).row();

        contentTable.add(new Label("Upload sprites for action: " + currentAction, skin)).padBottom(20).row();

        // Frame grid
        Table frameGrid = new Table();
        int frameCount = currentElement.getFrameCount();
        int cols = frameCount <= 4 ? 4 : 8;

        for (int i = 0; i < frameCount; i++) {
            final int frameIndex = i;
            Table frameSlot = new Table();
            frameSlot.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 1f)));
            frameSlot.pad(10);

            Label frameLabel = new Label("Frame " + (i + 1), skin);
            frameLabel.setFontScale(0.8f);
            frameSlot.add(frameLabel).row();

            // Path display
            String path = currentElement.getSpritePath(currentAction, i);
            String displayPath = (path != null && !path.isEmpty()) ? "✓ Set" : "Not set";
            Label pathLabel = new Label(displayPath, skin);
            pathLabel.setColor(path != null ? Color.GREEN : Color.RED);
            frameSlot.add(pathLabel).padTop(5).row();

            // Input field
            TextField pathField = new TextField(path != null ? path : "", skin);
            pathField.setMessageText("Enter file path...");
            pathField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    currentElement.setSpritePath(currentAction, frameIndex, pathField.getText());
                    pathLabel.setText(pathField.getText().isEmpty() ? "Not set" : "✓ Set");
                    pathLabel.setColor(pathField.getText().isEmpty() ? Color.RED : Color.GREEN);
                }
            });
            frameSlot.add(pathField).width(200).padTop(5);

            frameGrid.add(frameSlot).size(220, 120).pad(5);
            if ((i + 1) % cols == 0) {
                frameGrid.row();
            }
        }

        ScrollPane scroll = new ScrollPane(frameGrid, skin);
        scroll.setFadeScrollBars(false);
        contentTable.add(scroll).grow();
    }

    // ==================== Step 4: Properties ====================

    private void buildPropertyStep() {
        if (currentElement == null)
            return;

        contentTable.add(new Label("Configure element properties:", skin)).padBottom(20).row();

        Table propTable = new Table();

        for (String prop : currentElement.getType().getRequiredProperties()) {
            Table row = new Table();
            row.left();

            String displayName = currentElement.getType().getPropertyDisplayName(prop);
            Label label = new Label(displayName + ":", skin);
            label.setColor(Color.LIGHT_GRAY);
            row.add(label).width(200).left().padRight(20);

            Object defaultVal = currentElement.getProperties().get(prop);
            if (defaultVal instanceof Boolean) {
                CheckBox cb = new CheckBox("", skin);
                cb.setChecked((Boolean) defaultVal);
                cb.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        currentElement.setProperty(prop, cb.isChecked());
                    }
                });
                row.add(cb).left();
            } else if (defaultVal instanceof Number) {
                TextField tf = new TextField(defaultVal.toString(), skin);
                tf.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
                tf.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        try {
                            if (tf.getText().contains(".")) {
                                currentElement.setProperty(prop, Float.parseFloat(tf.getText()));
                            } else {
                                currentElement.setProperty(prop, Integer.parseInt(tf.getText()));
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
                row.add(tf).width(150).left();
            } else {
                TextField tf = new TextField(defaultVal != null ? defaultVal.toString() : "", skin);
                tf.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        currentElement.setProperty(prop, tf.getText());
                    }
                });
                row.add(tf).width(200).left();
            }

            propTable.add(row).fillX().padBottom(15).row();
        }

        // Spawn probability
        Table probRow = new Table();
        probRow.left();
        probRow.add(new Label("Spawn Probability:", skin)).width(200).left().padRight(20);
        Slider probSlider = new Slider(0f, 1f, 0.1f, false, skin);
        probSlider.setValue(currentElement.getSpawnProbability());
        Label probLabel = new Label(String.format("%.0f%%", probSlider.getValue() * 100), skin);
        probSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentElement.setSpawnProbability(probSlider.getValue());
                probLabel.setText(String.format("%.0f%%", probSlider.getValue() * 100));
            }
        });
        probRow.add(probSlider).width(200).left();
        probRow.add(probLabel).padLeft(10);
        propTable.add(probRow).fillX();

        contentTable.add(propTable).grow();
    }

    // ==================== Step 5: Level Assignment ====================

    private void buildLevelAssignmentStep() {
        if (currentElement == null)
            return;

        contentTable.add(new Label("Select levels where this element will appear:", skin)).padBottom(20).row();

        Table levelGrid = new Table();
        for (int i = 1; i <= 20; i++) {
            final int level = i;
            TextButton levelBtn = new TextButton(String.valueOf(i), skin, "toggle");
            levelBtn.setChecked(currentElement.isAssignedToLevel(level));
            levelBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (levelBtn.isChecked()) {
                        currentElement.assignToLevel(level);
                    } else {
                        currentElement.removeFromLevel(level);
                    }
                }
            });
            levelGrid.add(levelBtn).size(80, 60).pad(8);
            if (i % 5 == 0) {
                levelGrid.row();
            }
        }
        contentTable.add(levelGrid).row();

        // Quick select buttons
        Table quickBtns = new Table();
        TextButton selectAll = new TextButton("Select All", skin);
        selectAll.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 1; i <= 20; i++) {
                    currentElement.assignToLevel(i);
                }
                buildLevelAssignmentStep();
            }
        });
        quickBtns.add(selectAll).width(150).padRight(20);

        TextButton selectNone = new TextButton("Clear All", skin);
        selectNone.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 1; i <= 20; i++) {
                    currentElement.removeFromLevel(i);
                }
                buildLevelAssignmentStep();
            }
        });
        quickBtns.add(selectNone).width(150);

        contentTable.add(quickBtns).padTop(20);
    }

    // ==================== Step 6: Confirmation ====================

    private void buildConfirmationStep() {
        if (currentElement == null)
            return;

        contentTable.add(new Label("Review your element:", skin)).padBottom(20).row();

        Table summary = new Table();
        summary.left().top();

        addSummaryRow(summary, "Name", currentElement.getName());
        addSummaryRow(summary, "Type", currentElement.getType().getDisplayName());
        addSummaryRow(summary, "Frame Count", String.valueOf(currentElement.getFrameCount()));
        addSummaryRow(summary, "Sprites Complete", currentElement.isComplete() ? "Yes ✓" : "No ✗");
        addSummaryRow(summary, "Assigned Levels", currentElement.getAssignedLevels().toString());
        addSummaryRow(summary, "Spawn Probability",
                String.format("%.0f%%", currentElement.getSpawnProbability() * 100));

        // Properties
        summary.add(new Label("Properties:", skin)).left().padTop(20).colspan(2).row();
        for (String prop : currentElement.getType().getRequiredProperties()) {
            String displayName = currentElement.getType().getPropertyDisplayName(prop);
            Object value = currentElement.getProperties().get(prop);
            addSummaryRow(summary, "  " + displayName, value != null ? value.toString() : "N/A");
        }

        contentTable.add(summary).grow();

        if (!currentElement.isComplete()) {
            Label warningLabel = new Label("WARNING: Not all sprites are uploaded. Element may not display correctly.",
                    skin);
            warningLabel.setColor(Color.ORANGE);
            contentTable.row();
            contentTable.add(warningLabel).padTop(20);
        }
    }

    private void addSummaryRow(Table table, String label, String value) {
        Label labelL = new Label(label + ":", skin);
        labelL.setColor(Color.GRAY);
        table.add(labelL).left().padRight(20);

        Label valueL = new Label(value, skin);
        valueL.setColor(Color.WHITE);
        table.add(valueL).left().row();
    }

    // ==================== Validation & Save ====================

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                // Get selected type from highlighted card
                TextField nameField = stage.getRoot().findActor("nameField");
                String name = nameField != null ? nameField.getText().trim() : "Custom Element";
                if (name.isEmpty())
                    name = "Custom Element";

                ElementType selectedType = null;
                for (Actor actor : contentTable.getChildren()) {
                    if (actor instanceof Table) {
                        for (Actor child : ((Table) actor).getChildren()) {
                            if (child instanceof Table) {
                                Table card = (Table) child;
                                // Check if highlighted (green background)
                                if (card.getUserObject() instanceof ElementType) {
                                    selectedType = (ElementType) card.getUserObject();
                                }
                            }
                        }
                    }
                }
                if (selectedType == null) {
                    selectedType = ElementType.ENEMY; // Default
                }
                // Initialize element with default 4 frames
                currentElement = new CustomElementDefinition(name, selectedType, 4);
                return true;

            case 2:
                // Get frame count
                @SuppressWarnings("unchecked")
                ButtonGroup<TextButton> group = (ButtonGroup<TextButton>) contentTable.getUserObject();
                if (group != null) {
                    TextButton checked = group.getChecked();
                    if (checked != null && checked.getUserObject() instanceof Integer) {
                        int frames = (Integer) checked.getUserObject();
                        if (frames != currentElement.getFrameCount()) {
                            // Recreate element with new frame count
                            currentElement = new CustomElementDefinition(
                                    currentElement.getName(),
                                    currentElement.getType(),
                                    frames);
                        }
                    }
                }
                return true;

            case 3:
            case 4:
            case 5:
                return true; // No strict validation

            case 6:
                return true;

            default:
                return true;
        }
    }

    private void saveAndFinish() {
        CustomElementManager.getInstance().saveElement(currentElement);

        // Show success dialog
        Dialog dialog = new Dialog("Success", skin);
        dialog.text("Element '" + currentElement.getName() + "' created successfully!");
        dialog.button("OK", true);
        dialog.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        dialog.show(stage);
    }

    // ==================== Screen Lifecycle ====================

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
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
