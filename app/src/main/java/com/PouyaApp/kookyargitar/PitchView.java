package com.PouyaApp.kookyargitar;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressLint("DrawAllocation")
public class PitchView extends SurfaceView implements Runnable {
    // Tuning parameters
    private float centerPitch = 0, currentPitch, midiRef;
    private int laFrequnes;
    
    // Animation and UI state variables
    private int alpha = 0;
    private final int alphaChangeSpeed = 10;
    private int alphaCent = 0;
    private int alphaChangeSpeedCent = 0;
    private final int alphaChangeSpeedCentUnit = 10;
    private boolean firstCenter = true;
    private boolean fixed;
    private final float speed = 0.12f;
    private float lastDX = -1;
    private int width, height;
    
    // Sound intensity gauge variables
    private float soundIntensity = 0f;
    private float targetIntensity = 0f;
    private final float intensitySpeed = 0.1f;
    
    // Performance optimization: Pre-allocated objects
    private final RectF tempRectF = new RectF();
    private final Paint segmentPaint = new Paint();
    private final Paint warningPaint = new Paint();
    private final int[] segmentColors = new int[20];
    private long lastAnimationUpdate = 0;
    private float animationPhase = 0f;
    
    private final Paint paint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private final Paint needlePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint arcPaint = new Paint();
    private final Paint shadowPaint = new Paint();
    
    Thread thread;
    SurfaceHolder surfaceHolder;
    boolean isItOK = false;

