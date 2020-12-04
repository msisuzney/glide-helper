package com.msisuzney.glidehelper.fetcher;


import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Synthetic;
import com.msisuzney.glidehelper.LogSwitch;
import com.msisuzney.glidehelper.model.MD5CheckUrl;
import com.msisuzney.glidehelper.utils.MD5Utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * A DataFetcher that retrieves an {@link InputStream} for a Url.
 */
public class HttpMD5CheckUrlFetcher implements DataFetcher<InputStream> {
    private static final String TAG = "MD5Fetcher";
    private static final int MAXIMUM_REDIRECTS = 5;
    @VisibleForTesting
    static final String REDIRECT_HEADER_FIELD = "Location";

    @VisibleForTesting
    static final HttpUrlConnectionFactory DEFAULT_CONNECTION_FACTORY =
            new DefaultHttpUrlConnectionFactory();
    /**
     * Returned when a connection error prevented us from receiving an http error.
     */
    @VisibleForTesting
    static final int INVALID_STATUS_CODE = -1;

    private final MD5CheckUrl glideUrl;
    private final int timeout;
    private final HttpUrlConnectionFactory connectionFactory;

    private HttpURLConnection urlConnection;
    private InputStream stream;
    private volatile boolean isCancelled;
    private final DiskCacheStrategy diskCacheStrategy;

    public HttpMD5CheckUrlFetcher(MD5CheckUrl glideUrl, int timeout, DiskCacheStrategy diskCacheStrategy) {
        this(glideUrl, timeout, DEFAULT_CONNECTION_FACTORY, diskCacheStrategy);
    }

    @VisibleForTesting
    HttpMD5CheckUrlFetcher(MD5CheckUrl glideUrl, int timeout, HttpUrlConnectionFactory connectionFactory, DiskCacheStrategy diskCacheStrategy) {
        this.glideUrl = glideUrl;
        this.timeout = timeout;
        this.connectionFactory = connectionFactory;
        this.diskCacheStrategy = diskCacheStrategy;
    }

    @Override
    public void loadData(
            @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        long startTime = LogTime.getLogTime();
        try {
            InputStream result = loadDataWithRedirects(glideUrl.toURL(), 0, null, glideUrl.getHeaders());
            if (LogSwitch.LOGGING) {
                Log.d(TAG, " result:  " + result + ",glideUrl:" + glideUrl.toURL());
            }
            //如果是缓存原始数据，不做校验了，数据会被写到diskcache，然后读取，在DataCacheGenerator那里做md5比对，这样就避免两次比对。
            if (diskCacheStrategy.equals(DiskCacheStrategy.DATA)) {
                callback.onDataReady(result);
                return;
            }
            long startUps = 0;
            if (LogSwitch.LOGGING) {
                Log.d(TAG, "url:" + glideUrl.toStringUrl());
                startUps = SystemClock.uptimeMillis();
            }
            if (result == null) {
                callback.onDataReady(null);
                return;
            }

            String md5Param = glideUrl.getMd5Value();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(result, baos);
            byte[] data = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            String sum = MD5Utils.encode(bais).toLowerCase();
            if (LogSwitch.LOGGING) {
                Log.d(TAG, "checksum:" + sum + ",md5Param:" + md5Param);
                Log.d(TAG, "md5 checksum cost:" + (SystemClock.uptimeMillis() - startUps) + "ms");
            }
            if (md5Param.equals(sum)) {
                bais.reset();
                callback.onDataReady(bais);
            } else {
                callback.onLoadFailed(new RuntimeException("MD5 check failed"));
            }

        } catch (IOException e) {
            if (LogSwitch.LOGGING) {
                Log.d(TAG, "Failed to load data for url", e);
            }
            callback.onLoadFailed(e);
        } finally {
            if (LogSwitch.LOGGING) {
                Log.v(TAG, "Finished http url fetcher fetch in " + LogTime.getElapsedMillis(startTime));
            }
        }
    }

