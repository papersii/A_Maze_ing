package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.Texture;

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
        public Animation<TextureRegion> enemyWalk; // Slime
        public Animation<TextureRegion> batFly; // Bat (Optional)

        public TextureRegion wallRegion;
        public TextureRegion floorRegion; // Default
        public TextureRegion floorDungeon, floorDesert, floorForest, floorIce, floorLava, floorRain, floorSpace;
        public TextureRegion entryRegion;
        public TextureRegion exitRegion;
        public TextureRegion trapRegion;
        public TextureRegion keyRegion;
        public TextureRegion potionRegion;
        public TextureRegion heartRegion;
        public Animation<TextureRegion> heartBreak;
        public TextureRegion arrowRegion;

        // Fallback
        private TextureRegion fallbackRegion;
        private Texture fallbackTexture;

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
                // "character" is the name of the region (from character.png)
                TextureRegion charRegion = findRegionSafe("character");

                System.out
                                .println("Character Region Size: " + charRegion.getRegionWidth() + "x"
                                                + charRegion.getRegionHeight());

                // Split the region into tiles
                // Split the region into tiles (16x32 for character)
                // If fallback, it's 16x16, splitting 16x32 might fail or produce weirdness?
                // Split handles strict matching or no? region.split returns tiles based on
                // block size.
                // If region is 16x16 and we ask for 16x32, it returns empty or fails?
                // LibGDX split checks region dimensions.
                // Let's ensure fallback is big enough or adapt.
                // Fallback is 16x16. Splitting 16x32 will effectively crash or return empty.
                // For safety, let's make the fallback 64x128 or something reusable?
                // Or just use 16x16 and hope split works?
                // Actually, if charRegion is fallback (16x16), splitting 16x32 produces [0][0]
                // only?

                TextureRegion[][] charTiles;
                if (charRegion == fallbackRegion) {
                        // Create a fake array of regions pointing to fallback
                        charTiles = new TextureRegion[4][4];
                        for (int i = 0; i < 4; i++)
                                for (int j = 0; j < 4; j++)
                                        charTiles[i][j] = fallbackRegion;
                } else {
                        charTiles = charRegion.split(16, 32);
                }

                // Multi-frame Animations & Standing Frames
                // Assuming 4 rows: 0=Down, 1=Up, 2=Right, 3=Left (Based on previous code
                // indices)

                // Stand Frames (Frame 0)
                playerDownStand = charTiles[0][0];
                playerRightStand = charTiles[1][0];
                playerUpStand = charTiles[2][0];
                playerLeftStand = charTiles[3][0];

                // Walk Animations (Frames 0, 1, 2, 3 - or 1, 2, 3, 1?
                // Standard LPC is 9 frames usually. But here we have 4.
                // If 4 frames: usually 0 is stand, 1 is stride, 2 is stand, 3 is stride.
                // Previous code used: 0, 1, 2, 1. (Ping pong)
                // Let's keep 0, 1, 2, 1 pattern as originally intended but using 16x32 tiles.

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

                // Hearts: Row 0, starting at Col 4. (Based on user image: Chests(0,1),
                // Crystal(2), Blank(3), Hearts(4..8))
                heartRegion = objTiles[0][4];

                Array<TextureRegion> heartFrames = new Array<>();
                for (int i = 4; i < 9; i++) {
                        heartFrames.add(objTiles[0][i]);
                }
                heartBreak = new Animation<>(0.1f, heartFrames, Animation.PlayMode.NORMAL);

                // Arrow: Row 1, Col 1 (Assuming safe)
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

                // Note: "things" region not currently used. trapRegion uses objTiles instead.

                wallRegion = tiles[0][6]; // Wall Stone
                floorRegion = tiles[1][1]; // Dirt Floor
                exitRegion = tiles[0][2]; // Door Closed
                entryRegion = tiles[3][6]; // Stairs Down

                // Trap: Use Green Crystal from objects [0][2] as visual for 'Magic Trap'
                trapRegion = objTiles[0][2];

                // Key: Guessing Row 4 (where Coins are) Col 1?
                // If correct, [4][0] is Coin (User saw yellow circles).
                // Let's try [4][1] for Key.
                keyRegion = objTiles[4][1];
                // Guessing Potion at [4][2] or similar. If not, we reuse Key tint.
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

                // 4. Attack Animations from test.png (Raw Texture)
                if (attackTexture != null && attackTexture != fallbackTexture) {
                        try {
                                // Rows 0-3 are presumably walk (16x32), so attack rows start at y = 4 * 32 =
                                // 128
                                // We assume Attack frames are WIDER (e.g. 32px) to accommodate weapon swing.

                                // Row 4: Down (Index 4 in 0-indexed rows of height 32)
                                // y = 128. Width = 32.
                                int attackFrameWidth = 32;
                                int attackFrameHeight = 32;
                                int startY = 128; // 4 * 32

                                // Helper to get frames
                                // Row 4: Down
                                TextureRegion d0 = new TextureRegion(attackTexture, 0 * attackFrameWidth, startY,
                                                attackFrameWidth, attackFrameHeight);
                                TextureRegion d1 = new TextureRegion(attackTexture, 1 * attackFrameWidth, startY,
                                                attackFrameWidth, attackFrameHeight);
                                TextureRegion d2 = new TextureRegion(attackTexture, 2 * attackFrameWidth, startY,
                                                attackFrameWidth, attackFrameHeight);
                                TextureRegion d3 = new TextureRegion(attackTexture, 3 * attackFrameWidth, startY,
                                                attackFrameWidth, attackFrameHeight);
                                // 1-4-4-1 distribution (10 frames, 0.02s each => 0.2s total)
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
                                // Keep default (which is playerWalk)
                        }
                }
                // 5. Load Optimized Floor Textures
                floorDungeon = loadTextureSafe("images/floors/tile_dungeon_stone.png");
                floorDesert = loadTextureSafe("images/floors/tile_desert_sand.png");
                floorForest = loadTextureSafe("images/floors/tile_forest_grass.png");
                floorIce = loadTextureSafe("images/floors/tile_ice_frozen.png");
                floorLava = loadTextureSafe("images/floors/tile_lava_magma.png");
                floorRain = loadTextureSafe("images/floors/tile_rain_puddle.png");
                floorSpace = loadTextureSafe("images/floors/tile_space_metal.png");
        }

        private TextureRegion loadTextureSafe(String path) {
                try {
                        Texture t = new Texture(com.badlogic.gdx.Gdx.files.internal(path));
                        return new TextureRegion(t);
                } catch (Exception e) {
                        System.err.println("Failed to load texture: " + path);
                        return fallbackRegion;
                }
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
                // Dispose specific floor textures if they are backed by their own textures
                // (which they are)
                disposeRegionTexture(floorDungeon);
                disposeRegionTexture(floorDesert);
                disposeRegionTexture(floorForest);
                disposeRegionTexture(floorIce);
                disposeRegionTexture(floorLava);
                disposeRegionTexture(floorRain);
                disposeRegionTexture(floorSpace);
        }

        private void disposeRegionTexture(TextureRegion region) {
                if (region != null && region.getTexture() != null && region != fallbackRegion) {
                        region.getTexture().dispose();
                }
        }
}
