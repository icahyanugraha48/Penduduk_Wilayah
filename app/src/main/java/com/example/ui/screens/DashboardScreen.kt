package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.Warga
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToWarga: (String) -> Unit, // "all" | "lansia" | "disabilitas"
    onNavigateToMutasi: () -> Unit,
    onNavigateToPelaporan: () -> Unit,
    onNavigateToPengumuman: () -> Unit
) {
    val wargaList by viewModel.filteredWargaList.collectAsState()
    val announcements by viewModel.announcementsList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Calculate active stats
    val activeWarga = wargaList.filter { it.status == "Aktif" }
    val totalPenduduk = activeWarga.size
    val totalKk = activeWarga.map { it.noKk }.distinct().size
    val totalLansia = activeWarga.filter { it.isLansia }.size
    val totalDisabilitas = activeWarga.filter { it.isDisabilitas }.size

    // Demographic variables
    val totalLakiLaki = activeWarga.count { it.gender == "Laki-laki" }
    val totalPerempuan = activeWarga.count { it.gender == "Perempuan" }
    val ageAnak = activeWarga.count { it.umur <= 12 }
    val ageRemaja = activeWarga.count { it.umur in 13..17 }
    val ageDewasa = activeWarga.count { it.umur in 18..59 }
    val ageLansia = activeWarga.count { it.isLansia }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header Card
        item {
            WelcomeCard(
                name = currentUser?.displayName ?: "Petugas",
                role = currentUser?.role ?: "RT",
                rt = currentUser?.rt ?: "-",
                rw = currentUser?.rw ?: "-"
            )
        }

        // Stats Header
        item {
            Text(
                text = "Statistik Wilayah RT/RW",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Grid of Stats Card
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                // Stat 1: Total Penduduk
                StatTile(
                    label = "Total Penduduk",
                    value = totalPenduduk.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToWarga("all") }
                )

                // Stat 2: Jumlah KK
                StatTile(
                    label = "Jumlah KK",
                    value = totalKk.toString(),
                    icon = Icons.Default.Home,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToWarga("all") }
                )
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                // Stat 3: Lansia (Color-coded light blue container as from HTML design bg-[#D1E4FF])
                StatTile(
                    label = "Lansia (>= 60 Thn)",
                    value = totalLansia.toString(),
                    icon = Icons.Default.Face,
                    color = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToWarga("lansia") }
                )

                // Stat 4: Disabilitas (Color-coded pink background as from HTML design bg-[#FFD9E2])
                StatTile(
                    label = "Disabilitas",
                    value = totalDisabilitas.toString(),
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.tertiary,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToWarga("disabilitas") }
                )
            }
        }

        // Demographic Visualization Charts
        item {
            DemographicChartsSection(
                activeWarga = activeWarga,
                totalLakiLaki = totalLakiLaki,
                totalPerempuan = totalPerempuan,
                ageAnak = ageAnak,
                ageRemaja = ageRemaja,
                ageDewasa = ageDewasa,
                ageLansia = ageLansia
            )
        }

        // Quick Horizontal Scroll Filters for "Akses Cepat Data Warga"
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Akses Cepat Data Warga",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Semua Warga pill
                    Button(
                        onClick = { onNavigateToWarga("all") },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("👥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Semua Warga", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Lansia pill
                    Button(
                        onClick = { onNavigateToWarga("lansia") },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("👵", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lansia ($totalLansia)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Disabilitas pill
                    Button(
                        onClick = { onNavigateToWarga("disabilitas") },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("♿", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disabilitas ($totalDisabilitas)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Quick Menu Actions Title
        item {
            Text(
                text = "Akses Menu Cepat",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Quick Actions Grid (Rounded 24dp Sleek Card, with clean layout)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickMenuBtn(
                        icon = Icons.Default.SwapHoriz,
                        label = "Mutasi",
                        onClick = onNavigateToMutasi
                    )
                    QuickMenuBtn(
                        icon = Icons.Default.Assessment,
                        label = "Pelaporan",
                        onClick = onNavigateToPelaporan
                    )
                    if (currentUser?.role == "Kepala Wilayah") {
                        QuickMenuBtn(
                            icon = Icons.Default.Campaign,
                            label = "Pengumuman",
                            onClick = onNavigateToPengumuman
                        )
                    }
                }
            }
        }

        // Announcements Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pengumuman Kelurahan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (currentUser?.role == "Kepala Wilayah") {
                    Text(
                        text = "Kelola Info",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onNavigateToPengumuman() }
                    )
                }
            }
        }

        // List of Announcements
        if (announcements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada pengumuman hari ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(announcements.take(3)) { announcement ->
                AnnouncementItem(announcement)
            }
        }
    }
}

