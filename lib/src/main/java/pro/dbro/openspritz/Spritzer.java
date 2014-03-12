package pro.dbro.openspritz;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;

import pro.dbro.openspritz.events.SpritzFinishedEvent;

/**
 * Spritzer parses a String into a Queue
 * of words, and displays them one-by-one
 * onto a TextView at a given WPM.
 */
public class Spritzer {
    protected static final String TAG = "Spritzer";
    protected static final boolean VERBOSE = false;

    protected static final int MSG_PRINT_WORD = 1;

    protected static final int MAX_WORD_LENGTH = 13;
    protected static final int CHARS_LEFT_OF_PIVOT = 3;
    protected String[] mWordArray;                  // The current list of words
    protected ArrayDeque<String> mWordQueue;        // The queue being actively displayed
    protected TextView mTarget;
    protected int mWPM;
    protected Handler mSpritzHandler;
    protected Object mPlayingSync = new Object();
    protected boolean mPlaying;
    protected boolean mPlayingRequested;
    protected boolean mSpritzThreadStarted;

    protected Bus mBus;

    public Spritzer(TextView target) {
        init();
        mTarget = target;
        mSpritzHandler = new SpritzHandler(this);
    }

    public void setText(String input) {
        createWordArrayFromString(input);
        refillWordQueue();
    }

    public void setEventBus(Bus bus) {
        mBus = bus;
    }

    private void createWordArrayFromString(String input) {
        mWordArray = input
                .replaceAll("/\\s+/g", " ")      // condense adjacent spaces
                .split(" ");                    // split on spaces
    }

    protected void init() {
        mWordQueue = new ArrayDeque<String>();
        mWPM = 500;
        mPlaying = false;
        mPlayingRequested = false;
        mSpritzThreadStarted = false;
    }

    public int getMinutesRemainingInQueue() {
        if (mWordQueue.size() == 0) {
            return 0;
        }
        return mWordQueue.size() / mWPM;
    }

    public void setWpm(int wpm) {
        mWPM = wpm;
    }

    public void swapTextView(TextView target) {
        mTarget = target;
        if (!mPlaying) {
            printLastWord();
        }

    }

    public void start() {
        if (mPlaying || mWordArray == null) {
            return;
        }
        if (mWordQueue.isEmpty()) {
            refillWordQueue();
        }

        mPlayingRequested = true;
        startTimerThread();
    }

    private int getInterWordDelay() {
        return 60000 / mWPM;
    }

    private void refillWordQueue() {
        mWordQueue.clear();
        mWordQueue.addAll(Arrays.asList(mWordArray));
    }

    protected void processNextWord() throws InterruptedException {
        if (!mWordQueue.isEmpty()) {
            String word = mWordQueue.remove();
            word = splitLongWord(word);

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
            if (mBus != null) {
                mBus.post(new SpritzFinishedEvent());
            }
        }
    }

    protected String splitLongWord(String word) {
        if (word.length() > MAX_WORD_LENGTH) {
            String firstSegment;
            int splitIndex;
            if (word.contains("-")) {
                splitIndex = word.indexOf("-") + 1;
            } else if (word.contains(".")) {
                splitIndex = word.indexOf(".") + 1;
            } else if (word.length() > MAX_WORD_LENGTH * 2) {
                splitIndex = MAX_WORD_LENGTH;
            } else {
                splitIndex = word.length() / 2;
            }
            if (VERBOSE) {
                Log.i(TAG, "Splitting long word " + word + " into " + word.substring(0, splitIndex) + " and " + word.substring(splitIndex));
            }
            firstSegment = word.substring(0, splitIndex);
            // A word split is always indicated with a hyphen unless ending in a period
            if (!firstSegment.contains("-") && !firstSegment.endsWith(".")) {
                firstSegment = firstSegment + "-";
            }
            mWordQueue.addFirst(word.substring(splitIndex));
            word = firstSegment;

        }
        return word;
    }

    private void printLastWord() {
        if (mWordArray != null) {
            printWord(mWordArray[mWordArray.length - 1]);
        }
    }

    private void printWord(String word) {
        int startSpan = 0;
        int endSpan = 0;
        word = word.trim();
        if (VERBOSE) Log.i(TAG + word.length(), word);
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
            if (VERBOSE) Log.i(TAG + word.length(), "pivot: " + word.substring(startSpan, endSpan));
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
        mPlayingRequested = false;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    private void startTimerThread() {
        synchronized (mPlayingSync) {
            if (!mSpritzThreadStarted) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (VERBOSE)
                            Log.i(TAG, "Starting spritzThread with queue length " + mWordQueue.size());
                        mPlaying = true;
                        mSpritzThreadStarted = true;
                        while (mPlayingRequested) {
                            try {
                                processNextWord();
                                if (mWordQueue.isEmpty()) {
                                    if (VERBOSE)
                                        Log.i(TAG, "Queue is empty after processNextWord. Pausing");
                                    mPlayingRequested = false;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (VERBOSE) Log.i(TAG, "Stopping spritzThread");
                        mPlaying = false;
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
                default:
                    throw new RuntimeException("Unexpected msg what=" + what);
            }
        }

    }
}
