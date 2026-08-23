package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

// (v8.25.4) GANTI TEKNIK TOTAL ke-4 kalinya -- user kirim palet+spec CSS
// literal (base #181a20, convex/concave dual box-shadow persis, accent
// neon) hasil ekstraksi dari referensi asli mereka (image 3, dashboard
// neumorphism genuine dgn dial+card yg jelas cekung/timbul lewat SOFT
// SHADOW dual-arah, BUKAN garis bingkai). Root cause kenapa v8.25.3
// (bevel border 2dp) masih "belum sempurna": bingkai garis TEGAS itu
// SECARA TEKNIK beda dari soft-shadow asli -- neumorphism genuine tidak
// pernah pakai garis tepi solid, tapi 2 shadow blur (terang+gelap) yang
// MELEBUR ke background di sekitarnya. Fix kali ini: implementasi
// shadow ganda SUNGGUHAN (bukan border, bukan fill wash, bukan glow
// blob -- 3 pendekatan sebelumnya yg sudah terbukti gagal), pakai teknik
// standar Android `Paint.setShadowLayer` dual-offset dual-warna, PERSIS
// terjemahan `box-shadow` CSS yg dikasih user.
object NeumorphTokens {

    /** Base body/card -- dasar SEMUA permukaan Neumorphism sebelum
     * gradient convex/concave di atasnya (dipakai [ThemeStyleOption]
     * switcher utk `surface`/`background` M3 saat gaya ini aktif --
     * lihat pemanggil di `Color.kt`/`Theme.kt`, TIDAK diubah di file ini,
     * murni token nilai). */
    val BaseColor: Color = Color(0xFF181A20)

    /** Convex/normal state (tombol/kartu timbul) -- gradient 145deg
     * didekati Compose via 2 titik diagonal (lihat [convexBrush]),
     * shadow terang kiri-atas + gelap kanan-bawah via [convexShadow]. */
    val ConvexGradientStart: Color = Color(0xFF1A1C22)
    val ConvexGradientEnd: Color = Color(0xFF16171D)
    val ConvexShadowDark: Color = Color(0xFF121318)
    val ConvexShadowLight: Color = Color(0xFF1E2128)
    val ConvexShadowOffset: Dp = 10.dp
    val ConvexShadowBlur: Dp = 24.dp
    val ConvexCornerRadius: Dp = 20.dp

    /** Concave/inset state (area cekung, mis. track switch OFF, pill
     * grabber sheet) -- gradient dibalik arah dari convex, shadow versi
     * INSET (gelap kiri-atas & terang kanan-bawah, DI DALAM tepi, lewat
     * [concaveShadow] -- teknik clip+stroke, lihat komentar fungsinya). */
    val ConcaveGradientStart: Color = Color(0xFF15161B)
    val ConcaveGradientEnd: Color = Color(0xFF1A1C22)
    val ConcaveShadowDark: Color = Color(0xFF111216)
    val ConcaveShadowLight: Color = Color(0xFF1F222A)
    val ConcaveShadowOffset: Dp = 8.dp
    val ConcaveShadowBlur: Dp = 16.dp

    /** Active neon accent (progress arc/highlight ikon) -- gradient
     * 135deg 3-stop + glow shadow warna sama alpha 0.4. TOKEN INI
     * DISEDIAKAN PERSIS SESUAI SPEC USER, TAPI SENGAJA BELUM DIPASANG ke
     * elemen mana pun di batch ini -- brand accent app ini biru
     * periwinkle (`pv_primary_accent`) dipakai KONSISTEN lintas 3 gaya
     * tema & puluhan file; menimpanya jadi gradient pink-oranye-emas di
     * sini akan jadi REBRAND GLOBAL tak diminta, di luar scope "benerin
     * neumorphism kartu yg belum sempurna". Menunggu konfirmasi elemen
     * spesifik mana yg dimaksud "accent aktif" sebelum di-wire ke call
     * site manapun. */
    val AccentGradientColors: List<Color> = listOf(
        Color(0xFFFF416C),
        Color(0xFFFF4B2B),
        Color(0xFFFBD786)
    )
    val AccentShadowColor: Color = Color(0xFFFF416C).copy(alpha = 0.4f)
    val AccentShadowOffsetY: Dp = 4.dp
    val AccentShadowBlur: Dp = 15.dp

