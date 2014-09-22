package pro.dbro.glance.formats;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

/**
 * This provides an implementation of {@link pro.dbro.glance.formats.SpritzerMedia}
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
     * Creates an {@link pro.dbro.glance.formats.HtmlPage} from a url.
     * Returns immediately with an {@link pro.dbro.glance.formats.HtmlPage}
     * that is not yet initialized. Pass a {@link pro.dbro.glance.formats.HtmlPage.HtmlPageParsedCallback}
     * to be notified when page parsing is complete, and the returned HtmlPage is populated.
     *
     * @param url The http url.
     * @param cb  A callback to be invoked when the HtmlPage is parsed
     * @return An HtmlPage with null JResult;
     * @throws pro.dbro.glance.formats.UnsupportedFormatException if HTML parsing fails
     */
    public static HtmlPage fromUri(String url, final HtmlPageParsedCallback cb) throws UnsupportedFormatException {
        final HtmlPage page = new HtmlPage(null);
        new AsyncTask<String, Void, JResult>() {

            public void recordRead(final String url, final String title){

                // Okay, so this is really shitty.
                // I know.
                // Here's the thing: I didn't know Parse can't do DISTINCT or GROUP BY.
                // Now I do.
                // Anyway, instead we're just incrementing a counter.

                ParseQuery<ParseObject> query = ParseQuery.getQuery("Article");
                query.whereEqualTo("url", url);
                query.whereEqualTo("title", title);
                query.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> scoreList, ParseException e) {
                        if (e == null) {
                            Log.d("score", "Retrieved " + scoreList.size() + " scores");

                           if(scoreList.isEmpty()){
                               // Don't have the object, create it.
                               ParseObject article = new ParseObject("Article");
                               article.put("url", url);
                               article.put("title", title);
                               article.put("reads", 1);
                               article.saveInBackground();
                               return;
                            } else {
                               // Update object if we already have it.
                               ParseObject article = scoreList.get(0);
                               article.increment("reads");
                               article.saveInBackground();
                           }
                        } else {
                            Log.d("score", "Error: " + e.getMessage());
                        }
                    }
                });

            }

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

                    recordRead(result.getUrl(), result.getTitle());
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
        try {
            return (mResult == null || mResult.getUrl() == null) ? "" : new URL(mResult.getUrl()).getHost();
        } catch (MalformedURLException e) {
            return "";
        }
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
