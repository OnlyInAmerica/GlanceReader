package pro.dbro.glance.tts;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import pro.dbro.glance.AppSpritzer;
import pro.dbro.glance.GlancePrefsManager;
import pro.dbro.glance.R;
import pro.dbro.glance.lib.SpritzerTextView;

/**
 * Created by george on 11/24/15.
 */
public class Util {
    private SpritzerTextView spritzerTextView;
    private HandlerThread ht;
    private Handler hd;

    boolean inScreen = false;

    public Util() {
        ht = new HandlerThread("UTILHELPER");
        ht.start();
        hd = new Handler(ht.getLooper());

    }

    public synchronized  void doAction(Context context) {

        try {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            inScreen = false;
            windowManager.removeView(spritzerTextView);
        } catch (Exception e) {
        }
    }

    public  void removeView(final Context context) {

        hd.postDelayed(    new Runnable() {
                        @Override
                        public void run() {
                            doAction(context);

                        }
                    }, 500);

    }
    public  View getView() {
        return spritzerTextView;
    }
    public synchronized  View generateView(Context context) {
        if ( spritzerTextView!=null && inScreen ) {
            hd.removeCallbacksAndMessages(null);
            return  spritzerTextView;
        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if ( spritzerTextView == null ) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            spritzerTextView = (SpritzerTextView) inflater.inflate(R.layout.tts_component, null);

            AppSpritzer mSpritzer = new AppSpritzer(spritzerTextView);
            spritzerTextView.setSpritzer(mSpritzer);

            int mWpm = GlancePrefsManager.getWpm(context);
            mSpritzer.setWpm(mWpm);
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;



        windowManager.addView(spritzerTextView, params);
        inScreen = true;
        return spritzerTextView;
    }

}
