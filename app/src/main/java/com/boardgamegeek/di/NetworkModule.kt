@file:Suppress("DEPRECATION")

package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.EnumConverterFactory
import com.boardgamegeek.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesBggService(): BggService = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
        .build()
        .create(BggService::class.java)

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
    fun provideHotnessRepository(api: BggService) = HotnessRepository(api)

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
