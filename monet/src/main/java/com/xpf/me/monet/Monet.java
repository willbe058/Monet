package com.xpf.me.monet;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;
import com.xpf.me.monet.components.bitmaploader.BitmapLoader;
import com.xpf.me.monet.components.bitmaploader.BitmapLoaderImpl;
import com.xpf.me.monet.components.cache.CacheLoader;
import com.xpf.me.monet.components.cache.CacheLoaderImpl;
import com.xpf.me.monet.components.downloader.DownLoader;
import com.xpf.me.monet.components.downloader.DownLoaderImpl;
import com.xpf.me.monet.executor.Dispatcher;
import com.xpf.me.monet.executor.MonetExecutorService;
import com.xpf.me.monet.utils.DebugLog;
import com.xpf.me.monety.R;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Created by xgo on 12/22/15.
 */
public class Monet {

    private static volatile Monet INSTANCE = null;

    public Context mContext;

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

    static private Drawable mDefalutDrawable;

    private Monet(Context context) {
        mContext = context.getApplicationContext();
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
        mDefalutDrawable = defaultDrawable;
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


    public static Monet with(Context context) {
        if (INSTANCE == null) {
            synchronized (Monet.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Monet(context);
                }
            }
        }
        return INSTANCE;
    }

    public RequestMaker load(String url) {
        return new RequestMaker(this, url);
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

        public Monet build() {
            return new Monet(context);
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
