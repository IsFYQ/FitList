package com.example.healthcheckin.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healthcheckin.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = backupRepository.triggerBackup(force = false)
        return when {
            result.skipped -> Result.success()
            result.success -> Result.success()
            else -> Result.retry()
        }
    }
}
