package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v8.23.2 — Neumorphism, gaya KEDUA di toggle "Tampilan". [REVERT DARURAT,
 * 2026-08-23] File ini SEBELUMNYA berisi teknik shadow-ganda "genuine"
 * (drawBehind + nativeCanvas + Paint.setShadowLayer + gradient brush
 * custom, v8.23.2 s/d v8.25.4, 4 percobaan teknik berbeda) -- SEMUA
 * percobaan itu DIHAPUS TOTAL setelah laporan user (screenshot): teknik
 * terakhir (v8.25.4) menyebabkan SELURUH UI washed-out/nyaris tak
 * terlihat di device nyata (bukan cuma efeknya lemah kayak v8.23.6,
 * kali ini benar-benar merusak kontras di semua permukaan).
 *
 * Root cause pasti dari kerusakan v8.25.4 TIDAK ditelusuri lebih lanjut
 * (di luar scope revert darurat -- prioritas STABIL dulu). Kalau efek
 * "timbul" custom mau dicoba lagi di masa depan, JANGAN reuse kode lama
 * dari riwayat git/zip sebelum v8.26.0 begitu saja -- verifikasi visual
 * nyata (emulator/device) SEBELUM dikirim, bukan blind lagi.
 *
 * Sekarang gaya ini HANYA dibedakan dari Material3 Murni lewat
 * [BorderColor]/[BorderWidth] (border solid lebih tebal) di
 * `TactileSurface.kt` -- rendering-nya sendiri `Surface` M3 baku, PERSIS
 * pola aman yang sudah dipakai cabang Material3 (0 shadow/brush custom).
 */
object NeumorphTokens {
    /** Lebar border pembeda gaya ini dari Material3 Murni (yang border-nya polos/caller-default). */
    val BorderWidth: Dp = 1.5.dp

    /** Warna border -- netral abu-abu terang, alpha cukup tinggi supaya jelas kebaca (bukan hairline tipis ala glass). */
    val BorderColor: Color = Color.White.copy(alpha = 0.35f)
}
