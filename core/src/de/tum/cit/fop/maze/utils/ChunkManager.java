package de.tum.cit.fop.maze.utils;

import de.tum.cit.fop.maze.config.EndlessModeConfig;
import de.tum.cit.fop.maze.model.MapChunk;
import de.tum.cit.fop.maze.model.WallEntity;

import java.util.*;

/**
 * 分块加载管理器 (Chunk Manager)
 * 
 * 管理无尽模式地图的分块加载和卸载。
 * 
 * 功能:
 * - 根据玩家位置动态加载周围区块
 * - 卸载远离玩家的区块以节省内存
 * - LRU缓存已生成的区块
 * 
 * 遵循单一职责原则：仅处理区块的加载/卸载逻辑。
 */
public class ChunkManager {

    /** 区块大小 */
    private final int chunkSize;

    /** 所有已生成的区块 (chunkId -> MapChunk) */
    private final Map<String, MapChunk> allChunks;

    /** 当前加载的区块ID集合 */
    private final Set<String> loadedChunkIds;

    /** 地图生成器 */
    private final EndlessMapGenerator mapGenerator;

    /** 最大缓存区块数 */
    private static final int MAX_CACHED_CHUNKS = 100;

    /** 监听器：区块加载/卸载时回调 */
    private ChunkListener listener;

    /**
     * 区块事件监听器接口
     */
    public interface ChunkListener {
        /** 区块加载时调用 */
        void onChunkLoaded(MapChunk chunk);

        /** 区块卸载时调用 */
        void onChunkUnloaded(MapChunk chunk);
    }

    public ChunkManager() {
        this.chunkSize = EndlessModeConfig.CHUNK_SIZE;
        this.allChunks = new LinkedHashMap<>(16, 0.75f, true); // LRU ordering
        this.loadedChunkIds = new HashSet<>();
        this.mapGenerator = new EndlessMapGenerator();
    }

    /**
     * 设置区块事件监听器
     */
    public void setListener(ChunkListener listener) {
        this.listener = listener;
    }

