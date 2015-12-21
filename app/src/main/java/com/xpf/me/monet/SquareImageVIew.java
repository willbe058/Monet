package com.xpf.me.monet;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by xgo on 12/22/15.
 */
public class SquareImageVIew extends ImageView {
    public SquareImageVIew(Context context) {
        super(context);
    }

    public SquareImageVIew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageVIew(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
