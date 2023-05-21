package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.GeekdoApi
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideArtistRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, imageRepository: ImageRepository) =
        ArtistRepository(context, api, imageRepository)

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context, @Named("without202Retry") httpClient: OkHttpClient) =
        AuthRepository(context, httpClient)

    @Provides
    @Singleton
    fun provideCategoryRepository(@ApplicationContext context: Context) = CategoryRepository(context)

    @Provides
    @Singleton
    fun provideCollectionItemRepository(@ApplicationContext context: Context, @Named("withAuth") api: BggService) =
        CollectionItemRepository(context, api)

    @Provides
    @Singleton
    fun provideCollectionViewRepository(@ApplicationContext context: Context) = CollectionViewRepository(context)

    @Provides
    @Singleton
    fun provideDesignerRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, imageRepository: ImageRepository) =
        DesignerRepository(context, api, imageRepository)

    @Provides
    @Singleton
    fun provideForumRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = ForumRepository(context, api)

    @Provides
    @Singleton
    fun provideGameRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, imageRepository: ImageRepository) =
        GameRepository(context, api, imageRepository)

    @Provides
    @Singleton
    fun provideGameCollectionRepository(@ApplicationContext context: Context, @Named("withAuth") api: BggService, imageRepository: ImageRepository) =
        GameCollectionRepository(context, api, imageRepository)

    @Provides
    @Singleton
    fun provideGeekListRepository(@Named("noAuth") api: BggService, ajaxApi: BggAjaxApi) = GeekListRepository(api, ajaxApi)

    @Provides
    @Singleton
    fun provideHotnessRepository(@Named("noAuth") api: BggService) = HotnessRepository(api)

    @Provides
    @Singleton
    fun provideImageRepository(@ApplicationContext context: Context, geekdoApi: GeekdoApi) = ImageRepository(context, geekdoApi)

    @Provides
    @Singleton
    fun provideMechanicRepository(@ApplicationContext context: Context) = MechanicRepository(context)

    @Provides
    @Singleton
    fun providePlayRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, phpApi: PhpApi) = PlayRepository(context, api, phpApi)

    @Provides
    @Singleton
    fun providePublisherRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, imageRepository: ImageRepository) =
        PublisherRepository(context, api, imageRepository)

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
