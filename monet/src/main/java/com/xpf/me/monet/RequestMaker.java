package com.xpf.me.monet;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

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
        this.defaultDrawable = monet.mContext.getDrawable(drawableId);
        return this;
    }

    public RequestMaker resize(int reqWidth, int reqHeight) {
        this.reqWidth = reqWidth;
        this.reqHeight = reqHeight;
        return this;
    }

    public void draw(ImageView imageView) {
        //check not null
        //cache
        //placeholder
        //make action
        //monet submit with dispatcher
    }
}
