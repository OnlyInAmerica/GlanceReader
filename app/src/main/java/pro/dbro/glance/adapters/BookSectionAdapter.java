package pro.dbro.glance.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import pro.dbro.glance.fragments.BookFeedFragment;
import pro.dbro.glance.fragments.FeedFragment;

public class BookSectionAdapter extends FragmentPagerAdapter {

    /**
     * A content feed.
     *
     * If a content feed is managed from a direct Parse query, it doesn't require a url.
     * See {@link pro.dbro.glance.fragments.FeedFragment#createFeedAdapter()} and
     * {@link ArticleAdapter}
    */
    public static enum BookFeed {
        LIBRARY     ("Your Library"),
        FEATURED    ("Featured",       "http://pipes.yahoo.com/pipes/pipe.run?_id=91eda8779a03f39b0e6d097cdde06284&_render=json"),
        FICTION  ("Fiction", "http://pipes.yahoo.com/pipes/pipe.run?_id=91eda8779a03f39b0e6d097cdde06284&_render=json"),
        NONFICTION ("Non-Fiction", "http://pipes.yahoo.com/pipes/pipe.run?_id=91eda8779a03f39b0e6d097cdde06284&_render=json");

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
