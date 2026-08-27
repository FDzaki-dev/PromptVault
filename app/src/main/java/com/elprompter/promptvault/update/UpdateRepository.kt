package com.elprompter.promptvault.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * [Fitur baru 2026-08-19, Release Downloader Spec] Cek versi terbaru & unduh
 * APK dari GitHub Releases, sesuai spesifikasi wajib proyek:
 * - Streaming chunk-by-chunk langsung ke disk lewat Okio sink -- TIDAK
 *   PERNAH memuat biner APK penuh ke RAM (anti-freeze/anti-OOM utk APK
 *   berukuran puluhan MB).
 * - Timeout eksplisit: connect 15s, read 20s (bukan default OkHttp yang
 *   tanpa batas utk read -- koneksi lemot tidak akan menggantung selamanya).
 * - `followRedirects(true)` -- asset binary GitHub Release SELALU di-serve
 *   lewat redirect HTTP 302 ke S3/CDN (`objects.githubusercontent.com`),
 *   bukan langsung dari `api.github.com`/`github.com`.
 * - Header `Authorization: Bearer <token>` (opsional -- repo ini publik,
 *   jadi TIDAK wajib diisi; disediakan supaya user bisa naikkan rate-limit
 *   GitHub API 60/jam->5000/jam pakai PAT sendiri lewat Pengaturan, atau
 *   kalau suatu saat repo dipindah ke private) + `Accept:
 *   application/octet-stream` khusus saat request biner asset.
 */
class UpdateRepository(private val context: Context) {

    companion object {
        // Repo publik resmi, lihat README.md ("Repo publik: github.com/FDzaki-dev/PromptVault").
        private const val OWNER = "FDzaki-dev"
        private const val REPO = "PromptVault"
        private const val API_LATEST_RELEASE = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

        // 8 KB per chunk -- ukuran umum utk streaming I/O, cukup kecil utk
        // progress callback terasa halus, cukup besar utk tidak overhead syscall.
        private const val CHUNK_SIZE = 8L * 1024L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * APK dicari sbg asset PERTAMA yang nama filenya berakhiran ".apk" --
     * pola nama dari CI (`PromptVault-v<versi>.apk`, lihat
     * .github/workflows/build.yml step "Rename APK with version name")
     * SENGAJA TIDAK di-hardcode persis di sini, biar tidak dobel-maintain
     * kalau pola penamaan CI berubah suatu saat.
     */
    suspend fun checkLatestRelease(currentVersionName: String, githubToken: String? = null): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url(API_LATEST_RELEASE)
                    .header("Accept", "application/vnd.github+json")
                if (!githubToken.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $githubToken")
                }
                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext UpdateCheckResult.Error(
                            "GitHub API gagal (HTTP ${response.code}). Coba lagi nanti."
                        )
                    }
                    // Metadata rilis JSON selalu kecil (KB, bukan MB) -- .string()
                    // di sini AMAN, beda kasus dgn biner APK di downloadApk() di
                    // bawah yang WAJIB streaming.
                    val bodyText = response.body?.string()
                        ?: return@withContext UpdateCheckResult.Error("Respons GitHub kosong.")
                    val release = json.decodeFromString(GithubReleaseDto.serializer(), bodyText)
                    val latestVersion = release.tagName.removePrefix("v")

                    if (!isNewerVersion(latestVersion, currentVersionName)) {
                        return@withContext UpdateCheckResult.UpToDate(currentVersionName)
                    }

                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                        ?: return@withContext UpdateCheckResult.NoApkAsset(latestVersion, release.htmlUrl)

