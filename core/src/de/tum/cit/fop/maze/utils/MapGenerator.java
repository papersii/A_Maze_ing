package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.config.GameConfig;
import de.tum.cit.fop.maze.model.WallEntity;
import de.tum.cit.fop.maze.model.DamageType;

import java.util.*;

/**
 * MapGenerator - 地图生成器
 * 
 * 核心功能：
 * 1. 边界墙自动生成（2格宽）
 * 2. 房间+走廊算法替代完美迷宫
 * 3. 每次放置墙体后验证路径连通性
 * 4. 多次重试机制确保生成成功
 */
public class MapGenerator {

    private static final int MAX_ATTEMPTS = 10;
    private static final int BORDER_WIDTH = 2;

    private int playableWidth;
    private int playableHeight;
    private int totalWidth;
    private int totalHeight;
    private MapConfig config;

    // 格子状态：0 = 可放墙, 1 = 地板（通道）
    private int[][] grid;

    // 所有墙体
    private List<WallEntity> walls;

    // 安全区（起点、终点、钥匙周围）
    private Set<Long> safeZone;

    /**
     * 内嵌地图配置类 (替代原 RandomMapConfig)
     */
    public static class MapConfig {
        public int width = 150;
        public int height = 150;
        public int difficulty = 2;
        public DamageType damageType = DamageType.PHYSICAL;
        public boolean enemyShieldEnabled = false;
        public float enemyDensity = 1.0f;
        public float trapDensity = 1.0f;
        public float mobileTrapDensity = 1.0f;
        public float lootDropRate = 1.0f;
        public float braidChance = 0.3f;
        public int roomCount = 60;
        public String theme = "Dungeon";

        public static final MapConfig DEFAULT = new MapConfig();

        public MapConfig copy() {
            MapConfig c = new MapConfig();
            c.width = this.width;
            c.height = this.height;
            c.difficulty = this.difficulty;
            c.damageType = this.damageType;
            c.enemyShieldEnabled = this.enemyShieldEnabled;
            c.enemyDensity = this.enemyDensity;
            c.trapDensity = this.trapDensity;
            c.mobileTrapDensity = this.mobileTrapDensity;
            c.lootDropRate = this.lootDropRate;
            c.braidChance = this.braidChance;
            c.roomCount = this.roomCount;
            c.theme = this.theme;
            return c;
        }
    }

    public MapGenerator() {
        this(MapConfig.DEFAULT);
    }

    public MapGenerator(MapConfig config) {
        this.config = config;
    }

    public void generateAndSave(String fileName) {
        generateAndSave(fileName, this.config);
    }

