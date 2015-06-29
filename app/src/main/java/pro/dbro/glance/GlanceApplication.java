package pro.dbro.glance;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import timber.log.Timber;


/**
 * A custom application that sets up common functionality.
 *
 * @author defer (diogo@underdev.org)
 */
public class GlanceApplication extends Application {
    private Bus mBus;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBus = new Bus(ThreadEnforcer.ANY);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    /**
     * Obtains the Bus that is used throughout the App.
     *
     * @return The bus instance used throughout the app.
     */
    public Bus getBus() {
        return this.mBus;
    }
}
