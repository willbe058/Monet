package com.xpf.me.monet.executor;

import android.support.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by pengfeixie on 16/3/15.
 */
public class MonetExecutorService extends ThreadPoolExecutor {

    private static final int DEFAULT_THREAD_COUNT = 3;

    public MonetExecutorService() {
        super(DEFAULT_THREAD_COUNT, DEFAULT_THREAD_COUNT + 3, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        return new Thread(r);
                    }
                });
    }

    @Override
    public void execute(Runnable command) {
        MonetFutureTask task = new MonetFutureTask(command);
        super.execute(task);
    }

    private static final class MonetFutureTask extends FutureTask<Runnable>
            implements Comparable<MonetFutureTask> {
        private final Runnable runnable;

        public MonetFutureTask(Runnable runnable) {
            super(runnable, null);
            this.runnable = runnable;
        }

        @Override
        public int compareTo(MonetFutureTask other) {
            // High-priority requests are "lesser" so they are sorted to the front.
            // Equal priorities are sorted by sequence number to provide FIFO ordering.
            return 0;
        }
    }
}
