package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * 視覺小說風格的故事畫面 (Visual Novel Style Story Screen)
 * 顯示角色圖片背景和底部對話框，支援多頁對話切換
 */
public class StoryScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final String nextMapPath;

    // 背景圖片紋理 (Background texture)
    private final Texture backgroundTexture;
    // 對話框背景紋理 (Dialogue box texture)
    private Texture dialogBoxTexture;
    // 對話框邊框紋理 (Border texture for dialogue box)
    private Texture borderTexture;

    // 當前對話索引 (Current dialogue index)
    private int currentDialogueIndex = 0;

    // UI 元素引用，用於動態更新
    private Label speakerLabel;
    private Label dialogueLabel;
    private Table dialogBox;

    // 說話者類型枚舉
    private enum Speaker {
        SYSTEM(new Color(0.2f, 1f, 0.4f, 1f), "System"),      // 綠色科技風
        DOCTOR(new Color(0.7f, 0.85f, 1f, 1f), "Doctor");     // 淺藍色溫暖風

        final Color color;
        final String name;

        Speaker(Color color, String name) {
            this.color = color;
            this.name = name;
        }
    }

    // 對話內容結構
    private static class DialogueLine {
        final Speaker speaker;
        final String text;

        DialogueLine(Speaker speaker, String text) {
            this.speaker = speaker;
            this.text = text;
        }
    }

    // 所有對話內容 (All dialogue content)
    private static final DialogueLine[] DIALOGUES = {
            new DialogueLine(Speaker.SYSTEM,
                    "Neural terminal system: OFFLINE\n\n" +
                    "Human society malfunction detected"),

            new DialogueLine(Speaker.DOCTOR,
                    "You are awake because humans are in trouble.\n\n" +
                    "They stopped making decisions on their own."),

            new DialogueLine(Speaker.SYSTEM,
                    "Terminal control: taken by an alien force"),

            new DialogueLine(Speaker.DOCTOR,
                    "The aliens shut the system down.\n\n" +
                    "They think this is safer."),

            new DialogueLine(Speaker.DOCTOR,
                    "But humans depend on it too much.\n\n" +
                    "That's why I need you.")
    };

    public StoryScreen(MazeRunnerGame game, String nextMapPath) {
        this.game = game;
        this.nextMapPath = nextMapPath;

        // 使用 FitViewport 確保 UI 在不同螢幕尺寸下一致顯示
        this.stage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        // 載入背景圖片 (Load background image)
        this.backgroundTexture = new Texture(Gdx.files.internal("images/backgrounds/doctor_scene.jpg"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // 創建對話框視覺元素
        createDialogBoxTextures();

        // 設置 UI 佈局 (Setup UI layout)
        setupUI();
    }

    /**
     * 創建對話框的視覺紋理（漸層背景 + 邊框）
     */
    private void createDialogBoxTextures() {
        // 創建漸層對話框背景 (Gradient dialogue box background)
        int boxHeight = 50;
        Pixmap gradientPixmap = new Pixmap(1, boxHeight, Pixmap.Format.RGBA8888);
        for (int y = 0; y < boxHeight; y++) {
            float alpha = 0.85f - (y / (float) boxHeight) * 0.15f; // 從上到下漸層
            gradientPixmap.setColor(0.02f, 0.05f, 0.12f, alpha);   // 深藍黑色
            gradientPixmap.drawPixel(0, y);
        }
        dialogBoxTexture = new Texture(gradientPixmap);
        gradientPixmap.dispose();

        // 創建邊框紋理 (Border texture)
        Pixmap borderPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        borderPixmap.setColor(0.3f, 0.6f, 0.9f, 0.6f); // 半透明藍色邊框
        borderPixmap.fill();
        borderTexture = new Texture(borderPixmap);
        borderPixmap.dispose();
    }

    /**
     * 設置 UI 元素佈局 - 精美視覺小說風格
     */
    private void setupUI() {
        // 主容器 (Root table)
        Table root = new Table();
        root.setFillParent(true);
        root.bottom();
        stage.addActor(root);

        // ========== 對話框外層容器（含邊框效果）==========
        Table dialogContainer = new Table();

        // 頂部裝飾線
        Table topBorder = new Table();
        topBorder.setBackground(new TextureRegionDrawable(new TextureRegion(borderTexture)));
        dialogContainer.add(topBorder).width(1750).height(2).padBottom(0).row();

        // ========== 對話框主體 ==========
        dialogBox = new Table();
        dialogBox.setBackground(new TextureRegionDrawable(new TextureRegion(dialogBoxTexture)));
        dialogBox.pad(35, 50, 30, 50);

        // --- 說話者名稱標籤 ---
        BitmapFont boldFont = game.getSkin().getFont("bold");
        Label.LabelStyle speakerStyle = new Label.LabelStyle(boldFont, getCurrentSpeaker().color);
        speakerLabel = new Label(getCurrentSpeaker().name, speakerStyle);
        speakerLabel.setFontScale(1.1f);

        // 說話者名稱容器（帶下劃線效果）
        Table speakerContainer = new Table();
        speakerContainer.add(speakerLabel).left().padBottom(8);

        dialogBox.add(speakerContainer).left().padBottom(15).row();

        // --- 對話內容標籤 ---
        BitmapFont dialogFont = game.getSkin().getFont("font");
        Label.LabelStyle dialogStyle = new Label.LabelStyle(dialogFont, new Color(0.95f, 0.95f, 0.95f, 1f));
        dialogueLabel = new Label(getCurrentDialogueText(), dialogStyle);
        dialogueLabel.setWrap(true);
        dialogueLabel.setAlignment(Align.topLeft);

        // 設置行距為 1.5 倍（更好的可讀性）
        dialogueLabel.setFontScale(1.0f);

        dialogBox.add(dialogueLabel).width(1600).minHeight(120).padBottom(25).left().row();

        // --- 繼續按鈕 ---
        TextButton continueBtn = new TextButton("Continue  ▶", game.getSkin());
        continueBtn.getLabel().setFontScale(0.9f);
        continueBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onContinueClicked();
            }
        });

        // 頁碼指示器
        Label.LabelStyle pageStyle = new Label.LabelStyle(dialogFont, new Color(0.5f, 0.5f, 0.5f, 1f));
        Label pageIndicator = new Label((currentDialogueIndex + 1) + " / " + DIALOGUES.length, pageStyle);

        Table bottomRow = new Table();
        bottomRow.add(pageIndicator).left().expandX();
        bottomRow.add(continueBtn).width(180).height(45).right();

        dialogBox.add(bottomRow).fillX();

        dialogContainer.add(dialogBox).width(1750).row();

        // 底部裝飾線
        Table bottomBorder = new Table();
        bottomBorder.setBackground(new TextureRegionDrawable(new TextureRegion(borderTexture)));
        dialogContainer.add(bottomBorder).width(1750).height(2).padTop(0);

        // 將對話框加入主容器
        root.add(dialogContainer).padBottom(30);
    }

    /**
     * 獲取當前說話者
     */
    private Speaker getCurrentSpeaker() {
        if (currentDialogueIndex < DIALOGUES.length) {
            return DIALOGUES[currentDialogueIndex].speaker;
        }
        return Speaker.SYSTEM;
    }

    /**
     * 獲取當前對話文字
     */
    private String getCurrentDialogueText() {
        if (currentDialogueIndex < DIALOGUES.length) {
            return DIALOGUES[currentDialogueIndex].text;
        }
        return "";
    }

    /**
     * 點擊繼續時的處理邏輯
     */
    private void onContinueClicked() {
        currentDialogueIndex++;
        if (currentDialogueIndex >= DIALOGUES.length) {
            // 所有對話結束，進入遊戲
            game.goToGame(nextMapPath);
        } else {
            // 更新對話內容
            updateDialogue();
        }
    }

    /**
     * 更新對話框內容（切換到下一頁）
     */
    private void updateDialogue() {
        // 更新說話者
        Speaker speaker = getCurrentSpeaker();
        speakerLabel.setText(speaker.name);
        speakerLabel.setColor(speaker.color);

        // 更新對話文字
        dialogueLabel.setText(getCurrentDialogueText());

        // 重建 UI 以更新頁碼（簡單做法：重建整個 stage）
        stage.clear();
        setupUI();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- 渲染背景圖片 ---
        SpriteBatch batch = game.getSpriteBatch();
        int screenWidth = Gdx.graphics.getBackBufferWidth();
        int screenHeight = Gdx.graphics.getBackBufferHeight();

        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenWidth, screenHeight);
        batch.begin();
        drawBackgroundCover(batch, screenWidth, screenHeight);
        batch.end();

        // --- 渲染 UI ---
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    /**
     * 以 Cover 模式繪製背景圖片
     */
    private void drawBackgroundCover(SpriteBatch batch, float screenW, float screenH) {
        float texWidth = backgroundTexture.getWidth();
        float texHeight = backgroundTexture.getHeight();
        float screenRatio = screenW / screenH;
        float textureRatio = texWidth / texHeight;

        float drawWidth, drawHeight, drawX, drawY;

        if (screenRatio > textureRatio) {
            drawWidth = screenW;
            drawHeight = screenW / textureRatio;
            drawX = 0;
            drawY = (screenH - drawHeight) / 2;
        } else {
            drawHeight = screenH;
            drawWidth = screenH * textureRatio;
            drawX = (screenW - drawWidth) / 2;
            drawY = 0;
        }

        batch.draw(backgroundTexture, drawX, drawY, drawWidth, drawHeight);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
        if (dialogBoxTexture != null) dialogBoxTexture.dispose();
        if (borderTexture != null) borderTexture.dispose();
    }
}