@Composable
fun WelcomeCard(
    name: String,
    role: String,
    rt: String,
    rw: String
) {
    // Elegant deep-blue styled gradient matching '#0061A4' and dark slate
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF003F70)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Sleek 'rounded-3xl' style
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selamat Datang,",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Hierarchy Pill Badge
                val hierarchyText = if (role == "Kepala Wilayah") "Kepala Wilayah (Admin)" else "Unit Kerja: RT $rt / RW $rw"
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = hierarchyText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            // Profile circular badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                val symbol = if (role == "Kepala Wilayah") "ADM" else "RT$rt"
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(24.dp), // Highly rounded corners matching theme's rounded-3xl
        border = if (containerColor == MaterialTheme.colorScheme.surface) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (containerColor == MaterialTheme.colorScheme.surface) {
                        color.copy(alpha = 0.12f)
                    } else {
                        Color.White.copy(alpha = 0.25f)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (containerColor == MaterialTheme.colorScheme.surface) color else contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = contentColor.copy(alpha = 0.5f)
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickMenuBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp), // Rounded-2xl matching theme
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    val dateText = announcement.date
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Highly rounded matching theme
        colors = CardDefaults.cardColors(
            containerColor = if (announcement.isUrgent) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (announcement.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "URGENT",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (announcement.wasSentToWhatsapp) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "WA Sent icon",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Disiarkan otomatis via WhatsApp",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E753B),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DemographicChartsSection(
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
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "Demografi Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Visualisasi Demografi Wilayah",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Chart 1: Rasio Gender Card / Component
            Text(
                text = "Rasio Jenis Kelamin (Gender Ratio)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val totalGender = totalLakiLaki + totalPerempuan
            val pctLaki = if (totalGender > 0) (totalLakiLaki.toFloat() / totalGender) else 0f
            val pctPerempuan = if (totalGender > 0) (totalPerempuan.toFloat() / totalGender) else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ring/donut chart visualization inside Canvas
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val mRadius = (size.minDimension - strokeWidth) / 2
                        val mCenter = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                        // If empty, draw placeholder gray ring
                        if (totalGender == 0) {
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                radius = mRadius,
                                center = mCenter,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                        } else {
                            // Draw Laki-laki arc (Blue)
                            drawArc(
                                color = Color(0xFF0061A4),
                                startAngle = -90f,
                                sweepAngle = pctLaki * 360f,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset((size.width - mRadius * 2) / 2, (size.height - mRadius * 2) / 2),
                                size = androidx.compose.ui.geometry.Size(mRadius * 2, mRadius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            // Draw Perempuan arc (Pink)
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Legend keys
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFF0061A4), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Laki-laki: $totalLakiLaki warga (${String.format(java.util.Locale.getDefault(), "%.1f", pctLaki * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFFFFB3B3), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Perempuan: $totalPerempuan warga (${String.format(java.util.Locale.getDefault(), "%.1f", pctPerempuan * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // Chart 2: Age Distribution Bar chart
            Text(
                text = "Distribusi Kelompok Umur (Age Distribution)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val maxAgeVal = maxOf(ageAnak, ageRemaja, ageDewasa, ageLansia).coerceAtLeast(1)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeBarItem(label = "Anak-anak (<= 12 Thn)", value = ageAnak, maxValue = maxAgeVal, color = Color(0xFF4CAF50))
                AgeBarItem(label = "Remaja (13-17 Thn)", value = ageRemaja, maxValue = maxAgeVal, color = Color(0xFFFF9800))
                AgeBarItem(label = "Dewasa (18-59 Thn)", value = ageDewasa, maxValue = maxAgeVal, color = Color(0xFF00BCD4))
                AgeBarItem(label = "Lansia (>= 60 Thn)", value = ageLansia, maxValue = maxAgeVal, color = Color(0xFFE91E63))
            }

            // Chart 3: Sebaran Kategori Disabilitas
            val disabilitasWarga = activeWarga.filter { it.isDisabilitas }
            val sebaranDisabilitas = remember(activeWarga) {
                disabilitasWarga
                    .groupBy { it.jenisDisabilitas.trim().lowercase().replaceFirstChar { char -> char.uppercase() }.ifEmpty { "Umum/Lainnya" } }
                    .mapValues { it.value.size }
            }

            if (disabilitasWarga.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sebaran Kategori Disabilitas (${disabilitasWarga.size} Warga)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val maxDisVal = sebaranDisabilitas.values.maxOrNull()?.coerceAtLeast(1) ?: 1

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00BCD4))
                    sebaranDisabilitas.entries.forEachIndexed { index, entry ->
                        val barColor = colors[index % colors.size]
                        DisabilitasBarItem(
                            label = entry.key,
                            value = entry.value,
                            maxValue = maxDisVal,
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgeBarItem(label: String, value: Int, maxValue: Int, color: Color) {
    val pct = value.toFloat() / maxValue.toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("$value warga", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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

@Composable
fun DisabilitasBarItem(label: String, value: Int, maxValue: Int, color: Color) {
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
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
