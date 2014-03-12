package pro.dbro.openspritz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import nl.siegmann.epublib.domain.SpineReference;

/**
 * Created by davidbrodsky on 3/5/14.
 */
public class SpineReferenceAdapter extends ArrayAdapter<SpineReference> {

    public SpineReferenceAdapter(final Context context, int resource, List<SpineReference> objects) {
        super(context, resource, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chapter_list_item, parent, false);
            assert convertView != null;

        }
        SpineReference ref = getItem(position);
        if (ref.getResource().getTitle() == null || ref.getResource().getTitle().trim().compareTo("") == 0) {
            ((TextView) convertView.findViewById(R.id.title)).setText(String.format("Chapter %d", position));
        } else {
            ((TextView) convertView.findViewById(R.id.title)).setText(ref.getResource().getTitle());
        }
        return convertView;
    }

}