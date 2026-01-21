package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.model.DamageType;
import de.tum.cit.fop.maze.model.items.MagicalArmor;
import de.tum.cit.fop.maze.model.items.PhysicalArmor;
import de.tum.cit.fop.maze.utils.AudioManager;
import de.tum.cit.fop.maze.utils.MapLoader;
import de.tum.cit.fop.maze.utils.UIConstants;

/**
 * 护甲选择界面 - 重新设计版 (Armor Selection Screen - Redesigned)
 * 
 * 采用现代玻璃形态设计，配合AI生成的像素风格背景，提供世界级视觉体验。
 * Features:
 * - AI generated pixel art background
 * - Glassmorphism card design with gradient borders
 * - Glowing effect for recommended armor
 * - Smooth hover animations
 */
public class ArmorSelectScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final String mapPath;
    private final MapLoader.LevelConfig levelConfig;
    private Texture backgroundTexture;

    // Animation states
    private float pulseTime = 0f;

    public ArmorSelectScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;
        this.mapPath = mapPath;
        this.stage = new Stage(new FitViewport(UIConstants.VIEWPORT_WIDTH, UIConstants.VIEWPORT_HEIGHT),
                game.getSpriteBatch());

        // Load level config to get suggested armor
        MapLoader.LoadResult result = MapLoader.loadMapWithConfig(mapPath);
        this.levelConfig = result.config;

        loadBackground();
        setupUI();
    }

    private void loadBackground() {
        try {
            backgroundTexture = new Texture(Gdx.files.internal("images/backgrounds/armor_select_bg.png"));
        } catch (Exception e) {
            Gdx.app.error("ArmorSelectScreen", "Failed to load background, using fallback", e);
            // Fallback to solid color
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(UIConstants.BG_COLOR_DEFAULT);
            pm.fill();
            backgroundTexture = new Texture(pm);
            pm.dispose();
        }
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        // Dark overlay for better text readability
        root.setBackground(createColorDrawable(new Color(0f, 0f, 0f, 0.55f)));
        root.pad(40);
        stage.addActor(root);

        // ============ Header Section ============
        buildHeader(root);

        // ============ Main Armor Cards ============
        buildArmorCards(root);

        // ============ Footer Buttons ============
        buildFooter(root);
    }

    private void buildHeader(Table root) {
        // Main Title with gradient effect simulation
        Label title = new Label("CHOOSE YOUR ARMOR", game.getSkin(), "title");
        title.setColor(UIConstants.VICTORY_GOLD);
        title.setFontScale(1.0f);
        root.add(title).padBottom(20).row();

        // Threat Warning Banner
        Table warningBanner = new Table();
        warningBanner.setBackground(createGradientBannerBackground());
        warningBanner.pad(12, 30, 12, 30);

        String dangerType = levelConfig.damageType == DamageType.MAGICAL ? "MAGICAL" : "PHYSICAL";
        Color dangerColor = levelConfig.damageType == DamageType.MAGICAL
                ? UIConstants.DANGER_MAGICAL
                : UIConstants.DANGER_PHYSICAL;

        Label warningIcon = new Label("⚠", game.getSkin());
        warningIcon.setColor(dangerColor);
        warningIcon.setFontScale(1.2f);
        warningBanner.add(warningIcon).padRight(10);

        Label warningText = new Label("This area has " + dangerType + " threats!", game.getSkin());
        warningText.setColor(Color.WHITE);
        warningText.setFontScale(0.95f);
        warningBanner.add(warningText);

        root.add(warningBanner).padBottom(15).row();

        // Recommendation Label
        String suggestedName = levelConfig.suggestedArmor == DamageType.MAGICAL
                ? "Arcane Robe (Magical)"
                : "Iron Shield (Physical)";
        Label suggestionLabel = new Label("Recommended: " + suggestedName, game.getSkin());
        suggestionLabel.setColor(UIConstants.ARMOR_RECOMMENDED_GLOW);
        suggestionLabel.setFontScale(0.8f);
        root.add(suggestionLabel).padBottom(20).row();
    }

    private void buildArmorCards(Table root) {
        Table cardsContainer = new Table();
        cardsContainer.defaults().pad(10);

        // Physical Armor Card
        Table physicalCard = createArmorCard(
                "Iron Shield",
                "Physical Armor",
                "Heavy steel protection\nBlocks swords, arrows & melee attacks",
                "⚔",
                UIConstants.ARMOR_PHYSICAL_COLOR,
                DamageType.PHYSICAL,
                levelConfig.suggestedArmor == DamageType.PHYSICAL);
        cardsContainer.add(physicalCard);

        // VS Divider
        Label vsLabel = new Label("VS", game.getSkin(), "title");
        vsLabel.setColor(new Color(0.5f, 0.5f, 0.55f, 0.6f));
        vsLabel.setFontScale(1.0f);
        cardsContainer.add(vsLabel).padLeft(15).padRight(15);

        // Magical Armor Card
        Table magicalCard = createArmorCard(
                "Arcane Robe",
                "Magical Armor",
                "Enchanted mystic barrier\nBlocks spells, fireballs & magic",
                "✦",
                UIConstants.ARMOR_MAGICAL_COLOR,
                DamageType.MAGICAL,
                levelConfig.suggestedArmor == DamageType.MAGICAL);
        cardsContainer.add(magicalCard);

        root.add(cardsContainer).padBottom(20).row();
    }

    private Table createArmorCard(String name, String type, String description,
            String icon, Color themeColor, DamageType armorType, boolean isRecommended) {

        final Table card = new Table();
        card.pad(15, 25, 15, 25);

        // Apply glassmorphism background with gradient border
        if (isRecommended) {
            card.setBackground(createGlowingCardBackground(themeColor, UIConstants.ARMOR_RECOMMENDED_GLOW));
        } else {
            card.setBackground(createGlassCardBackground(themeColor));
        }

        // Armor Type Label (category)
        Label typeLabel = new Label(type.toUpperCase(), game.getSkin());
        typeLabel.setColor(themeColor);
        typeLabel.setFontScale(0.7f);
        typeLabel.setAlignment(Align.center);
        card.add(typeLabel).padBottom(10).row();

        // Armor Name (主要标题)
        Label nameLabel = new Label(name, game.getSkin(), "title");
        nameLabel.setColor(Color.WHITE);
        nameLabel.setFontScale(0.9f);
        nameLabel.setAlignment(Align.center);
        card.add(nameLabel).padBottom(12).row();

        // Description (增大字体使其更易读)
        Label descLabel = new Label(description, game.getSkin());
        descLabel.setColor(UIConstants.VICTORY_TEXT_DIM);
        descLabel.setFontScale(0.75f);
        descLabel.setAlignment(Align.center);
        descLabel.setWrap(true);
        card.add(descLabel).width(200).padBottom(12).row();

        // Recommended Badge
        if (isRecommended) {
            Table badgeContainer = new Table();
            badgeContainer.setBackground(createBadgeBackground());
            badgeContainer.pad(5, 12, 5, 12);

            Label badgeLabel = new Label("★ RECOMMENDED", game.getSkin());
            badgeLabel.setColor(UIConstants.ARMOR_RECOMMENDED_GLOW);
            badgeLabel.setFontScale(0.6f);
            badgeContainer.add(badgeLabel);

            card.add(badgeContainer).padBottom(10).row();
        }

        // Equip Button
        TextButton equipBtn = new TextButton("Equip", game.getSkin());
        equipBtn.setColor(themeColor);
        equipBtn.getLabel().setFontScale(0.75f);
        equipBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                AudioManager.getInstance().playSound("select");
                startGame(armorType);
            }
        });

        // Hover effect for button
        equipBtn.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                equipBtn.addAction(Actions.scaleTo(1.05f, 1.05f, 0.1f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                equipBtn.addAction(Actions.scaleTo(1f, 1f, 0.1f));
            }
        });

        card.add(equipBtn).width(130).height(40);

        // Card hover effect
        card.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                card.addAction(Actions.scaleTo(1.02f, 1.02f, 0.15f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                card.addAction(Actions.scaleTo(1f, 1f, 0.15f));
            }
        });

        return card;
    }

    private void buildFooter(Table root) {
        Table footer = new Table();
        footer.defaults().pad(0, 10, 0, 10);

        // Continue without armor
        TextButton noArmorBtn = new TextButton("No Armor", game.getSkin());
        noArmorBtn.setColor(UIConstants.BTN_SECONDARY_BORDER);
        noArmorBtn.getLabel().setFontScale(0.95f);
        noArmorBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startGame(null);
            }
        });
        footer.add(noArmorBtn).width(200).height(58);

        // Back to Menu
        TextButton backBtn = new TextButton("Menu", game.getSkin());
        backBtn.setColor(UIConstants.BTN_SECONDARY_BORDER);
        backBtn.getLabel().setFontScale(0.95f);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        footer.add(backBtn).width(160).height(58);

        root.add(footer);
    }

    // ==================== Helper Methods ====================

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
        pm.dispose();
        return drawable;
    }

    /**
     * 创建玻璃形态卡片背景 (Glassmorphism Card Background)
     */
    private NinePatchDrawable createGlassCardBackground(Color borderColor) {
        int size = 24;
        int borderThickness = 2;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Fill with glass background
        Color bgColor = UIConstants.ARMOR_CARD_BG;
        pm.setColor(bgColor);
        pm.fill();

        // Draw border
        pm.setColor(new Color(borderColor.r, borderColor.g, borderColor.b, 0.5f));
        // Top
        pm.fillRectangle(0, 0, size, borderThickness);
        // Bottom
        pm.fillRectangle(0, size - borderThickness, size, borderThickness);
        // Left
        pm.fillRectangle(0, 0, borderThickness, size);
        // Right
        pm.fillRectangle(size - borderThickness, 0, borderThickness, size);

        Texture texture = new Texture(pm);
        pm.dispose();

        NinePatch patch = new NinePatch(texture, borderThickness + 2, borderThickness + 2,
                borderThickness + 2, borderThickness + 2);
        return new NinePatchDrawable(patch);
    }

    /**
     * 创建发光效果卡片背景 (Glowing Card Background for Recommended)
     */
    private NinePatchDrawable createGlowingCardBackground(Color borderColor, Color glowColor) {
        int size = 32;
        int borderThickness = 3;
        int glowThickness = 2;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Fill with glass background
        pm.setColor(UIConstants.ARMOR_CARD_BG);
        pm.fill();

        // Draw outer glow (subtle)
        pm.setColor(new Color(glowColor.r, glowColor.g, glowColor.b, 0.15f));
        pm.fillRectangle(0, 0, size, glowThickness);
        pm.fillRectangle(0, size - glowThickness, size, glowThickness);
        pm.fillRectangle(0, 0, glowThickness, size);
        pm.fillRectangle(size - glowThickness, 0, glowThickness, size);

        // Draw inner border (gradient effect - top/left lighter)
        pm.setColor(new Color(glowColor.r, glowColor.g, glowColor.b, 0.7f));
        pm.fillRectangle(glowThickness, glowThickness, size - 2 * glowThickness, borderThickness);
        pm.fillRectangle(glowThickness, glowThickness, borderThickness, size - 2 * glowThickness);

        // Bottom/right slightly darker
        pm.setColor(new Color(borderColor.r, borderColor.g, borderColor.b, 0.6f));
        pm.fillRectangle(glowThickness, size - glowThickness - borderThickness,
                size - 2 * glowThickness, borderThickness);
        pm.fillRectangle(size - glowThickness - borderThickness, glowThickness,
                borderThickness, size - 2 * glowThickness);

        Texture texture = new Texture(pm);
        pm.dispose();

        NinePatch patch = new NinePatch(texture, glowThickness + borderThickness + 2,
                glowThickness + borderThickness + 2,
                glowThickness + borderThickness + 2,
                glowThickness + borderThickness + 2);
        return new NinePatchDrawable(patch);
    }

    /**
     * 创建警告横幅渐变背景
     */
    private NinePatchDrawable createGradientBannerBackground() {
        int width = 32;
        int height = 16;
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        // Horizontal gradient from transparent to semi-transparent
        for (int x = 0; x < width; x++) {
            float ratio = (float) x / width;
            float alpha = 0.15f + 0.35f * (1f - Math.abs(ratio - 0.5f) * 2); // Center brighter
            pm.setColor(new Color(0.1f, 0.1f, 0.15f, alpha));
            for (int y = 0; y < height; y++) {
                pm.drawPixel(x, y);
            }
        }

        Texture texture = new Texture(pm);
        pm.dispose();

        NinePatch patch = new NinePatch(texture, 8, 8, 4, 4);
        return new NinePatchDrawable(patch);
    }

    /**
     * 创建推荐徽章背景
     */
    private NinePatchDrawable createBadgeBackground() {
        int size = 16;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Dark semi-transparent background with golden tint
        pm.setColor(new Color(0.15f, 0.12f, 0.05f, 0.85f));
        pm.fill();

        // Golden border
        pm.setColor(new Color(UIConstants.ARMOR_RECOMMENDED_GLOW.r,
                UIConstants.ARMOR_RECOMMENDED_GLOW.g,
                UIConstants.ARMOR_RECOMMENDED_GLOW.b, 0.5f));
        pm.drawRectangle(0, 0, size, size);

        Texture texture = new Texture(pm);
        pm.dispose();

        NinePatch patch = new NinePatch(texture, 3, 3, 3, 3);
        return new NinePatchDrawable(patch);
    }

    private void startGame(DamageType selectedArmorType) {
        // 通过 LoadingScreen 进入游戏，显示加载进度条并预加载所有动画
        game.setScreen(new LoadingScreen(game, mapPath, selectedArmorType, levelConfig.damageType));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // Update pulse animation
        pulseTime += delta;

        // Clear screen
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background image using Cover mode (same as MenuScreen)
        if (backgroundTexture != null) {
            com.badlogic.gdx.graphics.g2d.SpriteBatch batch = game.getSpriteBatch();

            // Get actual screen size - use backbuffer size for correctness
            int screenWidth = Gdx.graphics.getBackBufferWidth();
            int screenHeight = Gdx.graphics.getBackBufferHeight();

            // Reset GL Viewport to full screen
            Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);

            // Set projection matrix to screen pixel coordinate system
            batch.getProjectionMatrix().setToOrtho2D(0, 0, screenWidth, screenHeight);
            batch.begin();
            batch.setColor(0.7f, 0.7f, 0.7f, 1f); // Slightly dimmed

            // Background texture original size
            float texWidth = backgroundTexture.getWidth();
            float texHeight = backgroundTexture.getHeight();

            // Calculate Cover mode scale ratio
            float screenRatio = (float) screenWidth / screenHeight;
            float textureRatio = texWidth / texHeight;

            float drawWidth, drawHeight;
            float drawX, drawY;

            if (screenRatio > textureRatio) {
                // Screen is wider, fit to width
                drawWidth = screenWidth;
                drawHeight = screenWidth / textureRatio;
                drawX = 0;
                drawY = (screenHeight - drawHeight) / 2;
            } else {
                // Screen is taller, fit to height
                drawHeight = screenHeight;
                drawWidth = screenHeight * textureRatio;
                drawX = (screenWidth - drawWidth) / 2;
                drawY = 0;
            }

            batch.draw(backgroundTexture, drawX, drawY, drawWidth, drawHeight);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // Restore Stage's Viewport
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }
}
