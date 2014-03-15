package pro.dbro.openspritz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import nl.siegmann.epublib.domain.Book;

/**
 * Created by davidbrodsky on 3/1/14.
 */
public class TocDialogFragment extends DialogFragment implements ListView.OnItemClickListener {

    private Book mBook;
    private TocReferenceAdapter mAdapter;
    private ListView mList;
    private OnChapterSelectListener mOnChapterSelectListener;

    static TocDialogFragment newInstance(Book book) {
        TocDialogFragment f = new TocDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("book", book);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnChapterSelectListener = (OnChapterSelectListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SpritzFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBook = (Book) getArguments().getSerializable("book");
        mAdapter = new TocReferenceAdapter(getActivity(), R.layout.chapter_list_item, mBook.getTableOfContents());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_chapters, null);
        mList = (ListView) v.findViewById(R.id.list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.select_chapter))
                .setView(v);
        return builder.create();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mOnChapterSelectListener.onChapterSelected(position);
        getDialog().dismiss();
    }

    public interface OnChapterSelectListener {
        public abstract void onChapterSelected(int chapter);
    }
}
