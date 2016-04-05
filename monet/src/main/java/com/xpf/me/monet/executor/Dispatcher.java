package com.xpf.me.monet.executor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import com.xpf.me.monet.Monet;
import com.xpf.me.monet.Performer;
import com.xpf.me.monet.components.bitmaploader.BitmapLoader;
import com.xpf.me.monet.components.bitmaploader.BitmapLoaderImpl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Created by pengfeixie on 16/3/15.
 */
public final class Dispatcher {

    static final int REQUEST_SUBMIT = 1;

    static final int RESULT_COMPLETE = 2;

    static final int REQUEST_CANCEL = 3;

    final ExecutorService executorService;

    private final DispatcherHandler handler;

    private final Handler mainHandler;

    private final DispatcherThread thread;

    private Map<String, BitmapLoaderImpl> loaderMap;

    public Dispatcher(ExecutorService service, Handler handler) {
        thread = new DispatcherThread();
        thread.start();
        this.executorService = service;
        this.handler = new DispatcherHandler(thread.getLooper(), this);
        this.mainHandler = handler;
        this.loaderMap = new LinkedHashMap<>();
    }

    public void dispatcherSubmit(Performer<ImageView> performer) {
        handler.obtainMessage(REQUEST_SUBMIT, performer).sendToTarget();
    }


    public void dispatcherComplete(BitmapLoader result) {
        handler.obtainMessage(RESULT_COMPLETE, result).sendToTarget();
    }

    public void dispatcherCancel(Performer<ImageView> performer) {
        handler.obtainMessage(REQUEST_CANCEL, performer).sendToTarget();
    }

    void performSubmit(Performer<ImageView> imageViewPerformer) {
        BitmapLoaderImpl bitmapLoader = loaderMap.get(imageViewPerformer.url);
        if (bitmapLoader != null) {
            bitmapLoader.attach(imageViewPerformer);
            return;
        }

        bitmapLoader = BitmapLoaderImpl.CreateBitmapLoader(
                this,
                imageViewPerformer.monet.cacheLoader,
                imageViewPerformer.monet.downLoader,
                imageViewPerformer);
        bitmapLoader.future = executorService.submit(bitmapLoader);
        loaderMap.put(imageViewPerformer.url, bitmapLoader);

    }

    void performComplete(BitmapLoader result) {
        mainHandler.obtainMessage(Monet.MESSAGE_POST_RESULT, result).sendToTarget();
    }

    void performCancel(Performer<ImageView> performer) {
        String key = performer.url;
        BitmapLoaderImpl hunter = loaderMap.get(key);
        if (hunter != null) {
            hunter.detach(performer);
            if (hunter.cancel()) {
                loaderMap.remove(key);
            }
        }
    }

    private static class DispatcherHandler extends Handler {
        private final Dispatcher dispatcher;

        public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REQUEST_SUBMIT:
                    Performer<ImageView> runnable = ((Performer<ImageView>) msg.obj);
                    dispatcher.performSubmit(runnable);
                    break;
                case RESULT_COMPLETE:
                    BitmapLoader bitmapLoader = ((BitmapLoader) msg.obj);
                    dispatcher.performComplete(bitmapLoader);
                    break;
                case REQUEST_CANCEL:
                    Performer<ImageView> performer = ((Performer<ImageView>) msg.obj);
                    dispatcher.performCancel(performer);
                    break;
            }
        }
    }


    static class DispatcherThread extends HandlerThread {

        public DispatcherThread() {
            super("Monet#Thread");
        }
    }
}
