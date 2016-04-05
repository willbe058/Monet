package com.xpf.me.monet;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import com.xpf.me.monet.components.bitmaploader.BitmapLoaderImpl;
import com.xpf.me.monet.components.cache.CacheLoader;
import com.xpf.me.monet.components.cache.CacheLoaderImpl;
import com.xpf.me.monet.components.downloader.DownLoader;
import com.xpf.me.monet.components.downloader.DownLoaderImpl;
import com.xpf.me.monet.executor.Dispatcher;
import com.xpf.me.monet.executor.MonetExecutorService;
import com.xpf.me.monety.R;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Created by xgo on 12/22/15.
 */
public class Monet {

    private static volatile Monet INSTANCE = null;

    public Context mContext;

    public static final int MESSAGE_POST_RESULT = 1;

    public CacheLoader cacheLoader;

    public DownLoader downLoader;

    public final ExecutorService THREAD_POOL_EXECUTOR = new MonetExecutorService();

    private Map<Object, Performer> targetToPerformer;

    final static Handler MHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            BitmapLoaderImpl loader = ((BitmapLoaderImpl) msg.obj);
            if (loader.getPerformer() != null) {
                loader.getPerformer().monet.complete(loader);
            }
        }
    };

    public final Dispatcher dispatcher = new Dispatcher(THREAD_POOL_EXECUTOR, MHandler);

    private Monet(Context context) {
        mContext = context.getApplicationContext();
        cacheLoader = new CacheLoaderImpl(mContext);
        downLoader = new DownLoaderImpl(cacheLoader);
        this.targetToPerformer = new WeakHashMap<>();
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

    public void submit(Performer<ImageView> performer) {
        Object target = performer.getTarget();
        if (target != null && targetToPerformer.get(target) != performer) {
            cancelExistingRequest(target);
            targetToPerformer.put(target, performer);
        }
        dispatcher.dispatcherSubmit(performer);
    }

    private void cancelExistingRequest(Object target) {
        if (!(Looper.getMainLooper().getThread() == Thread.currentThread())) {
            throw new IllegalStateException("Method call should happen from the main thread.");
        }
        Performer<ImageView> performer = targetToPerformer.remove(target);
        if (performer != null) {
            performer.cancel();
            dispatcher.dispatcherCancel(performer);
        }
    }

    public void complete(BitmapLoaderImpl bitmapLoader) {
        deliverPerformer(bitmapLoader.getResult(), bitmapLoader.getPerformer());
    }

    public void deliverPerformer(Bitmap result, Performer<ImageView> performer) {
        if (performer.isCancelled()) {
            return;
        }
        if (result != null) {
            performer.complete(result);
        } else {
            //performer.error();
        }
    }

}
