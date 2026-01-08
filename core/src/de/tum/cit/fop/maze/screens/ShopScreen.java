package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.shop.ShopItem;
import de.tum.cit.fop.maze.shop.ShopManager;
import de.tum.cit.fop.maze.utils.AudioManager;
import de.tum.cit.fop.maze.utils.DialogFactory;
import de.tum.cit.fop.maze.utils.GameLogger;
import de.tum.cit.fop.maze.utils.UIUtils;

import java.util.List;

/**
 * 商店界面 (Shop Screen)
 * 
 * 显示可购买的武器和护甲，玩家可使用金币购买。
 */
public class ShopScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Skin skin;

    private Label coinLabel;
    private Table itemsTable;
    private ScrollPane scrollPane;

    public ShopScreen(MazeRunnerGame game) {
        this.game = game;
        this.skin = game.getSkin();
        // Use FitViewport to ensure consistent display across all screen sizes
        this.stage = new Stage(new com.badlogic.gdx.utils.viewport.FitViewport(1920, 1080), game.getSpriteBatch());

        setupUI();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Title
        Label title = new Label("SHOP", skin, "title");
        title.setAlignment(Align.center);
        root.add(title).padTop(30).padBottom(20).row();

        // Coin display
        coinLabel = new Label("Coins: " + ShopManager.getPlayerCoins(), skin);
        coinLabel.setColor(Color.GOLD);
        root.add(coinLabel).padBottom(20).row();

        // Category tabs
        HorizontalGroup tabs = new HorizontalGroup();
        tabs.space(20);

        TextButton weaponsTab = new TextButton("Weapons", skin);
        TextButton armorTab = new TextButton("Armor", skin);
        TextButton allTab = new TextButton("All", skin);

        weaponsTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCategory(ShopItem.ItemCategory.WEAPON);
                AudioManager.getInstance().playSound("select");
            }
        });

        armorTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCategory(ShopItem.ItemCategory.ARMOR);
                AudioManager.getInstance().playSound("select");
            }
        });

        allTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showAllItems();
                AudioManager.getInstance().playSound("select");
            }
        });

        tabs.addActor(allTab);
        tabs.addActor(weaponsTab);
        tabs.addActor(armorTab);
        root.add(tabs).padBottom(20).row();

        // Scrollable items container
        itemsTable = new Table();
        scrollPane = new ScrollPane(itemsTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // Auto-focus scroll on hover so user doesn't need to click
        UIUtils.enableHoverScrollFocus(scrollPane, stage);

        // Use percentage width for responsive layout (60% of screen width)
        root.add(scrollPane).width(Value.percentWidth(0.6f, root)).height(Value.percentHeight(0.6f, root)).padBottom(20)
                .row();

        // Back button
        TextButton backBtn = new TextButton("Back to Menu", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameLogger.debug("ShopScreen", "Back to Menu clicked");
                AudioManager.getInstance().playSound("select");
                game.setScreen(new MenuScreen(game));
            }
        });
        root.add(backBtn).padBottom(30);

        // Show all items by default
        showAllItems();
    }

    private void showAllItems() {
        populateItems(ShopManager.getAvailableItems());
    }

    private void showCategory(ShopItem.ItemCategory category) {
        populateItems(ShopManager.getItemsByCategory(category));
    }

    private void populateItems(List<ShopItem> items) {
        itemsTable.clear();

        for (ShopItem item : items) {
            Table itemRow = createItemRow(item);
            itemsTable.add(itemRow).growX().padBottom(10).row();
        }
    }

    private Table createItemRow(ShopItem item) {
        Table row = new Table();
        row.pad(10);
        row.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.3f, 0.8f)));

        // Item icon placeholder (colored square based on category)
        Table iconContainer = new Table();
        Color iconColor = item.getCategory() == ShopItem.ItemCategory.WEAPON ? new Color(0.8f, 0.3f, 0.3f, 1f)
                : new Color(0.3f, 0.5f, 0.8f, 1f);
        iconContainer.setBackground(skin.newDrawable("white", iconColor));
        Label iconLabel = new Label(item.getCategory() == ShopItem.ItemCategory.WEAPON ? "⚔" : "🛡", skin);
        iconContainer.add(iconLabel).size(40, 40).center();
        row.add(iconContainer).size(50, 50).padRight(15);

        // Item info
        Table infoTable = new Table();
        infoTable.left();

        Label nameLabel = new Label(item.getName(), skin);
        nameLabel.setColor(item.isPurchased() ? Color.GREEN : Color.WHITE);
        infoTable.add(nameLabel).left().row();

        Label descLabel = new Label(item.getDescription(), skin);
        descLabel.setFontScale(0.8f);
        descLabel.setColor(Color.LIGHT_GRAY);
        descLabel.setWrap(true); // Allow text wrapping
        infoTable.add(descLabel).left().growX(); // growX is important for wrapping

        row.add(infoTable).expandX().fillX().left();

        // Price and button
        Table priceTable = new Table();

        if (item.isPurchased()) {
            Label ownedLabel = new Label("OWNED", skin);
            ownedLabel.setColor(Color.GREEN);
            priceTable.add(ownedLabel);
        } else {
            Label priceLabel = new Label(item.getPrice() + " coins", skin);
            priceLabel.setColor(Color.GOLD);
            priceTable.add(priceLabel).padBottom(5).row();

            TextButton buyBtn = new TextButton("Buy", skin);
            boolean canAfford = ShopManager.getPlayerCoins() >= item.getPrice();
            buyBtn.setColor(canAfford ? Color.WHITE : Color.DARK_GRAY);

            // === 修复：始终添加监听器，余额不足时给出反馈 ===
            final ShopItem itemToBuy = item;
            final int itemPrice = item.getPrice();
            buyBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    int currentCoins = ShopManager.getPlayerCoins();
                    if (currentCoins >= itemPrice) {
                        if (ShopManager.purchaseItem(itemToBuy.getId())) {
                            AudioManager.getInstance().playSound("collect");
                            refreshUI();
                        }
                    } else {
                        // 余额不足反馈
                        AudioManager.getInstance().playSound("select");
                        showInsufficientFundsDialog(itemPrice, currentCoins);
                    }
                }
            });
            priceTable.add(buyBtn);
        }

        row.add(priceTable).right().padLeft(15);

        return row;
    }

    private void refreshUI() {
        coinLabel.setText("Coins: " + ShopManager.getPlayerCoins());
        showAllItems();
    }

    /**
     * 显示余额不足提示对话框
     */
    private void showInsufficientFundsDialog(int itemPrice, int currentCoins) {
        DialogFactory.showInsufficientFundsDialog(stage, skin, itemPrice, currentCoins);
    }

    @Override
    public void show() {
        GameLogger.info("ShopScreen", "Showing Shop Screen");
        Gdx.input.setInputProcessor(stage);
        // Initial scroll focus
        if (scrollPane != null) {
            stage.setScrollFocus(scrollPane);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
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
