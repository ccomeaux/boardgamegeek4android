@file:Suppress("DEPRECATION")

package com.boardgamegeek.di

import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.EnumConverterFactory
import com.boardgamegeek.repository.HotnessRepository
import com.boardgamegeek.repository.TopGameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideHotnessRepository(api: BggService) = HotnessRepository(api)

    @Provides
    @Singleton
    fun provideTopGameRepository() = TopGameRepository()
}
