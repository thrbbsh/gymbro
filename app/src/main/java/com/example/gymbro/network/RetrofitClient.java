package com.example.gymbro.network;

import android.content.Context;
import com.example.gymbro.BuildConfig;
import java.io.File;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://" + BuildConfig.PROXY_HOST_LAN + ":" + BuildConfig.PROXY_PORT + "/";
    
    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;

    public static OkHttpClient getOkHttpClient(Context context) {
        if (okHttpClient == null) {
            // Initialize 50MB Cache
            File cacheDir = new File(context.getCacheDir(), "http_cache");
            Cache cache = new Cache(cacheDir, 50 * 1024 * 1024);

            okHttpClient = new OkHttpClient.Builder()
                    .cache(cache)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addNetworkInterceptor(chain -> {
                        // Force cache for 24 hours as proxy might not send cache headers
                        okhttp3.Response response = chain.proceed(chain.request());
                        return response.newBuilder()
                                .header("Cache-Control", "public, max-age=86400")
                                .removeHeader("Pragma")
                                .build();
                    })
                    .build();
        }
        return okHttpClient;
    }

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
