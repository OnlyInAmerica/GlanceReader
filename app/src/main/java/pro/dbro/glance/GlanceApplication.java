package pro.dbro.glance;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
        Logger.getRootLogger().setLevel(Level.OFF);
        this.mBus = new Bus(ThreadEnforcer.ANY);
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
