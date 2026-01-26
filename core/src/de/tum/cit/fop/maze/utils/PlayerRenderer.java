package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.custom.CustomElementDefinition;
import de.tum.cit.fop.maze.custom.CustomElementManager;
import de.tum.cit.fop.maze.custom.ElementType;
import de.tum.cit.fop.maze.model.Player;

/**
 * PlayerRenderer - 统一的玩家渲染工具类
 * 
 * 抽取自 GameScreen 和 EndlessGameScreen 的共享渲染逻辑，
 * 确保两种模式下玩家渲染行为完全一致。
 * 
 * 使用方式:
 * 
 * <pre>
 * PlayerRenderer renderer = new PlayerRenderer(batch, textureManager, UNIT_SCALE);
 * renderer.render(player, direction, stateTime, isMoving);
 * </pre>
 */
public class PlayerRenderer {

    private final SpriteBatch batch;
    private final TextureManager textureManager;
    private final float unitScale;

    // 缓存的自定义皮肤ID，避免每帧重新查找
    private String cachedPlayerSkinId = null;
    private boolean skinCacheValid = false;

    public PlayerRenderer(SpriteBatch batch, TextureManager textureManager, float unitScale) {
        this.batch = batch;
        this.textureManager = textureManager;
        this.unitScale = unitScale;
    }

    /**
     * 渲染玩家精灵
     *
     * @param player    玩家对象
     * @param direction 玩家朝向 (0=下, 1=上, 2=左, 3=右)
     * @param stateTime 动画状态时间
     * @param isMoving  玩家是否正在移动
     */
    public void render(Player player, int direction, float stateTime, boolean isMoving) {
        render(player, direction, stateTime, isMoving, null);
    }

    /**
     * 渲染玩家精灵（带武器渲染回调）
     *
     * @param player         玩家对象
     * @param direction      玩家朝向 (0=下, 1=上, 2=左, 3=右)
     * @param stateTime      动画状态时间
     * @param isMoving       玩家是否正在移动
     * @param weaponRenderer 武器渲染回调（可选），用于在正确的层级渲染武器
     */
    public void render(Player player, int direction, float stateTime, boolean isMoving,
            WeaponRenderCallback weaponRenderer) {
        TextureRegion playerFrame = null;
        boolean flipX = false;

        String playerSkinId = getActivePlayerSkinId();
        boolean useCustomSkin = playerSkinId != null;

        if (useCustomSkin) {
            CustomElementManager manager = CustomElementManager.getInstance();

            if (player.isDead()) {
                // 死亡动画
                Animation<TextureRegion> deathAnim = manager.getAnimation(playerSkinId, "Death");
                if (deathAnim != null) {
                    playerFrame = deathAnim.getKeyFrame(player.getDeathProgress() * 0.5f, false);
                }
            } else if (player.isAttacking()) {
                // 攻击动画
                float progress = getAttackAnimProgress(player);
                String attackAction = getDirectionalAction("Attack", direction);
                Animation<TextureRegion> attackAnim = manager.getAnimation(playerSkinId, attackAction);
                if (attackAnim == null && !attackAction.equals("Attack")) {
                    attackAnim = manager.getAnimation(playerSkinId, "Attack");
                }
                if (attackAnim != null) {
                    playerFrame = attackAnim.getKeyFrame(progress, false);
                    flipX = (direction == 2);
                }
            } else if (isMoving) {
                // 移动动画
                String moveAction = getDirectionalAction("Move", direction);
                Animation<TextureRegion> moveAnim = manager.getAnimation(playerSkinId, moveAction);
                if (moveAnim == null && !moveAction.equals("Move")) {
                    moveAnim = manager.getAnimation(playerSkinId, "Move");
                }
                if (moveAnim != null) {
                    playerFrame = moveAnim.getKeyFrame(stateTime, true);
                    flipX = (direction == 2);
                }
            } else {
                // 待机动画
                String idleAction = getDirectionalAction("Idle", direction);
                Animation<TextureRegion> idleAnim = manager.getAnimation(playerSkinId, idleAction);
                if (idleAnim == null && !idleAction.equals("Idle")) {
                    idleAnim = manager.getAnimation(playerSkinId, "Idle");
                }
                if (idleAnim != null) {
                    playerFrame = idleAnim.getKeyFrame(stateTime, true);
                    flipX = (direction == 2);
                }
            }
        }

        // 回退到默认动画
        if (playerFrame == null) {
            playerFrame = getDefaultPlayerFrame(player, direction, stateTime, isMoving);
        }

        // 保存当前颜色
        Color oldColor = batch.getColor().cpy();

        // 状态着色
        if (player.isDead()) {
            batch.setColor(0.5f, 0.5f, 0.5f, 1f);
        } else if (player.isHurt()) {
            batch.setColor(1f, 0f, 0f, 1f);
        }

        // 计算绘制位置和尺寸
        float drawX = player.getX() * unitScale;
        float drawY = player.getY() * unitScale;
        float drawWidth = playerFrame.getRegionWidth();
        float drawHeight = playerFrame.getRegionHeight();

        // 自定义皮肤统一缩放
        if (useCustomSkin) {
            drawWidth = unitScale;
            drawHeight = unitScale;
        } else if (playerFrame.getRegionWidth() > 16) {
            drawX -= (playerFrame.getRegionWidth() - 16) / 2f;
        }

        // 朝上或朝左时先渲染武器（在玩家身后）
        if (weaponRenderer != null && !player.isDead() && (direction == 1 || direction == 2)) {
            weaponRenderer.renderWeapon(player, direction, stateTime);
        }

        // 渲染玩家
        if (player.isDead()) {
            batch.draw(playerFrame, drawX, drawY, drawWidth, drawHeight);
        } else if (flipX) {
            batch.draw(playerFrame, drawX + drawWidth, drawY, -drawWidth, drawHeight);
        } else {
            batch.draw(playerFrame, drawX, drawY, drawWidth, drawHeight);
        }

        // 恢复颜色
        batch.setColor(oldColor);

        // 其他方向时武器在玩家前面
        if (weaponRenderer != null && !player.isDead() && direction != 1 && direction != 2) {
            weaponRenderer.renderWeapon(player, direction, stateTime);
        }
    }

