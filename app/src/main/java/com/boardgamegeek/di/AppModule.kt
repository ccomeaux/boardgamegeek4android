package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.BggService
import com.boardgamegeek.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCollectionItemRepository(@ApplicationContext context: Context, @Named("withAuth") api: BggService) =
        CollectionItemRepository(context, api)

    @Provides
    @Singleton
    fun provideArtistRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = ArtistRepository(context, api)

    @Provides
    @Singleton
    fun provideDesignerRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = DesignerRepository(context, api)

    @Provides
    @Singleton
    fun provideForumRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = ForumRepository(context, api)

    @Provides
    @Singleton
    fun provideGameRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = GameRepository(context, api)

    @Provides
    @Singleton
    fun provideGeekListRepository(@Named("noAuth") api: BggService, ajaxApi: BggAjaxApi) = GeekListRepository(api, ajaxApi)

    @Provides
    @Singleton
    fun provideHotnessRepository(@Named("noAuth") api: BggService) = HotnessRepository(api)

    @Provides
    @Singleton
    fun providePlayRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = PlayRepository(context, api)

    @Provides
    @Singleton
    fun providePublisherRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = PublisherRepository(context, api)

    @Provides
    @Singleton
    fun provideSearchRepository(@Named("noAuth") api: BggService) = SearchRepository(api)

    @Provides
    @Singleton
    fun provideTopGameRepository() = TopGameRepository()

    @Provides
    @Singleton
    fun provideUserRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = UserRepository(context, api)
}
