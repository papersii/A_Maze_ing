package de.tum.cit.fop.maze.ui.widgets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.model.Player;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * 可复用的生命值显示组件 (Reusable Health Bar Widget)
 * 
 * 从 GameHUD 和 EndlessHUD 中提取的公共逻辑。
 * 显示心形图标表示玩家生命值，支持心碎动画。
 */
public class HealthBarWidget extends Table {

    private final Player player;
    private final TextureManager textureManager;

    // Cached UI elements
    private final Array<Image> cachedHearts = new Array<>();
    private int lastRenderedLiveCount = -1;

    // Animation state
    private int currentLives = -1;
    private boolean isHeartAnimating = false;
    private float heartAnimTime = 0f;

    // Configuration
    private float heartSize = 50f;
    private float heartPadding = 4f;

    public HealthBarWidget(Player player, TextureManager textureManager) {
        this.player = player;
        this.textureManager = textureManager;
    }

    /**
     * 设置心形图标大小
     */
    public HealthBarWidget setHeartSize(float size, float padding) {
        this.heartSize = size;
        this.heartPadding = padding;
        this.lastRenderedLiveCount = -1; // Force rebuild
        return this;
    }

    /**
     * 更新心形显示
     */
    public void update(float delta) {
        int actualLives = player.getLives();

        // Initialize logic state if needed
        if (currentLives == -1) {
            currentLives = actualLives;
        }

        if (actualLives < currentLives) {
            isHeartAnimating = true;
        } else if (actualLives > currentLives) {
            // Healed
            currentLives = actualLives;
            isHeartAnimating = false;
            lastRenderedLiveCount = -1; // Force rebuild
        }

        if (isHeartAnimating) {
            heartAnimTime += delta;
            if (textureManager.heartBreak != null && textureManager.heartBreak.isAnimationFinished(heartAnimTime)) {
                currentLives = actualLives;
                isHeartAnimating = false;
                heartAnimTime = 0;
                lastRenderedLiveCount = -1; // Force rebuild
            }
        }

        int heartsToDraw = isHeartAnimating ? currentLives : actualLives;

        // Only rebuild if count changed
        if (heartsToDraw != lastRenderedLiveCount && textureManager.heartRegion != null) {
            clearChildren();
            cachedHearts.clear();

            for (int i = 0; i < heartsToDraw; i++) {
                Image heart = new Image(textureManager.heartRegion);
                cachedHearts.add(heart);
                add(heart).size(heartSize, heartSize).pad(heartPadding);
            }
            lastRenderedLiveCount = heartsToDraw;
        }

        // Update dying heart texture if animating
        if (isHeartAnimating && cachedHearts.size > 0 && textureManager.heartBreak != null) {
            Image dyingHeart = cachedHearts.peek();
            TextureRegion frame = textureManager.heartBreak.getKeyFrame(heartAnimTime, false);
            dyingHeart.setDrawable(new TextureRegionDrawable(frame));
        }
    }
}
