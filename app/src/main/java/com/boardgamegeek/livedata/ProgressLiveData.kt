package com.boardgamegeek.livedata

import androidx.lifecycle.LiveData

class ProgressLiveData : LiveData<ProgressData>() {
    private var progressData = ProgressData()

    fun start() {
        progressData = ProgressData(mode = ProgressData.Mode.INDETERMINATE)
        this.postValue(progressData)
    }

    fun start(max: Int) {
        progressData = ProgressData(0, max, ProgressData.Mode.DETERMINATE)
        this.postValue(progressData)
    }

    fun update(current: Int) {
        progressData = progressData.copy(current = current)
        this.postValue(progressData)
    }

    fun complete() {
        progressData = progressData.copy(current = progressData.max, mode = ProgressData.Mode.OFF)
        this.postValue(progressData)
    }
}
