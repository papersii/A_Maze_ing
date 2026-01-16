package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all audio in the game including sound effects and background music.
 * Supports theme-based BGM switching based on game level/screen context.
 *
 * BGM Tracks:
 * - menu_bgm.mp3 → Main menu, level selection, shop, etc.
 * - grassland_bgm.mp3 → Levels 1-4 (Grassland theme)
 * - jungle_bgm.mp3 → Levels 5-8 (Jungle theme)
 * - desert_bgm.mp3 → Levels 9-12 (Desert theme)
 * - ice_bgm.mp3 → Levels 13-16 (Ice/Tundra theme)
 * - space_bgm.mp3 → Levels 17-19 (Space theme)
 * - boss_bgm.mp3 → Level 20 (Final Boss)
 * - pause_bgm.mp3 → Pause menu overlay
 */
public class AudioManager implements Disposable {
    private static AudioManager instance;

    // === Sound Effects ===
    private final Map<String, Sound> soundEffects;

    // === Background Music ===
    private final Map<String, Music> bgmTracks;
    private Music currentBgm;
    private String currentBgmKey;

    // === Settings ===
    private boolean musicEnabled = true;
    private float musicVolume = 0.3f;
    private float sfxVolume = 1.0f;

    // === BGM Keys ===
    public static final String BGM_MENU = "menu";
    public static final String BGM_GRASSLAND = "grassland";
    public static final String BGM_JUNGLE = "jungle";
    public static final String BGM_DESERT = "desert";
    public static final String BGM_ICE = "ice";
    public static final String BGM_SPACE = "space";
    public static final String BGM_BOSS = "boss";
    public static final String BGM_PAUSE = "pause";

