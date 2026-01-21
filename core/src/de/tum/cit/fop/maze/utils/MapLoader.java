package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import de.tum.cit.fop.maze.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 工具类：负责读取 .properties 文件并将其转换为 GameMap 对象。
 * 
 * 支持的元数据配置键：
 * - damageType: PHYSICAL 或 MAGICAL（关卡敌人伤害类型）
 * - enemyShieldEnabled: true/false（敌人是否有护盾）
 * - levelDifficulty: 1-5（关卡难度）
 * - suggestedArmor: PHYSICAL 或 MAGICAL（推荐护甲类型）
 */
public class MapLoader {

    // 元数据配置键
    public static final String KEY_DAMAGE_TYPE = "damageType";
    public static final String KEY_ENEMY_SHIELD = "enemyShieldEnabled";
    public static final String KEY_DIFFICULTY = "levelDifficulty";
    public static final String KEY_SUGGESTED_ARMOR = "suggestedArmor";
    public static final String KEY_THEME = "theme";

    /**
     * 关卡配置信息类
     */
    public static class LevelConfig {
        public DamageType damageType = DamageType.PHYSICAL;
        public boolean enemyShieldEnabled = false;
        public int difficulty = 1;
        public DamageType suggestedArmor = DamageType.PHYSICAL;
        public String theme = "Grassland";
    }

    /**
     * 加载结果：包含地图和配置
     */
    public static class LoadResult {
        public GameMap map;
        public LevelConfig config;

        public LoadResult(GameMap map, LevelConfig config) {
            this.map = map;
            this.config = config;
        }
    }

    /**
     * 加载指定路径的地图文件（返回完整结果）
     */
    public static LoadResult loadMapWithConfig(String internalPath) {
        GameLogger.info("MapLoader", "Attempting to load map: " + internalPath);
        GameMap map = new GameMap();
        LevelConfig config = new LevelConfig();
        Properties props = new Properties();

        FileHandle file = Gdx.files.internal(internalPath);
        if (!file.exists()) {
            file = Gdx.files.local(internalPath);
        }

        if (!file.exists()) {
            GameLogger.error("MapLoader", "Map file not found in Internal or Local: " + internalPath);
            return new LoadResult(createFallbackMap(), config);
        }

        try (InputStream input = file.read()) {
            props.load(input);

            // 1. 解析元数据配置
            config = parseMetadata(props);
            // Set theme on map
            map.setTheme(config.theme);

            // 2. 解析地图尺寸并初始化
            int playableWidth = Integer.parseInt(props.getProperty("playableWidth", "50"));
            int playableHeight = Integer.parseInt(props.getProperty("playableHeight", "50"));
            map.initializeSize(playableWidth, playableHeight);

            // 3. 遍历 Properties 中的每一个 Key
            for (String key : props.stringPropertyNames()) {

                // 过滤掉非坐标格式的行 (必须包含逗号)
                if (!key.contains(",")) {
                    continue;
                }

                try {
                    String[] coords = key.split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());

                    String value = props.getProperty(key).trim();
                    int typeId = Integer.parseInt(value);

                    if (typeId == de.tum.cit.fop.maze.config.GameConfig.OBJECT_ID_ENTRY) {
                        map.setPlayerStart(x, y);
                    } else {
                        GameObject obj = EntityFactory.createEntity(typeId, (float) x, (float) y);
                        if (obj != null) {
                            // 如果是敌人
                            if (obj instanceof Enemy) {
                                Enemy enemy = (Enemy) obj;

                                // 统一使用第一关的怪物素材 (BOAR)
                                enemy.setType(Enemy.EnemyType.BOAR);

                                // 2. 如果护盾启用，设置护盾和攻击属性
                                if (config.enemyShieldEnabled) {
                                    enemy.setAttackDamageType(config.damageType);
                                    enemy.setShield(config.damageType, 3); // 默认3点护盾
                                }
                            }
                            map.addGameObject(obj);
                        } else {
                            GameLogger.info("MapLoader",
                                    "Unknown or unhandled object type ID: " + typeId + " at " + x + "," + y);
                        }
                    }

                } catch (NumberFormatException e) {
                    GameLogger.error("MapLoader", "Invalid format in map file at line: " + key);
                }
            }

        } catch (IOException e) {
            GameLogger.error("MapLoader", "Failed to load map file", e);
            return new LoadResult(createFallbackMap(), config);
        }

        if (map.getWidth() == 0 || map.getHeight() == 0) {
            GameLogger.error("MapLoader", "Map is empty! Using fallback.");
            return new LoadResult(createFallbackMap(), config);
        }

        GameLogger.info("MapLoader", "Map loaded successfully! Size: " + map.getWidth() + "x" + map.getHeight()
                + " | DamageType: " + config.damageType + " | Shields: " + config.enemyShieldEnabled);
        return new LoadResult(map, config);
    }

    /**
     * 加载指定路径的地图文件（向后兼容，只返回 GameMap）
     */
    public static GameMap loadMap(String internalPath) {
        return loadMapWithConfig(internalPath).map;
    }

    /**
     * 解析元数据配置
     */
    private static LevelConfig parseMetadata(Properties props) {
        LevelConfig config = new LevelConfig();

        // 解析伤害类型
        String dmgType = props.getProperty(KEY_DAMAGE_TYPE, "PHYSICAL").toUpperCase().trim();
        if (dmgType.equals("MAGICAL") || dmgType.equals("MAGIC")) {
            config.damageType = DamageType.MAGICAL;
        } else {
            config.damageType = DamageType.PHYSICAL;
        }

        // 解析敌人护盾
        String shieldEnabled = props.getProperty(KEY_ENEMY_SHIELD, "false").toLowerCase().trim();
        config.enemyShieldEnabled = shieldEnabled.equals("true") || shieldEnabled.equals("1");

        // 解析难度
        try {
            config.difficulty = Integer.parseInt(props.getProperty(KEY_DIFFICULTY, "1").trim());
            config.difficulty = Math.max(1, Math.min(5, config.difficulty));
        } catch (NumberFormatException e) {
            config.difficulty = 1;
        }

        // 解析推荐护甲
        String armorType = props.getProperty(KEY_SUGGESTED_ARMOR, "PHYSICAL").toUpperCase().trim();
        if (armorType.equals("MAGICAL") || armorType.equals("MAGIC")) {
            config.suggestedArmor = DamageType.MAGICAL;
        } else {
            config.suggestedArmor = DamageType.PHYSICAL;
        }

        // Parse Theme
        config.theme = props.getProperty(KEY_THEME, "Grassland").trim();

        return config;
    }

    private static GameMap createFallbackMap() {
        GameLogger.info("MapLoader", "Creating Fallback Map...");
        GameMap map = new GameMap();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                if (x == 0 || x == 4 || y == 0 || y == 4) {
                    map.addGameObject(
                            EntityFactory.createEntity(de.tum.cit.fop.maze.config.GameConfig.OBJECT_ID_WALL, x, y));
                }
            }
        }
        map.setPlayerStart(2, 2);
        map.addGameObject(EntityFactory.createEntity(de.tum.cit.fop.maze.config.GameConfig.OBJECT_ID_EXIT, 3, 3));
        return map;
    }
}