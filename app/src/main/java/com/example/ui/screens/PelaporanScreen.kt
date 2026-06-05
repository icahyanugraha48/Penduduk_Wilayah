package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.util.PdfReportGenerator
import android.widget.Toast
import com.example.data.model.Warga
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PelaporanScreen(
    viewModel: MainViewModel
) {
    val wargaList by viewModel.filteredWargaList.collectAsState()
    val mutasiList by viewModel.mutasiList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedRtFilter by remember { mutableStateOf(currentUser?.rt?.ifEmpty { "01" } ?: "01") }
    var selectedRwFilter by remember { mutableStateOf("02") }
    var selectedMonth by remember { mutableStateOf("Juni 2026") }

    var isGeneratingReport by remember { mutableStateOf(false) }
    var showPrintPreviewDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Determine current scoping
    val isRegionalHead = currentUser?.role == "Kepala Wilayah"

    // Filtered citizens active
    val filteredWarga = remember(wargaList, selectedRtFilter, selectedRwFilter, isRegionalHead) {
        wargaList.filter { warga ->
            warga.status == "Aktif" &&
            (isRegionalHead || (warga.rt == selectedRtFilter && warga.rw == selectedRwFilter))
        }
    }

    // Filter mutations for local scoped
    val filteredMutasi = remember(mutasiList, selectedRtFilter, selectedRwFilter, isRegionalHead) {
        mutasiList.filter { mut ->
            isRegionalHead || (mut.operator.contains("rt") && mut.operator.endsWith(selectedRtFilter))
        }
    }

    // Statistics Calculation
    val totalWarga = filteredWarga.size
    val totalKk = filteredWarga.map { it.noKk }.distinct().size

    val totalLakiLaki = filteredWarga.count { it.gender == "Laki-laki" }
    val totalPerempuan = filteredWarga.count { it.gender == "Perempuan" }
    val totalDisabilitas = filteredWarga.count { it.isDisabilitas }

    // SIKD Standard 8 Age Classifications (requested by user)
    val ageBalita = filteredWarga.count { it.umur in 0..5 }
    val ageAnakAnak = filteredWarga.count { it.umur in 6..11 }
    val ageRemajaDetil = filteredWarga.count { it.umur in 12..25 }
    val ageDewasaDetil = filteredWarga.count { it.umur in 26..45 }
    val agePraLansia = filteredWarga.count { it.umur in 46..59 }
    val ageLansiaMuda = filteredWarga.count { it.umur in 60..69 }
    val ageLansiaMadya = filteredWarga.count { it.umur in 70..79 }
    val ageLansiaParipurna = filteredWarga.count { it.umur >= 80 }
    val ageLansia = ageLansiaMuda + ageLansiaMadya + ageLansiaParipurna

    // Mutations counts this period
    val totalMelahirkan = filteredMutasi.count { it.tipe == "Kelahiran" }
    val totalWafat = filteredMutasi.count { it.tipe == "Kematian" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("pelaporan_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Heading Filters Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konfigurasi Rekapitulasi Laporan",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // RT Input (Disabled if not Admin)
                    OutlinedTextField(
                        value = selectedRtFilter,
                        onValueChange = { if (isRegionalHead) selectedRtFilter = it },
                        label = { Text("RT") },
                        enabled = isRegionalHead,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = selectedRwFilter,
                        onValueChange = { if (isRegionalHead) selectedRwFilter = it },
                        label = { Text("RW") },
                        enabled = isRegionalHead,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Month picker (Simulation Selection)
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = { selectedMonth = it },
                        label = { Text("Periode") },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                if (!isRegionalHead) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ℹ️ Anda登录 sebagai RT $selectedRtFilter. Rekapitulasi otomatis terbatas pada wilayah kerja Anda.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Summary Recap Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Data Rekapitulasi RT $selectedRtFilter / RW $selectedRwFilter",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // stats count GRID
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            ReportStatsTile(
                title = "Total Penduduk",
                value = "$totalWarga Orang",
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f)
            )

            ReportStatsTile(
                title = "Jumlah Kartu Keluarga",
                value = "$totalKk KK",
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.Home,
                modifier = Modifier.weight(1f)
            )
        }

        // Gender balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Distribusi Jenis Kelamin",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Male, contentDescription = "Laki-laki", tint = Color(0xFF1E88E5), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Laki-Laki", style = MaterialTheme.typography.bodySmall)
                        Text(text = "$totalLakiLaki", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Female, contentDescription = "Perempuan", tint = Color(0xFFEC407A), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Perempuan", style = MaterialTheme.typography.bodySmall)
                        Text(text = "$totalPerempuan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }

        // Age intervals statistics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Distribusi Kelompok Umur Kependudukan",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                AgebarItem(label = "👶 Balita (Bawah Lima Tahun | 0-5 Thn)", count = ageBalita, total = totalWarga, Color(0xFF4CAF50))
                AgebarItem(label = "🧒 Anak-anak (5-11 Thn)", count = ageAnakAnak, total = totalWarga, Color(0xFF8BC34A))
                AgebarItem(label = "👦 Remaja (12-25 Thn)", count = ageRemajaDetil, total = totalWarga, Color(0xFF00BCD4))
                AgebarItem(label = "🧑 Dewasa (26-45 Thn)", count = ageDewasaDetil, total = totalWarga, Color(0xFF2196F3))
                AgebarItem(label = "🧓 Pra-Lansia (Lansia Dini | 45-59 Thn)", count = agePraLansia, total = totalWarga, Color(0xFF9C27B0))
                AgebarItem(label = "👴 Lansia Muda (60-69 Thn)", count = ageLansiaMuda, total = totalWarga, Color(0xFFE91E63))
                AgebarItem(label = "👵 Lansia Madya (70-79 Thn)", count = ageLansiaMadya, total = totalWarga, Color(0xFFFF5722))
                AgebarItem(label = "👑 Lansia Paripurna (Sangat Tua | >=80 Thn)", count = ageLansiaParipurna, total = totalWarga, Color(0xFFFF9800))
            }
        }

        // Mutasi activities summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aktivitas Mutasi Bulan Ini ($selectedMonth)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("👶 Kelahiran", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Text("$totalMelahirkan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color(0xFF2E7D32))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("✝️ Kematian", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                            Text("$totalWafat", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color(0xFFC62828))
                        }
                    }
                }
            }
        }

        // Auto report generator button (Laporan Bulanan Otomatis)
        Button(
            onClick = {
                coroutineScope.launch {
                    isGeneratingReport = true
                    delay(1500) // Simulate complex database generation tasks
                    isGeneratingReport = false
                    showPrintPreviewDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_monthly_report_btn"),
            shape = RoundedCornerShape(16.dp) // Sleek rounded-2xl style
        ) {
            if (isGeneratingReport) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Menghitung Data Kependudukan...")
            } else {
                Icon(Icons.Default.Print, contentDescription = "Cetak")
                Spacer(modifier = Modifier.width(8.dp))
                Text("PROSES & CETAK LAPORAN BULANAN (PDF)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black))
            }
        }
    }

    // Official print preview dialog (Laporan Bulanan Tergenerate Otomatis)
    if (showPrintPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPrintPreviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Verified OK", tint = Color(0xFF25D366))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Laporan Bulanan Tergenerate!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Berikut draft surat laporan bulanan resmi yang siap dicetak ke Kantor Kelurahan:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Printed style receipt text area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Logo Head style
                            Text(
                                "LAPORAN BULANAN KEPENDUDUKAN",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "KELURAHAN WILAYAH RW 02 - RT $selectedRtFilter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "==============================",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            // Specs values
                            ReportRowPrint("BULAN LAPORAN", selectedMonth)
                            ReportRowPrint("TOTAL PENDUDUK", "$totalWarga Jiwa")
                            ReportRowPrint("TOTAL KK", "$totalKk Dokumen")
                            ReportRowPrint("MALE BALANCES", "$totalLakiLaki Orang")
                            ReportRowPrint("FEMALE BALANCES", "$totalPerempuan Orang")
                            ReportRowPrint("LANSIA (>=60 THN)", "$ageLansia Jiwa")
                            ReportRowPrint("DISABILITAS JML", "$totalDisabilitas Jiwa")
                            ReportRowPrint("MUTASI LAHIR", "$totalMelahirkan Orang")
                            ReportRowPrint("MUTASI WAFAT", "$totalWafat Orang")

                            Text(
                                "------------------------------",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            // Footer stamp
                            Text(
                                "Tanggal Cetak: 2026-06-05",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                textAlign = TextAlign.Start
                            )
                            Text(
                                "Ttd. Operator RT $selectedRtFilter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val context = LocalContext.current
                Button(
                    onClick = {
                        try {
                            val result = PdfReportGenerator.generateReportPdf(
                                context,
                                selectedRtFilter,
                                selectedRwFilter,
                                selectedMonth,
                                filteredWarga,
                                filteredMutasi
                            )
                            Toast.makeText(context, "Selesai Mengunduh: ${result.filename}", Toast.LENGTH_LONG).show()

                            viewModel.addNotification(
                                "Berkas PDF Siap",
                                "Laporan kependudukan RT $selectedRtFilter RW $selectedRwFilter berhasil disimpan ke instalan Downloads!"
                            )

                            // Try to let the user view/print the document immediately
                            result.uri?.let { uri ->
                                val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(android.content.Intent.createChooser(viewIntent, "Buka/Cetak Berkas PDF"))
                                } catch (e: Exception) {
                                    // Chooser alternate send stream
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Berkas PDF"))
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Eror membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showPrintPreviewDialog = false
                    }
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = "Download")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DOWNLOAD PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrintPreviewDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }
}

@Composable
fun ReportStatsTile(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AgebarItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0.0f

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "$count (${String.format("%.1f", progress * 100)}%)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun ReportRowPrint(left: String, right: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.DarkGray
        )
        Text(
            text = right,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.Black
        )
    }
}
