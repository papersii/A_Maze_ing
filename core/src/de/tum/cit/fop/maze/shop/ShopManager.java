package de.tum.cit.fop.maze.shop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import de.tum.cit.fop.maze.utils.GameLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * 商店管理器 (Shop Manager)
 * 
 * 管理商店物品、购买记录和玩家金币的持久化存储。
 */
public class ShopManager {

    private static final String PREFS_NAME = "maze_shop_v1";
    private static final String KEY_PURCHASED_ITEMS = "purchased_items";
    private static final String KEY_PLAYER_COINS = "player_coins";

    private static List<ShopItem> allItems;

    static {
        initializeShopItems();
    }

    /**
     * 初始化商店物品列表
     */
    private static void initializeShopItems() {
        allItems = new ArrayList<>();

        // === 武器 ===
        allItems.add(new ShopItem(
                "weapon_sword",
                "Steel Sword",
                "A reliable melee weapon. Balanced damage and speed.",
                50,
                "sword",
                ShopItem.ItemCategory.WEAPON));

        allItems.add(new ShopItem(
                "weapon_bow",
                "Ice Bow",
                "Freezes enemies on hit. Medium range.",
                100,
                "bow",
                ShopItem.ItemCategory.WEAPON));

        allItems.add(new ShopItem(
                "weapon_staff",
                "Fire Staff",
                "Burns enemies over time. High damage.",
                120,
                "magic_staff",
                ShopItem.ItemCategory.WEAPON));

        allItems.add(new ShopItem(
                "weapon_crossbow",
                "Crossbow",
                "Powerful ranged physical weapon. Slow reload.",
                150,
                "crossbow",
                ShopItem.ItemCategory.WEAPON));

        allItems.add(new ShopItem(
                "weapon_wand",
                "Magic Wand",
                "Fast magical attacks with burn effect.",
                130,
                "wand",
                ShopItem.ItemCategory.WEAPON));

        // === 护甲 ===
        allItems.add(new ShopItem(
                "armor_physical",
                "Iron Shield",
                "Blocks physical attacks. 5 shield points.",
                80,
                "physical_armor",
                ShopItem.ItemCategory.ARMOR));

        allItems.add(new ShopItem(
                "armor_magical",
                "Arcane Robe",
                "Absorbs magical attacks. 4 shield points.",
                80,
                "magical_armor",
                ShopItem.ItemCategory.ARMOR));

        allItems.add(new ShopItem(
                "armor_physical_heavy",
                "Knight's Plate",
                "Heavy physical armor. 8 shield points.",
                150,
                "physical_armor",
                ShopItem.ItemCategory.ARMOR));

        allItems.add(new ShopItem(
                "armor_magical_heavy",
                "Wizard's Cloak",
                "Powerful magical protection. 7 shield points.",
                150,
                "magical_armor",
                ShopItem.ItemCategory.ARMOR));

        GameLogger.info("ShopManager", "Initialized " + allItems.size() + " shop items.");
    }

    /**
     * 获取所有商店物品
     */
    public static List<ShopItem> getAvailableItems() {
        loadPurchaseStatus();
        return new ArrayList<>(allItems);
    }

    /**
     * 获取指定类别的物品
     */
    public static List<ShopItem> getItemsByCategory(ShopItem.ItemCategory category) {
        List<ShopItem> filtered = new ArrayList<>();
        for (ShopItem item : allItems) {
            if (item.getCategory() == category) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * 购买物品
     * 
     * @param itemId 物品 ID
     * @return true 如果购买成功
     */
    public static boolean purchaseItem(String itemId) {
        GameLogger.debug("ShopManager", "Attempting to purchase item: " + itemId);
        int playerCoins = getPlayerCoins();

        for (ShopItem item : allItems) {
            if (item.getId().equals(itemId)) {
                if (item.isPurchased()) {
                    GameLogger.info("ShopManager", "Purchase failed: Item already purchased - " + itemId);
                    return false; // 已购买
                }
                if (playerCoins < item.getPrice()) {
                    GameLogger.info("ShopManager",
                            "Purchase failed: Insufficient coins. Cost: " + item.getPrice() + ", Has: " + playerCoins);
                    return false; // 金币不足
                }

                // 扣款
                setPlayerCoins(playerCoins - item.getPrice());

                // 标记为已购买
                item.setPurchased(true);
                savePurchaseStatus();

                GameLogger.info("ShopManager", "Purchase successful: " + itemId + ". New balance: " + getPlayerCoins());
                return true;
            }
        }
        GameLogger.error("ShopManager", "Purchase failed: Item ID not found - " + itemId);
        return false; // 物品不存在
    }

    /**
     * 获取已购买的物品 ID 列表
     */
    public static List<String> getPurchasedItemIds() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String data = prefs.getString(KEY_PURCHASED_ITEMS, "");

        List<String> ids = new ArrayList<>();
        if (!data.isEmpty()) {
            for (String id : data.split(";")) {
                if (!id.trim().isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * 检查物品是否已购买
     */
    public static boolean isItemPurchased(String itemId) {
        return getPurchasedItemIds().contains(itemId);
    }

    /**
     * 获取玩家金币
     */
    public static int getPlayerCoins() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getInteger(KEY_PLAYER_COINS, 0);
    }

    /**
     * 设置玩家金币
     */
    public static void setPlayerCoins(int coins) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        GameLogger.debug("ShopManager",
                "Updating player coins: " + prefs.getInteger(KEY_PLAYER_COINS, 0) + " -> " + coins);
        prefs.putInteger(KEY_PLAYER_COINS, coins);
        prefs.flush();
    }

    /**
     * 添加金币
     */
    public static void addPlayerCoins(int amount) {
        setPlayerCoins(getPlayerCoins() + amount);
    }

    /**
     * 将游戏中收集的金币同步到持久化存储
     * 在关卡结束时调用此方法
     * 
     * @param coinsEarned 本关卡收集的金币数
     */
    public static void syncCoinsFromGame(int coinsEarned) {
        if (coinsEarned > 0) {
            addPlayerCoins(coinsEarned);
            GameLogger.info("ShopManager",
                    "Synced " + coinsEarned + " coins from game. New total: " + getPlayerCoins());
        }
    }

    /**
     * 加载购买状态到内存
     */
    private static void loadPurchaseStatus() {
        List<String> purchasedIds = getPurchasedItemIds();
        for (ShopItem item : allItems) {
            item.setPurchased(purchasedIds.contains(item.getId()));
        }
    }

    /**
     * 保存购买状态到持久化存储
     */
    private static void savePurchaseStatus() {
        StringBuilder sb = new StringBuilder();
        for (ShopItem item : allItems) {
            if (item.isPurchased()) {
                sb.append(item.getId()).append(";");
            }
        }

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(KEY_PURCHASED_ITEMS, sb.toString());
        prefs.flush();
    }

    /**
     * 重置所有购买记录（用于调试）
     */
    public static void resetAllPurchases() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(KEY_PURCHASED_ITEMS, "");
        prefs.flush();

        for (ShopItem item : allItems) {
            item.setPurchased(false);
        }
    }
}
