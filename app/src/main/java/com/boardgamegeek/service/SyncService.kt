package com.boardgamegeek.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.boardgamegeek.repository.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var gameCollectionRepository: GameCollectionRepository

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
