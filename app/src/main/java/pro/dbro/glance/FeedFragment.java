package pro.dbro.glance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

import java.util.Arrays;
import java.util.List;

public class FeedFragment extends Fragment {

    // adapter that holds tweets, obviously :)
    ArrayAdapter<JsonObject> tweetAdapter;
    ParseQueryAdapter<ParseObject> articleAdapter;

    // This "Future" tracks loading operations.
    // A Future is an object that manages the state of an operation
    // in progress that will have a "Future" result.
    // You can attach callbacks (setCallback) for when the result is ready,
    // or cancel() it if you no longer need the result.
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
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();
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

        switch(position) {
            case 0:
//                ParseQuery<ParseObject> query = ParseQuery.getQuery("Article");
//                query.findInBackground(new FindCallback<ParseObject>() {
//                    public void done(List<ParseObject> articleList, ParseException e) {
//                        if (e == null) {
//                            Log.d("score", "Retrieved " + articleList.size() + " scores");
//                        } else {
//                            Log.d("score", "Error: " + e.getMessage());
//                        }
//                    }
//                });

                articleAdapter =  new ArticleAdapter(getActivity());
                listView.setAdapter(articleAdapter);

                break;
            case 3:
                tweetAdapter = new ArrayAdapter<JsonObject>(getActivity(), 0) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null)
                            convertView = getActivity().getLayoutInflater().inflate(R.layout.tweet, null);

                        // we're near the end of the list adapter, so load more items
                        if (position >= getCount() - 3)
                            loadHN();

                        // grab the tweet (or retweet)
                        JsonObject post = getItem(position);

                        String twitterId = post.get("title").getAsString();

                        // and finally, set the name and text
                        TextView handle = (TextView) convertView.findViewById(R.id.handle);
                        handle.setText(twitterId);

                        TextView text = (TextView) convertView.findViewById(R.id.tweet);
                        text.setText(post.get("url").getAsString());

                        convertView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                TextView tv = (TextView) view.findViewById(R.id.tweet);
                                Intent communityIntent = new Intent(getActivity(), MainActivity.class);
                                communityIntent.setAction(Intent.ACTION_SEND);
                                communityIntent.putExtra(Intent.EXTRA_TEXT, tv.getText());
                                startActivity(communityIntent);
                            }
                        });

                        return convertView;
                    }
                };

                listView.setAdapter(tweetAdapter);

                // authenticate and do the first load
                //getCredentials();
                loadHN();
                break;
            default:
                break;
        }

        return myFragmentView;
    }

    private void loadHN() {
        // don't attempt to load more if a load is already in progress
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // load the tweets
       // String url = "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=BestAt&count=20";
        String url = "http://api.ihackernews.com/page";
//        if (tweetAdapter.getCount() > 0) {
//            // load from the "last" id
//            JsonObject last = tweetAdapter.getItem(tweetAdapter.getCount() - 1);
//            url += "&max_id=" + last.get("id_str").getAsString();
//        }

        // Request tweets from Twitter using Ion.
        // This is done using Ion's Fluent/Builder API.
        // This API lets you chain calls together to build
        // complex requests.

        // This request loads a URL as JsonArray and invokes
        // a callback on completion.
        loading = Ion.with(getActivity(), url)
                //.setHeader("Authorization", "Bearer " + accessToken)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {

                        // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                        if (e != null) {
                            e.printStackTrace();
                            return;
                        }

                        JsonArray results = result.getAsJsonArray("items");

                        // add the tweets
                        for (int i = 0; i < results.size(); i++) {
                            System.out.println(results.get(i).getAsJsonObject());
                            tweetAdapter.add(results.get(i).getAsJsonObject());
                        }
                    }
                });
    }

}