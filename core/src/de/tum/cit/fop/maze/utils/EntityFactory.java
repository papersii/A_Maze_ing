package de.tum.cit.fop.maze.utils;

import de.tum.cit.fop.maze.config.GameConfig;
import de.tum.cit.fop.maze.model.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 实体工厂类 (Factory Pattern)。
 * 负责根据 ID 创建对应的游戏对象实例。
 * 实现了注册机制，方便扩展新类型的物体而无需修改核心加载逻辑。
 */
public class EntityFactory {

    // 使用函数式接口 BiFunction<Float, Float, GameObject>
    // 接收 x, y 坐标，返回 GameObject
    private static final Map<Integer, BiFunction<Float, Float, GameObject>> registry = new HashMap<>();

    static {
        // === 注册默认实体 ===
        // ID=0 now creates 2x2 walls (no more 1x1 walls)
        register(GameConfig.OBJECT_ID_WALL, (x, y) -> new Wall(x, y, 2, 2));
        register(GameConfig.OBJECT_ID_EXIT, Exit::new);
        register(GameConfig.OBJECT_ID_TRAP, Trap::new);
        register(GameConfig.OBJECT_ID_ENEMY, Enemy::new);
        register(GameConfig.OBJECT_ID_KEY, Key::new);
        register(GameConfig.OBJECT_ID_MOBILE_TRAP, MobileTrap::new);

        // Register Multi-tile Walls (7 sizes: 2x2, 3x2, 2x3, 2x4, 4x2, 3x3, 4x4)
        register(GameConfig.OBJECT_ID_WALL_2X2, (x, y) -> new Wall(x, y, 2, 2));
        register(GameConfig.OBJECT_ID_WALL_3X2, (x, y) -> new Wall(x, y, 3, 2));
        register(GameConfig.OBJECT_ID_WALL_2X3, (x, y) -> new Wall(x, y, 2, 3));
        register(GameConfig.OBJECT_ID_WALL_2X4, (x, y) -> new Wall(x, y, 2, 4));
        register(GameConfig.OBJECT_ID_WALL_4X2, (x, y) -> new Wall(x, y, 4, 2));
        register(GameConfig.OBJECT_ID_WALL_3X3, (x, y) -> new Wall(x, y, 3, 3));
        register(GameConfig.OBJECT_ID_WALL_4X4, (x, y) -> new Wall(x, y, 4, 4));

        // 注册宝箱 (Treasure Chest)
        register(GameConfig.OBJECT_ID_CHEST, (x, y) -> {
            java.util.Random random = new java.util.Random();
            TreasureChest chest = TreasureChest.createRandom(x, y, random, GameConfig.CHEST_PUZZLE_PROBABILITY);
            // 设置奖励
            chest.setReward(ChestRewardGenerator.generateLevelModeReward(random));
            // 如果是谜题宝箱，设置谜题
            if (chest.getType() == TreasureChest.ChestType.PUZZLE) {
                chest.setPuzzle(PuzzleGenerator.generateRandom(random));
            }
            return chest;
        });

        // 注意：ID 1 (Entry) 通常不生成实体对象，而是设置玩家起始位置，
        // 所以这里不注册它，或者注册一个空操作（视 MapLoader 逻辑而定）。
        // MapLoader 目前特殊处理了 ID 1。
    }

    /**
     * 注册一个新的实体类型。
     * 
     * @param id      地图文件中的 ID
     * @param creator 创建函数，例如 Enemy::new
     */
    public static void register(int id, BiFunction<Float, Float, GameObject> creator) {
        registry.put(id, creator);
    }

    /**
     * 根据 ID 和坐标创建实体。
     * 
     * @param id 对象 ID
     * @param x  X 坐标
     * @param y  Y 坐标
     * @return 新创建的 GameObject，如果 ID 未注册则返回 null
     */
    public static GameObject createEntity(int id, float x, float y) {
        BiFunction<Float, Float, GameObject> creator = registry.get(id);
        if (creator != null) {
            return creator.apply(x, y);
        } else {
            // Log warning handled by caller or here
            return null;
        }
    }

    /**
     * 检查 ID 是否已注册
     */
    public static boolean isRegistered(int id) {
        return registry.containsKey(id);
    }
}
