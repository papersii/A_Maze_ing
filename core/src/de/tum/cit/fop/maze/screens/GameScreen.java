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
import de.tum.cit.fop.maze.utils.MapLoader;

/**
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
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

    // 玩家位置
    private float playerX;
    private float playerY;

    // 单元格大小 (16像素)
    private static final float UNIT_SCALE = 16f;

    // 相机跟随的平滑速度：数值越小惯性越大(越滑)，数值越大跟得越紧
    // 推荐范围: 2.0f (很滑) ~ 10.0f (很紧)
    private static final float CAMERA_LERP_SPEED = 3.0f;

    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        // 1. 加载地图
        gameMap = MapLoader.loadMap("maps/level-5.properties");

        // 2. 初始化玩家位置
        playerX = gameMap.getPlayerStartX();
        playerY = gameMap.getPlayerStartY();

        // 3. 创建临时素材 (后续可用 new Texture("wall.png") 替换)
        wallTexture = createColorTexture(Color.GRAY);
        playerTexture = createColorTexture(Color.BLUE);
        exitTexture = createColorTexture(Color.GREEN);
        trapTexture = createColorTexture(Color.RED);
        enemyTexture = createColorTexture(Color.ORANGE);
        keyTexture = createColorTexture(Color.YELLOW);

        // 4. 设置相机
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

        // 临时移动控制 (用于测试相机，实际项目中会被 Player 类替代)
        float speed = 5.0f * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) playerX -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) playerX += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) playerY += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) playerY -= speed;

        // --- 2. 逻辑更新 ---
        updateCamera(delta);

        // --- 3. 渲染绘制 ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // A. 绘制地图物体
        for (GameObject obj : gameMap.getGameObjects()) {
            Texture textureToDraw = wallTexture;

            String type = obj.getClass().getSimpleName();
            switch (type) {
                case "Exit": textureToDraw = exitTexture; break;
                case "Trap": textureToDraw = trapTexture; break;
                case "Enemy": textureToDraw = enemyTexture; break;
                case "Key": textureToDraw = keyTexture; break;
            }
            game.getSpriteBatch().draw(textureToDraw, obj.getX() * UNIT_SCALE, obj.getY() * UNIT_SCALE);
        }

        // B. 绘制玩家
        game.getSpriteBatch().draw(playerTexture, playerX * UNIT_SCALE, playerY * UNIT_SCALE);

        game.getSpriteBatch().end();
    }

    /**
     * 核心逻辑：带有惯性的相机跟随
     * 使用 Lerp (线性插值) 让相机平滑地飞向玩家
     */
    private void updateCamera(float delta) {
        // 1. 获取玩家当前的中心点坐标 (像素单位)
        float targetX = playerX * UNIT_SCALE + UNIT_SCALE / 2;
        float targetY = playerY * UNIT_SCALE + UNIT_SCALE / 2;

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
        if (Gdx.input.isKeyPressed(Input.Keys.Z)) camera.zoom -= 0.02f;
        if (Gdx.input.isKeyPressed(Input.Keys.X)) camera.zoom += 0.02f;
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
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void show() {}

    @Override
    public void hide() {}

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