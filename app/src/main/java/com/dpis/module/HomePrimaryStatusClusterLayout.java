package com.dpis.module;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public final class HomePrimaryStatusClusterLayout extends LinearLayout {
    public HomePrimaryStatusClusterLayout(Context context) {
        super(context);
        init();
    }

    public HomePrimaryStatusClusterLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomePrimaryStatusClusterLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // The update action card is laid out below the primary card, then drawn behind it
        // so the expanded state reads like a tucked card rather than an overlay.
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        int primaryIndex = findPrimaryCardIndex();
        if (primaryIndex < 0 || primaryIndex == childCount - 1) {
            return super.getChildDrawingOrder(childCount, drawingPosition);
        }
        if (drawingPosition == childCount - 1) {
            return primaryIndex;
        }
        return drawingPosition >= primaryIndex ? drawingPosition + 1 : drawingPosition;
    }

    private int findPrimaryCardIndex() {
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index).getId() == R.id.home_primary_status_card) {
                return index;
            }
        }
        return -1;
    }
}
