
# glide-helper
This library solve some problems I encountered when using Glide

 ##### 1. Application crashes caused by reuse Bitmap
An error is reported when using Glide to load pictures on some machines：    

*Fatal signal 11 (SIGSEGV) at 0x0000000f (code=1), thread 12975 (glide-source-th)*

We can use NoDirtyBitmapPool to replace LruBitmapPool
 ```java

@GlideModule
public class XXX extends AppGlideModule {
 
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
            builder.setBitmapPool(new NoDirtyBitmapPool(new MemorySizeCalculator.Builder(context).build().getBitmapPoolSize()));
    
    }
 
}
 ```

 ##### 2.Load pictures in APK
configuration：
 ```java
 @GlideModule
public class XXX extends AppGlideModule {
 
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.append(ApkResourceDrawable.class, Drawable.class, new ApkResourceLoader.Factory());
    }
}
 ```
load pictures in APK：
 ```java
ApkResourceDrawable model = new ApkResourceDrawable(context
                    , apkPath.getAbsolutePath()
                    , "yourDrawableName"
                    , "defType"
                    , "yourPackageName"
                    )
 
Glide.with(imageView).load(model).into(imageView);
 ```

 ##### 3. Image file md5 digest
Image HTTP URL with md5 parameter,the md5 parameter value represents the md5 digest value of the image file returned by the server,like this：
```html
https://192.168.0.101:5555/hello.jpg?md5=25f9e794323b453885f5181f1b624d0b
```
when loading a image, it is necessary to perform md5 digest on the obtained file and compare it with md5 parameter value. The image can be displayed only if they are the same.

configuration：

 ```java
 @GlideModule
public class XXX extends AppGlideModule {
 
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.append(MD5CheckUrl.class, InputStream.class, new HttpMD5CheckUrlLoader.Factory());
    }
 
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
 
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDiskCache(new MD5DiskLruCacheFactory(context));
 
    }
 
}
 ```

 use：
 ```java
 Glide.with(imageView)
         .load(new MD5CheckUrl.Builder("yourImageUrl", "MD5ParameterName").build())
         .apply(new MD5CheckRequestOption().diskCacheStrategy(DiskCacheStrategy.DATA))
         .into(imageView);
 ```
the md5 value of the original image loaded from the network and disk will be compared with the md5 parameter value of the image HTTP URL

 ###### ProGuard
 ```java
-keep class com.bumptech.glide.load.engine.DataCacheKey{ *;}
 ```

 
---
# glide-helper
 这个库帮助解决了与Glide相关的一些问题

 ##### 1. Bitmap复用导致的应用闪退
当在某些机器上使用Glide加载图片时报错：  
Fatal signal 11 (SIGSEGV) at 0x0000000f (code=1), thread 12975 (glide-source-th)
可以使用NoDirtyBitmapPool替换LruBitmapPool
 ```java

@GlideModule
public class XXX extends AppGlideModule {
 
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
            builder.setBitmapPool(new NoDirtyBitmapPool(new MemorySizeCalculator.Builder(context).build().getBitmapPoolSize()));
    
    }
 
}
 ```

 ##### 2.加载APK中的图片
 配置：
 ```java
 @GlideModule
public class XXX extends AppGlideModule {
 
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.append(ApkResourceDrawable.class, Drawable.class, new ApkResourceLoader.Factory());
    }
}
 ```
 加载APK中的图片：
 ```java
ApkResourceDrawable model = new ApkResourceDrawable(context
                    , apkPath.getAbsolutePath()//apk所在路径
                    , "yourDrawableName"//drawable的名字
                    , "defType"//defType类型
                    , "yourPackageName"//APK的包名
                    )
 
Glide.with(imageView).load(model).into(imageView);
 ```

 ##### 3. 图片文件 MD5 checksum
图片链接中带有MD5 ParameterName-Value,MD5 ParameterValue表示服务器返回的图片文件的MD5值,像这样：
```html
https://192.168.0.101:5555/hello.jpg?md5=25f9e794323b453885f5181f1b624d0b
```

加载图片时需要对得到的文件进行md5数据摘要，并与MD5 ParameterValue进行比较，相同才能显示图片.


 配置：

 ```java
 @GlideModule
public class XXX extends AppGlideModule {
 
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.append(MD5CheckUrl.class, InputStream.class, new HttpMD5CheckUrlLoader.Factory());
    }
 
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
 
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDiskCache(new MD5DiskLruCacheFactory(context));
 
    }
 
}
 ```

 使用：
 ```java
 Glide.with(imageView)
         .load(new MD5CheckUrl.Builder("yourImageUrl", "MD5ParameterName").build())
         .apply(new MD5CheckRequestOption().diskCacheStrategy(DiskCacheStrategy.DATA))
         .into(imageView);
 ```
会对从网络、磁盘加载的原始图片与HTTP连接带的MD5值进行MD5比对

 ###### ProGuard
 ```java
 #使用图片MD5比对时添加
-keep class com.bumptech.glide.load.engine.DataCacheKey{ *;}
 ```

