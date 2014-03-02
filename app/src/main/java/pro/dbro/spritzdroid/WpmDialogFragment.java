package pro.dbro.spritzdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by davidbrodsky on 3/1/14.
 */
public class WpmDialogFragment extends DialogFragment {

    public static final int MAX_WPM = 1800;
    public static final int MIN_WPM = 60;

    private SeekBar mWpmSeek;
    private TextView mWpmLabel;
    private OnWpmSelectListener mOnWpmSelectListener;
    private int mWpm;

    static WpmDialogFragment newInstance(int wpm) {
        WpmDialogFragment f = new WpmDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("wpm", wpm);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnWpmSelectListener = (OnWpmSelectListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnWpmSelectListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWpm = Math.max(MIN_WPM, getArguments().getInt("wpm"));
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
                mWpmLabel.setText(mWpm + " WPM");
                mOnWpmSelectListener.onWpmSelected(mWpm);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Set WPM")
                .setView(v)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        Dialog dialog = builder.create();
        //dialog.getWindow().setBackgroundDrawableResource(R.drawable.fragment_dialog_wpm_bg);
        return dialog;
    }

    public interface OnWpmSelectListener {
        public abstract void onWpmSelected(int wpm);
    }
}
