package pro.dbro.glance.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import pro.dbro.glance.SECRETS;
import pro.dbro.glance.activities.MainActivity;
import pro.dbro.glance.adapters.ArticleAdapter;
import pro.dbro.glance.adapters.ReaderSectionAdapter;

public class FeedFragment extends Fragment {

    ArrayAdapter<JsonObject> feedItemAdapter;
    ParseQueryAdapter<ParseObject> articleAdapter;

    // This "Future" tracks loading operations.
    Future<JsonObject> loading;

    private static final String ARG_FEED = "feed";
    private ReaderSectionAdapter.Feed mFeed;
    private static boolean sParseSetup = false;

    public static FeedFragment newInstance(ReaderSectionAdapter.Feed feed) {
        FeedFragment f = new FeedFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_FEED, feed);
        f.setArguments(b);

        return f;
    }

    public void setupParse(){
        Parse.initialize(this.getActivity(), SECRETS.getParseId(), SECRETS.getParseSecret());
        sParseSetup = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeed = (ReaderSectionAdapter.Feed) getArguments().getSerializable(ARG_FEED);
        if (!sParseSetup){
            setupParse();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.fragment_list, container, false);
        ListView listView = (ListView) myFragmentView.findViewById(R.id.list);

        switch(mFeed) {

            case POPULAR:
                articleAdapter =  new ArticleAdapter(getActivity(), ArticleAdapter.ArticleFilter.RECENT);
                listView.setAdapter(articleAdapter);
                break;
            case RECENT:
                articleAdapter =  new ArticleAdapter(getActivity(), ArticleAdapter.ArticleFilter.ALL);
                listView.setAdapter(articleAdapter);
                break;
            case NEWS:
            case COMMENTARY:
            case FICTION:
            case HN:
            //case TRUE_REDDIT:
                feedItemAdapter = createFeedAdapter();
                listView.setAdapter(feedItemAdapter);
                loadPipe(mFeed.getFeedUrl());
                break;
            default:
                break;
        }

        return myFragmentView;
    }

    // Create adapters from items coming from Pipes.
    private ArrayAdapter<JsonObject> createFeedAdapter(){
        return new ArrayAdapter<JsonObject>(getActivity(), 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.tweet, null);

                JsonObject post = getItem(position);
                try {

                    String title = post.get("title").getAsString();
                    TextView handle = (TextView) convertView.findViewById(R.id.title);
                    handle.setText(title);

                    TextView text = (TextView) convertView.findViewById(R.id.url);
                    text.setText(post.get("link").getAsString());

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TextView tv = (TextView) view.findViewById(R.id.url);
                            Intent communityIntent = new Intent(getActivity(), MainActivity.class);
                            communityIntent.setAction(Intent.ACTION_SEND);
                            communityIntent.putExtra(Intent.EXTRA_TEXT, tv.getText());
                            startActivity(communityIntent);
                        }
                    });
                } catch(Exception e){
                    // Parsing is fucked. NSFO.
                }

                return convertView;
            }
        };
    }

    private void loadPipe(String url) {

        // don't attempt to load more if a load is already in progress
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // This request loads a URL as JsonArray and invokes
        // a callback on completion.
        loading = Ion.with(getActivity(), url)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {

                        // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                        if (e != null) {
                            e.printStackTrace();
                            return;
                        }

                        JsonObject value = result.getAsJsonObject("value");
                        JsonArray results = value.getAsJsonArray("items");

                        for (int i = 0; i < results.size(); i++) {
                            feedItemAdapter.add(results.get(i).getAsJsonObject());
                        }
                    }
                });
    }

}