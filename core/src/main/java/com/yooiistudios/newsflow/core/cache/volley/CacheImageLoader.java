package com.yooiistudios.newsflow.core.cache.volley;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.IntDef;
import android.support.v4.app.FragmentActivity;
import android.support.v7.graphics.Palette;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.yooiistudios.newsflow.core.news.ImageRequestQueue;
import com.yooiistudios.newsflow.core.news.SimpleImageCache;
import com.yooiistudios.newsflow.core.news.database.NewsDb;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dongheyon Jeong in News Flow from Yooii Studios Co., LTD. on 15. 3. 24.
 *
 * ResizedImageLoader
 *  리사이징을 강제하는 이미지 로더
 */
public abstract class CacheImageLoader {
    public interface ImageListener {
        void onSuccess(ImageResponse response);
        void onFail(VolleyError error);
    }

    private interface ThumbnailListener {
        void onSuccess(Bitmap resizedBitmap);
    }

    private interface PaletteListener {
        void onSuccess(ImageResponse.PaletteColor paletteColor);
    }

    private static final int REQUESTED = 0;
    private static final int CANCEL_REQUESTED = 1;
//    private static final int CANCELLED = 2;

    private Context mContext;
    private ImageLoader mImageLoader;
    private ImageCache mCache;
//    private Point mImageSize;
//    private List<String> mUrlsToCancel = new ArrayList<>();
//    private Map<String, Integer> mRequestedUrls = new HashMap<>();
    private Map<UrlSupplier, Integer> mRequestedUrlSuppliers = new HashMap<>();

    protected CacheImageLoader(FragmentActivity activity) {
        mContext = activity.getApplicationContext();
        initImageLoader(activity);
//        initImageSize(activity.getApplicationContext());
    }

    protected CacheImageLoader(Context context) {
        mContext = context;
        initImageLoaderWithNonRetainingCache(context);
//        initImageSize(context);
    }

    private void initImageLoader(FragmentActivity activity) {
        RequestQueue requestQueue =
                ImageRequestQueue.getInstance(activity.getApplicationContext()).getRequestQueue();
        mCache = SimpleImageCache.getInstance().get(activity);
        mImageLoader = new ImageLoader(requestQueue, SimpleImageCache.getInstance().get(activity));
    }

    private void initImageLoaderWithNonRetainingCache(Context context) {
        RequestQueue requestQueue = ImageRequestQueue.getInstance(context.getApplicationContext())
                .getRequestQueue();
        mCache = SimpleImageCache.getInstance().getNonRetainingCache(context);
        mImageLoader = new ImageLoader(requestQueue, mCache);
    }

    public void get(String requestUrl, ImageListener imageListener) {
        ImageRequest request = new ImageRequest(
                createSimpleUrlSupplier(requestUrl),
                ImageRequest.TYPE_LARGE
        );
        get(request, imageListener);
    }

    public void getThumbnail(String requestUrl, ImageListener imageListener) {
        ImageRequest request = new ImageRequest(
                createSimpleUrlSupplier(requestUrl),
                ImageRequest.TYPE_THUMBNAIL
        );
        get(request, imageListener);
    }

    public void getThumbnail(UrlSupplier urlSupplier, ImageListener imageListener) {
        ImageRequest request = new ImageRequest(urlSupplier, ImageRequest.TYPE_THUMBNAIL);
        get(request, imageListener);
    }

    public void cancelRequest(String url) {
        cancelRequest(createSimpleUrlSupplier(url));
    }

    private SimpleUrlSupplier createSimpleUrlSupplier(String url) {
        return new SimpleUrlSupplier(url);
    }

    public void cancelRequest(UrlSupplier urlSupplier) {
        if (mRequestedUrlSuppliers.containsKey(urlSupplier)
                && mRequestedUrlSuppliers.get(urlSupplier).equals(REQUESTED)) {
            mRequestedUrlSuppliers.put(urlSupplier, CANCEL_REQUESTED);
//            print();
        }
    }

//    private void print() {
////        NLLog.now("size: " + mRequestedUrlSuppliers.size());
//        for (UrlSupplier supplier : mRequestedUrlSuppliers.keySet()) {
//            mRequestedUrlSuppliers.get(supplier);
////            NLLog.now(supplier.toString());
//            int state = mRequestedUrlSuppliers.get(supplier);
//            String message = state == REQUESTED ? "REQUESTED" : "CANCEL_REQUESTED";
////            NLLog.now("state: " + message);
//        }
//    }

