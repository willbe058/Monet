package com.xpf.me.monet;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;
import com.xpf.me.monety.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xgo on 12/22/15.
 */
public class Monet {

    private static final String TAG = "Monet";

    public static final int MESSAGE_POST_RESULT = 1;

    private static final int TAG_KEY_URI = R.id.dummy;

    private static CacheLoader cacheLoader;

    private DownLoader downLoader;

    public static final ExecutorService THREAD_POOL_EXECUTOR = new MonetExecutorService();

    final static Handler MHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            MonetResult result = ((MonetResult) msg.obj);
            ImageView imageView = result.imageView;
            imageView.setImageBitmap(result.bitmap);
            String uri = ((String) imageView.getTag(TAG_KEY_URI));
            if (uri.equals(result.uri)) {
                imageView.setImageBitmap(result.bitmap);
            } else {
                imageView.setImageDrawable(mDefalutDrawable);
                DebugLog.v("set image bitmap, but url has changed, ignored", this);
            }
        }
    };

    final static Dispatcher dispatcher = new Dispatcher(THREAD_POOL_EXECUTOR, MHandler);

    private DiskLruCache mDiskLruCache;

    static private Drawable mDefalutDrawable;

    private Monet(Context context) {
        Context mContext = context.getApplicationContext();
        cacheLoader = new CacheLoaderImpl(mContext);
        downLoader = new DownLoaderImpl(cacheLoader);
    }


    /**
     * load bitmap from mem cache or disk cache or network async, then bind
     *
     * @param uri
     * @param imageView
     */
    private void draw(final String uri, final ImageView imageView
            , final int reqWidth, final int reqHeight, Drawable defaultDrawable) {
        this.mDefalutDrawable = defaultDrawable;
        imageView.setTag(TAG_KEY_URI, uri);
        final Bitmap bitmap = cacheLoader.loadFromMemCache(uri);

        if (bitmap != null) {
            String url = ((String) imageView.getTag(TAG_KEY_URI));
            if (url.equals(uri)) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageDrawable(mDefalutDrawable);
                DebugLog.v("set image bitmap, but url has changed, ignored", this);
            }
            return;
        }
        BitmapLoader bitmapLoader = BitmapLoaderImpl.CreateBitmapLoader(dispatcher, cacheLoader, downLoader, imageView, uri, reqWidth, reqHeight);
        dispatcher.dispatcherSubmit(bitmapLoader);
    }


    public File getDiskCacheDir(Context context, String uniqueName) {
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return stats.getBlockSizeLong() * stats.getAvailableBlocksLong();
    }

    public static Builder with(Context context) {
        return new Builder(context);
    }

    public static class Builder {

        private int reqWidth, reqHeight;

        private ImageView targetView;

        private Context context;

        private String url;

        private Drawable defaultDrawable;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder load(String url) {
            this.url = url;
            return this;
        }

        public Builder size(int reqWidth, int reqHeight) {
            this.reqWidth = reqWidth;
            this.reqHeight = reqHeight;
            return this;
        }

        public Builder on(ImageView target) {
            this.targetView = target;
            return this;
        }

        public Builder placeHolder(int resId) {
            this.defaultDrawable = context.getResources().getDrawable(resId);
            return this;
        }

        public void draw() {
            new Monet(context).draw(url, targetView, reqWidth, reqHeight, defaultDrawable);
        }
    }

    public static class MonetResult {
        public ImageView imageView;
        public String uri;
        public Bitmap bitmap;

        public MonetResult(ImageView imageView, String uri, Bitmap bitmap) {
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }

}
