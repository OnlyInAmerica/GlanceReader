package pro.dbro.openspritz;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Created by andrewgiang on 3/3/14.
 */
public class SpritzerTextView extends TextView implements View.OnClickListener {

    private  Spritzer mSpritzer;
    private Paint mPaintGuides;
    private Paint mPaintPivotIndicator;
    private boolean mDefaultClickListener = false;

    public SpritzerTextView(Context context) {
        super(context);
        init();
    }
    public SpritzerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SpritzerTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        final TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SpritzerTextView, 0, 0);
        try {
            mDefaultClickListener = a.getBoolean(R.styleable.SpritzerTextView_clickControls, false);
        }finally {
            a.recycle();
        }
        init();

    }

    private void init() {
        mSpritzer = new Spritzer(this);
        mPaintGuides = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintGuides.setColor(getCurrentTextColor());
        mPaintPivotIndicator = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintPivotIndicator.setStyle(Paint.Style.STROKE);
        mPaintPivotIndicator.setStrokeWidth(3);
        if(mDefaultClickListener){
            this.setOnClickListener(this);
        }

    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Measurements for top & bottom guide line
        int beginTopX = 0, beginTopY = 0, endTopX = getMeasuredWidth(), endTopY = 0;
        int beginBottomX = 0, beginBottomY = getMeasuredHeight(), endBottomX = getMeasuredWidth(), endBottomY = getMeasuredHeight();
        //Paint the top guide and bottom guide bars
        canvas.drawLine(beginTopX, beginTopY, endTopX, endTopY ,mPaintGuides);
        canvas.drawLine(beginBottomX, beginBottomY, endBottomX, endBottomY, mPaintGuides);

        //Measurements for pivot indicator
        final float textSize = getTextSize();
        final float centerX = textSize * 2 + getPaddingLeft();
        final float centerY = 0;
        final int radius = 10;
        //Paint the pivot indicator
        canvas.drawCircle(centerX, centerY, radius, mPaintPivotIndicator); // Circle for pivot
        canvas.drawLine(centerX, centerY, centerX, radius * 2, mPaintPivotIndicator); //line through center of circle
    }

    /**
     * This determines the words per minute the sprizter will read at
     *
     * @param wpm the number of words per minute
     */
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

    @Override
    public void onClick(View v) {
        if(mSpritzer.isPlaying()){
            pause();
        }else{
            play();
        }

    }
}