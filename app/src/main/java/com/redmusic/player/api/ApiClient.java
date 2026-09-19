package com.redmusic.player.api;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClient {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build();
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);

    public interface Callback {
        void onResult(String body);

        void onError(String msg);
    }

    public static void get(String url, Callback cb) {
        pool.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();
                try (Response r = client.newCall(req).execute()) {
                    String body = r.body() != null ? r.body().string() : "";
                    cb.onResult(body);
                }
            } catch (IOException e) {
                cb.onError(e.getMessage() == null ? "网络错误" : e.getMessage());
            }
        });
    }
}
