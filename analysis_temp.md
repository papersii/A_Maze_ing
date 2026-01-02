# 现有代码架构与项目结构分析

这份文档旨在帮助你理解当前项目的代码结构（Skeleton），以及它是如何与 Project 要求中的“完整架构”相对应的。

## 1. 项目概览

当前项目是一个基于 **libGDX** 游戏引擎的 Java 项目。libGDX 是一个跨平台的游戏开发框架，它处理了底层的图形渲染（OpenGL）、音频播放、输入处理等，让你能专注于游戏逻辑的开发。

### 文件结构解读

目前的核心代码位于 `core/src/de/tum/cit/fop/maze/` 目录下，主要包含以下三个核心类：

1.  **`MazeRunnerGame.java` (继承自 `Game`)**
    *   **角色**: 这是游戏的入口点（Main Entry Point）和核心控制器。
    *   **功能**:
        *   **生命周期管理**: 负责游戏的启动 (`create`) 和资源销毁 (`dispose`)。
        *   **屏幕管理**: 它持有一个 `Screen` 对象（当前显示的画面）。通过 `goToMenu()` 和 `goToGame()` 方法在“菜单界面”和“游戏界面”之间切换。
        *   **资源共享**: 它初始化并持有一些全局共享的资源，比如 `SpriteBatch` (用于绘图的大画笔) 和 `Skin` (UI 皮肤样式)，避免在通过 Screen 切换时重复加载资源。
        *   **全局音乐**: 负责播放背景音乐。

2.  **`MenuScreen.java` (实现 `Screen` 接口)**
    *   **角色**: 游戏的主菜单界面。
    *   **核心组件**: 使用了 `Stage` (舞台) 和 Scene2D UI (libGDX 的 UI 库)。
    *   **功能**: 显示标题和按钮。
        *   `Table`: 用于布局，把 UI 元素按行排列。
        *   `TextButton`: "Go To Game" 按钮，点击后调用 `game.goToGame()` 切换画面。

3.  **`GameScreen.java` (实现 `Screen` 接口)**
    *   **角色**: 实际进行游戏的界面。
    *   **核心组件**:
        *   `OrthographicCamera`: 正交相机，用于 2D 游戏的视角控制（决定你看哪里、看多大范围）。
        *   `render(float delta)`: 每一帧都会执行的循环（Game Loop）。
    *   **当前逻辑**:
        *   目前的代码非常简单，只是演示了如何“清空屏幕” -> “更新相机” -> “绘制文字” -> “绘制一个简单的角色动画”。
        *   **注意**: 这里还没有加载迷宫地图，也没有处理碰撞检测，这些是你需要实现的部分。

### 地图数据格式 (`maps/*.properties`)

在 `maps/` 目录下，地图以 `.properties` 文件存储。
*   **格式**: `x,y=type` (坐标=物体类型)
*   **例子**: `3,0=2` 表示在坐标 (3, 0) 的位置有一个类型为 2 的物体。
*   **类型对照表**:
    *   0: Wall (墙)
    *   1: Entry point (起点)
    *   2: Exit (终点)
    *   3: Trap (陷阱)
    *   4: Enemy (敌人)
    *   5: Key (钥匙)

这种简单的 Key-Value 结构非常适合用 Java 的 `Properties` 类来读取。

---

## 2. 架构蓝图：你需要实现什么？

根据 Project 的要求，你需要扩展这个骨架。目前的 Skeleton 只是搭建了舞台，演员（GameObject）和剧本（Game Logic）可以参照以下架构设计：

### 类层次结构 (Class Hierarchy)

为了满足“面向对象”的要求，你需要创建实体类来代表地图上的每一个物体。

*   **`GameObject` (抽象基类)**
    *   所有物体的父类。
    *   **属性**: `position` (位置 x, y), `texture` (图片外观), `width`, `height`.
    *   **方法**: `draw(SpriteBatch)` (自我绘制), `update(float delta)` (逻辑更新).

*   **继承自 `GameObject` 的具体类**:
    *   `Wall`: 静态物体，阻挡移动。
    *   `Trap`: 静态障碍物，造成伤害。
    *   `Key`: 可收集物品。
    *   **`Character` (抽象类, 继承 GameObject)**:
        *   具有生命值 (`lives`)、移动速度 (`speed`)。
        *   **方法**: `move(Direction)`, `takeDamage()`.
        *   **子类**:
            *   `Player`: 玩家控制的角色。处理键盘输入 (`INPUT`)。
            *   `Enemy`: AI 控制的角色。包含寻路算法（如 A* 或简单的巡逻逻辑）。

