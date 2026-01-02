package de.tum.cit.fop.maze.model;

/**
 * 所有游戏实体的基类
 * 对应文档要求：Inheriting from at least one common superclass
 */
public abstract class GameObject {
    // 坐标使用 float，方便平滑移动，但地图解析时通常是整数
    protected float x;
    protected float y;

    // 这种逻辑上的宽和高通常是 1 (代表 1 个格子)
    protected float width = 1;
    protected float height = 1;

    public GameObject(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Getters
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }

    // Setters (用于移动)
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
