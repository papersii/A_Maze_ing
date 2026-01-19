package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Manages game assets (textures, animations) and their slicing coordinates.
 */
public class TextureManager implements Disposable {

        private TextureAtlas atlas;
        private Texture attackTexture; // Raw texture for attacks (avoids atlas trimming)

        // Regions & Animations
        public Animation<TextureRegion> playerDown, playerUp, playerLeft, playerRight;
        public Animation<TextureRegion> playerAttackDown, playerAttackUp, playerAttackLeft, playerAttackRight;
        public TextureRegion playerDownStand, playerUpStand, playerLeftStand, playerRightStand;
        public Animation<TextureRegion> enemyWalk; // Slime (Legacy)
        public Animation<TextureRegion> batFly; // Bat (Optional)

        // Boar Animations (4 directions)
        public Animation<TextureRegion> boarWalkDown, boarWalkUp, boarWalkLeft, boarWalkRight;

        // Scorpion Animations (4 directions)
        public Animation<TextureRegion> scorpionWalkDown, scorpionWalkUp, scorpionWalkLeft, scorpionWalkRight;

        // Legacy Wall Regions (Fallbacks)
        public TextureRegion wallRegion;
        public TextureRegion wallRegion2x1, wallRegion3x1, wallRegion2x2, wallRegion3x3;

        public TextureRegion floorRegion; // Default
        public TextureRegion floorDungeon, floorDesert, floorGrassland, floorJungle, floorIce, floorLava, floorRain,
                        floorSpace;
        public TextureRegion entryRegion;
        public TextureRegion exitRegion;
        public TextureRegion trapRegion;
        // Themed Trap Regions
        public TextureRegion trapGrassland, trapDesert, trapIce, trapJungle, trapSpace;
        // [NEW] Themed Trap Animations
        public Animation<TextureRegion> trapGrasslandAnim;
        public Animation<TextureRegion> trapDesertAnim;
        public Animation<TextureRegion> trapIceAnim;
        public Animation<TextureRegion> trapJungleAnim;
        public Animation<TextureRegion> trapSpaceAnim;
        public TextureRegion keyRegion;
        public TextureRegion potionRegion;
        public TextureRegion heartRegion;
        public Animation<TextureRegion> heartBreak;
        public TextureRegion arrowRegion;

        // Treasure Chest Textures
        public TextureRegion chestClosedRegion;
        public TextureRegion chestHalfRegion;
        public TextureRegion chestOpenRegion;
        public Animation<TextureRegion> chestAnimation;

        // White pixel for drawing solid color rectangles (health bars, etc.)
        public TextureRegion whitePixel;

        // Fallback
        private TextureRegion fallbackRegion;
        private Texture fallbackTexture;
        private Texture whitePixelTexture;

        // Wall Asset Cache
        // Key: "theme_WxH" e.g., "grassland_2x1" -> List of variants
        private ObjectMap<String, Array<TextureRegion>> wallStaticCache = new ObjectMap<>();
        // Key: "theme_WxH" e.g., "space_3x3"
        private ObjectMap<String, Animation<TextureRegion>> wallAnimCache = new ObjectMap<>();

