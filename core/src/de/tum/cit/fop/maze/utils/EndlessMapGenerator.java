package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.config.EndlessModeConfig;
import de.tum.cit.fop.maze.config.GameConfig;
import de.tum.cit.fop.maze.model.MapChunk;
import de.tum.cit.fop.maze.model.WallEntity;

import java.util.Random;

/**
 * 无尽模式地图生成器 (Endless Map Generator)
 * 
 * 生成900×900格子的多主题大地图。
 * 使用分块生成策略，每次只生成一个64×64的区块。
 * 
 * 主题布局:
 * - 中心圆形区域 (半径200): Space
 * - 西北象限: Grassland
 * - 东北象限: Jungle
 * - 西南象限: Desert
 * - 东南象限: Ice
 * 
 * 遵循单一职责原则：仅处理地图生成逻辑。
 */
public class EndlessMapGenerator {

    /** 随机数生成器 */
    private final Random random;

    /** 种子（用于可重复生成） */
    private long seed;

    /** 墙体尺寸选项 */
    private static final int[][] WALL_SIZES = {
            { 2, 2 }, { 3, 2 }, { 2, 3 }, { 4, 2 }, { 2, 4 }, { 3, 3 }, { 4, 4 }
    };

    public EndlessMapGenerator() {
        this.seed = System.currentTimeMillis();
        this.random = new Random(seed);
    }

    public EndlessMapGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * 生成指定区块
     * 
     * @param chunkX 区块X坐标
     * @param chunkY 区块Y坐标
     * @return 生成的区块
     */
    public MapChunk generateChunk(int chunkX, int chunkY) {
        int chunkSize = EndlessModeConfig.CHUNK_SIZE;
        MapChunk chunk = new MapChunk(chunkX, chunkY, chunkSize);

        // 为此区块创建确定性随机数生成器
        long chunkSeed = seed ^ ((long) chunkX << 16) ^ chunkY;
        Random chunkRandom = new Random(chunkSeed);

        // 确定区块主题
        int worldCenterX = chunk.getWorldStartX() + chunkSize / 2;
        int worldCenterY = chunk.getWorldStartY() + chunkSize / 2;
        String theme = EndlessModeConfig.getThemeForPosition(worldCenterX, worldCenterY);
        chunk.setTheme(theme);

        // 生成边界墙（如果是边缘区块）
        generateBorderWalls(chunk, chunkRandom);

        // 生成内部墙体
        generateInternalWalls(chunk, chunkRandom);

        // 生成陷阱位置
        generateTraps(chunk, chunkRandom);

        // 生成敌人刷新点
        generateSpawnPoints(chunk, chunkRandom);

        chunk.markGenerated();
        return chunk;
    }

    /**
     * 生成边界墙（如果是地图边缘区块）
     */
    private void generateBorderWalls(MapChunk chunk, Random rand) {
        int chunkSize = chunk.getSize();
        int worldStartX = chunk.getWorldStartX();
        int worldStartY = chunk.getWorldStartY();
        int mapWidth = EndlessModeConfig.MAP_WIDTH;
        int mapHeight = EndlessModeConfig.MAP_HEIGHT;
        int borderWidth = 2;

        // 左边界
        if (worldStartX == 0) {
            for (int y = 0; y < chunkSize; y += 2) {
                int worldY = worldStartY + y;
                if (worldY >= 0 && worldY < mapHeight - 1) {
                    WallEntity wall = new WallEntity(0, worldY, 2, 2,
                            GameConfig.OBJECT_ID_WALL_2X2, true);
                    chunk.addWall(wall);
                }
            }
        }

        // 右边界
        if (worldStartX + chunkSize >= mapWidth) {
            int borderX = mapWidth - borderWidth;
            for (int y = 0; y < chunkSize; y += 2) {
                int worldY = worldStartY + y;
                if (worldY >= 0 && worldY < mapHeight - 1) {
                    WallEntity wall = new WallEntity(borderX, worldY, 2, 2,
                            GameConfig.OBJECT_ID_WALL_2X2, true);
                    chunk.addWall(wall);
                }
            }
        }

        // 下边界
        if (worldStartY == 0) {
            for (int x = 0; x < chunkSize; x += 2) {
                int worldX = worldStartX + x;
                if (worldX >= 0 && worldX < mapWidth - 1) {
                    WallEntity wall = new WallEntity(worldX, 0, 2, 2,
                            GameConfig.OBJECT_ID_WALL_2X2, true);
                    chunk.addWall(wall);
                }
            }
        }

        // 上边界
        if (worldStartY + chunkSize >= mapHeight) {
            int borderY = mapHeight - borderWidth;
            for (int x = 0; x < chunkSize; x += 2) {
                int worldX = worldStartX + x;
                if (worldX >= 0 && worldX < mapWidth - 1) {
                    WallEntity wall = new WallEntity(worldX, borderY, 2, 2,
                            GameConfig.OBJECT_ID_WALL_2X2, true);
                    chunk.addWall(wall);
                }
            }
        }
    }

