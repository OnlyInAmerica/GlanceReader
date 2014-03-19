package pro.dbro.openspritz.lib.events;

/**
 * Event that is fired whenever the user requests to change
 * the WPM rate.
 */
public class WpmSelectedEvent {
    private final int mWpm;

    public WpmSelectedEvent(int wpm) {
        this.mWpm = wpm;
    }

    public int getWpm() {
        return this.mWpm;
    }
}
