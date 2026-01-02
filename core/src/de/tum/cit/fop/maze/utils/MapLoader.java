package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import de.tum.cit.fop.maze.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 工具类：负责读取 .properties 文件并将其转换为 GameMap 对象。
 */
public class MapLoader {

    /**
     * 加载指定路径的地图文件
     * @param internalPath assets 文件夹下的相对路径，例如 "maps/level1.properties"
     * @return 解析好的 GameMap 对象
     */
    public static GameMap loadMap(String internalPath) {
        GameMap map = new GameMap();
        Properties props = new Properties();

        // 使用 LibGDX 的文件处理，确保在不同操作系统下都能找到文件
        FileHandle file = Gdx.files.internal(internalPath);

        if (!file.exists()) {
            Gdx.app.error("MapLoader", "Map file not found: " + internalPath);
            return map; // 返回空地图防止崩溃
        }

        try (InputStream input = file.read()) {
            props.load(input);

            // 遍历 Properties 中的每一个 Key
            for (String key : props.stringPropertyNames()) {

                // 过滤掉非坐标格式的行 (必须包含逗号)
                if (!key.contains(",")) {
                    continue;
                }

                try {
                    // 1. 解析坐标 Key "x,y"
                    String[] coords = key.split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());

                    // 2. 解析类型 Value (ID)
                    String value = props.getProperty(key).trim();
                    int typeId = Integer.parseInt(value);

                    // 3. 根据文档 ID 表创建对应的对象
                    // 0=Wall, 1=Entry, 2=Exit, 3=Trap, 4=Enemy, 5=Key
                    switch (typeId) {
                        case 0:
                            map.addGameObject(new Wall(x, y));
                            break;
                        case 1:
                            // ID 1 是出生点，不生成实体，只设置坐标
                            map.setPlayerStart(x, y);
                            break;
                        case 2:
                            map.addGameObject(new Exit(x, y));
                            break;
                        case 3:
                            map.addGameObject(new Trap(x, y));
                            break;
                        case 4:
                            map.addGameObject(new Enemy(x, y));
                            break;
                        case 5:
                            map.addGameObject(new Key(x, y));
                            break;
                        default:
                            Gdx.app.log("MapLoader", "Unknown object type ID: " + typeId + " at " + x + "," + y);
                            break;
                    }

                } catch (NumberFormatException e) {
                    Gdx.app.error("MapLoader", "Invalid format in map file at line: " + key);
                }
            }

        } catch (IOException e) {
            Gdx.app.error("MapLoader", "Failed to load map file", e);
        }

        Gdx.app.log("MapLoader", "Map loaded successfully! Size: " + map.getWidth() + "x" + map.getHeight());
        return map;
    }
}