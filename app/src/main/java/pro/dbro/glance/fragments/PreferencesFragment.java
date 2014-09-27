package pro.dbro.glance.fragments;



import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pro.dbro.glance.R;

/**
 * A simple {@link Fragment} for managing user preferences.
 *
 */
public class PreferencesFragment extends PreferenceFragment {


    public PreferencesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }


}
