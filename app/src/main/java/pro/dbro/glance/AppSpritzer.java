package pro.dbro.glance;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.UriPermission;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.otto.Bus;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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

    private Context application;

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

    public void setApplicationContect(Context ctx) {
        application = ctx;
    }

    public void setMediaUri(Uri uri) {
        pause();
        openMedia(uri);
    }

    private void openMedia(Uri uri) {
        if (isHttpUri(uri)) {
            if (isRemoteEpub(uri)){
                openRemoteEpub(uri);
            } else {
                openHtmlPage(uri);
            }
        } else {
            openEpub(uri);
        }
    }

    private void initParse() {
        Parse.initialize(application, "IKXOwtsEGwpJxjD56rloizwwsB4pijEve8nU5wkB", "8K0yHwwEevmCiuuHTjGj7HRhFTzHmycBXXspmnPU");
        Parse.enableLocalDatastore(application);
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

    private void openRemoteEpub(Uri epubUri) {

        new EpubDownloadManager(application).execute(epubUri.toString(), "filename");

    }

    private void openHtmlPage(Uri htmlUri) {
        try {
            mMediaUri = htmlUri;
            mMedia = HtmlPage.fromUri(mTarget.getContext().getApplicationContext(), htmlUri.toString(), new HtmlPage.HtmlPageParsedCallback() {
                @Override
                public void onPageParsed(HtmlPage result) {
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
     * <p/>
     * This method is useful because some "Chapters" contain only HTML data
     * that isn't useful to a Spritzer.
     *
     * @param chapter the first chapter to load
     * @return the sanitized text of the first non-zero length chapter
     */
    private String loadCleanStringFromNextNonEmptyChapter(int chapter) {
        int chapterToTry = chapter;
        String result = "";
        while (result.length() == 0 && chapterToTry <= getMaxChapter()) {
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
            if (VERBOSE)
                Log.i(TAG, "Saving state at chapter " + mChapter + " word: " + mCurWordIdx);
            GlancePrefsManager.saveState(
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
        final GlancePrefsManager.SpritzState state = GlancePrefsManager.getState(mTarget.getContext());
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
                        GlancePrefsManager.clearState(mTarget.getContext());
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
            if (VERBOSE)
                Log.i(TAG, "Resuming " + mMedia.getTitle() + " from chapter " + mChapter + " word " + mCurWordIdx);
        } else {
            // Begin content anew
            mChapter = 0;
            mCurWordIdx = 0;
            setWpm(state.getWpm());
            content = loadCleanStringFromNextNonEmptyChapter(mChapter);
        }
        final String finalContent = content;
        if (!mPlaying && finalContent.length() > 0) {
            setWpm(SPECIAL_MESSAGE_WPM);
            // Set mSpritzingSpecialMessage to true, so processNextWord doesn't
            // automatically proceed to the next chapter
            mSpritzingSpecialMessage = true;
            mTarget.setEnabled(false);
            setTextAndStart(mTarget.getContext().getString(R.string.touch_to_start), new SpritzerCallback() {
                @Override
                public void onSpritzerFinished() {
                    setText(finalContent);
                    setWpm(state.getWpm());
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

    public static boolean isRemoteEpub(Uri uri) {
        return uri.getScheme() != null && uri.getScheme().contains("http") && uri.toString().contains(".epub");
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

    class EpubDownloadManager extends AsyncTask<String, Integer, Drawable> {

        private Drawable d;
        private HttpURLConnection conn;
        private InputStream stream; //to read
        private ByteArrayOutputStream out; //to write
        private Context mCtx;

        private double fileSize;
        private double downloaded; // number of bytes downloaded
        private int status = DOWNLOADING; //status of current process

        private ProgressDialog progressDialog;

        private static final int MAX_BUFFER_SIZE = 1024; //1kb
        private static final int DOWNLOADING = 0;
        private static final int COMPLETE = 1;

        public EpubDownloadManager(Context ctx) {
            d = null;
            conn = null;
            fileSize = 0;
            downloaded = 0;
            status = DOWNLOADING;
            mCtx = ctx;
        }

        public boolean isOnline() {
            try {
                ConnectivityManager cm = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
                return cm.getActiveNetworkInfo().isConnectedOrConnecting();
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected Drawable doInBackground(String... url) {
            try {
                String filename = url[1];
                if (isOnline()) {
                    conn = (HttpURLConnection) new URL(url[0]).openConnection();
                    fileSize = conn.getContentLength();
                    out = new ByteArrayOutputStream((int) fileSize);
                    conn.connect();

                    stream = conn.getInputStream();
                    // loop with step
                    while (status == DOWNLOADING) {
                        byte buffer[];

                        if (fileSize - downloaded > MAX_BUFFER_SIZE) {
                            buffer = new byte[MAX_BUFFER_SIZE];
                        } else {
                            buffer = new byte[(int) (fileSize - downloaded)];
                        }
                        int read = stream.read(buffer);

                        if (read == -1) {
                            publishProgress(100);
                            break;
                        }
                        // writing to buffer
                        out.write(buffer, 0, read);
                        downloaded += read;
                        // update progress bar
                        publishProgress((int) ((downloaded / fileSize) * 100));
                    } // end of while

                    if (status == DOWNLOADING) {
                        status = COMPLETE;
                    }
                    try {
                        FileOutputStream fos = new FileOutputStream(filename);
                        fos.write(out.toByteArray());
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }

                    //d = Drawable.createFromStream((InputStream) new ByteArrayInputStream(out.toByteArray()), "filename");
                    return d;
                } // end of if isOnline
                else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }// end of catch
        } // end of class DownloadManager()

        @Override
        protected void onProgressUpdate(Integer... changed) {
            //progressDialog.setProgress(changed[0]);
            setText("Downloading (" + changed[0].toString() + "%)..");
        }

        @Override
        protected void onPreExecute() {
          setText("Downloading..");
//        progressDialog = new ProgressDialog(); // your activity
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setMessage("Downloading ...");
//        progressDialog.setCancelable(false);
//        progressDialog.show();

        }

        @Override
        protected void onPostExecute(Drawable result) {
            //progressDialog.dismiss();
            // do something

//        try {
//            mMediaUri = epubUri;
//            mMedia = Epub.fromUri(mTarget.getContext(), mMediaUri);
//            restoreState(false);
//        } catch (UnsupportedFormatException e) {
//            reportFileUnsupported();
//        }

        }
    }
}