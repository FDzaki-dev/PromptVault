package com.elprompter.promptvault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * v8.0.0 — Skala BAKU Material 3 (spec resmi M3 Shape scale), menggantikan
 * skala kustom v-sebelumnya (8/12/16/20/28dp, "kesan lembut ala iOS")
 * yang eksplisit BUKAN M3 murni. Nilai di bawah PERSIS skala default M3
 * (extraSmall=4, small=8, medium=12, large=16, extraLarge=28) -- syarat
 * "default Material 3 murni".
 */
val PromptVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * (v8.31.2, "lanjut sempurnakan" gaya HYBRID) Skala sudut KHUSUS gaya
 * [com.elprompter.promptvault.data.ThemeStyleOption.HYBRID] -- radius
 * SENGAJA lebih besar dari [PromptVaultShapes] di semua tingkat (bukan
 * proporsi acak, tetap urutan naik extraSmall->extraLarge yang sama),
 * meniru kesan sudut "continuous corner"/lebih membulat khas Cupertino
 * TANPA custom `Shape` class rumit (Compose stok tidak native dukung
 * squircle iOS asli -- radius lebih besar via `RoundedCornerShape` biasa
 * sudah cukup memberi "rasa"-nya, sesuai skala usaha 1 batch).
 *
 * Dipasang di `PromptVaultTheme` (`Theme.kt`) -- GANTI `shapes` M3 secara
 * KONDISIONAL per `themeStyle`, BUKAN nilai statis baru di `TactileSurface`
 * per-cabang (lihat alasan di javadoc `Theme.kt`). Efeknya OTOMATIS
 * menjalar ke SEMUA caller yang pakai `MaterialTheme.shapes.*` -- termasuk
 * default parameter `TactileSurface.shape` sendiri (`= MaterialTheme.shapes.medium`),
 * jadi mayoritas kartu/kontrol di app (yang tidak eksplisit override shape)
 * otomatis ikut lebih membulat saat gaya HYBRID aktif, 0 file caller lain
 * perlu disentuh satu per satu.
 *
 * Caveat jujur: ~11 titik pakai `RoundedCornerShape(Xdp)` literal LANGSUNG
 * (bukan lewat `MaterialTheme.shapes.*`) di seluruh app TIDAK ikut berubah
 * batch ini -- di luar cakupan 1 perubahan tunggal di sini, perlu disentuh
 * manual kalau diminta lanjut lebih jauh.
 */
val HybridShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
