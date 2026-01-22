package de.tum.cit.fop.maze.config;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Clipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GameSettingsAttackRangeTest {

    @BeforeEach
    void setUp() {
        Gdx.app = new HeadlessApplicationStub();
    }

    @Test
    void testDefaultValues() {
        GameSettings.resetUserDefaultsToHardcoded();
        assertTrue(GameSettings.isShowAttackRange(), "Attack range should be enabled by default");
    }

    @Test
    void testToggleSetting() {
        GameSettings.setShowAttackRange(false);
        assertFalse(GameSettings.isShowAttackRange(), "Should be disabled");
        
        GameSettings.setShowAttackRange(true);
        assertTrue(GameSettings.isShowAttackRange(), "Should be enabled");
    }

    @Test
    void testPersistence() {
        // 1. Save "false"
        GameSettings.setShowAttackRange(false);
        GameSettings.saveAsUserDefaults();
        
        // 2. Reset to "true" (simulating a fresh start or default)
        // Switch to true to ensure we are actually loading the saved "false"
        GameSettings.setShowAttackRange(true); 
        
        // 3. Load
        GameSettings.loadUserDefaults();
        assertFalse(GameSettings.isShowAttackRange(), "Should load 'false' from preferences");
        
        // 4. Save "true" to restore
        GameSettings.setShowAttackRange(true);
        GameSettings.saveAsUserDefaults();
        GameSettings.loadUserDefaults();
        assertTrue(GameSettings.isShowAttackRange(), "Should load 'true' from preferences");
    }

    // Stub implementations
    static class HeadlessApplicationStub implements Application {
        private final Map<String, Preferences> prefsMap = new HashMap<>();

        @Override
        public Preferences getPreferences(String name) {
            return prefsMap.computeIfAbsent(name, k -> new PreferencesStub());
        }

        @Override public ApplicationListener getApplicationListener() { return null; }
        @Override public Graphics getGraphics() { return null; }
        @Override public Audio getAudio() { return null; }
        @Override public Input getInput() { return null; }
        @Override public Files getFiles() { return null; }
        @Override public Net getNet() { return null; }
        @Override public void log(String tag, String message) {}
        @Override public void log(String tag, String message, Throwable exception) {}
        @Override public void error(String tag, String message) {}
        @Override public void error(String tag, String message, Throwable exception) {}
        @Override public void debug(String tag, String message) {}
        @Override public void debug(String tag, String message, Throwable exception) {}
        @Override public void setLogLevel(int logLevel) {}
        @Override public int getLogLevel() { return 0; }
        @Override public void setApplicationLogger(com.badlogic.gdx.ApplicationLogger applicationLogger) {}
        @Override public com.badlogic.gdx.ApplicationLogger getApplicationLogger() { return null; }
        @Override public ApplicationType getType() { return ApplicationType.HeadlessDesktop; }
        @Override public int getVersion() { return 0; }
        @Override public long getJavaHeap() { return 0; }
        @Override public long getNativeHeap() { return 0; }
        @Override public void postRunnable(Runnable runnable) { runnable.run(); }
        @Override public void exit() {}
        @Override public void addLifecycleListener(LifecycleListener listener) {}
        @Override public void removeLifecycleListener(LifecycleListener listener) {}
        @Override public Clipboard getClipboard() { return null; }
    }

    static class PreferencesStub implements Preferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override public Preferences putBoolean(String key, boolean val) { values.put(key, val); return this; }
        @Override public Preferences putInteger(String key, int val) { values.put(key, val); return this; }
        @Override public Preferences putLong(String key, long val) { values.put(key, val); return this; }
        @Override public Preferences putFloat(String key, float val) { values.put(key, val); return this; }
        @Override public Preferences putString(String key, String val) { values.put(key, val); return this; }
        @Override public Preferences put(Map<String, ?> vals) { values.putAll(vals); return this; }
        @Override public boolean getBoolean(String key) { return getBoolean(key, false); }
        @Override public int getInteger(String key) { return getInteger(key, 0); }
        @Override public long getLong(String key) { return getLong(key, 0); }
        @Override public float getFloat(String key) { return getFloat(key, 0); }
        @Override public String getString(String key) { return getString(key, ""); }
        @Override public boolean getBoolean(String key, boolean defValue) { Object v = values.get(key); return v instanceof Boolean ? (Boolean) v : defValue; }
        @Override public int getInteger(String key, int defValue) { Object v = values.get(key); return v instanceof Integer ? (Integer) v : defValue; }
        @Override public long getLong(String key, long defValue) { Object v = values.get(key); return v instanceof Long ? (Long) v : defValue; }
        @Override public float getFloat(String key, float defValue) { Object v = values.get(key); return v instanceof Float ? (Float) v : defValue; }
        @Override public String getString(String key, String defValue) { Object v = values.get(key); return v instanceof String ? (String) v : defValue; }
        @Override public Map<String, ?> get() { return values; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public void clear() { values.clear(); }
        @Override public void remove(String key) { values.remove(key); }
        @Override public void flush() {}
    }
}
