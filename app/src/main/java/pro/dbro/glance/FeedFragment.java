package pro.dbro.glance;

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

public class FeedFragment extends Fragment {

    private static String NEWS_URL = "http://pipes.yahoo.com/pipes/pipe.run?_id=40805955111ac2e85631facfb362f067&_render=json";
    private static String COMMENTARY_URL = "http://pipes.yahoo.com/pipes/pipe.run?_id=dc1a399c275cbc0bcf6329c8419d6f4f&_render=json";
    private static String FICTION_URL = "http://pipes.yahoo.com/pipes/pipe.run?_id=ee8d2db2513114660b054cd82da29b69&_render=json";
    private static String HN_URL = "http://pipes.yahoo.com/pipes/pipe.run?_id=af38f38c0a21785ef8409d48ab4c1246&_render=json";

    ArrayAdapter<JsonObject> feedItemAdapter;
    ParseQueryAdapter<ParseObject> articleAdapter;

    // This "Future" tracks loading operations.
    Future<JsonObject> loading;

    private static final String ARG_POSITION = "position";
    private int position;

    public static FeedFragment newInstance(int position) {
        FeedFragment f = new FeedFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    public void setupParse(){
        Parse.initialize(this.getActivity(), "IKXOwtsEGwpJxjD56rloizwwsB4pijEve8nU5wkB", "8K0yHwwEevmCiuuHTjGj7HRhFTzHmycBXXspmnPU");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
        if (position == 0){
            setupParse();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.fragment_list, container, false);
        ListView listView = (ListView) myFragmentView.findViewById(R.id.list);

        // Which tab are we on?
        switch(position) {

            // Most Popular
            case 0:
                articleAdapter =  new ArticleAdapter(getActivity(), 0);
                listView.setAdapter(articleAdapter);
                break;
            // Most Recent
            case 1:
                articleAdapter =  new ArticleAdapter(getActivity(), 1);
                listView.setAdapter(articleAdapter);
                break;
            // News
            case 2:
                feedItemAdapter = createFeedAdapter();
                listView.setAdapter(feedItemAdapter);
                loadPipe(NEWS_URL);
                break;
            // Commentary
            case 3:
                feedItemAdapter = createFeedAdapter();
                listView.setAdapter(feedItemAdapter);
                loadPipe(COMMENTARY_URL);
                break;

            // Fiction
            case 4:
                feedItemAdapter = createFeedAdapter();
                listView.setAdapter(feedItemAdapter);
                loadPipe(FICTION_URL);
                break;

            // HN
            case 5:
                feedItemAdapter = createFeedAdapter();
                listView.setAdapter(feedItemAdapter);
                loadPipe(HN_URL);
                break;

            default:
                break;
        }

        return myFragmentView;
    }

    // Create adapters from items coming from Pipes.
    private ArrayAdapter createFeedAdapter(){
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