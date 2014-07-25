package pro.dbro.glance;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.squareup.otto.Bus;

import pro.dbro.glance.events.ChapterSelectedEvent;
import pro.dbro.glance.formats.SpritzerMedia;

/**
 * Created by davidbrodsky on 3/1/14.
 */
public class TocDialogFragment extends DialogFragment implements ListView.OnItemClickListener {

    private SpritzerMedia mBook;
    private TocReferenceAdapter mAdapter;
    private ListView mList;
    private Bus mBus;

    static TocDialogFragment newInstance(SpritzerMedia book) {
        TocDialogFragment f = new TocDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("book", book);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBook = (SpritzerMedia) getArguments().getSerializable("book");
        mAdapter = new TocReferenceAdapter(getActivity(), R.layout.chapter_list_item, mBook);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_chapters, null);
        mList = (ListView) v.findViewById(R.id.list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        GlanceApplication app = (GlanceApplication) getActivity().getApplication();
        this.mBus = ((GlanceApplication)getActivity().getApplication()).getBus();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.select_chapter))
                .setView(v);
        return builder.create();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBus.post(new ChapterSelectedEvent(position));
        getDialog().dismiss();
    }
}
