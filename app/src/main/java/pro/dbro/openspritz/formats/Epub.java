package pro.dbro.openspritz.formats;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;
import pro.dbro.openspritz.FileUtils;

/**
 * This provides an implementation of {@link SpritzerMedia}
 * that serves chapters in the Epub format.
 * <p/>
 * The chapters are provided in a best-effort way: If a TOC is available, those are the
 * chapters. Otherwise, it will use the book spines.
 * <p/>
 * This class uses epublib to expose the required features.
 *
 * @author defer (diogo@underdev.org)
 */
public class Epub implements SpritzerMedia {
    /**
     * The logging tag.
     */
    private static final String TAG = "Epub";

    /**
     * The epublib book.
     */
    private final Book mBook;

    /**
     * Whether there is a table of contents available.
     */
    private final boolean mHasToc;

    /**
     * Builds an Epub from a epublib {@link nl.siegmann.epublib.domain.Book}.
     *
     * @param book The book.
     */
    private Epub(Book book) {
        this.mBook = book;
        this.mHasToc = inferTocPresence(book);
    }

    /**
     * Infers whether a TOC is present for the given book.
     *
     * @param book The book.
     * @return {@code true} if there is a TOC, {@code false} otherwise.
     */
    private boolean inferTocPresence(Book book) {
        TableOfContents contents = book.getTableOfContents();

        // If the TableOfContents doesn't include all Spine resources
        // ignore the TableOfContents alltogether
        return contents != null && contents.getTocReferences() != null &&
                contents.size() == book.getSpine().size();
    }

    /**
     * Creates an {@link Epub} from a context and URI.
     *
     * @param context The context.
     * @param uri     The uri.
     * @return An Epub from the URI.
     * @throws UnsupportedFormatException If the book is in an unexpected format or if it fails to read for some reason.
     */
    public static Epub fromUri(Context context, Uri uri) throws UnsupportedFormatException {
        return new Epub(openEpub(context, uri));
    }

    private static Book openEpub(Context context, Uri epubUri) throws UnsupportedFormatException {
        try {
            InputStream epubInputStream = context.getContentResolver().openInputStream(epubUri);
            String epubPath = FileUtils.getPath(context, epubUri);
            // Opening an attachment in Gmail may produce
            // content://gmail-ls/xxx@xxx.com/messages/9852/attachments/0.1/BEST/false
            // and no path
            if (epubPath != null && !epubPath.contains("epub")) {
                throw new UnsupportedFormatException("Unrecognized file format");
            }
            return new EpubReader().readEpub(epubInputStream);

        } catch (IOException e) {
            throw new UnsupportedFormatException("Unable to read from file", e);
        }
    }

    @Override
    public String getTitle() {
        return mBook.getMetadata().getFirstTitle();
    }

    @Override
    public String getAuthor() {
        List<Author> authors = mBook.getMetadata().getAuthors();

        if (authors.isEmpty()) {
            return "";
        } else {
            Author firstAuthor = authors.get(0);
            return firstAuthor.getFirstname() + " " + firstAuthor.getLastname();
        }
    }

    @Override
    public String loadChapter(int chapterNumber) {
        try {
            byte[] data = getChapterData(chapterNumber);
            if (data == null) {
                Log.e(TAG, "Unable to load chapter" + chapterNumber + " from " + mBook.getTitle());
                return "";
            }
            String bookStr = new String(data, "UTF-8");
            // Stripping epub content preceding the body tag
            // in this manner seems the most performant way to
            // strip css and other data that aren't removed by
            // Android's Html.fromHtml. Jsoup processing
            // seems to be prohibitively slow
            if (bookStr.contains("<body")) {
                bookStr = bookStr.substring(bookStr.indexOf("<body"));
            }
            return Html.fromHtml(bookStr).toString().replaceAll("\\n+", " ").replaceAll("(?s)<!--.*?-->", "");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Parsing failed " + e.getMessage());
            return "";
        }
    }

    @Override
    public String getChapterTitle(int chapterNumber) {
        String title = mHasToc ?
                getChapterTitleFromToc(chapterNumber) :
                mBook.getSpine().getResource(chapterNumber).getTitle();
        if (title == null || title.length() == 0) {
            return String.format("Chapter %d", chapterNumber);
        } else {
            return title;
        }
    }

    private byte[] getChapterData(int chapterNumber) throws IOException {
        Resource resource = mHasToc ?
                mBook.getTableOfContents().getAllUniqueResources().get(chapterNumber) :
                mBook.getSpine().getResource(chapterNumber);

        if (resource != null) {
            return resource.getData();
        }

        return null;
    }

    @Override
    public int countChapters() {
        // A book Toc does not necessarily contain all reading sections
        // The Spine is the definitive source for in-order reading content
        // Additionally, if a book has a Toc, getTocReferences() returns TocReferences
        // each of which may have several children Resources
        // E.g: TocReference describes "Part 1" of a novel, which contains
        // 4 chapter Resources.
        return mBook.getSpine().getSpineReferences().size();
    }


    private String getChapterTitleFromToc(int chapter) {
        // Is there no easier way to connect a TOCReference
        // to an absolute spine index?
        String title = "";
        int counter = 0;
        Resource targetResource = mBook.getTableOfContents().getAllUniqueResources().get(chapter);
        ArrayList<TOCReference> references = (ArrayList<TOCReference>) mBook.getTableOfContents().getTocReferences();
        for (TOCReference ref : references) {
            if (ref.getResource().equals(targetResource)) {
                return ref.getTitle();
            }
            for (TOCReference childRef : ref.getChildren()) {
                if (childRef.getResource().equals(targetResource)) {
                    return childRef.getTitle();
                }
            }
        }
        return title;
    }
}
