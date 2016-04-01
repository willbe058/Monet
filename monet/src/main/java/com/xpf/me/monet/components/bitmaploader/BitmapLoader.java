package com.xpf.me.monet.components.bitmaploader;

import android.graphics.Bitmap;

/**
 * Created by pengfeixie on 16/4/2.
 */
public interface BitmapLoader extends Runnable{

    Bitmap load(String uri, int reqWidth, int reqHeight);

}