        /**
         * Creates TextureManager using a shared TextureAtlas.
         * 
         * @param sharedAtlas The atlas loaded by MazeRunnerGame (avoids duplicate
         *                    loading)
         */
        public TextureManager(TextureAtlas sharedAtlas) {
                this.atlas = sharedAtlas;

                // Create Fallback Texture (Magenta 1x1)
                com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(16, 16,
                                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                pixmap.setColor(com.badlogic.gdx.graphics.Color.MAGENTA);
                pixmap.fill();
                // Add checkerboard pattern
                pixmap.setColor(com.badlogic.gdx.graphics.Color.BLACK);
                pixmap.fillRectangle(0, 0, 8, 8);
                pixmap.fillRectangle(8, 8, 8, 8);

                this.fallbackTexture = new Texture(pixmap);
                pixmap.dispose();
                this.fallbackRegion = new TextureRegion(fallbackTexture);

                // Create white pixel for drawing solid rectangles
                com.badlogic.gdx.graphics.Pixmap whitePix = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                whitePix.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                whitePix.fill();
                this.whitePixelTexture = new Texture(whitePix);
                whitePix.dispose();
                this.whitePixel = new TextureRegion(whitePixelTexture);

                // Load raw texture for attacks to avoid atlas trimming issues (e.g. whitespace
                // stripping)
                try {
                        this.attackTexture = new Texture(com.badlogic.gdx.Gdx.files.internal("images/test.png"));
                } catch (Exception e) {
                        System.err.println("Failed to load images/test.png: " + e.getMessage());
                        this.attackTexture = fallbackTexture;
                }
                loadAssets();
        }

        private TextureRegion findRegionSafe(String name) {
                TextureRegion region = atlas.findRegion(name);
                if (region == null) {
                        System.err.println("TextureManager: Region '" + name + "' not found in atlas. Using fallback.");
                        return fallbackRegion;
                }
                return region;
        }

        private void loadAssets() {
                // Atlas is now provided externally to avoid duplicate loading

                // 1. Player
                TextureRegion charRegion = findRegionSafe("character");

                // Split the region into tiles
                TextureRegion[][] charTiles;
                if (charRegion == fallbackRegion) {
                        charTiles = new TextureRegion[4][4];
                        for (int i = 0; i < 4; i++)
                                for (int j = 0; j < 4; j++)
                                        charTiles[i][j] = fallbackRegion;
                } else {
                        charTiles = charRegion.split(16, 32);
                }

                // Stand Frames (Frame 0)
                playerDownStand = charTiles[0][0];
                playerRightStand = charTiles[1][0];
                playerUpStand = charTiles[2][0];
                playerLeftStand = charTiles[3][0];

                // Walk Animations
                playerDown = new Animation<>(0.15f, charTiles[0][0], charTiles[0][1], charTiles[0][2], charTiles[0][3]);
                playerRight = new Animation<>(0.15f, charTiles[1][0], charTiles[1][1], charTiles[1][2],
                                charTiles[1][3]);
                playerUp = new Animation<>(0.15f, charTiles[2][0], charTiles[2][1], charTiles[2][2], charTiles[2][3]);
                playerLeft = new Animation<>(0.15f, charTiles[3][0], charTiles[3][1], charTiles[3][2], charTiles[3][3]);

                playerDown.setPlayMode(Animation.PlayMode.LOOP);
                playerUp.setPlayMode(Animation.PlayMode.LOOP);
                playerRight.setPlayMode(Animation.PlayMode.LOOP);
                playerLeft.setPlayMode(Animation.PlayMode.LOOP);

                // Assets for HUD - from "objects" region
                TextureRegion objRegion = findRegionSafe("objects");
                TextureRegion[][] objTiles;
                if (objRegion == fallbackRegion) {
                        objTiles = new TextureRegion[5][10]; // Enough size for indices below
                        for (int i = 0; i < 5; i++)
                                for (int j = 0; j < 10; j++)
                                        objTiles[i][j] = fallbackRegion;
                } else {
                        objTiles = objRegion.split(16, 16);
                }

                // Hearts
                heartRegion = objTiles[0][4];

                Array<TextureRegion> heartFrames = new Array<>();
                for (int i = 4; i < 9; i++) {
                        heartFrames.add(objTiles[0][i]);
                }
                heartBreak = new Animation<>(0.1f, heartFrames, Animation.PlayMode.NORMAL);

                // Arrow
                arrowRegion = objTiles[1][1];

                // 2. Mobs (Slime) - from "mobs" region
                TextureRegion mobRegion = findRegionSafe("mobs");
                TextureRegion[][] mobTiles;
                if (mobRegion == fallbackRegion) {
                        mobTiles = new TextureRegion[5][5];
                        for (int i = 0; i < 5; i++)
                                for (int j = 0; j < 5; j++)
                                        mobTiles[i][j] = fallbackRegion;
                } else {
                        mobTiles = mobRegion.split(16, 16);
                }

                Array<TextureRegion> slimeFrames = new Array<>();
                slimeFrames.add(mobTiles[4][0]);
                slimeFrames.add(mobTiles[4][1]);
                enemyWalk = new Animation<>(0.2f, slimeFrames, Animation.PlayMode.LOOP);

                // Load Boar Animations from sprite sheets
                loadBoarAnimations();
                // Load Scorpion Animations from sprite sheets
                loadScorpionAnimations();

                // 3. Tiles - from "basictiles" region
                TextureRegion tileRegion = findRegionSafe("basictiles");
                TextureRegion[][] tiles;
                if (tileRegion == fallbackRegion) {
                        tiles = new TextureRegion[5][10];
                        for (int i = 0; i < 5; i++)
                                for (int j = 0; j < 10; j++)
                                        tiles[i][j] = fallbackRegion;
                } else {
                        tiles = tileRegion.split(16, 16);
                }

                wallRegion = tiles[0][6]; // Wall Stone
                // Fallback for multi-tile walls
                wallRegion2x1 = wallRegion;
                wallRegion3x1 = wallRegion;
                wallRegion2x2 = wallRegion;
                wallRegion3x3 = wallRegion;

                floorRegion = tiles[1][1]; // Dirt Floor
                exitRegion = tiles[0][2]; // Door Closed
                entryRegion = tiles[3][6]; // Stairs Down

                // Trap: Use Green Crystal from objects [0][2] as visual for 'Magic Trap'
                trapRegion = objTiles[0][2];

                // Key
                TextureRegion loadedKey = loadTextureSafe("images/items/item_key_gold.png");
                if (loadedKey != fallbackRegion) {
                        keyRegion = loadedKey;
                } else {
                        keyRegion = objTiles[4][1];
                }
                // Potion
                if (objTiles[4].length > 2) {
                        potionRegion = objTiles[4][2];
                } else {
                        potionRegion = keyRegion; // Fallback
                }
                // Initialize with fallbacks to avoid NPE
                playerAttackDown = playerDown;
                playerAttackUp = playerUp;
                playerAttackLeft = playerLeft;
                playerAttackRight = playerRight;

                // 4. Attack Animations from test.png
                if (attackTexture != null && attackTexture != fallbackTexture) {
                        loadAttackAnimations();
                }

                // 5. Load Optimized Floor Textures
                floorDungeon = loadTextureSafe("images/floors/tile_dungeon_stone.png");
                floorDesert = loadTextureSafe("images/floors/tile_desert_sand.png");
                floorGrassland = loadTextureSafe("images/floors/tile_grassland_grass.png");
                floorIce = loadTextureSafe("images/floors/tile_ice_frozen.png");
                floorLava = loadTextureSafe("images/floors/tile_lava_magma.png");
                floorRain = loadTextureSafe("images/floors/tile_rain_puddle.png");
                floorSpace = loadTextureSafe("images/floors/tile_space_metal.png");
                floorJungle = loadTextureSafe("images/floors/tile_jungle_floor.png");

                // 6. Load Arrow Texture (Standalone)
                TextureRegion loadedArrow = loadTextureSafe("images/items/item_arrow.png");
                if (loadedArrow != fallbackRegion) {
                        arrowRegion = loadedArrow;
                }

                // 7. Load Dynamic Wall Assets
                loadWallAssets();

                // 8. Load Themed Trap Textures
                trapGrassland = loadTextureSafe("images/traps/trap_grassland_v1.png");
                // [NEW] Load Grassland Trap Animation (Fog Overlay)
                trapGrasslandAnim = loadSpriteSheetAnimation("images/animations/anim_trap_grassland_fog_4f.png", 4, 64,
                                0.15f);

                trapDesert = loadTextureSafe("images/traps/trap_desert_v1.png");
                trapDesertAnim = loadSpriteSheetAnimation("images/animations/anim_trap_desert_sandstorm_4f.png", 4, 64,
                                0.15f);

                trapIce = loadTextureSafe("images/traps/trap_ice_v1.png");
                trapIceAnim = loadSpriteSheetAnimation("images/animations/anim_trap_ice_frost_4f.png", 4, 64, 0.15f);

                trapJungle = loadTextureSafe("images/traps/trap_jungle_v1.png");
                trapJungleAnim = loadSpriteSheetAnimation("images/animations/anim_trap_jungle_spores_4f.png", 4, 64,
                                0.15f);

                trapSpace = loadTextureSafe("images/traps/trap_space_v1.png");
                trapSpaceAnim = loadSpriteSheetAnimation("images/animations/anim_trap_space_shock_4f.png", 4, 64,
                                0.15f);

                // 9. Load Treasure Chest Textures
                loadChestAssets();
        }

        /**
         * Loads treasure chest textures and animation.
         * Expected files: images/items/chest_closed.png, chest_half.png, chest_open.png
         * Or: images/items/chest_3f.png (3-frame sprite sheet)
         */
        private void loadChestAssets() {
                // Try loading individual frames first
                chestClosedRegion = loadTextureSafe("images/items/chest_closed.png");
                chestHalfRegion = loadTextureSafe("images/items/chest_half.png");
                chestOpenRegion = loadTextureSafe("images/items/chest_open.png");

                // If individual frames not found, try sprite sheet
                if (chestClosedRegion == fallbackRegion) {
                        Animation<TextureRegion> sheetAnim = loadSpriteSheetAnimation(
                                        "images/items/chest_3f.png", 3, 32, 0.2f);
                        if (sheetAnim != null) {
                                TextureRegion[] frames = sheetAnim.getKeyFrames();
                                if (frames.length >= 3) {
                                        chestClosedRegion = frames[0];
                                        chestHalfRegion = frames[1];
                                        chestOpenRegion = frames[2];
                                }
                        }
                }

                // Create animation from frames
                if (chestClosedRegion != fallbackRegion && chestHalfRegion != fallbackRegion
                                && chestOpenRegion != fallbackRegion) {
                        chestAnimation = new Animation<>(0.2f, chestClosedRegion, chestHalfRegion, chestOpenRegion);
                        chestAnimation.setPlayMode(Animation.PlayMode.NORMAL);
                        System.out.println("TextureManager: Loaded chest textures successfully");
                } else {
                        // Use trap region as fallback
                        chestClosedRegion = trapRegion;
                        chestHalfRegion = trapRegion;
                        chestOpenRegion = trapRegion;
                        System.out.println("TextureManager: Using fallback for chest textures");
                }
        }

        private void loadAttackAnimations() {
                try {
                        int attackFrameWidth = 32;
                        int attackFrameHeight = 32;
                        int startY = 128; // 4 * 32

                        // Row 4: Down
                        TextureRegion d0 = new TextureRegion(attackTexture, 0 * attackFrameWidth, startY,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion d1 = new TextureRegion(attackTexture, 1 * attackFrameWidth, startY,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion d2 = new TextureRegion(attackTexture, 2 * attackFrameWidth, startY,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion d3 = new TextureRegion(attackTexture, 3 * attackFrameWidth, startY,
                                        attackFrameWidth, attackFrameHeight);
                        playerAttackDown = new Animation<>(0.02f, d0, d1, d1, d1, d1, d2, d2, d2, d2, d3);
                        playerAttackDown.setPlayMode(Animation.PlayMode.NORMAL);

                        // Row 5: Up
                        TextureRegion u0 = new TextureRegion(attackTexture, 0 * attackFrameWidth, startY + 32,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion u1 = new TextureRegion(attackTexture, 1 * attackFrameWidth, startY + 32,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion u2 = new TextureRegion(attackTexture, 2 * attackFrameWidth, startY + 32,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion u3 = new TextureRegion(attackTexture, 3 * attackFrameWidth, startY + 32,
                                        attackFrameWidth, attackFrameHeight);
                        playerAttackUp = new Animation<>(0.02f, u0, u1, u1, u1, u1, u2, u2, u2, u2, u3);
                        playerAttackUp.setPlayMode(Animation.PlayMode.NORMAL);

                        // Row 6: Right
                        TextureRegion r0 = new TextureRegion(attackTexture, 0 * attackFrameWidth, startY + 64,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion r1 = new TextureRegion(attackTexture, 1 * attackFrameWidth, startY + 64,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion r2 = new TextureRegion(attackTexture, 2 * attackFrameWidth, startY + 64,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion r3 = new TextureRegion(attackTexture, 3 * attackFrameWidth, startY + 64,
                                        attackFrameWidth, attackFrameHeight);
                        playerAttackRight = new Animation<>(0.02f, r0, r1, r1, r1, r1, r2, r2, r2, r2, r3);
                        playerAttackRight.setPlayMode(Animation.PlayMode.NORMAL);

                        // Row 7: Left
                        TextureRegion l0 = new TextureRegion(attackTexture, 0 * attackFrameWidth, startY + 96,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion l1 = new TextureRegion(attackTexture, 1 * attackFrameWidth, startY + 96,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion l2 = new TextureRegion(attackTexture, 2 * attackFrameWidth, startY + 96,
                                        attackFrameWidth, attackFrameHeight);
                        TextureRegion l3 = new TextureRegion(attackTexture, 3 * attackFrameWidth, startY + 96,
                                        attackFrameWidth, attackFrameHeight);
                        playerAttackLeft = new Animation<>(0.02f, l0, l1, l1, l1, l1, l2, l2, l2, l2, l3);
                        playerAttackLeft.setPlayMode(Animation.PlayMode.NORMAL);
                } catch (Exception e) {
                        System.err.println("Error loading attack animations: " + e.getMessage());
                }
        }

        /**
         * Loads boar animations from sprite sheet files.
         * Files expected: images/mobs/mob_boar_walk_{direction}_4f.png
         */
        private void loadBoarAnimations() {
                boarWalkDown = loadSpriteSheetAnimation("images/mobs/mob_boar_walk_down_4f.png", 4, 64, 0.15f);
                boarWalkUp = loadSpriteSheetAnimation("images/mobs/mob_boar_walk_up_4f.png", 4, 64, 0.15f);
                boarWalkLeft = loadSpriteSheetAnimation("images/mobs/mob_boar_walk_left_4f.png", 4, 64, 0.15f);
                boarWalkRight = loadSpriteSheetAnimation("images/mobs/mob_boar_walk_right_4f.png", 4, 64, 0.15f);

                // Log success
                if (boarWalkDown != null) {
                        System.out.println("TextureManager: Loaded boar animations successfully");
                }
        }

        private void loadScorpionAnimations() {
                scorpionWalkDown = loadSpriteSheetAnimation("images/mobs/mob_scorpion_walk_down_4f.png", 4, 64, 0.15f);
                scorpionWalkUp = loadSpriteSheetAnimation("images/mobs/mob_scorpion_walk_up_4f.png", 4, 64, 0.15f);
                scorpionWalkLeft = loadSpriteSheetAnimation("images/mobs/mob_scorpion_walk_left_4f.png", 4, 64, 0.15f);
                scorpionWalkRight = loadSpriteSheetAnimation("images/mobs/mob_scorpion_walk_right_4f.png", 4, 64,
                                0.15f);

                // Log success
                if (scorpionWalkDown != null) {
                        System.out.println("TextureManager: Loaded scorpion animations successfully (4 directions)");
                }
        }

        /**
         * Loads a horizontal sprite sheet animation.
         * 
         * @param path          Path to the sprite sheet file
         * @param frameCount    Number of frames
         * @param frameSize     Size of each frame (width = height)
         * @param frameDuration Duration of each frame in seconds
         * @return Animation or null if failed
         */
        private Animation<TextureRegion> loadSpriteSheetAnimation(String path, int frameCount, int frameSize,
                        float frameDuration) {
                try {
                        if (!com.badlogic.gdx.Gdx.files.internal(path).exists()) {
                                System.err.println("Sprite sheet not found: " + path);
                                return null;
                        }
                        Texture texture = new Texture(com.badlogic.gdx.Gdx.files.internal(path));
                        Array<TextureRegion> frames = new Array<>();
                        for (int i = 0; i < frameCount; i++) {
                                frames.add(new TextureRegion(texture, i * frameSize, 0, frameSize, frameSize));
                        }
                        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
                        return anim;
                } catch (Exception e) {
                        System.err.println("Failed to load sprite sheet: " + path + " - " + e.getMessage());
                        return null;
                }
        }

        /**
         * Loads wall assets for various themes and sizes based on conventions.
         */
        private void loadWallAssets() {
                String[] themes = { "dungeon", "grassland", "desert", "ice", "jungle", "space" };
                // 7 sizes: 2x2, 3x2, 2x3, 2x4, 4x2, 3x3, 4x4
                int[][] sizes = {
                                { 2, 2 }, { 3, 2 }, { 2, 3 }, { 2, 4 }, { 4, 2 }, { 3, 3 }, { 4, 4 }
                };

                for (String theme : themes) {
                        for (int[] size : sizes) {
                                int w = size[0];
                                int h = size[1];
                                String sizeSuffix = w + "x" + h;
                                String key = theme + "_" + sizeSuffix;
                                Array<TextureRegion> variants = new Array<>();

                                // Load variants v1, v2
                                for (int v = 1; v <= 2; v++) {
                                        String path = "images/walls/wall_" + theme + "_" + sizeSuffix + "_v" + v
                                                        + ".png";
                                        if (com.badlogic.gdx.Gdx.files.internal(path).exists()) {
                                                try {
                                                        Texture tex = new Texture(
                                                                        com.badlogic.gdx.Gdx.files.internal(path));
                                                        TextureRegion reg = new TextureRegion(tex);
                                                        variants.add(reg);
                                                        // Cache color for grout logic
                                                        calculateAndCacheColor(tex, reg);
                                                        // System.out.println("Loaded " + path);
                                                } catch (Exception e) {
                                                        System.err.println("Failed to load wall variant: " + path);
                                                }
                                        }
                                }

                                // Legacy Fallback (if no variants found)
                                if (variants.size == 0) {
                                        String staticPath = "images/walls/wall_" + theme + "_" + sizeSuffix + ".png";
                                        if (com.badlogic.gdx.Gdx.files.internal(staticPath).exists()) {
                                                try {
                                                        Texture staticTex = new Texture(com.badlogic.gdx.Gdx.files
                                                                        .internal(staticPath));
                                                        TextureRegion reg = new TextureRegion(staticTex);
                                                        variants.add(reg);
                                                        calculateAndCacheColor(staticTex, reg);
                                                } catch (Exception e) {
                                                        // ignore
                                                }
                                        }
                                }

                                if (variants.size > 0) {
                                        wallStaticCache.put(key, variants);
                                }
                        }
                }
        }

        /**
         * Returns a wall texture region for the given theme, size, and position.
         * Position is used to deterministically select a variant.
         */
        public TextureRegion getWallRegion(String theme, int width, int height, int x, int y) {
                String usedTheme = (theme == null) ? "dungeon" : theme.toLowerCase();
                String key = usedTheme + "_" + width + "x" + height;

                if (wallStaticCache.containsKey(key)) {
                        Array<TextureRegion> variants = wallStaticCache.get(key);
                        if (variants != null && variants.size > 0) {
                                // Better hash for ~50/50 distribution using XOR
                                int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                                return variants.get(hash % variants.size);
                        }
                }

                // Fallback: Generic Size
                // If variant not found
                if (width == 2 && height == 1)
                        return wallRegion2x1;
                if (width == 3 && height == 1)
                        return wallRegion3x1;
                if (width == 2 && height == 2)
                        return wallRegion2x2;
                if (width == 3 && height == 3)
                        return wallRegion3x3;

                return wallRegion; // Ultimate fallback
        }

        /**
         * Returns a trap texture region for the given theme.
         * Falls back to default trapRegion if themed texture is not available.
         */
        public TextureRegion getTrapRegion(String theme) {
                if (theme == null)
                        return trapRegion;
                switch (theme) {
                        case "Grassland":
                                return trapGrassland != fallbackRegion ? trapGrassland : trapRegion;
                        case "Desert":
                                return trapDesert != fallbackRegion ? trapDesert : trapRegion;
                        case "Ice":
                                return trapIce != fallbackRegion ? trapIce : trapRegion;
                        case "Jungle":
                                return trapJungle != fallbackRegion ? trapJungle : trapRegion;
                        case "Space":
                                return trapSpace != fallbackRegion ? trapSpace : trapRegion;
                        default:
                                return trapRegion;
                }
        }

        /**
         * Returns the trap animation for the given theme.
         * Returns null if no animation exists for that theme.
         */
        public Animation<TextureRegion> getTrapAnimation(String theme) {
                if (theme == null)
                        return null;
                switch (theme) {
                        case "Grassland":
                                return trapGrasslandAnim;
                        case "Desert":
                                return trapDesertAnim;
                        case "Ice":
                                return trapIceAnim;
                        case "Jungle":
                                return trapJungleAnim;
                        case "Space":
                                return trapSpaceAnim;
                        default:
                                return null;
                }
        }

        /**
         * Returns the appropriate boar animation based on direction.
         * Falls back to enemyWalk (slime) if boar animation not loaded.
         * 
         * @param direction 0=down, 1=left, 2=up, 3=right
         * @return The boar walking animation for the given direction
         */
        public Animation<TextureRegion> getBoarAnimation(int direction) {
                Animation<TextureRegion> anim = null;
                switch (direction) {
                        case 0:
                                anim = boarWalkDown;
                                break;
                        case 1:
                                anim = boarWalkLeft;
                                break;
                        case 2:
                                anim = boarWalkUp;
                                break;
                        case 3:
                                anim = boarWalkRight;
                                break;
                }
                // Fallback to legacy slime animation
                return anim != null ? anim : enemyWalk;
        }

        /**
         * Returns boar animation based on velocity direction.
         * 
         * @param vx X velocity
         * @param vy Y velocity
         * @return The boar walking animation for the given velocity
         */
        public Animation<TextureRegion> getBoarAnimationByVelocity(float vx, float vy) {
                // Determine primary direction
                if (Math.abs(vx) > Math.abs(vy)) {
                        return vx > 0 ? (boarWalkRight != null ? boarWalkRight : enemyWalk)
                                        : (boarWalkLeft != null ? boarWalkLeft : enemyWalk);
                } else {
                        return vy > 0 ? (boarWalkUp != null ? boarWalkUp : enemyWalk)
                                        : (boarWalkDown != null ? boarWalkDown : enemyWalk);
                }
        }

        /**
         * Returns scorpion animation based on velocity direction.
         *
         * @param vx X velocity
         * @param vy Y velocity
         * @return The scorpion walking animation for the given velocity
         */
        public Animation<TextureRegion> getScorpionAnimationByVelocity(float vx, float vy) {
                // Determine primary direction
                if (Math.abs(vx) > Math.abs(vy)) {
                        return vx > 0 ? (scorpionWalkRight != null ? scorpionWalkRight : enemyWalk)
                                        : (scorpionWalkLeft != null ? scorpionWalkLeft : enemyWalk);
                } else {
                        return vy > 0 ? (scorpionWalkUp != null ? scorpionWalkUp : enemyWalk)
                                        : (scorpionWalkDown != null ? scorpionWalkDown : enemyWalk);
                }
        }

        public Animation<TextureRegion> getEnemyAnimation(de.tum.cit.fop.maze.model.Enemy.EnemyType type, float vx,
                        float vy) {
                switch (type) {
                        case SCORPION:
                                return getScorpionAnimationByVelocity(vx, vy);
                        case BOAR:
                                return getBoarAnimationByVelocity(vx, vy);
                        default:
                                return enemyWalk;
                }
        }

        /**
         * Returns the treasure chest texture frame based on chest state.
         * 
         * @param state The chest state from TreasureChest.ChestState
         * @return The corresponding texture region
         */
        public TextureRegion getChestFrame(de.tum.cit.fop.maze.model.TreasureChest.ChestState state) {
                if (state == null) {
                        return chestClosedRegion;
                }
                switch (state) {
                        case CLOSED:
                                return chestClosedRegion;
                        case OPENING:
                                return chestHalfRegion;
                        case OPEN:
                                return chestOpenRegion;
                        default:
                                return chestClosedRegion;
                }
        }

        /**
         * Returns the treasure chest texture frame based on frame index.
         * 
         * @param frameIndex 0=closed, 1=half, 2=open
         * @return The corresponding texture region
         */
        public TextureRegion getChestFrame(int frameIndex) {
                switch (frameIndex) {
                        case 0:
                                return chestClosedRegion;
                        case 1:
                                return chestHalfRegion;
                        case 2:
                                return chestOpenRegion;
                        default:
                                return chestClosedRegion;
                }
        }

        /**
         * Returns the treasure chest animation.
         */
        public Animation<TextureRegion> getChestAnimation() {
                return chestAnimation;
        }

        /**
         * 获取宝箱纹理的渲染高度（用于底部对齐）
         * 
         * 宝箱纹理尺寸：closed (30x31), half (30x34), open (30x51)
         * 需要根据状态返回正确的高度以实现底部对齐。
         * 
         * @param state    宝箱状态
         * @param tileSize 格子大小（像素）
         * @return 渲染高度（格子单位）
         */
        public float getChestRenderHeight(de.tum.cit.fop.maze.model.TreasureChest.ChestState state, float tileSize) {
                TextureRegion region = getChestFrame(state);
                if (region == null) {
                        return 1f;
                }
                // 基准高度：closed 的高度（31px）对应 1 格
                float baseHeight = 31f;
                float actualHeight = region.getRegionHeight();
                return actualHeight / baseHeight;
        }

        /**
         * 获取宝箱纹理的宽高比
         */
        public float getChestAspectRatio(de.tum.cit.fop.maze.model.TreasureChest.ChestState state) {
                TextureRegion region = getChestFrame(state);
                if (region == null || region.getRegionHeight() == 0) {
                        return 1f;
                }
                return (float) region.getRegionWidth() / region.getRegionHeight();
        }

        private TextureRegion loadTextureSafe(String path) {
                try {
                        Texture t = new Texture(com.badlogic.gdx.Gdx.files.internal(path));
                        // [NEW] Calculate and cache average color for Grout
                        calculateAndCacheColor(t, new TextureRegion(t));
                        return new TextureRegion(t);
                } catch (Exception e) {
                        // System.err.println("Failed to load texture: " + path);
                        return fallbackRegion;
                }
        }

        // [NEW] Cache for average colors of textures
        private final com.badlogic.gdx.utils.ObjectMap<TextureRegion, com.badlogic.gdx.graphics.Color> regionColorCache = new com.badlogic.gdx.utils.ObjectMap<>();

        private void calculateAndCacheColor(Texture texture, TextureRegion region) {
                try {
                        if (!texture.getTextureData().isPrepared()) {
                                texture.getTextureData().prepare();
                        }
                        com.badlogic.gdx.graphics.Pixmap pixmap = texture.getTextureData().consumePixmap();

                        long r = 0, g = 0, b = 0;
                        int pixelCount = 0;
                        int width = pixmap.getWidth();
                        int height = pixmap.getHeight();

                        // Sample pixels (every 4th pixel for performance)
                        for (int x = 0; x < width; x += 4) {
                                for (int y = 0; y < height; y += 4) {
                                        int colorInt = pixmap.getPixel(x, y);
                                        // RGBA8888
                                        r += (colorInt & 0xff000000) >>> 24;
                                        g += (colorInt & 0x00ff0000) >>> 16;
                                        b += (colorInt & 0x0000ff00) >>> 8;
                                        pixelCount++;
                                }
                        }

                        if (texture.getTextureData()
                                        .getType() == com.badlogic.gdx.graphics.TextureData.TextureDataType.Pixmap) {
                                pixmap.dispose();
                        }

                        if (pixelCount > 0) {
                                regionColorCache.put(region, new com.badlogic.gdx.graphics.Color(
                                                (r / pixelCount) / 255f,
                                                (g / pixelCount) / 255f,
                                                (b / pixelCount) / 255f,
                                                1f));
                        } else {
                                regionColorCache.put(region, com.badlogic.gdx.graphics.Color.GRAY);
                        }
                } catch (Exception e) {
                        System.err.println("Failed to calculate average color: " + e.getMessage());
                        regionColorCache.put(region, com.badlogic.gdx.graphics.Color.GRAY);
                }
        }

        public com.badlogic.gdx.graphics.Color getTextureColor(TextureRegion region) {
                return regionColorCache.get(region, com.badlogic.gdx.graphics.Color.GRAY);
        }

        @Override
        public void dispose() {
                if (atlas != null) {
                        atlas.dispose();
                }
                if (attackTexture != null) {
                        attackTexture.dispose();
                }
                if (fallbackTexture != null) {
                        fallbackTexture.dispose();
                }
                if (whitePixelTexture != null) {
                        whitePixelTexture.dispose();
                }

                disposeRegionTexture(floorDungeon);
                disposeRegionTexture(floorDesert);
                disposeRegionTexture(floorGrassland);
                disposeRegionTexture(floorJungle);
                disposeRegionTexture(floorIce);
                disposeRegionTexture(floorLava);
                disposeRegionTexture(floorRain);
                disposeRegionTexture(floorSpace);
                disposeRegionTexture(arrowRegion);
                disposeRegionTexture(keyRegion);

                for (Array<TextureRegion> variants : wallStaticCache.values()) {
                        for (TextureRegion reg : variants) {
                                disposeRegionTexture(reg);
                        }
                }
        }

        private void disposeRegionTexture(TextureRegion region) {
                if (region != null && region.getTexture() != null && region != fallbackRegion) {
                        // Only dispose if it's NOT atlas or fallback.
                        // Rough check using atlas variable
                        if (atlas != null) {
                                boolean isAtlas = false;
                                for (Texture t : atlas.getTextures())
                                        if (t == region.getTexture())
                                                isAtlas = true;
                                if (isAtlas)
                                        return;
                        }

                        if (region.getTexture() != fallbackTexture &&
                                        region.getTexture() != attackTexture) {
                                try {
                                        region.getTexture().dispose();
                                } catch (Exception e) {
                                }
                        }
                }
        }
}
