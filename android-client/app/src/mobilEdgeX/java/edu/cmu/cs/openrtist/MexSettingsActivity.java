package edu.cmu.cs.openrtist;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import edu.cmu.cs.gabriel.MexConst;

public class MexSettingsActivity extends SettingsActivity {
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return super.isValidFragment(fragmentName)
                || MEXPreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class MEXPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_mex);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("mex_dme_url"));
            bindPreferenceSummaryToValue(findPreference("mex_app"));
            bindPreferenceSummaryToValue(findPreference("mex_dev"));
            bindPreferenceSummaryToValue(findPreference("mex_carrier"));
            bindPreferenceSummaryToValue(findPreference("mex_tag"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SettingsActivity.getSBindPreferenceSummaryToValueListener().onPreferenceChange(
                            preference, value
                    );

                    MexConst.loadPref(preference.getContext(), preference.getKey(), value);
                    return true;
                }
            };

    static void bindPreferenceSummaryToValue(Preference preference) {
        SettingsActivity.bindPreferenceSummaryToValue(
                preference, sBindPreferenceSummaryToValueListener);
    }
}
