package pro.dbro.openspritz;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by andrewgiang on 3/3/14.
 */
public class SpritzerTextView extends TextView{

    private  Spritzer mSpritzer;

    public SpritzerTextView(Context context) {
        super(context);
        init();
    }
    public SpritzerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpritzerTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setWpm(int wpm){
        mSpritzer.setWpm(wpm);
    }
    /**
     * Set a custom spritzer
     * @param spritzer
     */
    public void setSpritzer(Spritzer spritzer){
        mSpritzer = spritzer;
        mSpritzer.swapTextView(this);
    }

    /**
     * Initialize a basic spritzer
     */
    private void init() {
        mSpritzer = new Spritzer(this);
    }

    /**
     * Pass input text to spritzer object
     * @param input
     */
    public void setSpritzText(String input){
        mSpritzer.setText(input);
    }

    /**
     * Will play the spritz text that was set in setSpritzText
     */
    public void play(){
        mSpritzer.start();
    }
    public void pause(){
        mSpritzer.pause();
    }
    public Spritzer getSpritzer(){
        return mSpritzer;
    }
}