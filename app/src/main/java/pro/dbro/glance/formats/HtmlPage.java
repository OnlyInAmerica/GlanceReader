package pro.dbro.glance.formats;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import pro.dbro.glance.SECRETS;
import pro.dbro.glance.http.TrustManager;

/**
 * This provides an implementation of {@link pro.dbro.glance.formats.SpritzerMedia}
 * that serves a web page
 *
 * @author defer (diogo@underdev.org)
 */
public class HtmlPage implements SpritzerMedia {
    public static final boolean VERBOSE = true;

    private static boolean sSetupTrustManager = false;
    /**
     * The logging tag.
     */
    private static final String TAG = "HtmlPage";

    private String mTitle;
    private String mUrl;
    private String mContent;


    /**
     * Builds an HtmlPage from a {@link com.google.gson.JsonObject} in diffbot format.
     * See http://www.diffbot.com/products/automatic/
     *
     * @param result The {@link com.google.gson.JsonObject} to display
     */
    private HtmlPage(JsonObject result) {
        if (result != null)
            initFromJson(result);
    }

    public void setResult(JsonObject result) {
        initFromJson(result);
    }

    private void initFromJson(JsonObject json) {
        // Diffbot json format
        // see http://www.diffbot.com/products/automatic/
        if (json == null) {
            Log.e(TAG, "Error parsing page");
            return;
        }
        mTitle   =  json.get("title").getAsString();
        mUrl     =  json.get("url").getAsString();
        mContent =  json.get("text").getAsString();

        // Sanitize content
        mContent = Html.fromHtml(mContent).toString().replaceAll("\\n+", " ").replaceAll("(?s)<!--.*?-->", "");
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
    public static HtmlPage fromUri(final Context context, String url, final HtmlPageParsedCallback cb) throws UnsupportedFormatException {
    // Seems to be a bug in Ion setting trust manager
    // When that's resolved, go back to Ion request
//        if (!sSetupTrustManager) {
//            sSetupTrustManager = TrustManager.setupIonTrustManager(context);
//        }
        final HtmlPage page = new HtmlPage(null);
        String encodedUrlToParse = Uri.encode(url);
        String requestUrl = String.format("http://api.diffbot.com/v2/article?url=%s&token=%s", encodedUrlToParse, SECRETS.getDiffbotKey());
        Log.i(TAG, "Loading url: " + requestUrl);
//        TrustManager.makeTrustRequest(context, requestUrl, new TrustManager.TrustRequestCallback() {
//            @Override
//            public void onSuccess(JsonObject result) {
//                page.setResult(result);
//                recordRead(page);
//
//                if (cb != null) {
//                    cb.onPageParsed(page);
//
//                }
//            }
//        });
        Ion.getInstance(context, TrustManager.sIonInstanceName)
                .build(context)
                .load(requestUrl)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace();
                            Log.e(TAG, "Unable to parse page");
                            return;
                        }
                        //Log.i(TAG, "Got diffbot result " + result.toString());
                        page.setResult(result);

                        if (cb != null) {
                            cb.onPageParsed(page);

                        }
                    }
                });

        return page;
    }


    public String getUrl() {
        return mUrl;
    }

    @Override
    public String getTitle() {
        return (mTitle == null) ? "" : mTitle;
    }

    @Override
    public String getAuthor() {
        try {
            if (mUrl != null)
                return new URL(mUrl).getHost();
            return "";
        } catch (MalformedURLException e) {
            return "";
        }
    }

    @Override
    public String loadChapter(int ignored) {
        return (mContent == null) ? "" : mContent;
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
        public void onPageParsed(HtmlPage result);
    }

}
