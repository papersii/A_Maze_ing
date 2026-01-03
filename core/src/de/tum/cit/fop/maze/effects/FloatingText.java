package de.tum.cit.fop.maze.effects;

import com.badlogic.gdx.graphics.Color;

public class FloatingText {
    public float x, y;
    public String text;
    public float timer;
    public float maxTime;
    private float velocityY;
    public Color color;

    // Warning: Legacy constructor-ish behavior can be supported or we just migrate
    public FloatingText(float x, float y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.maxTime = 1.0f;
        this.timer = maxTime;
        this.velocityY = 2.0f;
    }

    public void update(float delta) {
        y += velocityY * delta;
        timer -= delta;
    }

    public boolean isExpired() {
        return timer <= 0;
    }
}
