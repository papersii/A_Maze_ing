package de.tum.cit.fop.maze.model;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图块数据结构 (Map Chunk)
 * 
 * 表示无尽模式地图中的一个64×64区块。
 * 用于分块加载和渲染优化。
 */
public class MapChunk {

    /** 区块X坐标（区块单位，非格子单位） */
    private final int chunkX;

    /** 区块Y坐标（区块单位） */
    private final int chunkY;

    /** 区块大小（格子数） */
    private final int size;

    /** 区块主题 */
    private String theme;

    /** 区块内的墙体实体 */
    private List<WallEntity> walls;

    /** 区块内的陷阱位置 */
    private List<Vector2> trapPositions;

    /** 区块内的敌人刷新点 */
    private List<Vector2> spawnPoints;

    /** 是否已生成 */
    private boolean isGenerated;

    /** 是否已加载到渲染系统 */
    private boolean isLoaded;

    /** 最后访问时间（用于LRU缓存） */
    private long lastAccessTime;

    /**
     * 构造函数
     * 
     * @param chunkX 区块X坐标
     * @param chunkY 区块Y坐标
     * @param size   区块大小
     */
    public MapChunk(int chunkX, int chunkY, int size) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.size = size;
        this.walls = new ArrayList<>();
        this.trapPositions = new ArrayList<>();
        this.spawnPoints = new ArrayList<>();
        this.isGenerated = false;
        this.isLoaded = false;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 获取区块在世界坐标系中的起始X（格子单位）
     */
    public int getWorldStartX() {
        return chunkX * size;
    }

    /**
     * 获取区块在世界坐标系中的起始Y（格子单位）
     */
    public int getWorldStartY() {
        return chunkY * size;
    }

    /**
     * 获取区块在世界坐标系中的结束X（格子单位，不含）
     */
    public int getWorldEndX() {
        return (chunkX + 1) * size;
    }

    /**
     * 获取区块在世界坐标系中的结束Y（格子单位，不含）
     */
    public int getWorldEndY() {
        return (chunkY + 1) * size;
    }

    /**
     * 检查指定世界坐标是否在此区块内
     */
    public boolean containsWorldPosition(float worldX, float worldY) {
        int startX = getWorldStartX();
        int startY = getWorldStartY();
        return worldX >= startX && worldX < startX + size &&
                worldY >= startY && worldY < startY + size;
    }

    /**
     * 添加墙体
     */
    public void addWall(WallEntity wall) {
        walls.add(wall);
    }

    /**
     * 添加陷阱位置
     */
    public void addTrap(float x, float y) {
        trapPositions.add(new Vector2(x, y));
    }

    /**
     * 添加敌人刷新点
     */
    public void addSpawnPoint(float x, float y) {
        spawnPoints.add(new Vector2(x, y));
    }

    /**
     * 标记为已生成
     */
    public void markGenerated() {
        this.isGenerated = true;
    }

    /**
     * 标记为已加载
     */
    public void markLoaded() {
        this.isLoaded = true;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 标记为已卸载
     */
    public void markUnloaded() {
        this.isLoaded = false;
    }

    /**
     * 更新访问时间
     */
    public void touch() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 获取唯一标识符
     */
    public String getId() {
        return chunkX + "_" + chunkY;
    }

    /**
     * 清空区块内容（释放内存）
     */
    public void clear() {
        walls.clear();
        trapPositions.clear();
        spawnPoints.clear();
        isGenerated = false;
        isLoaded = false;
    }

    // ========== Getters ==========

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    public int getSize() {
        return size;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public List<WallEntity> getWalls() {
        return walls;
    }

    public List<Vector2> getTrapPositions() {
        return trapPositions;
    }

    public List<Vector2> getSpawnPoints() {
        return spawnPoints;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public String toString() {
        return "MapChunk{" +
                "pos=(" + chunkX + "," + chunkY + ")" +
                ", theme=" + theme +
                ", walls=" + walls.size() +
                ", generated=" + isGenerated +
                ", loaded=" + isLoaded +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MapChunk mapChunk = (MapChunk) o;
        return chunkX == mapChunk.chunkX && chunkY == mapChunk.chunkY;
    }

    @Override
    public int hashCode() {
        return 31 * chunkX + chunkY;
    }
}
