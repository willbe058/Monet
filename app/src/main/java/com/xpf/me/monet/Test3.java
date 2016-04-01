package com.xpf.me.monet;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pengfeixie on 16/3/16.
 */
public class Test3 implements Test2 {
    @Override
    public void test() {
        new Canvas().drawRect(0f,0f,0f,0f,new Paint());
    }
}
