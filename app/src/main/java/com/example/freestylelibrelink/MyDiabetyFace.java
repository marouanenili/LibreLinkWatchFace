package com.example.freestylelibrelink;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MyDiabetyFace extends CanvasWatchFaceService {
    private ApiManager apiManager;

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;
    private static final long UPDATE_INTERVAL_MS = 60000; // 1 minute en millisecondes
    private final Handler mHandler = new Handler();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(MyDiabetyFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyDiabetyFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private int mTic;
        private String mToken;
        private String mId;
        private String mValueInMgPerDl;
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        // Runnable pour effectuer l'appel API périodiquement
        private final Runnable mFetchDataRunnable = new Runnable() {
            @Override
            public void run() {
                fetchDataFromApi(); // Appeler la méthode pour récupérer les données de l'API
                mHandler.postDelayed(this, UPDATE_INTERVAL_MS); // Planifier le prochain appel
            }
        };
        private void fetchDataFromApi() {
            // Exécuter l'appel API de manière asynchrone
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Fetching data from API...");
                    try {
                        JSONObject cgmData = apiManager.getCGMData(mToken, mId).getJSONObject("data").getJSONObject("connection");
                        JSONObject GlucoseMeasurment = cgmData.getJSONObject("glucoseMeasurement");
                        mValueInMgPerDl = GlucoseMeasurment.getString("ValueInMgPerDl");
                        // Demander à redessiner l'UI après avoir obtenu les données

                    } catch (JSONException | IOException e) {
                        System.out.println("exception :"+e.getMessage());
                        mValueInMgPerDl = e.getMessage();
                        e.printStackTrace();
                    }
                    invalidate();
                }
            }).start();
        }

        // Méthode pour démarrer la mise à jour périodique
        private void startPeriodicUpdates() {
            // Planifier le premier appel après un délai initial
            mHandler.postDelayed(mFetchDataRunnable, UPDATE_INTERVAL_MS);
        }

        // Méthode pour arrêter la mise à jour périodique
        private void stopPeriodicUpdates() {
            mHandler.removeCallbacks(mFetchDataRunnable);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            startPeriodicUpdates();

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyDiabetyFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.GREEN);

            mTextPaint = new Paint();
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextSize(50f); // Taille du texte en pixels
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTic = 0;
            mToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjFiZWU0MjM2LTljZDItZTYxMS04MTI4LTA2MTBlNmUzOGNiZCIsImZpcnN0TmFtZSI6Ik1hcm91YW5lIiwibGFzdE5hbWUiOiJOaWxsaSIsImNvdW50cnkiOiJGUiIsInJlZ2lvbiI6ImZyIiwicm9sZSI6InBhdGllbnQiLCJ1bml0cyI6MSwicHJhY3RpY2VzIjpbXSwiYyI6MSwicyI6ImxsdS5hbmRyb2lkIiwiZXhwIjoxNzM0NjM2NTE2fQ.c9iozBFOp1SNZFs9WuIZ_tuYx_In-ffgUS2SfbiDMrA";
            mId = "1bee4236-9cd2-e611-8128-0610e6e38cbd";
            mValueInMgPerDl = "pas d'information";

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
            mTic = (mTic + 1) % 60;
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            invalidate();
            updateWatchPaints();
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            // Redraw the watch face based on mute mode
            if (inMuteMode) {
                // Adjust paint colors or other visual elements for mute mode
            } else {
                // Restore normal paint colors or elements
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // Perform any setup or resizing necessary based on the surface dimensions.
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            // Register the time zone receiver here if needed
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            // Unregister the time zone receiver here if needed
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            // Handle tap actions here if needed
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            try {
                drawTime(canvas);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void drawTime(Canvas canvas) throws JSONException, IOException {
            String timeText = String.format("%02d:%02d:%02d",
                    mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE),
                    mCalendar.get(Calendar.SECOND));
            canvas.drawColor(Color.BLACK);
            float TimeX = canvas.getWidth() / 2f;
            float TimeY = canvas.getHeight() / 4f;
            String DateText = String.format("%02d/%02d/%04d",
                    mCalendar.get(Calendar.DAY_OF_MONTH),
                    mCalendar.get(Calendar.MONTH) + 1,
                    mCalendar.get(Calendar.YEAR));
            float DateX = canvas.getWidth() / 2f;
            float DateY = canvas.getHeight() / 1.5f;
            canvas.drawText(DateText, DateX, DateY, mTextPaint);
            canvas.drawText(timeText, TimeX, TimeY, mTextPaint);
            canvas.drawText(mValueInMgPerDl, canvas.getWidth() / 2f, canvas.getHeight() / 2f, mTextPaint);

        }

        private void updateWatchPaints() {
            // Update paints based on ambient mode if needed
        }
    }
}
