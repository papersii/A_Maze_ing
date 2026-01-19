package de.tum.cit.fop.maze.model;

import de.tum.cit.fop.maze.config.GameConfig;
import de.tum.cit.fop.maze.utils.EntityFactory;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import de.tum.cit.fop.maze.custom.CustomElementManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class GameWorldTest {

    // Subclass to bypass Gdx.input and safe path calculation (logic only)
    static class TestGameWorld extends GameWorld {
        public TestGameWorld(GameMap map) {
            super(map, "test_level");
        }

        // Override to prevent Gdx.input usage during update()
        @Override
        protected void handleInput(float delta) {
            // Do nothing during tests unless we want to simulate input
        }

        // Expose player for verification
        public Player getPlayerPublic() {
            return getPlayer();
        }
    }

    @BeforeEach
    public void setup() {
        // Mock Gdx.files if null
        if (Gdx.files == null) {
            Gdx.files = new Files() {
                @Override
                public FileHandle getFileHandle(String path, FileType type) {
                    return new FileHandle(path);
                }

                @Override
                public FileHandle classpath(String path) {
                    return new FileHandle(path);
                }

                @Override
                public FileHandle internal(String path) {
                    return new FileHandle(path);
                }

                @Override
                public FileHandle external(String path) {
                    return new FileHandle(path);
                }

                @Override
                public FileHandle absolute(String path) {
                    return new FileHandle(path);
                }

                @Override
                public FileHandle local(String path) {
                    return new FileHandle(path);
                }

                @Override
                public String getExternalStoragePath() {
                    return "";
                }

                @Override
                public boolean isExternalStorageAvailable() {
                    return true;
                }

                @Override
                public String getLocalStoragePath() {
                    return "";
                }

                @Override
                public boolean isLocalStorageAvailable() {
                    return true;
                }
            };
        }

        // Mock Gdx.app if null
        if (Gdx.app == null) {
            Gdx.app = (Application) java.lang.reflect.Proxy.newProxyInstance(
                    Application.class.getClassLoader(),
                    new Class[] { Application.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("getPreferences")) {
                            return new Preferences() {

                                @Override
                                public Preferences putBoolean(String key, boolean val) {
                                    return this;
                                }

                                @Override
                                public Preferences putInteger(String key, int val) {
                                    return this;
                                }

                                @Override
                                public Preferences putLong(String key, long val) {
                                    return this;
                                }

                                @Override
                                public Preferences putFloat(String key, float val) {
                                    return this;
                                }

                                @Override
                                public Preferences putString(String key, String val) {
                                    return this;
                                }

                                @Override
                                public Preferences put(Map<String, ?> vals) {
                                    return this;
                                }

                                @Override
                                public boolean getBoolean(String key) {
                                    return false;
                                }

                                @Override
                                public int getInteger(String key) {
                                    return 0;
                                }

                                @Override
                                public long getLong(String key) {
                                    return 0;
                                }

                                @Override
                                public float getFloat(String key) {
                                    return 0;
                                }

                                @Override
                                public String getString(String key) {
                                    return "";
                                }

                                @Override
                                public boolean getBoolean(String key, boolean def) {
                                    return def;
                                }

                                @Override
                                public int getInteger(String key, int def) {
                                    return def;
                                }

                                @Override
                                public long getLong(String key, long def) {
                                    return def;
                                }

                                @Override
                                public float getFloat(String key, float def) {
                                    return def;
                                }

                                @Override
                                public String getString(String key, String def) {
                                    return def;
                                }

                                @Override
                                public Map<String, ?> get() {
                                    return new HashMap<>();
                                }

                                @Override
                                public boolean contains(String key) {
                                    return false;
                                }

                                @Override
                                public void clear() {
                                }

                                @Override
                                public void remove(String key) {
                                }

                                @Override
                                public void flush() {
                                }
                            };
                        }
                        if (method.getName().equals("log") || method.getName().equals("error")
                                || method.getName().equals("debug")) {
                            return null;
                        }
                        if (method.getReturnType().equals(int.class))
                            return 0;
                        if (method.getReturnType().equals(boolean.class))
                            return false;
                        return null;
                    });
        }

        // Clear singleton to ensure clean state
        try {
            CustomElementManager.getInstance().clearAll();
        } catch (

        Exception e) {
            // Ignore init errors
        }
    }

    @Test
    public void testInitialization() {
        GameMap map = new GameMap();
        map.setPlayerStart(5, 5);

        GameWorld world = new TestGameWorld(map);

        assertNotNull(world.getPlayer());
        assertEquals(5, world.getPlayer().getX());
        assertEquals(5, world.getPlayer().getY());
        assertEquals(0, world.getEnemies().size());
    }

    @Test
    public void testEnemyLoading() {
        GameMap map = new GameMap();
        map.setPlayerStart(0, 0);
        map.addGameObject(EntityFactory.createEntity(GameConfig.OBJECT_ID_ENEMY, 10, 10));

        GameWorld world = new TestGameWorld(map);

        assertEquals(1, world.getEnemies().size());
        assertEquals(10, world.getEnemies().get(0).getX());
    }
}
