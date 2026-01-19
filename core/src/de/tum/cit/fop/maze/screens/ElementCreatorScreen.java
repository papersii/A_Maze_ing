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
import de.tum.cit.fop.maze.utils.GameLogger;
import de.tum.cit.fop.maze.utils.GameLogger;

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
    private int selectedFrames = 4; // State for Step 2

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
        buildUI();
        showStep(0); // Start at Dashboard
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
        nextBtn.setVisible(step > 0); // Visible for all except Dashboard
        nextBtn.setText(step == TOTAL_STEPS ? "Create Element" : "Next >");
        nextBtn.setColor(step == TOTAL_STEPS ? Color.GREEN : Color.WHITE);

        switch (step) {
            case 0:
                prevBtn.setVisible(false);
                nextBtn.setVisible(false);
                buildDashboardStep();
                break;
            case 1:
                prevBtn.setVisible(true); // Back to dashboard
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
            case 0:
                return "Dashboard";
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
        contentTable.add(new Label("Select element type:", skin)).padBottom(15).row();

        Table typeGrid = new Table();
        int col = 0;
        for (ElementType type : ElementType.values()) {
            Table typeCard = createTypeCard(type);
            typeGrid.add(typeCard).size(220, 120).pad(8); // Smaller cards
            col++;
            if (col >= 3) { // 3 columns for better fit
                typeGrid.row();
                col = 0;
            }
        }

        // Wrap in ScrollPane for overflow
        ScrollPane scrollPane = new ScrollPane(typeGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // Only vertical scroll
        contentTable.add(scrollPane).maxHeight(320).fillX().row();

        // Name input
        contentTable.add(new Label("Element Name:", skin)).padTop(15).left().row();

        String defaultName = "My Custom Element";
        if (currentElement != null) {
            defaultName = currentElement.getName();
        }

        TextField nameField = new TextField(defaultName, skin);
        nameField.setName("nameField");
        contentTable.add(nameField).width(400).padTop(8);
    }

    private Table createTypeCard(ElementType type) {
        Table card = new Table();
        card.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.25f, 1f)));
        card.pad(8); // Smaller padding

        // Pre-select logic
        if (currentElement != null && currentElement.getType() == type) {
            card.setBackground(skin.newDrawable("white", new Color(0.2f, 0.4f, 0.3f, 1f)));
            card.setUserObject("SELECTED:" + type.name());
        } else {
            card.setUserObject(type.name());
        }

        Label nameLabel = new Label(type.getDisplayName(), skin);
        nameLabel.setFontScale(1.1f); // Smaller font
        nameLabel.setColor(Color.GOLD);
        card.add(nameLabel).padBottom(5).row();

        // Simpler description for compact layout
        String shortDesc = getShortTypeDescription(type);
        Label descLabel = new Label(shortDesc, skin);
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        descLabel.setColor(Color.LIGHT_GRAY);
        descLabel.setFontScale(0.85f); // Smaller description font
        card.add(descLabel).width(190).row();

        TextButton selectBtn = new TextButton("Select", skin);
        selectBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Clear selection from all cards
                for (Actor a : card.getParent().getChildren()) {
                    if (a instanceof Table) {
                        Table t = (Table) a;
                        t.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.25f, 1f)));
                        // Reset userObject to just the type (remove selection marker)
                        if (t.getUserObject() instanceof String) {
                            String marker = (String) t.getUserObject();
                            if (marker.startsWith("SELECTED:")) {
                                t.setUserObject(marker.substring(9)); // Remove marker
                            }
                        }
                    }
                }
                // Mark this card as selected (green background + marker)
                card.setBackground(skin.newDrawable("white", new Color(0.2f, 0.4f, 0.3f, 1f)));
                card.setUserObject("SELECTED:" + type.name());
            }
        });
        card.add(selectBtn).padTop(8).width(90).height(28); // Smaller button

        // Store type name in card for retrieval (not marked as selected yet)
        return card;
    }

    private String getShortTypeDescription(ElementType type) {
        switch (type) {
            case PLAYER:
                return "Custom skin";
            case ENEMY:
                return "Custom mob";
            case WEAPON:
                return "Custom weapon";
            case OBSTACLE:
                return "Trap/obstacle";
            case ITEM:
                return "Pickup item";
            default:
                return "";
        }
    }

    /**
     * Validate Step 1 and initialize/update currentElement
     */
    private boolean validateCurrentStep() {
        if (currentStep == 1) {
            // Find selected type
            ElementType selectedType = null;

            // Search in contentTable children - typeGrid is now inside a ScrollPane
            for (Actor child : contentTable.getChildren()) {
                Table gridToSearch = null;

                if (child instanceof ScrollPane) {
                    // ScrollPane wraps the typeGrid
                    Actor widget = ((ScrollPane) child).getActor();
                    if (widget instanceof Table) {
                        gridToSearch = (Table) widget;
                    }
                } else if (child instanceof Table) {
                    gridToSearch = (Table) child;
                }

                if (gridToSearch != null) {
                    for (Actor card : gridToSearch.getChildren()) {
                        if (card.getUserObject() instanceof String) {
                            String marker = (String) card.getUserObject();
                            if (marker.startsWith("SELECTED:")) {
                                try {
                                    selectedType = ElementType.valueOf(marker.substring(9));
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }
            }

            if (selectedType == null) {
                // Show error - simplified by just blocking print
                GameLogger.warn("ElementCreator", "No type selected");
                return false;
            }

            // Get Name
            String name = "Custom Element";
            for (Actor child : contentTable.getChildren()) {
                if (child instanceof TextField) {
                    name = ((TextField) child).getText();
                }
            }

            if (name.trim().isEmpty()) {
                GameLogger.warn("ElementCreator", "Name is empty");
                return false;
            }

            // Create or Update Element
            if (currentElement == null) {
                currentElement = new CustomElementDefinition(name, selectedType, 4);
            } else {
                currentElement.setName(name);
                // Changing type is tricky as sprites depend on actions.
                // If type changes, sprites might be invalid.
                if (currentElement.getType() != selectedType) {
                    currentElement = new CustomElementDefinition(name, selectedType, 4); // Reset if type changed
                }
            }
            return true;
        } else if (currentStep == 2) {
            if (selectedFrames != currentElement.getFrameCount()) {
                String oldId = currentElement.getId();
                CustomElementDefinition newEl = new CustomElementDefinition(
                        currentElement.getName(),
                        currentElement.getType(),
                        selectedFrames);
                newEl.setId(oldId); // Preserve ID
                newEl.getProperties().putAll(currentElement.getProperties());
                newEl.getAssignedLevels().addAll(currentElement.getAssignedLevels());
                currentElement = newEl;
            }
            return true;
        }
        return true;
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

        // Use regular buttons with manual toggle via color
        // Initialize state
        if (currentElement != null) {
            selectedFrames = currentElement.getFrameCount();
        } else {
            selectedFrames = 4;
        }

        final TextButton btn4 = new TextButton("4 Frames (Simple)", skin);
        final TextButton btn16 = new TextButton("16 Frames (Detailed)", skin);

        // Update visual state helper
        Runnable updateButtons = () -> {
            btn4.setColor(selectedFrames == 4 ? Color.GREEN : Color.WHITE);
            btn16.setColor(selectedFrames == 16 ? Color.GREEN : Color.WHITE);
        };

        updateButtons.run();

        btn4.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedFrames = 4;
                updateButtons.run();
            }
        });

        btn16.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedFrames = 16;
                updateButtons.run();
            }
        });

        options.add(btn4).size(250, 80).pad(20);
        options.add(btn16).size(250, 80).pad(20);

        contentTable.add(options).row();

        // Preview info
        Label infoLabel = new Label(
                "4 frames: Recommended for simple animations.\n16 frames: For smooth, detailed animations.", skin);
        infoLabel.setColor(Color.GRAY);
        infoLabel.setAlignment(Align.center);
        contentTable.add(infoLabel).padTop(30);

    }

    // ==================== Step 0: Dashboard ====================

    private void buildDashboardStep() {
        contentTable.add(new Label("Managed Elements:", skin)).padBottom(20).left().row();

        Table listTable = new Table();
        listTable.top().left();

        java.util.Collection<CustomElementDefinition> elements = CustomElementManager.getInstance().getAllElements();

        for (final CustomElementDefinition el : elements) {
            Table row = new Table();
            row.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.3f, 1f)));
            row.pad(10);

            Label nameLbl = new Label(el.getName(), skin);
            nameLbl.setColor(Color.WHITE);
            row.add(nameLbl).expandX().left().padRight(20);

            Label typeLbl = new Label(el.getType().toString(), skin);
            typeLbl.setColor(Color.YELLOW);
            row.add(typeLbl).width(100).padRight(20);

            TextButton editBtn = new TextButton("Edit", skin);
            editBtn.setColor(Color.ORANGE);
            editBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    // Load for editing
                    currentElement = el;
                    currentAction = null;
                    showStep(1);
                }
            });
            row.add(editBtn).width(80).height(30).padRight(10);

            TextButton deleteBtn = new TextButton("Delete", skin);
            deleteBtn.setColor(Color.RED);
            deleteBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CustomElementManager.getInstance().deleteElement(el.getId());
                    showStep(0); // Refresh list
                }
            });
            row.add(deleteBtn).width(80).height(30);

            listTable.add(row).growX().padBottom(5).row();
        }

        ScrollPane scroll = new ScrollPane(listTable, skin);
        contentTable.add(scroll).grow().row();

        TextButton createBtn = new TextButton("+ Create New Element", skin);
        createBtn.setColor(Color.GREEN);
        createBtn.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentElement = null; // New element
                currentAction = null;
                showStep(1);
            }
        });
        contentTable.add(createBtn).height(50).width(250).padTop(20);
    }

    // ==================== Step 3: Sprite Upload ====================

    // Track which frame is selected
    private int selectedFrameIndex = 0;

    private void buildSpriteUploadStep() {
        if (currentElement == null)
            return;

        // Clear existing content to prevent duplication
        contentTable.clearChildren();

        String[] actions = currentElement.getType().getActions();
        if (currentAction == null) {
            currentAction = actions[0];
        }

        int frameCount = currentElement.getFrameCount();

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
                    selectedFrameIndex = 0;
                    buildSpriteUploadStep(); // Rebuild
                }
            });
            tabs.add(tabBtn).width(120).height(40).padRight(10);
        }
        contentTable.add(tabs).padBottom(15).row();

        // Instructions
        Label instructionLabel = new Label("Click a frame, then paste the image file path below", skin);
        instructionLabel.setColor(Color.YELLOW);
        contentTable.add(instructionLabel).padBottom(5).row();

        Label hintLabel = new Label("Tip: Right-click file in Finder → Hold Option → Copy as Pathname", skin);
        hintLabel.setFontScale(0.8f);
        hintLabel.setColor(Color.GRAY);
        contentTable.add(hintLabel).padBottom(15).row();

        // Frame grid
        Table frameGrid = new Table();
        int cols = frameCount <= 4 ? 4 : 4;

        for (int i = 0; i < frameCount; i++) {
            final int frameIndex = i;
            Table frameSlot = new Table();

            // Highlight selected frame
            if (i == selectedFrameIndex) {
                frameSlot.setBackground(skin.newDrawable("white", new Color(0.2f, 0.5f, 0.3f, 1f)));
            } else {
                frameSlot.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 1f)));
            }
            frameSlot.pad(10);

            Label frameLabel = new Label("Frame " + (i + 1), skin);
            frameLabel.setFontScale(0.9f);
            frameLabel.setColor(i == selectedFrameIndex ? Color.WHITE : Color.GOLD);
            frameSlot.add(frameLabel).row();

            // Path display
            String path = currentElement.getSpritePath(currentAction, i);
            String displayText = (path != null && !path.isEmpty()) ? "✓ " + getFileName(path) : "(empty)";
            Label pathLabel = new Label(displayText, skin);
            pathLabel.setFontScale(0.7f);
            pathLabel.setColor(path != null && !path.isEmpty() ? Color.GREEN : Color.LIGHT_GRAY);
            pathLabel.setEllipsis(true);
            frameSlot.add(pathLabel).width(140).padTop(5);

            // Make the slot clickable
            frameSlot.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            frameSlot.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {

                @Override
                public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                        int pointer, int button) {
                    selectedFrameIndex = frameIndex;
                    buildSpriteUploadStep();
                    return true;
                }
            });

            frameGrid.add(frameSlot).size(170, 70).pad(5);
            if ((i + 1) % cols == 0) {
                frameGrid.row();
            }
        }

        ScrollPane scroll = new ScrollPane(frameGrid, skin);
        scroll.setFadeScrollBars(false);
        contentTable.add(scroll).height(180).fillX().row();

        // Path input section
        Table inputSection = new Table();
        inputSection.setBackground(skin.newDrawable("white", new Color(0.15f, 0.15f, 0.2f, 1f)));
        inputSection.pad(15);

        inputSection.add(new Label("Selected: Frame " + (selectedFrameIndex + 1) + " (" + currentAction + ")", skin))
                .left().padBottom(10).row();

        Table inputRow = new Table();
        TextField pathField = new TextField("", skin);
        pathField.setMessageText("Paste file path here (e.g., /Users/name/image.png)");
        inputRow.add(pathField).width(500).height(40).padRight(10);

        TextButton setBtn = new TextButton("Set Path", skin);
        setBtn.setColor(Color.GREEN);
        setBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String path = pathField.getText().trim();
                if (!path.isEmpty()) {
                    currentElement.setSpritePath(currentAction, selectedFrameIndex, path);
                    // Move to next frame
                    if (selectedFrameIndex < frameCount - 1) {
                        selectedFrameIndex++;
                    }
                    buildSpriteUploadStep();
                }
            }
        });
        inputRow.add(setBtn).width(100).height(40);

        inputSection.add(inputRow).row();

        // Clear button
        TextButton clearBtn = new TextButton("Clear Selected", skin);
        clearBtn.setColor(Color.CORAL);
        clearBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentElement.setSpritePath(currentAction, selectedFrameIndex, null);
                disposePreviewTextures();
                buildSpriteUploadStep();
            }
        });
        inputSection.add(clearBtn).width(150).height(35).padTop(10).left();

        contentTable.add(inputSection).fillX().padTop(10).row();

        // Sprite preview section
        buildSpritePreview(frameCount);
    }

    // Preview textures - need to track for disposal
    private java.util.List<Texture> previewTextures = new java.util.ArrayList<>();

    /**
     * Build sprite preview showing uploaded images
     */
    private void buildSpritePreview(int frameCount) {
        // Count how many sprites are set
        int setCount = 0;
        for (int i = 0; i < frameCount; i++) {
            String path = currentElement.getSpritePath(currentAction, i);
            if (path != null && !path.isEmpty())
                setCount++;
        }

        if (setCount == 0) {
            Label noPreview = new Label("No sprites uploaded yet for " + currentAction, skin);
            noPreview.setColor(Color.GRAY);
            contentTable.add(noPreview).padTop(10);
            return;
        }

        // Preview section
        Table previewSection = new Table();
        previewSection.setBackground(skin.newDrawable("white", new Color(0.1f, 0.15f, 0.1f, 1f)));
        previewSection.pad(10);

        Label previewLabel = new Label("Preview: " + currentAction + " (" + setCount + "/" + frameCount + " frames)",
                skin);
        previewLabel.setColor(Color.LIME);
        previewSection.add(previewLabel).left().padBottom(10).row();

        // Dispose previous textures
        disposePreviewTextures();

        // Preview thumbnails
        Table thumbnailRow = new Table();
        for (int i = 0; i < frameCount; i++) {
            String path = currentElement.getSpritePath(currentAction, i);
            Table thumb = new Table();
            thumb.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 1f)));

            if (path != null && !path.isEmpty()) {
                if (path.startsWith("internal:")) {
                    Label defaultLabel = new Label("DEF", skin);
                    defaultLabel.setColor(Color.CYAN);
                    thumb.add(defaultLabel).size(48, 48);
                } else {
                    try {
                        // Try to load the texture
                        com.badlogic.gdx.files.FileHandle file = Gdx.files.absolute(path);
                        if (!file.exists()) {
                            file = Gdx.files.local(path);
                        }
                        if (file.exists()) {
                            Texture tex = new Texture(file);
                            previewTextures.add(tex);
                            Image img = new Image(tex);
                            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                            thumb.add(img).size(48, 48);
                        } else {
                            Label errLabel = new Label("?", skin);
                            errLabel.setColor(Color.RED);
                            thumb.add(errLabel).size(48, 48);
                        }
                    } catch (Exception e) {
                        Label errLabel = new Label("!", skin);
                        errLabel.setColor(Color.ORANGE);
                        thumb.add(errLabel).size(48, 48);
                    }
                }
            } else {
                Label emptyLabel = new Label("-", skin);
                emptyLabel.setColor(Color.DARK_GRAY);
                thumb.add(emptyLabel).size(48, 48);
            }
            thumbnailRow.add(thumb).size(56, 56).pad(3);
        }
        previewSection.add(thumbnailRow);

        contentTable.add(previewSection).fillX().padTop(10);
    }

    /**
     * Dispose preview textures to avoid memory leaks
     */
    private void disposePreviewTextures() {
        for (Texture tex : previewTextures) {
            try {
                tex.dispose();
            } catch (Exception ignored) {
            }
        }
        previewTextures.clear();
    }

    /**
     * Extract filename from full path
     */
    private String getFileName(String path) {
        if (path == null || path.isEmpty())
            return "";
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
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
            if (defaultVal == null) {
                defaultVal = currentElement.getType().getDefaultValue(prop);
            }
            if (defaultVal instanceof Boolean) {
                // 使用 TextButton 替代 CheckBox（因为皮肤中没有 CheckBox 样式）
                final boolean[] checked = { (Boolean) defaultVal };
                final TextButton toggleBtn = new TextButton(checked[0] ? "ON" : "OFF", skin);
                toggleBtn.setColor(checked[0] ? Color.GREEN : Color.GRAY);
                toggleBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        checked[0] = !checked[0];
                        toggleBtn.setText(checked[0] ? "ON" : "OFF");
                        toggleBtn.setColor(checked[0] ? Color.GREEN : Color.GRAY);
                        currentElement.setProperty(prop, checked[0]);
                    }
                });
                row.add(toggleBtn).width(80).left();
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
        propTable.add(probRow).fillX().padBottom(15).row(); // Added padding

        // Spawn Count
        Table countRow = new Table();
        countRow.left();
        countRow.add(new Label("Max Spawn Count:", skin)).width(200).left().padRight(20);

        final Slider countSlider = new Slider(1, 20, 1, false, skin);
        countSlider.setValue(currentElement.getSpawnCount());
        final Label countLabel = new Label(String.valueOf((int) countSlider.getValue()), skin);

        countSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentElement.setSpawnCount((int) countSlider.getValue());
                countLabel.setText(String.valueOf((int) countSlider.getValue()));
            }
        });

        countRow.add(countSlider).width(200).left();
        countRow.add(countLabel).padLeft(10);
        propTable.add(countRow).fillX();

        contentTable.add(propTable).grow();
    }

    // ==================== Step 5: Level Assignment ====================

    private void buildLevelAssignmentStep() {
        if (currentElement == null)
            return;

        contentTable.add(new Label("Select levels and spawn rates:", skin)).padBottom(20).row();

        Table listTable = new Table();
        listTable.top().left();

        for (int i = 1; i <= 20; i++) {
            final int level = i;
            final boolean assigned = currentElement.isAssignedToLevel(level);
            final float currentProb = currentElement.getSpawnProbability(level);

            Table row = new Table();
            row.pad(5);
            row.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, assigned ? 0.8f : 0.3f)));

            final TextButton toggleBtn = new TextButton("Level " + level, skin);
            toggleBtn.setChecked(assigned);

            final Slider probSlider = new Slider(0f, 1f, 0.05f, false, skin);
            probSlider.setValue(assigned ? (currentProb > 0 ? currentProb : 1.0f) : 1.0f);
            probSlider.setTouchable(assigned ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                    : com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            probSlider.setColor(assigned ? Color.WHITE : Color.GRAY);

            final Label probLabel = new Label(String.format("%.0f%%", probSlider.getValue() * 100), skin);
            probLabel.setColor(assigned ? Color.WHITE : Color.GRAY);

            toggleBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    boolean isChecked = toggleBtn.isChecked();
                    row.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, isChecked ? 0.8f : 0.3f)));

                    if (isChecked) {
                        currentElement.assignToLevel(level, probSlider.getValue());
                    } else {
                        currentElement.removeFromLevel(level);
                    }

                    probSlider.setTouchable(isChecked ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                            : com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
                    probSlider.setColor(isChecked ? Color.WHITE : Color.GRAY);
                    probLabel.setColor(isChecked ? Color.WHITE : Color.GRAY);
                }
            });

            probSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (toggleBtn.isChecked()) {
                        currentElement.assignToLevel(level, probSlider.getValue());
                        probLabel.setText(String.format("%.0f%%", probSlider.getValue() * 100));
                    }
                }
            });

            row.add(toggleBtn).width(100).padRight(20);
            row.add(probSlider).width(200).padRight(20);
            row.add(probLabel).width(50);

            listTable.add(row).growX().padBottom(5).row();
        }

        ScrollPane scroll = new ScrollPane(listTable, skin);
        scroll.setFadeScrollBars(false);
        contentTable.add(scroll).grow().row();

        // Quick select buttons
        Table quickBtns = new Table();
        TextButton selectAll = new TextButton("Select All", skin);
        selectAll.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (int i = 1; i <= 20; i++) {
                    currentElement.assignToLevel(i, 1.0f);
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

    // Old validateCurrentStep removed. logic moved to new method.

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
