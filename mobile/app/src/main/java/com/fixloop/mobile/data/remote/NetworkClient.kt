package com.fixloop.mobile.data.remote

import android.content.Context
import com.fixloop.mobile.data.local.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private var retrofitInstance: Retrofit? = null
    private var cachedBaseUrl: String? = null

    /**
     * Forces the next [getApi] call to rebuild Retrofit with the current base URL.
     * Call this whenever the user changes the connection mode or hotspot IP.
     */
    fun invalidate() {
        retrofitInstance = null
        cachedBaseUrl = null
    }

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    fun getApi(context: Context): FixLoopApi {
        val sessionManager = SessionManager(context)
        val currentBaseUrl = sessionManager.getBaseUrl()

        if (retrofitInstance == null || cachedBaseUrl != currentBaseUrl) {
            cachedBaseUrl = currentBaseUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                val token = sessionManager.getToken()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                requestBuilder.header("Accept", "application/json")

                val response = chain.proceed(requestBuilder.build())

                if (response.code == 401) {
                    // Token expired or invalid
                    sessionManager.clearSession()
                }

                response
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            retrofitInstance = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

        return retrofitInstance!!.create(FixLoopApi::class.java)
    }

    fun getRawOkHttpClient(context: Context): OkHttpClient {
        val sessionManager = SessionManager(context)
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val token = sessionManager.getToken()
                val builder = original.newBuilder()
                if (!token.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSockets
            .retryOnConnectionFailure(true)
            .build()
    }
}
