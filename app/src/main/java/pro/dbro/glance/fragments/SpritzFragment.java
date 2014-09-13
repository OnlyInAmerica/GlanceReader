package pro.dbro.glance.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import java.lang.ref.WeakReference;

import pro.dbro.glance.AppSpritzer;
import pro.dbro.glance.GlanceApplication;
import pro.dbro.glance.R;
import pro.dbro.glance.events.ChapterSelectRequested;
import pro.dbro.glance.events.HttpUrlParsedEvent;
import pro.dbro.glance.events.NextChapterEvent;
import pro.dbro.glance.formats.SpritzerMedia;
import pro.dbro.glance.lib.SpritzerTextView;
import pro.dbro.glance.lib.TextUtil;
import pro.dbro.glance.lib.events.SpritzFinishedEvent;

public class SpritzFragment extends Fragment {
    private static final String TAG = "SpritzFragment";

    // SpritzFragmentHandler Message codes
    protected static final int MSG_SPRITZ_TEXT = 1;
    protected static final int MSG_HIDE_CHAPTER_LABEL = 2;

    private static AppSpritzer mSpritzer;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mChapterView;
    private TextView mSpritzHistoryView;
    private ProgressBar mProgress;
    private SpritzerTextView mSpritzView;
    private Bus mBus;
    private SpritzFragmentHandler mHandler;
    private boolean mUserIsChoosingEpub;

    public static SpritzFragment newInstance() {
        SpritzFragment fragment = new SpritzFragment();
        return fragment;
    }

    public SpritzFragment() {
        // Required empty public constructor
    }

    public void feedMediaUriToSpritzer(Uri mediaUri) {
        if (mSpritzer == null) {
            mSpritzer = new AppSpritzer(mBus, mSpritzView, mediaUri);
            mSpritzView.setSpritzer(mSpritzer);
        } else {
            mSpritzer.setMediaUri(mediaUri);
        }

//        Why is this commented out?
        if (AppSpritzer.isHttpUri(mediaUri)) {
            mSpritzer.setTextAndStart(getString(R.string.loading));
            showIndeterminateProgress(true);
        }
    }

    public void showIndeterminateProgress(boolean show) {
        mProgress.setIndeterminate(show);
    }

