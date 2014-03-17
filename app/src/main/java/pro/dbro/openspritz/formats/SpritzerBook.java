package pro.dbro.openspritz.formats;

import java.io.Serializable;

/**
 * SpritzerBook provides an interface that abstracts different
 * book types.
 *
 * @author defer (diogo@underdev.org)
 */
public interface SpritzerBook extends Serializable {
    /**
     * Obtains the book title.
     * @return The book title, or {@code ""} if it is not available.
     */
    String getTitle();

    /**
     * Obtains the book author.
     * @return The book author, or {@code ""} if it is not available.
     */
    String getAuthor();

    /**
     * Obtains the title for a given chapter.
     * @param chapterNumber The chapter number.
     * @return The chapter title, or {@code null} if it is not available.
     */
    String getChapterTitle(int chapterNumber);

    /**
     * Obtains the plain text for a given chapter number.
     *
     * @return The plain text for the chapter, or {@code null} if it is not available.
     * @throws java.lang.IllegalArgumentException If the chapter number is out of bounds.
     */
    String loadChapter(int chapterNumber);

    /**
     * Obtains the number of chapters in the book.
     *
     * @return An integer containing the number of chapters.
     */
    int countChapters();
}
