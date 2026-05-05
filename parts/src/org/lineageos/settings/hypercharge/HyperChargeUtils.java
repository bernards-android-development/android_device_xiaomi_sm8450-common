/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.hypercharge;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;
import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.hypercharge.HyperChargeService;

public class HyperChargeUtils {
    private static final String TAG = "HyperChargeUtils";

    public static void startService(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.hypercharge_settings, false);
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isHyperChargeEnabled = prefs.getBoolean(Constants.KEY_HYPERCHARGE_STATUS, true);
            
            // Note: We use a try-catch here as well just in case boot happens before UI sanitization
            String currentLimit;
            try {
                currentLimit = prefs.getString(Constants.KEY_HYPERCHARGE_LIMIT, Constants.CHARGE_LIMIT_67W);
            } catch (ClassCastException e) {
                currentLimit = Constants.CHARGE_LIMIT_67W;
            }

            if (!isHyperChargeEnabled || !Constants.CHARGE_LIMIT_67W.equals(currentLimit)) {
                context.startService(new Intent(context, HyperChargeService.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start HyperChargeService on boot", e);
        }
    }
}