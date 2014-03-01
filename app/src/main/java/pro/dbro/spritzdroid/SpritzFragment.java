package pro.dbro.spritzdroid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import pro.dbro.spritzdroid.events.SpritzFinishedEvent;

public class SpritzFragment extends Fragment {

    private static EpubSpritzer mSpritzer;
    private TextView mTextView;
    private EventBus mEventBus;

    public static SpritzFragment newInstance() {
        SpritzFragment fragment = new SpritzFragment();
        return fragment;
    }

    public SpritzFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void feedEpubToSpritzer(String epubPath) {
        if (mSpritzer == null) {
            mSpritzer = new EpubSpritzer(mTextView, epubPath);
        } else {
            // If the activity was destroyed & recreated, we need to update the TextView reference
            mSpritzer.swapTextView(mTextView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);
        mTextView = (TextView) root.findViewById(R.id.spritzText);
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpritzer != null) {
                    if (mSpritzer.isPlaying()) {
                        mSpritzer.pause();
                    } else {
                        mSpritzer.start();
                    }
                }
            }
        });
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        /**** Put an epub in ../../../assets/book/ *****/
        feedEpubToSpritzer("book/pp.epub");
        mEventBus = new EventBus();
        mEventBus.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mEventBus != null)
            mEventBus.unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {

    }

}
