package pro.dbro.glance.formats;

/**
 * Exception that is thrown whenever a book parser cannot
 * comply with the given format.
 *
 * @author defer (diogo@underdev.org)
 */
public class UnsupportedFormatException extends Exception {
    /**
     * Builds an {@link pro.dbro.glance.formats.UnsupportedFormatException} with
     * a given message and root cause.
     *
     * @param message The message.
     * @param rootCause The root cause.
     */
    public UnsupportedFormatException(String message, Exception rootCause) {
        super(message, rootCause);
    }

    /**
     * Builds an {@link pro.dbro.glance.formats.UnsupportedFormatException} with
     * a given message.
     *
     * @param message The message.
     */
    public UnsupportedFormatException(String message) {
        super(message);
    }
}
