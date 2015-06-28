package pro.dbro.glance.fragments;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;

import pro.dbro.glance.AppSpritzer;
import pro.dbro.glance.GlanceApplication;
import pro.dbro.glance.GlancePrefsManager;
import pro.dbro.glance.R;
import pro.dbro.glance.events.ChapterSelectRequested;
import pro.dbro.glance.events.EpubDownloadedEvent;
import pro.dbro.glance.events.HttpUrlParsedEvent;
import pro.dbro.glance.events.NextChapterEvent;
import pro.dbro.glance.events.SpritzMediaReadyEvent;
import pro.dbro.glance.formats.SpritzerMedia;
import pro.dbro.glance.lib.SpritzerTextView;
import pro.dbro.glance.lib.events.SpritzFinishedEvent;

public class SpritzFragment extends Fragment {
    private static final String TAG = "SpritzFragment";

    // SpritzFragmentHandler Message codes
    protected static final int MSG_SPRITZ_TEXT = 1;
    protected static final int MSG_HIDE_CHAPTER_LABEL = 2;

    private AppSpritzer mSpritzer;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mChapterView;
    private TextView mSpritzHistoryView;
    private ProgressBar mProgress;
    private SpritzerTextView mSpritzView;
    private Bus mBus;
    private SpritzFragmentHandler mHandler;
    private boolean mShowingTips;

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

        // It is the Spritzer's responsibility to display loading text display
        // It is this fragment's responsibility to toggle the progress indicator
        if (AppSpritzer.isHttpUri(mediaUri)) {
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
            mAuthorView.setText("");
            mTitleView.setText("");
            mChapterView.setText("");
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
    }

