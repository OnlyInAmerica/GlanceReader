package pro.dbro.glance.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import pro.dbro.glance.activities.MainActivity;

/**
 * Common functions
 *
 * Created by davidbrodsky on 9/12/14.
 */
public class AdapterUtils {

    /** Intent Keys */

    /** Whether Activity should finish after
     * completing action specified in Intent
     */
    public static final String FINISH_AFTER = "FinishAfter";

    /** Indicates this media is internal to the Glance network and NOT from an external source
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
}
