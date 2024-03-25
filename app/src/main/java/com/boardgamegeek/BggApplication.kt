package com.boardgamegeek

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.work.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.util.CrashReportingTree
import com.boardgamegeek.util.RemoteConfig
import com.boardgamegeek.work.PlayUploadWorker
import com.boardgamegeek.work.SyncCollectionWorker
import com.boardgamegeek.work.SyncPlaysWorker
import com.boardgamegeek.work.SyncUsersWorker
import com.facebook.stetho.Stetho
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidApp
class BggApplication : MultiDexApplication(), Configuration.Provider {
    @Inject
    @Named("withCache")
    lateinit var httpClient: OkHttpClient

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        enableStrictMode()
        initializeStetho()
        initializeFirebase()
        initializeTimber()
        initializePicasso()
        migrateData()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncCollectionWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncCollectionWorker>(1, TimeUnit.DAYS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .setConstraints(createWorkConstraints(true))
                .build()
        )
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                PlayUploadWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncPlaysWorker>(2, TimeUnit.HOURS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                    .setConstraints(createWorkConstraints(true))
                    .build()
            )
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                SyncPlaysWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncPlaysWorker>(1, TimeUnit.DAYS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                    .setConstraints(createWorkConstraints(true))
                    .build()
            )
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                SyncUsersWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncUsersWorker>(1, TimeUnit.DAYS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .setConstraints(createWorkConstraints(true))
                    .build()
            )
    }

    private fun initializeFirebase() {
        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            val firebase = FirebaseCrashlytics.getInstance()
            PreferenceManager.getDefaultSharedPreferences(this).getString(AccountPreferences.KEY_USERNAME, "")?.let {
                if (it.isNotBlank()) firebase.setUserId(it.hashCode().toString())
            }
        }
        RemoteConfig.init()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String?> ->
            if (task.isSuccessful) {
                Timber.i("Firebase token is %s", task.result)
            } else {
                Timber.w(task.exception, "Fetching FCM registration token failed")
            }
        }
    }

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }

    private fun initializeStetho() {
        if (BuildConfig.DEBUG) {
            Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build()
            )
        }
    }

    private fun initializePicasso() {
        Picasso.setSingletonInstance(
            Picasso.Builder(this)
                .downloader(OkHttp3Downloader(httpClient))
                .build()
        )
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            val builder = VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .detectFileUriExposure()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                builder.detectCleartextNetwork()
            }
            StrictMode.setVmPolicy(builder.build())
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
        }
    }

    private fun migrateData() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) NotificationChannels.create(applicationContext)
        migrateCollectionStatusSettings()
        SyncPrefs.migrate(this)
    }

    private fun migrateCollectionStatusSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val set = prefs.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null)
        if (set == null) {
            val oldSyncStatuses = prefs.getOldSyncStatuses(applicationContext)
            if (oldSyncStatuses.isNotEmpty()) {
                prefs.setSyncStatuses(oldSyncStatuses)
            }
        }
    }
}
