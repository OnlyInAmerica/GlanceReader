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
import pro.dbro.openspritz.events.ChapterSelectedEvent;

/**
 * Created by davidbrodsky on 3/1/14.
 */
public class ChapterListDialogFragment extends DialogFragment implements ListView.OnItemClickListener {

    private Book mBook;
    private SpineReferenceAdapter mAdapter;
    private ListView mList;
    private Bus mBus;

    static ChapterListDialogFragment newInstance(Book book) {
        ChapterListDialogFragment f = new ChapterListDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("book", book);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBook = (Book) getArguments().getSerializable("book");
        mAdapter = new SpineReferenceAdapter(getActivity(), R.layout.chapter_list_item, mBook.getSpine().getSpineReferences());
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
