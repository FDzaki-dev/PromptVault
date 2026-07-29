package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class OnboardingStep(val title: String, val body: String)

private val steps = listOf(
    OnboardingStep("Selamat datang di PromptVault", "App ini merapikan otomatis file ZIP & TXT di folder Downloads kamu."),
    OnboardingStep("Buat rule", "Rule menentukan pattern nama file (mis. *.txt) dan folder tujuan di dalam Downloads/PromptVault/."),
    OnboardingStep("Izin penyimpanan", "PromptVault perlu izin akses semua file agar bisa memindahkan file di Downloads."),
    OnboardingStep("Auto-sort berjalan sendiri", "Setelah rule dibuat, app akan memindai secara berkala sesuai interval yang kamu atur.")
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    val step = steps[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Langkah ${index + 1} dari ${steps.size}", style = MaterialTheme.typography.labelLarge)
        Text(step.title, style = MaterialTheme.typography.headlineSmall)
        Text(step.body, style = MaterialTheme.typography.bodyLarge)

        Button(
            onClick = { if (index < steps.lastIndex) index++ else onFinished() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (index < steps.lastIndex) "Lanjut" else "Mulai")
        }
        if (index > 0) {
            Button(onClick = { index-- }, modifier = Modifier.fillMaxWidth()) { Text("Kembali") }
        }
    }
}
