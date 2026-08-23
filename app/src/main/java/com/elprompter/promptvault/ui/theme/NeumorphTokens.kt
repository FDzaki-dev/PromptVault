package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v8.23.2 — Neumorphism, gaya KEDUA di toggle "Tampilan" (v8.23.0
 * scaffold, sekarang live). Teknik klasik "soft UI": fill OPAQUE (bukan
 * translucent spt Glassmorphism di [GlassTokens]) sewarna surface aslinya,
 * kedalaman murni dari SEPASANG shadow terarah -- terang di
 * kiri-atas (seolah cahaya datang dari sana), gelap di kanan-bawah.
 *
 * 3 syarat user (sama seperti [GlassTokens], ditelusuri utk gaya ini juga):
 * 1. **"murni, tanpa keluar dari gaya desain itu sendiri"** -- HANYA
 *    shadow ganda + fill opaque yang jadi treatment visual, TIDAK ada
 *    border/translucency/sheen (itu ciri Glassmorphism, BUKAN
 *    Neumorphism -- mencampur keduanya justru keluar dari "murni").
 * 2. **"calm, bukan warm"** -- shadow terang & gelap SAMA-SAMA netral
 *    (hitam/putih alpha rendah, bukan hue kustom), warna fill dasar
 *    tetap dari [Color.kt] (H222 biru, TIDAK disentuh). Tidak ada
 *    penambahan hue baru sama sekali di file ini.
 * 3. **"100% WCAG"** -- fill 100% OPAQUE (alpha 1.0, BEDA dari
 *    Glassmorphism yang translucent) -- kontras teks di atasnya PERSIS
 *    angka worst-case yang SUDAH diverifikasi manual di [Color.kt]
 *    (11.64:1/7.54:1 dst), 0 ketidakpastian tambahan dari blending.
 *    Shadow ganda 100% dekoratif (di LUAR shape konten, bukan di atas
 *    teks) -- tidak relevan dinilai WCAG teks/1.4.11 sama sekali.
 */
object NeumorphTokens {
    /** Jarak offset shadow dari tepi permukaan (kedua arah, simetris). */
    val ShadowOffset: Dp = 7.dp

    /** Radius blur shadow -- lebih besar dari offset supaya terasa "lembut" (ciri khas neumorphism, bukan shadow tegas M3 biasa). */
    val ShadowBlurRadius: Dp = 18.dp

    // [fix bug nyata, 2026-08-23 -- laporan user "Neumorphism sama Material3
    // gak ada bedanya sama sekali"] ROOT CAUSE: shadow SECARA TEKNIS memang
    // render (kode sudah benar, dua Box beroffset + Modifier.shadow), TAPI
    // alpha lama (putih 0.06f / hitam 0.45f) jauh terlalu tipis utk terlihat
    // -- yang kelihatan cuma "bleed" tipis di LUAR bentuk konten (fill di
    // atasnya menutupi sisanya, lihat TactileSurface.kt), jadi di alpha
    // serendah itu efeknya nyaris 0% kebaca di layar HP nyata, apalagi warna
    // terang di atas latar app yang sudah gelap total. Dinaikkan jauh lebih
    // kuat di sini -- BUKAN restrukturisasi teknik (offset+shadow tetap sama
    // persis), murni kontras dinaikkan supaya "efek timbul" benar2 terlihat.

    /** Shadow terang (kiri-atas) -- putih alpha rendah, netral, bukan warm. */
    val LightShadowColor: Color = Color.White.copy(alpha = 0.35f)

    /** Shadow gelap (kanan-bawah) -- hitam alpha lebih tinggi (latar app sudah gelap, perlu sedikit lebih kuat drpd shadow terang supaya kedua arah sama-sama kebaca). */
    val DarkShadowColor: Color = Color.Black.copy(alpha = 0.70f)

