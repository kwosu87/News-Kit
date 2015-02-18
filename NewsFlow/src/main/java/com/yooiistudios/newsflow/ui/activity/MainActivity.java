package com.yooiistudios.newsflow.ui.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.yooiistudios.newsflow.NewsApplication;
import com.yooiistudios.newsflow.R;
import com.yooiistudios.newsflow.iab.IabProducts;
import com.yooiistudios.newsflow.model.BackgroundServiceUtils;
import com.yooiistudios.newsflow.model.Settings;
import com.yooiistudios.newsflow.model.database.NewsDb;
import com.yooiistudios.newsflow.model.news.News;
import com.yooiistudios.newsflow.model.news.util.NewsFeedArchiveUtils;
import com.yooiistudios.newsflow.ui.fragment.SettingFragment;
import com.yooiistudios.newsflow.ui.widget.MainAdView;
import com.yooiistudios.newsflow.ui.widget.MainBottomContainerLayout;
import com.yooiistudios.newsflow.ui.widget.MainRefreshLayout;
import com.yooiistudios.newsflow.ui.widget.MainTopContainerLayout;
import com.yooiistudios.newsflow.util.AdDialogFactory;
import com.yooiistudios.newsflow.util.AdUtils;
import com.yooiistudios.newsflow.util.AnalyticsUtils;
import com.yooiistudios.newsflow.util.AppValidationChecker;
import com.yooiistudios.newsflow.util.ConnectivityUtils;
import com.yooiistudios.newsflow.util.FacebookUtils;
import com.yooiistudios.newsflow.util.NLLog;
import com.yooiistudios.newsflow.util.ReviewUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity
        implements MainTopContainerLayout.OnMainTopLayoutEventListener,
        MainBottomContainerLayout.OnMainBottomLayoutEventListener {
    public static final String TAG = MainActivity.class.getName();
    public static final String INTENT_KEY_TINT_TYPE = "INTENT_KEY_TINT_TYPE";

    // 뉴스 새로고침시 사용할 인텐트 변수
    public static final String INTENT_KEY_NEWS_FEED_LOCATION = "INTENT_KEY_NEWS_FEED_LOCATION";
    public static final String INTENT_VALUE_TOP_NEWS_FEED = "INTENT_VALUE_TOP_NEWS_FEED";
    public static final String INTENT_VALUE_BOTTOM_NEWS_FEED = "INTENT_VALUE_BOTTOM_NEWS_FEED";
    public static final String INTENT_KEY_BOTTOM_NEWS_FEED_INDEX = "INTENT_KEY_BOTTOM_NEWS_FEED_INDEX";

    // 액티비티 트랜지션시 이미지뷰 애니메이트를 위한 변수를 넘길 인텐트 변수
    public static final String INTENT_KEY_TRANSITION_PROPERTY = "INTENT_KEY_TRANSITION_PROPERTY";

    public static final int RC_NEWS_FEED_DETAIL = 10001;
    public static final int RC_SETTING = 10002;

    /**
     * Auto Refresh Handler
     */
    // auto refresh handler
    private static final int AUTO_REFRESH_HANDLER_DELAY = 7 * 1000; // finally to be 10 secs
    private boolean mIsHandlerRunning = false;
    private NewsAutoRefreshHandler mNewsAutoRefreshHandler = new NewsAutoRefreshHandler();

    private static final int INVALID_WINDOW_INSET = -1;

    @InjectView(R.id.main_root_layout)              RelativeLayout mRootView;
    @InjectView(R.id.main_loading_container)        ViewGroup mLoadingContainer;
    @InjectView(R.id.main_loading_log)              TextView mLoadingLog;
    @InjectView(R.id.main_loading_image_view)       ImageView mLoadingImageView;
    @InjectView(R.id.main_scroll_view)              ScrollView mScrollView;
    @InjectView(R.id.main_scrolling_content)        View mScrollingContent;
    @InjectView(R.id.main_swipe_refresh_layout)     MainRefreshLayout mSwipeRefreshLayout;
    @InjectView(R.id.main_top_layout_container)     MainTopContainerLayout mMainTopContainerLayout;
    @InjectView(R.id.main_bottom_layout_container)  MainBottomContainerLayout mMainBottomContainerLayout;
//    @InjectView(R.id.main_adView)                   AdView mAdView;

    private MainAdView mBannerAd;

    // Quit Ad Dialog
    private AdRequest mQuitAdRequest;
    private AdView mQuitAdView;

    private int mSystemWindowInsetBottom = INVALID_WINDOW_INSET;
    private int mSystemWindowInsetRight = INVALID_WINDOW_INSET;

    private class NewsAutoRefreshHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // 갱신
            mMainTopContainerLayout.autoRefreshTopNewsFeed();
            mMainBottomContainerLayout.autoRefreshBottomNewsFeeds();

            // tick 의 동작 시간을 계산해서 정확히 틱 초마다 UI 갱신을 요청할 수 있게 구현
            mNewsAutoRefreshHandler.sendEmptyMessageDelayed(0,
                    Settings.getAutoRefreshHandlerDelay(MainActivity.this));
        }
    }

    /**
     * init
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (NLLog.isDebug()) {
            AppValidationChecker.validationCheck(this);
        }

        // start service on starting app
        NLLog.i("BackgroundServiceUtils", "onCreate");
        BackgroundServiceUtils.startService(getApplicationContext());

        // TODO off-line configuration
        boolean needsRefresh = NewsFeedArchiveUtils.newsNeedsToBeRefreshed(getApplicationContext());
        boolean isOnline = ConnectivityUtils.isNetworkAvailable(getApplicationContext());
        if (needsRefresh && !isOnline) {
            initNetworkUnavailableCoverLayout();
            return;
        }

        init();
    }

    private void init() {
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // TODO ConcurrentModification 문제 우회를 위해 애니메이션이 끝나기 전 스크롤을 막던지 처리 해야함.
        initRefreshLayout();
        initBannerAdView();
        initQuitAdView();
        mMainTopContainerLayout.init(this);
        mMainBottomContainerLayout.init(this);
        bringLoadingContainerToFront();
        showMainContentIfReady();
        requestSystemWindowsBottomInset();

        AdUtils.showPopupAdIfSatisfied(this);
        AnalyticsUtils.startAnalytics((NewsApplication) getApplication(), TAG);
    }

    private void initRefreshLayout() {
        mSwipeRefreshLayout.setColorSchemeResources(R.color.app_color_accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mMainTopContainerLayout.isReady() &&
                        !mMainBottomContainerLayout.isRefreshingBottomNewsFeeds()) {
                    stopNewsAutoRefresh();
                    setSwipeRefreshLayoutEnabled(false);

                    mMainTopContainerLayout.refreshNewsFeedOnSwipeDown();
                    mMainBottomContainerLayout.refreshBottomNewsFeeds();
                }
            }
        });

        // 초기 로딩시 swipe refresh 가 되지 않도록 설정
        setSwipeRefreshLayoutEnabled(false);
    }

    private void bringLoadingContainerToFront() {
        mLoadingContainer.bringToFront();
    }

    private void initNetworkUnavailableCoverLayout() {
        @SuppressLint("InflateParams")
        View networkUnavailableCoverLayout = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.network_unavailable_cover, null);
        setContentView(networkUnavailableCoverLayout);

        findViewById(R.id.network_unavailable_reload).setOnClickListener(new View.OnClickListener() {
            private int mPressCount = 0;

            @Override
            public void onClick(View v) {
                mPressCount++;
                boolean isNetworkAvailable = ConnectivityUtils.isNetworkAvailable(getApplicationContext());
                if (isNetworkAvailable) {
                    init();
                } else {
                    if (mPressCount > 5) {
                        TextView networkUnavailableMessageTextView
                                = (TextView) findViewById(R.id.network_unavailable_message);
                        int lineCount = networkUnavailableMessageTextView.getLineCount();
                        String messageToAppend = "\n";
                        switch (lineCount % 9) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                messageToAppend += "안돼";
                                break;
                            case 6:
                                messageToAppend += "(｀Д´#)";
                                break;
                            case 7:
                                messageToAppend += "(#｀Д´)";
                                break;
                            case 8:
                                messageToAppend += "(#`・д・)";
                                break;
                            case 0:
                            default:
                                messageToAppend += "(´ﾟДﾟ`)";
                                break;
                        }
                        networkUnavailableMessageTextView.append(messageToAppend);
                    }
                }
            }
        });
    }

    private void initBannerAdView() {
        mBannerAd = new MainAdView(this);
        mBannerAd.setId(R.id.main_banner);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mRootView.addView(mBannerAd, lp);
    }

    private void configBannerAdOnInsetChanged() {
        if (isPortrait()) {
            mBannerAd.applyBottomMarginOnPortrait(mSystemWindowInsetBottom);
        } else {
            mBannerAd.applyBottomMarginOnPortrait(0);
        }

        bringLoadingContainerToFront();
    }

//    private void configSwipeRefreshLayoutOnOrientationChanged() {
//        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mSwipeRefreshLayout.getLayoutParams();
//        if (isPortrait()) {
//            lp.addRule(RelativeLayout.ABOVE, View.NO_ID);
//        } else {
//            lp.addRule(RelativeLayout.ABOVE, mBannerAd.getId());
//        }
//    }

    private void initQuitAdView() {
        // make AdView earlier for showing ad fast in the quit dialog
        mQuitAdRequest = new AdRequest.Builder().build();
        mQuitAdView = AdDialogFactory.initAdView(this, mQuitAdRequest);
    }

    private void configOnSystemInsetChanged() {
        // NO_ADS 만 체크해도 풀버전까지 체크됨
        boolean adPurchased = IabProducts.containsSku(getApplicationContext(), IabProducts.SKU_NO_ADS);
        if (adPurchased) {
            if (isPortrait()) {
                mScrollingContent.setPadding(0, 0, 0, mSystemWindowInsetBottom);
            } else {
                mScrollingContent.setPadding(0, 0, mSystemWindowInsetRight, 0);
            }
            mBannerAd.hide();
//            mBannerAd.setVisibility(View.GONE);
        } else {
            configBannerAdOnInsetChanged();
//            configSwipeRefreshLayoutOnOrientationChanged();
            if (isPortrait()) {
//                int adViewHeight = AdSize.SMART_BANNER.getHeightInPixels(getApplicationContext());
                int adViewHeight = getResources().getDimensionPixelSize(R.dimen.admob_smart_banner_height);
                mScrollingContent.setPadding(0, 0, 0, mSystemWindowInsetBottom + adViewHeight);
            } else {
                mScrollingContent.setPadding(0, 0, mSystemWindowInsetRight, 0);
            }
            mBannerAd.show();
//            mBannerAd.setVisibility(View.VISIBLE);

            mBannerAd.resume();
            mQuitAdView.resume();
        }
//        configNavigationTranslucentState();
    }

    private void configNavigationTranslucentState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean adPurchased = IabProducts.containsSku(getApplicationContext(), IabProducts.SKU_NO_ADS);
            if (adPurchased && isPortrait()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
        }
    }

    public ViewGroup getMainTopContainerLayout() {
        return mMainTopContainerLayout;
    }

    private void setSwipeRefreshLayoutEnabled(boolean enable) {
        if (isPortrait()) {
            boolean readyForRefresh = mMainTopContainerLayout.isReady()
                    && !mMainBottomContainerLayout.isRefreshingBottomNewsFeeds();
            mSwipeRefreshLayout.setEnabled(enable && readyForRefresh);
        } else {
            mSwipeRefreshLayout.setEnabled(false);
        }
    }

    private void requestSystemWindowsBottomInset() {
        configNavigationTranslucentState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isSystemWindowInsetInvalid()) {
                requestSystemWindowsBottomInsetAfterLollipop();
            } else {
                configOnSystemInsetChanged();
            }
        } else {
            setSystemWindowInset(0, 0);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void requestSystemWindowsBottomInsetAfterLollipop() {
        // TODO 아래 코드에 대해
        // TODO 1. 있는 경우 모든 inset 이 0으로 들어옴.
        // TODO 2. 없는 경우 필요한 inset 값이 들어온다.
        // TODO 나중에 최적화? 정리? 해야함
        mScrollingContent.setFitsSystemWindows(true);
        mScrollingContent.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                setSystemWindowInset(
                        windowInsets.getSystemWindowInsetRight(),
                        windowInsets.getSystemWindowInsetBottom());
//                configOnSystemInsetChanged(); // onResume 보다 늦게 호출되기에 최초 한 번은 여기서 확인이 필요
                return windowInsets.consumeSystemWindowInsets();
            }
        });
    }

    private void setSystemWindowInset(int rightInset, int bottomInset) {
        mSystemWindowInsetRight = rightInset;
        mSystemWindowInsetBottom = bottomInset;
        configOnSystemInsetChanged();
    }

    private boolean isSystemWindowInsetInvalid() {
        return mSystemWindowInsetRight == INVALID_WINDOW_INSET
                || mSystemWindowInsetBottom == INVALID_WINDOW_INSET;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRootView != null) {
            onConfigurationChanged(getResources().getConfiguration());
            startNewsAutoRefreshIfReady();
        }

        // 화면 켜짐 유지 설정
        SharedPreferences preferences = getSharedPreferences(
                SettingFragment.KEEP_SCREEN_ON_PREFS, Context.MODE_PRIVATE);
        if (preferences.getBoolean(SettingFragment.KEEP_SCREEN_ON_KEY, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        if (mRootView != null) {
            mBannerAd.pause();
            mQuitAdView.pause();
            stopNewsAutoRefresh();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        SubMenu subMenu = menu.addSubMenu(Menu.NONE, R.id.action_newsfeed_overflow, 0, "");
        subMenu.setIcon(R.drawable.ic_menu_moreoverflow_mtrl_alpha);

        subMenu.add(Menu.NONE, R.id.action_store, 0, R.string.store);
        subMenu.add(Menu.NONE, R.id.action_info, 1, R.string.action_info);
        subMenu.add(Menu.NONE, R.id.action_settings, 2, R.string.action_settings);
        subMenu.add(Menu.NONE, R.id.action_rate_app, 3, R.string.action_rate_app);
        subMenu.add(Menu.NONE, R.id.action_facebook_like, 4, R.string.action_facebook_like);

        if (NLLog.isDebug()) {
            subMenu.add(Menu.NONE, R.id.action_remove_archive, 5, "Remove archive(Debug)");
            subMenu.add(Menu.NONE, R.id.action_slow_anim, 6, "Slow Activity Transition(Debug)");
            subMenu.add(Menu.NONE, R.id.action_service_log, 7, "Show service log(Debug)");
        }
        MenuItemCompat.setShowAsAction(subMenu.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_store) {
            startActivity(new Intent(MainActivity.this, StoreActivity.class));
            return true;
        } else if (id == R.id.action_info) {
            startActivity(new Intent(MainActivity.this, InfoActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivityForResult(new Intent(MainActivity.this, SettingActivity.class), RC_SETTING);
            return true;
        } else if (id == R.id.action_rate_app) {
            ReviewUtils.showReviewActivity(this);
            return true;
        } else if (id == R.id.action_facebook_like) {
            FacebookUtils.openYooiiPage(this);
            return true;
        // 여기서부터 debug 용
        } else if (id == R.id.action_remove_archive) {
            // 젠킨스 한글 설정 테스트용 주석
            NewsDb.getInstance(getApplicationContext()).clearArchive();
            NewsFeedArchiveUtils.clearArchive(getApplicationContext());
        } else if (id == R.id.action_slow_anim) {
            NewsFeedDetailActivity.sAnimatorScale = item.isChecked() ?
                    1 : getResources().getInteger(R.integer.news_feed_detail_debug_transition_scale);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_service_log) {
            BackgroundServiceUtils.showDialog(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMainTopInitialLoad() {
        showMainContentIfReady();
        startNewsAutoRefreshIfReady();
    }

    @Override
    public void onMainTopRefresh() {
        configAfterRefreshDone();
    }

    @Override
    public void onMainBottomInitialLoad() {
        showMainContentIfReady();
        startNewsAutoRefreshIfReady();
    }

    @Override
    public void onMainBottomRefresh() {
        configAfterRefreshDone();
    }

    @Override
    public void onMainBottomNewsImageInitiallyAllFetched() {
        startNewsAutoRefreshIfReady();
    }

    @Override
    public void onMainBottomNewsReplaceDone() {
        startNewsAutoRefreshIfReady();
    }

    @Override
    public void onMainBottomMatrixChanged() {
        startNewsAutoRefreshIfReady();
    }

    private void startNewsAutoRefreshIfReady() {
        if (mMainTopContainerLayout.isReady()
                && mMainBottomContainerLayout.isInitialized()
                && mMainBottomContainerLayout.isInitializedFirstImages()
                && !mMainBottomContainerLayout.isReplacingBottomNewsFeed()
                && !mMainBottomContainerLayout.isRefreshingBottomNewsFeeds()
                && !mMainBottomContainerLayout.isFetchingAddedBottomNewsFeeds()) {
            startNewsAutoRefresh();
        }
    }

    private void startNewsAutoRefresh() {
        if (mIsHandlerRunning) {
            return;
        }
        mIsHandlerRunning = true;
        mNewsAutoRefreshHandler.sendEmptyMessageDelayed(0, Settings.getAutoRefreshHandlerDelay(this));
    }

    private void stopNewsAutoRefresh() {
        if (!mIsHandlerRunning) {
            return;
        }
        mIsHandlerRunning = false;
        mNewsAutoRefreshHandler.removeMessages(0);
    }

    private void showMainContentIfReady() {
        boolean topReady = mMainTopContainerLayout.isReady();
        boolean bottomReady = mMainBottomContainerLayout.isInitialized();

        String loadingStatus = "Top news feed ready : " + topReady
                + "\nBottom news feed ready : " + bottomReady;

        mLoadingLog.setText(loadingStatus);
        if (mLoadingImageView.getBackground() instanceof AnimationDrawable) {
            AnimationDrawable animation = (AnimationDrawable) mLoadingImageView.getBackground();
            if (!animation.isRunning()) {
                animation.start();
            }
        }

        if (mLoadingContainer.getVisibility() == View.GONE) {
            return;
        }

        if (topReady && bottomReady) {
            mMainTopContainerLayout.animateOnInit();
            mMainBottomContainerLayout.animateBottomNewsFeedListOnInit();

            mSwipeRefreshLayout.setRefreshing(false);

            setSwipeRefreshLayoutEnabled(true);

            NewsFeedArchiveUtils.saveRecentCacheMillisec(getApplicationContext());

            // loaded
            if (mLoadingImageView.getBackground() instanceof AnimationDrawable) {
                AnimationDrawable animation = (AnimationDrawable) mLoadingImageView.getBackground();
                animation.stop();
            }
            mLoadingContainer.setVisibility(View.GONE);
        }
    }

    private void configAfterRefreshDone() {
        if (mMainTopContainerLayout.isReady() &&
                !mMainBottomContainerLayout.isRefreshingBottomNewsFeeds()) {
            // dismiss loading progress bar
            mSwipeRefreshLayout.setRefreshing(false);
            setSwipeRefreshLayoutEnabled(true);
            NewsFeedArchiveUtils.saveRecentCacheMillisec(getApplicationContext());
            startNewsAutoRefresh();
        }
    }

    private void configOnPortraitOrientation() {
        RelativeLayout.LayoutParams bottomLayoutParams =
                ((RelativeLayout.LayoutParams)mMainBottomContainerLayout.getLayoutParams());

        bottomLayoutParams.addRule(RelativeLayout.BELOW, mMainTopContainerLayout.getId());
        bottomLayoutParams.addRule(RelativeLayout.RIGHT_OF, 0);

        setSwipeRefreshLayoutEnabled(true);

        mScrollView.setEnabled(true);
    }

    private void configOnLandscapeOrientation() {
        RelativeLayout.LayoutParams bottomLayoutParams =
                ((RelativeLayout.LayoutParams)mMainBottomContainerLayout.getLayoutParams());

        bottomLayoutParams.addRule(RelativeLayout.BELOW, 0);
        bottomLayoutParams.addRule(RelativeLayout.RIGHT_OF, mMainTopContainerLayout.getId());

        setSwipeRefreshLayoutEnabled(false);

        mScrollView.setEnabled(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // TODO 여기서 회전시 광고 설정 해줘야 함
        requestSystemWindowsBottomInset();

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            configOnPortraitOrientation();
        } else {
            configOnLandscapeOrientation();
        }

        mMainTopContainerLayout.configOnOrientationChange();
        mMainBottomContainerLayout.configOnOrientationChange();
    }

    private boolean isPortrait() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            switch (requestCode) {
                case RC_NEWS_FEED_DETAIL:
                    boolean hasNewsFeedReplaced = extras.getBoolean(
                            NewsFeedDetailActivity.INTENT_KEY_NEWSFEED_REPLACED, false);
                    String newsFeedType = extras.getString(INTENT_KEY_NEWS_FEED_LOCATION, null);
                    boolean newImageLoaded = extras.getBoolean(
                            NewsFeedDetailActivity.INTENT_KEY_IMAGE_LOADED, false);

                    if (newsFeedType == null) {
                        return;
                    }
                    if (hasNewsFeedReplaced) {
                        // 교체된게 top news feed 인지 bottom news feed 인지 구분
                        if (newsFeedType.equals(INTENT_VALUE_BOTTOM_NEWS_FEED)) {
                            // bottom news feed 중 하나가 교체됨

                            // bottom news feed 의 index 를 가져옴
                            int idx = extras.getInt(INTENT_KEY_BOTTOM_NEWS_FEED_INDEX, -1);
                            if (idx >= 0) {
                                mMainBottomContainerLayout.reloadNewsFeedAt(idx);
                            }
                        } else if (newsFeedType.equals(INTENT_VALUE_TOP_NEWS_FEED)) {
                            // top news feed 가 교체됨
                            mMainTopContainerLayout.configOnNewsFeedReplaced();
                        }
                    } else if (newImageLoaded) {
                        String imgUrl = extras.getString(
                                NewsFeedDetailActivity.INTENT_KEY_IMAGE_URL, null);

                        int newsIndex = extras.getInt(News.KEY_CURRENT_NEWS_INDEX, -1);
                        if (newsFeedType.equals(INTENT_VALUE_BOTTOM_NEWS_FEED)) {
                            int newsFeedIndex = extras.getInt(INTENT_KEY_BOTTOM_NEWS_FEED_INDEX, -1);
                            if (newsFeedIndex >= 0 && newsIndex >= 0) {
                                mMainBottomContainerLayout.configOnNewsImageUrlLoadedAt(
                                        imgUrl, newsFeedIndex, newsIndex);
                            }
                        } else if (newsFeedType.equals(INTENT_VALUE_TOP_NEWS_FEED)) {
                            if (newsIndex >= 0) {
                                mMainTopContainerLayout.configOnNewsImageUrlLoadedAt(imgUrl, newsIndex);

                                mSwipeRefreshLayout.setRefreshing(false);
                                setSwipeRefreshLayoutEnabled(true);

                                startNewsAutoRefreshIfReady();
                            }
                        }
                    }

                    break;
                case RC_SETTING:
                    boolean panelMatrixChanged = extras.getBoolean(SettingActivity.PANEL_MATRIX_CHANGED);

                    if (panelMatrixChanged) {
                        mMainBottomContainerLayout.notifyPanelMatrixChanged();
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!IabProducts.containsSku(this, IabProducts.SKU_NO_ADS)
                && ConnectivityUtils.isNetworkAvailable(getApplicationContext())
                && mRootView != null) {
            AlertDialog adDialog = AdDialogFactory.makeAdDialog(MainActivity.this, mQuitAdView);
            if (adDialog != null) {
                adDialog.show();
                // make AdView again for next quit dialog
                // prevent child reference
                mQuitAdView = AdDialogFactory.initAdView(this, mQuitAdRequest);
            } else {
                // just finish activity when dialog is null
                super.onBackPressed();
            }
        } else {
            // just finish activity when no ad item is bought
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        // Activity visible to user
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        // Activity no longer visible
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }
}
