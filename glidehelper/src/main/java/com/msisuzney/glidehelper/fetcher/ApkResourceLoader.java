package com.msisuzney.glidehelper.fetcher;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.msisuzney.glidehelper.model.ApkResourceDrawable;

public class ApkResourceLoader implements ModelLoader<ApkResourceDrawable, Drawable> {

    @Nullable
    @Override
    public LoadData<Drawable> buildLoadData(@NonNull ApkResourceDrawable apkResourceDrawable, int width, int height, @NonNull Options options) {
        return new LoadData<Drawable>(apkResourceDrawable,new ApkResourceFetcher(apkResourceDrawable));
    }

    @Override
    public boolean handles(@NonNull ApkResourceDrawable apkResourceDrawable) {
        return true;
    }

    /**
     * The default factory for {@link com.bumptech.glide.load.model.stream.HttpGlideUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<ApkResourceDrawable, Drawable> {

        @NonNull
        @Override
        public ModelLoader<ApkResourceDrawable, Drawable> build(MultiModelLoaderFactory multiFactory) {
            return new ApkResourceLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
