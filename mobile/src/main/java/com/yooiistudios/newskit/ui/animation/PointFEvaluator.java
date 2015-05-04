package com.yooiistudios.newskit.ui.animation;

/**
 * Created by Dongheyon Jeong in News-Android-L from Yooii Studios Co., LTD. on 15. 2. 26.
 *
 * PointFEvaluator
 *  PointF 객체를 사용하는 TypeEvaluator. 구글 내부 코드에서 PointFEvaluator 내용을 복사해옴.
 */


import android.animation.TypeEvaluator;
import android.graphics.PointF;

public class PointFEvaluator implements TypeEvaluator<PointF> {

    /**
     * When null, a new PointF is returned on every evaluate call. When non-null,
     * mPoint will be modified and returned on every evaluate.
     */
    private PointF mPoint;

    /**
     * Construct a PointFEvaluator that returns a new PointF on every evaluate call.
     * To avoid creating an object for each evaluate call,
     * {@link PointFEvaluator#PointFEvaluator(android.graphics.PointF)} should be used
     * whenever possible.
     */
    public PointFEvaluator() {
    }

    /**
     * Constructs a PointFEvaluator that modifies and returns <code>reuse</code>
     * in {@link #evaluate(float, android.graphics.PointF, android.graphics.PointF)} calls.
     * The value returned from
     * {@link #evaluate(float, android.graphics.PointF, android.graphics.PointF)} should
     * not be cached because it will change over time as the object is reused on each
     * call.
     *
     * @param reuse A PointF to be modified and returned by evaluate.
     */
    public PointFEvaluator(PointF reuse) {
        mPoint = reuse;
    }

    /**
     * This function returns the result of linearly interpolating the start and
     * end PointF values, with <code>fraction</code> representing the proportion
     * between the start and end values. The calculation is a simple parametric
     * calculation on each of the separate components in the PointF objects
     * (x, y).
     *
     * <p>If {@link #PointFEvaluator(android.graphics.PointF)} was used to construct
     * this PointFEvaluator, the object returned will be the <code>reuse</code>
     * passed into the constructor.</p>
     *
     * @param fraction   The fraction from the starting to the ending values
     * @param startValue The start PointF
     * @param endValue   The end PointF
     * @return A linear interpolation between the start and end values, given the
     *         <code>fraction</code> parameter.
     */
    @Override
    public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
        float x = startValue.x + (fraction * (endValue.x - startValue.x));
        float y = startValue.y + (fraction * (endValue.y - startValue.y));

        if (mPoint != null) {
            mPoint.set(x, y);
            return mPoint;
        } else {
            return new PointF(x, y);
        }
    }
}
