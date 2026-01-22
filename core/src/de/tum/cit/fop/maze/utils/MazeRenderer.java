package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.model.GameMap;
import de.tum.cit.fop.maze.model.WallEntity;

import java.util.List;

/**
 * MazeRenderer - 重构版
 * 使用 WallEntity 列表渲染墙体，而不是遍历格子。
 */
public class MazeRenderer {

    private final SpriteBatch batch;
    private final TextureManager textureManager;
    private static final float UNIT_SCALE = 16f;

    private final GroutRenderer groutRenderer;

    public MazeRenderer(SpriteBatch batch, TextureManager textureManager) {
        this.batch = batch;
        this.textureManager = textureManager;
        this.groutRenderer = new GroutRenderer(textureManager);
    }

    public void renderFloor(GameMap gameMap, OrthographicCamera camera, TextureRegion floorTexture) {
        float zoom = camera.zoom;
        float viewW = camera.viewportWidth * zoom;
        float viewH = camera.viewportHeight * zoom;
        float viewX = camera.position.x - viewW / 2;
        float viewY = camera.position.y - viewH / 2;

        int minX = (int) (viewX / UNIT_SCALE) - 1;
        int maxX = (int) ((viewX + viewW) / UNIT_SCALE) + 1;
        int minY = (int) (viewY / UNIT_SCALE) - 1;
        int maxY = (int) ((viewY + viewH) / UNIT_SCALE) + 1;

        // Ensure bounds are within map limits
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(gameMap.getWidth() - 1, maxX);
        maxY = Math.min(gameMap.getHeight() - 1, maxY);

        // Pass 1: Floors - 区分可行走区域和墙体区域
        // 获取墙体底砖纹理（用于墙体所在格子）
        TextureRegion wallBaseFloor = textureManager.getWallBaseFloor(gameMap.getTheme());
        TextureRegion walkableFloor = floorTexture != null ? floorTexture
                : textureManager.getWalkableFloor(gameMap.getTheme());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                TextureRegion region;
                if (gameMap.isOccupied(x, y)) {
                    // 墙体所在格子 -> 使用墙体底砖
                    region = wallBaseFloor;
                } else {
                    // 可行走区域 -> 使用可行走地砖
                    region = walkableFloor;
                }
                batch.draw(region, x * UNIT_SCALE, y * UNIT_SCALE, UNIT_SCALE, UNIT_SCALE);
            }
        }

        // Pass 2: Grout (美缝)
        Color groutColor = getGroutColorForBiome(floorTexture);
        Color wallBoundaryColor = new Color(groutColor).mul(0.4f, 0.4f, 0.4f, 1f);
        wallBoundaryColor.a = 1f;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                groutRenderer.renderGrout(batch, gameMap, x, y, UNIT_SCALE, groutColor, wallBoundaryColor);
            }
        }

        // Reset color to avoid tinting walls
        batch.setColor(Color.WHITE);
    }

    public void renderWalls(GameMap gameMap, OrthographicCamera camera, float stateTime) {
        float zoom = camera.zoom;
        float viewW = camera.viewportWidth * zoom;
        float viewH = camera.viewportHeight * zoom;
        float viewX = camera.position.x - viewW / 2;
        float viewY = camera.position.y - viewH / 2;

        // Pass 3: Walls - 使用 WallEntity 列表渲染
        // 按Y坐标从高到低排序，实现正确的Z-ordering (Back to Front)
        List<WallEntity> walls = new java.util.ArrayList<>(gameMap.getWalls());
        walls.sort((w1, w2) -> Integer.compare(w2.getOriginY(), w1.getOriginY()));

        // 延迟渲染列表：用于存储墙顶部的渲染指令 (Grassland specific)
        List<Runnable> deferredTops = new java.util.ArrayList<>();

        boolean isGrassland = "grassland".equalsIgnoreCase(gameMap.getTheme());

        for (WallEntity wall : walls) {
            float wallX = wall.getOriginX() * UNIT_SCALE;
            float wallY = wall.getOriginY() * UNIT_SCALE;
            float wallW = wall.getGridWidth() * UNIT_SCALE;
            float wallH = wall.getGridHeight() * UNIT_SCALE;

            // 视锥体剔除 (Expanded bounds for tall walls)
            if (wallX + wallW < viewX || wallX > viewX + viewW)
                continue;
            if (wallY + wallH + UNIT_SCALE * 2 < viewY || wallY > viewY + viewH)
                continue;

            // 获取贴图
            TextureRegion reg = getWallRegion(wall, gameMap.getTheme(), stateTime);
            if (reg == null)
                continue;

            // Grassland Special Handling: Split Body and Top
            if (isGrassland && reg.getRegionHeight() >= 32) { // 至少由Top(16)+Body(16)组成
                int topH = 16;
                int bodyH = reg.getRegionHeight() - topH;

                // TextureRegion(region, x, y, w, h) - relative to region
                // Top Part (0, 0, W, 16)
                TextureRegion topReg = new TextureRegion(reg, 0, 0, reg.getRegionWidth(), topH);

                // Body Part (0, 16, W, BodyH)
                TextureRegion bodyReg = new TextureRegion(reg, 0, topH, reg.getRegionWidth(), bodyH);

                // Draw Body immediately
                // Note: bodyH pixels should map to wallH world units?
                // Visual Ratio: bodyH is H * 16. wallH is H * 16. Match 1:1.
                batch.draw(bodyReg, wallX, wallY, wallW, wallH);

                // Defer Top Draw
                // Top is drawn at wallY + wallH. Height = 1 unit (16px)
                deferredTops.add(() -> {
                    batch.draw(topReg, wallX, wallY + wallH, wallW, UNIT_SCALE);
                });

            } else {
                // Standard Rendering (Other themes or fallback)
                float drawWidth = wallW;
                float drawHeight = wallH;

                // Dynamic Height for single-sprite walls
                float texW = reg.getRegionWidth();
                float texH = reg.getRegionHeight();
                if (texW > 0) {
                    drawHeight = texH * (wallW / texW);
                }

                // 孤立墙体增加视觉高度
                if (drawHeight <= wallH && isWallIsolated(gameMap, wall) && !hasWallAbove(gameMap, wall)) {
                    drawHeight = wallH + 0.5f * UNIT_SCALE;
                }

                batch.draw(reg, wallX, wallY, drawWidth, drawHeight);
            }
        }

        // Render all deferred tops (sorted indirectly by insertion order, which is
        // Back-to-Front)
        // Back-to-Front Tops ensures Top(Front) covers Top(Back) if overlap.
        // And importantly, Tops are drawn AFTER all Bodies (implicit in this step).
        for (Runnable task : deferredTops) {
            task.run();
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * 检查墙体是否孤立（四周无相邻墙体）
     */
    private boolean isWallIsolated(GameMap gameMap, WallEntity wall) {
        int x = wall.getOriginX();
        int y = wall.getOriginY();
        int w = wall.getGridWidth();
        int h = wall.getGridHeight();

        // 检查左边
        for (int dy = 0; dy < h; dy++) {
            if (gameMap.isOccupied(x - 1, y + dy))
                return false;
        }
        // 检查右边
        for (int dy = 0; dy < h; dy++) {
            if (gameMap.isOccupied(x + w, y + dy))
                return false;
        }
        // 检查下边
        for (int dx = 0; dx < w; dx++) {
            if (gameMap.isOccupied(x + dx, y - 1))
                return false;
        }
        // 检查上边
        for (int dx = 0; dx < w; dx++) {
            if (gameMap.isOccupied(x + dx, y + h))
                return false;
        }

        return true;
    }

    /**
     * 检查墙体上方是否有墙
     */
    private boolean hasWallAbove(GameMap gameMap, WallEntity wall) {
        int x = wall.getOriginX();
        int y = wall.getOriginY();
        int w = wall.getGridWidth();
        int h = wall.getGridHeight();

        for (int dx = 0; dx < w; dx++) {
            if (gameMap.isOccupied(x + dx, y + h))
                return true;
        }
        return false;
    }

    // 缓存生物群系颜色
    private final com.badlogic.gdx.utils.ObjectMap<TextureRegion, Color> biomeColorCache = new com.badlogic.gdx.utils.ObjectMap<>();

    private Color getGroutColorForBiome(TextureRegion currentFloor) {
        if (currentFloor == null)
            return Color.DARK_GRAY;

        if (biomeColorCache.containsKey(currentFloor)) {
            return biomeColorCache.get(currentFloor);
        }

        Color color;

        if (currentFloor == textureManager.floorDungeon || currentFloor == textureManager.floorRegion) {
            color = new Color(0.25f, 0.25f, 0.25f, 0.5f);
        } else if (currentFloor == textureManager.floorDesert) {
            color = new Color(0.7f, 0.45f, 0.1f, 0.5f);
        } else if (currentFloor == textureManager.floorIce) {
            color = new Color(0.2f, 0.3f, 0.6f, 0.5f);
        } else if (currentFloor == textureManager.floorGrassland) {
            // 灰褐色调，匹配苔藓石板地砖
            color = new Color(0.25f, 0.22f, 0.18f, 0.5f);
        } else if (currentFloor == textureManager.floorJungle) {
            color = new Color(0.05f, 0.2f, 0.05f, 0.5f);
        } else if (currentFloor == textureManager.floorSpace) {
            color = new Color(0.05f, 0.1f, 0.3f, 0.5f);
        } else if (currentFloor == textureManager.floorLava) {
            color = new Color(0.4f, 0.1f, 0.1f, 0.5f);
        } else {
            Color autoColor = textureManager.getTextureColor(currentFloor);
            color = new Color(autoColor).mul(0.7f, 0.7f, 0.7f, 0.5f);
        }

        biomeColorCache.put(currentFloor, color);
        return color;
    }

    private TextureRegion getTextureForTheme(String theme) {
        if (theme == null)
            return textureManager.floorRegion;
        switch (theme.toLowerCase()) {
            case "desert":
                return textureManager.floorDesert;
            case "ice":
                return textureManager.floorIce;
            case "jungle":
                return textureManager.floorJungle;
            case "space":
                return textureManager.floorSpace;
            case "grassland":
                return textureManager.floorGrassland;
            default:
                return textureManager.floorRegion;
        }
    }

    private TextureRegion getWallRegion(WallEntity wall, String theme, float stateTime) {
        return textureManager.getWallRegion(theme, wall.getGridWidth(), wall.getGridHeight(),
                wall.getOriginX(), wall.getOriginY());
    }

    public void dispose() {
        if (groutRenderer != null) {
            groutRenderer.dispose();
        }
    }
}
