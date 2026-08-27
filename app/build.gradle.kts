import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "com.elprompter.promptvault"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elprompter.promptvault"
        minSdk = 26
        targetSdk = 34
        versionCode = 197
        versionName = "8.35.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("ANDROID_KEYSTORE_PATH") ?: keystoreProps.getProperty("storeFile")
            if (ksPath != null && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // [ROLLBACK 2026-08-27] v8.34.0 mengaktifkan isMinifyEnabled/
            // isShrinkResources -- user laporkan REGRESI BESAR di APK
            // release setelahnya. Sandbox kerja Claude TIDAK punya akses
            // ./gradlew/device asli utk mendiagnosis root cause spesifik
            // dari laporan tanpa crash log/stack trace terlampir -- sesuai
            // STABILITY WINS (prioritas #1 di atas kecepatan/fitur), jalan
            // paling aman & terverifikasi 100% adalah balik ke state SEBELUM
            // v8.34.0 (false permanen sejak awal project, TERBUKTI stabil
            // sepanjang riwayat) -- BUKAN menebak tambahan keep rule lain
            // tanpa bukti konkret area mana yang gagal. Ini pola yang SAMA
            // dgn rollback-rollback lain di riwayat project ini (v8.22.15,
            // v8.22.21, v8.26.0) saat sebuah fitur terverifikasi regresi
            // nyata & tidak bisa diverifikasi ulang blind di sandbox ini.
            // `proguard-rules.pro` SENGAJA TIDAK dihapus/disentuh -- 3 blok
            // keep rule di dalamnya tetap tersimpan dorman, siap dipakai
            // lagi kalau fitur ini diaktifkan ulang nanti dgn crash log
            // asli utk diagnosis bertarget (bukan diulang blind spt sesi
            // v8.34.0).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // [Fitur baru 2026-08-17, integrasi Shizuku] Wajib true supaya
        // app/src/main/aidl/.../IFileOpsService.aidl digenerate jadi
        // Stub/Proxy Kotlin oleh AGP -- lihat shizuku/ package.
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // [v8.22.21 ROLLBACK] Robolectric DIHAPUS LAGI -- exit value 10 (OOM
    // silent-crash) TERULANG PERSIS sama walau 2 lapis mitigasi v8.22.16
    // (maxParallelForks=1 + maxHeapSize=2048m + testDebugUnitTest-only)
    // SUDAH aktif jalan (baru bisa dites nyata sekarang, DSL-nya sempat
    // salah syntax di v8.22.19->20). Sesuai kontingensi yang SUDAH ditulis
    // eksplisit di v8.22.16: "kalau MASIH merah dgn sinyal OOM yang sama
    // meski sudah 2 lapis mitigasi -> skip Robolectric permanen, terima
    // reboot-survival end-to-end sbg gap test terdokumentasi". Runner CI
    // (~7GB) terbukti tidak cukup utk Robolectric shadow-classload Android
    // SDK + Gradle/Kotlin daemon resident, TERLEPAS dari tuning heap/fork.
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // [SAF, syarat (c) Insiden #7] Ditambahkan kembali khusus untuk fitur
    // Folder Kustom (DocumentFile) -- dibuang total di v2.13.0 waktu SAF lama
    // dihapus, lihat CHANGELOG.md & PROJECT_STATE.md.
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Room: dipakai khusus untuk ActivityLog & MoveHistory (data yang bisa tumbuh
    // ribuan baris). Rules & Settings TETAP di DataStore -- keduanya kecil dan
    // tidak butuh query relasional.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // [Fitur baru 2026-08-19, Release Downloader Spec] In-app updater: cek
    // rilis terbaru via GitHub Releases API + download APK streaming
    // chunk-by-chunk ke disk (Okio sink, BUKAN readBytes() penuh ke RAM).
    // okio dideklarasikan eksplisit walau ikut transitif dari okhttp --
    // dipakai LANGSUNG (File.sink()/buffer()) di UpdateRepository.kt, bukan
    // cuma dependency transitif diam-diam.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")

    // [Fitur baru 2026-08-17, integrasi Shizuku -- permintaan eksplisit user]
    // "api" = kelas Shizuku/ShizukuBinderWrapper/UserServiceArgs dkk, dipakai
    // ShizukuManager.kt. "provider" = ShizukuProvider (dideklarasikan manual
    // di AndroidManifest.xml, authorities pakai applicationId) supaya app ini
    // BISA menerima binder dari Shizuku (baik dari Shizuku Manager app biasa
    // MAUPUN dari mode `adb shell` langsung). Versi 13.1.5 dipilih (rilis
    // stabil terakhir yang dikenal luas dipakai per akhir masa training) --
    // BELUM diverifikasi lewat Gradle asli (tidak ada akses network di sini).
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // [v8.22.21 ROLLBACK] testImplementation Robolectric/androidx.test/
    // work-testing DIHAPUS -- lihat catatan lengkap di `testOptions` block
    // atas (yang juga dihapus) & PROJECT_STATE.md v8.22.21.

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
