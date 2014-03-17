package pro.dbro.openspritz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.squareup.otto.Bus;

import nl.siegmann.epublib.domain.Book;
import pro.dbro.openspritz.formats.SpritzerBook;
import pro.dbro.openspritz.events.ChapterSelectedEvent;

/**
 * Created by davidbrodsky on 3/1/14.
 */
public class TocDialogFragment extends DialogFragment implements ListView.OnItemClickListener {

    private SpritzerBook mBook;
    private TocReferenceAdapter mAdapter;
    private ListView mList;
    private Bus mBus;

    static TocDialogFragment newInstance(SpritzerBook book) {
        TocDialogFragment f = new TocDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("book", book);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBook = (SpritzerBook) getArguments().getSerializable("book");
        mAdapter = new TocReferenceAdapter(getActivity(), R.layout.chapter_list_item, mBook);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_chapters, null);
        mList = (ListView) v.findViewById(R.id.list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        OpenSpritzApplication app = (OpenSpritzApplication) getActivity().getApplication();
        this.mBus = ((OpenSpritzApplication)getActivity().getApplication()).getBus();

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
