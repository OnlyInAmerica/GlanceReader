package pro.dbro.openspritz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import pro.dbro.openspritz.formats.SpritzerBook;

/**
 * Created by davidbrodsky on 3/5/14.
 */
public class TocReferenceAdapter extends BaseAdapter {

    private final Context mContext;
    private final int mResource;
    private final SpritzerBook mBook;

    public TocReferenceAdapter(final Context context, int resource, SpritzerBook book) {
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

        if (chapterTitle == null) {
            ((TextView) convertView.findViewById(R.id.title)).setText(String.format("Chapter %d", position));
        } else {
            ((TextView) convertView.findViewById(R.id.title)).setText(chapterTitle);
        }
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