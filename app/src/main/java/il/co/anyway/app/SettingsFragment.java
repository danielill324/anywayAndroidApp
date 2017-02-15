package il.co.anyway.app;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class SettingsFragment extends PreferenceFragment {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencescreen);
        // Load the preferences from an XML resource
        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'date range' preferences.
        addPreferencesFromResource(R.xml.pref_date_range);

        // Set the summary of the date-picker and add listener to bind the summary with the real value
        final DatePreference dp_from = (DatePreference) findPreference(getString(R.string.pref_from_date_key));
        String current_from = dp_from.getText();
        current_from = current_from.equals("") ? getString(R.string.pref_default_from_date) : current_from;
        dp_from.setText(current_from);
        dp_from.setSummary(current_from);
        bindPreferenceSummaryToValue(dp_from);

        final DatePreference dp_to = (DatePreference) findPreference(getString(R.string.pref_to_date_key));
        String current_to = dp_to.getText();
        current_to = current_to.equals("") ? getString(R.string.pref_default_to_date) : current_to;
        dp_to.setText(current_to);
        dp_to.setSummary(current_to);
        bindPreferenceSummaryToValue(dp_to);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set preferences padding to 10 (to avoid tablet two-pane layout hiding the title)
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if(v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(10, 10, 10, 10);
        }
        return v;
    }
}
