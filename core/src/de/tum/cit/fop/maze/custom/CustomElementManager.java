package de.tum.cit.fop.maze.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.utils.GameLogger;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

import java.util.*;

/**
 * Singleton manager for custom game elements.
 * Handles saving, loading, and querying custom element definitions.
 */
public class CustomElementManager {

    private static CustomElementManager instance;

    private static final String SAVE_DIR = "custom_elements/";
    private static final String LOCAL_IMAGE_DIR = "custom_images/";
    private static final String ELEMENTS_FILE = "elements.json";

    private Map<String, CustomElementDefinition> elements;
    private Json json;

    // Cache for loaded animations: Key = "elementId:action"
    private Map<String, Animation<TextureRegion>> animationCache;

    private CustomElementManager() {
        elements = new HashMap<>();
        animationCache = new HashMap<>();
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        loadElements();
        initializeDefaults();
    }

    public static CustomElementManager getInstance() {
        if (instance == null) {
            instance = new CustomElementManager();
        }
        return instance;
    }

    /**
     * Add or update an element
     */
    /**
     * Add or update an element.
     * Copies sprites to local storage ("image" folder) if they are external.
     */
    public void saveElement(CustomElementDefinition element) {
        // Process sprite paths to localize them
        FileHandle localImgDir = Gdx.files.local(LOCAL_IMAGE_DIR + element.getId() + "/");
        if (!localImgDir.exists()) {
            localImgDir.mkdirs();
        }

        Map<String, String[]> spritePaths = element.getSpritePaths();
        for (Map.Entry<String, String[]> entry : spritePaths.entrySet()) {
            String action = entry.getKey();
            String[] paths = entry.getValue();
            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];
                if (path != null && !path.isEmpty() && !path.startsWith("internal:")) {
                    // check if already local
                    if (!path.startsWith(LOCAL_IMAGE_DIR)) {
                        try {
                            FileHandle source = Gdx.files.absolute(path);
                            if (source.exists()) {
                                String fileName = action.toLowerCase() + "_" + i + ".png";
                                FileHandle dest = localImgDir.child(fileName);
                                source.copyTo(dest);
                                // Update path to relative local path (Force relative)
                                String relPath = LOCAL_IMAGE_DIR + element.getId() + "/" + fileName;
                                element.setSpritePath(action, i, relPath);
                            }
                        } catch (Exception e) {
                            GameLogger.error("CustomElementManager", "Failed to localize sprite: " + path);
                        }
                    }
                }
            }
        }

        elements.put(element.getId(), element);
        persistToFile();
        GameLogger.info("CustomElementManager", "Saved element: " + element.getName());
    }

    /**
     * Get all elements
     */
    public java.util.Collection<CustomElementDefinition> getAllElements() {
        return elements.values();
    }

    /**
     * Find an element definition by name (case-insensitive)
     */
    public CustomElementDefinition getElementByName(String name) {
        for (CustomElementDefinition def : elements.values()) {
            if (def.getName().equalsIgnoreCase(name)) {
                return def;
            }
        }
        return null;
    }

    /**
     * Delete an element
     */
    public void deleteElement(String id) {
        CustomElementDefinition removed = elements.remove(id);
        if (removed != null) {
            persistToFile();

            // Cleanup local images
            try {
                FileHandle localImgDir = Gdx.files.local(LOCAL_IMAGE_DIR + id + "/");
                if (localImgDir.exists()) {
                    localImgDir.deleteDirectory();
                }
            } catch (Exception e) {
                GameLogger.error("CustomElementManager", "Failed to delete sprite dir: " + e.getMessage());
            }

            GameLogger.info("CustomElementManager", "Deleted element: " + removed.getName());
        }
    }

    /**
     * Get element by ID
     */
    public CustomElementDefinition getElement(String id) {
        return elements.get(id);
    }

    /**
     * Get elements by type
     */
    public List<CustomElementDefinition> getElementsByType(ElementType type) {
        List<CustomElementDefinition> result = new ArrayList<>();
        for (CustomElementDefinition element : elements.values()) {
            if (element.getType() == type) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Get elements assigned to a specific level
     */
    public List<CustomElementDefinition> getElementsForLevel(int level) {
        List<CustomElementDefinition> result = new ArrayList<>();
        for (CustomElementDefinition element : elements.values()) {
            if (element.isAssignedToLevel(level) && element.isComplete()) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Get elements by type for a specific level
     */
    public List<CustomElementDefinition> getElementsForLevel(int level, ElementType type) {
        List<CustomElementDefinition> result = new ArrayList<>();
        for (CustomElementDefinition element : elements.values()) {
            if (element.getType() == type && element.isAssignedToLevel(level) && element.isComplete()) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Load elements from disk
     */
    @SuppressWarnings("unchecked")
    private void loadElements() {
        try {
            FileHandle file = Gdx.files.local(SAVE_DIR + ELEMENTS_FILE);
            if (file.exists()) {
                String jsonStr = file.readString();
                CustomElementDefinition[] loaded = json.fromJson(CustomElementDefinition[].class, jsonStr);
                if (loaded != null) {
                    for (CustomElementDefinition element : loaded) {
                        elements.put(element.getId(), element);
                        GameLogger.info("CustomElementManager",
                                "Loaded element: " + element.getName() + " [" + element.getId() + "]");
                    }
                }
                GameLogger.info("CustomElementManager", "Total loaded: " + elements.size());
            } else {
                GameLogger.info("CustomElementManager", "No custom elements file found at: " + file.path());
            }
        } catch (Exception e) {
            GameLogger.error("CustomElementManager", "Failed to load elements: " + e.getMessage());
        }
    }

    /**
     * Save all elements to disk
     */
    private void persistToFile() {
        try {
            FileHandle dir = Gdx.files.local(SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileHandle file = Gdx.files.local(SAVE_DIR + ELEMENTS_FILE);
            CustomElementDefinition[] array = elements.values().toArray(new CustomElementDefinition[0]);
            String jsonStr = json.prettyPrint(array);
            file.writeString(jsonStr, false);
            GameLogger.info("CustomElementManager", "Persisted " + elements.size() + " elements to disk");
        } catch (Exception e) {
            GameLogger.error("CustomElementManager", "Failed to save elements: " + e.getMessage());
        }
    }

    /**
     * Get the sprite storage directory for an element
     */
    public FileHandle getSpriteDir(String elementId) {
        return Gdx.files.local(SAVE_DIR + "sprites/" + elementId + "/");
    }

    /**
     * Copy a sprite file to the element's sprite directory
     */
    public String copySprite(String elementId, String action, int frameIndex, FileHandle sourceFile) {
        try {
            FileHandle spriteDir = getSpriteDir(elementId);
            if (!spriteDir.exists()) {
                spriteDir.mkdirs();
            }

            String fileName = action.toLowerCase() + "_" + frameIndex + ".png";
            FileHandle destFile = spriteDir.child(fileName);
            sourceFile.copyTo(destFile);

            return destFile.path();
        } catch (Exception e) {
            GameLogger.error("CustomElementManager", "Failed to copy sprite: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the number of custom elements
     */
    public int getElementCount() {
        return elements.size();
    }

    /**
     * Creates a TextureRegion that pads non-square textures to square with
     * transparency.
     * Preserves all original content without cropping.
     */
    private TextureRegion createCroppedRegion(Texture tex) {
        int w = tex.getWidth();
        int h = tex.getHeight();

        if (w == h) {
            return new TextureRegion(tex); // Already square
        }

        // Pad to square (use larger dimension)
        int size = Math.max(w, h);

        // Read original texture data
        if (!tex.getTextureData().isPrepared()) {
            tex.getTextureData().prepare();
        }
        com.badlogic.gdx.graphics.Pixmap original = tex.getTextureData().consumePixmap();

        // Create new square pixmap with transparency
        com.badlogic.gdx.graphics.Pixmap padded = new com.badlogic.gdx.graphics.Pixmap(
                size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        padded.setColor(0, 0, 0, 0); // Transparent
        padded.fill();

        // Center the original image
        int offsetX = (size - w) / 2;
        int offsetY = (size - h) / 2;
        padded.drawPixmap(original, offsetX, offsetY);

        // Create new texture from padded pixmap
        Texture paddedTex = new Texture(padded);

        // Cleanup
        original.dispose();
        padded.dispose();

        GameLogger.info("CustomElementManager",
                "Auto-padding texture from " + w + "x" + h + " to " + size + "x" + size);
        return new TextureRegion(paddedTex);
    }

    /**
     * Get animation for a custom element action.
     * Loads textures on demand and caches them.
     */
    public Animation<TextureRegion> getAnimation(String elementId, String action) {
        String key = elementId + ":" + action;
        if (animationCache.containsKey(key)) {
            return animationCache.get(key);
        }

        CustomElementDefinition def = getElement(elementId);
        if (def == null)
            return null;

        String[] paths = def.getSpritePaths().get(action);
        if (paths == null)
            return null;

        Array<TextureRegion> frames = new Array<>();
        for (String path : paths) {
            if (path == null || path.isEmpty())
                continue;
            try {
                // AUTO-FIX: If path is absolute (from another PC) but contains "custom_images",
                // make it relative
                if (path.contains(LOCAL_IMAGE_DIR) && (path.contains("/") || path.contains("\\"))) {
                    int idx = path.indexOf(LOCAL_IMAGE_DIR);
                    if (idx > 0) {
                        String fixedPath = path.substring(idx);
                        // GameLogger.info("Fixed path: " + path + " -> " + fixedPath);
                        path = fixedPath;
                    }
                }

                FileHandle file = null;

                // 1. Try Local Storage (Preferred for custom items)
                if (path.startsWith(LOCAL_IMAGE_DIR)) {
                    file = Gdx.files.local(path);
                }

                // 2. Try Absolute
                if (file == null || !file.exists()) {
                    file = Gdx.files.absolute(path);
                }

                // 3. Try Internal
                if (!file.exists()) {
                    file = Gdx.files.internal(path);
                }

                // 4. Fallback: Try local again if not tried (e.g. if path didn't start with
                // prefix)
                if (!file.exists()) {
                    file = Gdx.files.local(path);
                }

                if (file.exists()) {
                    Texture tex = new Texture(file);
                    frames.add(createCroppedRegion(tex));
                }
            } catch (Exception e) {
                GameLogger.error("CustomElementManager", "Failed to load texture for " + elementId + ": " + path);
            }
        }

        if (frames.size > 0) {
            Animation.PlayMode mode = action.equalsIgnoreCase("Death") ? Animation.PlayMode.NORMAL
                    : Animation.PlayMode.LOOP;
            Animation<TextureRegion> anim = new Animation<>(0.15f, frames, mode);
            animationCache.put(key, anim);
            return anim;
        }

        return null;
    }

    public void clearAll() {
        elements.clear();
        animationCache.clear();
        persistToFile();
    }

    private void initializeDefaults() {
        // Standard Slime (Level 1)
        if (!elements.containsKey("default_slime")) {
            CustomElementDefinition slime = new CustomElementDefinition("Standard Slime", ElementType.ENEMY, 4);
            slime.setId("default_slime");
            slime.setSpawnCount(10);
            slime.assignToLevel(1);
            slime.setProperty("health", 2);
            slime.setProperty("moveSpeed", 2.0f);
            slime.setProperty("enemyType", "SLIME");
            for (int i = 0; i < 4; i++) {
                slime.setSpritePath("Move", i, "internal:default");
                slime.setSpritePath("Death", i, "internal:default");
            }
            saveElement(slime);
        }

        // Standard Boar (Level 2)
        if (!elements.containsKey("default_boar")) {
            CustomElementDefinition boar = new CustomElementDefinition("Standard Boar", ElementType.ENEMY, 4);
            boar.setId("default_boar");
            boar.setSpawnCount(8);
            boar.assignToLevel(2);
            // Also assign to Level 1 for variety if desired, but sticking to 2 for
            // progression
            boar.setProperty("health", 5);
            boar.setProperty("moveSpeed", 3.0f);
            boar.setProperty("enemyType", "BOAR");
            for (int i = 0; i < 4; i++) {
                boar.setSpritePath("Move", i, "internal:default");
                boar.setSpritePath("Death", i, "internal:default");
            }
            saveElement(boar);
        }
    }
}
