package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.model.InventorySystem;
import de.tum.cit.fop.maze.model.items.Potion;
import de.tum.cit.fop.maze.model.weapons.Weapon;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.List;

/**
 * 背包界面 (Inventory UI)
 * 
 * 显示玩家的武器和药水库存，支持选择、使用和丢弃操作。
 * 使用 Scene2D 的 Window 组件作为弹窗容器。
 */
public class InventoryUI {

    private final Stage stage;
    private final Skin skin;
    private final TextureManager textureManager;
    private final InventorySystem inventorySystem;

    // UI Elements
    private Window inventoryWindow;
    private Table weaponsTable;
    private Table potionsTable;
    private Label detailLabel;
    private TextButton useButton;
    private TextButton dropButton;
    private TextButton closeButton;

    // State
    private boolean visible = false;
    private Object selectedItem = null; // Weapon or Potion
    private int selectedItemIndex = -1;
    private boolean isWeaponSelected = false;

    // Callbacks
    private Runnable onCloseCallback;

    // Style Cache
    private TextureRegionDrawable selectedSlotBg;
    private TextureRegionDrawable normalSlotBg;
    private TextureRegionDrawable emptySlotBg;

    // Constants - 放大1.5倍以确保文字不会与边框切到
    private static final float WINDOW_WIDTH = 1125f;  // 750 * 1.5
    private static final float WINDOW_HEIGHT = 930f;  // 620 * 1.5
    private static final float SLOT_SIZE = 96f;   // 放大槽位格子
    private static final float SLOT_PADDING = 12f; // 增加间距

    public InventoryUI(Stage stage, Skin skin, TextureManager textureManager, InventorySystem inventorySystem) {
        this.stage = stage;
        this.skin = skin;
        this.textureManager = textureManager;
        this.inventorySystem = inventorySystem;

        createSlotStyles();
        buildUI();

        // Register inventory change listener
        inventorySystem.setOnInventoryChanged(this::refreshUI);
    }

    private void createSlotStyles() {
        // Create drawable backgrounds for slots
        selectedSlotBg = new TextureRegionDrawable(
            createColoredRegion(new Color(0.3f, 0.6f, 0.9f, 0.9f)));
        normalSlotBg = new TextureRegionDrawable(
            createColoredRegion(new Color(0.25f, 0.25f, 0.25f, 0.8f)));
        emptySlotBg = new TextureRegionDrawable(
            createColoredRegion(new Color(0.15f, 0.15f, 0.15f, 0.6f)));
    }

    private TextureRegion createColoredRegion(Color color) {
        // Use a 1x1 white pixel stretched
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, 
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private void buildUI() {
        // Create main window
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = skin.getFont("font");
        windowStyle.titleFontColor = Color.WHITE;
        windowStyle.background = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.95f));

        inventoryWindow = new Window("Bag", windowStyle);
        inventoryWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        inventoryWindow.setPosition(
            (stage.getWidth() - WINDOW_WIDTH) / 2,
            (stage.getHeight() - WINDOW_HEIGHT) / 2
        );
        inventoryWindow.setMovable(true);
        inventoryWindow.setModal(true);
        inventoryWindow.setVisible(false);

        // Main content table
        Table contentTable = new Table();
        contentTable.pad(25).padTop(40);

        // === Weapons Section ===
        Label weaponsLabel = new Label("Weapons / 武器", skin);
        weaponsLabel.setColor(Color.GOLD);
        contentTable.add(weaponsLabel).left().padBottom(10);
        contentTable.row();

        weaponsTable = new Table();
        weaponsTable.pad(5);
        contentTable.add(weaponsTable).left().padBottom(20);
        contentTable.row();

        // === Potions Section ===
        Label potionsLabel = new Label("Potions / 药水", skin);
        potionsLabel.setColor(Color.CYAN);
        contentTable.add(potionsLabel).left().padBottom(10);
        contentTable.row();

        potionsTable = new Table();
        potionsTable.pad(5);
        contentTable.add(potionsTable).left().padBottom(20);
        contentTable.row();

