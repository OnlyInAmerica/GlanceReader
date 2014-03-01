package pro.dbro.spritzdroid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.google.common.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;

import pro.dbro.spritzdroid.events.SpritzFinishedEvent;

/**
 * Created by davidbrodsky on 2/28/14.
 */
public class Spritzer implements Runnable {
    protected static final String TAG = "Spritzer";
    protected static final int MSG_PRINT_WORD = 1;

    protected String[] mWordArray;
    protected ArrayDeque<String> mWords;
    protected TextView mTarget;
    protected int mWPM;
    protected Handler mSpritzHandler;
    protected Object mReadySync = new Object();
    protected boolean mPlaying;
    protected boolean mStarted;

    protected EventBus mEventBus;

    public Spritzer(TextView target) {
        init();
        mTarget = target;
        mSpritzHandler = new SpritzHandler(this);
    }

    public void setText(String input){
        createWordArrayFromString(input);
        refillWordQueue();
    }

    public void setEventBus(EventBus bus){
        mEventBus = bus;
    }

    private void createWordArrayFromString(String input) {
        mWordArray = input
                .replaceAll("/\\s+/g", " ")      // condense adjacent spaces
                .split(" ");                    // split on spaces
    }

    protected void init() {
        mWords = new ArrayDeque<String>();
        mWPM = 600;
        mPlaying = false;
        mStarted = false;
    }

    public void setWpm(int wpm) {
        mWPM = wpm;
    }

    public void swapTextView(TextView target){
        mTarget = target;
        if(!mPlaying){
            printLastWord();
        }

    }

    public void start() {
        if (mPlaying || mWordArray == null) {
            return;
        }
        if (mWords.isEmpty()) {
            refillWordQueue();
        }

        mPlaying = true;
        startTimerThread();
    }

    private int getInterWordDelay(){
        return 60000 / mWPM;
    }

    private void refillWordQueue() {
        mWords.clear();
        mWords.addAll(Arrays.asList(mWordArray));
    }

    protected void processNextWord() throws InterruptedException {
        if (!mWords.isEmpty()) {
            String word = mWords.remove();
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
            if(mEventBus != null){
                mEventBus.post(new SpritzFinishedEvent());
            }
        }
    }

    private void printLastWord(){
        printWord(mWordArray[mWordArray.length-1]);
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
        mPlaying = false;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    private void startTimerThread() {
        if (!mStarted) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mStarted = true;
                    while (mPlaying) {
                        try {
                            processNextWord();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mStarted = false;

                }
            }).start();
        }
    }

    private int delayMultiplierForWord(String word) {
        // double rest if length > 6 or contains (.,!?)
        if (word.length() > 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"") ) {
            return 2;
        }
        return 1;
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadySync) {
            mStarted = true;
            mSpritzHandler = new SpritzHandler(this);
        }

        Looper.loop();
        synchronized (mReadySync) {
            mStarted = false;
            mSpritzHandler = null;
        }

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
                Log.w(TAG, "spritzer is null");
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
