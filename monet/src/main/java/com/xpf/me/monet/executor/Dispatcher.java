package com.xpf.me.monet.executor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.xpf.me.monet.Monet;
import com.xpf.me.monet.components.bitmaploader.BitmapLoader;

import java.util.concurrent.ExecutorService;

/**
 * Created by pengfeixie on 16/3/15.
 */
public final class Dispatcher {

    static final int REQUEST_SUBMIT = 1;

    static final int RESULT_COMPLETE = 2;

    final ExecutorService executorService;

    private final DispatcherHandler handler;

    private final Handler mainHandler;

    private final DispatcherThread thread;

    public Dispatcher(ExecutorService service, Handler handler) {
        thread = new DispatcherThread();
        thread.start();
        this.executorService = service;
        this.handler = new DispatcherHandler(thread.getLooper(), this);
        this.mainHandler = handler;
    }

    public void dispatcherSubmit(BitmapLoader runnable) {
        handler.obtainMessage(REQUEST_SUBMIT, runnable).sendToTarget();
    }


    public void dispatcherComplete(BitmapLoader result) {
        handler.obtainMessage(RESULT_COMPLETE, result).sendToTarget();
    }

    void performSubmit(BitmapLoader runnable) {
        // TODO: 16/4/2 make bitmap loader with request
        executorService.submit(runnable);
    }

    void performComplete(BitmapLoader result) {
        mainHandler.obtainMessage(Monet.MESSAGE_POST_RESULT, result).sendToTarget();
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
                    BitmapLoader runnable = ((BitmapLoader) msg.obj);
                    dispatcher.performSubmit(runnable);
                    break;
                case RESULT_COMPLETE:
                    BitmapLoader bitmapLoader = ((BitmapLoader) msg.obj);
                    dispatcher.performComplete(bitmapLoader);
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
