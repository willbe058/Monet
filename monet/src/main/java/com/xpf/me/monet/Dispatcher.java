package com.xpf.me.monet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

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

    Dispatcher(ExecutorService service, Handler handler) {
        thread = new DispatcherThread();
        thread.start();
        this.executorService = service;
        this.handler = new DispatcherHandler(thread.getLooper(), this);
        this.mainHandler = handler;
    }

    void dispatcherSubmit(Runnable runnable) {
        handler.obtainMessage(REQUEST_SUBMIT, runnable).sendToTarget();
    }

    void dispatcherComplete(Monet.MonetResult result) {
        handler.obtainMessage(RESULT_COMPLETE, result).sendToTarget();
    }

    void performSubmit(Runnable runnable) {
        executorService.execute(runnable);
    }

    void performComplete(Monet.MonetResult result) {
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
                    Runnable runnable = ((Runnable) msg.obj);
                    dispatcher.performSubmit(runnable);
                    break;
                case RESULT_COMPLETE:
                    Monet.MonetResult result = ((Monet.MonetResult) msg.obj);
                    dispatcher.performComplete(result);
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
