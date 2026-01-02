package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.model.GameMap;
import de.tum.cit.fop.maze.model.GameObject;
import de.tum.cit.fop.maze.model.Player;
import de.tum.cit.fop.maze.model.Enemy;
import de.tum.cit.fop.maze.model.CollisionManager;
import de.tum.cit.fop.maze.utils.MapLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * GameScreen 类负责渲染游戏画面。
 * 它处理游戏逻辑和游戏元素的渲染。
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;

    // 地图与对象数据
    private GameMap gameMap;

    // 临时纹理 (用于代替真实的 png 图片)
    private Texture wallTexture;
    private Texture playerTexture;
    private Texture exitTexture;
    private Texture trapTexture;
    private Texture enemyTexture;
    private Texture keyTexture;

    // 玩家与物理系统
    private Player player;
    private CollisionManager collisionManager;

    // 敌人列表 (从 GameMap 中提取)
    private List<Enemy> enemies;

    // 单元格大小 (16像素)
    private static final float UNIT_SCALE = 16f;

    // 相机跟随的平滑速度：数值越小惯性越大(越滑)，数值越大跟得越紧
    // 推荐范围: 2.0f (很滑) ~ 10.0f (很紧)
    private static final float CAMERA_LERP_SPEED = 5.0f;

    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        // 1. 加载地图
        gameMap = MapLoader.loadMap("maps/level-1.properties");

        // 2. 初始化玩家与物理
        player = new Player(gameMap.getPlayerStartX(), gameMap.getPlayerStartY());
        collisionManager = new CollisionManager(gameMap);

        // 3. 从 GameMap 中提取敌人引用
        enemies = new ArrayList<>();
        for (GameObject obj : gameMap.getGameObjects()) {
            if (obj instanceof Enemy) {
                enemies.add((Enemy) obj);
            }
        }

        // 4. 创建临时素材 (后续可用 new Texture("wall.png") 替换)
        wallTexture = createColorTexture(Color.GRAY);
        playerTexture = createColorTexture(Color.BLUE);
        exitTexture = createColorTexture(Color.GREEN);
        trapTexture = createColorTexture(Color.RED);
        enemyTexture = createColorTexture(Color.ORANGE);
        keyTexture = createColorTexture(Color.YELLOW);

        // 5. 设置相机
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 0.5f;
    }

    // 辅助方法：创建一个 16x16 的纯色方块纹理
    private Texture createColorTexture(Color color) {
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void render(float delta) {
        // --- 1. 输入处理 ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // 更新玩家状态 (冷却计时器)
        player.update(delta);

        // 跑步状态
        boolean isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        player.setRunning(isRunning);

        // 基于网格的移动 - 只有冷却完成才能移动
        if (player.canMove()) {
            int deltaX = 0;
            int deltaY = 0;

            // 使用 isKeyJustPressed 确保每次按键只移动一格
            // 如果想按住连续移动，使用 isKeyPressed 配合冷却
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
                deltaX = -1;
            else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
                deltaX = 1;
            else if (Gdx.input.isKeyPressed(Input.Keys.UP))
                deltaY = 1;
            else if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
                deltaY = -1;

            // 尝试移动
            if (deltaX != 0 || deltaY != 0) {
                int nextX = (int) player.getX() + deltaX;
                int nextY = (int) player.getY() + deltaY;

                if (collisionManager.isWalkable(nextX, nextY)) {
                    player.moveGrid(deltaX, deltaY);
                }
            }
        }

        // --- 2. 更新敌人 ---
        for (Enemy enemy : enemies) {
            enemy.update(delta, player, collisionManager);
        }

        // --- 2.5 检测玩家与敌人的碰撞 ---
        int playerGridX = Math.round(player.getX());
        int playerGridY = Math.round(player.getY());

        for (Enemy enemy : enemies) {
            int enemyGridX = Math.round(enemy.getX());
            int enemyGridY = Math.round(enemy.getY());

            // 如果玩家和敌人在同一格
            if (playerGridX == enemyGridX && playerGridY == enemyGridY) {
                if (player.damage(1)) {
                    // 玩家受伤了，可以在这里添加音效/特效
                    System.out.println("玩家受伤! 剩余生命: " + player.getLives());
                }
                break; // 每帧只受一次伤
            }
        }

        // --- 3. 逻辑更新 ---
        updateCamera(delta);

        // --- 4. 渲染绘制 ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // A. 绘制地图物体 (不包括敌人，因为敌人位置会动态变化)
        for (GameObject obj : gameMap.getGameObjects()) {
            Texture textureToDraw = wallTexture;

            String type = obj.getClass().getSimpleName();
            switch (type) {
                case "Exit":
                    textureToDraw = exitTexture;
                    break;
                case "Trap":
                    textureToDraw = trapTexture;
                    break;
                case "Enemy":
                    // 敌人单独绘制，跳过这里
                    continue;
                case "Key":
                    textureToDraw = keyTexture;
                    break;
            }
            game.getSpriteBatch().draw(textureToDraw, obj.getX() * UNIT_SCALE, obj.getY() * UNIT_SCALE);
        }

        // B. 绘制敌人 (使用动态位置)
        for (Enemy enemy : enemies) {
            game.getSpriteBatch().draw(enemyTexture, enemy.getX() * UNIT_SCALE, enemy.getY() * UNIT_SCALE);
        }

        // C. 绘制玩家
        game.getSpriteBatch().draw(playerTexture, player.getX() * UNIT_SCALE, player.getY() * UNIT_SCALE);

        game.getSpriteBatch().end();
    }

    /**
     * 核心逻辑：带有惯性的相机跟随
     * 使用 Lerp (线性插值) 让相机平滑地飞向玩家
     */
    private void updateCamera(float delta) {
        // 1. 获取玩家当前的中心点坐标 (像素单位)
        float targetX = player.getX() * UNIT_SCALE + UNIT_SCALE / 2;
        float targetY = player.getY() * UNIT_SCALE + UNIT_SCALE / 2;

        // 2. 使用 Lerp 实现惯性移动
        // 公式：当前位置 += (目标位置 - 当前位置) * 速度 * 时间增量
        camera.position.x += (targetX - camera.position.x) * CAMERA_LERP_SPEED * delta;
        camera.position.y += (targetY - camera.position.y) * CAMERA_LERP_SPEED * delta;

        // 3. 限制相机范围 (防止看到地图外的黑边)
        // 只有当地图比当前视口大时才进行限制
        float mapPixelWidth = gameMap.getWidth() * UNIT_SCALE;
        float mapPixelHeight = gameMap.getHeight() * UNIT_SCALE;

        // 计算视口在当前缩放下一半的宽和高
        float halfWidth = (camera.viewportWidth * camera.zoom) / 2;
        float halfHeight = (camera.viewportHeight * camera.zoom) / 2;

        if (mapPixelWidth > camera.viewportWidth * camera.zoom) {
            camera.position.x = MathUtils.clamp(camera.position.x, halfWidth, mapPixelWidth - halfWidth);
        }
        if (mapPixelHeight > camera.viewportHeight * camera.zoom) {
            camera.position.y = MathUtils.clamp(camera.position.y, halfHeight, mapPixelHeight - halfHeight);
        }

        // 4. 缩放控制
        if (Gdx.input.isKeyPressed(Input.Keys.Z))
            camera.zoom -= 0.02f;
        if (Gdx.input.isKeyPressed(Input.Keys.X))
            camera.zoom += 0.02f;
        camera.zoom = MathUtils.clamp(camera.zoom, 0.2f, 2.0f);

        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        wallTexture.dispose();
        playerTexture.dispose();
        exitTexture.dispose();
        trapTexture.dispose();
        enemyTexture.dispose();
        keyTexture.dispose();
    }
}