    /**
     * 获取默认玩家动画帧
     */
    private TextureRegion getDefaultPlayerFrame(Player player, int direction, float stateTime, boolean isMoving) {
        if (player.isAttacking()) {
            float progress = getAttackAnimProgress(player);
            switch (direction) {
                case 1:
                    return textureManager.playerAttackUp.getKeyFrame(progress, false);
                case 2:
                    return textureManager.playerAttackLeft.getKeyFrame(progress, false);
                case 3:
                    return textureManager.playerAttackRight.getKeyFrame(progress, false);
                default:
                    return textureManager.playerAttackDown.getKeyFrame(progress, false);
            }
        } else if (isMoving) {
            switch (direction) {
                case 1:
                    return textureManager.playerUp.getKeyFrame(stateTime, true);
                case 2:
                    return textureManager.playerLeft.getKeyFrame(stateTime, true);
                case 3:
                    return textureManager.playerRight.getKeyFrame(stateTime, true);
                default:
                    return textureManager.playerDown.getKeyFrame(stateTime, true);
            }
        } else {
            switch (direction) {
                case 1:
                    return textureManager.playerUpStand;
                case 2:
                    return textureManager.playerLeftStand;
                case 3:
                    return textureManager.playerRightStand;
                default:
                    return textureManager.playerDownStand;
            }
        }
    }

    /**
     * 计算攻击动画进度
     */
    private float getAttackAnimProgress(Player player) {
        float total = player.getAttackAnimTotalDuration();
        if (total <= 0)
            total = 0.2f;
        float elapsed = total - player.getAttackAnimTimer();
        return (elapsed / total) * 0.2f;
    }

    /**
     * 根据方向获取对应的动作名称
     *
     * @param baseAction 基础动作名称 (Move, Idle, Attack)
     * @param direction  方向 (0=下, 1=上, 2=左, 3=右)
     * @return 方向性动作名称
     */
    public static String getDirectionalAction(String baseAction, int direction) {
        switch (direction) {
            case 1:
                return baseAction + "Up";
            case 0:
                return baseAction + "Down";
            default:
                return baseAction;
        }
    }

    /**
     * 获取当前激活的玩家皮肤元素ID
     *
     * @return 第一个PLAYER类型的自定义元素ID，如果没有则返回null
     */
    public String getActivePlayerSkinId() {
        if (skinCacheValid) {
            return cachedPlayerSkinId;
        }

        cachedPlayerSkinId = null;
        for (CustomElementDefinition def : CustomElementManager.getInstance().getAllElements()) {
            if (def.getType() == ElementType.PLAYER) {
                cachedPlayerSkinId = def.getId();
                break;
            }
        }
        skinCacheValid = true;
        return cachedPlayerSkinId;
    }

    /**
     * 根据武器名称查找自定义武器元素ID
     */
    public static String findCustomWeaponId(String weaponName) {
        for (CustomElementDefinition def : CustomElementManager.getInstance().getAllElements()) {
            if (def.getType() == ElementType.WEAPON &&
                    def.getName().equalsIgnoreCase(weaponName)) {
                return def.getId();
            }
        }
        return null;
    }

    /**
     * 清除皮肤缓存（当自定义元素变更时调用）
     */
    public void invalidateSkinCache() {
        skinCacheValid = false;
    }

    /**
     * 武器渲染回调接口
     */
    @FunctionalInterface
    public interface WeaponRenderCallback {
        void renderWeapon(Player player, int direction, float stateTime);
    }
}
