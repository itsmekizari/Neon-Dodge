package com.train.neondodge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/** A complete no-assets arcade game drawn with Android Canvas. */
public class GameView extends View {
    private static final int READY = 0;
    private static final int PLAYING = 1;
    private static final int GAME_OVER = 2;

    private static final int BG = Color.rgb(6, 8, 22);
    private static final int WHITE = Color.rgb(239, 247, 255);
    private static final int MUTED = Color.rgb(142, 158, 188);
    private static final int CYAN = Color.rgb(69, 231, 255);
    private static final int RED = Color.rgb(255, 76, 112);
    private static final int PURPLE = Color.rgb(137, 107, 255);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<FallingThing> things = new ArrayList<>();
    private final Star[] stars = new Star[72];
    private final SharedPreferences prefs;
    private final float density;

    private int state = READY;
    private int score = 0;
    private int bestScore = 0;
    private int bonusScore = 0;
    private float playerX;
    private float targetX;
    private float elapsed;
    private float obstacleTimer;
    private float pickupTimer;
    private float lastFrameSeconds = 0f;
    private float screenW;
    private float screenH;
    private boolean appPaused = false;

    public GameView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        prefs = context.getSharedPreferences("neon_dodge_save", Context.MODE_PRIVATE);
        bestScore = prefs.getInt("best_score", 0);
        setFocusable(true);
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(random.nextFloat(), random.nextFloat(),
                    (0.7f + random.nextFloat() * 1.8f) * density,
                    0.15f + random.nextFloat() * 0.85f);
        }
    }

    public void setAppPaused(boolean paused) {
        appPaused = paused;
        lastFrameSeconds = System.nanoTime() / 1_000_000_000f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        screenW = getWidth();
        screenH = getHeight();
        if (screenW <= 0 || screenH <= 0) return;

        float now = System.nanoTime() / 1_000_000_000f;
        float dt = lastFrameSeconds <= 0 ? 0.016f : Math.min(0.035f, Math.max(0f, now - lastFrameSeconds));
        lastFrameSeconds = now;

        drawBackground(canvas, now);
        if (appPaused) {
            drawPauseOverlay(canvas);
            postInvalidateDelayed(100);
            return;
        }

        if (state == PLAYING) updateGame(dt);
        if (state == PLAYING || state == GAME_OVER) drawThings(canvas);
        if (state != READY) drawPlayer(canvas);
        drawHud(canvas);

        if (state == READY) drawStartScreen(canvas);
        else if (state == GAME_OVER) drawGameOver(canvas);

        postInvalidateDelayed(16);
    }

    private void drawBackground(Canvas canvas, float now) {
        paint.setShader(new LinearGradient(0, 0, screenW, screenH, BG,
                Color.rgb(12, 16, 42), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, screenW, screenH, paint);
        paint.setShader(null);

        // Faint grid gives the playfield a simple synthwave/arcade feel.
        paint.setStrokeWidth(1f * density);
        paint.setColor(Color.argb(22, 85, 125, 200));
        float gap = 38f * density;
        for (float x = 0; x < screenW; x += gap) canvas.drawLine(x, 0, x, screenH, paint);
        for (float y = 0; y < screenH; y += gap) canvas.drawLine(0, y, screenW, y, paint);

        for (Star star : stars) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(now * 1.2 + star.x * 21f);
            paint.setColor(Color.argb((int) (star.alpha * pulse * 150), 145, 190, 255));
            canvas.drawCircle(star.x * screenW, star.y * screenH, star.radius, paint);
        }
        paint.setColor(Color.argb(70, 69, 231, 255));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * density);
        canvas.drawRoundRect(new RectF(8 * density, 8 * density,
                screenW - 8 * density, screenH - 8 * density), 18 * density, 18 * density, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void updateGame(float dt) {
        elapsed += dt;
        score = (int) (elapsed * 10f) + bonusScore;
        float margin = 26f * density;
        playerX += (targetX - playerX) * Math.min(1f, dt * 13f);
        playerX = Math.max(margin, Math.min(screenW - margin, playerX));

        obstacleTimer -= dt;
        if (obstacleTimer <= 0f) {
            float size = (12f + random.nextFloat() * 9f) * density;
            float x = size + random.nextFloat() * Math.max(1f, screenW - size * 2f);
            float speed = (190f + Math.min(250f, elapsed * 5.2f)) * density;
            things.add(new FallingThing(x, -size * 2f, size, speed, false));
            obstacleTimer = Math.max(0.34f, 0.82f - elapsed * 0.006f) * (0.72f + random.nextFloat() * 0.55f);
        }

        pickupTimer -= dt;
        if (pickupTimer <= 0f) {
            float size = 9f * density;
            float x = size + random.nextFloat() * Math.max(1f, screenW - size * 2f);
            float speed = (145f + Math.min(150f, elapsed * 2.2f)) * density;
            things.add(new FallingThing(x, -size * 2f, size, speed, true));
            pickupTimer = 1.4f + random.nextFloat() * 1.1f;
        }

        float playerY = screenH * 0.82f;
        float playerRadius = 16f * density;
        Iterator<FallingThing> iterator = things.iterator();
        while (iterator.hasNext()) {
            FallingThing thing = iterator.next();
            thing.y += thing.speed * dt;
            if (thing.y - thing.size > screenH + 12f * density) {
                iterator.remove();
                continue;
            }
            float dx = thing.x - playerX;
            float dy = thing.y - playerY;
            float hitRadius = playerRadius + thing.size * (thing.pickup ? 0.92f : 0.82f);
            if (dx * dx + dy * dy < hitRadius * hitRadius) {
                if (thing.pickup) {
                    bonusScore += 25;
                    score += 25;
                    iterator.remove();
                } else {
                    finishGame();
                    return;
                }
            }
        }
    }

    private void drawThings(Canvas canvas) {
        for (FallingThing thing : things) {
            if (thing.pickup) {
                paint.setColor(Color.argb(48, 69, 231, 255));
                canvas.drawCircle(thing.x, thing.y, thing.size * 2.1f, paint);
                paint.setColor(CYAN);
                canvas.drawCircle(thing.x, thing.y, thing.size, paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(thing.x - thing.size * 0.25f,
                        thing.y - thing.size * 0.25f, thing.size * 0.22f, paint);
            } else {
                paint.setColor(Color.argb(35, 255, 76, 112));
                canvas.drawCircle(thing.x, thing.y, thing.size * 1.75f, paint);
                canvas.save();
                canvas.rotate((thing.y * 0.18f) % 360f, thing.x, thing.y);
                paint.setColor(RED);
                canvas.drawRoundRect(new RectF(thing.x - thing.size, thing.y - thing.size,
                        thing.x + thing.size, thing.y + thing.size), 4 * density, 4 * density, paint);
                paint.setColor(Color.rgb(255, 171, 190));
                canvas.drawRoundRect(new RectF(thing.x - thing.size * 0.48f,
                        thing.y - thing.size * 0.48f, thing.x + thing.size * 0.48f,
                        thing.y + thing.size * 0.48f), 2 * density, 2 * density, paint);
                canvas.restore();
            }
        }
    }

    private void drawPlayer(Canvas canvas) {
        float x = playerX;
        float y = screenH * 0.82f;
        float r = 16f * density;
        paint.setColor(Color.argb(40, 69, 231, 255));
        canvas.drawCircle(x, y, r * 2.2f, paint);
        paint.setColor(Color.argb(65, 69, 231, 255));
        canvas.drawCircle(x, y, r * 1.6f, paint);
        paint.setColor(CYAN);
        Path ship = new Path();
        ship.moveTo(x, y - r * 1.35f);
        ship.lineTo(x + r * 0.92f, y + r * 0.8f);
        ship.lineTo(x, y + r * 0.4f);
        ship.lineTo(x - r * 0.92f, y + r * 0.8f);
        ship.close();
        canvas.drawPath(ship, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, r * 0.25f, paint);
    }

    private void drawHud(Canvas canvas) {
        float pad = 22f * density;
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(11f * density);
        paint.setColor(MUTED);
        canvas.drawText("SCORE", pad, 28f * density, paint);
        paint.setTextSize(24f * density);
        paint.setColor(WHITE);
        canvas.drawText(String.format(java.util.Locale.US, "%05d", score), pad, 54f * density, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(11f * density);
        paint.setColor(MUTED);
        canvas.drawText("BEST", screenW - pad, 28f * density, paint);
        paint.setTextSize(20f * density);
        paint.setColor(CYAN);
        canvas.drawText(String.format(java.util.Locale.US, "%05d", bestScore), screenW - pad, 52f * density, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        if (state == PLAYING) {
            paint.setTextSize(10f * density);
            paint.setColor(Color.argb(165, 150, 166, 196));
            canvas.drawText("DRAG TO MOVE", pad, screenH - 22f * density, paint);
        }
    }

    private void drawStartScreen(Canvas canvas) {
        float centerY = screenH * 0.39f;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        paint.setTextSize(43f * density);
        paint.setColor(WHITE);
        canvas.drawText("NEON", screenW / 2f, centerY, paint);
        paint.setColor(CYAN);
        canvas.drawText("DODGE", screenW / 2f, centerY + 45f * density, paint);

        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(12f * density);
        paint.setColor(MUTED);
        canvas.drawText("DODGE RED. COLLECT CYAN.", screenW / 2f, centerY + 79f * density, paint);

        drawPill(canvas, screenW / 2f, screenH * 0.68f, screenW * 0.67f,
                48f * density, CYAN, "TAP TO START", BG);
        paint.setTextSize(11f * density);
        paint.setColor(MUTED);
        canvas.drawText("Move your finger to steer", screenW / 2f,
                screenH * 0.68f + 70f * density, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setColor(Color.argb(188, 4, 6, 18));
        canvas.drawRect(0, 0, screenW, screenH, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        paint.setTextSize(36f * density);
        paint.setColor(RED);
        canvas.drawText("CRASHED", screenW / 2f, screenH * 0.38f, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(13f * density);
        paint.setColor(MUTED);
        canvas.drawText("YOUR SCORE", screenW / 2f, screenH * 0.445f, paint);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        paint.setTextSize(39f * density);
        paint.setColor(WHITE);
        canvas.drawText(String.valueOf(score), screenW / 2f, screenH * 0.505f, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(13f * density);
        paint.setColor(CYAN);
        canvas.drawText("BEST  " + bestScore, screenW / 2f, screenH * 0.55f, paint);
        drawPill(canvas, screenW / 2f, screenH * 0.66f, screenW * 0.72f,
                48f * density, CYAN, "TAP TO PLAY AGAIN", BG);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPauseOverlay(Canvas canvas) {
        paint.setColor(Color.argb(205, 4, 6, 18));
        canvas.drawRect(0, 0, screenW, screenH, paint);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(29f * density);
        paint.setColor(WHITE);
        canvas.drawText("PAUSED", screenW / 2f, screenH / 2f, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(13f * density);
        paint.setColor(MUTED);
        canvas.drawText("Return to the game to continue", screenW / 2f, screenH / 2f + 30f * density, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPill(Canvas canvas, float centerX, float centerY, float width,
                          float height, int accent, String label, int textColor) {
        RectF rect = new RectF(centerX - width / 2f, centerY - height / 2f,
                centerX + width / 2f, centerY + height / 2f);
        paint.setColor(Color.argb(32, Color.red(accent), Color.green(accent), Color.blue(accent)));
        canvas.drawRoundRect(new RectF(rect.left - 4 * density, rect.top - 4 * density,
                rect.right + 4 * density, rect.bottom + 4 * density), height, height, paint);
        paint.setColor(accent);
        canvas.drawRoundRect(rect, height, height, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        paint.setTextSize(13f * density);
        paint.setColor(textColor);
        canvas.drawText(label, centerX, centerY + 4.5f * density, paint);
    }

    private void startGame() {
        state = PLAYING;
        score = 0;
        bonusScore = 0;
        elapsed = 0f;
        obstacleTimer = 0.4f;
        pickupTimer = 1.1f;
        things.clear();
        playerX = screenW / 2f;
        targetX = playerX;
        lastFrameSeconds = System.nanoTime() / 1_000_000_000f;
    }

    private void finishGame() {
        state = GAME_OVER;
        if (score > bestScore) {
            bestScore = score;
            prefs.edit().putInt("best_score", bestScore).apply();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (state == READY || state == GAME_OVER) {
                startGame();
                targetX = x;
            } else {
                targetX = x;
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (state == PLAYING) targetX = x;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (state == PLAYING) targetX = x;
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static final class FallingThing {
        final float x;
        float y;
        final float size;
        final float speed;
        final boolean pickup;
        FallingThing(float x, float y, float size, float speed, boolean pickup) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.pickup = pickup;
        }
    }

    private static final class Star {
        final float x;
        final float y;
        final float radius;
        final float alpha;
        Star(float x, float y, float radius, float alpha) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.alpha = alpha;
        }
    }
}
