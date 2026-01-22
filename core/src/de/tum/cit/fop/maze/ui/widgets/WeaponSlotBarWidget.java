package de.tum.cit.fop.maze.ui.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import de.tum.cit.fop.maze.model.Player;
import de.tum.cit.fop.maze.model.weapons.Weapon;

import java.util.List;

/**
 * 可复用的武器栏组件 (Reusable Weapon Slot Bar Widget)
 * 
 * 从 GameHUD 和 EndlessHUD 中提取的公共逻辑。
 * 显示玩家背包中的武器并高亮当前选中武器。
 */
public class WeaponSlotBarWidget extends Table {

    private final Player player;
    private final Skin skin;

    // Cached state
    private int lastWeaponIndex = -1;
    private int lastInventorySize = -1;
    private Drawable selectedSlotBg;
    private Drawable normalSlotBg;

    // Configuration
    private float slotWidth = 70f;
    private float slotHeight = 40f;
    private float slotPadding = 3f;

    public WeaponSlotBarWidget(Player player, Skin skin) {
        this.player = player;
        this.skin = skin;
        pad(5);
    }

    /**
     * 设置槽位尺寸
     */
    public WeaponSlotBarWidget setSlotSize(float width, float height, float padding) {
        this.slotWidth = width;
        this.slotHeight = height;
        this.slotPadding = padding;
        this.lastInventorySize = -1; // Force rebuild
        return this;
    }

    /**
     * 更新武器栏显示
     */
    public void update() {
        int currentWeaponIdx = player.getCurrentWeaponIndex();
        List<Weapon> inventory = player.getInventory();

        boolean needRebuild = lastInventorySize != inventory.size() || getChildren().isEmpty();

        // Cache drawables on first use
        if (selectedSlotBg == null) {
            selectedSlotBg = skin.newDrawable("white", new Color(0.3f, 0.5f, 0.8f, 0.8f));
            normalSlotBg = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.6f));
        }

        if (needRebuild) {
            clearChildren();

            for (int i = 0; i < inventory.size(); i++) {
                Weapon w = inventory.get(i);
                Table slot = new Table();
                slot.setBackground(i == currentWeaponIdx ? selectedSlotBg : normalSlotBg);

                String shortName = w.getName().substring(0, Math.min(3, w.getName().length()));
                Label slotLabel = new Label((i + 1) + ": " + shortName, skin);
                slotLabel.setFontScale(0.7f);
                slot.add(slotLabel).pad(5);

                add(slot).width(slotWidth).height(slotHeight).pad(slotPadding);
            }
            lastInventorySize = inventory.size();
            lastWeaponIndex = currentWeaponIdx;
        } else if (currentWeaponIdx != lastWeaponIndex) {
            // Just update backgrounds without rebuilding
            for (int i = 0; i < getChildren().size; i++) {
                Table slot = (Table) getChildren().get(i);
                slot.setBackground(i == currentWeaponIdx ? selectedSlotBg : normalSlotBg);
            }
            lastWeaponIndex = currentWeaponIdx;
        }
    }
}
