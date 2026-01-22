package de.tum.cit.fop.maze.model;

import java.util.HashSet;
import java.util.Set;

/**
 * 墙体实体类 - 表示一个完整的多格墙体
 * 
 * 设计原则：
 * 1. 一个墙体是一个整体，不是多个单独格子的集合
 * 2. 碰撞区域 = width × height 格子（精确）
 * 3. 渲染尺寸可以有视觉延伸（不影响碰撞）
 */
public class WallEntity extends GameObject {

    // 墙体类型ID（对应贴图）
    private final int typeId;

    // 墙体逻辑尺寸（格子数）
    private final int gridWidth;
    private final int gridHeight;

    // 墙体碰撞高度（格子数）
    private final int collisionHeight;

    // 是否是边界墙
    private final boolean isBorderWall;

    // 缓存：该墙体占用的所有格子坐标
    private final Set<Long> occupiedCells;

    /**
     * 创建一个墙体实体
     * 
     * @param originX         左下角X坐标（格子坐标）
     * @param originY         左下角Y坐标（格子坐标）
     * @param gridWidth       宽度（格子数）
     * @param gridHeight      高度（格子数）
     * @param typeId          墙体类型ID
     * @param isBorderWall    是否是边界墙
     * @param collisionHeight 碰撞高度（通常等于gridHeight，但在某些主题下可能较小）
     */
    public WallEntity(int originX, int originY, int gridWidth, int gridHeight, int typeId, boolean isBorderWall,
            int collisionHeight) {
        super(originX, originY);
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.width = gridWidth;
        this.height = gridHeight;
        this.typeId = typeId;
        this.isBorderWall = isBorderWall;
        this.collisionHeight = collisionHeight;

        // 预计算占用的格子
        this.occupiedCells = new HashSet<>();
        for (int dx = 0; dx < gridWidth; dx++) {
            // 使用 collisionHeight 而不是 gridHeight 确定碰撞区域
            for (int dy = 0; dy < collisionHeight; dy++) {
                // 使用 x + (y << 16) 作为唯一key
                long key = (originX + dx) + ((long) (originY + dy) << 16);
                occupiedCells.add(key);
            }
        }
    }

    /**
     * 创建一个墙体实体（默认碰撞高度 = 网格高度）
     */
    public WallEntity(int originX, int originY, int gridWidth, int gridHeight, int typeId, boolean isBorderWall) {
        this(originX, originY, gridWidth, gridHeight, typeId, isBorderWall, gridHeight);
    }

    /**
     * 简化构造器（非边界墙）
     */
    public WallEntity(int originX, int originY, int gridWidth, int gridHeight, int typeId) {
        this(originX, originY, gridWidth, gridHeight, typeId, false);
    }

    /**
     * 检查指定格子是否被该墙体占用
     */
    public boolean occupies(int x, int y) {
        long key = x + ((long) y << 16);
        return occupiedCells.contains(key);
    }

    /**
     * 获取所有占用格子的坐标key集合
     */
    public Set<Long> getOccupiedCells() {
        return occupiedCells;
    }

    // ===== Getters =====

    public int getOriginX() {
        return (int) x;
    }

    public int getOriginY() {
        return (int) y;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public int getCollisionHeight() {
        return collisionHeight;
    }

    public int getTypeId() {
        return typeId;
    }

    public boolean isBorderWall() {
        return isBorderWall;
    }

    @Override
    public String toString() {
        return String.format("WallEntity[%d,%d %dx%d type=%d border=%s]",
                (int) x, (int) y, gridWidth, gridHeight, typeId, isBorderWall);
    }
}
