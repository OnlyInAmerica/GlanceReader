package pro.dbro.openspritz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import java.util.List;

import de.jetwick.snacktory.JResult;
import pro.dbro.openspritz.events.HttpUrlParsedEvent;
import pro.dbro.openspritz.events.NextChapterEvent;
import pro.dbro.openspritz.formats.Epub;
import pro.dbro.openspritz.formats.HtmlPage;
import pro.dbro.openspritz.formats.SpritzerMedia;
import pro.dbro.openspritz.formats.UnsupportedFormatException;
import pro.dbro.openspritz.lib.Spritzer;

/**
 * Parse a SpritzerMedia instance into a Queue of words
 * and display them on a TextView at
 * a given WPM
 */
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
    private SpritzerMedia mMedia;
    private Uri mMediaUri;

    public AppSpritzer(Bus bus, TextView target) {
        super(target);
        setEventBus(bus);
        restoreState(true);
    }

    public AppSpritzer(Bus bus, TextView target, Uri mediaUri) {
        super(target);
        setEventBus(bus);
        openMedia(mediaUri);
        mTarget.setText(mTarget.getContext().getString(R.string.touch_to_start));
    }

    public void setMediaUri(Uri uri) {
        pause();
        openMedia(uri);
        mTarget.setText(mTarget.getContext().getString(R.string.touch_to_start));
    }

    private void openMedia(Uri uri) {
        if (isHttpUri(uri)) {
            openHtmlPage(uri);
        } else {
            openEpub(uri);
        }
    }

    private void openEpub(Uri epubUri) {
        try {
            mChapter = 0;
            mMediaUri = epubUri;
            mMedia = Epub.fromUri(mTarget.getContext(), mMediaUri);
            restoreState(false);
        } catch (UnsupportedFormatException e) {
            reportFileUnsupported();
        }
    }

    private void openHtmlPage(Uri htmlUri) {
        try {
            mChapter = 0;
            mMediaUri = htmlUri;
            mMedia = HtmlPage.fromUri(htmlUri.toString(), new HtmlPage.HtmlPageParsedCallback() {
                @Override
                public void onPageParsed(JResult result) {
                    restoreState(false);
                    if (mBus != null) {
                        mBus.post(new HttpUrlParsedEvent(result));
                    }
                }
            });
        } catch (UnsupportedFormatException e) {
            reportFileUnsupported();
        }
    }

    public SpritzerMedia getMedia() {
        return mMedia;
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
        return mMedia.countChapters() - 1;
    }

    public boolean isMediaSelected() {
        return mMedia != null;
    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (mPlaying && mPlayingRequested && isWordListComplete() && mChapter < getMaxChapter()) {
            while (isWordListComplete()) {
                printNextChapter();
                if (mBus != null) {
                    mBus.post(new NextChapterEvent(mChapter));
                }
            }
        }
    }

    private void printNextChapter() {
        setText(loadCleanStringFromChapter(mChapter++));
        saveState();
        if (VERBOSE)
            Log.i(TAG, "starting next chapter: " + mChapter + " length " + mDisplayWordList.size());
    }

    private String loadCleanStringFromChapter(int chapter) {
        return mMedia.loadChapter(chapter);
    }

    public void saveState() {
        if (mMedia != null) {
            if (VERBOSE) Log.i(TAG, "Saving state at chapter " + mChapter);
            SharedPreferences.Editor editor = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            editor.putInt(PREF_CHAPTER, mChapter)
                    .putString(PREF_URI, mMediaUri.toString())
                    .putInt(PREF_WORD, mCurWordIdx)
                    .putString(PREF_TITLE, mMedia.getTitle())
                    .putInt(PREF_WPM, mWPM)
                    .apply();
        }
    }

    @SuppressLint("NewApi")
    private void restoreState(boolean openLastMediaUri) {
        SharedPreferences prefs = mTarget.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (openLastMediaUri) {
            if (prefs.contains(PREF_URI)) {
                Uri mediaUri = Uri.parse(prefs.getString(PREF_URI, ""));
                if (Build.VERSION.SDK_INT >= 19 && !isHttpUri(mediaUri)) {
                    boolean uriPermissionPersisted = false;
                     List<UriPermission> uriPermissions = mTarget.getContext().getContentResolver().getPersistedUriPermissions();
                    for (UriPermission permission : uriPermissions) {
                        if (permission.getUri().equals(mediaUri)) {
                            Log.i(TAG, "Found persisted url");
                            uriPermissionPersisted = true;
                            openMedia(mediaUri);
                            break;
                        }
                    }
                    if (!uriPermissionPersisted) {
                        Log.w(TAG, String.format("Permission not persisted for uri: %s. Clearing SharedPreferences ", mediaUri.toString()));
                        prefs.edit().clear().apply();
                        return;
                    }
                } else {
                    openMedia(mediaUri);
                }
            }
        } else if (prefs.contains(PREF_TITLE) && mMedia.getTitle().compareTo(prefs.getString(PREF_TITLE, "")) == 0) {
            mChapter = prefs.getInt(PREF_CHAPTER, 0);
            if (VERBOSE) Log.i(TAG, "Resuming " + mMedia.getTitle() + " from chapter " + mChapter);
            setText(loadCleanStringFromChapter(mChapter));
            setWpm(prefs.getInt(PREF_WPM, 500));
            mCurWordIdx = prefs.getInt(PREF_WORD, 0);
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

    public static boolean isHttpUri(Uri uri) {
        return uri.getScheme() != null && uri.getScheme().contains("http");
    }

}
