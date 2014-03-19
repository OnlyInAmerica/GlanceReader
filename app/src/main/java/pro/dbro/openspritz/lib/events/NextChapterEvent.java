package pro.dbro.openspritz.lib.events;

/**
 * Created by davidbrodsky on 3/5/14.
 */
public class NextChapterEvent {

    private final int mChapter;

    public NextChapterEvent(int chapter){
        mChapter = chapter;
    }

    public int getChapter(){
        return mChapter;
    }
}