    /** Brush diagonal ~135-145deg didekati via 2 titik sudut (Compose
     * `Brush.linearGradient` tidak punya parameter derajat langsung --
     * start=kiri-atas, end=kanan-bawah otomatis skala ke bounds elemen
     * lewat overload 2-warna default, cukup dekat scara visual dgn
     * 145deg CSS, DILARANG overthinking hitung trigonometri sudut
     * presisi utk beda ~10deg yg nyaris tak kasat mata). */
    fun convexBrush(): Brush = Brush.linearGradient(listOf(ConvexGradientStart, ConvexGradientEnd))
    fun concaveBrush(): Brush = Brush.linearGradient(listOf(ConcaveGradientStart, ConcaveGradientEnd))
    fun accentBrush(): Brush = Brush.linearGradient(AccentGradientColors)

    /** Shadow GANDA convex (di LUAR bentuk, meniru CSS
     * `box-shadow: dx dy blur dark, -dx -dy blur light`) -- gambar 2
     * lapis shadow TRANSPARAN (fill tembus pandang, cuma shadow layer-nya
     * yg tampak) di belakang bentuk asli; bentuk asli (gradient fill)
     * digambar TERPISAH & DI ATAS lewat `Modifier.background(brush,
     * shape)` di `TactileSurface.kt`, jadi cuma bagian shadow yg
     * "mengintip" di tepi luar yg kelihatan -- PERSIS efek CSS box-shadow
     * ganda. `Paint.setShadowLayer` utk shape (bukan teks) baru didukung
     * penuh hardware-accelerated canvas sejak API 28 (project ini target
     * modern API 31+ per aturan arsitektur) -- di API di bawah itu shadow
     * tidak tampak (fallback flat gradient polos, TIDAK crash). */
    fun Modifier.convexShadow(shape: Shape): Modifier = drawBehind {
        drawDualShadow(
            shape = shape,
            darkColor = ConvexShadowDark,
            lightColor = ConvexShadowLight,
            offset = ConvexShadowOffset,
            blur = ConvexShadowBlur,
            inset = false
        )
    }

    /** Shadow GANDA concave (DI DALAM bentuk, kesan "ditekan") -- teknik
     * clip-ke-shape lalu gambar stroke tebal di TEPI DALAM dgn shadow
     * layer, cuma separuh dalam garisnya yg kelihatan (separuh luar
     * kepotong clip) -- trik umum "faux inner shadow" Android Canvas,
     * dipakai krn platform TIDAK punya inset-shadow bawaan spt CSS. */
    fun Modifier.concaveShadow(shape: Shape): Modifier = drawBehind {
        drawDualShadow(
            shape = shape,
            darkColor = ConcaveShadowDark,
            lightColor = ConcaveShadowLight,
            offset = ConcaveShadowOffset,
            blur = ConcaveShadowBlur,
            inset = true
        )
    }

    private fun DrawScope.drawDualShadow(
        shape: Shape,
        darkColor: Color,
        lightColor: Color,
        offset: Dp,
        blur: Dp,
        inset: Boolean
    ) {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = androidx.compose.ui.graphics.Path().apply {
            when (outline) {
                is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
                is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
                is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
            }
        }
        val androidPath = path.asAndroidPath()
        val offsetPx = offset.toPx()
        val blurPx = blur.toPx()

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint()
            paint.isAntiAlias = true
            paint.color = android.graphics.Color.TRANSPARENT

            if (inset) {
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = blurPx * 2f
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.clipPath(androidPath)
                paint.setShadowLayer(blurPx, offsetPx, offsetPx, darkColor.toArgb())
                canvas.nativeCanvas.drawPath(androidPath, paint)
                paint.setShadowLayer(blurPx, -offsetPx, -offsetPx, lightColor.toArgb())
                canvas.nativeCanvas.drawPath(androidPath, paint)
                canvas.nativeCanvas.restore()
            } else {
                paint.style = android.graphics.Paint.Style.FILL
                paint.setShadowLayer(blurPx, offsetPx, offsetPx, darkColor.toArgb())
                canvas.nativeCanvas.drawPath(androidPath, paint)
                paint.setShadowLayer(blurPx, -offsetPx, -offsetPx, lightColor.toArgb())
                canvas.nativeCanvas.drawPath(androidPath, paint)
            }
        }
    }
}
