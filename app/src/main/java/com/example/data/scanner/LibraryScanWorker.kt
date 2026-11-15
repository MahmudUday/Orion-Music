package com.example.data.scanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.OrionDatabase
import com.example.data.local.SettingsDataStore
import java.util.concurrent.TimeUnit

class LibraryScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = OrionDatabase.getInstance(applicationContext)
            val settings = SettingsDataStore(applicationContext)
            val scanner = MusicLibraryScanner(applicationContext, database, settings)
            scanner.scanLibrary()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_IMMEDIATE_SCAN = "orion_immediate_library_scan"
        private const val UNIQUE_PERIODIC_SCAN = "orion_periodic_library_scan"

        fun enqueueImmediateScan(context: Context) {
            val request = OneTimeWorkRequestBuilder<LibraryScanWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_IMMEDIATE_SCAN,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun schedulePeriodicScan(context: Context) {
            val request = PeriodicWorkRequestBuilder<LibraryScanWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_SCAN,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
