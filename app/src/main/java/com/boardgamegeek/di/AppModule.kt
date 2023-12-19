package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.db.*
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
    fun provideArtistRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, artistDao: ArtistDao, collectionDao: CollectionDao, imageRepository: ImageRepository) =
        ArtistRepository(context, api, artistDao, collectionDao, imageRepository)

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context, @Named("without202Retry") httpClient: OkHttpClient) =
        AuthRepository(context, httpClient)

    @Provides
    @Singleton
    fun provideCategoryRepository(@ApplicationContext context: Context, categoryDao: CategoryDao, collectionDao: CollectionDao) = CategoryRepository(context, categoryDao, collectionDao)

    @Provides
    @Singleton
    fun provideCollectionViewRepository(@ApplicationContext context: Context, collectionViewDao: CollectionViewDao) = CollectionViewRepository(context, collectionViewDao)

    @Provides
    @Singleton
    fun provideDesignerRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, designerDao: DesignerDao, collectionDao: CollectionDao, imageRepository: ImageRepository) =
        DesignerRepository(context, api, designerDao, collectionDao, imageRepository)

    @Provides
    @Singleton
    fun provideForumRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService) = ForumRepository(context, api)

    @Provides
    @Singleton
    fun provideGameRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, imageRepository: ImageRepository, playDao: PlayDao, gameColorDao: GameColorDao, gameDao: GameDao, artistDao: ArtistDao, designerDao: DesignerDao, publisherDao: PublisherDao, categoryDao: CategoryDao, mechanicDao: MechanicDao, collectionDao: CollectionDao) =
        GameRepository(context, api, imageRepository, playDao, gameColorDao, gameDao, artistDao, designerDao, publisherDao, categoryDao, mechanicDao, collectionDao)

    @Provides
    @Singleton
    fun provideGameCollectionRepository(@ApplicationContext context: Context, @Named("withAuth") api: BggService, imageRepository: ImageRepository, phpApi: PhpApi, gameDao: GameDao, collectionDao: CollectionDao) =
        GameCollectionRepository(context, api, imageRepository, phpApi, gameDao, collectionDao)

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
    fun provideMechanicRepository(@ApplicationContext context: Context, mechanicDao: MechanicDao, collectionDao: CollectionDao) = MechanicRepository(context, mechanicDao, collectionDao)

    @Provides
    @Singleton
    fun providePlayRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, phpApi: PhpApi, playDao: PlayDao, playerColorDao: PlayerColorDao, userDao: UserDao, gameColorDao: GameColorDao, gameDao: GameDao, collectionDao: CollectionDao) =
        PlayRepository(context, api, phpApi, playDao, playerColorDao, userDao, gameColorDao, gameDao, collectionDao)

    @Provides
    @Singleton
    fun providePublisherRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, publisherDao: PublisherDao, collectionDao: CollectionDao, imageRepository: ImageRepository) =
        PublisherRepository(context, api, publisherDao, collectionDao, imageRepository)

    @Provides
    @Singleton
    fun provideSearchRepository(@Named("noAuth") api: BggService) = SearchRepository(api)

    @Provides
    @Singleton
    fun provideTopGameRepository() = TopGameRepository()

    @Provides
    @Singleton
    fun provideUserRepository(@ApplicationContext context: Context, @Named("noAuth") api: BggService, userDao: UserDao) = UserRepository(context, api, userDao)
}
