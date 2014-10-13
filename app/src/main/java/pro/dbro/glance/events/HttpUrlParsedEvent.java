package pro.dbro.glance.events;

import de.jetwick.snacktory.JResult;
import pro.dbro.glance.formats.HtmlPage;

/**
 * Created by David Brodsky on 3/23/14.
 */
public class HttpUrlParsedEvent {

    private JResult mResult;

    public HttpUrlParsedEvent(JResult result) {
        mResult = result;
    }

    public JResult getResult() {
        return mResult;
    }

}
