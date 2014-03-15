package pro.dbro.openspritz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;

/**
 * Created by davidbrodsky on 3/5/14.
 */
public class TocReferenceAdapter extends ArrayAdapter<TOCReference> {

    public TocReferenceAdapter(final Context context, int resource, TableOfContents toc) {
        super(context, resource, toc.getTocReferences());
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chapter_list_item, parent, false);
            assert convertView != null;

        }
        TOCReference ref = getItem(position);
        if (ref.getTitle() == null || ref.getTitle().trim().compareTo("") == 0) {
            ((TextView) convertView.findViewById(R.id.title)).setText(String.format("Chapter %d", position));
        } else {
            ((TextView) convertView.findViewById(R.id.title)).setText(String.format("%d: %s", position, ref.getTitle()));
        }
        return convertView;
    }

}