package com.example.freestylelibrelink;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MyDiabetyFace extends CanvasWatchFaceService {

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;
    private static final long UPDATE_INTERVAL_MS = 600000; // 10 minutes

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ApiManager apiManager;
    private CredentialsManager credentialsManager;
    private GlucoseManager glucoseManager;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Engine engine = mWeakReference.get();
            if (engine != null && msg.what == MSG_UPDATE_TIME) {
                engine.handleUpdateTimeMessage();
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private Calendar calendar;
        private Paint backgroundPaint;
        private Paint textPaint;
        private Paint glucosePaint;
        private Paint batteryPaint;

        private boolean ambient;
        private int tic;
        private String valueInMgPerDl = "Chargement...";
        private String batteryLevel = "Chargement...";
        private String correction = "Chargement...";

        private final Handler updateTimeHandler = new EngineHandler(this);

        private final Runnable fetchDataRunnable = new Runnable() {
            @Override
            public void run() {
                fetchDataFromApi();
                mainHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        private void fetchDataFromApi() {
            new Thread(() -> {
                try {
                    credentialsManager = new CredentialsManager("test", "test");
                    apiManager = new ApiManager();
                    glucoseManager = new GlucoseManager(apiManager);

                    String glucoseValue = glucoseManager.getLatestValue(
                            credentialsManager.getToken(),
                            credentialsManager.getPatientIdSha256(),
                            credentialsManager.getPatientId()
                    );

                    valueInMgPerDl = glucoseValue + " mg/dL";
                    correction = glucoseManager.getCorrection(glucoseValue);
                    // Update glucose color based on value
                    updateGlucoseColor(glucoseValue);

                } catch (JSONException | IOException e) {
                    valueInMgPerDl = "Erreur de données";
                    e.printStackTrace();
                }

                // Fetch battery level
                batteryLevel = getBatteryLevel();

                // Force UI update on main thread
                mainHandler.post(this::invalidate);
            }).start();
        }

        private String getBatteryLevel() {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int level = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
            return level + "%";
        }

        private void updateGlucoseColor(String glucoseValueString) {
            if (glucoseValueString == "Chargement..." || glucoseValueString == "Erreur de données") {
                glucosePaint.setColor(Color.WHITE);
            }
            int glucoseValue = Integer.parseInt(glucoseValueString);

            // Update glucose color based on value range
            if (glucoseValue < 70) {
                glucosePaint.setColor(Color.BLUE);  // Low glucose - blue
            } else if (glucoseValue >= 70 && glucoseValue <= 140) {
                glucosePaint.setColor(Color.GREEN);  // Normal glucose - green
            } else {
                glucosePaint.setColor(Color.RED);  // High glucose - red
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyDiabetyFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            calendar = Calendar.getInstance();

            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);

            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(50f);
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.CENTER);

            glucosePaint = new Paint();
            glucosePaint.setTextSize(40f);
            glucosePaint.setAntiAlias(true);
            glucosePaint.setTextAlign(Paint.Align.CENTER);

            batteryPaint = new Paint();
            batteryPaint.setColor(Color.YELLOW); // Set battery text color
            batteryPaint.setTextSize(30f);
            batteryPaint.setAntiAlias(true);
            batteryPaint.setTextAlign(Paint.Align.CENTER);

            fetchDataFromApi(); // Initial fetch
            startPeriodicUpdates();
        }

        private void startPeriodicUpdates() {
            mainHandler.postDelayed(fetchDataRunnable, UPDATE_INTERVAL_MS);
        }

        private void stopPeriodicUpdates() {
            mainHandler.removeCallbacks(fetchDataRunnable);
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            stopPeriodicUpdates();
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
            tic = (tic + 1) % 60;
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            ambient = inAmbientMode;
            invalidate();
            updateTimer();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }

            updateTimer();
        }

        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !ambient;
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar.setTimeInMillis(System.currentTimeMillis());

            canvas.drawColor(Color.BLACK);
            String timeText = String.format("%02d:%02d:%02d",
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));

            if (ambient) {
                // Si la montre est en mode veille, afficher uniquement l'heure

                float centerX = canvas.getWidth() / 2f;
                float centerY = canvas.getHeight() / 2f;
                canvas.drawText(timeText, centerX, centerY, textPaint);  // Afficher uniquement l'heure
            }
            else{

                timeText = "🕒 " + timeText;

            String dateText = "📅 " + String.format("%02d/%02d/%04d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.YEAR));

            // Center of the circular watch face
            float centerX = canvas.getWidth() / 2f;
            float centerY = canvas.getHeight() / 2f;
            float radius = Math.min(canvas.getWidth(), canvas.getHeight()) / 2f;

            // Offsets for positioning text within the circular watch face
            float timeYOffset = -radius / 3f;
            float glucoseYOffset = 0;
            float dateYOffset = radius / 1.8f;
            float batteryYOffset = radius / 1.2f;
            float correctionYOffset = radius / 3f;

            // Draw time at the top
            canvas.drawText(timeText, centerX, centerY + timeYOffset, textPaint);

            // Draw blood drop emoji and glucose value in the center
            canvas.drawText("🩸 " + valueInMgPerDl, centerX, centerY + glucoseYOffset, glucosePaint);
            canvas.drawText("\uD83D\uDC89" + correction, centerX, centerY + correctionYOffset, glucosePaint);

            // Draw date near the bottom
            canvas.drawText(dateText, centerX, centerY + dateYOffset, textPaint);

            // Draw battery emoji and level towards the bottom
            canvas.drawText("🔋 " + batteryLevel, centerX, centerY + batteryYOffset, batteryPaint);


        }}
    }
}
