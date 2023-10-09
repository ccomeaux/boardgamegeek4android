package com.boardgamegeek.di

import com.boardgamegeek.db.BggDatabase
import com.boardgamegeek.db.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun providesUserDao(database: BggDatabase): UserDao = database.userDao()
}
