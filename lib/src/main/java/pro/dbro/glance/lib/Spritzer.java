package pro.dbro.glance.lib;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import pro.dbro.glance.lib.events.SpritzFinishedEvent;
import pro.dbro.glance.lib.events.SpritzProgressEvent;

/**
 * Spritzer parses a String into a Queue
 * of words, and displays them one-by-one
 * onto a TextView at a given WPM.
 */
public class Spritzer {
    protected static final String TAG = "Spritzer";
    protected static final boolean VERBOSE = false;

    protected static final int MSG_PRINT_WORD = 1;
    protected static final int MSG_SET_ENABLED = 2;

    protected static final int CHARS_LEFT_OF_PIVOT = 3;
    protected int mMaxWordLength = 13;
    protected String[] mWordArray;                  // A parsed list of words parsed from {@link #setText(String input)}
    protected ArrayList<String> mDisplayWordList;        // The queue of words from mWordArray yet to be displayed
    protected TextView mTarget;
    protected int mWPM;
    protected Handler mSpritzHandler;
    protected final Object mSpritzThreadStartedSync = new Object();
    protected boolean mPlaying;
    protected boolean mPlayingRequested;
    protected boolean mSpritzThreadStarted;

    protected Bus mBus;
    protected int mCurWordIdx;

    public static interface SpritzerCallback {
        public void onSpritzerFinished();
    }

    public Spritzer(TextView target) {
        init();
        mTarget = target;
        mSpritzHandler = new SpritzHandler(this);
    }

    public void setTextAndStart(String input, boolean fireFinishEvent) {
        if (VERBOSE) Log.i(TAG, "setTextAndStart, no cb");
        pause();
        setText(input);
        start(fireFinishEvent);
    }

    public void setTextAndStart(String input, SpritzerCallback cb, boolean fireFinishEvent) {
        if (VERBOSE) Log.i(TAG, "setTextAndStart, with cb");
        pause();
        setText(input);
        start(cb, fireFinishEvent);
    }

    /**
     * Prepare to Spritz the given String input
     * <p/>
     * Call {@link #start(boolean)} to begin display
     *
     * @param input
     */
    public void setText(String input) {
        createWordArrayFromString(input);
        refillWordDisplayList();
    }

    public void clearText() {
        mDisplayWordList.clear();
        mWordArray = null;
        mCurWordIdx = 0;
    }

    public String getNextWord() {
        if (!isWordListComplete()) {
            return mDisplayWordList.get(mCurWordIdx);
        }
        return null;
    }

    public void setMaxWordLength(int maxWordLength) {
        mMaxWordLength = maxWordLength;
    }

    /**
     * Pass a Bus to receive events on, such as
     * when the display of a given String is finished
     *
     * @param bus
     */
    public void setEventBus(Bus bus) {
        mBus = bus;
    }

    /**
     * Create a String[] from a given String, splitting
     * on spaces but condensing adjacent spaces
     *
     * @param input
     */
    private void createWordArrayFromString(String input) {
        mWordArray = input
                .replaceAll("/\\s+/g", " ")      // condense adjacent spaces
                .split(" ");                     // split on spaces
    }

    protected void init() {
        mCurWordIdx = 0;
        mDisplayWordList = new ArrayList<String>();
        mWPM = 500;
        mPlaying = false;
        mPlayingRequested = false;
        mSpritzThreadStarted = false;
    }

    /**
     * Rewind the spritzer by the specified
     * amount of words
     *
     * @param numWords
     */
    public void rewind(int numWords) {
        if (mCurWordIdx > numWords) {
            mCurWordIdx -= numWords;
        } else {
            mCurWordIdx = 0;
        }
    }

    /**
     * Get the estimated time remaining in the
     * currently loaded String Queue
     *
     * @return
     */
    public int getMinutesRemainingInQueue() {
        if (mDisplayWordList.size() == 0) {
            return 0;
        }
        return (mDisplayWordList.size() - (mCurWordIdx + 1)) / mWPM;
    }

    /**
     * Return the completeness of the current
     * Spritz segment as a float between 0 and 1.
     *
     * @return a float between 0 (not started) and 1 (complete)
     */
    public float getQueueCompleteness() {
        return (mWordArray == null) ? 0 : ((float) mCurWordIdx) / mDisplayWordList.size();
    }

    /**
     * Return the current Word Per Minute rate.
     */
    public int getWpm() {
        return mWPM;
    }

    /**
     * Set the target Word Per Minute rate.
     * Effective immediately.
     *
     * @param wpm
     */
    public void setWpm(int wpm) {
        mWPM = wpm;
    }

