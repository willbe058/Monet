package com.xpf.me.monet;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.xpf.me.monet.components.bitmaploader.BitmapLoader;
import com.xpf.me.monet.components.bitmaploader.BitmapLoaderImpl;
import com.xpf.me.monet.utils.DebugLog;

/**
 * Created by pengfeixie on 16/4/2.
 */
public class RequestMaker {

    private Monet monet;

    private String url;

    private int reqWidth, reqHeight;

    private Drawable defaultDrawable;

    public RequestMaker(Monet monet, String url) {
        this.url = url;
        this.monet = monet;
    }

    public RequestMaker placeHolder(int drawableId) {
        this.defaultDrawable = monet.mContext.getResources().getDrawable(drawableId);
        return this;
    }

    public RequestMaker resize(int reqWidth, int reqHeight) {
        this.reqWidth = reqWidth;
        this.reqHeight = reqHeight;
        return this;
    }

    public void draw(ImageView imageView) {
        if (imageView == null) {
            throw new RuntimeException("ImageView can not be null");
        }
        imageView.setTag(Monet.TAG_KEY_URI, url);
        final Bitmap bitmap = monet.cacheLoader.loadFromMemCache(url);

        if (bitmap != null) {
            String url = ((String) imageView.getTag(Monet.TAG_KEY_URI));
            if (url.equals(this.url)) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageDrawable(defaultDrawable);
                DebugLog.v("set image bitmap, but url has changed, ignored", this);
            }
            return;
        }

        Performer performer = new ImagePerformer(monet, imageView, url, reqWidth, reqHeight, defaultDrawable);
        BitmapLoader bitmapLoader = BitmapLoaderImpl.CreateBitmapLoader(monet.dispatcher,
                monet.cacheLoader,
                monet.downLoader,
                performer);

        monet.dispatcher.dispatcherSubmit(bitmapLoader);

        //cache
        //placeholder
        //make action
        //monet submit with dispatcher
    }
}
