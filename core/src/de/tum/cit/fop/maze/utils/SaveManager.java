package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.model.GameState;

import java.util.Arrays;
import java.util.Comparator;

/*
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  ⚠️  CORE SYSTEM FILE - DO NOT MODIFY WITHOUT TEAM LEAD APPROVAL ⚠️      ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  This file implements the SAVE/LOAD system using LibGDX JSON:             ║
 * ║  • Saves game state as human-readable JSON to local storage               ║
 * ║  • Loads and deserializes GameState objects                               ║
 * ║  • Lists and sorts save files by modification date                        ║
 * ║                                                                           ║
 * ║  CRITICAL: The JSON format must match GameState.java fields exactly.      ║
 * ║  If you add fields to GameState, they auto-serialize. Removing fields     ║
 * ║  will break loading of old saves.                                         ║
 * ║                                                                           ║
 * ║  DO NOT CHANGE:                                                           ║
 * ║  - SAVE_DIR path (breaks existing user saves)                             ║
 * ║  - Method signatures (used by GameScreen)                                 ║
 * ║  - JSON output format (JsonWriter.OutputType.json)                        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */

public class SaveManager {

    // 所有的存档都放在 saves 文件夹下
    private static final String SAVE_DIR = "saves/";

    /**
     * 保存游戏，允许指定文件名
     * 
     * @param state    游戏状态
     * @param filename 用户输入的文件名 (不需要带 .json 后缀)
     */
    public static void saveGame(GameState state, String filename) {
        Json json = new Json();
        // 关键设置：输出标准的 JSON 格式
        json.setOutputType(JsonWriter.OutputType.json);

        // 确保文件夹存在
        if (!Gdx.files.local(SAVE_DIR).exists()) {
            Gdx.files.local(SAVE_DIR).mkdirs();
        }

        // 自动加上 .json 后缀
        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        String text = json.prettyPrint(state);
        FileHandle file = Gdx.files.local(SAVE_DIR + filename);
        file.writeString(text, false);

        Gdx.app.log("SaveManager", "Saved to: " + file.path());
    }

    /**
     * 兼容方法：默认保存 (保存为 auto_save.json)
     */
    public static void saveGame(GameState state) {
        saveGame(state, "auto_save");
    }

    /**
     * 读取指定文件名的存档
     */
    public static GameState loadGame(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = "auto_save.json";
        }
        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        FileHandle file = Gdx.files.local(SAVE_DIR + filename);

        if (!file.exists()) {
            Gdx.app.log("SaveManager", "Save file not found: " + filename);
            return null;
        }

        Json json = new Json();
        try {
            return json.fromJson(GameState.class, file.readString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 兼容方法：默认读取 (读取 auto_save.json)
     */
    public static GameState loadGame() {
        return loadGame("auto_save.json");
    }

    /**
     * 获取所有存档文件，并按修改时间倒序排列（最新的在最前面）
     * 供 GameScreen 的读档列表使用
     */
    public static FileHandle[] getSaveFiles() {
        FileHandle dir = Gdx.files.local(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            return new FileHandle[0];
        }

        // 获取所有 .json 结尾的文件
        FileHandle[] files = dir.list(".json");

        // 按时间排序 (最新的排前面)
        Arrays.sort(files, new Comparator<FileHandle>() {
            @Override
            public int compare(FileHandle f1, FileHandle f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        return files;
    }

    public static boolean deleteSave(String filename) {
        if (filename == null || filename.isEmpty())
            return false;

        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        FileHandle file = Gdx.files.local(SAVE_DIR + filename);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Gdx.app.log("SaveManager", "Deleted save file: " + filename);
            } else {
                Gdx.app.error("SaveManager", "Failed to delete: " + filename);
            }
            return deleted;
        }
        return false;
    }

    // ==================== ENDLESS MODE SAVE SYSTEM ====================

    private static final String ENDLESS_SAVE_DIR = "saves/endless/";

    /**
     * 保存无尽模式游戏状态
     * 
     * @param state    无尽模式游戏状态
     * @param filename 文件名 (不需要带 .json 后缀)
     */
    public static void saveEndlessGame(de.tum.cit.fop.maze.model.EndlessGameState state, String filename) {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        // 确保无尽模式存档文件夹存在
        if (!Gdx.files.local(ENDLESS_SAVE_DIR).exists()) {
            Gdx.files.local(ENDLESS_SAVE_DIR).mkdirs();
        }

        // 自动加上 .json 后缀
        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        String text = json.prettyPrint(state);
        FileHandle file = Gdx.files.local(ENDLESS_SAVE_DIR + filename);
        file.writeString(text, false);

        Gdx.app.log("SaveManager", "Saved Endless Mode to: " + file.path());
    }

    /**
     * 默认保存无尽模式 (endless_auto_save.json)
     */
    public static void saveEndlessGame(de.tum.cit.fop.maze.model.EndlessGameState state) {
        saveEndlessGame(state, "endless_auto_save");
    }

    /**
     * 读取无尽模式存档
     */
    public static de.tum.cit.fop.maze.model.EndlessGameState loadEndlessGame(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = "endless_auto_save.json";
        }
        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        FileHandle file = Gdx.files.local(ENDLESS_SAVE_DIR + filename);

        if (!file.exists()) {
            Gdx.app.log("SaveManager", "Endless save file not found: " + filename);
            return null;
        }

        Json json = new Json();
        try {
            return json.fromJson(de.tum.cit.fop.maze.model.EndlessGameState.class, file.readString());
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to load endless save: " + e.getMessage());
            return null;
        }
    }

    /**
     * 默认读取无尽模式存档
     */
    public static de.tum.cit.fop.maze.model.EndlessGameState loadEndlessGame() {
        return loadEndlessGame("endless_auto_save.json");
    }

    /**
     * 获取所有无尽模式存档文件
     */
    public static FileHandle[] getEndlessSaveFiles() {
        FileHandle dir = Gdx.files.local(ENDLESS_SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            return new FileHandle[0];
        }

        FileHandle[] files = dir.list(".json");
        Arrays.sort(files, new Comparator<FileHandle>() {
            @Override
            public int compare(FileHandle f1, FileHandle f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        return files;
    }

    /**
     * 删除无尽模式存档
     */
    public static boolean deleteEndlessSave(String filename) {
        if (filename == null || filename.isEmpty())
            return false;

        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        FileHandle file = Gdx.files.local(ENDLESS_SAVE_DIR + filename);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Gdx.app.log("SaveManager", "Deleted endless save: " + filename);
            }
            return deleted;
        }
        return false;
    }

    /**
     * 检查是否有无尽模式存档
     */
    public static boolean hasEndlessSave() {
        FileHandle[] files = getEndlessSaveFiles();
        return files != null && files.length > 0;
    }
}
