package com.msisuzney.glidehelper.request;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.msisuzney.glidehelper.fetcher.HttpMD5CheckUrlLoader;

public class MD5CheckRequestOption extends RequestOptions {
    @NonNull
    @Override
    public RequestOptions diskCacheStrategy(@NonNull DiskCacheStrategy strategy) {
        RequestOptions requestOptions = super.diskCacheStrategy(strategy);
        return requestOptions.set(HttpMD5CheckUrlLoader.DISKCACHESTRATEGY, strategy);
    }
}
