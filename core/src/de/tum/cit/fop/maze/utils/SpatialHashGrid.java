package de.tum.cit.fop.maze.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 空间哈希网格 (Spatial Hash Grid)
 * 
 * 用于优化大规模实体的邻近查询，将 O(N) 线性扫描降至 O(1) 摊销查询。
 * 
 * 原理：将2D空间划分为固定大小的单元格，每个实体根据位置存入对应单元格。
 * 查询邻近实体时只需检查相关单元格，无需遍历全部实体。
 * 
 * @param <T> 实体类型（必须实现 Positioned 接口或提供坐标访问方式）
 */
public class SpatialHashGrid<T> {

    /** 单元格大小（世界单位） */
    private final float cellSize;

    /** 哈希表：cellKey -> 该单元格内的实体集合 */
    private final Map<Long, Set<T>> grid;

    /** 反向索引：实体 -> 当前所在单元格Key */
    private final Map<T, Long> entityCells;

    /**
     * 构造函数
     * 
     * @param cellSize 单元格大小（建议与渲染半径匹配，如16或32）
     */
    public SpatialHashGrid(float cellSize) {
        this.cellSize = cellSize;
        this.grid = new HashMap<>();
        this.entityCells = new HashMap<>();
    }

    /**
     * 计算单元格Key
     */
    private long getCellKey(float x, float y) {
        int cellX = (int) Math.floor(x / cellSize);
        int cellY = (int) Math.floor(y / cellSize);
        // 使用长整型组合两个int，避免碰撞
        return ((long) cellX << 32) | (cellY & 0xFFFFFFFFL);
    }

    /**
     * 插入实体
     * 
     * @param entity 实体对象
     * @param x      当前X坐标
     * @param y      当前Y坐标
     */
    public void insert(T entity, float x, float y) {
        long key = getCellKey(x, y);
        grid.computeIfAbsent(key, k -> new HashSet<>()).add(entity);
        entityCells.put(entity, key);
    }

    /**
     * 移除实体
     * 
     * @param entity 实体对象
     */
    public void remove(T entity) {
        Long key = entityCells.remove(entity);
        if (key != null) {
            Set<T> cell = grid.get(key);
            if (cell != null) {
                cell.remove(entity);
                if (cell.isEmpty()) {
                    grid.remove(key);
                }
            }
        }
    }

    /**
     * 更新实体位置
     * 
     * @param entity 实体对象
     * @param newX   新X坐标
     * @param newY   新Y坐标
     */
    public void update(T entity, float newX, float newY) {
        long newKey = getCellKey(newX, newY);
        Long oldKey = entityCells.get(entity);

        // 如果还在同一个单元格，无需更新
        if (oldKey != null && oldKey == newKey) {
            return;
        }

        // 从旧单元格移除
        if (oldKey != null) {
            Set<T> oldCell = grid.get(oldKey);
            if (oldCell != null) {
                oldCell.remove(entity);
                if (oldCell.isEmpty()) {
                    grid.remove(oldKey);
                }
            }
        }

        // 插入新单元格
        grid.computeIfAbsent(newKey, k -> new HashSet<>()).add(entity);
        entityCells.put(entity, newKey);
    }

    /**
     * 获取指定位置周围的实体
     * 
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param radius  查询半径
     * @return 半径内的实体列表
     */
    public List<T> getNearby(float centerX, float centerY, float radius) {
        List<T> result = new ArrayList<>();

        // 确定需要检查的单元格范围
        int minCellX = (int) Math.floor((centerX - radius) / cellSize);
        int maxCellX = (int) Math.floor((centerX + radius) / cellSize);
        int minCellY = (int) Math.floor((centerY - radius) / cellSize);
        int maxCellY = (int) Math.floor((centerY + radius) / cellSize);

        float radiusSq = radius * radius;

        // 遍历相关单元格
        for (int cx = minCellX; cx <= maxCellX; cx++) {
            for (int cy = minCellY; cy <= maxCellY; cy++) {
                long key = ((long) cx << 32) | (cy & 0xFFFFFFFFL);
                Set<T> cell = grid.get(key);
                if (cell != null) {
                    result.addAll(cell);
                }
            }
        }

        return result;
    }

    /**
     * 获取指定位置周围的实体（带精确距离过滤）
     * 
     * 此方法需要实体提供坐标，使用 PositionProvider 接口
     * 
     * @param centerX          中心X坐标
     * @param centerY          中心Y坐标
     * @param radius           查询半径
     * @param positionProvider 坐标提供器
     * @return 精确距离内的实体列表
     */
    public List<T> getNearbyExact(float centerX, float centerY, float radius, PositionProvider<T> positionProvider) {
        List<T> candidates = getNearby(centerX, centerY, radius);
        List<T> result = new ArrayList<>();

        float radiusSq = radius * radius;
        for (T entity : candidates) {
            float ex = positionProvider.getX(entity);
            float ey = positionProvider.getY(entity);
            float dx = ex - centerX;
            float dy = ey - centerY;
            if (dx * dx + dy * dy <= radiusSq) {
                result.add(entity);
            }
        }

        return result;
    }

    /**
     * 清空所有实体
     */
    public void clear() {
        grid.clear();
        entityCells.clear();
    }

    /**
     * 获取当前实体总数
     */
    public int size() {
        return entityCells.size();
    }

    /**
     * 坐标提供器接口
     */
    @FunctionalInterface
    public interface PositionProvider<T> {
        float getX(T entity);

        default float getY(T entity) {
            return 0; // 将由调用者实现完整版本
        }
    }

    /**
     * 完整的坐标提供器接口
     */
    public interface FullPositionProvider<T> {
        float getX(T entity);

        float getY(T entity);
    }

    /**
     * 获取指定位置周围的实体（带精确距离过滤，使用完整坐标提供器）
     */
    public List<T> getNearbyExact(float centerX, float centerY, float radius,
            FullPositionProvider<T> positionProvider) {
        List<T> candidates = getNearby(centerX, centerY, radius);
        List<T> result = new ArrayList<>();

        float radiusSq = radius * radius;
        for (T entity : candidates) {
            float ex = positionProvider.getX(entity);
            float ey = positionProvider.getY(entity);
            float dx = ex - centerX;
            float dy = ey - centerY;
            if (dx * dx + dy * dy <= radiusSq) {
                result.add(entity);
            }
        }

        return result;
    }
}
