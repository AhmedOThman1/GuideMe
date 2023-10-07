package com.guideMe.FCM;

import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;

public class FCM_ApiClient {

    private static final String BASE_URL = "https://fcm.googleapis.com";
    private static final String ServerKey = "AAAA-XkIkrw:APA91bGcjDui_j3f-KesfYeMqqJh1O9SG2s0XYu71p3OizAptdYTDq9B-nwBTk-Ptmh_h64dQmLMy3fxuZRJwVHKbL5yDacrIGSJRF-It5fmJrn-2VCnwW15-u22RDzfGox9pOi7zAYf";
    private final FCM_Interface fcm_interface;
    private static FCM_ApiClient INSTANCE;

    public FCM_ApiClient() {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        fcm_interface = retrofit.create(FCM_Interface.class);

    }

    public static FCM_ApiClient getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new FCM_ApiClient();
        }
        return INSTANCE;
    }

    public Call<JsonObject> pushNotification(@Body() PushNotification notification) {
        return fcm_interface.pushNotification("key=" + ServerKey, notification);
    }


}
