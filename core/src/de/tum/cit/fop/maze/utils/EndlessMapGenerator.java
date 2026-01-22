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

    /** 玩家出生点安全区域半径（格子数，确保玩家不会出生在墙内） */
    private static final int SPAWN_SAFE_ZONE_RADIUS = 8;

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

        // 生成宝箱位置 (密度约为陷阱的 1/20)
        generateChests(chunk, chunkRandom);

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

            // 转换为世界坐标进行安全区域检查
            int worldX = worldStartX + localX;
            int worldY = worldStartY + localY;

            // 检查是否在玩家出生安全区域内
            if (isInSpawnSafeZone(worldX, worldY, width, height)) {
                continue;
            }

            // 检查是否可以放置
            if (canPlaceWall(occupied, localX, localY, width, height, chunkSize)) {
                int typeId = getTypeIdForSize(width, height);

                // [MODIFIED] Determine collision height based on theme
                int collisionHeight = height;
                if ("grassland".equalsIgnoreCase(chunk.getTheme())) {
                    collisionHeight = 1;
                }

                WallEntity wall = new WallEntity(worldX, worldY, width, height, typeId, false, collisionHeight);
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
     * 
     * 修复：使用整数坐标确保陷阱严格对齐到网格格子，
     * 并通过HashSet追踪已占用位置避免重叠。
     */
    private void generateTraps(MapChunk chunk, Random rand) {
        int chunkSize = chunk.getSize();
        int worldStartX = chunk.getWorldStartX();
        int worldStartY = chunk.getWorldStartY();

        // 陷阱密度
        float trapDensity = 0.005f;
        int expectedTraps = (int) (chunkSize * chunkSize * trapDensity);

        // 追踪已放置陷阱的格子位置，避免重复
        java.util.Set<String> occupiedCells = new java.util.HashSet<>();

        int attempts = 0;
        int maxAttempts = expectedTraps * 3; // 最大尝试次数，避免无限循环
        int trapsPlaced = 0;

        while (trapsPlaced < expectedTraps && attempts < maxAttempts) {
            attempts++;

            // 使用整数坐标，确保陷阱严格对齐到格子
            int localX = rand.nextInt(chunkSize - 2) + 1;
            int localY = rand.nextInt(chunkSize - 2) + 1;

            int worldX = worldStartX + localX;
            int worldY = worldStartY + localY;

            // 生成唯一键用于去重
            String cellKey = worldX + "," + worldY;

            // 检查是否已有陷阱在此位置
            if (occupiedCells.contains(cellKey)) {
                continue;
            }

            // 检查是否在玩家出生安全区域内
            if (isInSpawnSafeZone(worldX, worldY, 1, 1)) {
                continue;
            }

            // 检查是否与墙体重叠
            boolean collision = false;
            for (WallEntity wall : chunk.getWalls()) {
                if (worldX >= wall.getOriginX() && worldX < wall.getOriginX() + wall.getGridWidth() &&
                        worldY >= wall.getOriginY() && worldY < wall.getOriginY() + wall.getGridHeight()) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                // 使用整数坐标添加陷阱
                chunk.addTrap(worldX, worldY);
                occupiedCells.add(cellKey);
                trapsPlaced++;
            }
        }
    }

    /**
     * 生成宝箱位置
     * 
     * 密度约为陷阱数量的 1/20，确保每个区块有 0-1 个宝箱概率。
     * 宝箱不与墙体或陷阱重叠。
     */
    private void generateChests(MapChunk chunk, Random rand) {
        int chunkSize = chunk.getSize();
        int worldStartX = chunk.getWorldStartX();
        int worldStartY = chunk.getWorldStartY();

        // 根据陷阱数量计算宝箱数量（约 1/20，至少0个）
        int trapCount = chunk.getTrapPositions().size();
        int expectedChests = Math.max(0, (int) (trapCount * GameConfig.CHEST_DENSITY_RATIO));

        // 每个区块最多 1-2 个宝箱，避免过于密集
        expectedChests = Math.min(expectedChests, 2);

        // 收集已占用的格子（墙体 + 陷阱）
        java.util.Set<String> occupiedCells = new java.util.HashSet<>();
        for (WallEntity wall : chunk.getWalls()) {
            for (int wx = wall.getOriginX(); wx < wall.getOriginX() + wall.getGridWidth(); wx++) {
                for (int wy = wall.getOriginY(); wy < wall.getOriginY() + wall.getGridHeight(); wy++) {
                    occupiedCells.add(wx + "," + wy);
                }
            }
        }
        for (Vector2 trap : chunk.getTrapPositions()) {
            occupiedCells.add((int) trap.x + "," + (int) trap.y);
        }

        int attempts = 0;
        int maxAttempts = expectedChests * 5;
        int chestsPlaced = 0;

        while (chestsPlaced < expectedChests && attempts < maxAttempts) {
            attempts++;

            // 使用整数坐标，确保宝箱对齐到格子
            int localX = rand.nextInt(chunkSize - 4) + 2;
            int localY = rand.nextInt(chunkSize - 4) + 2;

            int worldX = worldStartX + localX;
            int worldY = worldStartY + localY;

            String cellKey = worldX + "," + worldY;

            // 检查是否已占用
            if (occupiedCells.contains(cellKey)) {
                continue;
            }

            // 检查是否在玩家出生安全区域内
            if (isInSpawnSafeZone(worldX, worldY, 1, 1)) {
                continue;
            }

            // 放置宝箱
            chunk.addChest(worldX, worldY);
            occupiedCells.add(cellKey);
            chestsPlaced++;
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
     * 检查给定位置是否在玩家出生安全区域内
     * 
     * @param worldX 世界坐标X
     * @param worldY 世界坐标Y
     * @param width  物体宽度
     * @param height 物体高度
     * @return true如果该区域与安全区域重叠
     */
    private boolean isInSpawnSafeZone(int worldX, int worldY, int width, int height) {
        float spawnX = EndlessModeConfig.MAP_WIDTH / 2f;
        float spawnY = EndlessModeConfig.MAP_HEIGHT / 2f;
        float radius = SPAWN_SAFE_ZONE_RADIUS;

        // 检查物体的四个角是否在安全区域内
        float[] cornersX = { worldX, worldX + width, worldX, worldX + width };
        float[] cornersY = { worldY, worldY, worldY + height, worldY + height };

        for (int i = 0; i < 4; i++) {
            float dx = cornersX[i] - spawnX;
            float dy = cornersY[i] - spawnY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= radius) {
                return true;
            }
        }

        // 额外检查：物体中心到出生点的距离
        float centerX = worldX + width / 2f;
        float centerY = worldY + height / 2f;
        float dx = centerX - spawnX;
        float dy = centerY - spawnY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // 考虑物体尺寸的一半
        float effectiveRadius = radius + Math.max(width, height) / 2f;
        return dist <= effectiveRadius;
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
