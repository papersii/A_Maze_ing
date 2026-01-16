package de.tum.cit.fop.maze.tools;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import de.tum.cit.fop.maze.model.DamageType;
import de.tum.cit.fop.maze.utils.MapGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试类：使用修复后的 MapGenerator 重新生成所有 20 个关卡地图。
 * 
 * 关卡主题分配：
 * - Levels 1-4: Grassland
 * - Levels 5-8: Desert
 * - Levels 9-12: Ice
 * - Levels 13-16: Jungle
 * - Levels 17-20: Space
 * 
 * 运行方式：./gradlew :core:test --tests "LevelMapGeneratorTest.generateAllLevels"
 */
public class LevelMapGeneratorTest {

    private static final String OUTPUT_DIR = "assets/maps/";
    private static final String[] THEMES = { "Grassland", "Desert", "Ice", "Jungle", "Space" };

    @BeforeAll
    static void setup() {
        // Mock Gdx.files
        Gdx.files = new MockFiles();
        Gdx.app = new MockApplication();
    }

    @Test
    void generateAllLevels() {
        System.out.println("=== Level Map Generator ===");
        System.out.println("Generating 20 level maps with validated paths...\n");

        int successCount = 0;
        for (int level = 1; level <= 20; level++) {
            try {
                generateLevel(level);
                successCount++;
            } catch (Exception e) {
                System.err.printf("✗ Level %2d: FAILED - %s%n", level, e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=== Generation complete: " + successCount + "/20 levels ===");
        assertEquals(20, successCount, "All 20 levels should be generated successfully");
    }

    private void generateLevel(int level) {
        // 确定主题 (每4关一个主题)
        int themeIndex = (level - 1) / 4;
        String theme = THEMES[themeIndex];

        // 确定难度
        int localDifficulty = ((level - 1) % 4) + 1;
        int globalDifficultyBonus = themeIndex;
        int difficulty = Math.min(5, localDifficulty + globalDifficultyBonus / 2);

        // 确定地图大小 (50-200，随关卡增加)
        int mapSize = 50 + (level - 1) * 8;
        mapSize = Math.min(200, Math.max(50, mapSize));

        // 确定伤害类型
        DamageType damageType = (level % 2 == 0) ? DamageType.MAGICAL : DamageType.PHYSICAL;

        // 创建配置
        MapGenerator.MapConfig config = new MapGenerator.MapConfig();
        config.width = mapSize;
        config.height = mapSize;
        config.difficulty = difficulty;
        config.theme = theme;
        config.damageType = damageType;
        config.enemyShieldEnabled = (level >= 9);
        config.enemyDensity = 0.5f + level * 0.05f;
        config.trapDensity = 0.3f + level * 0.04f;
        config.mobileTrapDensity = 0.2f + level * 0.03f;
        config.lootDropRate = 1.5f - level * 0.03f;
        config.braidChance = 0.5f - level * 0.02f;
        config.roomCount = 20 + level * 2;

        // 生成地图
        String fileName = OUTPUT_DIR + "level-" + level + ".properties";
        MapGenerator generator = new MapGenerator(config);
        generator.generateAndSave(fileName);

        System.out.printf("✓ Level %2d: %s | %dx%d | Difficulty: %d | %s%n",
                level, theme, mapSize, mapSize, difficulty, damageType.name());

        // 验证文件存在
        File file = new File(fileName);
        assertTrue(file.exists(), "Level " + level + " map file should exist");
    }

    // === Mock implementations ===

    private static class MockFiles implements Files {
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
        }

        @Override
        public void error(String tag, String message) {
            System.err.println("[ERROR " + tag + "] " + message);
        }

        @Override
        public void error(String tag, String message, Throwable exception) {
            System.err.println("[ERROR " + tag + "] " + message);
        }

        @Override
        public void debug(String tag, String message) {
        }

        @Override
        public void debug(String tag, String message, Throwable exception) {
        }

        @Override
        public int getLogLevel() {
            return LOG_INFO;
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
            return 0;
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
        public Files getFiles() {
            return new MockFiles();
        }

        @Override
        public com.badlogic.gdx.Net getNet() {
            return null;
        }
    }
}
