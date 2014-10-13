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

import java.net.MalformedURLException;
import java.net.URL;

import pro.dbro.glance.adapters.AdapterUtils;
import pro.dbro.glance.R;
//import pro.dbro.glance.SECRETS;
import pro.dbro.glance.adapters.ArticleAdapter;
import pro.dbro.glance.adapters.ReaderSectionAdapter;
import pro.dbro.glance.lib.SpritzerTextView;

public class FeedFragment extends ListFragment {

    ArrayAdapter<JsonObject> mFeedItemAdapter;
    ParseQueryAdapter<ParseObject> mArticleAdapter;
    //    ProgressBar mLoadingView;
    SpritzerTextView mLoadingView;

    // This "Future" tracks loading operations.
    Future<JsonObject> mFuture;

    private static final String ARG_FEED = "feed";
    private ReaderSectionAdapter.Feed mFeed;
    private static boolean sParseSetup = false;
    private boolean mLoading = false;

    public static FeedFragment newInstance(ReaderSectionAdapter.Feed feed) {
        FeedFragment f = new FeedFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_FEED, feed);
        f.setArguments(b);
        return f;
    }

    public void setupParse() {
        Parse.initialize(this.getActivity(), "MY_GLANCE_READER_PARSE_ID", "MY_GLANCE_READER_SECRET_KEY");
        sParseSetup = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeed = (ReaderSectionAdapter.Feed) getArguments().getSerializable(ARG_FEED);
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

        switch (mFeed) {

            case POPULAR:
                mArticleAdapter = new ArticleAdapter(getActivity(), ArticleAdapter.ArticleFilter.RECENT);
                listView.setAdapter(mArticleAdapter);
                break;
            case RECENT:
                mArticleAdapter = new ArticleAdapter(getActivity(), ArticleAdapter.ArticleFilter.ALL);
                listView.setAdapter(mArticleAdapter);
                break;
            default:
                mFeedItemAdapter = createFeedAdapter();
                listView.setAdapter(mFeedItemAdapter);
                loadPipe(mFeed.getFeedUrl());
                break;
        }

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
                    convertView.setTag((post.get("link").getAsString()));
                    try {
                        text.setText(new URL(post.get("link").getAsString()).getHost());
                    } catch (MalformedURLException e) {
                        text.setText(post.get("link").getAsString());
                    }

                    convertView.setOnClickListener(AdapterUtils.getArticleClickListener(convertView.getContext()));
                } catch (Exception e) {
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
        mLoadingView.getSpritzer().pause();
        mLoadingView.getSpritzer().setLoopingPlayback(true);
        mLoadingView.getSpritzer().setWpm(200);
        mLoadingView.getSpritzer().setTextAndStart(getString(R.string.spritz_error), false);
    }

    private void showLoading() {
        mLoadingView.getSpritzer().pause();
        mLoadingView.getSpritzer().setLoopingPlayback(true);
        mLoadingView.getSpritzer().setWpm(400);
        mLoadingView.getSpritzer().setTextAndStart(getString(R.string.spritz_loading), false);
    }
}