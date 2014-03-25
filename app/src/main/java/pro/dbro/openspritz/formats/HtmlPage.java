package pro.dbro.openspritz.formats;

import android.os.AsyncTask;
import android.util.Log;

import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

/**
 * This provides an implementation of {@link pro.dbro.openspritz.formats.SpritzerMedia}
 * that serves a web page
 *
 * @author defer (diogo@underdev.org)
 */
public class HtmlPage implements SpritzerMedia {
    public static final boolean VERBOSE = true;
    /**
     * The logging tag.
     */
    private static final String TAG = "HtmlPage";

    /**
     * The JResult from snacktory's HTMLFetcher
     */
    private JResult mResult;


    /**
     * Builds an HtmlPage from a {@link de.jetwick.snacktory.JResult}
     *
     * @param result The {@link de.jetwick.snacktory.JResult} to display
     */
    private HtmlPage(JResult result) {
        mResult = result;
    }

    public void setResult(JResult result) {
        mResult = result;
    }

    /**
     * Creates an {@link pro.dbro.openspritz.formats.HtmlPage} from a context and URI.
     *
     * @param url The http url.
     * @param cb  A callback to be invoked when the HtmlPage is parsed
     * @return An HtmlPage with null JResult;
     * @throws pro.dbro.openspritz.formats.UnsupportedFormatException if HTML parsing fails
     */
    public static HtmlPage fromUri(String url, final HtmlPageParsedCallback cb) throws UnsupportedFormatException {
        final HtmlPage page = new HtmlPage(null);
        new AsyncTask<String, Void, JResult>() {

            @Override
            protected JResult doInBackground(String... url) {
                try {
                    HtmlFetcher fetcher = new HtmlFetcher();
                    // set cache. e.g. take the map implementation from google collections:
//                    fetcher.setCache((de.jetwick.snacktory.SCache) CacheBuilder.newBuilder()
//                            .maximumSize(3)
//                            .expireAfterWrite(1, TimeUnit.HOURS)
//                            .build());

                    if (VERBOSE) Log.i(TAG, "Fetching " + url[0]);
                    JResult result = fetcher.fetchAndExtract(url[0], 10 * 1000, true);
                    if (result == null || result.getText().length() < 1) {
                        throw new UnsupportedFormatException("Failed to parse text from " + url);
                    }
                    page.setResult(result);
                    return result;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JResult result) {
                if (cb != null) {
                    cb.onPageParsed(result);
                }
            }
        }.execute(url);
        return page;
    }


    @Override
    public String getTitle() {
        return (mResult == null || mResult.getTitle() == null) ? "" : mResult.getTitle();
    }

    @Override
    public String getAuthor() {
        return (mResult == null || mResult.getUrl() == null) ? "" : mResult.getUrl();
    }

    @Override
    public String loadChapter(int ignored) {
        return (mResult == null || mResult.getText() == null) ? "" : mResult.getText();
    }

    @Override
    public String getChapterTitle(int ignored) {
        return "";
    }


    @Override
    public int countChapters() {
        return 1;
    }

    public static interface HtmlPageParsedCallback {
        public void onPageParsed(JResult result);
    }

}
