package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MutasiWarga
import com.example.data.model.Warga
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreLoginRecapScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit
) {
    val wargaList by viewModel.wargaList.collectAsState()
    val mutasiList by viewModel.mutasiList.collectAsState()

    // Process statistics
    val totalWargaAktif = remember(wargaList) { wargaList.count { it.status == "Aktif" } }
    val totalKeluarga = remember(wargaList) { wargaList.filter { it.status == "Aktif" }.map { it.noKk }.distinct().size }
    val totalLansia = remember(wargaList) { wargaList.count { it.status == "Aktif" && it.isLansia } }
    val totalDisabilitas = remember(wargaList) { wargaList.count { it.status == "Aktif" && it.isDisabilitas } }
    val totalKelahiran = remember(mutasiList) { mutasiList.count { it.tipe == "Kelahiran" } }
    val totalKematian = remember(mutasiList) { mutasiList.count { it.tipe == "Kematian" } }

    val activeWarga = remember(wargaList) { wargaList.filter { it.status == "Aktif" } }
    val totalLakiLaki = remember(activeWarga) { activeWarga.count { it.gender == "Laki-laki" } }
    val totalPerempuan = remember(activeWarga) { activeWarga.count { it.gender == "Perempuan" } }
    val ageAnak = remember(activeWarga) { activeWarga.count { it.umur <= 12 } }
    val ageRemaja = remember(activeWarga) { activeWarga.count { it.umur in 13..17 } }
    val ageDewasa = remember(activeWarga) { activeWarga.count { it.umur in 18..59 } }
    val ageLansiaForCharts = remember(activeWarga) { activeWarga.count { it.isLansia } }

    val recentMutations = remember(mutasiList) {
        mutasiList.sortedByDescending { it.id }.take(6)
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pre_login_recap_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🇮🇩 REKAP DATA PENDUKUK REALTIME",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevationRecap(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(gradientBrush)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcoming Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sistem Informasi Kependudukan Desa (SIKD)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Statistik tabulasi demografis dan mutasi rukun tetangga/rukun warga yang terus diperbarui secara realtime oleh operator wilayah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Grid Rekapitulasi Data Penduduk
                Text(
                    text = "📊 STATISTIK AGREGAT KEPENDUKUKAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecapStatCard(
                        title = "Warga Aktif",
                        value = totalWargaAktif.toString(),
                        subtitle = "Jiwa Terdaftar",
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    RecapStatCard(
                        title = "Kepala Keluarga",
                        value = totalKeluarga.toString(),
                        subtitle = "Kartu Keluarga",
                        icon = Icons.Default.Home,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecapStatCard(
                        title = "Kelompok Lansia",
                        value = totalLansia.toString(),
                        subtitle = "Umur >= 60 tahun",
                        icon = Icons.Default.Elderly,
                        color = Color(0xFF673AB7),
                        modifier = Modifier.weight(1f)
                    )
                    RecapStatCard(
                        title = "Disabilitas",
                        value = totalDisabilitas.toString(),
                        subtitle = "Kebutuhan Khusus",
                        icon = Icons.Default.AccessibilityNew,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecapStatCard(
                        title = "Total Kelahiran",
                        value = "👶 $totalKelahiran",
                        subtitle = "Bayi Baru Lahir",
                        icon = Icons.Default.ChildCare,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    RecapStatCard(
                        title = "Total Kematian",
                        value = "✝️ $totalKematian",
                        subtitle = "Meninggal Dunia",
                        icon = Icons.Default.Nature,
                        color = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Demographic Charts Section (Visualizations)
                Text(
                    text = "📊 GRAFIK VISUALISASI DEMOGRAFI REAL-TIME",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                PreLoginDemographicChartsSection(
                    activeWarga = activeWarga,
                    totalLakiLaki = totalLakiLaki,
                    totalPerempuan = totalPerempuan,
                    ageAnak = ageAnak,
                    ageRemaja = ageRemaja,
                    ageDewasa = ageDewasa,
                    ageLansia = ageLansiaForCharts
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timeline update mutasi kependudukan terbaru
                Text(
                    text = "🔔 REKAM PERUBAHAN DATA PENDUDUK TERBARU",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                if (recentMutations.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Belum ada pencatatan mutasi kependudukan di wilayah ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentMutations.forEach { mutation ->
                                PreLoginMutationRow(mutation)
                                if (recentMutations.last() != mutation) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom bar action panel (Proceed to Secure Login Portal)
            Surface(
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("go_to_login_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Sistem Login", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOGIN SEBAGAI OPERATOR WILAYAH",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aman • Hanya RT/RW/Admin yang diberi wewenang dapat masuk.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RecapStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PreLoginMutationRow(mutation: MutasiWarga) {
    val colorAccent = when (mutation.tipe) {
        "Kelahiran" -> Color(0xFF4CAF50)
        "Kematian" -> Color(0xFFE53935)
        else -> MaterialTheme.colorScheme.primary
    }
    val emoji = when (mutation.tipe) {
        "Kelahiran" -> "👶"
        "Kematian" -> "✝️"
        else -> "📌"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = colorAccent.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 16.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (mutation.tipe == "Kelahiran") "Demografi Kelahiran" else "Demografi Kematian",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (mutation.tipe == "Kelahiran") "Nama Anak: ${mutation.namaWarga}" else "Nama Almarhum: ${mutation.namaWarga}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tanggal: ${mutation.tanggal} • No. KK: ${mutation.noKk.take(12)}...",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = mutation.tipe,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

fun ColorScheme.surfaceColorAtElevationRecap(elevation: androidx.compose.ui.unit.Dp): Color {
    return this.surfaceVariant.copy(alpha = 0.5f)
}

@Composable
fun PreLoginDemographicChartsSection(
    activeWarga: List<Warga>,
    totalLakiLaki: Int,
    totalPerempuan: Int,
    ageAnak: Int,
    ageRemaja: Int,
    ageDewasa: Int,
    ageLansia: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Chart 1: Gender Ratio
            Text(
                text = "Rasio Jenis Kelamin (Gender Ratio)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            val totalGender = totalLakiLaki + totalPerempuan
            val pctLaki = if (totalGender > 0) (totalLakiLaki.toFloat() / totalGender) else 0f
            val pctPerempuan = if (totalGender > 0) (totalPerempuan.toFloat() / totalGender) else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        val mRadius = (size.minDimension - strokeWidth) / 2
                        val mCenter = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                        if (totalGender == 0) {
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                radius = mRadius,
                                center = mCenter,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                        } else {
                            drawArc(
                                color = Color(0xFF0061A4),
                                startAngle = -90f,
                                sweepAngle = pctLaki * 360f,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset((size.width - mRadius * 2) / 2, (size.height - mRadius * 2) / 2),
                                size = androidx.compose.ui.geometry.Size(mRadius * 2, mRadius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFFFFB3B3),
                                startAngle = -90f + (pctLaki * 360f),
                                sweepAngle = pctPerempuan * 360f,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset((size.width - mRadius * 2) / 2, (size.height - mRadius * 2) / 2),
                                size = androidx.compose.ui.geometry.Size(mRadius * 2, mRadius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = "$totalGender",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF0061A4), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Laki-laki: $totalLakiLaki (${String.format(java.util.Locale.getDefault(), "%.1f", pctLaki * 100)}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFB3B3), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Perempuan: $totalPerempuan (${String.format(java.util.Locale.getDefault(), "%.1f", pctPerempuan * 100)}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Chart 2: Age Distribution
            Text(
                text = "Distribusi Kelompok Umur (Age Groups)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            val maxAgeVal = maxOf(ageAnak, ageRemaja, ageDewasa, ageLansia).coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PreLoginAgeBarItem(label = "Anak-anak (<=12)", value = ageAnak, maxValue = maxAgeVal, color = Color(0xFF4CAF50))
                PreLoginAgeBarItem(label = "Remaja (13-17)", value = ageRemaja, maxValue = maxAgeVal, color = Color(0xFFFF9800))
                PreLoginAgeBarItem(label = "Dewasa (18-59)", value = ageDewasa, maxValue = maxAgeVal, color = Color(0xFF00BCD4))
                PreLoginAgeBarItem(label = "Lansia (>=60)", value = ageLansia, maxValue = maxAgeVal, color = Color(0xFFE91E63))
            }

            // Chart 3: Disability sebaran
            val disabilitasWarga = activeWarga.filter { it.isDisabilitas }
            if (disabilitasWarga.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sebaran Kategori Disabilitas (${disabilitasWarga.size} Warga)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                val sebaranDisabilitas = disabilitasWarga
                    .groupBy { it.jenisDisabilitas.trim().lowercase().replaceFirstChar { char -> char.uppercase() }.ifEmpty { "Umum/Lainnya" } }
                    .mapValues { it.value.size }

                val maxDisVal = sebaranDisabilitas.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                val colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00BCD4))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sebaranDisabilitas.entries.forEachIndexed { index, entry ->
                        PreLoginAgeBarItem(
                            label = entry.key,
                            value = entry.value,
                            maxValue = maxDisVal,
                            color = colors[index % colors.size]
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreLoginAgeBarItem(label: String, value: Int, maxValue: Int, color: Color) {
    val pct = value.toFloat() / maxValue.toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("$value warga", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
