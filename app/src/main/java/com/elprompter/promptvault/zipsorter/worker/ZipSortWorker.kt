package com.elprompter.promptvault.zipsorter.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.elprompter.promptvault.zipsorter.model.SortConfig
import com.elprompter.promptvault.zipsorter.model.SortState
import com.elprompter.promptvault.zipsorter.repository.ZipSorterRepositoryImpl
import kotlinx.coroutines.flow.collectLatest

/**
 * Fix vs draft asli: `override async suspend fun doWork()` bukan syntax
 * Kotlin yang valid (tidak ada keyword `async` di deklarasi fungsi) --
 * dibetulkan jadi `override suspend fun doWork()` polos.
 */
class ZipSortWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val treeUriString = inputData.getString(KEY_TREE_URI) ?: return Result.failure()
        val autoExtract = inputData.getBoolean(KEY_AUTO_EXTRACT, true)

        val repository = ZipSorterRepositoryImpl(applicationContext)
        val config = SortConfig(autoExtractZip = autoExtract)
        val uri = Uri.parse(treeUriString)

        var isSuccess = false

        repository.processFolder(uri, config).collectLatest { state ->
            when (state) {
                is SortState.Processing -> {
                    setProgress(
                        workDataOf(
                            PROGRESS_FILE to state.fileName,
                            PROGRESS_PERCENT to state.progressPercent
                        )
                    )
                }
                is SortState.Success -> isSuccess = true
                is SortState.Error -> isSuccess = false
                else -> {}
            }
        }

        return if (isSuccess) Result.success() else Result.failure()
    }

    companion object {
        const val KEY_TREE_URI = "key_tree_uri"
        const val KEY_AUTO_EXTRACT = "key_auto_extract"
        const val PROGRESS_FILE = "progress_file"
        const val PROGRESS_PERCENT = "progress_percent"
    }
}
