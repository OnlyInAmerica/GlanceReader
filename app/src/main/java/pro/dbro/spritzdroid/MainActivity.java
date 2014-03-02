package pro.dbro.spritzdroid;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements WpmDialogFragment.OnWpmSelectListener{

    private int mWpm;

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
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment = WpmDialogFragment.newInstance(mWpm);
            newFragment.show(ft, "dialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWpmSelected(int wpm) {
        if(((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).getSpritzer() != null){
            ((SpritzFragment) getSupportFragmentManager().findFragmentByTag("spritsfrag")).getSpritzer()
                    .setWpm(wpm);
        }
        mWpm = wpm;
    }
}
