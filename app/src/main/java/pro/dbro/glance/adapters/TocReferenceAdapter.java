package pro.dbro.glance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import pro.dbro.glance.formats.SpritzerMedia;

/**
 * Created by davidbrodsky on 3/5/14.
 */
public class TocReferenceAdapter extends BaseAdapter {

    private final Context mContext;
    private final int mResource;
    private final SpritzerMedia mBook;

    public TocReferenceAdapter(final Context context, int resource, SpritzerMedia book) {
        this.mContext = context;
        this.mResource = resource;
        this.mBook = book;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chapter_list_item, parent, false);
            assert convertView != null;

        }

        String chapterTitle = mBook.getChapterTitle(position);
        ((TextView) convertView.findViewById(R.id.url)).setText(chapterTitle);

        return convertView;
    }


    @Override
    public int getCount() {
        return mBook.countChapters();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
}