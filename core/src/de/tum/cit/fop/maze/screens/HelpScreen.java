package de.tum.cit.fop.maze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.config.GameSettings;
import de.tum.cit.fop.maze.utils.UIConstants;
import de.tum.cit.fop.maze.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Help Screen - 游戏帮助界面 (Operator's Manual)
 * 提供游戏玩法、操作、系统的完整说明书。
 */
public class HelpScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Skin skin;
    private Texture backgroundTexture;

    // Layout
    private Table contentTable;
    private ScrollPane contentScrollPane;
    private TextButton[] navButtons;

    // State
    private int currentSection = 0;
    private int currentZoneIndex = 0;

    // 统一的内容宽度
    private static final float CONTENT_WIDTH = 1200f;

    // Navigation sections
    private static final String[] NAV_ITEMS = { "Controls", "Weapons", "Armor", "World", "Tips" };

    // Weapon data
    private static final String[][] WEAPONS = {
            { "Iron Sword", "Melee", "PHYSICAL", "1", "2.2", "0.3s", "None" },
            { "Ice Bow", "Ranged", "PHYSICAL", "1", "5.0", "0.8s", "Freeze" },
            { "Fire Staff", "Ranged", "MAGICAL", "2", "3.0", "1.2s", "Burn DoT" },
            { "Crossbow", "Ranged", "PHYSICAL", "2", "6.0", "1.5s", "High Penetration" },
            { "Magic Wand", "Ranged", "MAGICAL", "1", "5.0", "1.0s", "Burn" }
    };

    // Armor data
    private static final String[][] ARMORS = {
            { "Iron Shield", "PHYSICAL", "5", "80g", "vs Slimes, Scorpions" },
            { "Arcane Robe", "MAGICAL", "4", "80g", "vs Mages" },
            { "Knight's Plate", "PHYSICAL", "8", "150g", "Tank Build" },
            { "Wizard's Cloak", "MAGICAL", "7", "150g", "Late Game Essential" }
    };

    // Zone data
    private static final String[][] ZONES = {
            { "The Primitive Forest", "1-4", "Grassland", "Easy",
                    "The journey begins in the overgrown ruins.",
                    "Basic navigation, Key hunting, Slow Slimes" },
            { "The Scorched Sands", "5-8", "Desert", "Normal",
                    "An immense desert where the sun never sets.",
                    "Quicksand (slow), Spike Traps, Fast Scorpions" },
            { "The Frozen Tundra", "9-12", "Ice", "Hard",
                    "Bitter cold freezes the very air.",
                    "Ice Floor (slide), Blizzard (fog), Yetis" },
            { "The Toxic Jungle", "13-16", "Jungle", "Very Hard",
                    "A bio-hazardous zone filled with mutated flora.",
                    "Poison Gas (DoT), Teleporters, Fast Spiders" },
            { "The Orbital Station", "17-20", "Space", "Extreme",
                    "The final frontier. Survival is mandatory.",
                    "Laser Gates, Electric Floors, Security Drones" }
    };

    // Hazards
    private static final String[][] HAZARDS = {
            { "Spike Trap", "Zone 2+", "1 HP", "Periodic activation" },
            { "Quicksand", "Zone 2", "-", "50% speed reduction" },
            { "Ice Floor", "Zone 3", "-", "Slide until wall" },
            { "Blizzard", "Zone 3", "-", "Limited visibility" },
            { "Poison Gas", "Zone 4", "Continuous", "Area DoT" },
            { "Teleporter", "Zone 4", "-", "Teleport player" },
            { "Electric Floor", "Zone 5", "2 HP", "Periodic discharge" },
            { "Laser Gate", "Zone 5", "Instant", "Needs switch" }
    };

    private final List<Texture> managedTextures = new ArrayList<>();

    public HelpScreen(MazeRunnerGame game) {
        this.game = game;
        this.skin = game.getSkin();

        // Load background
        try {
            backgroundTexture = new Texture(Gdx.files.internal("help_bg.png"));
        } catch (Exception e) {
            backgroundTexture = null;
        }

        // 使用与其他Screen完全相同的viewport设置
        stage = new Stage(new FitViewport(1920, 1080), game.getSpriteBatch());

        buildUI();
        refreshContent();
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        stage.addActor(root);

        // ===== Header =====
        Table headerTable = new Table();
        headerTable.pad(15);

        TextButton backBtn = new TextButton("Back", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        headerTable.add(backBtn).width(120).height(45).left().padRight(40);

        Label titleLabel = new Label("OPERATOR'S MANUAL", skin, "title");
        titleLabel.setColor(UIConstants.HELP_BORDER_CYAN);
        headerTable.add(titleLabel).expandX().center();

        headerTable.add().width(160); // spacer for symmetry
        root.add(headerTable).fillX().padBottom(10).row();

        // ===== Main Area: Left Nav + Right Content =====
        Table mainTable = new Table();

        // ----- Left Navigation Panel -----
        Table navPanel = new Table();
        navPanel.top().pad(15);
        navPanel.setBackground(createColorDrawable(new Color(0.08f, 0.08f, 0.12f, 0.95f)));

        Label navTitle = new Label("SECTIONS", skin);
        navTitle.setColor(UIConstants.HELP_BORDER_CYAN);
        navPanel.add(navTitle).padBottom(20).row();

        navButtons = new TextButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final int index = i;
            TextButton btn = new TextButton(NAV_ITEMS[i], skin);
            btn.getLabel().setAlignment(Align.left);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    currentSection = index;
                    updateNavSelection();
                    refreshContent();
                }
            });
            navButtons[i] = btn;
            navPanel.add(btn).width(180).height(50).padBottom(10).row();
        }

        // Spacer to push buttons to top
        navPanel.add().expandY();

        // ----- Right Content Area -----
        contentTable = new Table();
        contentTable.top().left();
        contentTable.pad(25);

        contentScrollPane = new ScrollPane(contentTable, skin);
        contentScrollPane.setFadeScrollBars(false);
        contentScrollPane.setScrollingDisabled(true, false);
        UIUtils.enableHoverScrollFocus(contentScrollPane, stage);

        // Add panels to main table
        mainTable.add(navPanel).width(220).fillY().top();
        mainTable.add(contentScrollPane).expand().fill().top();

        root.add(mainTable).expand().fill().pad(10).row();

        // ===== Footer =====
        Table footerTable = new Table();
        footerTable.pad(10);

        TextButton prevBtn = new TextButton("< Prev", skin);
        prevBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (currentSection > 0) {
                    currentSection--;
                    updateNavSelection();
                    refreshContent();
                }
            }
        });
        footerTable.add(prevBtn).width(120).height(40).padRight(30);

        // Progress dots
        Table dots = new Table();
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            Label dot = new Label(i == currentSection ? " [X] " : " [ ] ", skin);
            dot.setColor(i == currentSection ? UIConstants.HELP_BORDER_CYAN : Color.GRAY);
            dots.add(dot);
        }
        footerTable.add(dots).expandX().center();

        TextButton nextBtn = new TextButton("Next >", skin);
        nextBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (currentSection < NAV_ITEMS.length - 1) {
                    currentSection++;
                    updateNavSelection();
                    refreshContent();
                }
            }
        });
        footerTable.add(nextBtn).width(120).height(40).padLeft(30);

        root.add(footerTable).fillX();

        updateNavSelection();
    }

    private void updateNavSelection() {
        for (int i = 0; i < navButtons.length; i++) {
            if (i == currentSection) {
                navButtons[i].setColor(UIConstants.HELP_BORDER_CYAN);
            } else {
                navButtons[i].setColor(Color.WHITE);
            }
        }
    }

    private void refreshContent() {
        contentTable.clearChildren();
        contentScrollPane.setScrollY(0);

        switch (currentSection) {
            case 0:
                buildControlsContent();
                break;
            case 1:
                buildWeaponsContent();
                break;
            case 2:
                buildArmorContent();
                break;
            case 3:
                buildWorldContent();
                break;
            case 4:
                buildTipsContent();
                break;
        }
    }

    // ==================== Controls Section ====================

    private void buildControlsContent() {
        addSectionTitle("BASIC CONTROLS");

        // Movement
        addSubTitle("Movement");
        Table moveCard = createCard();

        String keyUp = Input.Keys.toString(GameSettings.KEY_UP);
        String keyDown = Input.Keys.toString(GameSettings.KEY_DOWN);
        String keyLeft = Input.Keys.toString(GameSettings.KEY_LEFT);
        String keyRight = Input.Keys.toString(GameSettings.KEY_RIGHT);

        addKeyRow(moveCard, keyUp, "Move Up");
        addKeyRow(moveCard, keyDown, "Move Down");
        addKeyRow(moveCard, keyLeft, "Move Left");
        addKeyRow(moveCard, keyRight, "Move Right");
        addKeyRow(moveCard, "SHIFT", "Sprint (Hold)");

        contentTable.add(moveCard).width(CONTENT_WIDTH).padBottom(20).row();
        addTip("TIP: Customize keys in Settings > Controls");

        // Combat
        addSubTitle("Combat");
        Table combatCard = createCard();

        String keyAttack = Input.Keys.toString(GameSettings.KEY_ATTACK);
        String keySwitch = Input.Keys.toString(GameSettings.KEY_SWITCH_WEAPON);

        addKeyRow(combatCard, keyAttack, "Primary Attack");
        addKeyRow(combatCard, "1-5", "Weapon Quick Slots");
        addKeyRow(combatCard, keySwitch, "Cycle Weapon");

        contentTable.add(combatCard).width(CONTENT_WIDTH).padBottom(20).row();
        addWarning("Attacks have cooldown. Watch the cooldown bar!");

        // Other
        addSubTitle("Other Controls");
        Table otherCard = createCard();

        addKeyRow(otherCard, "ESC", "Pause Menu");
        addKeyRow(otherCard, "`", "Dev Console (Cheat Mode)");

        contentTable.add(otherCard).width(CONTENT_WIDTH).row();
    }

    private void addKeyRow(Table card, String key, String desc) {
        Table row = new Table();
        row.left();

        Label keyLabel = new Label("[" + key + "]", skin);
        keyLabel.setColor(UIConstants.HELP_BORDER_CYAN);
        row.add(keyLabel).width(180).left().padRight(30);

        row.add(new Label(desc, skin)).expandX().left();

        card.add(row).width(CONTENT_WIDTH - 60).left().padBottom(12).row();
    }

    // ==================== Weapons Section ====================

    private void buildWeaponsContent() {
        addSectionTitle("WEAPON ARSENAL");
        addTip("Match weapon damage type to enemy weaknesses.");

        for (String[] w : WEAPONS) {
            Table card = createCard();

            // Name
            Label nameLabel = new Label(w[0], skin);
            nameLabel.setColor(UIConstants.HELP_TITLE_GOLD);
            card.add(nameLabel).left().padBottom(8).row();

            // Type info
            Label typeLabel = new Label("Type: " + w[1] + "  |  Damage Type: " + w[2], skin);
            typeLabel.setColor(Color.LIGHT_GRAY);
            card.add(typeLabel).left().padBottom(8).row();

            // Stats row
            Table stats = new Table();
            stats.left();
            stats.add(new Label("Damage: " + w[3], skin)).padRight(50);
            stats.add(new Label("Range: " + w[4], skin)).padRight(50);
            stats.add(new Label("Cooldown: " + w[5], skin)).padRight(50);

            Label effectLabel = new Label("Effect: " + w[6], skin);
            effectLabel.setColor(w[6].equals("None") ? Color.GRAY : Color.CYAN);
            stats.add(effectLabel);

            card.add(stats).left();
            contentTable.add(card).width(CONTENT_WIDTH).padBottom(15).row();
        }
    }

    // ==================== Armor Section ====================

    private void buildArmorContent() {
        addSectionTitle("ARMOR PROTECTION SYSTEM");

        // Mechanism
        Table mechCard = createCard();
        mechCard.add(new Label("DAMAGE FLOW:", skin)).left().padBottom(10).row();
        mechCard.add(new Label("1. Enemy attacks with PHYSICAL or MAGICAL damage", skin)).left().padBottom(5).row();
        mechCard.add(new Label("2. If armor matches damage type, shield absorbs it", skin)).left().padBottom(5).row();
        mechCard.add(new Label("3. Remaining or unmatched damage hits your HP", skin)).left();
        contentTable.add(mechCard).width(CONTENT_WIDTH).padBottom(20).row();

        addWarning("Physical armor does NOT block magic damage!");

        addSubTitle("Available Armors");

        for (String[] a : ARMORS) {
            Table card = createCard();

            // Name
            Label nameLabel = new Label(a[0], skin);
            nameLabel.setColor(UIConstants.HELP_TITLE_GOLD);
            card.add(nameLabel).left().padBottom(8).row();

            // Resist type
            Label resistLabel = new Label("Resists: " + a[1], skin);
            resistLabel.setColor(a[1].equals("PHYSICAL") ? Color.ORANGE : new Color(0.8f, 0.4f, 1f, 1f));
            card.add(resistLabel).left().padBottom(8).row();

            // Stats
            Table stats = new Table();
            stats.left();
            stats.add(new Label("Shield: " + a[2], skin)).padRight(60);
            stats.add(new Label("Price: " + a[3], skin)).padRight(60);

            Label recLabel = new Label("Best for: " + a[4], skin);
            recLabel.setColor(Color.GREEN);
            stats.add(recLabel);

            card.add(stats).left();
            contentTable.add(card).width(CONTENT_WIDTH).padBottom(15).row();
        }
    }

    // ==================== World Section ====================

    private void buildWorldContent() {
        addSectionTitle("EXPLORE THE WORLD");

        // Zone navigation
        Table navRow = new Table();

        TextButton prevZone = new TextButton("< Prev Zone", skin);
        prevZone.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentZoneIndex = (currentZoneIndex - 1 + ZONES.length) % ZONES.length;
                refreshContent();
            }
        });
        navRow.add(prevZone).width(140).height(40).padRight(40);

        Label indicator = new Label("Zone " + (currentZoneIndex + 1) + " / " + ZONES.length, skin);
        indicator.setColor(UIConstants.HELP_BORDER_CYAN);
        navRow.add(indicator).expandX().center();

        TextButton nextZone = new TextButton("Next Zone >", skin);
        nextZone.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentZoneIndex = (currentZoneIndex + 1) % ZONES.length;
                refreshContent();
            }
        });
        navRow.add(nextZone).width(140).height(40).padLeft(40);

        contentTable.add(navRow).width(CONTENT_WIDTH).padBottom(20).row();

        // Current zone card
        String[] z = ZONES[currentZoneIndex];
        Table zoneCard = createCard();

        Label zoneName = new Label("ZONE " + (currentZoneIndex + 1) + ": " + z[0], skin);
        zoneName.setColor(UIConstants.HELP_TITLE_GOLD);
        zoneCard.add(zoneName).left().padBottom(12).row();

        Table infoRow = new Table();
        infoRow.left();
        infoRow.add(new Label("Levels: " + z[1], skin)).padRight(50);
        infoRow.add(new Label("Theme: " + z[2], skin)).padRight(50);
        Label diffLabel = new Label("Difficulty: " + z[3], skin);
        diffLabel.setColor(Color.YELLOW);
        infoRow.add(diffLabel);
        zoneCard.add(infoRow).left().padBottom(12).row();

        Label lore = new Label("\"" + z[4] + "\"", skin);
        lore.setColor(Color.LIGHT_GRAY);
        zoneCard.add(lore).left().padBottom(12).row();

        Label features = new Label("Features: " + z[5], skin);
        features.setWrap(true);
        zoneCard.add(features).width(CONTENT_WIDTH - 60).left();

        contentTable.add(zoneCard).width(CONTENT_WIDTH).padBottom(25).row();

        // Hazards
        addSubTitle("Hazards & Traps");

        // Header
        Table headerRow = createCard();
        Table hdr = new Table();
        hdr.left();
        hdr.add(createHeaderLabel("Name")).width(200);
        hdr.add(createHeaderLabel("Location")).width(150);
        hdr.add(createHeaderLabel("Damage")).width(150);
        hdr.add(createHeaderLabel("Effect")).expandX().left();
        headerRow.add(hdr).expandX().fillX();
        contentTable.add(headerRow).width(CONTENT_WIDTH).padBottom(5).row();

        for (String[] h : HAZARDS) {
            Table row = createCard();
            Table r = new Table();
            r.left();

            Label nameL = new Label(h[0], skin);
            nameL.setColor(UIConstants.HELP_WARN);
            r.add(nameL).width(200).left();
            r.add(new Label(h[1], skin)).width(150).left();
            r.add(new Label(h[2], skin)).width(150).left();
            r.add(new Label(h[3], skin)).expandX().left();

            row.add(r).expandX().fillX();
            contentTable.add(row).width(CONTENT_WIDTH).padBottom(5).row();
        }
    }

    private Label createHeaderLabel(String text) {
        Label l = new Label(text, skin);
        l.setColor(UIConstants.HELP_BORDER_CYAN);
        return l;
    }

    // ==================== Tips Section ====================

    private void buildTipsContent() {
        addSectionTitle("PRO TIPS & STRATEGIES");

        String[][] tips = {
                { "1. Never get surrounded",
                        "Use corridors to line up enemies. Fight with your back to a wall." },
                { "2. Match gear to zone",
                        "Zone 1-2: Physical armor. Zone 3+: Consider magic defense." },
                { "3. Use navigation arrow",
                        "Arrow points to exit. Enable Cheat Mode to also see key location." },
                { "4. Conserve health",
                        "Only 3 lives per run. Think before rushing." },
                { "5. Shop wisely",
                        "Check next zone enemies before buying. Armor upgrades are priority." }
        };

        for (String[] tip : tips) {
            Table card = createCard();

            Label title = new Label(tip[0], skin);
            title.setColor(UIConstants.HELP_TITLE_GOLD);
            card.add(title).left().padBottom(8).row();

            Label desc = new Label(tip[1], skin);
            desc.setWrap(true);
            desc.setColor(Color.LIGHT_GRAY);
            card.add(desc).width(CONTENT_WIDTH - 60).left();

            contentTable.add(card).width(CONTENT_WIDTH).padBottom(15).row();
        }

        addSubTitle("Speedrun Tips");
        Table speedCard = createCard();
        speedCard.add(new Label("- Sprint (SHIFT) to rush objectives", skin)).left().padBottom(8).row();
        speedCard.add(new Label("- Lure enemies away, then circle back", skin)).left().padBottom(8).row();
        speedCard.add(new Label("- Number keys switch weapons instantly", skin)).left();
        contentTable.add(speedCard).width(CONTENT_WIDTH).padBottom(20).row();

        // Secret
        Table secret = new Table();
        secret.setBackground(createColorDrawable(new Color(0.18f, 0.12f, 0.05f, 0.9f)));
        secret.pad(18);
        Label secretL = new Label("HIDDEN: Play between 2-4 AM for secrets...", skin);
        secretL.setColor(UIConstants.RARITY_LEGENDARY);
        secret.add(secretL);
        contentTable.add(secret).width(CONTENT_WIDTH).row();
    }

    // ==================== UI Helpers ====================

    private void addSectionTitle(String text) {
        Label l = new Label(text, skin, "title");
        l.setColor(UIConstants.HELP_BORDER_CYAN);
        contentTable.add(l).left().padBottom(25).row();
    }

    private void addSubTitle(String text) {
        Label l = new Label("--- " + text + " ---", skin);
        l.setColor(UIConstants.HELP_TITLE_GOLD);
        contentTable.add(l).left().padTop(20).padBottom(15).row();
    }

    private void addTip(String text) {
        Table box = new Table();
        box.setBackground(createColorDrawable(new Color(0.06f, 0.15f, 0.08f, 0.9f)));
        box.pad(15);

        Label l = new Label(text, skin);
        l.setColor(Color.GREEN);
        l.setWrap(true);
        box.add(l).width(CONTENT_WIDTH - 40).left();

        contentTable.add(box).width(CONTENT_WIDTH).padBottom(20).row();
    }

    private void addWarning(String text) {
        Table box = new Table();
        box.setBackground(createColorDrawable(new Color(0.18f, 0.06f, 0.03f, 0.9f)));
        box.pad(15);

        Label l = new Label("WARNING: " + text, skin);
        l.setColor(UIConstants.HELP_WARN);
        l.setWrap(true);
        box.add(l).width(CONTENT_WIDTH - 40).left();

        contentTable.add(box).width(CONTENT_WIDTH).padBottom(20).row();
    }

    private Table createCard() {
        Table card = new Table();
        card.setBackground(createColorDrawable(UIConstants.HELP_CARD_BG));
        card.pad(20);
        card.left();
        return card;
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        managedTextures.add(tex);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    // ==================== Screen Lifecycle ====================

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background at full screen size (before viewport is applied)
        if (backgroundTexture != null) {
            game.getSpriteBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight());
            game.getSpriteBatch().begin();
            game.getSpriteBatch().setColor(0.4f, 0.4f, 0.4f, 1f); // Dim
            game.getSpriteBatch().draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            game.getSpriteBatch().setColor(1, 1, 1, 1);
            game.getSpriteBatch().end();
        }

        // Apply viewport for UI
        stage.getViewport().apply();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
            return;
        }

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
        if (backgroundTexture != null)
            backgroundTexture.dispose();
        for (Texture t : managedTextures) {
            if (t != null)
                t.dispose();
        }
        managedTextures.clear();
    }
}
