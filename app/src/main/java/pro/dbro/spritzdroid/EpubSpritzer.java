package pro.dbro.spritzdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * Created by davidbrodsky on 2/28/14.
 */
// TODO: Save epub title : chapter-word
// TODO: Save State for multiple books
public class EpubSpritzer extends Spritzer {

    private static final String PREFS = "espritz";

    private Book mBook;
    private int mChapter;
    private int mMaxChapter;

    public EpubSpritzer(TextView target, Uri epubPath) {
        super(target);
        mChapter = 0;

        // Load Book and parse first "chapter"
        try {
            //AssetManager assetManager = target.getContext().getAssets();
            //InputStream epubInputStream = assetManager.open(epubPath);
            InputStream epubInputStream = target.getContext().getContentResolver().openInputStream(epubPath);
            mBook = (new EpubReader()).readEpub(epubInputStream);
            mMaxChapter = mBook.getSpine().getSpineReferences().size();
            restoreState();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (!mPlaying && mPlayingRequested && (mChapter < mMaxChapter)) {
            mPlaying = true;
            printNextChapter();
        }
    }

    private void printNextChapter() {
        setText(loadCleanStringFromChapter(mChapter++));
        saveState();
        start();
    }

    private String loadCleanStringFromChapter(int chapter) {
        try {
            String bookStr = new String(mBook.getSpine().getResource(chapter).getData(), "UTF-8");
            return Html.fromHtml(bookStr).toString().replace("\n", "").replaceAll("(?s)<!--.*?-->", "");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Parsing failed " + e.getMessage());
            return "";
        }
    }

    private void saveState() {
        SharedPreferences.Editor editor = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt("Chapter", mChapter)
                .putInt("Word", mWordArray.length - mWords.size())
                .putString("Title", mBook.getTitle())
                .apply();
    }

    private void restoreState() {
        SharedPreferences prefs = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (mBook.getTitle().compareTo(prefs.getString("Title", "<>?l")) == 0) {
            mChapter = prefs.getInt("Chapter", 0);
            setText(loadCleanStringFromChapter(mChapter));
            int oldSize = prefs.getInt("Word", 0);
            while (mWords.size() > oldSize) {
                mWords.remove();
            }
        } else {
            mChapter = 0;
            setText(loadCleanStringFromChapter(mChapter));
        }
    }
}