    // [v8.24.0 -- permintaan eksplisit user: "BUKAN bagian shadow/opaque
    // yang diutak-atik" -- `ShadowOffset`/`ShadowBlurRadius`/
    // `LightShadowColor`/`DarkShadowColor` di ATAS ini 100% TIDAK DISENTUH,
    // 0 baris diubah. Tambahan MURNI di bawah, token BARU sama sekali.]
    //
    // "Background kartu dibikin berlapis 3 supaya muncul desain 3D khas
    // Neumorphism asli" -- fill Surface SEBELUMNYA cuma 1 lapis (solid flat
    // `color` polos, kedalaman 3D 100% dari shadow DI LUAR bentuk saja).
    // Neumorphism otentik biasanya PERMUKAAN itu sendiri juga "puffy"/
    // cembung (bukan cuma shadow di tepi) -- dicapai dengan gradient
    // dekoratif tipis di ATAS fill dasar: terang di sudut kiri-atas (kesan
    // cahaya menimpa permukaan cembung), gelap di sudut kanan-bawah (kesan
    // permukaan menjauhi cahaya). Jadi 3 lapis total per kartu:
    //   Lapis 1: fill solid dasar (punya `TactileSurface`, TIDAK berubah)
    //   Lapis 2: gradient terang, sudut kiri-atas (BARU, token di bawah)
    //   Lapis 3: gradient gelap, sudut kanan-bawah (BARU, token di bawah)
    // 3 syarat user tetap ditelusuri persis sama seperti javadoc atas:
    // 1. Murni teknik Neumorphism (gradient dekoratif DI DALAM fill,
    //    bukan border/translucency ala Glassmorphism -- fill TETAP 100%
    //    OPAQUE, cuma di-layer 2 brush dekoratif tambahan di atasnya, alpha
    //    brush itu sendiri BUKAN alpha fill -- fill dasar tidak jadi
    //    translucent sama sekali).
    // 2. Netral (White/Black alpha rendah), 0 hue baru.
    // 3. WCAG -- worst-case teks (`TextSecondary`, kontras terkecil di
    //    Color.kt: 7.54:1 vs SurfaceContainerHighest) DIHITUNG ULANG saat
    //    diblend dgn TITIK PUNCAK gradient terang (bukan rata-rata -- harus
    //    titik paling terang, sama metodologi seluruh Color.kt/GlassTokens):
    //      composited = white*0.14 + SurfaceContainerHighest*0.86 = (74,78,85)
    //      TextPrimary vs composited: 7.47:1 (masih AAA)
    //      TextSecondary vs composited: 4.84:1 (masih AA, ambang 4.5:1,
    //      margin sengaja disisakan -- 0.16 sudah pas di 4.53:1, TERLALU
    //      mepet, makanya dipilih 0.14 bukan 0.16). Dihitung skrip Python
    //      formula W3C persis, bukan tafsiran. Sisi gelap (Black alpha)
    //      TIDAK py=unya batas atas WCAG -- menggelapkan background CUMA
    //      MENAIKKAN kontras teks terang di atasnya, tidak pernah
    //      menurunkan -- makanya alpha sisi gelap boleh lebih kuat dari
    //      sisi terang (konsisten dgn rasio LightShadowColor:DarkShadowColor
    //      = 0.35:0.70 di atas, pola sama persis dipertahankan di sini).

    /** Tint gradient terang di FILL (bukan shadow luar) -- sudut kiri-atas,
     * lapis ke-2 dari 3, lihat penjelasan lengkap di atas. */
    val SurfaceHighlightTint: Color = Color.White.copy(alpha = 0.14f)

    /** Tint gradient gelap di FILL (bukan shadow luar) -- sudut kanan-bawah,
     * lapis ke-3 dari 3, lihat penjelasan lengkap di atas. */
    val SurfaceShadeTint: Color = Color.Black.copy(alpha = 0.22f)

    /** Brush lapis ke-2 (terang, kiri-atas -> transparan). Tanpa `start`/
     * `end` eksplisit -- pola sama persis `GlassTokens.highlightBrush()`,
     * Compose otomatis pas ke ukuran gambar saat draw. */
    fun surfaceHighlightBrush(): Brush = Brush.linearGradient(
        colors = listOf(SurfaceHighlightTint, Color.Transparent)
    )

    /** Brush lapis ke-3 (transparan -> gelap, kanan-bawah). */
    fun surfaceShadeBrush(): Brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, SurfaceShadeTint)
    )
}
