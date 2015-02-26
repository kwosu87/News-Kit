package com.yooiistudios.newsflow.ui.fragment;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.yooiistudios.newsflow.R;
import com.yooiistudios.newsflow.model.news.News;
import com.yooiistudios.newsflow.model.news.NewsFeed;
import com.yooiistudios.newsflow.model.news.NewsImageRequestQueue;
import com.yooiistudios.newsflow.model.news.TintType;
import com.yooiistudios.newsflow.model.news.util.NewsFeedUtils;
import com.yooiistudios.newsflow.ui.activity.MainActivity;
import com.yooiistudios.newsflow.ui.adapter.MainTopPagerAdapter.OnItemClickListener;
import com.yooiistudios.newsflow.util.ImageMemoryCache;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 2014. 8. 23.
 *
 * MainNewsFeedFragment
 *  메인화면 상단의 뷰페이저에 들어갈 프레그먼트
 */
public class MainNewsFeedFragment extends Fragment {
    private static final String KEY_NEWS_FEED = "KEY_NEWS_FEED";
    private static final String KEY_NEWS = "KEY_CURRENT_NEWS_INDEX";
    private static final String KEY_POSITION = "KEY_TAB_INDEX";

    private ImageLoader mImageLoader;
    private NewsFeed mNewsFeed;
    private News mNews;
    private int mPosition;
    private boolean mRecycled;

    public static MainNewsFeedFragment newInstance(NewsFeed newsFeed, News news, int position) {
        MainNewsFeedFragment f = new MainNewsFeedFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putParcelable(KEY_NEWS_FEED, newsFeed);
        args.putParcelable(KEY_NEWS, news);
        args.putInt(KEY_POSITION, position);
        f.setArguments(args);

        return f;
    }
    
    @SuppressWarnings("UnusedDeclaration")
    public MainNewsFeedFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNews = getArguments().getParcelable(KEY_NEWS);
            mNewsFeed = getArguments().getParcelable(KEY_NEWS_FEED);
            mPosition = getArguments().getInt(KEY_POSITION);
        } else {
            mNews = null;
            mPosition = -1;
        }
        mRecycled = false;

        Context context = getActivity().getApplicationContext();

        RequestQueue requestQueue = NewsImageRequestQueue.getInstance(context).getRequestQueue();
        mImageLoader = new ImageLoader(requestQueue,
                ImageMemoryCache.getInstance(context));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup)inflater.inflate(R.layout.fragment_main_top_viewpager_item,
                container, false);

        ItemViewHolder holder = new ItemViewHolder(root);

        if (mNews == null) {
            holder.imageView.setImageDrawable(null);
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.titleTextView.setText("");

            root.setOnClickListener(null);

            return root;
        }

        applyImage(holder);

        String newsName = mNews.getTitle();
        if (newsName != null) {
            holder.titleTextView.setText(newsName);
        }

        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemViewHolder viewHolder = (ItemViewHolder) view.getTag();
                getOnItemClickListener().onTopItemClick(viewHolder, mNewsFeed, mPosition);
            }
        });
        root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                getOnLongClickListener().onLongClick(v);
                return true;
            }
        });
        root.setTag(holder);

        return root;
    }

    public void applyImage() {
        View root;
        ItemViewHolder viewHolder;
        if ((root = getView()) != null &&
                (viewHolder = (ItemViewHolder)root.getTag()) != null) {
            applyImage(viewHolder);
        }

    }

    private void applyImage(final ItemViewHolder viewHolder) {
        viewHolder.imageView.setImageDrawable(null);

        // set image
        String imgUrl = mNews.getImageUrl();
        if (imgUrl != null) {
            mImageLoader.get(imgUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (mRecycled) {
                        return;
                    }

                    Bitmap bitmap = response.getBitmap();

                    viewHolder.progressBar.setVisibility(View.GONE);

                    if (bitmap == null && isImmediate) {
                        // 비트맵이 null이지만 인터넷을 통하지 않고 바로 불린 콜백이라면 무시하자
                        return;
                    }

                    if (bitmap != null && viewHolder.imageView != null) {
                        viewHolder.imageView.setImageBitmap(bitmap);
                        viewHolder.imageView.setColorFilter(NewsFeedUtils.getGrayFilterColor());
                        viewHolder.imageView.setTag(TintType.GRAY_SCALE);
                    } else {
                        // TODO cache 에 비트맵이 없는 경우. 기본적으로 없어야 할 상황.
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    showDummyImage(viewHolder);
                }
            });
        } else if (!mNews.isImageUrlChecked()) {
            // image url not yet fetched. show loading progress dialog
            viewHolder.progressBar.setVisibility(View.VISIBLE);

        } else {
            // failed to find image url
            showDummyImage(viewHolder);
        }
    }

    private void showDummyImage(ItemViewHolder viewHolder) {
        if (!mRecycled && viewHolder.imageView != null) {
            Bitmap dummyImage = NewsFeedUtils.getDummyNewsImage(getActivity().getApplicationContext());
            viewHolder.imageView.setImageBitmap(dummyImage);
            viewHolder.imageView.setColorFilter(NewsFeedUtils.getDummyImageFilterColor());
            viewHolder.imageView.setTag(TintType.DUMMY);

            viewHolder.progressBar.setVisibility(View.GONE);
        }
    }

    private OnItemClickListener getOnItemClickListener() {
        ViewGroup parentView = getMainTopContainerLayout();
        if (parentView instanceof OnItemClickListener) {
            return (OnItemClickListener)parentView;
        } else {
            throw new IllegalStateException("Containing layout should implement "
                    + OnItemClickListener.class.getSimpleName() + ".");
        }
    }

    private View.OnLongClickListener getOnLongClickListener() {
        ViewGroup parentView = getMainTopContainerLayout();
        if (parentView instanceof View.OnLongClickListener) {
            return (View.OnLongClickListener)parentView;
        } else {
            throw new IllegalStateException("Containing layout should implement "
                    + View.OnLongClickListener.class.getSimpleName() + ".");
        }
    }

    private ViewGroup getMainTopContainerLayout() {
        return ((MainActivity)getActivity()).getMainTopContainerLayout();
    }

    public void setRecycled(boolean recycled) {
        mRecycled = recycled;
    }

    public static class ItemViewHolder {
        public @InjectView(R.id.main_top_feed_image_view)  ImageView imageView;
        public @InjectView(R.id.main_top_news_title)       TextView titleTextView;
        public @InjectView(R.id.main_top_item_progress)    android.widget.ProgressBar progressBar;

        public ItemViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}