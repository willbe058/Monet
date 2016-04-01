package com.xpf.me.monet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pengfeixie on 16/4/2.
 */
public class DownLoaderImpl implements DownLoader {

    private CacheLoader cacheLoader;

    public DownLoaderImpl(CacheLoader cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    @Override
    public Bitmap loadFromHttp(String uri, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not visit network from UI Thread.");
        }
        if (cacheLoader.getMemCache() == null) {
            return null;
        }

        String key = cacheLoader.hashKeyFormUrl(uri);
        DiskLruCache.Editor editor = cacheLoader.getDiskCache().edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(CacheLoader.DISK_CACHE_INDEX);
            if (downloadUrlToStream(uri, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            cacheLoader.getDiskCache().flush();
        }
        return cacheLoader.loadFromDiskCache(uri, reqWidth, reqHeight);
    }

    @Override
    public Bitmap downloadBitmapFromUrl(String uri) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(uri);
            urlConnection = ((HttpURLConnection) url.openConnection());
            in = new BufferedInputStream(urlConnection.getInputStream(), CacheLoader.IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (final IOException e) {
            DebugLog.v("Error in downloadBitmap: " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            IOUtils.closeSilently(in);
        }
        return bitmap;
    }

    public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = ((HttpURLConnection) url.openConnection());
            in = new BufferedInputStream(urlConnection.getInputStream(), CacheLoader.IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, CacheLoader.IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (IOException e) {
            DebugLog.v("downloadBitmap failed" + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            IOUtils.closeSilently(out);
            IOUtils.closeSilently(in);
        }

        return false;
    }

}
