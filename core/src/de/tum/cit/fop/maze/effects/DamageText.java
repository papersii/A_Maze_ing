package de.tum.cit.fop.maze.effects;

import com.badlogic.gdx.math.Vector2;

public class DamageText {
    public float x, y;
    public String text;
    public float timer;
    public float maxTime;
    private float velocityY;

    public DamageText(float x, float y, int damage) {
        this.x = x;
        this.y = y;
        this.text = "-" + damage;
        this.maxTime = 1.0f; // Lasts 1 second
        this.timer = maxTime;
        this.velocityY = 2.0f; // Float up speed
    }

    public void update(float delta) {
        y += velocityY * delta;
        timer -= delta;
    }

    public boolean isExpired() {
        return timer <= 0;
    }
}