    /**
     * Swap the target TextView. Call this if your
     * host Activity is Destroyed and Re-Created.
     * Effective immediately.
     *
     * @param target
     */
    public void swapTextView(TextView target) {
        mTarget = target;
        if (!mPlaying) {
            peekNextWord();
        }
    }

    /**
     * Start displaying the String input
     * fed to {@link #setText(String)}
     */
    public void start(boolean fireFinishEvent) {
        start(null, fireFinishEvent);
    }

    /**
     * Start displaying the String input
     * fed to {@link #setText(String)}
     *
     * @param cb callback to be notified when Spritzer finished.
     *           Called from background thread.
     */
    public void start(SpritzerCallback cb, boolean fireFinishEvent) {
        if (mPlaying || mWordArray == null) {
            if (VERBOSE) Log.w(TAG, "Start called in invalid state");
            return;
        }
        if (VERBOSE) Log.i(TAG, "Start called " + ((cb == null) ? "without" : "with") + " callback." );

        mPlayingRequested = true;
        startTimerThread(cb, fireFinishEvent);
    }

    private int getInterWordDelay() {
        return 60000 / mWPM;
    }

    private void refillWordDisplayList() {
        mCurWordIdx = 0;
        mDisplayWordList.clear();
        mDisplayWordList.addAll(Arrays.asList(mWordArray));
    }

    /**
     * Read the current head of mDisplayWordList and
     * submit the appropriate Messages to mSpritzHandler.
     * <p/>
     * Split long words submitting the first segment of a word
     * and placing the second at the head of mDisplayWordList for processing
     * during the next cycle.
     * <p/>
     * Must be called on a background thread, as this method uses
     * {@link Thread#sleep(long)} to time pauses in display.
     *
     * @throws InterruptedException
     */
    protected void processNextWord() throws InterruptedException {
        if (mCurWordIdx < mDisplayWordList.size()) {
            String word = mDisplayWordList.get(mCurWordIdx);
            word = splitLongWord(word);

            if (mBus != null) {
                mBus.post(new SpritzProgressEvent(mCurWordIdx));
            }
            mSpritzHandler.sendMessage(mSpritzHandler.obtainMessage(MSG_PRINT_WORD, word));
            Thread.sleep(getInterWordDelay() * delayMultiplierForWord(word));
            // If word is end of a sentence, add three blanks
            if (word.contains(".") || word.contains("?") || word.contains("!")) {
                for (int x = 0; x < 3; x++) {
                    mSpritzHandler.sendMessage(mSpritzHandler.obtainMessage(MSG_PRINT_WORD, "  "));
                    Thread.sleep(getInterWordDelay());
                }
            }
        } else {
            if (VERBOSE) Log.i(TAG, "processNextWord called with invalid mCurWordIdx: " + mCurWordIdx + " array size " + mDisplayWordList.size());
        }
    }

    /**
     * Split the given String if appropriate and
     * add the tail of the split to the head of
     * {@link #mDisplayWordList}
     * <p/>
     * Currently public for testing purposes
     *
     * @param word
     * @return
     */
    public String splitLongWord(String word) {
        if (word.length() > mMaxWordLength) {
            int splitIndex = findSplitIndex(word);
            String firstSegment;
            firstSegment = word.substring(0, splitIndex);
            // A word split is always indicated with a hyphen unless ending in a period
            if (!firstSegment.contains("-") && !firstSegment.endsWith(".")) {
                firstSegment = firstSegment + "-";
            }
            mDisplayWordList.add(mCurWordIdx + 1, word.substring(splitIndex));
            word = firstSegment;
        }
        return word;
    }

    /**
     * Determine the split index on a given String
     * e.g If it exceeds mMaxWordLength or contains a hyphen
     *
     * @param thisWord
     * @return the index on which to split the given String
     */
    private int findSplitIndex(String thisWord) {
        int splitIndex;
        // Split long words, at hyphen or dot if present.
        if (thisWord.contains("-")) {
            splitIndex = thisWord.indexOf("-") + 1;
        } else if (thisWord.contains(".")) {
            splitIndex = thisWord.indexOf(".") + 1;
        } else if (thisWord.length() > mMaxWordLength * 2) {
            // if the word is floccinaucinihilipilifcation, for example.
            splitIndex = mMaxWordLength - 1;
            // 12 characters plus a "-" == 13.
        } else {
            // otherwise we want to split near the middle.
            splitIndex = Math.round(thisWord.length() / 2F);
        }
        // in case we found a split character that was > mMaxWordLength characters in.
        if (splitIndex > mMaxWordLength) {
            // If we split the word at a splitting char like "-" or ".", we added one to the splitIndex
            // in order to ensure the splitting char appears at the head of the split. Not accounting
            // for this in the recursive call will cause a StackOverflowException
            return findSplitIndex(thisWord.substring(0,
                    wordContainsSplittingCharacter(thisWord) ? splitIndex - 1 : splitIndex));
        }
        if (VERBOSE) {
            Log.i(TAG, "Splitting long word " + thisWord + " into " + thisWord.substring(0, splitIndex) +
                    " and " + thisWord.substring(splitIndex));
        }
        return splitIndex;
    }

