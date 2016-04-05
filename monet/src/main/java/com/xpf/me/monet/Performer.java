package com.xpf.me.monet;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by pengfeixie on 16/4/3.
 */
public abstract class Performer<T> {

    public final Monet monet;
    public final T target;
    public final int reqWidth, reqHeight;
    public final String url;
    public Drawable drawable;
    public boolean cancelled;

    public Performer(Monet monet, T target, String url, int reqWidth, int reqHeight, Drawable drawable) {
        this.drawable = drawable;
        this.monet = monet;
        this.target = target;
        this.url = url;
        this.reqWidth = reqWidth;
        this.reqHeight = reqHeight;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public abstract void complete(Bitmap result);

    public T getTarget() {
        return target;
    }
}

