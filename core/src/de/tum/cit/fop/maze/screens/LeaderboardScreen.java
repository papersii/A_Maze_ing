package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.utils.LeaderboardManager;
import de.tum.cit.fop.maze.utils.LeaderboardManager.LeaderboardEntry;
import de.tum.cit.fop.maze.utils.UIUtils;

import java.util.List;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 排行榜界面 (Leaderboard Screen)
 * 
 * 显示本地高分排行榜。
 * 支持按关卡筛选和全部显示。
 */
public class LeaderboardScreen extends BaseScreen {

    private Table contentTable;
    private ScrollPane scrollPane;
    private String currentFilter = null; // null = 全部
    private Texture backgroundTexture;

    public LeaderboardScreen(MazeRunnerGame game) {
        super(game);
        try {
            backgroundTexture = new Texture(Gdx.files.internal("leaderboard_bg.png"));
        } catch (Exception e) {
            backgroundTexture = null;
        }
        buildUI();
    }

    @Override
    protected void buildUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // 标题
        Label titleLabel = new Label("LEADERBOARD", skin, "title");
        titleLabel.setColor(Color.GOLD);
        rootTable.add(titleLabel).padTop(30).padBottom(20).row();

        // 筛选按钮栏
        Table filterTable = new Table();
        TextButton allBtn = new TextButton("All", skin);
        allBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentFilter = null;
                refreshLeaderboard();
            }
        });
        filterTable.add(allBtn).padRight(10);

        // 添加关卡筛选按钮 (前 5 关)
        for (int i = 1; i <= 5; i++) {
            final String levelPath = "maps/level-" + i + ".properties";
            final String levelName = "L" + i;
            TextButton levelBtn = new TextButton(levelName, skin);
            levelBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    currentFilter = levelPath;
                    refreshLeaderboard();
                }
            });
            filterTable.add(levelBtn).padRight(5);
        }

        rootTable.add(filterTable).padBottom(15).row();

        // 排行榜表头
        Table headerTable = new Table();
        headerTable.add(new Label("Rank", skin)).width(60).padRight(10);
        headerTable.add(new Label("Player", skin)).width(150).padRight(10);
        headerTable.add(new Label("Score", skin)).width(100).padRight(10);
        headerTable.add(new Label("Level", skin)).width(80).padRight(10);
        headerTable.add(new Label("Time", skin)).width(80).padRight(10);
        headerTable.add(new Label("Kills", skin)).width(60).padRight(10);
        headerTable.add(new Label("Date", skin)).width(150);
        rootTable.add(headerTable).padBottom(10).row();

        // 分隔线
        Table divider = new Table();
        divider.setBackground(skin.newDrawable("white", Color.GRAY));
        rootTable.add(divider).height(2).fillX().padBottom(10).row();

        // 排行榜内容
        contentTable = new Table();
        contentTable.top();

        scrollPane = new ScrollPane(contentTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // 自动聚焦滚动
        UIUtils.enableHoverScrollFocus(scrollPane, stage);

        rootTable.add(scrollPane).grow().pad(10).row();

        // 底部按钮
        Table buttonTable = new Table();

        TextButton refreshBtn = new TextButton("Refresh", skin);
        refreshBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshLeaderboard();
            }
        });
        buttonTable.add(refreshBtn).width(150).padRight(20);

        TextButton clearBtn = new TextButton("Clear All", skin);
        clearBtn.setColor(Color.RED);
        clearBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showClearConfirmDialog();
            }
        });
        buttonTable.add(clearBtn).width(150).padRight(20);

        TextButton backBtn = new TextButton("Back to Menu", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        buttonTable.add(backBtn).width(200);

        rootTable.add(buttonTable).padTop(15).padBottom(30);

        // 初始加载
        refreshLeaderboard();
    }

    /**
     * 刷新排行榜显示
     */
    private void refreshLeaderboard() {
        contentTable.clear();

        LeaderboardManager manager = LeaderboardManager.getInstance();
        List<LeaderboardEntry> entries;

        if (currentFilter != null) {
            entries = manager.getScoresByLevel(currentFilter);
        } else {
            entries = manager.getTopScores(50);
        }

        if (entries.isEmpty()) {
            Label emptyLabel = new Label("No scores yet. Play some levels!", skin);
            emptyLabel.setColor(Color.GRAY);
            contentTable.add(emptyLabel).pad(50);
            return;
        }

        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            Table rowTable = new Table();

            // 排名颜色
            Label rankLabel = new Label(String.valueOf(rank), skin);
            if (rank == 1)
                rankLabel.setColor(Color.GOLD);
            else if (rank == 2)
                rankLabel.setColor(Color.LIGHT_GRAY);
            else if (rank == 3)
                rankLabel.setColor(new Color(0.8f, 0.5f, 0.2f, 1f)); // Bronze

            rowTable.add(rankLabel).width(60).padRight(10);
            rowTable.add(new Label(truncate(entry.playerName, 15), skin)).width(150).padRight(10);

            Label scoreLabel = new Label(String.valueOf(entry.score), skin);
            scoreLabel.setColor(Color.YELLOW);
            rowTable.add(scoreLabel).width(100).padRight(10);

            rowTable.add(new Label(entry.getLevelDisplayName(), skin)).width(80).padRight(10);
            rowTable.add(new Label(entry.getFormattedTime(), skin)).width(80).padRight(10);
            rowTable.add(new Label(String.valueOf(entry.kills), skin)).width(60).padRight(10);

            Label dateLabel = new Label(entry.getFormattedDate(), skin);
            dateLabel.setColor(Color.LIGHT_GRAY);
            rowTable.add(dateLabel).width(150);

            contentTable.add(rowTable).fillX().padBottom(5).row();
            rank++;
        }
    }

    /**
     * 显示清除确认对话框
     */
    private void showClearConfirmDialog() {
        Dialog dialog = new Dialog("Confirm Clear", skin) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    LeaderboardManager.getInstance().clearAll();
                    refreshLeaderboard();
                }
            }
        };
        dialog.text("Are you sure you want to clear ALL leaderboard data?\nThis action cannot be undone.");
        dialog.button("Cancel", false);
        dialog.button("Clear All", true);
        dialog.show(stage);
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null)
            return "";
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 2) + "..";
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background using Cover mode (same as MenuScreen)
        if (backgroundTexture != null) {
            SpriteBatch batch = game.getSpriteBatch();

            // 获取实际屏幕尺寸 - 使用 backbuffer 尺寸以确保正确
            int screenWidth = Gdx.graphics.getBackBufferWidth();
            int screenHeight = Gdx.graphics.getBackBufferHeight();

            // 重置 GL Viewport 到整个屏幕
            Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);

            // 设置投影矩阵到屏幕像素坐标系
            batch.getProjectionMatrix().setToOrtho2D(0, 0, screenWidth, screenHeight);
            batch.begin();
            batch.setColor(0.4f, 0.4f, 0.4f, 1f); // Dim

            // 背景图片原始尺寸
            float texWidth = backgroundTexture.getWidth();
            float texHeight = backgroundTexture.getHeight();

            // 计算Cover模式的缩放比例
            float screenRatio = (float) screenWidth / screenHeight;
            float textureRatio = texWidth / texHeight;

            float drawWidth, drawHeight;
            float drawX, drawY;

            if (screenRatio > textureRatio) {
                // 屏幕更宽，以宽度为准，高度可能超出
                drawWidth = screenWidth;
                drawHeight = screenWidth / textureRatio;
                drawX = 0;
                drawY = (screenHeight - drawHeight) / 2; // 垂直居中
            } else {
                // 屏幕更高，以高度为准，宽度可能超出
                drawHeight = screenHeight;
                drawWidth = screenHeight * textureRatio;
                drawX = (screenWidth - drawWidth) / 2; // 水平居中
                drawY = 0;
            }

            batch.draw(backgroundTexture, drawX, drawY, drawWidth, drawHeight);
            batch.setColor(1, 1, 1, 1);
            batch.end();
        }

        // 恢复 Stage 的 Viewport（这会重新设置正确的 glViewport）
        stage.getViewport().apply();
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (backgroundTexture != null)
            backgroundTexture.dispose();
    }
}
