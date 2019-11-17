/*
 *  Copyright (C) 2018 Havoc-OS
 *  Copyright (C) 2018 ExtendedUI
 *  Copyright (C) 2020 PixelPlusUI
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ppui.config.center;

import com.android.internal.logging.nano.MetricsProto;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Surface;
import androidx.preference.*;

import com.ppui.config.center.preferences.Utils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import android.widget.Toast;

import com.ppui.config.center.preferences.CustomSeekBarPreference;
import com.ppui.config.center.preferences.SystemSettingMasterSwitchPreference;

public class ConfigCenter extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String INCALL_VIB_OPTIONS = "incall_vib_options";
    private static final String TORCH_POWER_BUTTON_GESTURE = "torch_power_button_gesture";
    private static final String QS_BACKGROUND_BLUR_ALPHA = "qs_blur_alpha";
    private static final String QS_BACKGROUND_BLUR_INTENSITY = "qs_blur_intensity";
    private static final String GAMING_MODE_ENABLED = "gaming_mode_enabled";

    private ContentResolver mResolver;
    private ListPreference mTorchPowerButton;

    private CustomSeekBarPreference mQSBlurAlpha;
    private CustomSeekBarPreference mQSBlurIntensity;
    private SystemSettingMasterSwitchPreference mGamingMode;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.config_center);

        mResolver = getActivity().getContentResolver();

        PreferenceScreen prefScreen = getPreferenceScreen();

        PreferenceCategory incallVibCategory = (PreferenceCategory) findPreference(INCALL_VIB_OPTIONS);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(incallVibCategory);
        }
        // screen off torch
        mTorchPowerButton = (ListPreference) findPreference(TORCH_POWER_BUTTON_GESTURE);
        int mTorchPowerButtonValue = Settings.Secure.getInt(mResolver,
                Settings.Secure.TORCH_POWER_BUTTON_GESTURE, 0);
        mTorchPowerButton.setValue(Integer.toString(mTorchPowerButtonValue));
        mTorchPowerButton.setSummary(mTorchPowerButton.getEntry());
        mTorchPowerButton.setOnPreferenceChangeListener(this);

        mQSBlurAlpha = (CustomSeekBarPreference) findPreference(QS_BACKGROUND_BLUR_ALPHA);
        int qsBlurAlpha = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_BACKGROUND_BLUR_ALPHA, 100);
        mQSBlurAlpha.setValue(qsBlurAlpha);
        mQSBlurAlpha.setOnPreferenceChangeListener(this);

        mQSBlurIntensity = (CustomSeekBarPreference) findPreference(QS_BACKGROUND_BLUR_INTENSITY);
        int qsBlurIntensity = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_BACKGROUND_BLUR_INTENSITY, 30);
        mQSBlurIntensity.setValue(qsBlurIntensity);
        mQSBlurIntensity.setOnPreferenceChangeListener(this);

        mGamingMode = (SystemSettingMasterSwitchPreference) findPreference(GAMING_MODE_ENABLED);
        mGamingMode.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.GAMING_MODE_ENABLED, 0) == 1));
        mGamingMode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean DoubleTapPowerGesture = Settings.Secure.getInt(mResolver,
                    Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 1) == 0;
        if (preference == mTorchPowerButton) {
            int mTorchPowerButtonValue = Integer.valueOf((String) newValue);
            int index = mTorchPowerButton.findIndexOfValue((String) newValue);
            mTorchPowerButton.setSummary(
                    mTorchPowerButton.getEntries()[index]);
            Settings.Secure.putInt(mResolver, Settings.Secure.TORCH_POWER_BUTTON_GESTURE,
                    mTorchPowerButtonValue);
            if (mTorchPowerButtonValue == 1 && DoubleTapPowerGesture) {
                //if doubletap for torch is enabled, switch off double tap for camera
                Settings.Secure.putInt(mResolver, Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                        1);
                Toast.makeText(getActivity(),
                    (R.string.torch_power_button_gesture_dt_toast),
                    Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (preference == mQSBlurAlpha) {
            int value = (Integer) newValue;
            Settings.System.putInt(mResolver, Settings.System.QS_BACKGROUND_BLUR_ALPHA, value);
            return true;
        } else if (preference == mQSBlurIntensity) {
            int value = (Integer) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_BACKGROUND_BLUR_INTENSITY, value);
            return true;
	} else if (preference == mGamingMode) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GAMING_MODE_ENABLED, value ? 1 : 0);
            return true;
	}
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    public static void lockCurrentOrientation(Activity activity) {
        int currentRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        int frozenRotation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        switch (currentRotation) {
            case Surface.ROTATION_0:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case Surface.ROTATION_270:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        activity.setRequestedOrientation(frozenRotation);
    }
}
