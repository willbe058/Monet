package com.xpf.me.monet.components.downloader;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 * Created by pengfeixie on 16/4/2.
 */
public interface DownLoader {

    Bitmap loadFromHttp(String uri, int reqWidth, int reqHeight) throws IOException;

    Bitmap downloadBitmapFromUrl(String uri);
}
