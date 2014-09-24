package pro.dbro.glance.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.otto.Bus;

import pro.dbro.glance.GlanceApplication;
import pro.dbro.glance.PrefsManager;
import pro.dbro.glance.R;
import pro.dbro.glance.events.WpmSelectedEvent;


/**
 * Created by davidbrodsky on 3/1/14.
 */
public class WpmDialogFragment extends DialogFragment {

    public static final int MAX_WPM = 1200;
    public static final int WHOAH_THRESHOLD_WPM = 800;
    public static final int MIN_WPM = 300;

    private View mView;
    private Animation mCurrentAnimation;
    private boolean mAnimationRunning;
    private SeekBar mWpmSeek;
    private TextView mWpmLabel;
    private int mWpm;
    private Bus mBus;

    public static WpmDialogFragment newInstance() {
        WpmDialogFragment f = new WpmDialogFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWpm = PrefsManager.getWpm(getActivity());
        mAnimationRunning = false;

        GlanceApplication app = (GlanceApplication) getActivity().getApplication();
        this.mBus = app.getBus();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_wpm, null);
        mWpmLabel = (TextView) v.findViewById(R.id.wpmText);
        mWpmSeek = ((SeekBar) v.findViewById(R.id.seekBar));

        mWpmSeek.setProgress((int) ((float) 100 * mWpm / MAX_WPM));
        mWpmLabel.setText(mWpm + " WPM");

        mWpmSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mWpm = Math.max(MIN_WPM, (int) ((progress / 100.0) * MAX_WPM));
                String wpmStr = mWpm + " WPM";
                mWpmLabel.setText(wpmStr);
                getDialog().setTitle(wpmStr);
                if (mWpm >= WHOAH_THRESHOLD_WPM + 50 && !mAnimationRunning) {
                    setTrippin(true);
                } else if (mWpm <= WHOAH_THRESHOLD_WPM - 50 && mAnimationRunning) {
                    setTrippin(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.set_wpm))
                .setView(v);
        mView = v;
        return builder.create();
    }

    private void setTrippin(boolean beTrippin){
        if(beTrippin){
            mAnimationRunning = true;
            if (mCurrentAnimation == null) {
                mCurrentAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.wobble);
                mView.startAnimation(mCurrentAnimation);
            } else {
                mView.startAnimation(mCurrentAnimation);
            }
        } else {
            mView.clearAnimation();
            mAnimationRunning = false;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mBus.post(new WpmSelectedEvent(mWpm));
        PrefsManager.setWpm(getActivity(), mWpm);
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }
}
