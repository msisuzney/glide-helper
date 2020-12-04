package com.msisuzney.glidehelper.diskcache;

import android.os.SystemClock;
import android.util.Log;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.msisuzney.glidehelper.LogSwitch;
import com.msisuzney.glidehelper.model.MD5CheckUrl;
import com.msisuzney.glidehelper.utils.MD5Utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MD5DiskCacheWrapper extends DiskLruCacheWrapper {
    private static final String TAG = "GlideDiskLruCache";
    private final Class<?> dataCacheKeyClass;
    private final Method getSourceKeyMethod;

    public MD5DiskCacheWrapper(File directory, long maxSize) {
        super(directory, maxSize);
        try {
            //DataCacheKey不能访问，只能反射了
            dataCacheKeyClass = Class.forName("com.bumptech.glide.load.engine.DataCacheKey");
            getSourceKeyMethod = dataCacheKeyClass.getDeclaredMethod("getSourceKey");
            getSourceKeyMethod.setAccessible(true);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public File get(Key key) {
        File file = super.get(key);
        if (file == null) {
            return null;
        }
        long startUps = 0;
        if (LogSwitch.LOGGING) {
            startUps = SystemClock.uptimeMillis();
        }

        //排除ResourceCacheKey
        if (key.getClass().isAssignableFrom(dataCacheKeyClass)) {
            Key sourceKey = null;
            try {
                sourceKey = (Key) getSourceKeyMethod.invoke(key);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            if (sourceKey instanceof MD5CheckUrl) {
                MD5CheckUrl md5CheckUrl = (MD5CheckUrl) sourceKey;
                String md5Param = md5CheckUrl.getMd5Value();

                String sum = MD5Utils.encode(file).toLowerCase();
                if (LogSwitch.LOGGING) {
                    Log.d(TAG, "checksum:" + sum + ",md5Param:" + md5Param);
                }
                if (md5Param.equals(sum)) {
                    if (LogSwitch.LOGGING) {
                        Log.d(TAG, "md5 checksum ok cost:" + (SystemClock.uptimeMillis() - startUps) + "ms");
                    }
                    return file;
                } else {
                    if (LogSwitch.LOGGING) {
                        Log.i(TAG, "md5 checksum cost:" + (SystemClock.uptimeMillis() - startUps) + "ms");
                    }
                    return null;
                }
            }
        }

        return file;
    }

    /**
     * Create a new DiskCache in the given directory with a specified max size.
     *
     * @param directory The directory for the disk cache
     * @param maxSize   The max size for the disk cache
     * @return The new disk cache with the given arguments
     */
    @SuppressWarnings("deprecation")
    public static DiskCache create(File directory, long maxSize) {
        return new MD5DiskCacheWrapper(directory, maxSize);
    }
}
