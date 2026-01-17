package de.tum.cit.fop.maze.custom;

import java.util.*;

/**
 * Definition of a custom game element created by the user.
 * Contains all data needed to spawn and render the element in-game.
 */
public class CustomElementDefinition {

    private String id;
    private String name;
    private ElementType type;
    private int frameCount; // 4 or 16
    private Map<String, String[]> spritePaths; // action -> frame paths
    private Map<String, Object> properties;
    private Set<Integer> assignedLevels;
    private float spawnProbability; // 0.0 - 1.0

    public CustomElementDefinition() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.spritePaths = new HashMap<>();
        this.properties = new HashMap<>();
        this.assignedLevels = new HashSet<>();
        this.spawnProbability = 0.5f;
    }

    public CustomElementDefinition(String name, ElementType type, int frameCount) {
        this();
        this.name = name;
        this.type = type;
        this.frameCount = frameCount;

        // Initialize default properties
        for (String prop : type.getRequiredProperties()) {
            properties.put(prop, type.getDefaultValue(prop));
        }

        // Initialize sprite path arrays for each action
        for (String action : type.getActions()) {
            spritePaths.put(action, new String[frameCount]);
        }
    }

    // === Getters & Setters ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ElementType getType() {
        return type;
    }

    public void setType(ElementType type) {
        this.type = type;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public Map<String, String[]> getSpritePaths() {
        return spritePaths;
    }

    public void setSpritePath(String action, int frameIndex, String path) {
        if (spritePaths.containsKey(action) && frameIndex < spritePaths.get(action).length) {
            spritePaths.get(action)[frameIndex] = path;
        }
    }

    public String getSpritePath(String action, int frameIndex) {
        if (spritePaths.containsKey(action) && frameIndex < spritePaths.get(action).length) {
            return spritePaths.get(action)[frameIndex];
        }
        return null;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null)
            return null;
        return (T) value;
    }

    public int getIntProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public float getFloatProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0f;
    }

    public boolean getBoolProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    public Set<Integer> getAssignedLevels() {
        return assignedLevels;
    }

    public void assignToLevel(int level) {
        assignedLevels.add(level);
    }

    public void removeFromLevel(int level) {
        assignedLevels.remove(level);
    }

    public boolean isAssignedToLevel(int level) {
        return assignedLevels.contains(level);
    }

    public float getSpawnProbability() {
        return spawnProbability;
    }

    public void setSpawnProbability(float probability) {
        this.spawnProbability = Math.max(0f, Math.min(1f, probability));
    }

    /**
     * Check if all sprites are uploaded for this element
     */
    public boolean isComplete() {
        for (String action : type.getActions()) {
            String[] paths = spritePaths.get(action);
            if (paths == null)
                return false;
            for (String path : paths) {
                if (path == null || path.isEmpty())
                    return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "CustomElementDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", frameCount=" + frameCount +
                ", assignedLevels=" + assignedLevels +
                '}';
    }
}
