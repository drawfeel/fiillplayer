/*
 * This is the activity for settings.
 */

package com.fiill.fiillplayer.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import com.fiill.fiillplayer.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);
    }
}
