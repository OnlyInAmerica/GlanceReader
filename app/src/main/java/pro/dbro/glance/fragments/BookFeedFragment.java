package pro.dbro.glance.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQueryAdapter;

import pro.dbro.glance.R;
import pro.dbro.glance.adapters.AdapterUtils;
import pro.dbro.glance.adapters.BookSectionAdapter;
import pro.dbro.glance.lib.SpritzerTextView;
import timber.log.Timber;

//import pro.dbro.glance.SECRETS;

public class BookFeedFragment extends ListFragment {

    ArrayAdapter<JsonObject> mFeedItemAdapter;
    ParseQueryAdapter<ParseObject> mArticleAdapter;
    //    ProgressBar mLoadingView;
    SpritzerTextView mLoadingView;

    // This "Future" tracks loading operations.
    Future<JsonObject> mFuture;

    private static final String ARG_FEED = "feed";
    private BookSectionAdapter.BookFeed mFeed;
    private static boolean sParseSetup = false;
    private boolean mLoading = false;

    public static BookFeedFragment newInstance(BookSectionAdapter.BookFeed feed) {
        BookFeedFragment f = new BookFeedFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_FEED, feed);
        f.setArguments(b);
        return f;
    }

    public void setupParse() {
        Parse.initialize(this.getActivity(), "IKXOwtsEGwpJxjD56rloizwwsB4pijEve8nU5wkB", "8K0yHwwEevmCiuuHTjGj7HRhFTzHmycBXXspmnPU");
        sParseSetup = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeed = (BookSectionAdapter.BookFeed) getArguments().getSerializable(ARG_FEED);
        if (!sParseSetup) {
            setupParse();
        }
    }

    public void onResume() {
        super.onResume();
        if (mLoading) showLoading();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.fragment_list, container, false);
        ListView listView = (ListView) myFragmentView.findViewById(android.R.id.list);
//        mLoadingView = (ProgressBar) myFragmentView.findViewById(android.R.id.empty);
        mLoadingView = (SpritzerTextView) myFragmentView.findViewById(android.R.id.empty);

        mFeedItemAdapter = createFeedAdapter();
        listView.setAdapter(mFeedItemAdapter);
        loadPipe(mFeed.getFeedUrl());

        return myFragmentView;
    }

    // Create adapters from items coming from Pipes.
    private ArrayAdapter<JsonObject> createFeedAdapter() {
        return new ArrayAdapter<JsonObject>(getActivity(), 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.article_list_item, null);
                }

                JsonObject post = getItem(position);
                try {

                    String title = post.get("title").getAsString();
                    TextView handle = (TextView) convertView.findViewById(R.id.title);
                    handle.setText(title);

                    TextView text = (TextView) convertView.findViewById(R.id.url);
                    Timber.d(post.get("link").getAsString().replace(".atom", ".epub"));
                    convertView.setTag((post.get("link").getAsString().replace(".atom", ".epub")));
                    try {
                        JsonObject author = post.get("author").getAsJsonObject();
                        String name = author.get("name").getAsString();
                        text.setText(name);
                    } catch (Exception e) {
                        text.setText(post.get("author").getAsString());
                    }

                    convertView.setOnClickListener(AdapterUtils.getBookClickListener());
                    convertView.setOnLongClickListener(AdapterUtils.getArticleLongClickListener());
                } catch (Exception e) {
                    Timber.e(e, "Bind error");
                    // Parsing is fucked. NSFO.
                }

                return convertView;
            }
        };
    }

    private void loadPipe(String url) {
        // don't attempt to load more if a load is already in progress
        if (mFuture != null && !mFuture.isDone() && !mFuture.isCancelled())
            return;

        mLoading = true;

        // This request loads a URL as JsonArray and invokes
        // a callback on completion.
        mFuture = Ion.with(getActivity(), url)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        mLoading = false;

                        // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                        if (e != null) {
                            showError();
                            e.printStackTrace();
                            return;
                        }

                        JsonObject value = result.getAsJsonObject("value");
                        JsonArray results = value.getAsJsonArray("items");

                        if (results.size() == 0) {
                            showError();
                            return;
                        }

                        for (int i = 0; i < results.size(); i++) {
                            mFeedItemAdapter.add(results.get(i).getAsJsonObject());
                        }
                    }
                });

        // Replace network fetch code with this to simulate network error
//        mLoadingView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mLoading = false;
//                showError();
//            }
//        }, 6000);
    }

    private void showError() {
        mLoadingView.getSpritzer().setStaticText(getString(R.string.spritz_error));
    }

    private void showLoading() {
        mLoadingView.getSpritzer().setStaticText(getString(R.string.spritz_loading));
    }
}