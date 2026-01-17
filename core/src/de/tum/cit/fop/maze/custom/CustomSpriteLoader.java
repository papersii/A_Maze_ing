package de.tum.cit.fop.maze.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.utils.GameLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches sprites for custom elements.
 */
public class CustomSpriteLoader {

    private static CustomSpriteLoader instance;

    // Cache: elementId -> action -> Animation
    private Map<String, Map<String, Animation<TextureRegion>>> animationCache;
    // Keep track of loaded textures for disposal
    private Map<String, Texture> textureCache;

    private static final float FRAME_DURATION = 0.15f;

    private CustomSpriteLoader() {
        animationCache = new HashMap<>();
        textureCache = new HashMap<>();
    }

    public static CustomSpriteLoader getInstance() {
        if (instance == null) {
            instance = new CustomSpriteLoader();
        }
        return instance;
    }

    /**
     * Load or get cached animation for a custom element action
     */
    public Animation<TextureRegion> getAnimation(CustomElementDefinition element, String action) {
        String elementId = element.getId();

        // Check cache
        if (animationCache.containsKey(elementId)) {
            Map<String, Animation<TextureRegion>> actions = animationCache.get(elementId);
            if (actions.containsKey(action)) {
                return actions.get(action);
            }
        }

        // Load animation
        Animation<TextureRegion> anim = loadAnimation(element, action);

        // Cache it
        if (!animationCache.containsKey(elementId)) {
            animationCache.put(elementId, new HashMap<>());
        }
        animationCache.get(elementId).put(action, anim);

        return anim;
    }

    /**
     * Load animation from sprite files
     */
    private Animation<TextureRegion> loadAnimation(CustomElementDefinition element, String action) {
        int frameCount = element.getFrameCount();
        TextureRegion[] frames = new TextureRegion[frameCount];

        for (int i = 0; i < frameCount; i++) {
            String path = element.getSpritePath(action, i);
            if (path != null && !path.isEmpty()) {
                try {
                    FileHandle file = Gdx.files.external(path);
                    if (!file.exists()) {
                        file = Gdx.files.internal(path);
                    }
                    if (file.exists()) {
                        String cacheKey = element.getId() + "_" + action + "_" + i;
                        Texture tex = new Texture(file);
                        textureCache.put(cacheKey, tex);
                        frames[i] = new TextureRegion(tex);
                    } else {
                        GameLogger.warn("CustomSpriteLoader", "Sprite not found: " + path);
                        frames[i] = createPlaceholder();
                    }
                } catch (Exception e) {
                    GameLogger.error("CustomSpriteLoader", "Failed to load sprite: " + path);
                    frames[i] = createPlaceholder();
                }
            } else {
                frames[i] = createPlaceholder();
            }
        }

        return new Animation<>(FRAME_DURATION, frames);
    }

    /**
     * Create a placeholder texture for missing sprites
     */
    private TextureRegion createPlaceholder() {
        // Use a simple colored placeholder
        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(32, 32,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pm.setColor(1f, 0f, 1f, 1f); // Magenta for missing
        pm.fill();
        pm.setColor(0f, 0f, 0f, 1f);
        pm.drawRectangle(0, 0, 32, 32);
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegion(tex);
    }

    /**
     * Preload all sprites for an element
     */
    public void preloadElement(CustomElementDefinition element) {
        for (String action : element.getType().getActions()) {
            getAnimation(element, action);
        }
    }

    /**
     * Clear cache for a specific element (when element is deleted)
     */
    public void clearCache(String elementId) {
        animationCache.remove(elementId);
        // Dispose textures for this element
        textureCache.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(elementId)) {
                entry.getValue().dispose();
                return true;
            }
            return false;
        });
    }

    /**
     * Dispose all cached textures
     */
    public void dispose() {
        for (Texture tex : textureCache.values()) {
            tex.dispose();
        }
        textureCache.clear();
        animationCache.clear();
    }
}
