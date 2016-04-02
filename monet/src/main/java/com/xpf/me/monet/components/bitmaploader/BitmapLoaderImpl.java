package com.xpf.me.monet.components.bitmaploader;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.xpf.me.monet.Performer;
import com.xpf.me.monet.components.cache.CacheLoader;
import com.xpf.me.monet.components.downloader.DownLoader;
import com.xpf.me.monet.executor.Dispatcher;
import com.xpf.me.monet.utils.DebugLog;

import java.io.IOException;

/**
 * Created by pengfeixie on 16/4/2.
 */
public class BitmapLoaderImpl implements BitmapLoader {

    private CacheLoader cacheLoader;

    private DownLoader downLoader;

    private Dispatcher dispatcher;

    private Performer<ImageView> performer;

    private Bitmap result;

    public BitmapLoaderImpl(Dispatcher dispatcher, CacheLoader cacheLoader, DownLoader downLoader, Performer performer) {
        this.performer = performer;
        this.dispatcher = dispatcher;
        this.cacheLoader = cacheLoader;
        this.downLoader = downLoader;
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
        result = load(performer.url, performer.reqWidth, performer.reqHeight);
        if (result != null) {
            dispatcher.dispatcherComplete(this);
        }
    }

    public Bitmap getResult() {
        return result;
    }

    public Performer<ImageView> getPerformer() {
        return performer;
    }

    public static BitmapLoader CreateBitmapLoader(Dispatcher dispatcher, CacheLoader cacheLoader, DownLoader downLoader, Performer performer) {
        return new BitmapLoaderImpl(dispatcher, cacheLoader, downLoader, performer);
    }
}
