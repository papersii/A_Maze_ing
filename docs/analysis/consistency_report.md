# 深度分析：为什么"同样的代码跑出不一样的结果"？
(Deep Analysis: Why "Same Code, Different Results"?)

## 1. 问题的根源 (Root Cause)
经过代码审计，我们发现了导致"初始血量不一致"等问题的核心原因：**隐式本地持久化 (Implicit Local Persistence)**。

### 罪魁祸首：`GameSettings.java` and `Preferences`
你的代码并不是"无状态"的。在 `GameSettings.java` (Line 81) 中：

```java
public static void loadUserDefaults() {
    Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
    userPlayerMaxLives = prefs.getInteger("playerMaxLives", DEFAULT_PLAYER_MAX_LIVES);
    // ...
}
```

这意味着：
1. **LibGDX Preferences** 是存储在项目文件夹**之外**的。
   - macOS: `~/.prefs/maze_runner_settings`
   - Windows: `C:\Users\User\.prefs\maze_runner_settings`
2. **Git 无法覆盖这些文件**。哪怕你删光了项目重跑，这些隐藏在系统里的文件还在。
3. **现象解释**：
   - 队友 A 曾经在测试时修改过设置，或者商店购买了升级，导致本地 `playerMaxLives` 变成了 5。
   - 队友 B 第一次运行，没有本地记录，所以读到了代码里的默认值 3。
   - 虽然你们代码一样 (Git Commit SHA 一致)，但**读取的外部数据源**不一样。

---

## 2. 影响范围 (Scope of Impact)
不仅仅是血量，以下系统也受到影响：
1. **商店系统 (`ShopManager`)**:
   - `maze_shop` 存储了金币数量 (`player_coins`) 和已购买物品。
   - 某些人可能进游戏就自带神装，也是这个原因。
2. **成就与排行榜 (`AchievementManager`, `LeaderboardManager`)**:
   - 存储在 `maze_achievements` 和 `maze_leaderboard`。
3. **按键绑定**:
   - 如果有人改了键位，push 代码后，别人拉下来也是按自己的键位跑，不会同步。

---

## 3. 为什么这是个"坏味道" (Bad Smell)？
- **违反了"显式依赖"原则**：游戏的初始状态依赖于不可见的外部文件，而不是代码中的常量。
- **破坏了"可复现性" (Reproducibility)**：测试 Bug 时，你无法确定对方是在什么初始状态下触发的。

---

## 4. 解决方案 (Proposed Solutions)

### 方案 A：开发模式强制重置 (推荐)
在 `DesktopLauncher` 中加入启动参数或检测，如果是开发环境，启动时自动清除 Preferences。

```java
// 在游戏启动时
if (debugMode) {
    Gdx.app.getPreferences("maze_runner_settings").clear();
    Gdx.app.getPreferences("maze_shop").clear();
    Gdx.app.getPreferences("maze_runner_settings").flush();
}
```

### 方案 B：添加"一键重置"功能
在主菜单添加一个 "Reset All Data" 按钮，调用 `resetUserDefaultsToHardcoded()` 和 `ShopManager.resetAllPurchases()`。

### 方案 C：分离开发配置与生产配置
使用不同的 `PREFS_NAME`。
`GameSettings.java`:
```java
private static final String PREFS_NAME = "maze_runner_settings_" + VERSION; 
// 每次版本更新换个名字，强制所有人重置
```

---

## 5. 总结
这个 Bug 不是代码逻辑错误，而是**状态管理**的问题。
- **Git** 管理的是**逻辑**。
- **Preferences** 管理的是**状态**。
- 当逻辑依赖于不同步的状态时，就不一致了。
