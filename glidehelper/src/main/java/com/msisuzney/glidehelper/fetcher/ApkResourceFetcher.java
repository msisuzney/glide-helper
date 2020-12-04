package com.msisuzney.glidehelper.fetcher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.LruCache;
import com.msisuzney.glidehelper.model.ApkResourceDrawable;

import java.io.File;
import java.util.Objects;

public class ApkResourceFetcher implements DataFetcher<Drawable> {

    private final ApkResourceDrawable apkResourceDrawable;

    public ApkResourceFetcher(ApkResourceDrawable apkResourceDrawable) {
        this.apkResourceDrawable = apkResourceDrawable;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Drawable> callback) {
        Resources resources = apkResourceDrawable.getResources();
        int id = resources.getIdentifier(apkResourceDrawable.getDrawableName(), apkResourceDrawable.getDefType(), apkResourceDrawable.getPackageName());
        Drawable drawable = ResourcesCompat.getDrawable(resources, id, null);
        if (drawable != null) {
            callback.onDataReady(drawable);
        } else {
            callback.onLoadFailed(new RuntimeException("fetch drawable from " + apkResourceDrawable.toString() + " failed"));
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<Drawable> getDataClass() {
        return Drawable.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.DATA_DISK_CACHE;
    }


}
