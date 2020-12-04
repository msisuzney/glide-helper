package com.msisuzney.glidehelper.diskcache;

import android.content.Context;

import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.File;


public class MD5DiskLruCacheFactory implements DiskCache.Factory {
    private final long diskCacheSize;
    private final CacheDirectoryGetter cacheDirectoryGetter;

    /** Interface called out of UI thread to get the cache folder. */
    public interface CacheDirectoryGetter {
        File getCacheDirectory();
    }

    public MD5DiskLruCacheFactory(Context context) {
        this(
                context,
                DiskCache.Factory.DEFAULT_DISK_CACHE_DIR,
                DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE);
    }

    public MD5DiskLruCacheFactory(
            final Context context, final String diskCacheName, long diskCacheSize) {
        this(
                new CacheDirectoryGetter() {
                    @Override
                    public File getCacheDirectory() {
                        File cacheDirectory = context.getCacheDir();
                        if (cacheDirectory == null) {
                            return null;
                        }
                        if (diskCacheName != null) {
                            return new File(cacheDirectory, diskCacheName);
                        }
                        return cacheDirectory;
                    }
                },
                diskCacheSize);
    }


    /**
     * When using this constructor {@link CacheDirectoryGetter#getCacheDirectory()} will be called out
     * of UI thread, allowing to do I/O access without performance impacts.
     *
     * @param cacheDirectoryGetter Interface called out of UI thread to get the cache folder.
     * @param diskCacheSize Desired max bytes size for the LRU disk cache.
     */
    // Public API.
    @SuppressWarnings("WeakerAccess")
    public MD5DiskLruCacheFactory(CacheDirectoryGetter cacheDirectoryGetter, long diskCacheSize) {
        this.diskCacheSize = diskCacheSize;
        this.cacheDirectoryGetter = cacheDirectoryGetter;
    }

    @Override
    public DiskCache build() {
        File cacheDir = cacheDirectoryGetter.getCacheDirectory();

        if (cacheDir == null) {
            return null;
        }

        if (!cacheDir.mkdirs() && (!cacheDir.exists() || !cacheDir.isDirectory())) {
            return null;
        }

        return MD5DiskCacheWrapper.create(cacheDir, diskCacheSize);
    }
}
