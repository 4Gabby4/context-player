package com.gabchmel.contextmusicplayer

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PredictionWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(
        appContext, workerParams
    ) {

    companion object {
        const val Progress = "progress"
        private const val delayDuration = 2000L
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {

//            val firstUpdate = workDataOf(Progress to 0)
//            val lastUpdate = workDataOf(Progress to 100)

//            setProgress(firstUpdate)
//            delay(delayDuration)
//            Log.d("WorkManager", "Working")
//            setProgress(lastUpdate)
//            delay(delayDuration)

            // Connect to the MediaBrowserService, run prediction and create notification
            MediaBrowserConnector(ProcessLifecycleOwner.get(), appContext)

            // Enqueue this unique work again so it achieves periodicity
            val tenMinutesRequest = OneTimeWorkRequestBuilder<PredictionWorker>()
                .setInitialDelay(10, java.util.concurrent.TimeUnit.MINUTES)
                .addTag("WIFIJOB1")
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork("work2",
                    ExistingWorkPolicy.KEEP,
                    tenMinutesRequest)

            Log.d("Work", "Done Work")
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}