    protected abstract Point getImageSize();

    protected Context getContext() {
        return mContext;
    }

    public ImageCache getCache() {
        return mCache;
    }

    private Bitmap getCachedThumbnail(String url) {
        return mCache.getBitmap(getThumbnailCacheKey(url));
    }

    private void get(final ImageRequest request, final ImageListener imageListener) {
        markRequested(request.urlSupplier);
        if (request.type == ImageRequest.TYPE_THUMBNAIL) {
            final Bitmap bitmap = getCachedThumbnail(request.urlSupplier.getUrl());
            if (bitmap != null) {
                getPaletteColors(request.urlSupplier.getUrl(), bitmap, new PaletteListener() {
                    @Override
                    public void onSuccess(ImageResponse.PaletteColor paletteColor) {
                        notifyOnSuccess(imageListener,
                                new ImageResponse(request.urlSupplier, bitmap, paletteColor));
                    }
                });
            } else {
                getOriginalImage(request, imageListener);
            }
        } else {
            getOriginalImage(request, imageListener);
        }
    }

    private void getOriginalImage(final ImageRequest request, final ImageListener imageListener) {
        mImageLoader.get(request.urlSupplier.getUrl(), new ImageLoader.ImageListener() {

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                final Bitmap bitmap = response.getBitmap();
                if (bitmap != null) {
                    getPaletteColors(request.urlSupplier.getUrl(), bitmap, new PaletteListener() {

                        @Override
                        public void onSuccess(final ImageResponse.PaletteColor paletteColor) {
                            if (request.type == ImageRequest.TYPE_LARGE) {
                                notifyOnSuccess(imageListener,
                                        new ImageResponse(request.urlSupplier, bitmap, paletteColor));
                            }
                            cacheThumbnail(bitmap, request, new ThumbnailListener() {
                                @Override
                                public void onSuccess(Bitmap thumbnailBitmap) {
                                    if (request.type == ImageRequest.TYPE_THUMBNAIL) {
                                        ImageResponse imageResponse = new ImageResponse(
                                                request.urlSupplier,
                                                thumbnailBitmap,
                                                paletteColor
                                        );
                                        notifyOnSuccess(imageListener, imageResponse);
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                notifyOnFail(imageListener, request.urlSupplier, error);
            }
        }, getImageSize().x, getImageSize().y);
    }

    private void getPaletteColors(final String url, Bitmap bitmap, final PaletteListener listener) {
        ImageResponse.PaletteColor paletteColor = NewsDb.getInstance(mContext).loadPaletteColor(url);
        if (paletteColor.isFetched()) {
            listener.onSuccess(paletteColor);
        } else {
            Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    final int vibrantColor = palette.getVibrantColor(ImageResponse.PaletteColor.FALLBACK_COLOR);
                    ImageResponse.PaletteColor paletteColor
                            = new ImageResponse.PaletteColor(vibrantColor, true);
                    NewsDb.getInstance(mContext).savePaletteColor(url, paletteColor);
                    listener.onSuccess(paletteColor);
                }
            });
        }
    }

    private void cacheThumbnail(final Bitmap bitmap, final ImageRequest request,
                                final ThumbnailListener listener) {
        Bitmap thumbnail = getCachedThumbnail(request.urlSupplier.getUrl());
        if (thumbnail == null) {
            int targetWidth = bitmap.getWidth() / 2;
            int targetHeight = bitmap.getHeight() / 2;
            ImageResizer.createScaledBitmap(bitmap, targetWidth, targetHeight, false, false,
                    new ImageResizer.ResizeListener() {
                        @Override
                        public void onResize(Bitmap resizedBitmap) {
                            mCache.putBitmap(getThumbnailCacheKey(request.urlSupplier.getUrl()),
                                    resizedBitmap);
                            listener.onSuccess(resizedBitmap);
                        }
                    });
        }
    }

    private void notifyOnSuccess(ImageListener listener, ImageResponse response) {
        if (!isCancelRequested(response.urlSupplier)) {
            listener.onSuccess(response);
        }
        markDelivered(response.urlSupplier);
    }

    private void notifyOnFail(ImageListener listener, UrlSupplier urlSupplier, VolleyError error) {
        if (!isCancelRequested(urlSupplier)) {
            listener.onFail(error);
        }
        markDelivered(urlSupplier);
    }

//    private boolean isCancelRequested(String url) {
//        return mRequestedUrls.get(url) == CANCELLED;
//    }

    private boolean isCancelRequested(UrlSupplier supplier) {
        return mRequestedUrlSuppliers.containsKey(supplier)
                && mRequestedUrlSuppliers.get(supplier) == CANCEL_REQUESTED;
    }

//    private void markRequested(String url) {
//        mRequestedUrls.put(url, REQUESTED);
//    }

    private void markRequested(UrlSupplier supplier) {
        mRequestedUrlSuppliers.put(supplier, REQUESTED);
    }

//    private void markDelivered(String url) {
//        mRequestedUrls.remove(url);
//    }

    private void markDelivered(UrlSupplier supplier) {
        mRequestedUrlSuppliers.remove(supplier);
//        NLLog.now("markDelivered");
//        print();
    }

//    private void markCancelled(String url) {
//        mRequestedUrls.put(url, CANCELLED);
//    }

//    private void markCancelled(UrlSupplier supplier) {
//        mRequestedUrlSuppliers.put(supplier, CANCELLED);
//    }

    private static String getThumbnailCacheKey(String url) {
        return "th_" + url;
    }

    public void flushCache() {
        CacheAsyncTask.flushCache(mCache);
    }

    public void closeCache() {
        CacheAsyncTask.closeCache(mCache);
    }

    private static class ImageRequest {
        @IntDef(value = {TYPE_LARGE, TYPE_THUMBNAIL})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {}

        public static final int TYPE_LARGE = 0;
        public static final int TYPE_THUMBNAIL = 1;

        private static final int PALETTE_FALLBACK = Color.TRANSPARENT;

        public final UrlSupplier urlSupplier;
        public final @Type int type;
        public int filterColor = PALETTE_FALLBACK;

        public ImageRequest(UrlSupplier urlSupplier, @Type int type) {
            this.urlSupplier = urlSupplier;
            this.type = type;
        }
    }

    public static class ImageResponse {
        public final UrlSupplier urlSupplier;
        public final Bitmap bitmap;
        public final PaletteColor paletteColor;
//        public final int vibrantColor;

        public ImageResponse(UrlSupplier urlSupplier, Bitmap bitmap, PaletteColor paletteColor) {
            this.urlSupplier = urlSupplier;
            this.bitmap = bitmap;
            this.paletteColor = paletteColor;
        }

        public static class PaletteColor {
            public static final int FALLBACK_COLOR = Color.TRANSPARENT;

//            public static final int STATUS_INVALID_VIBRANT_COLOR = 0;
//            public static final int STATUS_VALID_VIBRANT_COLOR = 1;
            public static final int STATUS_FETCHED = 0;
            public static final int STATUS_NOT_FETCHED = 1;

            private final int mVibrantColor;
            private final boolean mIsFetched;

            public PaletteColor(int vibrantColor, boolean isFetched) {
                mVibrantColor = vibrantColor;
                mIsFetched = isFetched;
            }

            public int getVibrantColor() {
                return mVibrantColor;
            }

            public boolean isFetched() {
                return mIsFetched;
            }

            public boolean hasValidVibrantColor() {
                return mVibrantColor != ImageResponse.PaletteColor.FALLBACK_COLOR;
            }

            public static PaletteColor createDefault() {
                return new PaletteColor(FALLBACK_COLOR, false);
            }
        }
    }

    public static abstract class UrlSupplier {
        public abstract String getUrl();

        @Override
        public abstract int hashCode();

        @Override
        public abstract boolean equals(Object o);

        @Override
        public String toString() {
            return "url: " + getUrl();
        }
    }

    private static class SimpleUrlSupplier extends UrlSupplier {
        private String mUrl;

        public SimpleUrlSupplier(String url) {
            mUrl = url;
        }

        @Override
        public String getUrl() {
            return mUrl;
        }

        @Override
        public int hashCode() {
            return mUrl.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SimpleUrlSupplier
                    && getUrl().equals(((SimpleUrlSupplier)o).getUrl());
        }
    }
}