                    UpdateCheckResult.UpdateAvailable(
                        currentVersion = currentVersionName,
                        latestVersion = latestVersion,
                        releaseNotes = release.body.orEmpty(),
                        releaseUrl = release.htmlUrl,
                        asset = apkAsset
                    )
                }
            } catch (e: IOException) {
                UpdateCheckResult.Error("Tidak bisa terhubung ke GitHub: ${e.message}")
            } catch (e: Exception) {
                UpdateCheckResult.Error("Gagal memproses respons GitHub: ${e.message}")
            }
        }

    /**
     * Download APK streaming chunk-by-chunk ke `cacheDir/updates/` (isi lama
     * dibersihkan dulu -- hanya 1 APK update tersimpan sekaligus, bukan
     * menumpuk tiap kali user cek pembaruan berkali-kali). File sementara
     * `.part` dipakai SELAMA proses download, baru di-rename ke nama final
     * SETELAH sukses penuh -- kalau koneksi/proses mati di tengah jalan,
     * sisa `.part` yang tertinggal TIDAK PERNAH dianggap APK valid siap
     * instal.
     */
    suspend fun downloadApk(
        asset: GithubAssetDto,
        githubToken: String? = null,
        onProgress: (DownloadState.Downloading) -> Unit
    ): DownloadState = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { it.delete() }

        val destFile = File(updatesDir, asset.name)
        val tmpFile = File(updatesDir, "${asset.name}.part")

        try {
            val requestBuilder = Request.Builder()
                .url(asset.browserDownloadUrl)
                .header("Accept", "application/octet-stream")
            if (!githubToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $githubToken")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadState.Failed("Download gagal (HTTP ${response.code}).")
                }
                val body = response.body
                    ?: return@withContext DownloadState.Failed("Body kosong dari server.")
                val totalBytes = body.contentLength()
                var bytesRead = 0L

                // Streaming manual chunk-by-chunk lewat BufferedSource (Okio,
                // dipakai internal oleh OkHttp) -> BufferedSink ke file .part.
                // TIDAK PERNAH memanggil body.bytes()/readBytes() -- itu akan
                // memuat SELURUH APK ke RAM sekaligus, persis yang dilarang
                // Release Downloader Spec.
                body.source().use { source ->
                    tmpFile.sink().buffer().use { sink ->
                        val buffer = Buffer()
                        while (true) {
                            val read = source.read(buffer, CHUNK_SIZE)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            bytesRead += read
                            onProgress(DownloadState.Downloading(bytesRead, totalBytes))
                        }
                        sink.flush()
                    }
                }
            }

            if (!tmpFile.exists() || tmpFile.length() == 0L) {
                tmpFile.delete()
                return@withContext DownloadState.Failed("File hasil download kosong/rusak.")
            }
            if (!tmpFile.renameTo(destFile)) {
                tmpFile.delete()
                return@withContext DownloadState.Failed("Gagal finalisasi file APK.")
            }
            DownloadState.Completed(destFile.absolutePath)
        } catch (e: IOException) {
            tmpFile.delete()
            DownloadState.Failed("Koneksi terputus: ${e.message}")
        } catch (e: Exception) {
            tmpFile.delete()
            DownloadState.Failed("Gagal download: ${e.message}")
        }
    }

    /**
     * [fix 2026-08-27] Root cause "app usang kira dirinya sudah versi
     * terbaru": versi lama pakai perbandingan TUPLE POSISIONAL penuh
     * (major dulu, baru minor, baru patch -- cocok utk skema semantic lama
     * "8.x.y"). Skema versionName sekarang PERMANEN "1.0.<run_number>"
     * (rule pinned PROJECT_STATE.md, major di-reset ke 1) -- app lama yang
     * masih pegang versionName skema lama (mis. "8.35.0") dibandingkan
     * lawan rilis baru (mis. "1.0.209") kalah di posisi PERTAMA (1 < 8) ->
     * selamanya dianggap "sudah terbaru", update TIDAK PERNAH ditawarkan,
     * padahal app itu justru paling usang.
     * Fix: bandingkan SEGMEN TERAKHIR saja (run number -- satu-satunya
     * angka yang pernah berubah di skema baru, dan dijamin PINNED naik
     * terus), bukan seluruh tuple posisional. Kebal thd skema versi lama
     * MAUPUN perubahan skema apa pun di masa depan.
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteBuild = remote.split(".", "-").mapNotNull { it.toIntOrNull() }.lastOrNull()
        val localBuild = local.split(".", "-").mapNotNull { it.toIntOrNull() }.lastOrNull()
        if (remoteBuild == null || localBuild == null) return remote != local && remote > local
        return remoteBuild > localBuild
    }
}
