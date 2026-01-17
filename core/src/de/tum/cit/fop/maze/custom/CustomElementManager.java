package de.tum.cit.fop.maze.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.utils.GameLogger;

import java.util.*;

/**
 * Singleton manager for custom game elements.
 * Handles saving, loading, and querying custom element definitions.
 */
public class CustomElementManager {

    private static CustomElementManager instance;

    private static final String SAVE_DIR = ".maze_runner/custom_elements/";
    private static final String ELEMENTS_FILE = "elements.json";

    private Map<String, CustomElementDefinition> elements;
    private Json json;

    private CustomElementManager() {
        elements = new HashMap<>();
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        loadElements();
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
    public void saveElement(CustomElementDefinition element) {
        elements.put(element.getId(), element);
        persistToFile();
        GameLogger.info("CustomElementManager", "Saved element: " + element.getName());
    }

    /**
     * Delete an element
     */
    public void deleteElement(String id) {
        CustomElementDefinition removed = elements.remove(id);
        if (removed != null) {
            persistToFile();
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
     * Get all elements
     */
    public Collection<CustomElementDefinition> getAllElements() {
        return elements.values();
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
            FileHandle file = Gdx.files.external(SAVE_DIR + ELEMENTS_FILE);
            if (file.exists()) {
                String jsonStr = file.readString();
                CustomElementDefinition[] loaded = json.fromJson(CustomElementDefinition[].class, jsonStr);
                if (loaded != null) {
                    for (CustomElementDefinition element : loaded) {
                        elements.put(element.getId(), element);
                    }
                }
                GameLogger.info("CustomElementManager", "Loaded " + elements.size() + " custom elements");
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
            FileHandle dir = Gdx.files.external(SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileHandle file = Gdx.files.external(SAVE_DIR + ELEMENTS_FILE);
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
        return Gdx.files.external(SAVE_DIR + "sprites/" + elementId + "/");
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
     * Clear all elements (for testing)
     */
    public void clearAll() {
        elements.clear();
        persistToFile();
    }
}
