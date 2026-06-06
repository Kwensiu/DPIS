package com.dpis.module;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;

import androidx.core.widget.NestedScrollView;

public final class MaxHeightNestedScrollView extends NestedScrollView {
    private int maxHeightPx;
    private float maxHeightFraction;

    public MaxHeightNestedScrollView(Context context) {
        this(context, null);
    }

    public MaxHeightNestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        maxHeightPx = readMaxHeight(context, attrs, 0);
        maxHeightFraction = readMaxHeightFraction(context, attrs, 0);
    }

    public MaxHeightNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        maxHeightPx = readMaxHeight(context, attrs, defStyleAttr);
        maxHeightFraction = readMaxHeightFraction(context, attrs, defStyleAttr);
    }

    void setMaxHeightFraction(float fraction) {
        maxHeightFraction = Math.max(0f, fraction);
        requestLayout();
    }

    void setMaxHeightPx(int maxHeightPx) {
        this.maxHeightPx = Math.max(0, maxHeightPx);
        requestLayout();
    }

    private static int readMaxHeight(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(
                attrs,
                R.styleable.MaxHeightNestedScrollView,
                defStyleAttr,
                0);
        try {
            return array.getDimensionPixelSize(
                    R.styleable.MaxHeightNestedScrollView_android_maxHeight, 0);
        } finally {
            array.recycle();
        }
    }

    private static float readMaxHeightFraction(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(
                attrs,
                R.styleable.MaxHeightNestedScrollView,
                defStyleAttr,
                0);
        try {
            return array.getFloat(R.styleable.MaxHeightNestedScrollView_maxHeightFraction, 0f);
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int constrainedHeightMeasureSpec = heightMeasureSpec;
        int constrainedMaxHeight = resolveConstrainedMaxHeight(heightMeasureSpec);
        if (constrainedMaxHeight > 0) {
            int mode = MeasureSpec.getMode(constrainedHeightMeasureSpec);
            int size = MeasureSpec.getSize(constrainedHeightMeasureSpec);
            if (mode == MeasureSpec.UNSPECIFIED || size > constrainedMaxHeight) {
                constrainedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        constrainedMaxHeight,
                        MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, constrainedHeightMeasureSpec);
    }

    private int resolveConstrainedMaxHeight(int heightMeasureSpec) {
        int constrainedMaxHeight = maxHeightPx;
        if (maxHeightFraction > 0f) {
            int mode = MeasureSpec.getMode(heightMeasureSpec);
            int size = MeasureSpec.getSize(heightMeasureSpec);
            if (mode != MeasureSpec.UNSPECIFIED && size > 0) {
                int fractionHeight = Math.round(size * maxHeightFraction);
                constrainedMaxHeight = constrainedMaxHeight > 0
                        ? Math.min(constrainedMaxHeight, fractionHeight)
                        : fractionHeight;
            }
        }
        return constrainedMaxHeight;
    }
}
