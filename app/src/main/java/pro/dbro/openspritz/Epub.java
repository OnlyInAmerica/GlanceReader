package pro.dbro.openspritz;

import android.util.Log;

import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;

/**
 * Created by davidbrodsky on 2/28/14.
 */
public class Epub {

    public static void getToc(Book book) {
        logTableOfContents(book.getTableOfContents().getTocReferences(), 0);
    }

    private static void logTableOfContents(List<TOCReference> tocReferences, int depth) {
        if (tocReferences == null) {
            return;
        }

        for (TOCReference tocReference : tocReferences) {
            StringBuilder tocString = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                tocString.append("\t");
            }
            tocString.append(tocReference.getTitle());
            Log.i("epublib", tocString.toString());
            logTableOfContents(tocReference.getChildren(), depth + 1);

        }

    }
}
