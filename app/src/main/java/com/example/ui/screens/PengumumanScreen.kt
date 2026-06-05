package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengumumanScreen(
    viewModel: MainViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val announcements by viewModel.announcementsList.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var triggerWhatsapp by remember { mutableStateOf(true) }

    val isRegionalHead = currentUser?.role == "Kepala Wilayah"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pengumuman_screen")
    ) {
        // Form to write Announcement (Only for Kepala Wilayah / Kelurahan level admin)
        if (isRegionalHead) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📢 Buat Pengumuman Kelurahan Resmi",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Rapat / Info Pengumuman") },
                        placeholder = { Text("Contoh: Rapat Pleno Keamanan RT 01-05") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Instruksi / Detail Pengumuman") },
                        placeholder = { Text("Contoh: Diharapkan pengurus membawa rekap KK terbaru...") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Urgent toggle
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                            Text("🚨 Urgent / Darurat")
                        }

                        // WhatsApp dispatch toggle
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                            Checkbox(checked = triggerWhatsapp, onCheckedChange = { triggerWhatsapp = it })
                            Text("📞 Broadcaster WA")
                        }
                    }

                    // Dispatch Button
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && content.isNotEmpty()) {
                                viewModel.createKelurahanAnnouncement(
                                    title = title,
                                    content = content,
                                    isUrgent = isUrgent,
                                    context = context,
                                    triggerWhatsapp = triggerWhatsapp
                                )
                                // Clear
                                title = ""
                                content = ""
                                isUrgent = false
                            }
                        },
                        enabled = title.isNotEmpty() && content.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp) // Sleek rounded-2xl style
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Publish")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (triggerWhatsapp) "PUBLISH & BAGIKAN KE WA" else "KIRIM PENGUMUMAN",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        } else {
            // Header for standard officers stating view only
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.infoContainerElevated())
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = "info announcments",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Halaman Pengumuman Kelurahan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Silakan ikuti instruksi terbaru dari Kelurahan. Anda dapat membagikan ulang info penting ini langsung ke grup RT Anda melalui WhatsApp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // List display header
        Text(
            text = "Semua Riwayat Pengumuman (${announcements.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Lazy scroll of announcements
        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ditemukan pengumuman apapun.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(announcements) { ann ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Diposting: ${ann.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (ann.isUrgent) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = "🚨 URGENT",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ann.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ann.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Share helper row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // WA status flag
                                if (ann.wasSentToWhatsapp) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = "✅ WA: Sent successfully",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    ) {
                                        Text(
                                            text = "Internal App Only",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Quick Share button
                                TextButton(
                                    onClick = {
                                        // Trigger individual manual forward share
                                        viewModel.createKelurahanAnnouncement(
                                            title = ann.title,
                                            content = ann.content,
                                            isUrgent = ann.isUrgent,
                                            context = context,
                                            triggerWhatsapp = true
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Forward WA", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bagikan Ulang WA", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
