package pro.dbro.glance.events;

import pro.dbro.glance.formats.HtmlPage;

/**
 * Created by David Brodsky on 3/23/14.
 */
public class HttpUrlParsedEvent {

    private HtmlPage mResult;
    private boolean mSucess;

    /**
     * Create an event representing an webpage parsing event.
     * If successful, result is non-null, else null
     */
    public HttpUrlParsedEvent(HtmlPage result) {
        mResult = result;
        mSucess = result != null;
    }

    public HtmlPage getResult() {
        return mResult;
    }

    public boolean isSuccessful() {
        return mSucess;
    }

}
