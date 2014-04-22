package pro.dbro.glance;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import pro.dbro.glance.GlanceApplication;
import pro.dbro.glance.R;
import pro.dbro.glance.SECRETS;
import pro.dbro.glance.Utils;
import pro.dbro.glance.billing.Catalog;
import pro.dbro.glance.billing.IabHelper;
import pro.dbro.glance.billing.IabResult;
import pro.dbro.glance.billing.Inventory;
import pro.dbro.glance.billing.Purchase;
import pro.dbro.glance.events.ChapterSelectRequested;
import pro.dbro.glance.events.ChapterSelectedEvent;
import pro.dbro.glance.events.WpmSelectedEvent;
import pro.dbro.glance.formats.SpritzerMedia;
import pro.dbro.glance.fragments.SpritzFragment;
import pro.dbro.glance.fragments.TocDialogFragment;
import pro.dbro.glance.fragments.WpmDialogFragment;
import pro.dbro.glance.lib.events.SpritzFinishedEvent;

public class MainActivity extends ActionBarActivity implements View.OnSystemUiVisibilityChangeListener {
    private static final String TAG = "MainActivity";
    public static final boolean VERBOSE = false;
    public static final String SPRITZ_FRAG_TAG = "spritzfrag";
    private static final String PREFS = "ui_prefs";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private IabHelper mBillingHelper;
    private boolean mIsPremium;
    private Menu mMenu;
    private boolean mFinishAfterSpritz = false;

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (VERBOSE) Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mBillingHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                Log.i(TAG, "Failed to query inventory: " + result);
                return;
            }

            if (VERBOSE) Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(Catalog.SKU_PREMIUM);
            boolean isPremiumUser = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            if (VERBOSE) Log.d(TAG, "User is " + (isPremiumUser ? "PREMIUM" : "NOT PREMIUM"));
            if (VERBOSE) Log.d(TAG, "Initial inventory query finished; enabling main UI.");
            mIsPremium = isPremiumUser;
            invalidateOptionsMenu();
        }
    };

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (VERBOSE) Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mBillingHelper == null) return;

            if (result.isFailure()) {
                Log.i(TAG, "Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                Log.i(TAG, "Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(Catalog.SKU_PREMIUM)) {
                // bought the premium upgrade!
                if (VERBOSE) Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                showDonateCompleteDialog();
                mIsPremium = true;
                invalidateOptionsMenu();
            }
        }
    };

    private int mWpm;
    private Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("THEME", 0);
        switch (theme) {
            case THEME_LIGHT:
                setTheme(R.style.Light);
                break;
            case THEME_DARK:
                setTheme(R.style.Dark);
                break;
        }
        super.onCreate(savedInstanceState);
        setupActionBar();
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new SpritzFragment(), SPRITZ_FRAG_TAG)
                .commit();

        GlanceApplication app = (GlanceApplication) getApplication();
        mBus = app.getBus();
        mBus.register(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        setupBillingConnection(SECRETS.getBillingPubKey());
    }

    @Override
    public void onResume() {
        super.onResume();
        dimSystemUi(true);

        boolean intentIncludesMediaUri = false;
        String action = getIntent().getAction();
        Uri intentUri = null;
        if(!isIntentMarkedAsHandled(getIntent())) {
            if (action.equals(Intent.ACTION_VIEW)) {
                intentIncludesMediaUri = true;
                intentUri = getIntent().getData();
            } else if (action.equals(Intent.ACTION_SEND)) {
                intentIncludesMediaUri = true;
                intentUri = Uri.parse(getIntent().getStringExtra(Intent.EXTRA_TEXT));
            }

            if (intentIncludesMediaUri && intentUri != null) {
                if (getIntent().hasExtra(Utils.INTENT_FINISH_AFTER)) mFinishAfterSpritz = true;
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
        if (mBillingHelper != null) {
            mBillingHelper.dispose();
            mBillingHelper = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (mIsPremium) {
            menu.removeItem(R.id.action_donate);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsPremium) {
            menu.removeItem(R.id.action_donate);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_speed) {
            if (mWpm == 0) {
                if (getSpritzFragment().getSpritzer() != null) {
                    mWpm = getSpritzFragment().getSpritzer().getWpm();
                } else {
                    mWpm = 500;
                }
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment = WpmDialogFragment.newInstance(mWpm);
            newFragment.show(ft, "dialog");
            return true;
        } else if (id == R.id.action_theme) {
            int theme = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getInt("THEME", THEME_LIGHT);
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
        else if(id == R.id.action_donate) {
            showDonateDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onWpmSelected(WpmSelectedEvent event) {
        if (getSpritzFragment() != null) {
            getSpritzFragment().getSpritzer()
                    .setWpm(event.getWpm());
        }
        mWpm = event.getWpm();
    }

    private void applyDarkTheme() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("THEME", THEME_DARK)
                .commit();
        recreate();

    }

    private void applyLightTheme() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("THEME", THEME_LIGHT)
                .commit();
        recreate();
    }

    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {
        if (mFinishAfterSpritz) this.finish();
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
            FragmentTransaction ft = getFragmentManager().beginTransaction();
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
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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

    private void setupBillingConnection(String base64EncodedPublicKey) {
        // compute your public key and store it in base64EncodedPublicKey
        mBillingHelper = new IabHelper(this, base64EncodedPublicKey);
        mBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d(TAG, "Problem setting up In-app Billing: " + result);
                }
                // Hooray, IAB is fully set up!
                if (mBillingHelper == null) return;
                // Query purchases
                mBillingHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    /**
     * Honor system payment validator.
     */
    private boolean verifyDeveloperPayload(Purchase p) {
        return true;
    }

    private void showDonateDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_donate_title))
                .setMessage(Html.fromHtml(getString(R.string.dialog_donate_msg)))
                .setPositiveButton(getString(R.string.dialog_donate_positive_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBillingHelper.launchPurchaseFlow(MainActivity.this, Catalog.SKU_PREMIUM, Catalog.PREMIUM_REQUEST,
                                mPurchaseFinishedListener, "");
                    }
                })
                .setNeutralButton(getString(R.string.dialog_donate_neutral_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://github.com/OnlyInAmerica/OpenSpritz-Android"));
                        startActivity(i);
                    }
                })
                .show();
    }

    private void showDonateCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_donate_complete_title))
                .setPositiveButton(getString(R.string.dialog_donate_complete_positive_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

}
