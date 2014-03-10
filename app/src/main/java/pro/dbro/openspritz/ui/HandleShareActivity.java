package pro.dbro.openspritz.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import pro.dbro.openspritz.R;
import pro.dbro.openspritz.Spritzer;
import pro.dbro.openspritz.SpritzerTextView;


public class HandleShareActivity extends ActionBarActivity {

    private static final String TYPE_TEXT = "text/plain";
    private SpritzerTextView mSpritzerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_share);
        mSpritzerText = (SpritzerTextView)findViewById(R.id.spritzText);
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();
        if(Intent.ACTION_SEND.equals(action) && type !=null){
            if(TYPE_TEXT.equals(type)){
                handleSpritzText(intent);
            }
        }
        mSpritzerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Spritzer spritzer = mSpritzerText.getSpritzer();
                if(spritzer.isPlaying()){
                    mSpritzerText.pause();
                }else{
                    mSpritzerText.play();
                }
            }
        });


    }

    private void handleSpritzText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            mSpritzerText.setSpritzText(sharedText);
        }
    }



}
