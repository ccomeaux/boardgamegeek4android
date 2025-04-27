@file:Suppress("DEPRECATION")

package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.BuildConfig
import com.boardgamegeek.io.*
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val HTTP_REQUEST_TIMEOUT_SEC = 15L

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(RetryInterceptor(true))
        .addLoggingInterceptor()
        .build()

    @Provides
    @Singleton
    @Named("withAuth")
    fun provideHttpClientWithAuth(@ApplicationContext context: Context?) = OkHttpClient.Builder()
        .connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor(context))
        .addInterceptor(AuthInterceptor(context))
        .addInterceptor(RetryInterceptor(true))
        .addLoggingInterceptor()
        .build()

    @Provides
    @Singleton
    @Named("withCache")
    fun provideHttpClientWithCache(@ApplicationContext context: Context) = OkHttpClient.Builder()
        .connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor(context))
        .addLoggingInterceptor()
        .cache(Cache(File(context.cacheDir, "http"), 10 * 1024 * 1024))
        .build()

    @Provides
    @Singleton
    @Named("without202Retry")
    fun provideHttpClientWithout202Retry() = OkHttpClient.Builder()
        .connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(RetryInterceptor(false))
        .addLoggingInterceptor()
        .build()

    private fun OkHttpClient.Builder.addLoggingInterceptor() = apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor { message ->
                Timber.d(message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            addNetworkInterceptor(StethoInterceptor())
        }
    }

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideBggService(@Named("noAuth") httpClient: OkHttpClient): BggService = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
        .client(httpClient)
        .build()
        .create(BggService::class.java)

    @Provides
    @Singleton
    @Named("withAuth")
    fun createForXmlWithAuth(@Named("withAuth") httpClient: OkHttpClient): BggService = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
        .client(httpClient)
        .build()
        .create(BggService::class.java)

    @Provides
    @Singleton
    fun provideBggAjaxApi(@Named("noAuth") httpClient: OkHttpClient): BggAjaxApi = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(BggAjaxApi::class.java)

    @Provides
    @Singleton
    fun provideGeekdoApi(@Named("noAuth") httpClient: OkHttpClient): GeekdoApi = Retrofit.Builder()
        .client(httpClient)
        .baseUrl("https://api.geekdo.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeekdoApi::class.java)

    @Provides
    @Singleton
    fun providePhpApi(@Named("withAuth") httpClient: OkHttpClient): PhpApi = Retrofit.Builder()
        .client(httpClient)
        .baseUrl("https://boardgamegeek.com")
        .addConverterFactory(BggUploadConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PhpApi::class.java)
}
