package com.yooiistudios.news.ui.animation;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.os.Build;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.PathInterpolator;

import com.yooiistudios.news.R;
import com.yooiistudios.news.model.Settings;
import com.yooiistudios.news.util.InterpolatorHelper;

/**
 * Created by Wooseong Kim in News-Android-L from Yooii Studios Co., LTD. on 2014. 9. 19.
 *
 * AnimationFactory
 *  필요한 애니메이션을 제작해주는 클래스
 */
public class AnimationFactory {
    private AnimationFactory() { throw new AssertionError("You MUST not create this class!"); }

    public static Animation makeBottomFadeOutAnimation(Context context) {
        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        // 속도에 따라 duration 조절
        int originalDuration = context.getResources().getInteger(
                R.integer.bottom_news_feed_fade_anim_duration_milli);
        animation.setDuration((long) (originalDuration * Settings.getAutoRefreshSpeed(context)));
        animation.setFillEnabled(true);
        animation.setFillAfter(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animation.setInterpolator(context, R.animator.interpolator_bottom_fade);
        } else {
            animation.setInterpolator(new CubicBezierInterpolator(.57f, .15f, .65f, .67f));
        }
        return animation;
    }

    public static Animation makeBottomFadeInAnimation(Context context) {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        int originalDuration = context.getResources().getInteger(
                R.integer.bottom_news_feed_fade_anim_duration_milli);
        animation.setDuration((long) (originalDuration * Settings.getAutoRefreshSpeed(context)));
        animation.setFillEnabled(true);
        animation.setFillAfter(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animation.setInterpolator(context, R.animator.interpolator_bottom_fade);
        } else {
            animation.setInterpolator(new CubicBezierInterpolator(.57f, .15f, .65f, .67f));
        }

        return animation;
    }

    public static TimeInterpolator makeDefaultPathInterpolator() {
        TimeInterpolator interpolator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            interpolator = new PathInterpolator(.4f, .0f, 1.f, .2f);
        } else {
            interpolator = new CubicBezierInterpolator(.4f, .0f, 1.f, .2f);
        }
        return interpolator;
    }

    public static TimeInterpolator makeDefaultReversePathInterpolator() {
        TimeInterpolator interpolator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            interpolator = new PathInterpolator(.0f, .4f, .2f, 1.f);
        } else {
            interpolator = new CubicBezierInterpolator(.0f, .4f, .2f, 1.f);
        }
        return interpolator;
    }

    // slow-out-slow-in
    public static TimeInterpolator makeNewsFeedImageAndRootTransitionInterpolator(Context context) {
        return InterpolatorHelper.makeImageAndRootTransitionInterpolator(context);
    }

    public static TimeInterpolator makeNewsFeedImageScaleInterpolator(Context context) {
        return InterpolatorHelper.makeImageScaleInterpolator(context);
    }

    // fast-out-slow-in
    public static TimeInterpolator makeNewsFeedRootBoundHorizontalInterpolator(Context context) {
        return InterpolatorHelper.makeRootWidthScaleInterpolator(context);
    }

    // ease-in-out
    public static TimeInterpolator makeNewsFeedRootBoundVerticalInterpolator(Context context) {
        return InterpolatorHelper.makeRootHeightScaleInterpolator(context);
    }

    public static TimeInterpolator makeNewsFeedReverseTransitionInterpolator() {
        TimeInterpolator interpolator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            interpolator = new PathInterpolator(.52f, .22f, 1.f, .21f);
        } else {
            interpolator = new CubicBezierInterpolator(.52f, .22f, 1.f, .21f);
        }
        return interpolator;
    }

    public static TimeInterpolator makeViewPagerScrollInterpolator() {
        TimeInterpolator interpolator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            interpolator = new PathInterpolator(0.15f, 0.12f, 0.24f, 1.0f);
        } else {
            interpolator = new CubicBezierInterpolator(0.15f, 0.12f, 0.24f, 1.0f);
        }
        return interpolator;
    }
}
