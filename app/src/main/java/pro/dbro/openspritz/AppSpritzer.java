package pro.dbro.openspritz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pro.dbro.openspritz.events.NextChapterEvent;
import pro.dbro.openspritz.formats.EpubBook;
import pro.dbro.openspritz.formats.SpritzerBook;
import pro.dbro.openspritz.formats.UnsupportedFormatException;

/**
 * Parse an .epub into a Queue of words
 * and display them on a TextView at
 * a given WPM
 */
// TODO: Save epub title : chapter-word
// TODO: Save State for multiple books
public class AppSpritzer extends Spritzer {
    public static final boolean VERBOSE = true;

    private static final String PREFS = "espritz";
    private static final String PREF_URI = "uri";
    private static final String PREF_TITLE = "title";
    private static final String PREF_CHAPTER = "chapter";
    private static final String PREF_WORD = "word";
    private static final String PREF_WPM = "wpm";

    private int mChapter;

    private SpritzerBook mBook;

    private Uri mEpubUri;

    public AppSpritzer(TextView target) {
        super(target);
        restoreState(true);
    }

    public AppSpritzer(TextView target, Uri epubPath) {
        super(target);
        openEpub(epubPath);
        mTarget.setText(mTarget.getContext().getString(R.string.touch_to_start));
    }

    public void setEpubPath(Uri epubPath) {
        pause();
        openEpub(epubPath);
        mTarget.setText(mTarget.getContext().getString(R.string.touch_to_start));
    }

    private void openEpub(Uri epubPath) {
        try {
            mChapter = 0;
            mBook = EpubBook.fromUri(mTarget.getContext(), epubPath);
            mEpubUri = epubPath;
            restoreState(false);
        } catch (UnsupportedFormatException e) {
            reportFileUnsupported();
        }
    }

    public SpritzerBook getBook() {
        return mBook;
    }

    public void printChapter(int chapter) {
        mChapter = chapter;
        setText(loadCleanStringFromChapter(mChapter));
        saveState();
    }

    public int getCurrentChapter() {
        return mChapter;
    }

    public int getMaxChapter() {
        return mBook.countChapters();
    }

    public boolean isBookSelected() {
        return mBook != null;
    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (mPlaying && mPlayingRequested && mWordQueue.isEmpty() && (mChapter + 1 < getMaxChapter())) {
            printNextChapter();
            if (mBus != null) {
                mBus.post(new NextChapterEvent(mChapter));
            }
        }
    }

    private void printNextChapter() {
        setText(loadCleanStringFromChapter(mChapter++));
        saveState();
        if (VERBOSE) Log.i(TAG, "starting next chapter: " + mChapter);
    }

    private String loadCleanStringFromChapter(int chapter) {
        return mBook.loadChapter(chapter);
    }

    public void saveState() {
        if (mBook != null) {
            if (VERBOSE) Log.i(TAG, "Saving state at chapter " + mChapter);
            SharedPreferences.Editor editor = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            editor.putInt(PREF_CHAPTER, mChapter)
                    .putString(PREF_URI, mEpubUri.toString())
                    .putInt(PREF_WORD, mWordArray.length - mWordQueue.size())
                    .putString(PREF_TITLE, mBook.getTitle())
                    .putInt(PREF_WPM, mWPM)
                    .apply();
        }
    }

    private void restoreState(boolean openLastEpubUri) {
        SharedPreferences prefs = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (openLastEpubUri) {
            if (prefs.contains(PREF_URI)) {
                Uri epubUri = Uri.parse(prefs.getString(PREF_URI, ""));
                if (Build.VERSION.SDK_INT >= 19) {
                    boolean uriPermissionPersisted = false;
                    List<UriPermission> uriPermissions = mTarget.getContext().getContentResolver().getPersistedUriPermissions();
                    for (UriPermission permission : uriPermissions) {
                        if (permission.getUri().equals(epubUri)) {
                            Log.i(TAG, "Found persisted url");
                            uriPermissionPersisted = true;
                            openEpub(epubUri);
                            break;
                        }
                    }
                    if (!uriPermissionPersisted) {
                        Log.w(TAG, String.format("Permission not persisted for uri: %s. Clearing SharedPreferences " + epubUri.toString()));
                        prefs.edit().clear().apply();
                        return;
                    }
                } else {
                    openEpub(epubUri);
                }
            }
        } else if (prefs.contains(PREF_TITLE) && mBook.getTitle().compareTo(prefs.getString(PREF_TITLE, "")) == 0) {
            mChapter = prefs.getInt(PREF_CHAPTER, 0);
            if (VERBOSE) Log.i(TAG, "Resuming " + mBook.getTitle() + " from chapter " + mChapter);
            setText(loadCleanStringFromChapter(mChapter));
            int oldSize = prefs.getInt(PREF_WORD, 0);
            setWpm(prefs.getInt(PREF_WPM, 500));
            while (mWordQueue.size() > oldSize) {
                mWordQueue.remove();
            }
        } else {
            mChapter = 0;
            setText(loadCleanStringFromChapter(mChapter));
        }
        if (!mPlaying) {
            mTarget.setText(mTarget.getContext().getString(R.string.touch_to_start));
        }
    }

    private void reportFileUnsupported() {
        Toast.makeText(mTarget.getContext(), mTarget.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
    }

}
