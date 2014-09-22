package pro.dbro.glance;

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
import pro.dbro.glance.events.HttpUrlParsedEvent;
import pro.dbro.glance.events.NextChapterEvent;
import pro.dbro.glance.formats.Epub;
import pro.dbro.glance.formats.HtmlPage;
import pro.dbro.glance.formats.SpritzerMedia;
import pro.dbro.glance.formats.UnsupportedFormatException;
import pro.dbro.glance.lib.Spritzer;

/**
 * A higher-level {@link pro.dbro.glance.lib.Spritzer} that operates
 * on Uris pointing to .epubs on disk or http urls, instead
 * of a plain String
 */
// TODO: Save State for multiple books
public class AppSpritzer extends Spritzer {
    public static final boolean VERBOSE = true;
    public static final int SPECIAL_MESSAGE_WPM = 100;

    private int mChapter;
    private SpritzerMedia mMedia;
    private Uri mMediaUri;
    private boolean mSpritzingSpecialMessage;

    public AppSpritzer(Bus bus, TextView target) {
        super(target);
        setEventBus(bus);
        restoreState(true);
    }

    public AppSpritzer(Bus bus, TextView target, Uri mediaUri) {
        super(target);
        setEventBus(bus);
        openMedia(mediaUri);
    }

    public void setMediaUri(Uri uri) {
        pause();
        openMedia(uri);
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
            mMediaUri = epubUri;
            mMedia = Epub.fromUri(mTarget.getContext(), mMediaUri);
            restoreState(false);
        } catch (UnsupportedFormatException e) {
            reportFileUnsupported();
        }
    }

    private void openHtmlPage(Uri htmlUri) {
        try {
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

    public boolean isSpritzingSpecialMessage() {
        return mSpritzingSpecialMessage;
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
        return (mMedia == null) ? 0 : mMedia.countChapters() - 1;
    }

    public boolean isMediaSelected() {
        return mMedia != null;
    }

    protected void processNextWord() throws InterruptedException {
        super.processNextWord();
        if (mPlaying && mPlayingRequested && isWordListComplete() && mChapter < getMaxChapter()) {
            // If we are Spritzing a special message, don't automatically proceed to the next chapter
            if (mSpritzingSpecialMessage) {
                mSpritzingSpecialMessage = false;
                return;
            }
            while (isWordListComplete() && mChapter < getMaxChapter()) {
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

    /**
     * Load the given chapter as sanitized text, proceeding
     * to the next chapter until a non-zero length result is found.
     *
     * This method is useful because some "Chapters" contain only HTML data
     * that isn't useful to a Spritzer.
     *
     * @param chapter the first chapter to load
     * @return the sanitized text of the first non-zero length chapter
     */
    private String loadCleanStringFromNextNonEmptyChapter(int chapter) {
        int chapterToTry = chapter;
        String result = "";
        while(result.length() == 0 && chapterToTry <= getMaxChapter()) {
            result = loadCleanStringFromChapter(chapterToTry);
            chapterToTry++;
        }
        return result;
    }

    /**
     * Load the given chapter as sanitized text.
     *
     * @param chapter the target chapter.
     * @return the sanitized chapter text.
     */
    private String loadCleanStringFromChapter(int chapter) {
        return mMedia.loadChapter(chapter);
    }

    public void saveState() {
        if (mMedia != null) {
            if (VERBOSE) Log.i(TAG, "Saving state at chapter " + mChapter + " word: " + mCurWordIdx);
            PrefsManager.saveState(
                    mTarget.getContext(),
                    mChapter,
                    mMediaUri.toString(),
                    mCurWordIdx,
                    mMedia.getTitle(),
                    mWPM);
        }
    }

    @SuppressLint("NewApi")
    private void restoreState(boolean openLastMediaUri) {
        PrefsManager.SpritzState state = PrefsManager.getState(mTarget.getContext());
        String content = "";
        if (openLastMediaUri) {
            // Open the last selected media
            if (state.hasUri()) {
                Uri mediaUri = state.getUri();
                if (Build.VERSION.SDK_INT >= 19 && !isHttpUri(mediaUri)) {
                    boolean uriPermissionPersisted = false;
                    List<UriPermission> uriPermissions = mTarget.getContext().getContentResolver().getPersistedUriPermissions();
                    for (UriPermission permission : uriPermissions) {
                        if (permission.getUri().equals(mediaUri)) {
                            uriPermissionPersisted = true;
                            openMedia(mediaUri);
                            break;
                        }
                    }
                    if (!uriPermissionPersisted) {
                        Log.w(TAG, String.format("Permission not persisted for uri: %s. Clearing SharedPreferences ", mediaUri.toString()));
                        PrefsManager.clearState(mTarget.getContext());
                        return;
                    }
                } else {
                    openMedia(mediaUri);
                }
            }
        } else if (state.hasTitle() && mMedia.getTitle().compareTo(state.getTitle()) == 0) {
            // Resume media at previous point
            mChapter = state.getChapter();
            content = loadCleanStringFromNextNonEmptyChapter(mChapter);
            setWpm(state.getWpm());
            mCurWordIdx = state.getWordIdx();
            if (VERBOSE) Log.i(TAG, "Resuming " + mMedia.getTitle() + " from chapter " + mChapter + " word " + mCurWordIdx);
        } else {
            // Begin content anew
            mChapter = 0;
            mCurWordIdx = 0;
            setWpm(state.getWpm());
            content = loadCleanStringFromNextNonEmptyChapter(mChapter);
        }
        final String finalContent = content;
        if (!mPlaying && finalContent.length() > 0) {
            final int initialWpm = getWpm();
            setWpm(SPECIAL_MESSAGE_WPM);
            // Set mSpritzingSpecialMessage to true, so processNextWord doesn't
            // automatically proceed to the next chapter
            mSpritzingSpecialMessage = true;
            mTarget.setEnabled(false);
            setTextAndStart(mTarget.getContext().getString(R.string.touch_to_start), new SpritzerCallback() {
                @Override
                public void onSpritzerFinished() {
                    setText(finalContent);
                    setWpm(initialWpm);
                    mSpritzHandler.sendMessage(mSpritzHandler.obtainMessage(MSG_SET_ENABLED));
                }
            }, false);
        }
    }

    private void reportFileUnsupported() {
        Toast.makeText(mTarget.getContext(), mTarget.getContext().getString(R.string.unsupported_file), Toast.LENGTH_LONG).show();
    }

    public static boolean isHttpUri(Uri uri) {
        return uri.getScheme() != null && uri.getScheme().contains("http");
    }

    /**
     * Return a String representing the maxChars most recently
     * Spritzed characters.
     *
     * @param maxChars The max number of characters to return. Pass a value less than 1 for no limit.
     * @return The maxChars number of most recently spritzed characters during this segment
     */
    public String getHistoryString(int maxChars) {
        if (maxChars <= 0) maxChars = Integer.MAX_VALUE;
        if (mCurWordIdx < 2 || mDisplayWordList.size() < 2) return "";
        StringBuilder builder = new StringBuilder();
        int numWords = 0;
        while (builder.length() + mDisplayWordList.get(mCurWordIdx - (numWords + 2)).length() < maxChars) {
            builder.insert(0, mDisplayWordList.get(mCurWordIdx - (numWords + 2)) + " ");
            numWords++;
            if (mCurWordIdx - (numWords + 2) < 0) break;
        }
        return builder.toString();
    }

}
