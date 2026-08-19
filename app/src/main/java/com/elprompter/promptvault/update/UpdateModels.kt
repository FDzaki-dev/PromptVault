package com.elprompter.promptvault.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Fitur baru 2026-08-19, Release Downloader Spec] Model minimal hasil
 * GitHub Releases API (`GET /repos/{owner}/{repo}/releases/latest`). Hanya
 * field yang benar-benar dipakai app ini yang dideklarasikan -- field lain
 * di response asli (author, prerelease flag, dst) otomatis diabaikan lewat
 * `Json { ignoreUnknownKeys = true }` di [UpdateRepository], bukan gagal parse.
 */
@Serializable
data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("body") val body: String? = null,
    @SerialName("assets") val assets: List<GithubAssetDto> = emptyList()
)

@Serializable
data class GithubAssetDto(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("size") val size: Long = 0L
)

/** Hasil pengecekan versi, sudah dibandingkan dgn versionName terpasang saat ini. */
sealed class UpdateCheckResult {
    data object Checking : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val releaseUrl: String,
        val asset: GithubAssetDto
    ) : UpdateCheckResult()
    /** Rilis baru ada, tapi tidak ada asset .apk terlampir (mis. rilis draft/CI belum publish APK). */
    data class NoApkAsset(val latestVersion: String, val releaseUrl: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/** Progres download APK, dipancarkan tiap chunk (lihat [UpdateRepository.downloadApk]). */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : DownloadState() {
        /** -1 kalau server tidak kirim Content-Length (progres tak diketahui, tampilkan indeterminate). */
        val percent: Int get() = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else -1
    }
    data class Completed(val filePath: String) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
