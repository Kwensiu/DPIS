package com.dpis.module;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;

import androidx.core.widget.NestedScrollView;

public final class MaxHeightNestedScrollView extends NestedScrollView {
    private final int maxHeightPx;

    public MaxHeightNestedScrollView(Context context) {
        this(context, null);
    }

    public MaxHeightNestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        maxHeightPx = readMaxHeight(context, attrs, 0);
    }

    public MaxHeightNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        maxHeightPx = readMaxHeight(context, attrs, defStyleAttr);
    }

    private static int readMaxHeight(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(
                attrs,
                new int[] { android.R.attr.maxHeight },
                defStyleAttr,
                0);
        try {
            return array.getDimensionPixelSize(0, 0);
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int constrainedHeightMeasureSpec = heightMeasureSpec;
        if (maxHeightPx > 0) {
            int mode = MeasureSpec.getMode(heightMeasureSpec);
            int size = MeasureSpec.getSize(heightMeasureSpec);
            if (mode == MeasureSpec.UNSPECIFIED || size > maxHeightPx) {
                constrainedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        maxHeightPx,
                        MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, constrainedHeightMeasureSpec);
    }
}
