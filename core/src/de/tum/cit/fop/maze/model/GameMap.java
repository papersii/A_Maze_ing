package de.tum.cit.fop.maze.model;

import java.util.ArrayList;
import java.util.List;

/**
 * GameMap 用于存储当前关卡的所有数据。
 * 它包含所有的游戏对象列表以及玩家的初始位置。
 */
public class GameMap {
    // 存储地图的像素或网格宽/高 (基于最远的物体位置)
    private int width = 0;
    private int height = 0;

    // 存储所有的实体对象 (墙, 敌人, 钥匙, 出口等)
    private List<GameObject> gameObjects;

    // 玩家的初始出生点 (对应 ID=1 的 Entry Point)
    private float playerStartX = 0;
    private float playerStartY = 0;

    public GameMap() {
        this.gameObjects = new ArrayList<>();
    }

    /**
     * 向地图添加一个对象，并根据对象位置自动更新地图边界
     */
    public void addGameObject(GameObject obj) {
        this.gameObjects.add(obj);

        // 动态更新地图尺寸，方便相机知道边界在哪里
        // 假设每个格子大小是 1 单位，那么边界就是坐标 + 1
        if ((int)obj.getX() + 1 > width) {
            width = (int)obj.getX() + 1;
        }
        if ((int)obj.getY() + 1 > height) {
            height = (int)obj.getY() + 1;
        }
    }

    /**
     * 设置玩家的出生点 (当解析到 ID=1 时调用)
     */
    public void setPlayerStart(float x, float y) {
        this.playerStartX = x;
        this.playerStartY = y;

        // 即使没有实体物体，出生点也算作地图的一部分，需要更新边界
        if ((int)x + 1 > width) width = (int)x + 1;
        if ((int)y + 1 > height) height = (int)y + 1;
    }

    // --- Getters ---

    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getPlayerStartX() {
        return playerStartX;
    }

    public float getPlayerStartY() {
        return playerStartY;
    }
}