package pro.dbro.glance.events;

import pro.dbro.glance.formats.HtmlPage;

/**
 * Created by David Brodsky on 3/23/14.
 */
public class HttpUrlParsedEvent {

    private HtmlPage mResult;

    public HttpUrlParsedEvent(HtmlPage result) {
        mResult = result;
    }

    public HtmlPage getResult() {
        return mResult;
    }

}
