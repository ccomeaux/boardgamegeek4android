package com.boardgamegeek.di

import android.content.Context
import androidx.room.Room
import com.boardgamegeek.db.BggDatabase
import com.boardgamegeek.db.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun providesBggDatabase(
        @ApplicationContext context: Context,
    ): BggDatabase = Room.databaseBuilder(
        context,
        BggDatabase::class.java,
        "bgg.db",
    ).addMigrations(
        DatabaseMigrations.MIGRATION_60_61,
        DatabaseMigrations.MIGRATION_61_62,
        DatabaseMigrations.MIGRATION_62_63,
        DatabaseMigrations.MIGRATION_63_64,
    ).build()
}