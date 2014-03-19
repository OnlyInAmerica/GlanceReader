package pro.dbro.openspritz;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import pro.dbro.openspritz.formats.SpritzerBook;
import pro.dbro.openspritz.lib.events.ChapterSelectRequested;
import pro.dbro.openspritz.lib.events.NextChapterEvent;
import pro.dbro.openspritz.lib.events.SpritzFinishedEvent;

public class SpritzFragment extends Fragment {
    private static final String TAG = "SpritzFragment";

    private static AppSpritzer mSpritzer;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mChapterView;
    private ProgressBar mProgress;
    private TextView mSpritzView;
    private Bus mBus;

    public static SpritzFragment newInstance() {
        SpritzFragment fragment = new SpritzFragment();
        return fragment;
    }

    public SpritzFragment() {
        // Required empty public constructor
    }

    public void feedEpubToSpritzer(Uri epubPath) {
        if (mSpritzer == null) {
            mSpritzer = new AppSpritzer(mSpritzView, epubPath);
            mSpritzer.setEventBus(mBus);
        } else {
            mSpritzer.setEpubPath(epubPath);
        }
    }

    /**
     * Update the UI related to Book Title, Author,
     * and current progress
     */
    public void updateMetaUi() {
        if (!mSpritzer.isBookSelected()) {
            return;
        }

        SpritzerBook book = mSpritzer.getBook();

        mAuthorView.setText(book.getAuthor());
        mTitleView.setText(book.getTitle());

        int curChapter = mSpritzer.getCurrentChapter();

        String chapterText = mSpritzer.getBook().getChapterTitle(curChapter);
        if (chapterText == null) {
            chapterText = String.format("Chapter %d", curChapter);
        }

        int startSpan = chapterText.length();
        chapterText = String.format("%s  %s m left", chapterText,
                (mSpritzer.getMinutesRemainingInQueue() == 0) ? "<1" : String.valueOf(mSpritzer.getMinutesRemainingInQueue()));
        int endSpan = chapterText.length();
        Spannable spanRange = new SpannableString(chapterText);
        TextAppearanceSpan tas = new TextAppearanceSpan(mChapterView.getContext(), R.style.MinutesToGo);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mChapterView.setText(spanRange);

        final int progressScale = 10;
        int progress = curChapter * progressScale + ( (int) (progressScale * (mSpritzer.getQueueCompleteness())) );
        mProgress.setMax((mSpritzer.getMaxChapter() + 1) * progressScale);
        mProgress.setProgress(progress);
    }

    /**
     * Hide or Show the UI related to Book Title, Author,
     * and current progress
     *
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

    public void dimActionBar(boolean dim) {
        if (dim) {
            getActivity().getActionBar().hide();
        } else {
            getActivity().getActionBar().show();
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if(mSpritzer.isPlaying()) {
                                mChapterView.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
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
                mBus.post(new ChapterSelectRequested());
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
                        dimActionBar(false);
                        mSpritzer.pause();
                    } else {
                        showMetaUi(false);
                        dimActionBar(true);
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
    public void onStart() {
        super.onStart();
        OpenSpritzApplication app = (OpenSpritzApplication) getActivity().getApplication();
        mBus = app.getBus();
        mBus.register(this);
        if (mSpritzer == null) {
            mSpritzer = new AppSpritzer(mSpritzView);
            mSpritzer.setEventBus(mBus);
            if (mSpritzer.getBook() == null) {
                mSpritzView.setText(getString(R.string.select_epub));
            } else {
                // AppSpritzer loaded the last book being read
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

    public AppSpritzer getSpritzer() {
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
}
