package com.fdzaki.promptvault.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Folder,
        title = "Selamat Datang di PromptVault",
        body = "Folder Downloads kamu sering penuh sesak dengan file ZIP dan TXT dari banyak proyek berbeda? " +
            "PromptVault membantu merapikannya secara otomatis, tanpa perlu kamu pindah-pindah file satu per satu."
    ),
    OnboardingPage(
        icon = Icons.Default.Search,
        title = "Cara Kerjanya Sederhana",
        body = "Kamu buat \"aturan\": misalnya file yang namanya diawali \"AudioPlayer\" akan otomatis " +
            "dipindahkan ke folder Downloads/PromptVault/AudioPlayer/. Semua file sejenis akan " +
            "berkumpul rapi di satu tempat."
    ),
    OnboardingPage(
        icon = Icons.Default.Lock,
        title = "Kenapa Perlu Izin Penyimpanan?",
        body = "Agar bisa memindahkan file di dalam folder Downloads, PromptVault butuh izin \"Akses semua file\". " +
            "Izin ini hanya dipakai untuk memindahkan file sesuai aturanmu — tidak membaca isi file " +
            "atau mengirim apa pun ke internet."
    ),
    OnboardingPage(
        icon = Icons.Default.CheckCircle,
        title = "Siap Digunakan",
        body = "Tekan \"Scan Downloads Now\" kapan saja untuk merapikan file secara manual, atau biarkan " +
            "PromptVault memeriksa otomatis setiap 15 menit di latar belakang. Kamu bisa tambah, " +
            "ubah, atau hapus aturan kapan pun dari layar utama."
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var pageIndex by remember { mutableStateOf(0) }
    val isLastPage = pageIndex == pages.lastIndex

    Scaffold(
        bottomBar = {
            Column(Modifier.padding(24.dp)) {
                PageIndicator(total = pages.size, current = pageIndex)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pageIndex > 0) {
                        TextButton(onClick = { pageIndex-- }) { Text("Kembali") }
                    } else {
                        TextButton(onClick = onFinish) { Text("Lewati") }
                    }
                    Button(onClick = {
                        if (isLastPage) onFinish() else pageIndex++
                    }) {
                        Text(if (isLastPage) "Mulai Pakai" else "Lanjut")
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = pageIndex,
            label = "onboarding_page",
            transitionSpec = { fadeThroughTransition() }
        ) { index ->
            OnboardingPageContent(
                page = pages[index],
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.body,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun PageIndicator(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == current) 10.dp else 8.dp)
                    .background(
                        color = if (i == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun fadeThroughTransition() =
    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)))
        .togetherWith(androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(120)))
