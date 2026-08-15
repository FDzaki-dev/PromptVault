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
 * PENTING (mencegah regresi kelas Insiden #3 "VaultCard wrap-content" DAN
 * Insiden #8 "SegmentedControl weight() nyasar"): `modifier` PARAMETER
 * (dari pemanggil -- `fillMaxWidth()`/`size(...)`/`weight(1f)`/dst) dipasang
 * di `Box` TERLUAR ini sendiri (root komposabel, sesuai konvensi resmi
 * Compose), BUKAN di `Surface` konten di dalamnya -- WAJIB begitu supaya
 * `ParentDataModifier` seperti `RowScope.weight()` terbaca benar oleh
 * Row/Column pemanggil (lihat Insiden #8, PROJECT_STATE.md). Kedua Box
 * shadow-caster memakai `Modifier.matchParentSize()` (BoxScope), BUKAN
 * `Modifier.fillMaxSize()`. `Surface` konten TIDAK matchParentSize (beda
 * dari shadow-caster) -- ia anak Box "biasa" yang mewarisi constraints yang
 * sama dari `Box` (lebar ikut terkunci kalau `modifier` mengunci lebar,
 * tinggi tetap longgar kalau tidak dikunci) sehingga `Box` TETAP bisa
 * wrap-content TINGGI mengikuti konten `Surface` asli persis seperti
 * sebelumnya (mis. `VaultCard`/CTA Home yang cuma `fillMaxWidth()` tanpa
 * tinggi eksplisit). Urutan deklarasi (caster dulu, baru Surface) HANYA
 * mempengaruhi z-order gambar (caster di belakang), bukan pengukuran ukuran.
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
    // [Fix Insiden #8, v2.24.2] SEBELUMNYA `modifier` pemanggil (mis.
    // `fillMaxWidth()`/`size(...)`/`weight(1f)`) dipasang di `Surface` KONTEN
    // di dalam, BUKAN di `Box` pembungkus terluar ini -- niatnya supaya `Box`
    // cuma wrap ukuran akhir `Surface`. Itu BEKERJA untuk modifier ukuran
    // biasa (size/fillMaxWidth/padding/offset/scale), TAPI SALAH FATAL untuk
    // `ParentDataModifier` seperti `RowScope.weight()`/`ColumnScope.weight()`
    // -- parent data itu HANYA dibaca oleh Row/Column dari modifier chain
    // milik ANAK LANGSUNG-nya. Karena anak langsung Row/Column di sini
    // adalah `Box` INI (bukan `Surface` yang beberapa lapis di dalamnya),
    // `weight()` yang nyasar ke `Surface` tidak pernah terbaca -- child
    // dianggap TIDAK punya weight sama sekali. Gejala nyata: `SegmentedControl`
    // segment TERPILIH (dibungkus komponen ini) melebar tak terkendali +
    // segment lain hilang/kolaps (lihat PROJECT_STATE.md Insiden #8).
    // **Fix**: `modifier` pemanggil sekarang dipasang LANGSUNG di `Box`
    // terluar ini (elemen yang BENAR-BENAR jadi anak Row/Column pemanggil,
    // sesuai konvensi resmi Compose "selalu pasang parameter modifier di
    // root komposabel"). Shadow-caster & `Surface` konten di bawah sekarang
    // `matchParentSize()` mengikuti `Box` ini -- hasil visual utk SEMUA
    // pemanggil modifier ukuran biasa (VaultCard, GroupedListRow, dst)
    // IDENTIK seperti sebelumnya (diverifikasi manual tiap call site di
    // CHANGELOG v2.24.2), cuma `weight()` yang sekarang benar-benar berfungsi.
    Box(modifier = modifier) {
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

        // `Surface` konten TIDAK lagi membawa `modifier` pemanggil sendiri
        // (sudah pindah ke `Box` terluar, lihat catatan di atas) -- TAPI
        // SENGAJA JUGA TIDAK `matchParentSize()` (beda dari shadow-caster).
        // `Surface` di sini tetap anak Box "biasa" (non-matchParentSize)
        // supaya kalau `modifier` pemanggil cuma mengunci LEBAR (mis.
        // `fillMaxWidth()`/`weight(1f)`, tinggi longgar), `Box` tetap bisa
        // wrap-content TINGGI mengikuti konten `Surface` asli -- persis
        // perilaku lama (`VaultCard`/CTA Home yang cuma `fillMaxWidth()`
        // TANPA tinggi eksplisit TETAP wrap-content). `Box` meneruskan
        // constraints yang sama ke `Surface` non-matchParentSize ini
        // (lebar terkunci ikut constraint `Box`, tinggi tetap longgar) --
        // hasil akhir identik dgn sebelumnya utk kasus ukuran biasa, TAPI
        // sekarang `weight()`/ParentData lain terbaca benar oleh Row/Column
        // karena sudah nempel di `Box`, bukan lagi nyasar ke sini.
        val contentModifier = Modifier.then(if (pressed) pressedScrim else Modifier)
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
