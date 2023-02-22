@file:Suppress("DEPRECATION")

package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.BuildConfig
import com.boardgamegeek.io.*
import com.boardgamegeek.repository.*
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val HTTP_REQUEST_TIMEOUT_SEC = 15L

    @Provides
    @Singleton
    fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor(null))
        .addInterceptor(RetryInterceptor(true))
        .addLoggingInterceptor()
        .build()

    private fun OkHttpClient.Builder.addLoggingInterceptor() = apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            addNetworkInterceptor(StethoInterceptor())
        }
    }

    @Provides
    @Singleton
    fun provideBggService(httpClient: OkHttpClient): BggService = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
        .client(httpClient)
        .build()
        .create(BggService::class.java)

    @Provides
    @Singleton
    fun provideBggAjaxApi(httpClient: OkHttpClient): BggAjaxApi = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(BggAjaxApi::class.java)

    @Provides
    @Singleton
    fun provideArtistRepository(@ApplicationContext context: Context, api: BggService) = ArtistRepository(context, api)

    @Provides
    @Singleton
    fun provideDesignerRepository(@ApplicationContext context: Context, api: BggService) = DesignerRepository(context, api)

    @Provides
    @Singleton
    fun provideForumRepository(@ApplicationContext context: Context, api: BggService) = ForumRepository(context, api)

    @Provides
    @Singleton
    fun provideGameRepository(@ApplicationContext context: Context, api: BggService, playRepository: PlayRepository) =
        GameRepository(context, api, playRepository)

    @Provides
    @Singleton
    fun provideGeekListRepository(api: BggService, ajaxApi: BggAjaxApi) = GeekListRepository(api, ajaxApi)

    @Provides
    @Singleton
    fun provideHotnessRepository(api: BggService) = HotnessRepository(api)

    @Provides
    @Singleton
    fun providePlayRepository(@ApplicationContext context: Context, api: BggService) = PlayRepository(context, api)

    @Provides
    @Singleton
    fun providePublisherRepository(@ApplicationContext context: Context, api: BggService) = PublisherRepository(context, api)

    @Provides
    @Singleton
    fun provideSearchRepository(api: BggService) = SearchRepository(api)

    @Provides
    @Singleton
    fun provideTopGameRepository() = TopGameRepository()

    @Provides
    @Singleton
    fun provideUserRepository(@ApplicationContext context: Context, api: BggService) = UserRepository(context, api)
}
