package com.msisuzney.glidehelper.fetcher;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.msisuzney.glidehelper.model.MD5CheckUrl;

import java.io.InputStream;

/**
 * An {@link ModelLoader} for translating {@link
 * com.bumptech.glide.load.model.GlideUrl} (http/https URLS) into {@link InputStream} data.
 */
// Public API.
@SuppressWarnings("WeakerAccess")
public class HttpMD5CheckUrlLoader implements ModelLoader<MD5CheckUrl, InputStream> {
    /**
     * An integer option that is used to determine the maximum connect and read timeout durations (in
     * milliseconds) for network connections.
     *
     * <p>Defaults to 2500ms.
     */
    public static final Option<Integer> TIMEOUT =
            Option.memory("com.bumptech.glide.load.model.stream.HttpGlideUrlLoader.Timeout", 2500);

    /**
     * 构建Request时缓存策略，这里用来判断是否进行HTTP下载阶段的MD5校验
     */
    public static final Option<DiskCacheStrategy> DISKCACHESTRATEGY =
            Option.memory("com.bumptech.glide.load.engine.DiskCacheStrategy", DiskCacheStrategy.AUTOMATIC);

    @Nullable
    private final ModelCache<MD5CheckUrl, MD5CheckUrl> modelCache;

    public HttpMD5CheckUrlLoader() {
        this(null);
    }

    public HttpMD5CheckUrlLoader(@Nullable ModelCache<MD5CheckUrl, MD5CheckUrl> modelCache) {
        this.modelCache = modelCache;
    }

    @Override
    public LoadData<InputStream> buildLoadData(
            @NonNull MD5CheckUrl model, int width, int height, @NonNull Options options) {
        // GlideUrls memoize parsed URLs so caching them saves a few object instantiations and time
        // spent parsing urls.
        MD5CheckUrl url = model;
        if (modelCache != null) {
            url = modelCache.get(model, 0, 0);
            if (url == null) {
                modelCache.put(model, 0, 0, model);
                url = model;
            }
        }
        int timeout = options.get(TIMEOUT);
        DiskCacheStrategy diskCacheStrategy = options.get(DISKCACHESTRATEGY);
        return new LoadData<>(url, new HttpMD5CheckUrlFetcher(url, timeout, diskCacheStrategy));
    }

    @Override
    public boolean handles(@NonNull MD5CheckUrl model) {
        return true;
    }

    /**
     * The default factory for {@link com.bumptech.glide.load.model.stream.HttpGlideUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<MD5CheckUrl, InputStream> {
        private final ModelCache<MD5CheckUrl, MD5CheckUrl> modelCache = new ModelCache<>(500);

        @NonNull
        @Override
        public ModelLoader<MD5CheckUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new HttpMD5CheckUrlLoader(modelCache);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
