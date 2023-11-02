package com.boardgamegeek.di

import com.boardgamegeek.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun provideArtistDao(database: BggDatabase): ArtistDao = database.artistDao()

    @Provides
    fun provideCategoryDao(database: BggDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideCollectionViewDao(database: BggDatabase): CollectionViewDao = database.collectionViewDao()

    @Provides
    fun provideDesignerDao(database: BggDatabase): DesignerDao = database.designerDao()

    @Provides
    fun provideMechanicDao(database: BggDatabase): MechanicDao = database.mechanicDao()

    @Provides
    fun providePlayDao(database: BggDatabase): PlayDao2 = database.playDao()

    @Provides
    fun providePlayerColorDao(database: BggDatabase): PlayerColorDao = database.playerColorDao()

    @Provides
    fun providePublisherDao(database: BggDatabase): PublisherDao = database.publisherDao()

    @Provides
    fun provideUserDao(database: BggDatabase): UserDao = database.userDao()
}