    private AudioManager() {
        soundEffects = new HashMap<>();
        bgmTracks = new HashMap<>();
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    /**
     * Load all audio resources. Should be called once during game initialization.
     */
    public void load() {
        // === Load BGM Tracks ===
        loadBgm(BGM_MENU, "audio/bgm/menu_bgm.mp3");
        loadBgm(BGM_GRASSLAND, "audio/bgm/grassland_bgm.mp3");
        loadBgm(BGM_JUNGLE, "audio/bgm/jungle_bgm.mp3");
        loadBgm(BGM_DESERT, "audio/bgm/desert_bgm.mp3");
        loadBgm(BGM_ICE, "audio/bgm/ice_bgm.mp3");
        loadBgm(BGM_SPACE, "audio/bgm/space_bgm.mp3");
        loadBgm(BGM_BOSS, "audio/bgm/boss_bgm.mp3");
        loadBgm(BGM_PAUSE, "audio/bgm/pause_bgm.mp3");

        // === Load Sound Effects ===
        loadSound("walk", "audio/walk.ogg");
        loadSound("hit", "audio/hit.ogg");
        loadSound("attack", "audio/attack.ogg");
        loadSound("kill", "audio/kill.ogg");
        loadSound("collect", "audio/collect.ogg");
        loadSound("victory", "audio/victory.ogg");
        loadSound("gameover", "audio/gameover.ogg");
        loadSound("select", "audio/select.ogg");
    }

    private void loadBgm(String key, String path) {
        if (Gdx.files.internal(path).exists()) {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
            music.setLooping(true);
            music.setVolume(musicVolume);
            bgmTracks.put(key, music);
        } else {
            GameLogger.warn("AudioManager", "BGM file not found: " + path);
        }
    }

    private void loadSound(String name, String path) {
        if (Gdx.files.internal(path).exists()) {
            soundEffects.put(name, Gdx.audio.newSound(Gdx.files.internal(path)));
        } else {
            // Try .wav fallback
            String wavPath = path.replace(".ogg", ".wav");
            if (Gdx.files.internal(wavPath).exists()) {
                soundEffects.put(name, Gdx.audio.newSound(Gdx.files.internal(wavPath)));
            } else {
                // Try .mp3 fallback
                String mp3Path = path.replace(".ogg", ".mp3");
                if (Gdx.files.internal(mp3Path).exists()) {
                    soundEffects.put(name, Gdx.audio.newSound(Gdx.files.internal(mp3Path)));
                }
            }
        }
    }

    // ==================== BGM Control ====================

    /**
     * Play background music for a specific theme.
     * If already playing the same track, does nothing.
     *
     * @param bgmKey One of the BGM constants (e.g., BGM_MENU, BGM_GRASSLAND)
     */
    public void playBgm(String bgmKey) {
        if (!musicEnabled) {
            return;
        }

        // If already playing this track, do nothing
        if (bgmKey.equals(currentBgmKey) && currentBgm != null && currentBgm.isPlaying()) {
            return;
        }

        // Stop current BGM
        stopMusic();

        // Start new BGM
        Music newBgm = bgmTracks.get(bgmKey);
        if (newBgm != null) {
            newBgm.setVolume(musicVolume);
            newBgm.play();
            currentBgm = newBgm;
            currentBgmKey = bgmKey;
            GameLogger.debug("AudioManager", "Playing BGM: " + bgmKey);
        } else {
            GameLogger.warn("AudioManager", "BGM not found for key: " + bgmKey);
        }
    }

    /**
     * Get the appropriate BGM key for a given level number.
     *
     * @param levelNumber The level number (1-20)
     * @return The BGM key constant
     */
    public static String getBgmKeyForLevel(int levelNumber) {
        if (levelNumber == 20) {
            return BGM_BOSS;
        } else if (levelNumber >= 17) {
            return BGM_SPACE;
        } else if (levelNumber >= 13) {
            return BGM_ICE;
        } else if (levelNumber >= 9) {
            return BGM_DESERT;
        } else if (levelNumber >= 5) {
            return BGM_JUNGLE;
        } else {
            return BGM_GRASSLAND;
        }
    }

    /**
     * Get the appropriate BGM key for a given theme name.
     *
     * @param themeName The theme name (e.g., "Grassland", "Jungle")
     * @return The BGM key constant
     */
    public static String getBgmKeyForTheme(String themeName) {
        if (themeName == null) {
            return BGM_GRASSLAND;
        }
        switch (themeName) {
            case "Grassland":
                return BGM_GRASSLAND;
            case "Jungle":
                return BGM_JUNGLE;
            case "Desert":
                return BGM_DESERT;
            case "Ice":
                return BGM_ICE;
            case "Space":
                return BGM_SPACE;
            default:
                return BGM_GRASSLAND;
        }
    }

    /**
     * Play menu background music.
     */
    public void playMenuBgm() {
        playBgm(BGM_MENU);
    }

    /**
     * Play BGM appropriate for the given level.
     *
     * @param levelNumber The level number (1-20)
     */
    public void playLevelBgm(int levelNumber) {
        playBgm(getBgmKeyForLevel(levelNumber));
    }

    /**
     * Play BGM appropriate for the given theme.
     *
     * @param themeName The theme name (e.g., "Grassland", "Jungle")
     */
    public void playThemeBgm(String themeName) {
        playBgm(getBgmKeyForTheme(themeName));
    }

    /**
     * Legacy method for backwards compatibility.
     * Starts playing menu BGM by default.
     */
    public void playMusic() {
        if (currentBgm != null) {
            if (musicEnabled && !currentBgm.isPlaying()) {
                currentBgm.play();
            }
        } else {
            playMenuBgm();
        }
    }

    public void stopMusic() {
        if (currentBgm != null) {
            currentBgm.stop();
        }
    }

    public void pauseMusic() {
        if (currentBgm != null && currentBgm.isPlaying()) {
            currentBgm.pause();
        }
    }

    public void resumeMusic() {
        if (musicEnabled && currentBgm != null && !currentBgm.isPlaying()) {
            currentBgm.play();
        }
    }

    public boolean isMusicPlaying() {
        return currentBgm != null && currentBgm.isPlaying();
    }

    public String getCurrentBgmKey() {
        return currentBgmKey;
    }

    // ==================== Settings ====================

    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (enabled) {
            resumeMusic();
        } else {
            stopMusic();
        }
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setVolume(float volume) {
        this.musicVolume = Math.max(0f, Math.min(1f, volume));
        if (currentBgm != null) {
            currentBgm.setVolume(musicVolume);
        }
        // Also update all loaded BGM volumes
        for (Music music : bgmTracks.values()) {
            music.setVolume(musicVolume);
        }
    }

    public float getVolume() {
        return musicVolume;
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    // ==================== Sound Effects ====================

    public void playSound(String name) {
        Sound sound = soundEffects.get(name);
        if (sound != null) {
            sound.play(sfxVolume);
        }
    }

    public void playSound(String name, float volume) {
        Sound sound = soundEffects.get(name);
        if (sound != null) {
            sound.play(volume * sfxVolume);
        }
    }

    // ==================== Lifecycle ====================

    @Override
    public void dispose() {
        for (Music music : bgmTracks.values()) {
            if (music != null) {
                music.dispose();
            }
        }
        bgmTracks.clear();

        for (Sound sound : soundEffects.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        soundEffects.clear();

        currentBgm = null;
        currentBgmKey = null;
    }

    /**
     * Reset the singleton instance. Used for testing or game restart.
     */
    public static void resetInstance() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
}
