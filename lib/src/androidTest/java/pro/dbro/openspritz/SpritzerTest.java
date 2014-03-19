package pro.dbro.openspritz;

import android.test.AndroidTestCase;
import android.widget.TextView;

import pro.dbro.openspritz.lib.Spritzer;


/**
 * Created by Andrew on 3/11/14.
 */
public class SpritzerTest extends AndroidTestCase {
    public static final String SEPARATOR = ":";

    /**
     * Note: MAX_WORD_LENGTH  = 13
     *
     * Test cases are in this format:
     *    "WordToTest"+ SEPARATOR +
     *     "FirstHalfWord"+ SEPARATOR +
     *     "SecondHalfWord",
     */
    public static String[] splitWordTests = {


            /**
             * Test a long word > MAX_WORD_LENGTH && word < MAX_WORD_LENGTH * 2
             *
             * Split index should be the word.length/2
             */
            "abcdefghijklmnopqrstuv" + SEPARATOR +
                    "abcdefghijk-" + SEPARATOR +
                    "lmnopqrstuv",

            /**
             * Test a word > MAX_WORD_LENGTH with a hyphen
             *
             * Split index should be after the hypen
             */
            "hyperactive-monkey" + SEPARATOR +
                    "hyperactive-" + SEPARATOR +
                    "monkey",

            /**
             * Test a word with length > 13 and has a period
             *
             * Split index should be after the period
             */
            "abcdefghijk.lmnopqrstuv" + SEPARATOR +
                    "abcdefghijk." + SEPARATOR +
                    "lmnopqrstuv",

            /**
             * Test a word longer than 26 (MAX_CHARS_LENGTH *2)
             *
             * Split index should be MAX_WORD_LENGTH - 1 so we can add a hypen to make it 13
             */
            "abcdefghijklmnopqrstuvwxyz0" + SEPARATOR +
                    "abcdefghijkl-" + SEPARATOR +
                    "mnopqrstuvwxyz0"
    };
    private Spritzer spritzer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        spritzer = new Spritzer(new TextView(getContext()));
    }


    public void testSplitWordList() {
        for (String tests : splitWordTests) {
            final String[] split = tests.split(":");
            assertEquals(3, split.length);
        }
    }

    public void testSplitWords() {
        for (String tests : splitWordTests) {
            final String[] arry = tests.split(":");
            String longword = arry[0];
            String expectedSplit = arry[1];
            String expectedAddedToQueue = arry[2];
            assertEquals(expectedSplit, spritzer.splitLongWord(longword));
            assertEquals(expectedAddedToQueue, spritzer.mWordQueue.peek());
            spritzer.mWordQueue.clear();
        }

    }
}