    public void pause() {
        isItOK = false;
        while (true) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }
    }

    public void resume() {
        isItOK = true;
        thread = new Thread(this);
        thread.start();
    }

    public float getCurrentPitch() {
        return currentPitch;
    }

    @Override
    public void run() {
        try {
            while (isItOK) {
                if (!surfaceHolder.getSurface().isValid()) {
                    continue;
                }

                Canvas canvas = surfaceHolder.lockCanvas();
                draw(canvas);
                surfaceHolder.unlockCanvasAndPost(canvas);
                try {
                    if (centerPitch == 0) thread.sleep(100);
                    else thread.sleep(33); // ~30 FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        surfaceHolder = getHolder();
        isItOK = true;
        
        // Initialize paint objects with anti-aliasing
        paint.setAntiAlias(true);
        backgroundPaint.setAntiAlias(true);
        needlePaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        arcPaint.setAntiAlias(true);
        shadowPaint.setAntiAlias(true);
        
        segmentPaint.setAntiAlias(true);
        warningPaint.setAntiAlias(true);
        warningPaint.setColor(Color.RED);
        warningPaint.setTextAlign(Align.CENTER);
        warningPaint.setFakeBoldText(true);
        
        preCalculateSegmentColors();
        
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setColor(Color.WHITE);
        
        needlePaint.setStrokeWidth(8.0f);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(6.0f);
        
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        shadowPaint.setStrokeWidth(10.0f);
        shadowPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    
    private void preCalculateSegmentColors() {
        for (int i = 0; i < 20; i++) {
            float intensity = (float) i / 20f;
            int red, green, blue;
            
            if (intensity < 0.3f) {
                red = 0;
                green = (int) (255 * intensity / 0.3f);
                blue = 255;
            } else if (intensity < 0.6f) {
                red = 0;
                green = 255;
                blue = (int) (255 * (0.6f - intensity) / 0.3f);
            } else if (intensity < 0.8f) {
                red = (int) (255 * (intensity - 0.6f) / 0.2f);
                green = 255;
                blue = 0;
            } else {
                red = 255;
                green = (int) (255 * (1.0f - intensity) / 0.2f);
                blue = 0;
            }
            
            segmentColors[i] = Color.argb(200, red, green, blue);
        }
    }

    public void start() {
        isItOK = true;
        thread = new Thread(this);
        thread.start();
    }

    public PitchView(Context context) {
        super(context);
        init();
        start();
    }

    public PitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        start();
    }

    public PitchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        start();
    }

    public void setCenterPitch(float centerPitch) {
        alpha = 0;
        this.centerPitch = centerPitch;
        firstCenter = false;
    }

    public void setMidiRef(float midiRef) {
        this.midiRef = midiRef;
    }

    public float getCenterPitch() {
        return centerPitch;
    }

    public void setCurrentPitch(float currentPitch) {
        if (currentPitch <= 12) {
            alphaChangeSpeedCent = -alphaChangeSpeedCentUnit;
        } else {
            alphaChangeSpeedCent = alphaChangeSpeedCentUnit;
        }
        this.currentPitch = currentPitch;
        targetIntensity = (currentPitch > 12) ? Math.min(1.0f, currentPitch / 100f) : 0.1f;
    }

    public void setAFrequnse(int Frequnes) {
        this.laFrequnes = Frequnes;
    }
    
    // Keep old name for backward compatibility
    public void setLaFrequnse(int Frequnes) {
        setAFrequnse(Frequnes);
    }
    
    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h / 2, oldw, oldh);
        width = w;
        height = h;
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        
        if (centerPitch == 0) {
            drawBackground(canvas);
            drawPlaceholderInterface(canvas);
            return;
        }

        updateSoundIntensity();
        drawBackground(canvas);
        
        float halfWidth = width / 2f;
        float centerY = height * 0.68f;
        float radius = Math.min(width, height) * 0.25f;

        drawInfoHeader(canvas);
        drawSoundIntensityGauge(canvas);
        drawTuningArc(canvas, halfWidth, centerY, radius);
        drawTickMarks(canvas, halfWidth, centerY, radius);

        float dx = (currentPitch - centerPitch) / 2f;
        boolean inTune = (-0.1 < dx && dx < 0.1);
        
        updateNeedlePosition(dx);
        drawStatusIndicator(canvas, dx, inTune);
        drawEnhancedNeedle(canvas, halfWidth, centerY, radius, lastDX, inTune);
        drawEnhancedCenterHub(canvas, halfWidth, centerY, inTune && fixed);
        drawEnhancedNoteInfo(canvas, halfWidth, centerY - radius - 70);
        drawFrequencyInfo(canvas, halfWidth, height * 0.98f);
    }

    private void updateSoundIntensity() {
        if (Math.abs(soundIntensity - targetIntensity) > 0.01f) {
            if (soundIntensity < targetIntensity) {
                soundIntensity = Math.min(targetIntensity, soundIntensity + intensitySpeed);
            } else {
                soundIntensity = Math.max(targetIntensity, soundIntensity - intensitySpeed);
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        LinearGradient gradient = new LinearGradient(0, 0, 0, height,
                Color.argb(255, 25, 25, 40),
                Color.argb(255, 15, 15, 25), Shader.TileMode.CLAMP);
        backgroundPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        backgroundPaint.setShader(null);
        
        Paint texturePaint = new Paint();
        texturePaint.setColor(Color.argb(10, 255, 255, 255));
        for (int i = 0; i < width; i += 4) {
            canvas.drawLine(i, 0, i, height, texturePaint);
        }
    }

    private void drawPlaceholderInterface(Canvas canvas) {
        textPaint.setTextSize(width / 20f);
        textPaint.setColor(Color.argb(120, 255, 255, 255));
        textPaint.setTextAlign(Align.CENTER);
        canvas.drawText("سیم مورد نظر را انتخاب کنید", width / 2f, height / 2f, textPaint);
        
        float pulseRadius = (float) (width / 8f + Math.sin(System.currentTimeMillis() / 500.0) * 10);
        Paint pulsePaint = new Paint();
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2.0f);
        pulsePaint.setColor(Color.argb(60, 100, 200, 255));
        canvas.drawCircle(width / 2f, height / 2f, pulseRadius, pulsePaint);
    }

    private void drawInfoHeader(Canvas canvas) {
        textPaint.setTextSize(width / 28f);
        textPaint.setTextAlign(Align.LEFT);
        
        RectF freqBg = new RectF(10, 10, width / 3f, 50);
        backgroundPaint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawRoundRect(freqBg, 15, 15, backgroundPaint);
        
        textPaint.setColor(Color.CYAN);
        canvas.drawText(laFrequnes + " Hz", 20, 35, textPaint);
        
        textPaint.setTextAlign(Align.RIGHT);
        RectF typeBg = new RectF(width * 2f / 3f, 10, width - 10, 50);
        canvas.drawRoundRect(typeBg, 15, 15, backgroundPaint);
        textPaint.setColor(Color.YELLOW);
        canvas.drawText("گیتار", width - 20, 35, textPaint);
    }

    private void drawSoundIntensityGauge(Canvas canvas) {
        float gaugeWidth = width * 0.85f;
        float gaugeHeight = 40f;
        float gaugeX = (width - gaugeWidth) / 2f;
        float gaugeY = height * 0.16f;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationUpdate > 50) {
            animationPhase = (float) Math.sin(currentTime / 300.0) * 0.2f + 0.8f;
            lastAnimationUpdate = currentTime;
        }
        
        tempRectF.set(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight);
        backgroundPaint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawRoundRect(tempRectF, 20, 20, backgroundPaint);
        
        int segmentCount = 20;
        float segmentWidth = (gaugeWidth - 4) / segmentCount;
        int currentLevel = (int) (soundIntensity * segmentCount);
        
        for (int i = 0; i <= Math.min(currentLevel, segmentCount - 1); i++) {
            float segmentX = gaugeX + 2 + (i * segmentWidth);
            tempRectF.set(segmentX, gaugeY + 2, segmentX + segmentWidth - 2, gaugeY + gaugeHeight - 2);
            
            int baseColor = segmentColors[i];
            int alpha = (int) (Color.alpha(baseColor) * animationPhase);
            segmentPaint.setColor(Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
            segmentPaint.clearShadowLayer();
            
            canvas.drawRoundRect(tempRectF, 6, 6, segmentPaint);
        }
        
        segmentPaint.setColor(Color.argb(40, 100, 100, 100));
        for (int i = Math.max(0, currentLevel + 1); i < segmentCount; i++) {
            float segmentX = gaugeX + 2 + (i * segmentWidth);
            tempRectF.set(segmentX, gaugeY + 2, segmentX + segmentWidth - 2, gaugeY + gaugeHeight - 2);
            canvas.drawRoundRect(tempRectF, 6, 6, segmentPaint);
        }
        
        if (soundIntensity > 0.8f) {
            warningPaint.setTextSize(width / 30f);
            canvas.drawText("⚠", gaugeX + gaugeWidth + 20, gaugeY + gaugeHeight / 2f + 5, warningPaint);
        }
        
        textPaint.setTextSize(width / 32f);
        textPaint.setColor(Color.argb(180, 255, 255, 255));
        textPaint.setTextAlign(Align.LEFT);
        canvas.drawText("آرام", gaugeX, gaugeY - 15, textPaint);
        textPaint.setTextAlign(Align.RIGHT);
        canvas.drawText("بلند", gaugeX + gaugeWidth, gaugeY - 15, textPaint);
        
        textPaint.setTextSize(width / 28f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setFakeBoldText(true);
        canvas.drawText("شدت صدا", width / 2f, gaugeY - 25, textPaint);
        textPaint.setFakeBoldText(false);
        
        textPaint.setTextSize(width / 35f);
        textPaint.setColor(Color.argb(200, 255, 255, 255));
        canvas.drawText(String.format("%.0f%%", soundIntensity * 100), width / 2f, gaugeY + gaugeHeight + 35, textPaint);
    }

    private void drawTuningArc(Canvas canvas, float centerX, float centerY, float radius) {
        RectF arcRect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(8.0f);
        RectF shadowRect = new RectF(centerX - radius + 2, centerY - radius + 2, 
                                   centerX + radius + 2, centerY + radius + 2);
        canvas.drawArc(shadowRect, 135, 270, false, shadowPaint);
        
        arcPaint.setColor(Color.argb(60, 255, 255, 255));
        arcPaint.setStrokeWidth(8.0f);
        canvas.drawArc(arcRect, 135, 270, false, arcPaint);
        
        arcPaint.setStrokeWidth(6.0f);
        arcPaint.setColor(Color.argb(100, 255, 100, 100));
        canvas.drawArc(arcRect, 135, 90, false, arcPaint);
        canvas.drawArc(arcRect, 315, 90, false, arcPaint);
        
        arcPaint.setColor(Color.argb(120, 100, 255, 100));
        canvas.drawArc(arcRect, 225, 90, false, arcPaint);
    }

    private void drawTickMarks(Canvas canvas, float centerX, float centerY, float radius) {
        Paint tickPaint = new Paint();
        tickPaint.setAntiAlias(true);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        for (int i = -4; i <= 4; i++) {
            double angle = i * Math.PI / 8;
            float startRadius = radius * 0.85f;
            float endRadius = radius * (i % 2 == 0 ? 0.95f : 0.90f);
            
            float startX = centerX + (float) Math.sin(angle) * startRadius;
            float startY = centerY - (float) Math.cos(angle) * startRadius;
            float endX = centerX + (float) Math.sin(angle) * endRadius;
            float endY = centerY - (float) Math.cos(angle) * endRadius;
            
            if (i == 0) {
                tickPaint.setStrokeWidth(6.0f);
                tickPaint.setColor(Color.GREEN);
                canvas.drawLine(startX, startY, endX, endY, tickPaint);
                tickPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(endX, endY, 4, tickPaint);
                tickPaint.setStyle(Paint.Style.STROKE);
            } else if (Math.abs(i) == 1) {
                tickPaint.setStrokeWidth(4.0f);
                tickPaint.setColor(Color.YELLOW);
                canvas.drawLine(startX, startY, endX, endY, tickPaint);
            } else {
                tickPaint.setStrokeWidth(2.0f);
                tickPaint.setColor(Color.WHITE);
                canvas.drawLine(startX, startY, endX, endY, tickPaint);
            }
        }
    }

    private void updateNeedlePosition(float dx) {
        dx = Math.max(-1, Math.min(1, dx));
        dx = Math.round(dx * 100) / 100f;
        lastDX = Math.round(lastDX * 100) / 100f;

        if (dx > lastDX && (lastDX + speed) <= dx) {
            lastDX += speed;
            fixed = false;
        } else if (dx < lastDX && (lastDX - speed) >= dx) {
            lastDX -= speed;
            fixed = false;
        } else {
            lastDX = dx;
            fixed = true;
        }
    }

    private void drawStatusIndicator(Canvas canvas, float dx, boolean inTune) {
        textPaint.setTextSize(width / 24f);
        textPaint.setTextAlign(Align.CENTER);
        
        RectF statusBg = new RectF(width * 0.1f, height * 0.9f - 20, width * 0.9f, height * 0.9f + 20);
        backgroundPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRoundRect(statusBg, 20, 20, backgroundPaint);
        
        if (inTune) {
            textPaint.setColor(Color.GREEN);
            canvas.drawText("✓ کوک شده", width / 2f, height * 0.9f + 5, textPaint);
        } else {
            textPaint.setColor(Color.YELLOW);
            String message;
            if (dx < -1) {
                message = (currentPitch == 12) ? "⚡ به سیم ضربه بزنید" : "⬆ سیم را سفت کنید";
            } else {
                message = "⬇ سیم را شل کنید";
            }
            canvas.drawText(message, width / 2f, height * 0.9f + 5, textPaint);
        }
    }

    private void drawEnhancedNeedle(Canvas canvas, float centerX, float centerY, float radius, float dx, boolean inTune) {
        dx = Math.max(-1, Math.min(1, dx));
        double phi = dx * Math.PI / 4;
        
        float needleLength = radius * 0.8f;
        float needleX = centerX + (float) Math.sin(phi) * needleLength;
        float needleY = centerY - (float) Math.cos(phi) * needleLength;
        
        shadowPaint.setStrokeWidth(10.0f);
        canvas.drawLine(centerX + 3, centerY + 3, needleX + 3, needleY + 3, shadowPaint);
        
        needlePaint.setStrokeWidth(8.0f);
        if (inTune) {
            needlePaint.setColor(Color.GREEN);
        } else {
            float intensity = Math.abs(dx);
            int red = (int) (255 * intensity);
            int green = (int) (255 * (1 - intensity));
            needlePaint.setColor(Color.rgb(red, green, 0));
        }
        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint);
        
        Paint tipPaint = new Paint();
        tipPaint.setAntiAlias(true);
        tipPaint.setStyle(Paint.Style.FILL);
        tipPaint.setColor(needlePaint.getColor());
        canvas.drawCircle(needleX, needleY, 6, tipPaint);
    }

    private void drawEnhancedCenterHub(Canvas canvas, float centerX, float centerY, boolean tuned) {
        float hubRadius = width / 35f;
        
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX + 2, centerY + 2, hubRadius + 2, shadowPaint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6.0f);
        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(centerX, centerY, hubRadius + 6, paint);
        
        paint.setStrokeWidth(3.0f);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, hubRadius + 3, paint);
        
        paint.setStyle(Paint.Style.FILL);
        if (tuned) {
            paint.setColor(Color.GREEN);
        } else {
            int alpha = (int) (128 + 127 * Math.sin(System.currentTimeMillis() / 300.0));
            paint.setColor(Color.argb(alpha, 255, 255, 0));
        }
        canvas.drawCircle(centerX, centerY, hubRadius, paint);
        
        paint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, hubRadius / 3f, paint);
    }

    private void drawEnhancedNoteInfo(Canvas canvas, float centerX, float centerY) {
        if (alpha + alphaChangeSpeed <= 255) {
            alpha += alphaChangeSpeed;
        } else {
            alpha = 255;
        }
        
        RectF noteBg = new RectF(centerX - width / 4f, centerY - 30, centerX + width / 4f, centerY + 30);
        backgroundPaint.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawRoundRect(noteBg, 15, 15, backgroundPaint);
        
        textPaint.setAlpha(alpha);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setTextSize(width / 20f);
        textPaint.setFakeBoldText(true);
        
        String noteName = drawName();
        canvas.drawText(noteName, centerX, centerY - 5, textPaint);
        
        textPaint.setTextSize(width / 28f);
        textPaint.setColor(Color.CYAN);
        textPaint.setFakeBoldText(false);
        canvas.drawText(String.format("%.1f Hz", midiToFreq(centerPitch)), centerX, centerY + 30, textPaint);
    }

    private void drawFrequencyInfo(Canvas canvas, float centerX, float centerY) {
        if (alphaChangeSpeedCent > 0) {
            alphaCent = Math.min(255, alphaCent + alphaChangeSpeedCent);
        } else if (alphaChangeSpeedCent < 0) {
            alphaCent = Math.max(0, alphaCent + alphaChangeSpeedCent);
        }

        textPaint.setAlpha(alphaCent);
        textPaint.setColor(Color.CYAN);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setTextSize(width / 28f);
        
        float dif = (currentPitch - centerPitch) * 100;
        dif = Math.round(dif * 10) / 10f;
        String sign = (dif > 0) ? "+" : "";
        
        canvas.drawText(String.format("%.1f Hz / Cent: %s%.1f", 
            midiToFreq(currentPitch), sign, dif), centerX, centerY, textPaint);
    }

    private float midiToFreq(float midi) {
        double frequens = 440 * Math.pow(2, ((midi - 69) / 12));
        frequens = Math.round(frequens * 100) / 100.0;
        return (float) frequens;
    }

    private String drawName() {
        String[] noteName = {"دو", "ر بمل", "ر", "می بمل", "می", "فا",
                "سل بمل", "سل", "لا بمل", "لا", "سی بمل", "سی"};
        String[] positionName = {"اول", "دوم", "سوم", "چهارم", "پنجم",
                "ششم", "هفتم"};
        float tone = midiRef % 1;
        float midi = (int) midiRef;
        int noteIndex = (int) (midi % 12);
        int position = (int) (midi / 12) - 2;
        String drawName;
        if (tone == 0) {
            drawName = noteName[noteIndex] + " اکتاو " + positionName[position];
        } else {
            if (noteIndex == 0 || noteIndex == 2 || noteIndex == 4
                    || noteIndex == 5 || noteIndex == 7
                    || noteIndex == 9 || noteIndex == 11) {
                drawName = noteName[noteIndex] + " سری " + " اکتاو " + positionName[position];
            } else {
                drawName = noteName[(noteIndex + 1)] + " کرن " + " اکتاو " + positionName[position];
            }
        }
        return drawName;
    }
}