    /**
     * 生成内部墙体
     */
    private void generateInternalWalls(MapChunk chunk, Random rand) {
        int chunkSize = chunk.getSize();
        int worldStartX = chunk.getWorldStartX();
        int worldStartY = chunk.getWorldStartY();

        // 墙体密度（提高到0.40f以匹配关卡地图）
        float wallDensity = 0.40f;
        int expectedWalls = (int) (chunkSize * chunkSize * wallDensity / 16);

        // 使用占用网格避免墙体重叠
        boolean[][] occupied = new boolean[chunkSize][chunkSize];

        int attempts = 0;
        int maxAttempts = expectedWalls * 5;
        int wallsPlaced = 0;

        while (wallsPlaced < expectedWalls && attempts < maxAttempts) {
            attempts++;

            // 随机选择墙体尺寸
            int[] size = WALL_SIZES[rand.nextInt(WALL_SIZES.length)];
            int width = size[0];
            int height = size[1];

            // 随机位置（区块内局部坐标）
            int localX = rand.nextInt(chunkSize - width);
            int localY = rand.nextInt(chunkSize - height);

            // 检查是否可以放置
            if (canPlaceWall(occupied, localX, localY, width, height, chunkSize)) {
                // 放置墙体
                int worldX = worldStartX + localX;
                int worldY = worldStartY + localY;

                int typeId = getTypeIdForSize(width, height);
                WallEntity wall = new WallEntity(worldX, worldY, width, height, typeId, false);
                chunk.addWall(wall);

                // 标记占用
                markOccupied(occupied, localX, localY, width, height);

                wallsPlaced++;
            }
        }
    }

    /**
     * 检查是否可以在指定位置放置墙体
     */
    private boolean canPlaceWall(boolean[][] occupied, int x, int y, int w, int h, int size) {
        // 减少安全边距以允许更密集的布局
        int margin = 0;
        int startX = Math.max(0, x - margin);
        int startY = Math.max(0, y - margin);
        int endX = Math.min(size, x + w + margin);
        int endY = Math.min(size, y + h + margin);

        for (int px = startX; px < endX; px++) {
            for (int py = startY; py < endY; py++) {
                if (occupied[px][py]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 标记区域为已占用
     */
    private void markOccupied(boolean[][] occupied, int x, int y, int w, int h) {
        for (int px = x; px < x + w; px++) {
            for (int py = y; py < y + h; py++) {
                if (px >= 0 && px < occupied.length && py >= 0 && py < occupied[0].length) {
                    occupied[px][py] = true;
                }
            }
        }
    }

    /**
     * 根据墙体尺寸获取类型ID
     */
    private int getTypeIdForSize(int w, int h) {
        if (w == 2 && h == 2)
            return GameConfig.OBJECT_ID_WALL_2X2;
        if (w == 3 && h == 2)
            return GameConfig.OBJECT_ID_WALL_3X2;
        if (w == 2 && h == 3)
            return GameConfig.OBJECT_ID_WALL_2X3;
        if (w == 4 && h == 2)
            return GameConfig.OBJECT_ID_WALL_4X2;
        if (w == 2 && h == 4)
            return GameConfig.OBJECT_ID_WALL_2X4;
        if (w == 3 && h == 3)
            return GameConfig.OBJECT_ID_WALL_3X3;
        if (w == 4 && h == 4)
            return GameConfig.OBJECT_ID_WALL_4X4;
        return GameConfig.OBJECT_ID_WALL_2X2;
    }

    /**
     * 生成陷阱位置
     */
    private void generateTraps(MapChunk chunk, Random rand) {
        int chunkSize = chunk.getSize();
        int worldStartX = chunk.getWorldStartX();
        int worldStartY = chunk.getWorldStartY();

        // 陷阱密度
        float trapDensity = 0.005f;
        int expectedTraps = (int) (chunkSize * chunkSize * trapDensity);

        for (int i = 0; i < expectedTraps; i++) {
            float localX = rand.nextFloat() * (chunkSize - 2) + 1;
            float localY = rand.nextFloat() * (chunkSize - 2) + 1;

            float worldX = worldStartX + localX;
            float worldY = worldStartY + localY;

            // 检查是否与墙体重叠（简化检查）
            boolean collision = false;
            for (WallEntity wall : chunk.getWalls()) {
                if (worldX >= wall.getOriginX() && worldX < wall.getOriginX() + wall.getGridWidth() &&
                        worldY >= wall.getOriginY() && worldY < wall.getOriginY() + wall.getGridHeight()) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                chunk.addTrap(worldX, worldY);
            }
        }
    }

    /**
     * 生成敌人刷新点
     */
    private void generateSpawnPoints(MapChunk chunk, Random rand) {
        int chunkSize = chunk.getSize();
        int worldStartX = chunk.getWorldStartX();
        int worldStartY = chunk.getWorldStartY();

        // 每个区块约4-8个刷新点
        int spawnCount = rand.nextInt(5) + 4;

        for (int i = 0; i < spawnCount; i++) {
            float localX = rand.nextFloat() * (chunkSize - 4) + 2;
            float localY = rand.nextFloat() * (chunkSize - 4) + 2;

            float worldX = worldStartX + localX;
            float worldY = worldStartY + localY;

            // 检查是否与墙体重叠
            boolean collision = false;
            for (WallEntity wall : chunk.getWalls()) {
                if (worldX >= wall.getOriginX() - 1 && worldX < wall.getOriginX() + wall.getGridWidth() + 1 &&
                        worldY >= wall.getOriginY() - 1 && worldY < wall.getOriginY() + wall.getGridHeight() + 1) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                chunk.addSpawnPoint(worldX, worldY);
            }
        }
    }

    /**
     * 获取玩家出生点（地图中心）
     */
    public Vector2 getPlayerSpawnPoint() {
        float centerX = EndlessModeConfig.MAP_WIDTH / 2f;
        float centerY = EndlessModeConfig.MAP_HEIGHT / 2f;
        return new Vector2(centerX, centerY);
    }

    /**
     * 设置随机种子
     */
    public void setSeed(long seed) {
        this.seed = seed;
        this.random.setSeed(seed);
    }

    /**
     * 获取当前种子
     */
    public long getSeed() {
        return seed;
    }
}
