package pro.dbro.glance.adapters;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import pro.dbro.glance.activities.MainActivity;

/**
 * Common functions
 * <p/>
 * Created by davidbrodsky on 9/12/14.
 */
public class AdapterUtils {

    /** Intent Keys */

    /**
     * Whether Activity should finish after
     * completing action specified in Intent
     */
    public static final String FINISH_AFTER = "FinishAfter";

    /**
     * Indicates this media is internal to the Glance network and NOT from an external source
     * e.g: Media shared from a web browser
     */
    public static final String IS_INTERNAL_MEDIA = "InternalMedia";

    public static View.OnClickListener getArticleClickListener(final Context c) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent communityIntent = new Intent(c, MainActivity.class);
                communityIntent.setAction(Intent.ACTION_SEND);
                communityIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                communityIntent.putExtra(IS_INTERNAL_MEDIA, true);
                communityIntent.putExtra(FINISH_AFTER, true);
                c.startActivity(communityIntent);
            }
        };
    }

    // XXX: If don't have book:
    //      Start book download
    //      Add to local library
    //      Open book
    //  If have book:
    //      Open book
    public static View.OnClickListener getBookClickListener(final Context c) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(c, MainActivity.class);
                bookIntent.setAction(Intent.ACTION_SEND);
                bookIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                bookIntent.putExtra(IS_INTERNAL_MEDIA, true);
                bookIntent.putExtra(FINISH_AFTER, true);
                c.startActivity(bookIntent);

                //new DownloadManager(c).execute((String) view.getTag(), (String) view.getTag());

            }

        };

    }

}