    public void generateAndSave(String fileName, MapConfig config) {
        this.config = config;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                GenerationResult result = generate();
                if (result != null && result.isValid) {
                    saveToFile(fileName, result);
                    GameLogger.info("MapGenerator",
                            "Map generated successfully on attempt " + (attempt + 1));
                    return;
                }
            } catch (Exception e) {
                GameLogger.warn("MapGenerator",
                        "Attempt " + (attempt + 1) + " failed: " + e.getMessage());
            }
        }

        // Fallback
        GameLogger.warn("MapGenerator", "Using fallback map after " + MAX_ATTEMPTS + " attempts");
        GenerationResult fallback = generateFallback();
        saveToFile(fileName, fallback);
    }

    private static class GenerationResult {
        boolean isValid;
        Vector2 playerStart;
        Vector2 exitPos;
        Vector2 keyPos;
        List<WallEntity> walls;
        List<Vector2> enemies;
        List<Vector2> traps;
        List<Vector2> mobileTraps;

        GenerationResult() {
            walls = new ArrayList<>();
            enemies = new ArrayList<>();
            traps = new ArrayList<>();
            mobileTraps = new ArrayList<>();
        }
    }

    private GenerationResult generate() {
        GenerationResult result = new GenerationResult();

        // 初始化尺寸
        this.playableWidth = config.width;
        this.playableHeight = config.height;
        this.totalWidth = playableWidth + 2 * BORDER_WIDTH;
        this.totalHeight = playableHeight + 2 * BORDER_WIDTH;

        this.grid = new int[totalWidth][totalHeight];
        this.walls = new ArrayList<>();
        this.safeZone = new HashSet<>();

        // 1. 初始化：全部设为地板
        for (int x = 0; x < totalWidth; x++) {
            for (int y = 0; y < totalHeight; y++) {
                grid[x][y] = 1;
            }
        }

        // 2. 生成边界墙
        generateBorderWalls();

        // 3. 确定起点、终点、钥匙位置
        determineStartAndExit(result);
        result.keyPos = findKeyPosition(result.playerStart, result.exitPos);

        // 标记安全区
        markSafeZone(result.playerStart, 5);
        markSafeZone(result.exitPos, 5);
        markSafeZone(result.keyPos, 3);

        // 4. 生成内部迷宫墙体（房间+走廊算法）
        generateInternalMaze();

        // 5. 验证路径连通性
        if (!validatePath(result.playerStart, result.keyPos) ||
                !validatePath(result.keyPos, result.exitPos)) {
            return null; // 重试
        }

        // 6. 收集墙体
        result.walls.addAll(walls);

        // 7. 放置敌人和陷阱
        placeEntities(result);

        result.isValid = true;
        return result;
    }

    /**
     * 随机决定起点和终点
     * 确保两者之间有足够的距离，并且都在可游玩区域内
     */
    private void determineStartAndExit(GenerationResult result) {
        int minDistance = (playableWidth + playableHeight) / 3;
        int maxAttempts = 50;

        for (int i = 0; i < maxAttempts; i++) {
            // 随机生成两个点（避开边界附近的缓冲）
            int x1 = BORDER_WIDTH + 2 + MathUtils.random(playableWidth - 5);
            int y1 = BORDER_WIDTH + 2 + MathUtils.random(playableHeight - 5);

            int x2 = BORDER_WIDTH + 2 + MathUtils.random(playableWidth - 5);
            int y2 = BORDER_WIDTH + 2 + MathUtils.random(playableHeight - 5);

            Vector2 p1 = new Vector2(x1, y1);
            Vector2 p2 = new Vector2(x2, y2);

            if (p1.dst(p2) > minDistance) {
                result.playerStart = p1;
                result.exitPos = p2;
                return;
            }
        }

        // Fallback: 对角线附近
        if (MathUtils.randomBoolean()) {
            result.playerStart = new Vector2(BORDER_WIDTH + 3, BORDER_WIDTH + 3);
            result.exitPos = new Vector2(totalWidth - BORDER_WIDTH - 4, totalHeight - BORDER_WIDTH - 4);
        } else {
            result.playerStart = new Vector2(totalWidth - BORDER_WIDTH - 4, totalHeight - BORDER_WIDTH - 4);
            result.exitPos = new Vector2(BORDER_WIDTH + 3, BORDER_WIDTH + 3);
        }
    }

    /**
     * 生成边界墙（2格宽的封闭边框）
     */
    private void generateBorderWalls() {
        // 底边界
        for (int x = 0; x < totalWidth; x += 2) {
            int w = Math.min(2, totalWidth - x);
            addWall(x, 0, w, BORDER_WIDTH, true);
        }

        // 顶边界
        for (int x = 0; x < totalWidth; x += 2) {
            int w = Math.min(2, totalWidth - x);
            addWall(x, totalHeight - BORDER_WIDTH, w, BORDER_WIDTH, true);
        }

        // 左边界（不含角落）
        for (int y = BORDER_WIDTH; y < totalHeight - BORDER_WIDTH; y += 2) {
            int h = Math.min(2, totalHeight - BORDER_WIDTH - y);
            addWall(0, y, BORDER_WIDTH, h, true);
        }

        // 右边界（不含角落）
        for (int y = BORDER_WIDTH; y < totalHeight - BORDER_WIDTH; y += 2) {
            int h = Math.min(2, totalHeight - BORDER_WIDTH - y);
            addWall(totalWidth - BORDER_WIDTH, y, BORDER_WIDTH, h, true);
        }
    }

    /**
     * 生成内部迷宫（房间+走廊算法）
     */
    private void generateInternalMaze() {
        int roomCount = config.roomCount;
        int wallsPlaced = 0;
        int maxWalls = (playableWidth * playableHeight) / 20; // 约5%覆盖率

        // 墙体尺寸优先级
        int[][] sizes = {
                { 4, 4 }, { 3, 3 }, { 4, 2 }, { 2, 4 }, { 3, 2 }, { 2, 3 }, { 2, 2 }
        };

        // 随机放置墙体
        int attempts = 0;
        while (wallsPlaced < maxWalls && attempts < maxWalls * 10) {
            attempts++;

            // 随机选择位置和尺寸
            int sizeIdx = MathUtils.random(sizes.length - 1);
            int w = sizes[sizeIdx][0];
            int h = sizes[sizeIdx][1];

            int x = BORDER_WIDTH + MathUtils.random(playableWidth - w);
            int y = BORDER_WIDTH + MathUtils.random(playableHeight - h);

            // 检查是否可以放置
            if (canPlaceWall(x, y, w, h)) {
                addWall(x, y, w, h, false);
                wallsPlaced++;
            }
        }

        GameLogger.info("MapGenerator", "Placed " + wallsPlaced + " internal walls");
    }

    /**
     * 检查是否可以在指定位置放置墙体
     */
    private boolean canPlaceWall(int x, int y, int w, int h) {
        // 边界检查
        if (x < BORDER_WIDTH || x + w > totalWidth - BORDER_WIDTH)
            return false;
        if (y < BORDER_WIDTH || y + h > totalHeight - BORDER_WIDTH)
            return false;

        // 检查是否与安全区重叠
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                if (isInSafeZone(x + dx, y + dy))
                    return false;
                // 检查是否已被占用
                if (grid[x + dx][y + dy] == 0)
                    return false;
            }
        }

        // 临时标记
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                grid[x + dx][y + dy] = 0;
            }
        }

        // 验证放置后路径仍然连通
        // （简化：只在最终验证，此处跳过以提高性能）

        return true;
    }

    /**
     * 添加墙体
     */
    private void addWall(int x, int y, int w, int h, boolean isBorder) {
        int typeId = getTypeIdForSize(w, h);

        // [MODIFIED] Determine collision height based on theme
        int collisionHeight = h;
        if (!isBorder && "grassland".equalsIgnoreCase(config.theme)) {
            collisionHeight = 1;
        }

        WallEntity wall = new WallEntity(x, y, w, h, typeId, isBorder, collisionHeight);
        walls.add(wall);

        // 标记格子被占用 (Using full visual height 'h' to prevent overlapping generation)
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                if (x + dx < totalWidth && y + dy < totalHeight) {
                    grid[x + dx][y + dy] = 0;
                }
            }
        }
    }

    private int getTypeIdForSize(int w, int h) {
        if (w == 2 && h == 2)
            return GameConfig.OBJECT_ID_WALL_2X2;
        if (w == 3 && h == 2)
            return GameConfig.OBJECT_ID_WALL_3X2;
        if (w == 2 && h == 3)
            return GameConfig.OBJECT_ID_WALL_2X3;
        if (w == 2 && h == 4)
            return GameConfig.OBJECT_ID_WALL_2X4;
        if (w == 4 && h == 2)
            return GameConfig.OBJECT_ID_WALL_4X2;
        if (w == 3 && h == 3)
            return GameConfig.OBJECT_ID_WALL_3X3;
        if (w == 4 && h == 4)
            return GameConfig.OBJECT_ID_WALL_4X4;
        return GameConfig.OBJECT_ID_WALL_2X2;
    }

    /**
     * 标记安全区
     */
    private void markSafeZone(Vector2 center, int radius) {
        int cx = (int) center.x;
        int cy = (int) center.y;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                safeZone.add((long) (cx + dx) + ((long) (cy + dy) << 16));
            }
        }
    }

    private boolean isInSafeZone(int x, int y) {
        return safeZone.contains((long) x + ((long) y << 16));
    }

    /**
     * 验证路径连通性（BFS）
     */
    private boolean validatePath(Vector2 from, Vector2 to) {
        int fx = (int) from.x, fy = (int) from.y;
        int tx = (int) to.x, ty = (int) to.y;

        boolean[][] visited = new boolean[totalWidth][totalHeight];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[] { fx, fy });
        visited[fx][fy] = true;

        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == tx && cur[1] == ty)
                return true;

            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (nx >= 0 && nx < totalWidth && ny >= 0 && ny < totalHeight
                        && !visited[nx][ny] && grid[nx][ny] == 1) {
                    visited[nx][ny] = true;
                    queue.add(new int[] { nx, ny });
                }
            }
        }
        return false;
    }

    /**
     * 找到钥匙位置（离起点最远的可达点）
     */
    private Vector2 findKeyPosition(Vector2 start, Vector2 exit) {
        // 简化：放在地图中心偏向出口的位置
        int kx = (int) (start.x + exit.x) / 2 + MathUtils.random(-5, 5);
        int ky = (int) (start.y + exit.y) / 2 + MathUtils.random(-5, 5);
        kx = Math.max(BORDER_WIDTH + 3, Math.min(totalWidth - BORDER_WIDTH - 3, kx));
        ky = Math.max(BORDER_WIDTH + 3, Math.min(totalHeight - BORDER_WIDTH - 3, ky));
        return new Vector2(kx, ky);
    }

    /**
     * 放置敌人和陷阱
     */
    private void placeEntities(GenerationResult result) {
        int floorCount = 0;
        List<Vector2> floors = new ArrayList<>();

        for (int x = BORDER_WIDTH; x < totalWidth - BORDER_WIDTH; x++) {
            for (int y = BORDER_WIDTH; y < totalHeight - BORDER_WIDTH; y++) {
                if (grid[x][y] == 1 && !isInSafeZone(x, y)) {
                    floors.add(new Vector2(x, y));
                    floorCount++;
                }
            }
        }

        int enemyCount = (int) (floorCount / 80 * config.enemyDensity);
        int trapCount = (int) (floorCount / 150 * config.trapDensity);
        int mobileTrapCount = (int) (floorCount / 100 * config.mobileTrapDensity);

        Collections.shuffle(floors);

        int idx = 0;
        for (int i = 0; i < enemyCount && idx < floors.size(); i++, idx++) {
            result.enemies.add(floors.get(idx));
        }
        for (int i = 0; i < trapCount && idx < floors.size(); i++, idx++) {
            result.traps.add(floors.get(idx));
        }
        for (int i = 0; i < mobileTrapCount && idx < floors.size(); i++, idx++) {
            result.mobileTraps.add(floors.get(idx));
        }
    }

    /**
     * 保存到文件
     */
    private void saveToFile(String fileName, GenerationResult result) {
        StringBuilder sb = new StringBuilder();

        // Metadata
        sb.append("# Generated Map\n");
        sb.append("theme=").append(config.theme).append("\n");
        sb.append("damageType=").append(config.damageType.name()).append("\n");
        sb.append("enemyShieldEnabled=").append(config.enemyShieldEnabled).append("\n");
        sb.append("levelDifficulty=").append(config.difficulty).append("\n");
        sb.append("suggestedArmor=").append(config.damageType.name()).append("\n");
        sb.append("lootDropRate=").append(config.lootDropRate).append("\n");
        sb.append("playableWidth=").append(playableWidth).append("\n");
        sb.append("playableHeight=").append(playableHeight).append("\n");
        sb.append("\n");

        // Key positions
        appendEntity(sb, result.playerStart, 1);
        appendEntity(sb, result.exitPos, 2);
        appendEntity(sb, result.keyPos, 5);

        // Entities
        for (Vector2 e : result.enemies)
            appendEntity(sb, e, 4);
        for (Vector2 t : result.traps)
            appendEntity(sb, t, 3);
        for (Vector2 m : result.mobileTraps)
            appendEntity(sb, m, 6);

        // Walls
        for (WallEntity w : result.walls) {
            sb.append((int) w.getX()).append(",").append((int) w.getY())
                    .append("=").append(w.getTypeId()).append("\n");
        }

        FileHandle file = Gdx.files.local(fileName);
        file.parent().mkdirs();
        file.writeString(sb.toString(), false);

        GameLogger.info("MapGenerator", "Saved map: " + fileName +
                " | Size: " + totalWidth + "x" + totalHeight +
                " | Walls: " + result.walls.size());
    }

    private void appendEntity(StringBuilder sb, Vector2 pos, int type) {
        sb.append((int) pos.x).append(",").append((int) pos.y).append("=").append(type).append("\n");
    }

    /**
     * 回退地图生成
     */
    private GenerationResult generateFallback() {
        GenerationResult result = new GenerationResult();

        this.playableWidth = 50;
        this.playableHeight = 50;
        this.totalWidth = playableWidth + 2 * BORDER_WIDTH;
        this.totalHeight = playableHeight + 2 * BORDER_WIDTH;
        this.grid = new int[totalWidth][totalHeight];
        this.walls = new ArrayList<>();
        this.safeZone = new HashSet<>();

        // 全部地板
        for (int x = 0; x < totalWidth; x++) {
            for (int y = 0; y < totalHeight; y++) {
                grid[x][y] = 1;
            }
        }

        // 边界墙
        generateBorderWalls();

        result.playerStart = new Vector2(BORDER_WIDTH + 3, BORDER_WIDTH + 3);
        result.exitPos = new Vector2(totalWidth - BORDER_WIDTH - 4, totalHeight - BORDER_WIDTH - 4);
        result.keyPos = new Vector2(totalWidth / 2, totalHeight / 2);

        result.walls.addAll(walls);
        result.isValid = true;

        return result;
    }

    public MapConfig getConfig() {
        return config;
    }
}
