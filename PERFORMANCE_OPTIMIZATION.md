# LibGDX 游戏性能优化实践指南

> 本文档总结了 FOP Amazeing 项目中的性能优化经验，供后续开发参考。

## 目录
1. [纹理加载优化](#1-纹理加载优化)
2. [空间索引优化](#2-空间索引优化)
3. [渲染优化](#3-渲染优化)
4. [相机处理](#4-相机处理)
5. [游戏逻辑优化](#5-游戏逻辑优化)
6. [预加载策略](#6-预加载策略)
7. [常见问题与解决方案](#7-常见问题与解决方案)

---

## 1. 纹理加载优化

### 问题描述
游戏运行时动态加载大纹理（如 2080x2048）导致严重卡顿，每次 Auto-padding 操作约需 60ms。

### 解决方案

#### 1.1 纹理缓存机制
```java
// CustomElementManager.java
private Map<String, TextureRegion> textureCache = new HashMap<>();

// 在加载纹理时使用缓存
if (file.exists()) {
    TextureRegion region = textureCache.get(path);
    if (region == null) {
        Texture tex = new Texture(file);
        region = createCroppedRegion(tex);
        textureCache.put(path, region);  // 缓存
    }
    frames.add(region);
}
```

#### 1.2 动画缓存
```java
// 动画缓存：Key = "elementId:action"
private Map<String, Animation<TextureRegion>> animationCache = new HashMap<>();

public Animation<TextureRegion> getAnimation(String elementId, String action) {
    String key = elementId + ":" + action;
    if (animationCache.containsKey(key)) {
        return animationCache.get(key);  // 命中缓存，直接返回
    }
    // ... 加载动画并缓存
}
```

### 经验总结
- **缓存键一致性**：确保缓存键（如路径）在查找前已完成规范化
- **双层缓存**：纹理缓存 + 动画缓存，避免重复 IO 和 padding 操作

---

## 2. 空间索引优化

### 问题描述
遍历所有敌人/陷阱进行碰撞检测和渲染导致 O(n) 复杂度，大量实体时性能下降。

### 解决方案

#### 2.1 SpatialIndex 网格索引
```java
// SpatialIndex.java - 基于网格的空间索引
public class SpatialIndex<T extends Positionable> {
    private final int cellSize;
    private final Map<Long, List<T>> grid = new HashMap<>();
    
    // 查询指定范围内的实体
    public List<T> queryRadius(float centerX, float centerY, float radius) {
        List<T> result = new ArrayList<>();
        int minCellX = (int) ((centerX - radius) / cellSize);
        int maxCellX = (int) ((centerX + radius) / cellSize);
        // ... 遍历相关网格单元
        return result;
    }
}
```

#### 2.2 使用场景
```java
// GameWorld.java - 敌人空间索引
private SpatialIndex<Enemy> enemyIndex;

// 只渲染附近敌人
List<Enemy> nearbyEnemies = enemyIndex.queryRadius(playerX, playerY, ENTITY_RENDER_RADIUS);
```

### 经验总结
- **渲染层安全使用**：空间索引适合用于渲染优化（只渲染视野内实体）
- **游戏逻辑慎用**：碰撞检测等核心逻辑应保持遍历所有实体，避免遗漏
- **网格大小**：建议 8-16 格子单位，平衡查询效率和内存占用

---

## 3. 渲染优化

### 3.1 渲染半径配置
```java
// GameConfig.java
public static final float ENTITY_RENDER_RADIUS = 10f;  // 只渲染玩家 10 格范围内的实体
```

### 3.2 避免重复创建对象
```java
// 错误示范：每帧创建新 Color 对象
game.getSpriteBatch().setColor(new Color(0.2f, 0.2f, 0.2f, 0.8f));

// 正确做法：使用常量或复用对象
game.getSpriteBatch().setColor(0.2f, 0.2f, 0.2f, 0.8f);
```

### 3.3 批量渲染
- 减少 `SpriteBatch.begin()` / `end()` 调用次数
- 按纹理分组渲染，减少纹理切换

---

## 4. 相机处理

### 问题描述
相机拉高（zoom 增大）时，视野超过地图尺寸导致地图偏移出现黑边。

### 解决方案
```java
// GameScreen.java - updateCamera()
float mapW = gameMap.getWidth() * UNIT_SCALE;
float mapH = gameMap.getHeight() * UNIT_SCALE;
float viewW = camera.viewportWidth * camera.zoom;
float viewH = camera.viewportHeight * camera.zoom;

// 当视野大于地图时居中相机，否则 clamp 到地图边界
if (mapW <= viewW) {
    camera.position.x = mapW / 2;  // 居中
} else {
    camera.position.x = MathUtils.clamp(camera.position.x, viewW / 2, mapW - viewW / 2);
}
if (mapH <= viewH) {
    camera.position.y = mapH / 2;  // 居中
} else {
    camera.position.y = MathUtils.clamp(camera.position.y, viewH / 2, mapH - viewH / 2);
}
```

---

## 5. 游戏逻辑优化

### 5.1 敌人更新优化
```java
// 原版优化：只更新附近敌人
for (Enemy enemy : enemies) {
    float dst2 = Vector2.dst2(playerX, playerY, enemy.getX(), enemy.getY());
    if (dst2 > 1600) continue;  // 距离 > 40 格时跳过更新
    enemy.update(delta, player, collisionManager, safeGrid);
}
```

### 5.2 碰撞检测保持全量
```java
// 碰撞检测必须遍历所有敌人，不能使用空间索引优化
for (Enemy enemy : enemies) {
    if (Vector2.dst(playerX, playerY, enemy.getX(), enemy.getY()) < hitDistance) {
        player.damage(1);
    }
}
```

### 经验总结
- **更新逻辑**：可以使用距离判断跳过远处敌人
- **碰撞检测**：必须遍历所有实体，否则可能遗漏碰撞

---

## 6. 预加载策略

### 问题描述
游戏初次遇到新动画时才加载纹理，导致运行时卡顿。

### 解决方案

#### 6.1 统一使用 LoadingScreen
```java
// ArmorSelectScreen.java - 通过 LoadingScreen 进入游戏
private void startGame(DamageType selectedArmorType) {
    game.setScreen(new LoadingScreen(game, mapPath, selectedArmorType, levelDamageType));
}
```

#### 6.2 LoadingScreen 预加载流程
```java
// LoadingScreen.java
private void initializePreloadTasks() {
    preloadTasks = CustomElementManager.getInstance().getPreloadTasks();
}

// 每帧加载一部分任务，更新进度条
for (int i = 0; i < tasksPerFrame && currentTaskIndex < preloadTasks.size(); i++) {
    CustomElementManager.getInstance().preloadAnimation(elementId, action);
    currentTaskIndex++;
}
progressBar.setValue((float) currentTaskIndex / preloadTasks.size());
```

### 经验总结
- **所有关卡入口都应通过 LoadingScreen**
- **显示进度条**：让玩家知道正在加载，避免误以为卡死
- **分帧加载**：每帧只加载少量任务（如 2 个），避免单帧卡顿

---

## 7. 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 移动/攻击时卡顿 | 纹理首次加载时 padding | 使用 LoadingScreen 预加载 |
| 同一纹理重复 padding | 缓存未命中 | 检查缓存键一致性，确保路径规范化 |
| 相机拉高时黑边 | 视野超过地图时未居中 | 添加居中逻辑 |
| 怪物/玩家显示为默认素材 | elements.json 配置错误或被覆盖 | 从 git 恢复正确配置 |
| 远处怪物不更新 | 距离优化导致休眠 | 检查 dst2 > 1600 阈值 |

---

## 配置参数参考

```java
// GameConfig.java
public static final int SPATIAL_CELL_SIZE = 8;        // 空间索引格子大小
public static final float ENTITY_UPDATE_RADIUS = 25f; // 实体更新半径
public static final float ENTITY_RENDER_RADIUS = 10f; // 实体渲染半径
public static final float ENEMY_CHASE_RADIUS = 15f;   // 敌人追踪半径
public static final float ENEMY_PATROL_RADIUS = 20f;  // 敌人巡逻半径
```

---

## 总结

1. **预加载**：所有资源应在 LoadingScreen 中预加载，避免运行时卡顿
2. **缓存**：纹理和动画都应缓存，避免重复加载
3. **空间索引**：用于渲染优化，但核心游戏逻辑（碰撞检测）应保持全量遍历
4. **相机**：处理好视野超过地图的边界情况
5. **配置化**：将阈值参数放入 GameConfig，便于调优

---

*Created: 2026-01-21 | Project: FOP Amazeing*
