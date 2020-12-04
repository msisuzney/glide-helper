package com.msisuzney.glidehelper.model;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.LruCache;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.Objects;

public class ApkResourceDrawable implements Key {

    private final static LruCache<ApkFileKey, WeakReference<Resources>> FILE_RESOURCES_LRU_CACHE = new LruCache<>(3);

    private final String drawableName;
    private final String defType;
    private final Resources resources;
    private final String apkPath;
    private final String packageName;
    @Nullable
    private volatile byte[] cacheKeyBytes;

    public ApkResourceDrawable(Context context, String apkPath, String drawableName, String defType, String packageName) {
        this.drawableName = drawableName;
        this.defType = defType;
        this.resources = loadResourcesFromOtherApk(context.getApplicationContext(), apkPath);
        this.apkPath = apkPath;
        this.packageName = packageName;
    }


    public String getDrawableName() {
        return drawableName;
    }

    public String getDefType() {
        return defType;
    }

    public Resources getResources() {
        return resources;
    }

    public String getApkPath() {
        return apkPath;
    }

    public String getPackageName() {
        return packageName;
    }

    private Resources loadResourcesFromOtherApk(Context context, String apkPath) {
        File apkFile = new File(apkPath);
        ApkFileKey apkFileKey = new ApkFileKey(apkFile, apkFile.lastModified());
        WeakReference<Resources> ref = FILE_RESOURCES_LRU_CACHE.get(apkFileKey);
        if (ref != null) {
            Resources res = ref.get();
            if (res != null) {
                return res;
            }
        }
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA);
        packageInfo.applicationInfo.publicSourceDir = apkPath;
        packageInfo.applicationInfo.sourceDir = apkPath;
        try {
            Resources newResources = packageManager.getResourcesForApplication(packageInfo.applicationInfo);
            FILE_RESOURCES_LRU_CACHE.put(apkFileKey, new WeakReference<>(newResources));
            return newResources;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return "ApkResourceDrawable{" +
                "drawableName='" + drawableName + '\'' +
                ", defType='" + defType + '\'' +
                ", resources=" + resources +
                ", apkPath='" + apkPath + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }

    public String getCacheKey() {
        StringBuilder builder = new StringBuilder();
        builder.append(apkPath);
        builder.append(packageName);
        builder.append(defType);
        builder.append(drawableName);
        return builder.toString();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(getCacheKeyBytes());
    }

    private byte[] getCacheKeyBytes() {
        if (cacheKeyBytes == null) {
            cacheKeyBytes = getCacheKey().getBytes(CHARSET);
        }
        return cacheKeyBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ApkResourceDrawable) {
            ApkResourceDrawable other = (ApkResourceDrawable) o;
            return getCacheKey().equals(other.getCacheKey());
        }
        return false;
    }



    private static final class ApkFileKey {
        private final long lastModified;
        private final File apkFile;

        public ApkFileKey(File apkFile, long lastModified) {
            this.lastModified = lastModified;
            this.apkFile = apkFile;
        }

        @Override
        public int hashCode() {
            if (Build.VERSION.SDK_INT >= 19) {
                return Objects.hash(lastModified, apkFile);
            }
            return 31 * apkFile.hashCode() + (int) lastModified;
        }
    }

}
