package pro.dbro.glance.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.regex.Matcher;
import android.util.Patterns;

import pro.dbro.glance.GlanceApplication;
import pro.dbro.glance.GlancePrefsManager;
import pro.dbro.glance.R;
import pro.dbro.glance.adapters.AdapterUtils;
import pro.dbro.glance.events.ChapterSelectRequested;
import pro.dbro.glance.events.ChapterSelectedEvent;
import pro.dbro.glance.events.WpmSelectedEvent;
import pro.dbro.glance.formats.HtmlPage;
import pro.dbro.glance.formats.SpritzerMedia;
import pro.dbro.glance.fragments.SpritzFragment;
import pro.dbro.glance.fragments.TocDialogFragment;
import pro.dbro.glance.fragments.WpmDialogFragment;
import pro.dbro.glance.lib.events.SpritzFinishedEvent;

//import pro.dbro.glance.SECRETS;

public class MainActivity extends ImmersiveActivityBase implements View.OnSystemUiVisibilityChangeListener {
    private static final String TAG = "MainActivity";
    public static final boolean VERBOSE = false;
    public static final String SPRITZ_FRAG_TAG = "spritzfrag";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private Menu mMenu;
    private boolean mFinishAfterSpritz = false;


    private Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = GlancePrefsManager.getTheme(this);
        switch (theme) {
            case THEME_LIGHT:
                setTheme(R.style.Light_Spritzer);
                break;
            case THEME_DARK:
                setTheme(R.style.Dark_Spritzer);
                break;
        }
        super.onCreate(savedInstanceState);
        setupActionBar();
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            // Retain the SpritzFragment instance so it survives screen rotation
            SpritzFragment frag = new SpritzFragment();
            frag.setRetainInstance(true);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, frag, SPRITZ_FRAG_TAG)
                    .commit();
        } else if (getSpritzFragment().getSpritzer().isPlaying()) {
            // If this Activity has been recreated while the SpritzFragment was playing
            // hide our action bar
            if (getSupportActionBar() != null) getSupportActionBar().hide();
        }

        GlanceApplication app = (GlanceApplication) getApplication();
        mBus = app.getBus();
        mBus.register(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        //setupBillingConnection(SECRETS.getBillingPubKey());
    }

    @Override
    public void onResume() {
        super.onResume();
//        dimSystemUi(true);

        boolean intentIncludesMediaUri = false;
        String action = getIntent().getAction();
        Uri intentUri = null;
        if (!isIntentMarkedAsHandled(getIntent())) {
            if (action.equals(Intent.ACTION_VIEW)) {
                intentIncludesMediaUri = true;
                intentUri = getIntent().getData();
            } else if (action.equals(Intent.ACTION_SEND)) {
                intentIncludesMediaUri = true;
                intentUri = Uri.parse(findURL(getIntent().getStringExtra(Intent.EXTRA_TEXT)));
            }

            if (intentIncludesMediaUri && intentUri != null) {
                if (getIntent().hasExtra(AdapterUtils.FINISH_AFTER)) mFinishAfterSpritz = true;
                SpritzFragment frag = getSpritzFragment();
                frag.feedMediaUriToSpritzer(intentUri);
            }
            markIntentAsHandled(getIntent());
        }
    }

    private void markIntentAsHandled(Intent intent) {
        intent.putExtra("handled", true);
    }

    private boolean isIntentMarkedAsHandled(Intent intent) {
        return intent.getBooleanExtra("handled", false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBus != null) {
            mBus.unregister(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
//        if (mIsPremium) {
//            menu.removeItem(R.id.action_donate);
//        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        if (mIsPremium) {
//            menu.removeItem(R.id.action_donate);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_speed) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            DialogFragment newFragment = WpmDialogFragment.newInstance();
            newFragment.show(ft, "dialog");
            return true;
        } else if (id == R.id.action_theme) {
            int theme = GlancePrefsManager.getTheme(this);
            if (theme == THEME_LIGHT) {
                applyDarkTheme();
            } else {
                applyLightTheme();
            }
        }
        /*
        else if (id == R.id.action_open) {
            getSpritzFragment().chooseMedia();
        }
        */
//        else if(id == R.id.action_donate) {
//            showDonateDialog();
//        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onWpmSelected(WpmSelectedEvent event) {
        if (getSpritzFragment() != null) {
            getSpritzFragment().getSpritzer()
                    .setWpm(event.getWpm());
        }
    }

    private void applyDarkTheme() {
        GlancePrefsManager.setTheme(this, THEME_DARK);
        recreate();

    }

    private void applyLightTheme() {
        GlancePrefsManager.setTheme(this, THEME_LIGHT);
        recreate();
    }

    private String findURL (String Text)
    {
        int longest_URL_length=-1;
        String longest_URL = Text;
        Matcher matcher = Patterns.WEB_URL.matcher(Text);
        while (matcher.find()) {
            if (matcher.group().length() > longest_URL_length) {
                longest_URL_length=matcher.group().length();
                longest_URL= matcher.group();
            }
        }
        return longest_URL;
    }

    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                takeSharingActionifAppropriateAndFinish(getSpritzFragment().getSpritzer().getMedia());
            }
        });
    }

    @Subscribe
    public void onChapterSelected(ChapterSelectedEvent event) {
        SpritzFragment frag = getSpritzFragment();
        if (frag != null && frag.getSpritzer() != null) {
            frag.getSpritzer().printChapter(event.getChapter());
            frag.updateMetaUi();
        } else {
            Log.e(TAG, "SpritzFragment not available to apply chapter selection");
        }
    }

    @Subscribe
    public void onChapterSelectRequested(ChapterSelectRequested ignored) {
        SpritzFragment frag = getSpritzFragment();
        if (frag != null && frag.isResumed() && frag.getSpritzer() != null) {
            SpritzerMedia book = frag.getSpritzer().getMedia();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            DialogFragment newFragment = TocDialogFragment.newInstance(book);
            newFragment.show(ft, "dialog");
        } else {
            Log.e(TAG, "SpritzFragment not available for chapter selection");
        }
    }

    private SpritzFragment getSpritzFragment() {
        return ((SpritzFragment) getSupportFragmentManager().findFragmentByTag(SPRITZ_FRAG_TAG));
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    private void dimSystemUi(boolean doDim) {
        final boolean isIceCreamSandwich = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
        if (isIceCreamSandwich) {
            final View decorView = getWindow().getDecorView();
            if (doDim) {
                int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
                decorView.setSystemUiVisibility(uiOptions);
            } else {
                decorView.setSystemUiVisibility(0);
                decorView.setOnSystemUiVisibilityChangeListener(null);
            }
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Stay in low-profile mode
        if ((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
            dimSystemUi(true);
        }
    }

    /**
     * Take the appropriate sharing action based on the recorded Preference
     */
    public void takeSharingActionifAppropriateAndFinish(SpritzerMedia media) {
        if (!(media instanceof HtmlPage)) return;
        boolean isInternalMedia = getIntent().getBooleanExtra(AdapterUtils.IS_INTERNAL_MEDIA, false);
        if (isInternalMedia) {
            recordHtmlPageRead((HtmlPage) media);
            if (mFinishAfterSpritz) finish();
            return;
        }
        GlancePrefsManager.SharePref sharePref = GlancePrefsManager.getShareMode(this);
        switch (sharePref) {
            case ALWAYS:
                recordHtmlPageRead((HtmlPage) media);
            case NEVER:
                if (mFinishAfterSpritz) finish();
                break;
            case ASK:
                showShareHtmlPageDialog((HtmlPage) media);
                break;
        }
    }

    private void showShareHtmlPageDialog(final HtmlPage page) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.share_dialog_title))
                .setMessage(getString(R.string.share_dialog_message))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recordHtmlPageRead(page);
                    }
                })
                .setNegativeButton(getString(R.string.no), null);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mFinishAfterSpritz) MainActivity.this.finish();
            }
        });

        dialog.show();
    }

    private void recordHtmlPageRead(final HtmlPage page) {

        // Okay, so this is really shitty.
        // I know.
        // Here's the thing: I didn't know Parse can't do DISTINCT or GROUP BY.
        // Now I do.
        // Anyway, instead we're just incrementing a counter.

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Article");
        query.whereEqualTo("url", page.getUrl());
        query.whereEqualTo("title", page.getTitle());
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    Log.d("score", "Retrieved " + scoreList.size() + " scores");

                    if (scoreList.isEmpty()) {
                        // Don't have the object, create it.
                        ParseObject article = new ParseObject("Article");
                        article.put("url", page.getUrl());
                        article.put("title", page.getTitle());
                        article.put("reads", 1);
                        article.saveInBackground();
                        return;
                    } else {
                        // Update object if we already have it.
                        ParseObject article = scoreList.get(0);
                        article.increment("reads");
                        article.saveInBackground();
                    }
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });

    }

}
