package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MutasiWarga
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutasiScreen(
    viewModel: MainViewModel
) {
    val mutasiList by viewModel.mutasiList.collectAsState()
    val notificationList by viewModel.notifications.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 -> Kelahiran, 1 -> Kematian
    var showFormDialog by remember { mutableStateOf(false) }

    val filteredMutasi = remember(mutasiList, selectedTab) {
        mutasiList.filter {
            if (selectedTab == 0) {
                it.tipe == "Kelahiran"
            } else {
                it.tipe == "Kematian"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mutasi_screen")
    ) {
        // Section: Live Real-Time Notifications Banner (Sistem Notifikasi Pembaruan Data Realtime)
        if (notificationList.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "Active Notifications",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOTIFIKASI REAL-TIME DATABASES (${notificationList.size})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Hapus Semua",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.clickable { viewModel.clearAllNotifications() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        notificationList.take(2).forEach { notif ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissNotification(notif.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mutation Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ChildCare, contentDescription = "Baby")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("👶 Kelahiran")
                }

                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.HeartBroken, contentDescription = "Death indicator")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✝️ Kematian")
                }
            }
        }

        // Section Title + Fab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleText = if (selectedTab == 0) "Daftar Mutasi Kelahiran" else "Daftar Mutasi Kematian"
            Text(
                text = "$titleText (${filteredMutasi.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = { showFormDialog = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("report_mutation_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Laporkan", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Laporkan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Main List Content
        if (filteredMutasi.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada laporan mutasi dicatat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMutasi) { mutasi ->
                    MutasiCard(mutasi)
                }
            }
        }
    }

    // Formulir dialog to record mutation
    if (showFormDialog) {
        MutasiFormDialog(
            tipe = if (selectedTab == 0) "Kelahiran" else "Kematian",
            viewModel = viewModel,
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
fun MutasiCard(mutasi: MutasiWarga) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val colorAccent = if (mutasi.tipe == "Kelahiran") Color(0xFF4CAF50) else Color(0xFFE53935)
                    val emoji = if (mutasi.tipe == "Kelahiran") "👶" else "✝️"
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = mutasi.tipe.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorAccent,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = mutasi.tanggal,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subjek warga
            Text(
                text = if (mutasi.tipe == "Kelahiran") "Nama Anak: ${mutasi.namaWarga}" else "Nama Almarhum: ${mutasi.namaWarga}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (mutasi.nik.isNotEmpty()) {
                Text(
                    text = "NIK: ${mutasi.nik}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "No. KK: ${mutasi.noKk}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Detail keterangan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Keterangan Kejadian:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mutasi.keterangan,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Operator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pelapor: ${mutasi.operator}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Automatic Update indicator
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFCAF0F8)
                ) {
                    Text(
                        text = "Sinkron Database Otomatis",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF03045E),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutasiFormDialog(
    tipe: String, // "Kelahiran" | "Kematian"
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val wargaList by viewModel.filteredWargaList.collectAsState()

    var namaWarga by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Laki-laki") }
    var nik by remember { mutableStateOf("") }
    var noKk by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var keterangan by remember { mutableStateOf("") }

    // Parent lookup search states for birth mutation
    var parentQuery by remember { mutableStateOf("") }
    var selectedParentRt by remember { mutableStateOf("") }
    var selectedParentRw by remember { mutableStateOf("") }

    val suggestedParents = remember(wargaList, parentQuery) {
        if (parentQuery.trim().length >= 2) {
            wargaList.filter {
                it.nama.contains(parentQuery, ignoreCase = true) ||
                it.noKk.contains(parentQuery)
            }.take(5)
        } else {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Laporkan Mutasi Baru: $tipe",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tipe == "Kelahiran") {
                    Text(
                        text = "🔍 CARI DATA ORANG TUA / KK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = parentQuery,
                        onValueChange = { parentQuery = it },
                        label = { Text("Nama Orang Tua atau No. KK") },
                        placeholder = { Text("Ketik nama atau No KK...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Lookup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (suggestedParents.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                suggestedParents.forEach { parent ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                noKk = parent.noKk
                                                selectedParentRt = parent.rt
                                                selectedParentRw = parent.rw
                                                keterangan = "Anak dari Ibu/Bapak ${parent.nama} (Keluarga RT ${parent.rt}/RW ${parent.rw}, KK: ${parent.noKk})"
                                                parentQuery = parent.nama
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = parent.nama,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "No KK: ${parent.noKk}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "RT ${parent.rt}/RW ${parent.rw}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (suggestedParents.last() != parent) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = namaWarga,
                    onValueChange = { namaWarga = it },
                    label = { Text(if (tipe == "Kelahiran") "Nama Anak Lahir" else "Nama Warga Meninggal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (tipe == "Kelahiran") {
                    Column {
                        Text("Jenis Kelamin Bayi", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "Laki-laki", onClick = { gender = "Laki-laki" })
                                Text("Laki-laki", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "Perempuan", onClick = { gender = "Perempuan" })
                                Text("Perempuan", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (tipe == "Kematian") {
                    OutlinedTextField(
                        value = nik,
                        onValueChange = { nik = it },
                        label = { Text("NIK Warga") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = noKk,
                    onValueChange = { noKk = it },
                    label = { Text("No. Kartu Keluarga (KK)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tanggal,
                    onValueChange = { tanggal = it },
                    label = { Text("Tanggal Kejadian (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan Details") },
                    placeholder = {
                        Text(
                            if (tipe == "Kelahiran") "Contoh: Anak dari Ibu Sarah & Bapak Budi, jam 03.11 WIB"
                            else "Contoh: Wafat di Rumah Sakit daerah karena sakit asma"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (tipe == "Kematian") {
                    // Help note indicating automatic update
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "ℹ️ Catatan: Melaporkan kematian otomatis mengubah status warga bersangkutan di daftar warga menjadi 'Meninggal' (non-aktif).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "ℹ️ Catatan: Melaporkan kelahiran otomatis menambahkan registrasi bayi baru (umur 0) di data warga aktif.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (namaWarga.isNotEmpty() && noKk.isNotEmpty() && tanggal.isNotEmpty()) {
                        viewModel.recordMutasi(
                            tipe = tipe,
                            tanggal = tanggal,
                            namaWarga = namaWarga,
                            nik = nik,
                            noKk = noKk,
                            keterangan = keterangan,
                            rt = selectedParentRt,
                            rw = selectedParentRw,
                            gender = if (tipe == "Kelahiran") gender else "Laki-laki",
                            onCompleted = onDismiss
                        )
                    }
                },
                enabled = namaWarga.isNotEmpty() && noKk.isNotEmpty() && tanggal.isNotEmpty()
            ) {
                Text("REKAM MUTASI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
}
