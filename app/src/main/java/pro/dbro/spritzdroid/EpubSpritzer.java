package pro.dbro.spritzdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * Parse an .epub into a Queue of words
 * and display them on a TextView at
 * a given WPM
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

        openEpub(epubPath);
        mTarget.getContext().getString(R.string.touch_to_start);
    }

    public void setEpubPath(Uri epubPath) {
        pause();
        openEpub(epubPath);
        mTarget.setText(mTarget.getContext().getString(R.string.touch_to_start));
    }

    public void openEpub(Uri epubUri) {
        try {
            InputStream epubInputStream = mTarget.getContext().getContentResolver().openInputStream(epubUri);
            String epubPath = FileUtils.getPath(mTarget.getContext(), epubUri);
            if(epubPath == null || !epubPath.contains("epub")){
                reportFileUnsupported();
                return;
            }
            mBook = (new EpubReader()).readEpub(epubInputStream);
            mMaxChapter = mBook.getSpine().getSpineReferences().size();
            restoreState();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean bookSelected(){
        return mBook != null;
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
                .putInt("Word", mWordArray.length - mWordQueue.size())
                .putString("Title", mBook.getTitle())
                .putInt("Wpm", mWPM)
                .apply();
    }

    private void restoreState() {
        SharedPreferences prefs = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (mBook.getTitle().compareTo(prefs.getString("Title", "<>?l")) == 0) {
            mChapter = prefs.getInt("Chapter", 0);
            setText(loadCleanStringFromChapter(mChapter));
            int oldSize = prefs.getInt("Word", 0);
            setWpm(prefs.getInt("Wpm", 500));
            while (mWordQueue.size() > oldSize) {
                mWordQueue.remove();
            }
        } else {
            mChapter = 0;
            setText(loadCleanStringFromChapter(mChapter));
        }
    }

    private void reportFileUnsupported() {
        Toast.makeText(mTarget.getContext(), mTarget.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
    }

}
