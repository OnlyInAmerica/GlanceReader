package pro.dbro.glance.activities;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.astuetz.PagerSlidingTabStrip;

import pro.dbro.glance.GlancePrefsManager;
import pro.dbro.glance.R;
import pro.dbro.glance.adapters.ReaderSectionAdapter;

//import pro.dbro.glance.SECRETS;

public class BooksActivity extends FragmentActivity {

    /** Intent Code */
    private static final int SELECT_MEDIA = 42;


    /** Theme Codes */
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = GlancePrefsManager.getTheme(this);
        switch (theme) {
            case THEME_LIGHT:
                setTheme(R.style.Light);
                break;
            case THEME_DARK:
                setTheme(R.style.Dark);
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_books);
        setupActionBar();

        // Initialize the ViewPager and set an adapter
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new ReaderSectionAdapter(getSupportFragmentManager()));

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(pager);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.action_open:
                chooseMedia();
                break;
            case R.id.action_settings:
                showPreferencesActivity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPreferencesActivity() {
        Intent prefIntent = new Intent(this, PreferencesActivity.class);
        startActivity(prefIntent);
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void chooseMedia() {

        // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
        Intent intent;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Currently no recognized epub MIME type
        intent.setType("*/*");

        startActivityForResult(intent, SELECT_MEDIA);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.community, menu);
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_MEDIA && data != null) {
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT >= 19) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            Intent spritzIntent = new Intent(this, MainActivity.class);
            spritzIntent.setAction(Intent.ACTION_VIEW);
            spritzIntent.setData(uri);
            startActivity(spritzIntent);
        }
    }
}