    /**
     * Hide or Show the UI related to Book Title, Author,
     * and current progress
     *
     * @param show
     */
    public void showMetaUi(boolean show) {
        if (show) {
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                mAuthorView.setVisibility(View.VISIBLE);
            }
            mTitleView.setVisibility(View.VISIBLE);
            mChapterView.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mAuthorView.setVisibility(View.INVISIBLE);
            mTitleView.setVisibility(View.INVISIBLE);
            mChapterView.setVisibility(View.INVISIBLE);
            mProgress.setVisibility(View.INVISIBLE);
            //mSpritzHistoryView.setText("");
        }
    }

    public void hideActionBar(boolean dim) {
        if (getActivity().getActionBar() == null) return;
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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mAuthorView.setVisibility(View.GONE);
        }
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
        setupViews(mSpritzView, mSpritzHistoryView);
        return root;
    }

    private void showTips() {
        mShowingTips = true;
        int[] viewLocation = new int[2];
        mSpritzView.getLocationOnScreen(viewLocation);
        PointTarget target = new PointTarget(viewLocation[0] + mSpritzView.getWidth() / 3, viewLocation[1] + mSpritzView.getHeight() / 2);
        final long SHOWCASE_SINGLESHOT_ID = 3141519;
        new ShowcaseView.Builder(getActivity())
                .setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                        showMetaUi(true);
                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {
                        showcaseView.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                Log.i(TAG, "hiding meta uti");
                                showMetaUi(false);
                            }

                        }, 500);
                    }
                })
                .singleShot(SHOWCASE_SINGLESHOT_ID)
                .setTarget(target)
                .setContentTitle("Welcome to Glance")
                .setContentText("Touch the glance view to pause or resume. If you miss something, pull down to view a brief history")
                .hideOnTouchOutside()
                .build();
    }

    private void pauseSpritzer() {
        mSpritzer.pause();
        updateMetaUi();
        showMetaUi(true);
        hideActionBar(false);
    }

    private void startSpritzer() {
        mSpritzer.start(true);
        showMetaUi(false);
        hideActionBar(true);
    }

    static float initHeight;

    /**
     * Adjust the target View's height in proportion to
     * drag events. On drag release, snap the view back into
     * it's original place.
     */
    private void setupViews(final View touchTarget, final View transformTarget) {
        touchTarget.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (initHeight == 0)
                    initHeight = transformTarget.getHeight();
            }
        });
        touchTarget.setOnTouchListener(new View.OnTouchListener() {

            private ViewGroup.LayoutParams params;
            private float peakHeight;
            private float lastTouchY;
            private float firstTouchY;
            private final float fullOpacityHeight = 300;
            /** The distance between ACTION_DOWN and ACTION_UP, above which should
             * be interpreted as a drag, below which a click.
             */
            private final float movementForDragThreshold = 20;
            /** The time between ACTION_DOWN and ACTION_MOVE, above which should
             * be interpreted as a drag, and the spritzer paused
             */
            private final int timeForPauseThreshold = 50;

            private boolean mSetText = false;
            private boolean mAnimatingBack = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (params == null) params = transformTarget.getLayoutParams();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mSpritzer.isPlaying())
                        mSpritzer.pause();
                    else
                        mSpritzer.start(true);
//                    Log.i("TOUCH", "Down");
                    int coords[] = new int[2];
                    transformTarget.getLocationOnScreen(coords);
                    lastTouchY = firstTouchY = event.getRawY();
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (mSpritzer.isPlaying() && (event.getEventTime() - event.getDownTime() > timeForPauseThreshold)) mSpritzer.pause();
                    if (!mSetText) {
                        mSpritzHistoryView.setText(mSpritzer.getHistoryString(400));
                        mSetText = true;
                    }
                    float newHeight = event.getRawY() - lastTouchY + transformTarget.getHeight();
//                    Log.i("MOVE", "touch-y: " + event.getRawY() + " lastTouch: " + lastTouchY + " height: " + transformTarget.getHeight());
                    if (newHeight > initHeight) {
//                        Log.i("TOUCH", "setting height " + params.height);
                        params.height = (int) newHeight;
                        if (newHeight >= fullOpacityHeight) {
                            transformTarget.setAlpha(1f);
//                            Log.i("TOUCH", "alpha 1");
                        } else {
                            transformTarget.setAlpha((newHeight / fullOpacityHeight) * .8f);
//                            Log.i("TOUCH", "alpha " + newHeight / fullOpacityHeight);
                        }
                        transformTarget.requestLayout();
                    }
                    lastTouchY = event.getRawY();
                }
                if (event.getAction() == MotionEvent.ACTION_UP && !mAnimatingBack) {
                    if (event.getRawY() - firstTouchY < movementForDragThreshold) {
                        // This is a click, not a drag
                        // show/hide meta ui on release
                        if (!mSpritzer.isPlaying()) pauseSpritzer();
                        else startSpritzer();
                        return false;
                    }
                    peakHeight = event.getRawY() - lastTouchY + transformTarget.getHeight();
                    mAnimatingBack = true;
//                    Log.i("TOUCH", "animating back up " + initHeight + " " + transformTarget.getHeight());
                    invokeSpring(transformTarget);

                }
                return true;
            }

            private void invokeSpring(final View targetView) {
                mAnimatingBack = true;
                // Create a system to run the physics loop for a set of springs.
                SpringSystem springSystem = SpringSystem.create();

                // Add a spring to the system.
                Spring spring = springSystem.createSpring();

                // Add a listener to observe the motion of the spring.
                spring.addListener(new SimpleSpringListener() {

                    @Override
                    public void onSpringUpdate(Spring spring) {
                        // You can observe the updates in the spring
                        // state by asking its current value in onSpringUpdate.
                        float value = (float) spring.getCurrentValue();
                        float scale = 1f - (value);
                        //Log.i("SPRING", String.valueOf(value));
                        // 0 - initHeight
                        // 1 - peakHeight
                        if (scale < 0.05) {
                            //Log.i("SPRING", "finished");
                            mSpritzHistoryView.setText("");
                            mSetText = false;
                            params.height = (int) initHeight;
                            transformTarget.setAlpha(0);
                            mAnimatingBack = false;
                            startSpritzer();
                        } else if (mAnimatingBack) {
                            params.height = (int) ((scale * (peakHeight - initHeight)) + initHeight);
                            if (transformTarget.getHeight() >= fullOpacityHeight * 2) {
                                transformTarget.setAlpha(1f);
                            } else {
                                //fullOpacityHeight*2 = full
                                //fullOpacityHeight = empty
                                transformTarget.setAlpha(Math.max(0, fullOpacityHeight - transformTarget.getHeight() * 1.1f));
                                //Log.i("TOUCH", "alpha " + touchTarget.getHeight() / fullOpacityHeight);
                            }
                        }
                        transformTarget.requestLayout();
                    }
                });

                // Set the spring in motion; moving from 0 to 1
                spring.setEndValue(1);
            }
        });
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
            if (mSpritzer.getMedia() == null) {
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
                    hideActionBar(true);
                }
            }
        }
        mSpritzView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!sShownTips) {
                    showTips();
                    sShownTips = true;
                }
            }
        });

    }

    private static boolean sShownTips = false;

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
                hideActionBar(false);
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
        if (!mShowingTips)
            showMetaUi(true);
    }

    @Subscribe
    public void onEpubDownloaded(EpubDownloadedEvent event) {
        showIndeterminateProgress(false);
        updateMetaUi();
        if (!mShowingTips)
            showMetaUi(true);
    }

    @Subscribe
    public void onSpritzMediaReady(SpritzMediaReadyEvent event) {
        updateMetaUi();
        if (!mShowingTips)
            showMetaUi(true);
    }

    public AppSpritzer getSpritzer() {
        return mSpritzer;
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
                        mSpritzer.setTextAndStart((String) obj, false);
                    }
                    break;
            }
        }

    }

}
