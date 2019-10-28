package com.boardgamegeek.io

import com.boardgamegeek.util.RemoteConfig
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class RetryInterceptor : Interceptor {
    private val initialIntervalMillisFor202 = RemoteConfig.getInt(RemoteConfig.KEY_RETRY_202_INITIAL_INTERVAL_MILLIS)
    private val randomizationFactorFor202 = RemoteConfig.getDouble(RemoteConfig.KEY_RETRY_202_RANDOMIZATION_FACTOR)
    private val multiplierFor202 = RemoteConfig.getDouble(RemoteConfig.KEY_RETRY_202_MULTIPLIER)
    private val maxIntervalMillisFor202 = RemoteConfig.getInt(RemoteConfig.KEY_RETRY_202_MAX_INTERVAL_MILLIS)
    private val maxElapsedMillisFor202 = RemoteConfig.getInt(RemoteConfig.KEY_RETRY_202_MAX_ELAPSED_MILLIS)
    private val maxBackOffCountFor429 = RemoteConfig.getInt(RemoteConfig.KEY_RETRY_429_MAX_BACKOFF_COUNT)

    private val backOff202: BackOff
    private val backOff429: BackOff
    private val backOff503: BackOff

    init {
        backOff202 = ExponentialBackOff(initialIntervalMillisFor202, randomizationFactorFor202, multiplierFor202, maxIntervalMillisFor202, maxElapsedMillisFor202)
        backOff429 = FixedBackOff(maxBackOffCount = maxBackOffCountFor429)
        backOff503 = FixedBackOff()
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        resetBackOff()
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)
        var millis = nextBackOffMillis(response)
        while (millis != BackOff.STOP) {
            response.close()

            Timber.d("...sleeping for %,d ms", millis)
            try {
                Thread.sleep(millis)
            } catch (e: InterruptedException) {
                Timber.w(e, "Interrupted while sleeping during retry.")
                return response
            }

            Timber.d("...retrying")
            response = chain.proceed(originalRequest)
            millis = nextBackOffMillis(response)
        }
        return response
    }

    private fun resetBackOff() {
        backOff202.reset()
        backOff429.reset()
        backOff503.reset()
    }

    private fun nextBackOffMillis(response: Response): Long {
        return when (response.code) {
            COLLECTION_REQUEST_PROCESSING -> backOff202.nextBackOffMillis()
            RATE_LIMIT_EXCEEDED -> backOff429.nextBackOffMillis()
            API_RATE_EXCEEDED -> backOff503.nextBackOffMillis()
            else -> BackOff.STOP
        }
    }

    companion object {
        private const val COLLECTION_REQUEST_PROCESSING = 202
        private const val RATE_LIMIT_EXCEEDED = 429
        private const val API_RATE_EXCEEDED = 503
    }
}
