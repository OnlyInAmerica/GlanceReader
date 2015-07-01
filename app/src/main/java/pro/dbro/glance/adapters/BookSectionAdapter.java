package pro.dbro.glance.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import pro.dbro.glance.fragments.BookFeedFragment;

public class BookSectionAdapter extends FragmentPagerAdapter {

    /**
     * A content feed.
     *
     * If a content feed is managed from a direct Parse query, it doesn't require a url.
     * See {@link pro.dbro.glance.fragments.FeedFragment#createFeedAdapter()} and
     * {@link ArticleAdapter}
    */
    public static enum BookFeed {
        FEATURED            ("Featured",        "http://pipes.yahoo.com/pipes/pipe.run?_id=f5376982cf7091007810f5afda06b03f&_render=json"),
        SHORT               ("Short Stories",   "http://pipes.yahoo.com/pipes/pipe.run?_id=af4a2509db6bc0556e0f4a19e5e6a102&_render=json"),
        SCIFI               ("Sci Fi",          "http://pipes.yahoo.com/pipes/pipe.run?_id=83690168c3d5affaa4774b8f524bb7f7&_render=json"),
        ADVENTURE           ("Adventure",       "http://pipes.yahoo.com/pipes/pipe.run?_id=66d87633a42d3472f4f036b571043675&_render=json"),
        MYSTERY             ("Mystery",         "http://pipes.yahoo.com/pipes/pipe.run?_id=f5a95b8a1527ebe468fac731b3c0396d&_render=json"),
        FANTASY             ("Fantasy",         "http://pipes.yahoo.com/pipes/pipe.run?_id=762b6ad467dfcb675fd6ffa080581692&_render=json"),
        ROMANCE             ("Romance",         "http://pipes.yahoo.com/pipes/pipe.run?_id=dbc93819b8795e9957aed0aa10ca2c08&_render=json"),
        HORROR              ("Horror",          "http://pipes.yahoo.com/pipes/pipe.run?_id=85a1a92b610f0802e4e3ab820db9c675&_render=json"),
        WESTERN             ("Western",         "http://pipes.yahoo.com/pipes/pipe.run?_id=69ff3f8e8ca051dd58a8212f0da133f7&_render=json"),
        HISTORICAL          ("Historical",      "http://pipes.yahoo.com/pipes/pipe.run?_id=32504cdaecb63ddc75e0d03ac7e3ac03&_render=json"),
        LITERARY            ("Literary",        "http://pipes.yahoo.com/pipes/pipe.run?_id=2e861ac39ecbefbe827d5d6c6cf85c75&_render=json"),
        HUMOR               ("Humor",           "http://pipes.yahoo.com/pipes/pipe.run?_id=e3657ab1b70e8cde0356c89c25ec45b1&_render=json"),
        OCCULT              ("Occult",          "http://pipes.yahoo.com/pipes/pipe.run?_id=2f1e3250960f8b731e162abf4b1fc8b3&_render=json"),
        DRAMA               ("Drama",           "http://pipes.yahoo.com/pipes/pipe.run?_id=4d581af82155f99575fb90253f20e248&_render=json"),
        WAR                 ("War",             "http://pipes.yahoo.com/pipes/pipe.run?_id=74fb745ac616317442aa8cd8cad93ebc&_render=json"),
        NONFICTION          ("Non Fiction",     "http://pipes.yahoo.com/pipes/pipe.run?_id=a4bc28d50c84a89ede1da1248e9c1569&_render=json");

        private final String mTitle;
        private final String mFeedUrl;

        private BookFeed(final String title) {
            mTitle = title;
            mFeedUrl = null;
        }

        private BookFeed(final String title, final String url) {
            mTitle = title;
            mFeedUrl = url;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getFeedUrl() {
            return mFeedUrl;
        }
    }

    public BookSectionAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return BookFeed.values()[position].getTitle();
    }

    @Override
    public int getCount() {
        return BookFeed.values().length;
    }

    @Override
    public Fragment getItem(int position) {
        return BookFeedFragment.newInstance(BookFeed.values()[position]);
    }

}
