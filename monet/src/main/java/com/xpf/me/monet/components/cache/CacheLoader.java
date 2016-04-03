package com.xpf.me.monet.components.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.IOException;

/**
 * Created by pengfeixie on 16/4/2.
 */
public interface CacheLoader {

    long DISK_CACHE_SIZE = 1024 * 1024 * 10;//50m for disk cache

    int IO_BUFFER_SIZE = 8 * 1024;

    int DISK_CACHE_INDEX = 0;

    Bitmap loadFromMemCache(String uri);

    Bitmap loadFromDiskCache(String uri, int reqWidth, int reqHeight) throws IOException;

    boolean isDiskCacheCreated();

    void setDiskCacheCreated(boolean isCreated);

    LruCache<String, Bitmap> getMemCache();

    DiskLruCache getDiskCache();

    String hashKeyFormUrl(String url);
}
