package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.elprompter.promptvault.ui.theme.Ink
import com.elprompter.promptvault.ui.theme.Kraft
import com.elprompter.promptvault.ui.theme.Pine

/**
 * Top bar konsisten dipakai di semua layar selain Home, supaya user selalu
 * punya jalan balik yang JELAS (bukan cuma mengandalkan gesture back sistem)
 * dan tahu lagi di layar mana -- standar dasar navigasi Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title.uppercase(), style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Pine)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Kraft,
            titleContentColor = Ink,
            navigationIconContentColor = Pine,
            actionIconContentColor = Pine
        )
    )
}