    private InputStream loadDataWithRedirects(
            URL url, int redirects, URL lastUrl, Map<String, String> headers) throws HttpException {
        if (redirects >= MAXIMUM_REDIRECTS) {
            throw new HttpException(
                    "Too many (> " + MAXIMUM_REDIRECTS + ") redirects!", INVALID_STATUS_CODE);
        } else {
            // Comparing the URLs using .equals performs additional network I/O and is generally broken.
            // See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
            try {
                if (lastUrl != null && url.toURI().equals(lastUrl.toURI())) {
                    throw new HttpException("In re-direct loop", INVALID_STATUS_CODE);
                }
            } catch (URISyntaxException e) {
                // Do nothing, this is best effort.
            }
        }

        urlConnection = buildAndConfigureConnection(url, headers);

        try {
            // Connect explicitly to avoid errors in decoders if connection fails.
            urlConnection.connect();
            // Set the stream so that it's closed in cleanup to avoid resource leaks. See #2352.
            stream = urlConnection.getInputStream();
        } catch (IOException e) {
            throw new HttpException(
                    "Failed to connect or obtain data", getHttpStatusCodeOrInvalid(urlConnection), e);
        }

        if (isCancelled) {
            return null;
        }

        final int statusCode = getHttpStatusCodeOrInvalid(urlConnection);
        if (isHttpOk(statusCode)) {
            return getStreamForSuccessfulRequest(urlConnection);
        } else if (isHttpRedirect(statusCode)) {
            String redirectUrlString = urlConnection.getHeaderField(REDIRECT_HEADER_FIELD);
            if (TextUtils.isEmpty(redirectUrlString)) {
                throw new HttpException("Received empty or null redirect url", statusCode);
            }
            URL redirectUrl;
            try {
                redirectUrl = new URL(url, redirectUrlString);
            } catch (MalformedURLException e) {
                throw new HttpException("Bad redirect url: " + redirectUrlString, statusCode, e);
            }
            // Closing the stream specifically is required to avoid leaking ResponseBodys in addition
            // to disconnecting the url connection below. See #2352.
            cleanup();
            return loadDataWithRedirects(redirectUrl, redirects + 1, url, headers);
        } else if (statusCode == INVALID_STATUS_CODE) {
            throw new HttpException(statusCode);
        } else {
            try {
                throw new HttpException(urlConnection.getResponseMessage(), statusCode);
            } catch (IOException e) {
                throw new HttpException("Failed to get a response message", statusCode, e);
            }
        }
    }

    private static int getHttpStatusCodeOrInvalid(HttpURLConnection urlConnection) {
        try {
            return urlConnection.getResponseCode();
        } catch (IOException e) {
            if (LogSwitch.LOGGING) {
                Log.d(TAG, "Failed to get a response code", e);
            }
        }
        return INVALID_STATUS_CODE;
    }

    private HttpURLConnection buildAndConfigureConnection(URL url, Map<String, String> headers)
            throws HttpException {
        HttpURLConnection urlConnection;
        try {
            urlConnection = connectionFactory.build(url);
        } catch (IOException e) {
            throw new HttpException("URL.openConnection threw", /*statusCode=*/ 0, e);
        }
        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            urlConnection.addRequestProperty(headerEntry.getKey(), headerEntry.getValue());
        }
        urlConnection.setConnectTimeout(timeout);
        urlConnection.setReadTimeout(timeout);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);
        // Stop the urlConnection instance of HttpUrlConnection from following redirects so that
        // redirects will be handled by recursive calls to this method, loadDataWithRedirects.
        urlConnection.setInstanceFollowRedirects(false);
        return urlConnection;
    }

    // Referencing constants is less clear than a simple static method.
    private static boolean isHttpOk(int statusCode) {
        return statusCode / 100 == 2;
    }

    // Referencing constants is less clear than a simple static method.
    private static boolean isHttpRedirect(int statusCode) {
        return statusCode / 100 == 3;
    }

    private InputStream getStreamForSuccessfulRequest(HttpURLConnection urlConnection)
            throws HttpException {
        try {
            if (TextUtils.isEmpty(urlConnection.getContentEncoding())) {
                int contentLength = urlConnection.getContentLength();
                stream = ContentLengthInputStream.obtain(urlConnection.getInputStream(), contentLength);
            } else {
                if (LogSwitch.LOGGING) {
                    Log.d(TAG, "Got non empty content encoding: " + urlConnection.getContentEncoding());
                }
                stream = urlConnection.getInputStream();
            }
        } catch (IOException e) {
            throw new HttpException(
                    "Failed to obtain InputStream", getHttpStatusCodeOrInvalid(urlConnection), e);
        }
        return stream;
    }

    @Override
    public void cleanup() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
        urlConnection = null;
    }

    @Override
    public void cancel() {
        // TODO: we should consider disconnecting the url connection here, but we can't do so
        // directly because cancel is often called on the main thread.
        isCancelled = true;
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }

    interface HttpUrlConnectionFactory {
        HttpURLConnection build(URL url) throws IOException;
    }

    private static class DefaultHttpUrlConnectionFactory implements HttpUrlConnectionFactory {

        @Synthetic
        DefaultHttpUrlConnectionFactory() {
        }

        @Override
        public HttpURLConnection build(URL url) throws IOException {
            return (HttpURLConnection) url.openConnection();
        }
    }
}

