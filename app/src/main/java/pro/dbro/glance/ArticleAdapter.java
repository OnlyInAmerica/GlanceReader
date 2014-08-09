package pro.dbro.glance;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

public class ArticleAdapter extends ParseQueryAdapter<ParseObject> {

    public ArticleAdapter(Context context) {
        super(context, new ParseQueryAdapter.QueryFactory<ParseObject>() {
            public ParseQuery<ParseObject> create() {
                ParseQuery query = new ParseQuery("Article");
                return query;
            }
        });
    }

    public ArticleAdapter(final Context context, final int filterType) {
        super(context, new ParseQueryAdapter.QueryFactory<ParseObject>() {
                    public ParseQuery<ParseObject> create() {

                        ParseQuery query = null;
                        switch(filterType) {
                            case 0:
                                query = new ParseQuery("Article");
                                return query;
                            case 1:
                                query = new ParseQuery("Article");
                                query.orderByDescending("createdAt");
                                return query;
                            default:
                                query = new ParseQuery("Article");
                                return query;
                        }
                    }
            });
    }

    @Override
    public View getItemView(ParseObject object, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.tweet, null);
        }

        TextView handle = (TextView) convertView.findViewById(R.id.title);
        handle.setText(object.getString("title"));

        TextView text = (TextView) convertView.findViewById(R.id.url);
        text.setText(object.getString("url"));

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView tv = (TextView) view.findViewById(R.id.url);
                Intent communityIntent = new Intent(getContext(), MainActivity.class);
                communityIntent.setAction(Intent.ACTION_SEND);
                communityIntent.putExtra(Intent.EXTRA_TEXT, tv.getText());
                getContext().startActivity(communityIntent);
            }
        });

        return convertView;
    }
}