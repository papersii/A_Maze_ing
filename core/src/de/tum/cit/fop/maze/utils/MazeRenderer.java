package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.model.GameMap;
import de.tum.cit.fop.maze.model.Wall;

/**
 * Handles rendering of the maze (Walls, Floors).
 */
public class MazeRenderer {

    private final SpriteBatch batch;
    private final TextureManager textureManager;
    private static final float UNIT_SCALE = 16f;

    public MazeRenderer(SpriteBatch batch, TextureManager textureManager) {
        this.batch = batch;
        this.textureManager = textureManager;
    }

    public void render(GameMap gameMap, OrthographicCamera camera, TextureRegion floorTexture) {
        float zoom = camera.zoom;
        float viewW = camera.viewportWidth * zoom;
        float viewH = camera.viewportHeight * zoom;
        float viewX = camera.position.x - viewW / 2;
        float viewY = camera.position.y - viewH / 2;

        int minX = (int) (viewX / UNIT_SCALE) - 1;
        int maxX = (int) ((viewX + viewW) / UNIT_SCALE) + 1;
        int minY = (int) (viewY / UNIT_SCALE) - 1;
        int maxY = (int) ((viewY + viewH) / UNIT_SCALE) + 1;

        // Apply Biome Tint - Disabled for new textures to show their natural colors
        // batch.setColor(biomeColor);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                // Always draw floor first
                if (floorTexture != null) {
                    batch.draw(floorTexture, x * UNIT_SCALE, y * UNIT_SCALE, UNIT_SCALE, UNIT_SCALE);
                } else {
                    batch.draw(textureManager.floorRegion, x * UNIT_SCALE, y * UNIT_SCALE, UNIT_SCALE, UNIT_SCALE);
                }

                // Draw Wall if present
                Wall wall = gameMap.getWall(x, y);
                if (wall != null) {
                    batch.draw(textureManager.wallRegion, wall.getX() * UNIT_SCALE, wall.getY() * UNIT_SCALE);
                }
            }
        }

        // Reset Tint
        batch.setColor(Color.WHITE);
    }
}
