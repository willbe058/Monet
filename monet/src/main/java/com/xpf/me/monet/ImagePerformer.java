package com.xpf.me.monet;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.xpf.me.monet.utils.DebugLog;

/**
 * Created by pengfeixie on 16/4/3.
 */
public class ImagePerformer extends Performer<ImageView> {

    public ImagePerformer(Monet monet, ImageView target, String url, int reqWidth, int reqHeight, Drawable drawable) {
        super(monet, target, url, reqWidth, reqHeight, drawable);
    }

    @Override
    public void complete(Bitmap result) {
        target.setImageBitmap(result);
        String uri = ((String) target.getTag(Monet.TAG_KEY_URI));
        if (uri.equals(url)) {
            target.setImageBitmap(result);
        } else {
            // TODO: 16/4/3 chong yong
            target.setImageDrawable(drawable);
            DebugLog.v("set image bitmap, but url has changed, ignored", this);
        }
    }
}
