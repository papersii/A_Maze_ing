package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import de.tum.cit.fop.maze.model.GameMap;
import de.tum.cit.fop.maze.model.Wall;

/**
 * Handles the rendering of "Grout" (美缝) between tiles.
 * Supports adaptive coloring and different styles for Wall-Floor vs
 * Floor-Floor.
 */
public class GroutRenderer implements Disposable {

    private final TextureManager textureManager;
    private static final float GROUT_WIDTH_A = 1.0f; // Floor-Floor width (wide)
    private static final float GROUT_WIDTH_B = 1.5f; // Floor-Wall width (medium)
    private static final float GROUT_WIDTH_C = 0.5f; // Wall-Wall width (thin)

    // 3D Effect constants
    private static final Color SHADOW_TINT = new Color(0f, 0f, 0f, 0.4f);
    private static final Color HIGHLIGHT_TINT = new Color(1f, 1f, 1f, 0.3f);
    private static final Color WALL_BOUNDARY_COLOR = new Color(0.2f, 0.2f, 0.2f, 1f); // Dark Grey for walls

    private TextureRegion verticalGroutTexture;
    private TextureRegion horizontalGroutTexture;

    public GroutRenderer(TextureManager textureManager) {
        this.textureManager = textureManager;
        initGroutTextures();
    }

    private void initGroutTextures() {
        // Create a 3x1 texture for Vertical Grout (Light - Dark - Light) for 3D effect
        // We use Grayscale so we can tint it.
        // Center = 200 (approx 0.8 brightness), Edges = 255 (1.0 brightness)
        Pixmap vPix = new Pixmap(3, 1, Pixmap.Format.RGBA8888);
        vPix.setColor(1f, 1f, 1f, 1f); // White (Edge)
        vPix.drawPixel(0, 0);
        vPix.drawPixel(2, 0);
        vPix.setColor(0.8f, 0.8f, 0.8f, 1f); // Light Grey (Center)
        vPix.drawPixel(1, 0);

        Texture vTex = new Texture(vPix);
        vTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // Keep sharp
        this.verticalGroutTexture = new TextureRegion(vTex);
        vPix.dispose();

        // Create 1x3 texture for Horizontal
        Pixmap hPix = new Pixmap(1, 3, Pixmap.Format.RGBA8888);
        hPix.setColor(1f, 1f, 1f, 1f);
        hPix.drawPixel(0, 0); // Bottom
        hPix.drawPixel(0, 2); // Top
        hPix.setColor(0.8f, 0.8f, 0.8f, 1f);
        hPix.drawPixel(0, 1); // Center

        Texture hTex = new Texture(hPix);
        hTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.horizontalGroutTexture = new TextureRegion(hTex);
        hPix.dispose();
    }

    /**
     * Renders Grout for a specific tile position (East and North edges).
     * 
     * @param batch             SpriteBatch to draw with
     * @param gameMap           Game map to check neighbors
     * @param x                 Grid X
     * @param y                 Grid Y
     * @param unitScale         Size of one tile (e.g., 16)
     * @param baseColor         The adaptive color of the current floor tile
     *                          (Floor-Floor)
     * @param wallBoundaryColor The color for Floor-Wall and Wall-Wall boundaries
     */
    public void renderGrout(SpriteBatch batch, GameMap gameMap, int x, int y, float unitScale, Color baseColor,
            Color wallBoundaryColor) {
        // We only draw updates for Right (East) and Top (North) to avoid double
        // drawing.
        // Left and Bottom are handled by the previous neighbor's Right and Top.

        // Current Tile Info
        boolean isWall = gameMap.isOccupied(x, y);

        // --- 1. Right Edge (East) ---
        // Check neighbor at (x+1, y)
        if (x < gameMap.getWidth() - 1) {
            // Unified Strategy: Apply Floor-Floor style everywhere per user request
            renderVerticalLine(batch, x, y, unitScale, GROUT_WIDTH_A, baseColor, true);
        }

        // --- 2. Top Edge (North) ---
        // Check neighbor at (x, y+1)
        if (y < gameMap.getHeight() - 1) {
            // Unified Strategy: Apply Floor-Floor style everywhere per user request
            renderHorizontalLine(batch, x, y, unitScale, GROUT_WIDTH_A, baseColor, true);
        }
    }

    private void renderVerticalLine(SpriteBatch batch, int x, int y, float unitScale, float width, Color color,
            boolean use3D) {
        float centerX = (x + 1) * unitScale - (width / 2);
        float drawY = y * unitScale;
        float height = unitScale;

        batch.setColor(color);

        if (use3D) {
            // Use the 3-stripe texture
            batch.draw(verticalGroutTexture, centerX, drawY, width, height);
        } else {
            // Solid line (fallback to whitePixel for flat look)
            batch.draw(textureManager.whitePixel, centerX, drawY, width, height);
        }
    }

    private void renderHorizontalLine(SpriteBatch batch, int x, int y, float unitScale, float height, Color color,
            boolean use3D) {
        float drawX = x * unitScale;
        float centerY = (y + 1) * unitScale - (height / 2);
        float width = unitScale;

        batch.setColor(color);

        if (use3D) {
            // Use the 3-stripe texture
            batch.draw(horizontalGroutTexture, drawX, centerY, width, height);
        } else {
            // Solid line
            batch.draw(textureManager.whitePixel, drawX, centerY, width, height);
        }
    }

    @Override
    public void dispose() {
        if (verticalGroutTexture != null && verticalGroutTexture.getTexture() != null) {
            verticalGroutTexture.getTexture().dispose();
        }
        if (horizontalGroutTexture != null && horizontalGroutTexture.getTexture() != null) {
            horizontalGroutTexture.getTexture().dispose();
        }
    }
}
