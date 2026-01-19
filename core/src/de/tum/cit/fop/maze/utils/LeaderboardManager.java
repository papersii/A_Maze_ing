package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 排行榜管理器 (Leaderboard Manager)
 * 
 * 管理本地高分存储和查询。
 * 使用 LibGDX Preferences 进行持久化存储。
 * 
 * 功能：
 * - 提交分数 (带玩家名称、关卡、日期)
 * - 获取排行榜前 N 名
 * - 按关卡筛选排行榜
 * - 计算分数公式
 */
public class LeaderboardManager {

    private static final String PREFS_NAME = "maze_leaderboard_v1";
    private static final String KEY_ENTRIES = "leaderboard_entries";
    private static final int MAX_ENTRIES = 100;

    private static LeaderboardManager instance;
    private List<LeaderboardEntry> entries;

    /**
     * 排行榜条目
     */
    public static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
        public String playerName;
        public int score;
        public String levelPath;
        public long timestamp; // Unix timestamp
        public int kills;
        public float completionTime;

        // 无参构造函数 (JSON 反序列化需要)
        public LeaderboardEntry() {
        }

        public LeaderboardEntry(String playerName, int score, String levelPath,
                int kills, float completionTime) {
            this.playerName = playerName;
            this.score = score;
            this.levelPath = levelPath;
            this.kills = kills;
            this.completionTime = completionTime;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public int compareTo(LeaderboardEntry other) {
            // 降序排列 (分数高的在前)
            return Integer.compare(other.score, this.score);
        }

        /**
         * 获取格式化的日期字符串
         */
        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.format(new Date(timestamp));
        }

        /**
         * 获取关卡显示名称
         */
        public String getLevelDisplayName() {
            if (levelPath == null)
                return "Unknown";
            // "maps/level-1.properties" -> "Level 1"
            try {
                String num = levelPath.replaceAll("[^0-9]", "");
                return "Level " + num;
            } catch (Exception e) {
                return levelPath;
            }
        }

        /**
         * 获取格式化的时间 (MM:SS)
         */
        public String getFormattedTime() {
            int minutes = (int) (completionTime / 60);
            int seconds = (int) (completionTime % 60);
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private LeaderboardManager() {
        entries = new ArrayList<>();
        load();
    }

    /**
     * 获取单例实例
     */
    public static LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }

    /**
     * 计算分数
     * 
     * 公式:
     * - 基础分: 1000
     * - 时间加成: max(0, 500 - time * 2) (越快分越高)
     * - 击杀加成: kills * 50
     * - 金币加成: coins * 10
     * - 完美加成: +200 (无伤通关)
     * 
     * @param timeSeconds 完成时间 (秒)
     * @param kills       击杀数
     * @param coins       收集的金币
     * @param tookDamage  是否受到伤害
     * @return 计算后的分数
     */
    public static int calculateScore(float timeSeconds, int kills, int coins, boolean tookDamage) {
        int baseScore = 1000;
        int timeBonus = Math.max(0, 500 - (int) (timeSeconds * 2));
        int killBonus = kills * 50;
        int coinBonus = coins * 10;
        int perfectBonus = tookDamage ? 0 : 200;
        return baseScore + timeBonus + killBonus + coinBonus + perfectBonus;
    }

    /**
     * 提交分数
     * 
     * @param playerName     玩家名称
     * @param score          分数
     * @param levelPath      关卡路径
     * @param kills          击杀数
     * @param completionTime 完成时间
     */
    public void submitScore(String playerName, int score, String levelPath,
            int kills, float completionTime) {
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Anonymous";
        }

        LeaderboardEntry entry = new LeaderboardEntry(
                playerName.trim(), score, levelPath, kills, completionTime);

        entries.add(entry);
        Collections.sort(entries);

        // 保留前 MAX_ENTRIES 名
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }

        save();
        GameLogger.info("Leaderboard", "Score submitted: " + playerName + " - " + score);
    }

    /**
     * 获取前 N 名
     * 
     * @param limit 返回的最大数量
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getTopScores(int limit) {
        int count = Math.min(limit, entries.size());
        return new ArrayList<>(entries.subList(0, count));
    }

    /**
     * 获取所有分数
     */
    public List<LeaderboardEntry> getAllScores() {
        return new ArrayList<>(entries);
    }

    /**
     * 按关卡筛选分数
     * 
     * @param levelPath 关卡路径
     * @return 该关卡的排行榜条目
     */
    public List<LeaderboardEntry> getScoresByLevel(String levelPath) {
        List<LeaderboardEntry> filtered = new ArrayList<>();
        for (LeaderboardEntry entry : entries) {
            if (entry.levelPath != null && entry.levelPath.equals(levelPath)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    /**
     * 获取玩家的最高分
     * 
     * @param playerName 玩家名称
     * @return 最高分条目，如果没有则返回 null
     */
    public LeaderboardEntry getPlayerBest(String playerName) {
        for (LeaderboardEntry entry : entries) {
            if (entry.playerName.equalsIgnoreCase(playerName)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 获取某个分数的排名
     * 
     * @param score 分数
     * @return 排名 (1-based)
     */
    public int getRank(int score) {
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (entry.score > score) {
                rank++;
            } else {
                break;
            }
        }
        return rank;
    }

    /**
     * 检查分数是否进入前 N 名
     * 
     * @param score 分数
     * @param topN  前 N 名
     * @return 是否为高分
     */
    public boolean isHighScore(int score, int topN) {
        if (entries.size() < topN)
            return true;
        return entries.get(topN - 1).score < score;
    }

    /**
     * 清除所有排行榜数据
     */
    public void clearAll() {
        entries.clear();
        save();
        GameLogger.info("Leaderboard", "Leaderboard cleared");
    }

    // ==================== 持久化 ====================

    /**
     * 保存到 Preferences
     */
    private void save() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            String jsonStr = json.toJson(entries);
            prefs.putString(KEY_ENTRIES, jsonStr);
            prefs.flush();
        } catch (Exception e) {
            GameLogger.error("Leaderboard", "Failed to save leaderboard", e);
        }
    }

    /**
     * 从 Preferences 加载
     */
    @SuppressWarnings("unchecked")
    private void load() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            String jsonStr = prefs.getString(KEY_ENTRIES, "");

            if (!jsonStr.isEmpty()) {
                Json json = new Json();
                entries = json.fromJson(ArrayList.class, LeaderboardEntry.class, jsonStr);
                if (entries == null) {
                    entries = new ArrayList<>();
                }
                Collections.sort(entries);
            }
        } catch (Exception e) {
            GameLogger.error("Leaderboard", "Failed to load leaderboard", e);
            entries = new ArrayList<>();
        }
    }

    /**
     * 获取排行榜条目总数
     */
    public int getEntryCount() {
        return entries.size();
    }
}
