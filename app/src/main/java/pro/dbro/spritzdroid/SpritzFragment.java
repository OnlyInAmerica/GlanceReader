package pro.dbro.spritzdroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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

    private void feedEpubToSpritzer(Uri epubPath) {
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
                } else {
                    chooseEpub();
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
        if (mSpritzer == null) {
            mTextView.setText("Touch to Select .epub");
        }

        mEventBus = new EventBus();
        mEventBus.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mEventBus != null)
            mEventBus.unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {

    }

    private static final int SELECT_EPUB = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void chooseEpub() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, SELECT_EPUB);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_EPUB) {
            Uri uri = data.getData();
            feedEpubToSpritzer(uri);
            mTextView.setText("Touch to Start");

        }
    }

}
