package pro.dbro.glance;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class FeedFragment extends Fragment {

    // adapter that holds tweets, obviously :)
    ArrayAdapter<JsonObject> tweetAdapter;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

//        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//
//        FrameLayout fl = new FrameLayout(getActivity());
//        fl.setLayoutParams(params);
//
//        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
//                .getDisplayMetrics());
//
//        TextView v = new TextView(getActivity());
//        params.setMargins(margin, margin, margin, margin);
//        v.setLayoutParams(params);
//        v.setLayoutParams(params);
//        v.setGravity(Gravity.CENTER);
//        v.setBackgroundResource(R.drawable.ic_launcher);
//        v.setText("CARD " + (position + 1));
//
//        fl.addView(v);
//        return fl;

        View myFragmentView = inflater.inflate(R.layout.fragment_list, container, false);
        tweetAdapter = new ArrayAdapter<JsonObject>(getActivity(), 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.tweet, null);

                // we're near the end of the list adapter, so load more items
                if (position >= getCount() - 3)
                    load();

                // grab the tweet (or retweet)
                JsonObject post = getItem(position);

                String twitterId = post.get("title").getAsString();

                // and finally, set the name and text
                TextView handle = (TextView)convertView.findViewById(R.id.handle);
                handle.setText(twitterId);

                TextView text = (TextView)convertView.findViewById(R.id.tweet);
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

        ListView listView = (ListView) myFragmentView.findViewById(R.id.list);
        listView.setAdapter(tweetAdapter);

        // authenticate and do the first load
        //getCredentials();
        load();

        return myFragmentView;
    }

//    String accessToken;
//    private void getCredentials() {
//        Ion.with(this)
//                .load("https://api.twitter.com/oauth2/token")
//                        // embedding twitter api key and secret is a bad idea, but this isn't a real twitter app :)
//                .basicAuthentication("e4LrcHB55R3WamRYHpNfA", "MIABn1DU5db3Aj0xXzhthsf4aUKMAdoWJTMxJJcY")
//                .setBodyParameter("grant_type", "client_credentials")
//                .asJsonObject()
//                .setCallback(new FutureCallback<JsonObject>() {
//                    @Override
//                    public void onCompleted(Exception e, JsonObject result) {
//                        Toast.makeText(getActivity(), "Loading credentials", Toast.LENGTH_LONG).show();
//
//                        if (e != null) {
//                            Toast.makeText(getActivity(), "Error loading tweets", Toast.LENGTH_LONG).show();
//                            return;
//                        }
//                        accessToken = result.get("access_token").getAsString();
//                        load();
//                    }
//                });
//    }

    private void load() {
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

                        Toast.makeText(getActivity(), "Loading HN", Toast.LENGTH_LONG).show();

                        // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                        if (e != null) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "Error loading tweets: ", Toast.LENGTH_LONG).show();
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