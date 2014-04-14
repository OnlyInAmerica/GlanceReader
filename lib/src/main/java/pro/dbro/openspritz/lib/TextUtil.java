package pro.dbro.openspritz.lib;

import android.widget.TextView;

/**
 * Created by David Brodsky on 4/13/14.
 */
public class TextUtil {

    private static String sTestString = "a";        // Avoid re-allocating per call

    /**
     * Calculate the number of characteres this TextView can display
     * without ellipsizing.
     * <p/>
     * Assumes the TextView uses a monospace font
     *
     * @param tv the TextView in question. Must already have been layed out.
     * @return
     */
    public static int calculateMonospacedCharacterLimit(TextView tv) {
        return calculateMonospacedCharacterLimit(tv, tv.getLineCount());
    }

    /**
     * Calculate the number of characteres this TextView can display
     * without ellipsizing when limited to the given number of lines.
     * <p/>
     * Assumes the TextView uses a monospace font
     *
     * @param lineCount the number of lines to allow the TextView
     * @param tv        the TextView in question. Must already have been layed out.
     * @return
     */
    public static int calculateMonospacedCharacterLimit(TextView tv, int lineCount) {
        int maxChars = Math.round(tv.getWidth() / calculateLengthOfPrintedMonospaceCharacters(tv, 1));
        return maxChars * lineCount;
    }

    /**
     * Calculate the length in pixels of the given number of characters
     * printed in monospace on the given TextView
     *
     * @param tv            The target TextView
     * @param numCharacters the target number of monospace characters to measure
     * @return
     */
    public static int calculateLengthOfPrintedMonospaceCharacters(TextView tv, float numCharacters) {
        // Choice of character is irrelevant given monospace assumption
        // If we abandoned this assumption, we'd have to take the target text as input
        return (int) (tv.getPaint().measureText("a", 0, 1) * numCharacters);
    }
}
