package com.boardgamegeek.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boardgamegeek.db.model.*

@Database(
    entities = [
        DesignerEntity::class,
        ArtistEntity::class,
        PublisherEntity::class,
        CategoryEntity::class,
        MechanicEntity::class,
        CollectionViewEntity::class,
        CollectionViewFilterEntity::class,
        GameEntity::class,
        GameColorsEntity::class,
        GamePollEntity::class,
        GamePollResultsEntity::class,
        GamePollResultsResultEntity::class,
        GameRankEntity::class,
        GameSuggestedPlayerCountPollResultsEntity::class,
        GameArtistEntity::class,
        GameCategoryEntity::class,
        GameDesignerEntity::class,
        GameExpansionEntity::class,
        GameMechanicEntity::class,
        GamePublisherEntity::class,
        CollectionItemEntity::class,
        PlayPlayerEntity::class,
        PlayerColorsEntity::class,
        PlayEntity::class,
        UserEntity::class
    ],
    version = 61,
)
@TypeConverters(DateConverter::class)
abstract class BggDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun categoryDao(): CategoryDao
    abstract fun collectionViewDao(): CollectionViewDao
    abstract fun designerDao(): DesignerDao
    abstract fun mechanicDao(): MechanicDao
    abstract fun playDao(): PlayDao
    abstract fun playerColorDao(): PlayerColorDao
    abstract fun publisherDao(): PublisherDao
    abstract fun userDao(): UserDao
}
