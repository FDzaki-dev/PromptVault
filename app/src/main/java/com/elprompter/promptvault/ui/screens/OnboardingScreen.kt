package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class OnboardingStep(val icon: ImageVector, val title: String, val body: String)

private val steps = listOf(
    OnboardingStep(Icons.Filled.Archive, "Selamat datang di PromptVault", "App ini merapikan otomatis file (ekstensi apa saja) di folder Downloads kamu, sesuai rule yang kamu buat."),
    OnboardingStep(Icons.Filled.Folder, "Buat rule", "Rule menentukan pattern nama file (mis. *.txt) dan folder tujuan di dalam Downloads/PromptVault/."),
    OnboardingStep(Icons.Filled.Lock, "Izin penyimpanan", "PromptVault perlu izin akses semua file agar bisa memindahkan file di Downloads."),
    OnboardingStep(Icons.Filled.Schedule, "Auto-sort berjalan sendiri", "Setelah rule dibuat, app akan memindai secara berkala sesuai interval yang kamu atur.")
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ProgressDots(total = steps.size, current = index)

            Crossfade(targetState = index, label = "onboardingStep", animationSpec = tween(220)) { stepIndex ->
                val currentStep = steps[stepIndex]
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(colors.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(currentStep.icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(36.dp))
                    }

                    Text("Langkah ${stepIndex + 1} dari ${steps.size}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(currentStep.title, style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
                    Text(currentStep.body, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (index < steps.lastIndex) index++ else onFinished() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = colors.onSecondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (index < steps.lastIndex) "Lanjut" else "Mulai")
            }
            if (index > 0) {
                OutlinedButton(
                    onClick = { index-- },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kembali") }
            }
        }
    }
}

@Composable
private fun ProgressDots(total: Int, current: Int) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (i == current) 24.dp else 12.dp)
                    .background(
                        if (i == current) colors.primary else colors.outline,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
