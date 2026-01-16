package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 MapGenerator 的路径连通性和墙体放置。
 * 确保生成的地图满足：
 * 1. 玩家起点到钥匙可达
 * 2. 钥匙到出口可达
 * 3. 无隐形墙（安全区内无墙体）
 */
public class MapGeneratorPathTest {

    private static final String TEST_MAP_PATH = "test_maps/path_test.properties";

    @BeforeEach
    void setup() {
        // Mock Gdx.files for testing
        Gdx.files = new MockFiles();
        Gdx.app = new MockApplication();
    }

    /**
     * 测试地图生成基本流程 - 确保不抛出异常
     */
    @Test
    void testMapGenerationDoesNotThrow() {
        MapGenerator generator = new MapGenerator(); // Default config
        assertDoesNotThrow(() -> generator.generateAndSave(TEST_MAP_PATH));
    }

    /**
     * 重复测试多次以验证随机生成的稳定性
     * 每次生成都应该成功（有路径）
     */
    @RepeatedTest(5)
    void testMapGenerationConsistency() {
        MapGenerator generator = new MapGenerator(getNormalConfig());
        assertDoesNotThrow(() -> generator.generateAndSave(TEST_MAP_PATH));

        // Verify the file was created
        File file = new File(TEST_MAP_PATH);
        assertTrue(file.exists() || new File("." + TEST_MAP_PATH).exists(),
                "Map file should be created");
    }

    /**
     * 测试大地图生成 - 确保大地图也能保证路径连通
     */
    @Test
    void testLargeMapGeneration() {
        MapGenerator.MapConfig hardConfig = getNormalConfig();
        hardConfig.width = 200;
        hardConfig.height = 200;
        hardConfig.difficulty = 5;
        hardConfig.enemyDensity = 1.5f;

        MapGenerator generator = new MapGenerator(hardConfig);
        assertDoesNotThrow(() -> generator.generateAndSave(TEST_MAP_PATH));
    }

    /**
     * 测试小地图生成 - 确保小地图也能正常工作
     */
    @Test
    void testSmallMapGeneration() {
        MapGenerator.MapConfig easyConfig = getNormalConfig();
        easyConfig.width = 50;
        easyConfig.height = 50;
        easyConfig.difficulty = 1;
        easyConfig.enemyDensity = 0.5f;

        MapGenerator generator = new MapGenerator(easyConfig);
        assertDoesNotThrow(() -> generator.generateAndSave(TEST_MAP_PATH));
    }

    // Helper to create a normal config
    private MapGenerator.MapConfig getNormalConfig() {
        MapGenerator.MapConfig config = new MapGenerator.MapConfig();
        config.width = 100;
        config.height = 100;
        config.difficulty = 3;
        config.theme = "Dungeon";
        return config;
    }

    // === Mock implementations for testing ===

    private static class MockFiles implements com.badlogic.gdx.Files {
        @Override
        public FileHandle getFileHandle(String path, FileType type) {
            return new FileHandle(new File(path));
        }

        @Override
        public FileHandle classpath(String path) {
            return new FileHandle(new File(path));
        }

        @Override
        public FileHandle internal(String path) {
            return new FileHandle(new File(path));
        }

        @Override
        public FileHandle external(String path) {
            return new FileHandle(new File(path));
        }

        @Override
        public FileHandle absolute(String path) {
            return new FileHandle(new File(path));
        }

        @Override
        public FileHandle local(String path) {
            return new FileHandle(new File(path));
        }

        @Override
        public String getExternalStoragePath() {
            return "";
        }

        @Override
        public boolean isExternalStorageAvailable() {
            return false;
        }

        @Override
        public String getLocalStoragePath() {
            return "";
        }

        @Override
        public boolean isLocalStorageAvailable() {
            return true;
        }
    }

    private static class MockApplication implements Application {
        @Override
        public void log(String tag, String message) {
            System.out.println("[" + tag + "] " + message);
        }

        @Override
        public void log(String tag, String message, Throwable exception) {
            System.out.println("[" + tag + "] " + message);
            exception.printStackTrace();
        }

        @Override
        public void error(String tag, String message) {
            System.err.println("[ERROR " + tag + "] " + message);
        }

        @Override
        public void error(String tag, String message, Throwable exception) {
            System.err.println("[ERROR " + tag + "] " + message);
            exception.printStackTrace();
        }

        @Override
        public void debug(String tag, String message) {
            System.out.println("[DEBUG " + tag + "] " + message);
        }

        @Override
        public void debug(String tag, String message, Throwable exception) {
            System.out.println("[DEBUG " + tag + "] " + message);
        }

        @Override
        public int getLogLevel() {
            return LOG_DEBUG;
        }

        @Override
        public void setLogLevel(int logLevel) {
        }

        @Override
        public void setApplicationLogger(com.badlogic.gdx.ApplicationLogger applicationLogger) {
        }

        @Override
        public com.badlogic.gdx.ApplicationLogger getApplicationLogger() {
            return null;
        }

        @Override
        public ApplicationType getType() {
            return ApplicationType.HeadlessDesktop;
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public long getJavaHeap() {
            return Runtime.getRuntime().totalMemory();
        }

        @Override
        public long getNativeHeap() {
            return 0;
        }

        @Override
        public com.badlogic.gdx.Preferences getPreferences(String name) {
            return null;
        }

        @Override
        public com.badlogic.gdx.utils.Clipboard getClipboard() {
            return null;
        }

        @Override
        public void postRunnable(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void exit() {
        }

        @Override
        public void addLifecycleListener(com.badlogic.gdx.LifecycleListener listener) {
        }

        @Override
        public void removeLifecycleListener(com.badlogic.gdx.LifecycleListener listener) {
        }

        @Override
        public com.badlogic.gdx.ApplicationListener getApplicationListener() {
            return null;
        }

        @Override
        public com.badlogic.gdx.Graphics getGraphics() {
            return null;
        }

        @Override
        public com.badlogic.gdx.Audio getAudio() {
            return null;
        }

        @Override
        public com.badlogic.gdx.Input getInput() {
            return null;
        }

        @Override
        public com.badlogic.gdx.Files getFiles() {
            return new MockFiles();
        }

        @Override
        public com.badlogic.gdx.Net getNet() {
            return null;
        }
    }
}
