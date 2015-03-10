/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.yooiistudios.newsflow;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.NewsBrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.yooiistudios.newsflow.core.news.News;
import com.yooiistudios.newsflow.core.news.NewsFeed;
import com.yooiistudios.newsflow.core.news.database.NewsDb;
import com.yooiistudios.newsflow.core.util.NLLog;
import com.yooiistudios.newsflow.reference.CardPresenter;
import com.yooiistudios.newsflow.reference.PicassoBackgroundManagerTarget;
import com.yooiistudios.newsflow.reference.R;
import com.yooiistudios.newsflow.ui.DetailsActivity;
import com.yooiistudios.newsflow.ui.adapter.NewsFeedAdapter;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainFragment extends NewsBrowseFragment {
    private static final String TAG = "MainFragment";
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 400;
    private static final int GRID_ITEM_HEIGHT = 200;

    public static final String NEWS_ARG_KEY = "news_arg_key";

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    CardPresenter mCardPresenter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        initVariables(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();
        setupUIElements();
//        loadRows();
        setupEventListeners();
    }

    private void initVariables(Activity activity) {
        mCardPresenter = new CardPresenter(activity);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    public void applyNewsFeeds(NewsFeed topNewsFeed, ArrayList<NewsFeed> bottomNewsFeeds) {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        rowsAdapter.add(makeListRow(topNewsFeed, 0));
        for (int i = 0; i < bottomNewsFeeds.size(); i++) {
            NewsFeed newsFeed = bottomNewsFeeds.get(i);

            rowsAdapter.add(makeListRow(newsFeed, i + 1));
        }

        HeaderItem gridHeader = new HeaderItem(bottomNewsFeeds.size(), "PREFERENCES", null);

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.remove_db));
        gridRowAdapter.add(getResources().getString(R.string.copy_db));
        gridRowAdapter.add(getString(R.string.error_fragment));
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        setAdapter(rowsAdapter);
    }

    private ListRow makeListRow(NewsFeed newsFeed, int i) {
        HeaderItem header = new HeaderItem(i, newsFeed.getTitle(), null);
        NewsFeedAdapter adapter = new NewsFeedAdapter(mCardPresenter, newsFeed);
        return new ListRow(header, adapter);
    }

    public void configOnTopNewsImageUrlLoad(News news, String url, int newsIndex) {
        NewsDb.getInstance(getActivity()).saveTopNewsImageUrlWithGuid(url, news.getGuid());
        applyTopNewsImageUrlAt(url, newsIndex);
//        NewsDb.getInstance(getActivity()).saveTopNewsFeed(getTopNewsFeed());
    }

    public void configOnBottomNewsImageUrlLoad(News news, String url,
                                               int newsFeedIndex, int newsIndex) {
        NewsDb.getInstance(getActivity()).saveTopNewsImageUrlWithGuid(url, news.getGuid());
        applyBottomNewsImageUrlAt(url, newsFeedIndex, newsIndex);
//        NewsDb.getInstance(getActivity()).saveBottomNewsFeedAt(
//                getBottomNewsFeedAt(newsFeedIndex), newsFeedIndex);
    }

    private void applyTopNewsImageUrlAt(String imageUrl, int newsIndex) {
        NewsFeedAdapter adapter = getTopNewsFeedAdapter();
        adapter.applyNewsImageAt(imageUrl, newsIndex);
    }

    private void applyBottomNewsImageUrlAt(String imageUrl, int newsFeedIndex, int newsIndex) {
        NewsFeedAdapter adapter = getBottomNewsFeedAdapter(newsFeedIndex);
        adapter.applyNewsImageAt(imageUrl, newsIndex);
    }

    private NewsFeed getTopNewsFeed() {
        return getTopNewsFeedAdapter().getNewsFeed();
    }

    private NewsFeed getBottomNewsFeedAt(int index) {
        return getBottomNewsFeedAdapter(index).getNewsFeed();
    }

    private NewsFeedAdapter getTopNewsFeedAdapter() {
        return getNewsFeedAdapterAt(0);
    }

    private NewsFeedAdapter getBottomNewsFeedAdapter(int newsFeedIndex) {
        return getNewsFeedAdapterAt(newsFeedIndex + 1);
    }

    private NewsFeedAdapter getNewsFeedAdapterAt(int newsFeedIndex) {
        ListRow row = (ListRow)getAdapter().get(newsFeedIndex);
        return (NewsFeedAdapter)row.getAdapter();
    }

    private void prepareBackgroundManager() {
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
//        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        setBadgeDrawable(getResources().getDrawable(R.drawable.videos_by_google_banner));

        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.material_light_blue_800));

        // set search icon color
//        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));

        setupUIElementsExperiments();
    }

    private void setupUIElementsExperiments() {
        setBrowseTransitionListener(new BrowseTransitionListener() {
            @Override
            public void onHeadersTransitionStart(boolean withHeaders) {
                super.onHeadersTransitionStart(withHeaders);
                if (isShowingHeaders()) {
                    setBadgeDrawable(getResources().getDrawable(R.drawable.videos_by_google_banner));
                    setTitle("News Flow");
                } else {
                    setBadgeDrawable(null);
                    setTitle("All News");
                }
            }
        });
    }

    private void setupEventListeners() {
        // FIXME: 리스너를 달지 않으면 검색창이 보이지 않음
        /*
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                        .show();
            }
        });
        */

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

//            if (item instanceof Movie) {
//                Movie movie = (Movie) item;
//                Log.d(TAG, "Item: " + item.toString());
//                Intent intent = new Intent(getActivity(), DetailsActivity.class);
//                intent.putExtra(DetailsActivity.MOVIE, movie);
//
//                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
//                getActivity().startActivity(intent, bundle);
//            } else if (item instanceof String) {
//                if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
//                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
//                    startActivity(intent);
//                } else {
//                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
//                            .show();
//                }
//            }
            if (item instanceof News) {
                NLLog.now("item instanceof News");
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(NEWS_ARG_KEY, ((News) item).getLink());
                startActivity(intent);
            }
            if (item instanceof String) {
                if (((String) item).indexOf(getString(R.string.copy_db)) >= 0) {
                    NewsDb.copyDbToExternalStorate(getActivity());
                } else if (((String) item).indexOf(getString(R.string.remove_db)) >= 0) {
                    NewsDb.getInstance(getActivity()).clearArchive();
                }
            }
        }
    }


    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof News) {
                mBackgroundUrl = ((News) item).getImageUrl();
                startBackgroundTimer();
            }

        }
    }

    protected void setDefaultBackground(Drawable background) {
        mDefaultBackground = background;
    }

    protected void setDefaultBackground(int resourceId) {
        mDefaultBackground = getResources().getDrawable(resourceId);
    }

    protected void updateBackground() {
        if (mBackgroundUrl != null && mBackgroundUrl.trim().length() > 0) {
            Picasso.with(getActivity())
                    .load(mBackgroundUrl)
                    .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                    .centerCrop()
                    .error(mDefaultBackground)
                    .into(mBackgroundTarget);
        }
    }

    protected void updateBackground(Drawable drawable) {
        BackgroundManager.getInstance(getActivity()).setDrawable(drawable);
    }

    protected void clearBackground() {
        BackgroundManager.getInstance(getActivity()).setDrawable(mDefaultBackground);
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground();
                }
            });

        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}