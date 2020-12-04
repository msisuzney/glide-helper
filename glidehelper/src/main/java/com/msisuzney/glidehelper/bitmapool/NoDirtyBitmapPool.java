package com.msisuzney.glidehelper.bitmapool;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;

import java.util.Set;

/**
 * https://github.com/bumptech/glide/issues/2623
 */
public class NoDirtyBitmapPool extends LruBitmapPool {
    public NoDirtyBitmapPool(long maxSize) {
        super(maxSize);
    }

    public NoDirtyBitmapPool(long maxSize, Set<Bitmap.Config> allowedConfigs) {
        super(maxSize, allowedConfigs);
    }

    @NonNull
    @Override
    public Bitmap getDirty(int width, int height, Bitmap.Config config) {
        return null;
    }
}
