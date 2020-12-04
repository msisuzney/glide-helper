package com.msisuzney.glidehelper.model;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.R;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.util.Preconditions;
import com.msisuzney.glidehelper.utils.HttpUrl;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Map;

/**
 * A wrapper for strings representing http/https URLs responsible for ensuring URLs are properly
 * escaped and avoiding unnecessary URL instantiations for loaders that require only string urls
 * rather than URL objects.
 *
 * <p>Users wishing to replace the class for handling URLs must register a factory using GlideUrl.
 *
 * <p>To obtain a properly escaped URL, call {@link #toURL()}. To obtain a properly escaped string
 * URL, call {@link #toStringUrl()}. To obtain a less safe, but less expensive to calculate cache
 * key, call {@link #getCacheKey()}.
 *
 * <p>This class can also optionally wrap {@link Headers} for
 * convenience.
 */
public class MD5CheckUrl implements Key {
    private static final String TAG = "MD5CheckUrl";
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%;$";
    private final Headers headers;
    @Nullable
    private final URL url;
    @Nullable
    private final String stringUrl;

    @Nullable
    private String safeStringUrl;
    @Nullable
    private URL safeUrl;
    @Nullable
    private volatile byte[] cacheKeyBytes;

    private int hashCode;

    @NonNull
    private final String md5Value;


    public static class Builder {
        private String url;
        private String md5ParamKey;

        public Builder(@NonNull String url, @NonNull String md5ParamKey) {
            this.url = url;
            this.md5ParamKey = md5ParamKey;
        }

        public Object build() {
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                Log.e(TAG, "use String model,because parse HttpUrl failed");
                return url;
            }

            String md5ParamVal = httpUrl.queryParameter(md5ParamKey);
            if (TextUtils.isEmpty(md5ParamVal)) {
                Log.e(TAG, "use String model, because there isn't md5 value with key:" + md5ParamKey);
                return url;
            }
            return new MD5CheckUrl(url, md5ParamVal);
        }
    }


    private MD5CheckUrl(String url, String md5ParamVal) {
        this(url, md5ParamVal, Headers.DEFAULT);
    }


    private MD5CheckUrl(@NonNull String url, @NonNull String md5ParamVal, Headers headers) {
        this.url = null;
        this.stringUrl = Preconditions.checkNotEmpty(url);
        this.headers = Preconditions.checkNotNull(headers);
        this.md5Value = Preconditions.checkNotEmpty(md5ParamVal);
    }

    @NonNull
    public String getMd5Value() {
        return md5Value;
    }

    public URL toURL() throws MalformedURLException {
        return getSafeUrl();
    }

    // See http://stackoverflow.com/questions/3286067/url-encoding-in-android. Although the answer
    // using URI would work, using it would require both decoding and encoding each string which is
    // more complicated, slower and generates more objects than the solution below. See also issue
    // #133.
    private URL getSafeUrl() throws MalformedURLException {
        if (safeUrl == null) {
            safeUrl = new URL(getSafeStringUrl());
        }
        return safeUrl;
    }

    /**
     * Returns a properly escaped {@link String} url that can be used to make http/https requests.
     *
     * @see #toURL()
     * @see #getCacheKey()
     */
    public String toStringUrl() {
        return getSafeStringUrl();
    }

    private String getSafeStringUrl() {
        if (TextUtils.isEmpty(safeStringUrl)) {
            String unsafeStringUrl = stringUrl;
            if (TextUtils.isEmpty(unsafeStringUrl)) {
                unsafeStringUrl = Preconditions.checkNotNull(url).toString();
            }
            safeStringUrl = Uri.encode(unsafeStringUrl, ALLOWED_URI_CHARS);
        }
        return safeStringUrl;
    }

    /**
     * Returns a non-null {@link Map} containing headers.
     */
    public Map<String, String> getHeaders() {
        return headers.getHeaders();
    }

    /**
     * Returns an inexpensive to calculate {@link String} suitable for use as a disk cache key.
     *
     * <p>This method does not include headers.
     *
     * <p>Unlike {@link #toStringUrl()}} and {@link #toURL()}, this method does not escape input.
     */
    // Public API.
    @SuppressWarnings("WeakerAccess")
    public String getCacheKey() {
        return stringUrl != null ? stringUrl : Preconditions.checkNotNull(url).toString();
    }

    @Override
    public String toString() {
        return getCacheKey();
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
        if (o instanceof MD5CheckUrl) {
            MD5CheckUrl other = (MD5CheckUrl) o;
            return getCacheKey().equals(other.getCacheKey()) && headers.equals(other.headers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = getCacheKey().hashCode();
            hashCode = 31 * hashCode + headers.hashCode();
        }
        return hashCode;
    }
}
