package pro.dbro.glance.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

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

    public static View.OnClickListener getArticleClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent communityIntent = new Intent(context, MainActivity.class);
                communityIntent.setAction(Intent.ACTION_SEND);
                communityIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                communityIntent.putExtra(IS_INTERNAL_MEDIA, true);
                communityIntent.putExtra(FINISH_AFTER, true);
                context.startActivity(communityIntent);
            }
        };
    }

    public static View.OnLongClickListener getArticleLongClickListener() {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) view.getTag()));
                view.getContext().startActivity(browserIntent);
                return true;
            }
        };
    }

    // XXX: If don't have book:
    //      Start book download
    //      Add to local library
    //      Open book
    //  If have book:
    //      Open book
    public static View.OnClickListener getBookClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent bookIntent = new Intent(context, MainActivity.class);
                bookIntent.setAction(Intent.ACTION_SEND);
                bookIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                bookIntent.putExtra(IS_INTERNAL_MEDIA, true);
                bookIntent.putExtra(FINISH_AFTER, true);
                context.startActivity(bookIntent);

                //new DownloadManager(c).execute((String) view.getTag(), (String) view.getTag());

            }

        };

    }

}