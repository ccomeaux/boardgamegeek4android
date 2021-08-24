package com.boardgamegeek.tasks

import android.os.AsyncTask
import com.boardgamegeek.BggApplication
import com.boardgamegeek.repository.PlayRepository
import kotlinx.coroutines.runBlocking

class CalculatePlayStatsTask(private val application: BggApplication) : AsyncTask<Void, Void, Void?>() {
    private val playRepository: PlayRepository = PlayRepository(application)

    override fun doInBackground(vararg params: Void): Void? {
        runBlocking {
            playRepository.calculatePlayStats()
        }
        return null
    }
}
