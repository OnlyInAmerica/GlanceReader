package pro.dbro.glance.events;

import de.jetwick.snacktory.JResult;

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