### 游戏循环 (The Game Loop)

在 `GameScreen` 的 `render(float delta)` 方法中，你需要把逻辑串联起来：

1.  **Input Handling**: 检查玩家是否按下了方向键 -> 更新 `Player` 的位置。
2.  **Game Logic Update**:
    *   让所有的 `Enemy` 更新位置（AI 移动）。
    *   **Collision Detection (碰撞检测)**:
        *   玩家撞墙了吗？-> 撤销移动。
        *   玩家碰到陷阱/敌人了吗？-> 扣血 (`lives--`)。
        *   玩家碰到钥匙了吗？-> 捡起 (`hasKey = true`)。
        *   玩家碰到出口了吗？-> 只有 `hasKey` 为真时才算胜利。
3.  **Rendering (绘图)**:
    *   清空屏幕。
    *   根据玩家位置移动 `Camera`。
    *   遍历所有的 `GameObject` 列表，调用它们的 `draw()` 方法。
    *   绘制 HUD (UI 层)，显示血量和钥匙状态。

## 3. 补充：深入理解 `Stage` 类 (Scene2D) (Added based on request)

你特别问到了 `Stage.class`，这是 libGDX 中 **Scene2D** 模块的核心。

### 什么是 `Stage` (舞台)？

想象一个真实的剧院舞台：
1.  **Stage (舞台)**：是一个容器（Container），它负责管理演员。
2.  **Actor (演员)**：无论是一棵树（Image），还是一句台词（Label），或者是一个按钮（Button），在这里都统称为 `Actor`。
3.  **Viewport (视口)**：决定了观众（玩家）如何看到这个舞台（例如：窗口变大时，舞台是拉伸还是显示更多内容）。

### `Stage` 在本项目中的作用

在 `MenuScreen.java` 中，你看到了这样的代码：

```java
stage = new Stage(viewport, game.getSpriteBatch());
// ...
stage.addActor(table); // 将 Table（也是一种 Actor）放入舞台
```

它的作用是构建 **UI (User Interface)** 系统：

1.  **层次管理 (Hierarchy)**:
    *   `Stage` 是根节点。
    *   我们在里面放了一个 `Table`（类似于 HTML 的 table 或 Android 的 LinearLayout）。
    *   `Table` 里放了 `Label`（标题）和 `TextButton`（按钮）。
    *   当你调用 `stage.draw()` 时，它会递归地根据这个层次结构，让每个 Actor 绘制自己。

2.  **事件处理 (Event Handling)**:
    *   这是 `Stage` 最强大的功能。
    *   你不需要自己计算鼠标点击了哪个坐标 (`x, y`)，然后判断这个坐标是不是在按钮内。
    *   `Gdx.input.setInputProcessor(stage);` 这行代码告诉 libGDX：“把所有的鼠标、键盘事件都交给舞台处理”。
    *   当玩家点击屏幕时，`Stage` 会自动检测“哪个 Actor 被点中了”，并触发相应的 `ChangeListener`（比如这里点击按钮跳转游戏）。

3.  **坐标转换 (Coordinate System)**:
    *   UI 坐标系通常是独立的。即使你的游戏世界很大（比如迷宫有 10000x10000 像素），UI 始终是“贴”在屏幕前面的。`Stage` 配合 `Viewport` 自动处理了这层映射。

### 总结

*   **MenuScreen**: 必须使用 `Stage`，因为它包含交互式按钮和布局。
*   **GameScreen**:
    *   游戏里的角色（Player, Enemy）通常**不**使用 `Stage`（虽然也可以，但性能和灵活性不如直接用 `SpriteBatch`）。
    *   但是，游戏里的 **HUD**（显示血量、分数的那一层 UI）**应该**再建立一个独立的 `Stage` 来管理。这样，无论你的相机怎么移动（跟随主角漫游迷宫），血量条始终固定在屏幕左上角。

## 4. 总结

目前的 Skeleton 是一个标准的 **MVC (Model-View-Controller)** 变体（在游戏开发中常融合在一起）：
*   **Model (数据)**: `maps/*.properties` (目前还没解析成 Java 对象)。
*   **View (视图)**: `GameScreen` 和 `MenuScreen` 负责显示。
*   **Controller (控制)**: `MazeRunnerGame` 负责流程控制。

**接下来的首要任务**通常是：
1.  编写一个 **MapLoader**，读取 `.properties` 文件，把它转换成 `List<GameObject>`。
2.  创建 `Wall`, `Player` 等类，让地图能真正“显示”在屏幕上，而不仅仅是现在演示的那个旋转小人。
