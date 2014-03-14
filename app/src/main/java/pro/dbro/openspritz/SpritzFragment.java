package pro.dbro.openspritz;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import pro.dbro.openspritz.events.NextChapterEvent;
import pro.dbro.openspritz.events.SpritzFinishedEvent;

import java.util.List;

public class SpritzFragment extends Fragment {
    private static final String TAG = "SpritzFragment";

    private static EpubSpritzer mSpritzer;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mChapterView;
    private ProgressBar mProgress;
    private TextView mSpritzView;
    private Bus mBus;

    private SpritzFragmentListener mSpritzFragmentListener;

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

    public void feedEpubToSpritzer(Uri epubPath) {
        if (mSpritzer == null) {
            mSpritzer = new EpubSpritzer(mSpritzView, epubPath);
            mSpritzer.setEventBus(mBus);
        } else {
            mSpritzer.setEpubPath(epubPath);
        }
        updateMetaUi();
    }

    /**
     * Update the UI related to Book Title, Author,
     * and current progress
     */
    public void updateMetaUi() {
        if(!mSpritzer.isBookSelected()){
            return;
        }
        
        Book book = mSpritzer.getBook();
        Metadata meta = book.getMetadata();

        //Set author if available
        List<Author> authors = meta.getAuthors();
        if(!authors.isEmpty()){
            Author author = authors.get(0);
            mAuthorView.setText(author.getFirstname() + " " + author.getLastname());
        }

        int curChapter = mSpritzer.getCurrentChapter();
        mTitleView.setText(meta.getFirstTitle());
        String chapterText;
        if (book.getSpine().getResource(curChapter).getTitle() == null || book.getSpine().getResource(curChapter).getTitle().trim().compareTo("") == 0) {
            chapterText = String.format("Chapter %d", curChapter);
        } else {
            chapterText = book.getSpine().getResource(curChapter).getTitle();
        }

        int startSpan = chapterText.length();
        chapterText = String.format("%s  %s m left", chapterText,
                (mSpritzer.getMinutesRemainingInQueue() == 0) ? "<1" : String.valueOf(mSpritzer.getMinutesRemainingInQueue()));
        int endSpan = chapterText.length();
        Spannable spanRange = new SpannableString(chapterText);
        TextAppearanceSpan tas = new TextAppearanceSpan(mChapterView.getContext(), R.style.MinutesToGo);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mChapterView.setText(spanRange);

        mProgress.setMax(mSpritzer.getMaxChapter());
        mProgress.setProgress(curChapter);
    }

    /**
     * Hide or Show the UI related to Book Title, Author,
     * and current progress
     * @param show
     */
    public void showMetaUi(boolean show) {
        if (show) {
            mAuthorView.setVisibility(View.VISIBLE);
            mTitleView.setVisibility(View.VISIBLE);
            mChapterView.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mAuthorView.setVisibility(View.INVISIBLE);
            mTitleView.setVisibility(View.INVISIBLE);
            mChapterView.setVisibility(View.INVISIBLE);
            mProgress.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Temporarily fade in the Chapter label.
     * Used when user crosses a chapter boundary.
     */
    private void peekChapter() {
        mChapterView.setVisibility(View.VISIBLE);
        // Clean this up
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mChapterView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);
        mAuthorView = ((TextView) root.findViewById(R.id.author));
        mTitleView = ((TextView) root.findViewById(R.id.title));
        mChapterView = ((TextView) root.findViewById(R.id.chapter));
        mChapterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSpritzFragmentListener.onChapterSelectRequested();
            }
        });
        mProgress = ((ProgressBar) root.findViewById(R.id.progress));
        mSpritzView = (TextView) root.findViewById(R.id.spritzText);
        mSpritzView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpritzer != null && mSpritzer.isBookSelected()) {
                    if (mSpritzer.isPlaying()) {
                        updateMetaUi();
                        showMetaUi(true);
                        mSpritzer.pause();
                    } else {
                        showMetaUi(false);
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
        try {
            mSpritzFragmentListener = (SpritzFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SpritzFragmentListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mBus = new Bus(ThreadEnforcer.ANY);
        mBus.register(this);
        if (mSpritzer == null) {
            mSpritzer = new EpubSpritzer(mSpritzView);
            if(mSpritzer.getBook() == null) {
                mSpritzView.setText(getString(R.string.select_epub));
            } else {
                // EpubSpritzer loaded the last book being reads
                updateMetaUi();
                showMetaUi(true);
            }
        } else {
            mSpritzer.setEventBus(mBus);
            mSpritzer.swapTextView(mSpritzView);
            if (!mSpritzer.isPlaying()) {
                updateMetaUi();
                showMetaUi(true);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSpritzer != null) {
            Log.i(TAG, "saving state");
            mSpritzer.saveState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBus != null) {
            mBus.unregister(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {
        updateMetaUi();
        showMetaUi(true);
    }

    @Subscribe
    public void onNextChapter(NextChapterEvent event) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    updateMetaUi();
                    peekChapter();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public EpubSpritzer getSpritzer() {
        return mSpritzer;
    }

    private static final int SELECT_EPUB = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void chooseEpub() {

        // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
        Intent intent;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Currently no recognized epub MIME type
        intent.setType("*/*");

        startActivityForResult(intent, SELECT_EPUB);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_EPUB && data != null) {
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT >= 19) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            feedEpubToSpritzer(uri);
            updateMetaUi();
        }
    }

    public interface SpritzFragmentListener {
        public abstract void onChapterSelectRequested();
    }

}