    /**
     * 根据玩家位置更新活跃区块
     * 
     * @param playerX 玩家X坐标（格子单位）
     * @param playerY 玩家Y坐标（格子单位）
     */
    public void updateActiveChunks(float playerX, float playerY) {
        int centerChunkX = (int) (playerX / chunkSize);
        int centerChunkY = (int) (playerY / chunkSize);

        int radius = EndlessModeConfig.ACTIVE_CHUNK_RADIUS;

        // 收集需要加载的区块
        Set<String> neededChunkIds = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int chunkX = centerChunkX + dx;
                int chunkY = centerChunkY + dy;

                // 检查是否在地图范围内
                if (!isValidChunkPosition(chunkX, chunkY)) {
                    continue;
                }

                String chunkId = getChunkId(chunkX, chunkY);
                neededChunkIds.add(chunkId);

                // 如果区块未加载，加载它
                if (!loadedChunkIds.contains(chunkId)) {
                    loadChunk(chunkX, chunkY);
                }
            }
        }

        // 卸载不再需要的区块
        List<String> toUnload = new ArrayList<>();
        for (String loadedId : loadedChunkIds) {
            if (!neededChunkIds.contains(loadedId)) {
                toUnload.add(loadedId);
            }
        }

        for (String id : toUnload) {
            unloadChunk(id);
        }

        // 清理缓存
        cleanupCache();
    }

    /**
     * 加载指定区块
     */
    private void loadChunk(int chunkX, int chunkY) {
        String chunkId = getChunkId(chunkX, chunkY);

        MapChunk chunk = allChunks.get(chunkId);

        if (chunk == null) {
            // 区块未生成，生成它
            chunk = mapGenerator.generateChunk(chunkX, chunkY);
            allChunks.put(chunkId, chunk);
        }

        if (!chunk.isLoaded()) {
            chunk.markLoaded();
            loadedChunkIds.add(chunkId);

            if (listener != null) {
                listener.onChunkLoaded(chunk);
            }
        }

        chunk.touch();
    }

    /**
     * 卸载指定区块
     */
    private void unloadChunk(String chunkId) {
        MapChunk chunk = allChunks.get(chunkId);

        if (chunk != null && chunk.isLoaded()) {
            chunk.markUnloaded();
            loadedChunkIds.remove(chunkId);

            if (listener != null) {
                listener.onChunkUnloaded(chunk);
            }
        }
    }

    /**
     * 清理超出缓存限制的区块
     */
    private void cleanupCache() {
        if (allChunks.size() <= MAX_CACHED_CHUNKS) {
            return;
        }

        // LRU: LinkedHashMap 按访问顺序排列，最早访问的在前
        Iterator<Map.Entry<String, MapChunk>> iterator = allChunks.entrySet().iterator();
        int toRemove = allChunks.size() - MAX_CACHED_CHUNKS;

        while (iterator.hasNext() && toRemove > 0) {
            Map.Entry<String, MapChunk> entry = iterator.next();
            MapChunk chunk = entry.getValue();

            // 不移除当前加载的区块
            if (!chunk.isLoaded()) {
                chunk.clear();
                iterator.remove();
                toRemove--;
            }
        }
    }

    /**
     * 检查区块位置是否有效
     */
    private boolean isValidChunkPosition(int chunkX, int chunkY) {
        int maxChunks = EndlessModeConfig.MAP_WIDTH / chunkSize;
        return chunkX >= 0 && chunkX < maxChunks &&
                chunkY >= 0 && chunkY < maxChunks;
    }

    /**
     * 获取区块唯一标识符
     */
    private String getChunkId(int chunkX, int chunkY) {
        return chunkX + "_" + chunkY;
    }

    /**
     * 获取指定位置的区块
     * 
     * @return 区块，如果不存在或未加载返回null
     */
    public MapChunk getChunk(int chunkX, int chunkY) {
        String chunkId = getChunkId(chunkX, chunkY);
        MapChunk chunk = allChunks.get(chunkId);
        if (chunk != null) {
            chunk.touch();
        }
        return chunk;
    }

    /**
     * 获取指定世界坐标所在的区块
     */
    public MapChunk getChunkAtWorld(float worldX, float worldY) {
        int chunkX = (int) (worldX / chunkSize);
        int chunkY = (int) (worldY / chunkSize);
        return getChunk(chunkX, chunkY);
    }

    /**
     * 获取所有已加载区块的墙体
     */
    public List<WallEntity> getLoadedWalls() {
        List<WallEntity> walls = new ArrayList<>();
        for (String id : loadedChunkIds) {
            MapChunk chunk = allChunks.get(id);
            if (chunk != null) {
                walls.addAll(chunk.getWalls());
            }
        }
        return walls;
    }

    /**
     * 获取所有已加载的区块
     */
    public List<MapChunk> getLoadedChunks() {
        List<MapChunk> chunks = new ArrayList<>();
        for (String id : loadedChunkIds) {
            MapChunk chunk = allChunks.get(id);
            if (chunk != null) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    /**
     * 检查指定区块是否已加载
     */
    public boolean isChunkLoaded(int chunkX, int chunkY) {
        return loadedChunkIds.contains(getChunkId(chunkX, chunkY));
    }

    /**
     * 获取已加载区块数量
     */
    public int getLoadedChunkCount() {
        return loadedChunkIds.size();
    }

    /**
     * 获取已缓存区块数量
     */
    public int getCachedChunkCount() {
        return allChunks.size();
    }

    /**
     * 获取中心区块坐标（用于玩家出生点）
     */
    public int getCenterChunkCoord() {
        return (EndlessModeConfig.MAP_WIDTH / chunkSize) / 2;
    }

    /**
     * 强制重新生成所有区块
     */
    public void regenerateAll() {
        allChunks.clear();
        loadedChunkIds.clear();
    }

    /**
     * 释放所有资源
     */
    public void dispose() {
        for (MapChunk chunk : allChunks.values()) {
            chunk.clear();
        }
        allChunks.clear();
        loadedChunkIds.clear();
    }
}