    private boolean wordContainsSplittingCharacter(String word) {
        return (word.contains(".") || word.contains("-"));
    }

    private void peekNextWord() {
        if (mCurWordIdx >= 0 && !isWordListComplete() && mDisplayWordList.get(mCurWordIdx) != null) {
            printWord(mDisplayWordList.get(mCurWordIdx));
        }
    }

    /**
     * Applies the given String to this Spritzer's TextView,
     * padding the beginning if necessary to align the pivot character.
     * Styles the pivot character.
     *
     * @param word
     */
    private void printWord(String word) {
        int startSpan = 0;
        int endSpan = 0;
        word = word.trim();
        if (word.length() == 1) {
            StringBuilder builder = new StringBuilder();
            for (int x = 0; x < CHARS_LEFT_OF_PIVOT; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        } else if (word.length() <= CHARS_LEFT_OF_PIVOT * 2) {
            StringBuilder builder = new StringBuilder();
            int halfPoint = word.length() / 2;
            int beginPad = CHARS_LEFT_OF_PIVOT - halfPoint;
            for (int x = 0; x <= beginPad; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = halfPoint + beginPad;
            endSpan = startSpan + 1;
        } else {
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        }

        Spannable spanRange = new SpannableString(word);
        TextAppearanceSpan tas = new TextAppearanceSpan(mTarget.getContext(), R.style.PivotLetter);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTarget.setText(spanRange);
    }

    public void pause() {
        if (VERBOSE) {
            Log.i(TAG, "Pausing spritzer");
        }
        mPlayingRequested = false;
        synchronized (mSpritzThreadStartedSync) {
            while (mPlaying) {
                try {
                    mSpritzThreadStartedSync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    /**
     * Begin the background timer thread
     */
    private void startTimerThread(final SpritzerCallback cb, final boolean fireFinishEvent) {
        synchronized (mSpritzThreadStartedSync) {
            if (!mSpritzThreadStarted) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (VERBOSE) {
                            Log.i(TAG, "Starting spritzThread with queue length " + mDisplayWordList.size() + " and " +
                                    ((cb == null) ? "no " : "a ") + "callback. Playback requested: " + mPlayingRequested);
                        }
                        mPlaying = true;
                        synchronized (mSpritzThreadStartedSync) {
                            mSpritzThreadStarted = true;
                        }
                        while (mPlayingRequested) {
                            try {
                                processNextWord();
                                if (isWordListComplete()) {
                                    if (VERBOSE) {
                                        Log.i(TAG, "Word list completely displayed after processNextWord. Pausing");
                                    }
                                    mPlayingRequested = false;
                                    if (mBus != null && fireFinishEvent) {
                                        mBus.post(new SpritzFinishedEvent());
                                    }
                                    if (cb != null) {
                                        cb.onSpritzerFinished();
                                    }
                                }
                                mCurWordIdx++;
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Exception spritzing");
                                e.printStackTrace();
                            }
                        }
                        if (VERBOSE) Log.i(TAG, "Stopping spritzThread");
                        mPlaying = false;
                        synchronized (mSpritzThreadStartedSync) {
                            mSpritzThreadStartedSync.notify();
                        }
                        mSpritzThreadStarted = false;

                    }
                }).start();
            }
        }
    }

    private int delayMultiplierForWord(String word) {
        // double rest if length > 6 or contains (.,!?)
        if (word.length() >= 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"")) {
            return 3;
        }
        return 1;
    }

    protected boolean isWordListComplete() {
        return mCurWordIdx >= mDisplayWordList.size() - 1;
    }

    /**
     * A Handler intended for creation on the Main thread.
     * Messages are intended to be passed from a background
     * timing thread. This Handler communicates timing
     * thread events to the Main thread for UI update.
     */
    protected static class SpritzHandler extends Handler {
        private WeakReference<Spritzer> mWeakSpritzer;

        public SpritzHandler(Spritzer muxer) {
            mWeakSpritzer = new WeakReference<Spritzer>(muxer);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            Spritzer spritzer = mWeakSpritzer.get();
            if (spritzer == null) {
                return;
            }

            switch (what) {
                case MSG_PRINT_WORD:
                    spritzer.printWord((String) obj);
                    break;
                case MSG_SET_ENABLED:
                    spritzer.mTarget.setEnabled(true);
                    break;
                default:
                    throw new RuntimeException("Unexpected msg what=" + what);
            }
        }

    }
}
