package pro.dbro.glance.activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.astuetz.PagerSlidingTabStrip;

import pro.dbro.glance.PrefsManager;
import pro.dbro.glance.R;
import pro.dbro.glance.adapters.ReaderSectionAdapter;

public class CommunityActivity extends ActionBarActivity {

    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = PrefsManager.getTheme(this);
        switch (theme) {
            case THEME_LIGHT:
                setTheme(R.style.Light);
                break;
            case THEME_DARK:
                setTheme(R.style.Dark);
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        if (getActionBar() != null) getActionBar().hide();

        // Initialize the ViewPager and set an adapter
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new ReaderSectionAdapter(getSupportFragmentManager()));

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(pager);

    }
}

