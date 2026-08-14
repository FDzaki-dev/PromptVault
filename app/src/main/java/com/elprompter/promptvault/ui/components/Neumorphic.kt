package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.AmoledBackground
import com.elprompter.promptvault.ui.theme.NeuHighlight
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * v5.0.0 — Primitif tunggal untuk seluruh redesign Glassmorphism ->
 * Neumorphism (bab 12: satu titik implementasi, bukan duplikasi teknik di
 * tiap komponen). Menggantikan pola lama VaultCard/GroupedListRow/dst
 * (Surface solid + Box overlay Brush gradient tipis + border rambut).
 *
 * ## Cara kerja (RAISED / permukaan timbul, `pressed = false`)
 * Neumorphism butuh SEPASANG shadow terarah: terang di kiri-atas (seolah
 * permukaan menangkap cahaya) + gelap di kanan-bawah (seolah permukaan
 * mendorong bayangan menjauh dari sumber cahaya). Compose (`Modifier.shadow`)
 * tidak punya parameter offset arah bawaan -- shadow selalu digambar
 * mengelilingi bentuk aslinya. Trik yang dipakai di sini (teknik neumorphism
 * standar utk Compose, BUKAN modifikasi API): 2 `Box` TAMBAHAN yang bentuk &
 * ukurannya PERSIS SAMA dengan permukaan asli, masing-masing DIGESER sedikit
 * (`Modifier.offset`) menjauh dari posisi asli lalu diberi `Modifier.shadow`.
 * Karena isi ("fill") kedua Box itu SOLID [baseColor] (default: warna
 * background di belakang komponen, misal [AmoledBackground]) -- badan Box
 * itu sendiri MENYATU/tak terlihat dengan latar di sekitarnya, yang terlihat
 * HANYA bayangannya yang tumpah keluar ke arah pergeserannya. Permukaan asli
 * (Surface solid, TANPA shadow sendiri) digambar PALING ATAS menutupi badan
 * kedua Box itu -- hasil akhir: satu permukaan solid dengan shadow gelap
 * mengintip di kanan-bawah + shadow terang mengintip di kiri-atas, PERSIS
 * ilusi neumorphism ("soft UI").
 *
 * Ini KONSISTEN dengan pola aman yang sudah terbukti di codebase ini sejak
 * v4.0.0 (lihat riwayat regresi CTA Home v2.14.0 di PROJECT_STATE.md):
 * shadow SELALU jatuh di node ber-`background` SOLID, TIDAK PERNAH dirantai
 * langsung ke node ber-`Brush` gradient di modifier chain yang sama.
 *
 * PENTING (mencegah regresi kelas Insiden #3 "VaultCard wrap-content"):
 * kedua Box shadow-caster memakai `Modifier.matchParentSize()` (BoxScope),
 * BUKAN `Modifier.fillMaxSize()` -- dan keduanya dideklarasikan SEBELUM
 * `Surface` konten asli. `matchParentSize()` tidak ikut menentukan ukuran
 * `Box` induk (hanya elemen TANPA `matchParentSize` yang menentukan ukuran,
 * lihat dokumentasi resmi Compose `Box`) -- jadi `Surface` konten asli
 * (wrap-content mengikuti isi + modifier dari pemanggil) TETAP yang
 * menentukan ukuran akhir, kedua shadow-caster otomatis mengikuti ukuran
 * itu. Urutan deklarasi (caster dulu, baru Surface) HANYA mempengaruhi
 * z-order gambar (caster di belakang), bukan pengukuran ukuran.
 *
 * ## Cara kerja (PRESSED / permukaan tenggelam, `pressed = true`)
 * Shadow-caster offset TIDAK dipakai (tidak ada "permukaan timbul" utk
 * elemen yang sedang ditekan/recessed). Sebagai gantinya, overlay gradien
 * diagonal tipis (gelap di kiri-atas -> terang di kanan-bawah -- ARAH
 * TERBALIK dari highlight normal, mensimulasikan cahaya yang justru
 * terhalang di sisi awal cekungan & memantul tipis di dasar cekungan)
 * ditumpuk DI ATAS `Surface` solid yang sama, alpha rendah
 * ([TactileTokens.NeuPressedDarkAlpha]/[TactileTokens.NeuPressedLightAlpha]).
 *
 * @param onClick jika non-null, permukaan jadi clickable (pakai overload
 *   `Surface(onClick=...)` M3 -- ripple & state layer bawaan tetap jalan).
 * @param baseColor warna badan shadow-caster -- WAJIB sama/dekat dengan
 *   warna LATAR di belakang komponen ini (bukan warna komponen itu sendiri)
 *   supaya badan caster menyatu & hanya shadow-nya yang terlihat. Default
 *   [AmoledBackground] karena mayoritas permukaan di app ini duduk langsung
 *   di atas latar AMOLED root.
 * @param interactionSource SENGAJA non-null dengan default `remember{...}`
 *   (bukan nullable) -- overload `Surface(onClick=...)` M3 di compose-bom
 *   2024.06.00 (versi project ini, lihat `app/build.gradle.kts`) menerima
 *   `MutableInteractionSource` non-null; default non-null di sini kompatibel
 *   baik untuk versi yang menerima non-null MAUPUN yang menerima nullable
 *   (nilai non-null selalu valid utk parameter nullable), jadi TIDAK
 *   bergantung asumsi versi API yang tidak bisa diverifikasi tanpa compiler
 *   asli di sandbox ini.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface,
    baseColor: Color = AmoledBackground,
    elevation: Dp = TactileTokens.NeuElevationCard,
    shadowOffset: Dp = TactileTokens.NeuOffsetCard,
    pressed: Boolean = false,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    // Sengaja TANPA receiver scope (bukan `@Composable BoxScope.() -> Unit`)
    // -- identik tipe parameter `content` bawaan `Surface` M3 (diteruskan
    // langsung ke situ di bawah). Kalau butuh alignment (mis. Icon di tengah
    // kotak kecil GroupedListRow), pemanggil bikin `Box(contentAlignment=...)`
    // SENDIRI di dalam lambda ini -- constraint tetap terpropagasi benar dari
    // `Surface` (`propagateMinConstraints = true` bawaan M3), TANPA perlu
    // `Modifier.fillMaxSize()` di dalam sini (itu yang menyebabkan Insiden #3
    // "VaultCard ketumpuk" dulu -- lihat PROJECT_STATE.md -- kalau modifier
    // dari pemanggil wrap-content, bukan ukuran tetap).
    content: @Composable () -> Unit
) {
    // PENTING: `modifier` (mis. `fillMaxWidth()`/`size(...)` dari pemanggil)
    // dipasang di `Surface` KONTEN ASLI di bawah, BUKAN di `Box` pembungkus
    // ini -- `Box` sengaja TANPA modifier ukuran sendiri supaya cuma wrap
    // apa pun ukuran akhir `Surface` (persis pola lama VaultCard: modifier
    // pemanggil selalu jatuh di elemen yang benar-benar menentukan ukuran).
    // Shadow-caster `matchParentSize()` di bawah otomatis ikut ukuran itu.
    Box {
        if (!pressed) {
            // Shadow gelap (kanan-bawah) -- pakai warna bayangan default
            // Compose (hitam, dimodulasi elevasi), identik mekanisme yang
            // sudah dipakai aman di seluruh app sejak v4.0.0.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .shadow(elevation = elevation, shape = shape, clip = false)
                    .background(baseColor, shape)
            )
            // Shadow terang (kiri-atas) -- spotColor/ambientColor custom
            // [NeuHighlight], parameter yang sudah tersedia di Modifier.shadow
            // sejak awal (bukan API baru), cuma belum pernah dipakai di app
            // ini krn sistem glassmorphism lama tidak butuh shadow berwarna.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = -shadowOffset, y = -shadowOffset)
                    .shadow(
                        elevation = elevation,
                        shape = shape,
                        clip = false,
                        ambientColor = NeuHighlight,
                        spotColor = NeuHighlight
                    )
                    .background(baseColor, shape)
            )
        }

        val pressedScrim = Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = TactileTokens.NeuPressedDarkAlpha),
                    Color.Transparent,
                    Color.White.copy(alpha = TactileTokens.NeuPressedLightAlpha)
                )
            ),
            shape = shape
        )

        // `modifier` pemanggil dipasang DI SINI (bukan di Box induk, lihat
        // catatan di atas) supaya perilaku ukuran identik dgn `Surface`
        // polos yang digantikan (fillMaxWidth/size dari pemanggil langsung
        // menentukan ukuran elemen konten asli, wrap-content kalau kosong).
        val contentModifier = modifier.then(if (pressed) pressedScrim else Modifier)
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = contentModifier,
                shape = shape,
                color = color,
                border = border,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                interactionSource = interactionSource,
                content = content
            )
        } else {
            Surface(
                modifier = contentModifier,
                shape = shape,
                color = color,
                border = border,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                content = content
            )
        }
    }
}
