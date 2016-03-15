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

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;

    private static final int TAG_KEY_URI = R.id.dummy;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;//50m for disk cache
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private boolean mIsDiskLruCacheCreated = false;

    final Dispatcher dispatcher;
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

    private ImageResizer mImageResizer = new ImageResizer();
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    static private Drawable mDefalutDrawable;

    private Monet(Context context, Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        Context mContext = context.getApplicationContext();
        int maxMemory = ((int) (Runtime.getRuntime().maxMemory() / 1024));
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
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
        final Bitmap bitmap = loadBitmapFromMemCache(uri);

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

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap1 = loadBitmap(uri, reqWidth, reqHeight);
                if (bitmap1 != null) {
                    MonetResult result = new MonetResult(imageView, uri, bitmap1);
                    dispatcher.dispatcherComplete(result);
                }
            }
        };
        dispatcher.dispatcherSubmit(loadBitmapTask);
    }

    public Bitmap loadBitmap(String uri, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemCache(uri);
        if (bitmap != null) {
            DebugLog.v("loadBitmapFromMemCache,url" + uri);
            return bitmap;
        }

        try {
            bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
            if (bitmap != null) {
                DebugLog.v("loadBitmapFromDisckCache,uri" + uri);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);
            DebugLog.v("loadBitmapFromHttp,uri" + uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null && !mIsDiskLruCacheCreated) {
            DebugLog.v("encounter error, DiskLruCache is not created.");
            bitmap = downloadBitmapFromUrl(uri);
        }
        return bitmap;
    }

    private Bitmap loadBitmapFromMemCache(String url) {
        final String key = hashKeyFormUrl(url);
        return getBitmapFromMemCache(key);
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not visit network from UI Thread.");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        String key = hashKeyFormUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            DebugLog.v("load bitmap from UI Thread, it's not recommended!");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        String key = hashKeyFormUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream fileInputStream = ((FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX));
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
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
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

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

    private Bitmap downloadBitmapFromUrl(String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = ((HttpURLConnection) url.openConnection());
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
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

    private String hashKeyFormUrl(String url) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
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
            new Monet(context, new Dispatcher(THREAD_POOL_EXECUTOR,MHandler)).draw(url, targetView, reqWidth, reqHeight, defaultDrawable);
        }
    }

    static class MonetResult {
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