        // === Detail Section ===
        Table detailSection = new Table();
        detailSection.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.7f)));
        detailSection.pad(10);

        detailLabel = new Label("Select an item to view details\n选择物品查看详情", skin);
        detailLabel.setWrap(true);
        detailLabel.setAlignment(Align.topLeft);
        detailSection.add(detailLabel).width(WINDOW_WIDTH - 80).minHeight(90);

        contentTable.add(detailSection).growX().padBottom(15);
        contentTable.row();

        // === Button Row ===
        Table buttonRow = new Table();

        useButton = new TextButton("Use / 使用", skin);
        useButton.setDisabled(true);
        useButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onUseClicked();
            }
        });

        dropButton = new TextButton("Drop / 丢弃", skin);
        dropButton.setDisabled(true);
        dropButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onDropClicked();
            }
        });

        closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });

        buttonRow.add(useButton).width(200).height(70).padRight(15);
        buttonRow.add(dropButton).width(200).height(70).padRight(15);
        buttonRow.add(closeButton).width(240).height(70);

        contentTable.add(buttonRow).right();

        inventoryWindow.add(contentTable).grow().pad(5);
        stage.addActor(inventoryWindow);

        // Initial refresh
        refreshUI();
    }

    /**
     * 刷新背包界面显示
     */
    public void refreshUI() {
        refreshWeaponsDisplay();
        refreshPotionsDisplay();
        updateDetailSection();
        updateButtonStates();
    }

    private void refreshWeaponsDisplay() {
        weaponsTable.clearChildren();

        List<Weapon> weapons = inventorySystem.getWeapons();
        int currentWeaponIdx = inventorySystem.getCurrentWeaponIndex();

        for (int i = 0; i < inventorySystem.getMaxWeapons(); i++) {
            Table slot = createSlot();

            if (i < weapons.size()) {
                Weapon weapon = weapons.get(i);
                final int index = i;

                // Slot background
                boolean isSelected = isWeaponSelected && selectedItemIndex == i;
                boolean isEquipped = i == currentWeaponIdx;
                slot.setBackground(isSelected ? selectedSlotBg : normalSlotBg);

                // Weapon icon placeholder (use first letter as fallback)
                Label iconLabel = new Label(weapon.getName().substring(0, 1).toUpperCase(), skin);
                iconLabel.setFontScale(1.5f);
                iconLabel.setColor(isEquipped ? Color.GOLD : Color.WHITE);
                slot.add(iconLabel).center();

                // Click listener
                slot.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectWeapon(index);
                    }
                });

                // Equipped indicator
                if (isEquipped) {
                    Label equippedLabel = new Label("E", skin);
                    equippedLabel.setFontScale(0.6f);
                    equippedLabel.setColor(Color.YELLOW);
                    equippedLabel.setPosition(SLOT_SIZE - 15, 2);
                    slot.addActor(equippedLabel);
                }
            } else {
                // Empty slot
                slot.setBackground(emptySlotBg);
                Label emptyLabel = new Label("-", skin);
                emptyLabel.setColor(Color.DARK_GRAY);
                slot.add(emptyLabel).center();
            }

            weaponsTable.add(slot).size(SLOT_SIZE).pad(SLOT_PADDING);
        }
    }

    private void refreshPotionsDisplay() {
        potionsTable.clearChildren();

        List<Potion> potions = inventorySystem.getPotions();

        int slotsPerRow = 4;
        int totalSlots = inventorySystem.getMaxPotionSlots();

        for (int i = 0; i < totalSlots; i++) {
            Table slot = createSlot();

            if (i < potions.size()) {
                Potion potion = potions.get(i);
                final int index = i;

                // Slot background
                boolean isSelected = !isWeaponSelected && selectedItemIndex == i;
                slot.setBackground(isSelected ? selectedSlotBg : normalSlotBg);

                // Potion icon (use colored circle as fallback)
                Label iconLabel = new Label("●", skin);
                iconLabel.setFontScale(2f);
                iconLabel.setColor(new Color(
                    ((potion.getColor() >> 16) & 0xFF) / 255f,
                    ((potion.getColor() >> 8) & 0xFF) / 255f,
                    (potion.getColor() & 0xFF) / 255f,
                    1f
                ));
                slot.add(iconLabel).center();

                // Stack count
                if (potion.getStackCount() > 1) {
                    Label countLabel = new Label("x" + potion.getStackCount(), skin);
                    countLabel.setFontScale(0.6f);
                    countLabel.setColor(Color.WHITE);
                    countLabel.setPosition(SLOT_SIZE - 25, 2);
                    slot.addActor(countLabel);
                }

                // Click listener
                slot.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectPotion(index);
                    }
                });
            } else {
                // Empty slot
                slot.setBackground(emptySlotBg);
                Label emptyLabel = new Label("-", skin);
                emptyLabel.setColor(Color.DARK_GRAY);
                slot.add(emptyLabel).center();
            }

            potionsTable.add(slot).size(SLOT_SIZE).pad(SLOT_PADDING);

            // New row after every slotsPerRow items
            if ((i + 1) % slotsPerRow == 0) {
                potionsTable.row();
            }
        }
    }

    private Table createSlot() {
        Table slot = new Table();
        slot.setTouchable(Touchable.enabled);
        return slot;
    }

    private void selectWeapon(int index) {
        isWeaponSelected = true;
        selectedItemIndex = index;
        selectedItem = inventorySystem.getWeapons().get(index);
        refreshUI();
    }

    private void selectPotion(int index) {
        isWeaponSelected = false;
        selectedItemIndex = index;
        selectedItem = inventorySystem.getPotions().get(index);
        refreshUI();
    }

    private void updateDetailSection() {
        if (selectedItem == null) {
            detailLabel.setText("Select an item to view details\n选择物品查看详情");
            return;
        }

        StringBuilder detail = new StringBuilder();

        if (selectedItem instanceof Weapon) {
            Weapon weapon = (Weapon) selectedItem;
            detail.append("[Weapon / 武器]\n");
            detail.append("Name / 名称: ").append(weapon.getName()).append("\n");
            detail.append("Damage / 伤害: ").append(weapon.getDamage()).append("\n");
            detail.append("Range / 范围: ").append(String.format("%.1f", weapon.getRange())).append("\n");
            detail.append("Type / 类型: ").append(weapon.isRanged() ? "Ranged / 远程" : "Melee / 近战");
        } else if (selectedItem instanceof Potion) {
            Potion potion = (Potion) selectedItem;
            detail.append("[Potion / 药水]\n");
            detail.append("Name / 名称: ").append(potion.getName()).append("\n");
            detail.append("Effect / 效果: ").append(potion.getDescription()).append("\n");
            detail.append("Quantity / 数量: ").append(potion.getStackCount());
        }

        detailLabel.setText(detail.toString());
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedItem != null;
        
        // Use button: only for potions
        useButton.setDisabled(!hasSelection || isWeaponSelected);
        useButton.setText(isWeaponSelected ? "Equip / 装备" : "Use / 使用");

        // If weapon selected, use button becomes equip button
        if (hasSelection && isWeaponSelected) {
            useButton.setDisabled(selectedItemIndex == inventorySystem.getCurrentWeaponIndex());
        }

        // Drop button: available for all items (except last weapon)
        boolean canDrop = hasSelection;
        if (isWeaponSelected && inventorySystem.getWeapons().size() <= 1) {
            canDrop = false;
        }
        dropButton.setDisabled(!canDrop);
    }

    private void onUseClicked() {
        if (selectedItem == null) return;

        if (isWeaponSelected) {
            // Equip weapon
            inventorySystem.switchWeapon(selectedItemIndex);
        } else {
            // Use potion
            inventorySystem.usePotion(selectedItemIndex);
            // Reset selection if potion was consumed
            if (selectedItemIndex >= inventorySystem.getPotions().size()) {
                selectedItem = null;
                selectedItemIndex = -1;
            }
        }
        refreshUI();
    }

    private void onDropClicked() {
        if (selectedItem == null) return;

        if (isWeaponSelected) {
            inventorySystem.removeWeapon(selectedItemIndex);
        } else {
            inventorySystem.removePotion(selectedItemIndex);
        }
        
        // Reset selection
        selectedItem = null;
        selectedItemIndex = -1;
        refreshUI();
    }

    // ==================== Visibility Control ====================

    public void show() {
        visible = true;
        inventoryWindow.setVisible(true);
        selectedItem = null;
        selectedItemIndex = -1;
        refreshUI();
    }

    public void hide() {
        visible = false;
        inventoryWindow.setVisible(false);
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (visible) {
            show();
        } else {
            hide();
        }
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * 处理按键输入（ESC 关闭）
     * @return true 如果按键被处理
     */
    public boolean handleKeyDown(int keycode) {
        if (!visible) return false;

        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
            hide();
            return true;
        }
        return false;
    }

    public void dispose() {
        // Dispose any textures created
        if (selectedSlotBg != null && selectedSlotBg.getRegion() != null) {
            selectedSlotBg.getRegion().getTexture().dispose();
        }
        if (normalSlotBg != null && normalSlotBg.getRegion() != null) {
            normalSlotBg.getRegion().getTexture().dispose();
        }
        if (emptySlotBg != null && emptySlotBg.getRegion() != null) {
            emptySlotBg.getRegion().getTexture().dispose();
        }
    }
}
