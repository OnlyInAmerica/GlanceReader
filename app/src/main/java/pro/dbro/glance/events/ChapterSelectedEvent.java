package pro.dbro.glance.events;

/**
 * Event that is fired whenever a chapter is selected.
 *
 * @author defer (diogo@underdev.org)
 */
public class ChapterSelectedEvent {
    private final int mChapter;

    public ChapterSelectedEvent(int chapter) {
        this.mChapter = chapter;
    }

    public int getChapter() {
        return this.mChapter;
    }
}
