/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.PreferenceManager;

public final class RefreshUtils {

    private static final String REFRESH_CONTROL = "refresh_control";

    private static float defaultMaxRate;
    private static float defaultMinRate;
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private Context mContext;
    protected static boolean isAppInList = false;

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_60 = 1;
    protected static final int STATE_120 = 2;
    protected static final int STATE_60_LAND = 3;
    protected static final int STATE_120_LAND = 4;

    private static final float REFRESH_STATE_DEFAULT = 120f;
    private static final float REFRESH_STATE_60 = 60f;
    private static final float REFRESH_STATE_120 = 120f;
    private static final float REFRESH_STATE_60_LAND = 60f;
    private static final float REFRESH_STATE_120_LAND = 120f;

    private static final String REFRESH_60 = "refresh.60=";
    private static final String REFRESH_120 = "refresh.120=";
    private static final String REFRESH_60_LAND = "refresh.60land=";
    private static final String REFRESH_120_LAND = "refresh.120land=";

    private SharedPreferences mSharedPrefs;

    // 替换为 DisplayManager 的组件
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener;
    private Handler mHandler;
    private Runnable mPendingLowerRateTask;
    
    private boolean isLandscape = false;

    protected RefreshUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, RefreshService.class),
                UserHandle.CURRENT);
    }

    private void applyRefreshRate(float peakRate, float minRate) {
        if (mPendingLowerRateTask != null) {
            mHandler.removeCallbacks(mPendingLowerRateTask);
            mPendingLowerRateTask = null;
        }

        float currentPeakRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        if (peakRate < currentPeakRate) {
            mPendingLowerRateTask = () -> {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, peakRate);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
                mPendingLowerRateTask = null;
            };
            mHandler.postDelayed(mPendingLowerRateTask, 1000);
        } else {
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, peakRate);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
        }
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply();
    }

    protected void getOldRate(){
        defaultMaxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        defaultMinRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }

    private float getUserMaxRefreshRate() {
        return Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }

    private float getUserMinRefreshRate() {
        return Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }
    
    private void initializeDisplayListener(String packageName) {
        if (mDisplayListener != null) {
            disableDisplayListener();
        }

        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {}

            @Override
            public void onDisplayRemoved(int displayId) {}

            @Override
            public void onDisplayChanged(int displayId) {
                int currentOrientation = mContext.getResources().getConfiguration().orientation;
                boolean newIsLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
                if (newIsLandscape != isLandscape) {
                    isLandscape = finalIsLandscape;
                    adjustRefreshRateForOrientation(packageName);
                }
            }
        };

        if (mDisplayManager != null) {
            mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
        }

        int currentOrientation = mContext.getResources().getConfiguration().orientation;
        isLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private void adjustRefreshRateForOrientation(String packageName) {
        int state = getStateForPackage(packageName);
        int currentOrientation = mContext.getResources().getConfiguration().orientation;
        boolean isLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);

        if (state == STATE_60_LAND) {
            if (isLandscape) {
                applyRefreshRate(REFRESH_STATE_60_LAND, REFRESH_STATE_60_LAND);
            } else {
                applyRefreshRate(defaultMaxRate, defaultMinRate);
            }
        } else if (state == STATE_120_LAND) {
            if (isLandscape) {
                applyRefreshRate(REFRESH_STATE_120_LAND, REFRESH_STATE_120_LAND);
            } else {
                applyRefreshRate(defaultMaxRate, defaultMinRate);
            }
        }
    }

    protected void checkOrientationAndSetRate(String packageName) {
        int currentOrientation = mContext.getResources().getConfiguration().orientation;
        boolean isCurrentlyLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
        float currentMaxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);

        if (isCurrentlyLandscape && isAppInList) {
            setLandscapeModeRefreshRate(packageName);
        } else if (!isCurrentlyLandscape && isAppInList) {
            setPortraitModeRefreshRate(packageName);
        }
    }

    private void disableDisplayListener() {
        if (mDisplayManager != null && mDisplayListener != null) {
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
            mDisplayListener = null;
        }
        
        if (mPendingRotationTask != null) {
            mHandler.removeCallbacks(mPendingRotationTask);
            mPendingRotationTask = null;
        }
    }

    protected void setRefreshRate(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        float maxRate = defaultMaxRate;
        float minRate = defaultMinRate;
        isAppInList = false;

        if (value != null) {
            modes = value.split(":");

            if (modes[0].contains(packageName + ",")) { // 60Hz
                disableDisplayListener();
                maxRate = REFRESH_STATE_60;
                minRate = REFRESH_STATE_60;
                isAppInList = true;
            } else if (modes[1].contains(packageName + ",")) { // 120Hz
                disableDisplayListener();
                maxRate = REFRESH_STATE_120;
                minRate = REFRESH_STATE_120;
                isAppInList = true;
            } else if (modes[2].contains(packageName + ",")) { // 60Hz in landscape
                initializeDisplayListener(packageName);
                isAppInList = true;
                return;
            } else if (modes[3].contains(packageName + ",")) { // 120Hz in landscape
                initializeDisplayListener(packageName);
                isAppInList = true;
                return;
            } else { // default
                disableDisplayListener();
                maxRate = defaultMaxRate;
                minRate = defaultMinRate;
            }
        }

        applyRefreshRate(maxRate, minRate);
    }

    private void setLandscapeModeRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);
        if (state == STATE_60_LAND) {
            applyRefreshRate(REFRESH_STATE_60_LAND, REFRESH_STATE_60_LAND);
        } else if (state == STATE_120_LAND) {
            applyRefreshRate(REFRESH_STATE_120_LAND, REFRESH_STATE_120_LAND);
        }
        // For all other states, do nothing (let setRefreshRate handle it)
    }

    private void setPortraitModeRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);
        if (state == STATE_60_LAND || state == STATE_120_LAND) {
            // Portrait: use default (system default)
            applyRefreshRate(defaultMaxRate, defaultMinRate);
        }
        // For all other states, do nothing (let setRefreshRate handle it)
    }

    private String getValue() {
        String value = mSharedPrefs.getString(REFRESH_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = REFRESH_60 + ":" + REFRESH_120 + ":" + REFRESH_60_LAND + ":" + REFRESH_120_LAND;
            writeValue(value);
        }

        String[] modes = value.split(":");
        if (modes.length < 4) {
            // Pad missing modes
            String[] newModes = new String[4];
            for (int i = 0; i < 4; i++) {
                if (i < modes.length) newModes[i] = modes[i];
                else if (i == 0) newModes[i] = REFRESH_60;
                else if (i == 1) newModes[i] = REFRESH_120;
                else if (i == 2) newModes[i] = REFRESH_60_LAND;
                else newModes[i] = REFRESH_120_LAND;
            }
            value = String.join(":", newModes);
            writeValue(value);
            modes = newModes;
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");
        String finalString;

        switch (mode) {
            case STATE_60:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_120:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_60_LAND:
                modes[2] = modes[2] + packageName + ",";
                break;
            case STATE_120_LAND:
                modes[3] = modes[3] + packageName + ",";
                break;
        }

        finalString = String.join(":", modes);
        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;
        if (modes[0].contains(packageName + ",")) {
            state = STATE_60;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_120;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_60_LAND;
        } else if (modes[3].contains(packageName + ",")) {
            state = STATE_120_LAND;
        }
        return state;
    }
}