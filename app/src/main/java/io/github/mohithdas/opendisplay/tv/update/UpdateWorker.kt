package io.github.mohithdas.opendisplay.tv.update

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (UpdateManager.get(applicationContext).backgroundCheck()) {
        UpdateError.OFFLINE, UpdateError.TIMEOUT, UpdateError.SERVER -> Result.retry()
        else -> Result.success()
    }
}

object UpdateScheduler {
    const val UNIQUE_WORK_NAME = "opendisplay-tv-daily-update-check"

    fun request(): androidx.work.PeriodicWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
        24,
        TimeUnit.HOURS,
    ).setConstraints(
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
    ).setInitialDelay(24, TimeUnit.HOURS)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
        .build()

    fun schedule(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request(),
        )
    }
}