    /**
     * Update the UI related to Book Title, Author,
     * and current progress. Everything but the {@link pro.dbro.glance.lib.SpritzerTextView}
     */
    public void updateMetaUi() {
        if (!mSpritzer.isMediaSelected()) {
            return;
        }

        SpritzerMedia book = mSpritzer.getMedia();

        mAuthorView.setText(book.getAuthor());
        mTitleView.setText(book.getTitle());

        int curChapter = mSpritzer.getCurrentChapter();

        String chapterText = mSpritzer.getMedia().getChapterTitle(curChapter);

        int startSpan = chapterText.length();
        chapterText = String.format("%s  %s m left", chapterText,
                (mSpritzer.getMinutesRemainingInQueue() == 0) ? "<1" : String.valueOf(mSpritzer.getMinutesRemainingInQueue()));
        int endSpan = chapterText.length();
        Spannable spanRange = new SpannableString(chapterText);
        TextAppearanceSpan tas = new TextAppearanceSpan(mChapterView.getContext(), R.style.MinutesToGo);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mChapterView.setText(spanRange);

        final int progressScale = 10;
        int progress;
        // If the spritzer is showing a special message
        // don't factor current word queue completeness
        // into progress.
        if (mSpritzer.isSpritzingSpecialMessage()) {
            progress = curChapter;
        } else {
            progress = curChapter * progressScale + ((int) (progressScale * (mSpritzer.getQueueCompleteness())));
        }
        mProgress.setMax((mSpritzer.getMaxChapter() + 1) * progressScale);

        mProgress.setProgress(progress);

        if (!mSpritzer.isPlaying()) {
            // If we're paused, show the Spritz history
            int mSpritzHistoryViewLength = TextUtil.calculateMonospacedCharacterLimit(mSpritzHistoryView, getResources().getInteger(R.integer.spritz_history_line_count));
            mSpritzHistoryView.setText(mSpritzer.getHistoryString(mSpritzHistoryViewLength));
        }
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
            mSpritzHistoryView.setVisibility(View.VISIBLE);
        } else {
            mAuthorView.setVisibility(View.INVISIBLE);
            mTitleView.setVisibility(View.INVISIBLE);
            mChapterView.setVisibility(View.INVISIBLE);
            mProgress.setVisibility(View.INVISIBLE);
            mSpritzHistoryView.setVisibility(View.INVISIBLE);
            //mSpritzHistoryView.setText("");
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
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_HIDE_CHAPTER_LABEL), 2000);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);
        mAuthorView = ((TextView) root.findViewById(R.id.author));
        mTitleView = ((TextView) root.findViewById(R.id.url));
        mChapterView = ((TextView) root.findViewById(R.id.chapter));
        mChapterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSpritzer.getMaxChapter() > 1) {
                    mBus.post(new ChapterSelectRequested());
                }
            }
        });
        mSpritzHistoryView = (TextView) root.findViewById(R.id.spritzHistory);
        mProgress = ((ProgressBar) root.findViewById(R.id.progress));
        mSpritzView = (SpritzerTextView) root.findViewById(R.id.spritzText);
        //mSpritzView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        //mSpritzHistoryView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "UbuntuMono-R.ttf"));
        mSpritzView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpritzer != null && mSpritzer.isMediaSelected()) {
                    if (mSpritzer.isPlaying()) {
                        mSpritzer.pause();
                        updateMetaUi();
                        showMetaUi(true);
                        dimActionBar(false);
                    } else {
                        mSpritzer.start();
                        showMetaUi(false);
                        dimActionBar(true);
                    }
                } else {
                    chooseMedia();
                }
            }
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        GlanceApplication app = (GlanceApplication) getActivity().getApplication();
        mBus = app.getBus();
        mBus.register(this);
        mHandler = new SpritzFragmentHandler(this);
        if (mSpritzer == null) {
            mSpritzer = new AppSpritzer(mBus, mSpritzView);
            mSpritzView.setSpritzer(mSpritzer);
            if (mSpritzer.getMedia() == null && !mUserIsChoosingEpub) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SPRITZ_TEXT, getString(R.string.select_epub)), 1500);
            } else {
                // AppSpritzer loaded the last book being read
                updateMetaUi();
                showMetaUi(true);
            }
        } else {
            mSpritzer.setEventBus(mBus);
            mSpritzView.setSpritzer(mSpritzer);
            if (!mSpritzer.isPlaying()) {
                updateMetaUi();
                showMetaUi(true);
            } else {
                // If the spritzer is currently playing, be sure to hide the ActionBar
                // Might the Android linter be a bit aggressive with these null checks?
                if (getActivity() != null && getActivity().getActionBar() != null) {
                    getActivity().getActionBar().hide();
                }
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

    /**
     * Called when the Spritzer finishes a section.
     * Called on a background thread
     */
    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateMetaUi();
                showMetaUi(true);
                dimActionBar(false);
            }
        });
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

    @Subscribe
    public void onHttpUrlParsed(HttpUrlParsedEvent event) {
        showIndeterminateProgress(false);
        //mSpritzer.pause();
        updateMetaUi();
        showMetaUi(true);
    }

    public AppSpritzer getSpritzer() {
        return mSpritzer;
    }

    private static final int SELECT_MEDIA = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void chooseMedia() {

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

        mUserIsChoosingEpub = true;
        startActivityForResult(intent, SELECT_MEDIA);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_MEDIA && data != null) {
            mUserIsChoosingEpub = false;
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT >= 19) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            feedMediaUriToSpritzer(uri);
            updateMetaUi();
        }
    }

    /**
     * A Handler bound to the UI thread. Used to conveniently
     * handle actions that should occur after some delay.
     */
    protected class SpritzFragmentHandler extends Handler {

        private WeakReference<SpritzFragment> mWeakSpritzFragment;

        public SpritzFragmentHandler(SpritzFragment fragment) {
            mWeakSpritzFragment = new WeakReference<SpritzFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Object obj = msg.obj;

            SpritzFragment spritzer = mWeakSpritzFragment.get();
            if (spritzer == null) {
                return;
            }
            switch (what) {
                case MSG_HIDE_CHAPTER_LABEL:
                    if (getActivity() != null) {
                        if (mSpritzer != null && mSpritzer.isPlaying()) {
                            spritzer.mChapterView.setVisibility(View.INVISIBLE);
                        }
                    }
                    break;
                case MSG_SPRITZ_TEXT:
                    if (mSpritzer != null) {
                        mSpritzer.setTextAndStart((String) obj);
                    }
                    break;
            }
        }

    }

}
