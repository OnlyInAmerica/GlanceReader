package pro.dbro.glance;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import pro.dbro.glance.activities.MainActivity;

/**
 * Common functions
 *
 * Created by davidbrodsky on 9/12/14.
 */
public class Utils {

    /** Intent key to specify whether Activity should finish after
     * completing action specified in Intent
     */
    public static final String INTENT_FINISH_AFTER = "FinishAfter";

    public static View.OnClickListener getArticleClickListener(final Context c) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent communityIntent = new Intent(c, MainActivity.class);
                communityIntent.setAction(Intent.ACTION_SEND);
                communityIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                communityIntent.putExtra(INTENT_FINISH_AFTER, true);
                c.startActivity(communityIntent);
            }
        };
    }
}
