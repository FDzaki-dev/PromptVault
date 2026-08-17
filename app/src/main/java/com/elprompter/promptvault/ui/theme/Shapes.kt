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
