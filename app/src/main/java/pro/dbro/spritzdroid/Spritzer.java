package pro.dbro.spritzdroid;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.widget.TextView;

import com.google.common.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;

import pro.dbro.spritzdroid.events.SpritzFinishedEvent;

/**
 * Spritzer parses a String into a Queue
 * of words, and displays them one-by-one
 * onto a TextView at a given WPM.
 */
public class Spritzer {
    protected static final String TAG = "Spritzer";
    protected static final int MSG_PRINT_WORD = 1;

    protected String[] mWordArray;                  // The current list of words
    protected ArrayDeque<String> mWordQueue;        // The queue being actively displayed
    protected TextView mTarget;
    protected int mWPM;
    protected Handler mSpritzHandler;
    protected Object mPlayingSync = new Object();
    protected boolean mPlaying;
    protected boolean mPlayingRequested;
    protected boolean mSpritzThreadStarted;

    protected EventBus mEventBus;

    public Spritzer(TextView target) {
        init();
        mTarget = target;
        mSpritzHandler = new SpritzHandler(this);
    }

    public void setText(String input) {
        pause();
        createWordArrayFromString(input);
        refillWordQueue();
    }

    public void setEventBus(EventBus bus) {
        mEventBus = bus;
    }

    private void createWordArrayFromString(String input) {
        mWordArray = input
                .replaceAll("/\\s+/g", " ")      // condense adjacent spaces
                .split(" ");                    // split on spaces
    }

    protected void init() {
        mWordQueue = new ArrayDeque<String>();
        mWPM = 600;
        mPlaying = false;
        mPlayingRequested = false;
        mSpritzThreadStarted = false;
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
            mPlaying = false;
            if (mEventBus != null) {
                mEventBus.post(new SpritzFinishedEvent());
            }
        }
    }

    private void printLastWord() {
        if(mWordArray != null){
            printWord(mWordArray[mWordArray.length - 1]);
        }
    }

    private void printWord(String word) {
        if (word.length() % 2 == 0) {
            word += " ";
        }
        int startSpan = word.length() / 2;
        int endSpan = startSpan + 1;
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
                        mPlaying = true;
                        mSpritzThreadStarted = true;
                        while (mPlayingRequested) {
                            try {
                                processNextWord();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        mPlaying = false;
                        mSpritzThreadStarted = false;

                    }
                }).start();
            }
        }
    }

    private int delayMultiplierForWord(String word) {
        // double rest if length > 6 or contains (.,!?)
        if (word.length() > 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"")) {
            return 2;
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
