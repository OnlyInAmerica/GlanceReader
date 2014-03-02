package pro.dbro.spritzdroid;

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
//
public class EpubSpritzer extends Spritzer {

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
            setText(loadCleanStringFromChapter(mChapter));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (!mPlaying && mPlayingRequested && (mChapter < mMaxChapter)) {
            mPlaying = true;
            Log.i(TAG, "Queue cleared, loading chapter " + mChapter);
            printNextChapter();
        }
    }

    private void printNextChapter() {
        setText(loadCleanStringFromChapter(mChapter++));
        start();
    }

    private String loadCleanStringFromChapter(int chapter) {
        try {
            Log.i(TAG, "Loading chapter " + chapter);
            String bookStr = new String(mBook.getSpine().getResource(chapter).getData(), "UTF-8");
            return Html.fromHtml(bookStr).toString().replace("\n", "").replaceAll("(?s)<!--.*?-->", "");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Parsing failed " + e.getMessage());
            return "";
        }
    }
}
