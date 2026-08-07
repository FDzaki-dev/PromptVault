package com.elprompter.promptvault.zipsorter.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.elprompter.promptvault.zipsorter.model.FileCategory
import com.elprompter.promptvault.zipsorter.model.SortConfig
import com.elprompter.promptvault.zipsorter.model.SortState
import com.elprompter.promptvault.zipsorter.util.ZipFileUriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

interface ZipSorterRepository {
    fun processFolder(targetTreeUri: Uri, config: SortConfig): Flow<SortState>
}

class ZipSorterRepositoryImpl(
    private val context: Context
) : ZipSorterRepository {

    override fun processFolder(targetTreeUri: Uri, config: SortConfig): Flow<SortState> = flow {
        emit(SortState.Scanning("Membuka direktori..."))

        val targetDir = DocumentFile.fromTreeUri(context, targetTreeUri)
        // Insiden #6/PROJECT_STATE: isDirectory dipakai sbg negative-check yang
        // reliable, JANGAN pakai exists()/canRead()/isFile() sbg gerbang di sini.
        if (targetDir == null || !targetDir.isDirectory) {
            emit(SortState.Error("Akses direktori ditolak atau URI tidak valid."))
            return@flow
        }

        val filesList = try {
            targetDir.listFiles().filter { !it.isDirectory && it.name != null }
        } catch (e: Exception) {
            emit(SortState.Error("Gagal membaca isi folder: ${e.message}", e))
            return@flow
        }

        val totalFiles = filesList.size
        if (totalFiles == 0) {
            emit(SortState.Success(0, 0))
            return@flow
        }

        var processedCount = 0
        var extractedZipCount = 0

        filesList.forEachIndexed { index, docFile ->
            val fileName = docFile.name ?: return@forEachIndexed
            val progress = ((index + 1).toDouble() / totalFiles * 100).toInt()

            emit(SortState.Processing(fileName, progress))

            try {
                val extension = fileName.substringAfterLast('.', "")

                if (extension.equals("zip", ignoreCase = true) && config.autoExtractZip) {
                    val extractSuccess = extractZip(docFile, targetDir)
                    if (extractSuccess) {
                        extractedZipCount++
                        if (config.deleteZipAfterExtract) {
                            docFile.delete()
                        } else {
                            moveFileToCategory(docFile, targetDir, FileCategory.ARCHIVES)
                        }
                    } else {
                        moveFileToCategory(docFile, targetDir, FileCategory.ARCHIVES)
                    }
                } else {
                    val category = FileCategory.fromExtension(extension)
                    if (category != FileCategory.OTHERS) {
                        moveFileToCategory(docFile, targetDir, category)
                    }
                }
                processedCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        emit(SortState.Success(processedCount, extractedZipCount))
    }.flowOn(Dispatchers.IO)

    private fun extractZip(zipDocFile: DocumentFile, rootDir: DocumentFile): Boolean {
        val contentResolver = context.contentResolver
        val zipName = zipDocFile.name?.substringBeforeLast('.') ?: "Extracted_Zip"
        val outputFolder = ZipFileUriHelper.getOrCreateSubFolder(rootDir, zipName)

        val inputStream = contentResolver.openInputStream(zipDocFile.uri) ?: return false

        return try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)

                while (entry != null) {
                    val entryName = entry.name

                    // Proteksi dari Zip Slip Vulnerability
                    if (entryName.contains("..")) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        createNestedFolders(outputFolder, entryName)
                    } else {
                        val targetFolder = getTargetFolderForEntry(outputFolder, entryName)
                        val actualFileName = entryName.substringAfterLast('/')
                        val newFile = ZipFileUriHelper.getUniqueTargetFile(targetFolder, actualFileName)

                        contentResolver.openOutputStream(newFile.uri)?.use { os ->
                            BufferedOutputStream(os).use { bos ->
                                var count: Int
                                while (zis.read(buffer).also { count = it } != -1) {
                                    bos.write(buffer, 0, count)
                                }
                                bos.flush()
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun moveFileToCategory(
        sourceFile: DocumentFile,
        rootDir: DocumentFile,
        category: FileCategory
    ) {
        val destFolder = ZipFileUriHelper.getOrCreateSubFolder(rootDir, category.folderName)
        if (sourceFile.parentFile?.name == destFolder.name) return

        val targetFile = ZipFileUriHelper.getUniqueTargetFile(destFolder, sourceFile.name!!)

        context.contentResolver.openInputStream(sourceFile.uri)?.use { input ->
            context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
        sourceFile.delete()
    }

    private fun createNestedFolders(baseDir: DocumentFile, path: String): DocumentFile {
        var currentDir = baseDir
        val parts = path.split("/").filter { it.isNotEmpty() }
        for (part in parts) {
            currentDir = ZipFileUriHelper.getOrCreateSubFolder(currentDir, part)
        }
        return currentDir
    }

    private fun getTargetFolderForEntry(baseDir: DocumentFile, entryName: String): DocumentFile {
        if (!entryName.contains("/")) return baseDir
        val folderPath = entryName.substringBeforeLast('/')
        return createNestedFolders(baseDir, folderPath)
    }
}
