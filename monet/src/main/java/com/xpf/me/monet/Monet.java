package com.xpf.me.monet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.xpf.me.monet.components.bitmaploader.BitmapLoaderImpl;
import com.xpf.me.monet.components.cache.CacheLoader;
import com.xpf.me.monet.components.cache.CacheLoaderImpl;
import com.xpf.me.monet.components.downloader.DownLoader;
import com.xpf.me.monet.components.downloader.DownLoaderImpl;
import com.xpf.me.monet.executor.Dispatcher;
import com.xpf.me.monet.executor.MonetExecutorService;
import com.xpf.me.monety.R;

import java.util.concurrent.ExecutorService;

/**
 * Created by xgo on 12/22/15.
 */
public class Monet {

    private static volatile Monet INSTANCE = null;

    public Context mContext;

    public static final int MESSAGE_POST_RESULT = 1;

    public static final int TAG_KEY_URI = R.id.dummy;

    public CacheLoader cacheLoader;

    public DownLoader downLoader;

    public final ExecutorService THREAD_POOL_EXECUTOR = new MonetExecutorService();


    final static Handler MHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            BitmapLoaderImpl loader = ((BitmapLoaderImpl) msg.obj);
            loader.getPerformer().complete(loader.getResult());
        }
    };

    public final Dispatcher dispatcher = new Dispatcher(THREAD_POOL_EXECUTOR, MHandler);

    private Monet(Context context) {
        mContext = context.getApplicationContext();
        cacheLoader = new CacheLoaderImpl(mContext);
        downLoader = new DownLoaderImpl(cacheLoader);
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

}
