package pro.dbro.spritzdroid;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements WpmDialogFragment.OnWpmSelectListener {

    private int mWpm;
    private boolean mStyleDark = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new SpritzFragment(), "spritsfrag")
                .commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
                if (((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).getSpritzer() != null) {
                    mWpm = ((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).getSpritzer().mWPM;
                }
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment = WpmDialogFragment.newInstance(mWpm);
            newFragment.show(ft, "dialog");
            return true;
        } else if (id == R.id.action_theme) {
            if (!mStyleDark) {
                applyLightTheme();
            } else {
                applyDarkTheme();
            }
            mStyleDark = !mStyleDark;
        } else if (id == R.id.action_open) {
            ((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).chooseEpub();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWpmSelected(int wpm) {
        if (((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).getSpritzer() != null) {
            ((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).getSpritzer()
                    .setWpm(wpm);
        }
        mWpm = wpm;
    }

    private void applyDarkTheme() {
        ((TextView) findViewById(R.id.spritzText)).setTextColor(Color.WHITE);
        ((TextView) findViewById(R.id.spritzText)).setBackgroundColor(Color.BLACK);
    }

    private void applyLightTheme() {
        ((TextView) findViewById(R.id.spritzText)).setTextColor(Color.BLACK);
        ((TextView) findViewById(R.id.spritzText)).setBackgroundColor(Color.WHITE);
    }
}
