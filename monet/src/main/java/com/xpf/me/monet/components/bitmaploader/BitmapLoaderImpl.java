package com.xpf.me.monet.components.bitmaploader;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.xpf.me.monet.components.cache.CacheLoader;
import com.xpf.me.monet.utils.DebugLog;
import com.xpf.me.monet.executor.Dispatcher;
import com.xpf.me.monet.Monet;
import com.xpf.me.monet.components.downloader.DownLoader;

import java.io.IOException;

/**
 * Created by pengfeixie on 16/4/2.
 */
public class BitmapLoaderImpl implements BitmapLoader {

    private CacheLoader cacheLoader;

    private DownLoader downLoader;

    private String url;

    private int reqWidth;

    private int reqHeight;

    private Dispatcher dispatcher;

    private ImageView imageView;

    public BitmapLoaderImpl(Dispatcher dispatcher, CacheLoader cacheLoader, DownLoader downLoader, ImageView imageView, String url, int reqWidth, int reqHeight) {
        this.url = url;
        this.reqWidth = reqWidth;
        this.reqHeight = reqHeight;
        this.cacheLoader = cacheLoader;
        this.downLoader = downLoader;
        this.dispatcher = dispatcher;
        this.imageView = imageView;
    }

    @Override
    public Bitmap load(String uri, int reqWidth, int reqHeight) {
        Bitmap bitmap = cacheLoader.loadFromMemCache(uri);
        if (bitmap != null) {
            DebugLog.v("loadBitmapFromMemCache,url" + uri);
            return bitmap;
        }

        try {
            bitmap = cacheLoader.loadFromDiskCache(uri, reqWidth, reqHeight);
            if (bitmap != null) {
                DebugLog.v("loadBitmapFromDisckCache,uri" + uri);
                return bitmap;
            }
            bitmap = downLoader.loadFromHttp(uri, reqWidth, reqHeight);
            DebugLog.v("loadBitmapFromHttp,uri" + uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null && !cacheLoader.isDiskCacheCreated()) {
            DebugLog.v("encounter error, DiskLruCache is not created.");
            bitmap = downLoader.downloadBitmapFromUrl(uri);
        }
        return bitmap;
    }

    @Override
    public void run() {
        Bitmap bitmap = load(url, reqWidth, reqHeight);
        if (bitmap != null) {
            Monet.MonetResult result = new Monet.MonetResult(imageView, url, bitmap);
            dispatcher.dispatcherComplete(result);
        }
    }

    public static BitmapLoader CreateBitmapLoader(Dispatcher dispatcher, CacheLoader cacheLoader, DownLoader downLoader, ImageView imageView, String url, int reqWidth, int reqHeight) {
        return new BitmapLoaderImpl(dispatcher, cacheLoader, downLoader, imageView, url, reqWidth, reqHeight);
    }
}
