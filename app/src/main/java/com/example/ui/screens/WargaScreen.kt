package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.Warga
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WargaScreen(
    viewModel: MainViewModel,
    initialFilter: String = "all" // "all" | "lansia" | "disabilitas"
) {
    val wargaList by viewModel.filteredWargaList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isAdmin = currentUser?.role == "Kepala Wilayah"

    // Action permissions controlled by Admin (Kepala Wilayah)
    val actionTambahEnabled by viewModel.actionTambahEnabled.collectAsState()
    val actionEditEnabled by viewModel.actionEditEnabled.collectAsState()
    val actionHapusEnabled by viewModel.actionHapusEnabled.collectAsState()
    val actionMeninggalEnabled by viewModel.actionMeninggalEnabled.collectAsState()
    val actionImportExportEnabled by viewModel.actionImportExportEnabled.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(if (initialFilter == "lansia") 1 else if (initialFilter == "disabilitas") 2 else 0) }

    var showFormDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var selectedWargaForEdit by remember { mutableStateOf<Warga?>(null) }
    var selectedWargaForDetail by remember { mutableStateOf<Warga?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Warga?>(null) }
    var showMeninggalConfirmDialog by remember { mutableStateOf<Warga?>(null) }

    // Filtered lists based on tab and active status
    val activeWargaList = wargaList.filter { it.status == "Aktif" }

    val filteredWarga = remember(wargaList, searchQuery, selectedTab) {
        activeWargaList.filter { warga ->
            // Tab Filter
            val matchesTab = when (selectedTab) {
                0 -> true // Semua
                1 -> warga.isLansia // Sub menu Lansia
                2 -> warga.isDisabilitas // Sub menu Disabilitas
                else -> true
            }

            // Search Query
            val matchesSearch = warga.nama.contains(searchQuery, ignoreCase = true) ||
                    warga.nik.contains(searchQuery) ||
                    warga.noKk.contains(searchQuery)

            matchesTab && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("warga_screen")
        ) {
        // Upper search and status bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari warga berdasarkan Nama/NIK/KK...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("warga_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom M3 Sub-Menu Tabs (Semua, Lansia, Disabilitas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPill(label = "Semua", selected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1.0f))
                    TabPill(label = "👴 Lansia", selected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1.2f))
                    TabPill(label = "🦽 Disabilitas", selected = selectedTab == 2, onClick = { selectedTab = 2 }, modifier = Modifier.weight(1.4f))
                }
            }
        }

        // Header Title + Fab trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleText = when (selectedTab) {
                0 -> "Semua Data Warga"
                1 -> "Warga Lansia"
                2 -> "Warga Disabilitas"
                else -> "Daftar Warga"
            }
            Text(
                text = "$titleText (${filteredWarga.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Import/Export Excel CSV Button
            if (isAdmin || actionImportExportEnabled) {
                IconButton(
                    onClick = { showImportExportDialog = true },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ImportExport,
                        contentDescription = "Format xlsx/csv Disdukcapil Alignment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // List display
        if (filteredWarga.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Data warga tidak ditemukan",
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWarga) { warga ->
                    WargaCard(
                        warga = warga,
                        isAdmin = isAdmin,
                        actionEditEnabled = actionEditEnabled,
                        actionHapusEnabled = actionHapusEnabled,
                        actionMeninggalEnabled = actionMeninggalEnabled,
                        onCardClick = {
                            selectedWargaForDetail = warga
                        },
                        onEditClick = {
                            selectedWargaForEdit = warga
                            showFormDialog = true
                        },
                        onDeleteClick = {
                            showDeleteConfirmDialog = warga
                        },
                        onMeninggalClick = {
                            showMeninggalConfirmDialog = warga
                        }
                    )
                }
            }
        }
    } // Closes Column

    // Floating Action Button in the bottom-right corner, directly above settings gear (bottom-right most menu item)
    if (isAdmin || actionTambahEnabled) {
        FloatingActionButton(
            onClick = {
                selectedWargaForEdit = null
                showFormDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 24.dp)
                .testTag("add_warga_btn"),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Warga",
                modifier = Modifier.size(24.dp)
            )
        }
    }
} // Closes Box

    // Citizen add/edit form Dialog
    if (showFormDialog) {
        WargaFormDialog(
            warga = selectedWargaForEdit,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false }
        )
    }

    // Citizen detailed display overlay dialog
    if (selectedWargaForDetail != null) {
        WargaDetailDialog(
            warga = selectedWargaForDetail!!,
            viewModel = viewModel,
            isAdmin = isAdmin,
            actionEditEnabled = actionEditEnabled,
            actionHapusEnabled = actionHapusEnabled,
            actionMeninggalEnabled = actionMeninggalEnabled,
            onDismiss = { selectedWargaForDetail = null },
            onEditClick = {
                selectedWargaForEdit = selectedWargaForDetail
                selectedWargaForDetail = null
                showFormDialog = true
            },
            onDeleteClick = {
                showDeleteConfirmDialog = selectedWargaForDetail
                selectedWargaForDetail = null
            },
            onMeninggalClick = {
                showMeninggalConfirmDialog = selectedWargaForDetail
            }
        )
    }

    // Death mutation logging dialog (Meninggal)
    if (showMeninggalConfirmDialog != null) {
        val target = showMeninggalConfirmDialog!!
        var tanggalKematian by remember { mutableStateOf<String>(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var keterangan by remember { mutableStateOf<String>("") }

        AlertDialog(
            onDismissRequest = { showMeninggalConfirmDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Meninggal", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Konfirmasi Warga Meninggal Dunia") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Apakah Anda yakin ingin menyatakan warga bernama '${target.nama}' (NIK: ${target.nik}) sebagai Meninggal Dunia? Tindakan ini akan memperbarui status kependudukan dan mencatat Mutasi Kematian.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    OutlinedTextField(
                        value = tanggalKematian,
                        onValueChange = { tanggalKematian = it },
                        label = { Text("Tanggal Meninggal (yyyy-mm-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        label = { Text("Keterangan Kematian (Sakit/Sebab, dll)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tanggalKematian.isNotEmpty()) {
                            viewModel.recordWargaMeninggal(
                                warga = target,
                                tanggalKematian = tanggalKematian,
                                keterangan = keterangan,
                                onCompleted = {
                                    showMeninggalConfirmDialog = null
                                    if (selectedWargaForDetail?.id == target.id) {
                                        selectedWargaForDetail = null
                                    }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("KONFIRMASI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMeninggalConfirmDialog = null }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Delete confirmation alerts
    if (showDeleteConfirmDialog != null) {
        val target = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Data Warga") },
            text = { Text("Apakah Anda yakin ingin menghapus data warga '${target.nama}' (NIK: ${target.nik})? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWarga(target)
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text("HAPUS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("BATAL")
                }
            }
        )
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            viewModel = viewModel,
            onDismiss = { showImportExportDialog = false }
        )
    }
}

@Composable
fun TabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WargaCard(
    warga: Warga,
    isAdmin: Boolean,
    actionEditEnabled: Boolean,
    actionHapusEnabled: Boolean,
    actionMeninggalEnabled: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMeninggalClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
        border = BorderStroke(1.dp, if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = warga.nama,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Gender tag
                        val genderColor = if (warga.gender == "Laki-laki") Color(0xFF1E88E5) else Color(0xFFEC407A)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = genderColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = if (warga.gender == "Laki-laki") "L" else "P",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = genderColor
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NIK: ${warga.nik}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No KK: ${warga.noKk}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Operations & Expand Indicator Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isAdmin || actionEditEnabled) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Warga",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (isAdmin || actionHapusEnabled) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Warga",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detail badges (Lansia / Disabilitas indicators)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RT RW
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "RT ${warga.rt}/RW ${warga.rw}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Umur Tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${warga.umur} Thn",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (warga.isLansia) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "👴 Lansia",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                              ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (warga.isDisabilitas) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE91E63).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🦽 ${warga.jenisDisabilitas.ifEmpty { "Disabilitas" }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFAD1457),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bantuan Label tag if there are items entered
                if (warga.bantuanList.trim().isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🎁 Bantuan: ${warga.bantuanList}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "📋 DATA KEPENDUDUKAN (DISDUKCAPIL)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Details structured grid
                    WargaDetailRow(label = "Tempat, Tgl Lahir", value = "${warga.tempatLahir}, ${warga.tanggalLahir}")
                    WargaDetailRow(label = "Agama", value = warga.agama)
                    WargaDetailRow(label = "Status Perkawinan", value = warga.statusPerkawinan)
                    WargaDetailRow(label = "Pendidikan Terakhir", value = warga.pendidikan)
                    WargaDetailRow(label = "Pekerjaan", value = warga.pekerjaan)
                    WargaDetailRow(label = "Golongan Darah", value = warga.golonganDarah)
                    WargaDetailRow(label = "Kewarganegaraan", value = warga.kewarganegaraan)
                    WargaDetailRow(label = "Hubungan Keluarga", value = warga.hubunganKeluarga)

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCardClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Detail Utama", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ZOOM DETAIL UTAMA & KELUARGA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (isAdmin || actionMeninggalEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onMeninggalClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Meninggal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LAPORKAN MENINGGAL (WAFAT)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WargaDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WargaFormDialog(
    warga: Warga?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    var nama by remember { mutableStateOf(warga?.nama ?: "") }
    var nik by remember { mutableStateOf(warga?.nik ?: "") }
    var noKk by remember { mutableStateOf(warga?.noKk ?: "") }
    var gender by remember { mutableStateOf(warga?.gender ?: "Laki-laki") }
    var tanggalLahir by remember { mutableStateOf(warga?.tanggalLahir ?: "1990-01-01") }
    var age by remember { mutableStateOf(warga?.umur?.toString() ?: "36") }

    var rt by remember { mutableStateOf(warga?.rt ?: currentUser?.rt?.ifEmpty { "01" } ?: "01") }
    var rw by remember { mutableStateOf(warga?.rw ?: currentUser?.rw?.ifEmpty { "02" } ?: "02") }

    var isDisabilitas by remember { mutableStateOf(warga?.isDisabilitas ?: false) }
    var jenisDisabilitas by remember { mutableStateOf(warga?.jenisDisabilitas ?: "") }

    var pkhSelected by remember { mutableStateOf(warga?.bantuanList?.contains("PKH") ?: false) }
    var bpntSelected by remember { mutableStateOf(warga?.bantuanList?.contains("BPNT") ?: false) }
    var pbijkSelected by remember { mutableStateOf(warga?.bantuanList?.contains("PBI JK") ?: false) }
    var bltddSelected by remember { mutableStateOf(warga?.bantuanList?.contains("BLT DD") ?: false) }
    var bstSelected by remember { mutableStateOf(warga?.bantuanList?.contains("BST") ?: false) }

    // Disdukcapil fields states
    var tempatLahir by remember { mutableStateOf(warga?.tempatLahir ?: "Jakarta") }
    var agama by remember { mutableStateOf(warga?.agama ?: "Islam") }
    var statusPerkawinan by remember { mutableStateOf(warga?.statusPerkawinan ?: "Belum Kawin") }
    var pekerjaan by remember { mutableStateOf(warga?.pekerjaan ?: "Karyawan Swasta") }
    var kewarganegaraan by remember { mutableStateOf(warga?.kewarganegaraan ?: "WNI") }
    var pendidikan by remember { mutableStateOf(warga?.pendidikan ?: "SMA/Sederajat") }
    var golonganDarah by remember { mutableStateOf(warga?.golonganDarah ?: "Tidak Tahu") }
    var hubunganKeluarga by remember { mutableStateOf(warga?.hubunganKeluarga ?: "Anggota Keluarga") }

    // Option Lists
    val agamaOptions = listOf("Islam", "Kristen Protestan", "Kristen Katolik", "Hindu", "Buddha", "Khonghucu", "Penganut Kepercayaan")
    val statusPerkawinanOptions = listOf("Belum Kawin", "Kawin", "Cerai Hidup", "Cerai Mati")
    val pekerjaanOptions = listOf("Belum/Tidak Bekerja", "Ibu Rumah Tangga", "PNS", "TNI", "Polri", "Karyawan Sektor Swasta", "Karyawan BUMN", "Wiraswasta", "Buruh", "Petani/Nelayan", "Pensiunan", "Lainnya")
    val kewarganegaraanOptions = listOf("WNI", "WNA")
    val pendidikanOptions = listOf("Tidak/Belum Sekolah", "SD/Sederajat", "SMP/Sederajat", "SMA/Sederajat", "Diploma I/II/III", "S1/Diploma IV", "S2/S3")
    val golDarahOptions = listOf("A", "B", "AB", "O", "Tidak Tahu")
    val hubunganKeluargaOptions = listOf("Kepala Keluarga", "Suami", "Istri", "Anak", "Orang Tua", "Mertua", "Famili Lain", "Lainnya")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (warga == null) "Tambah Data Warga Baru" else "Ubah Data Warga",
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
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nik,
                        onValueChange = { if (it.length <= 16) nik = it },
                        label = { Text("NIK (16 Digit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = noKk,
                        onValueChange = { if (it.length <= 16) noKk = it },
                        label = { Text("No. KK (16 Digit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gender Radio Row
                Column {
                    Text("Jenis Kelamin", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Laki-laki", onClick = { gender = "Laki-laki" })
                            Text("Laki-laki")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Perempuan", onClick = { gender = "Perempuan" })
                            Text("Perempuan")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempatLahir,
                        onValueChange = { tempatLahir = it },
                        label = { Text("Tempat Lahir") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = tanggalLahir,
                        onValueChange = { tanggalLahir = it },
                        label = { Text("Tgl Lahir (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Umur (Tahun)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    FormDropdownFieldCustom(
                        label = "Gol. Darah",
                        selectedValue = golonganDarah,
                        options = golDarahOptions,
                        onOptionSelected = { golonganDarah = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text("Kependudukan (Standard Disdukcapil)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rt,
                        onValueChange = { rt = it },
                        label = { Text("RT") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = rw,
                        onValueChange = { rw = it },
                        label = { Text("RW") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormDropdownFieldCustom(
                        label = "Agama",
                        selectedValue = agama,
                        options = agamaOptions,
                        onOptionSelected = { agama = it },
                        modifier = Modifier.weight(1f)
                    )

                    FormDropdownFieldCustom(
                        label = "Hub. Keluarga",
                        selectedValue = hubunganKeluarga,
                        options = hubunganKeluargaOptions,
                        onOptionSelected = { hubunganKeluarga = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormDropdownFieldCustom(
                        label = "Status Nikah",
                        selectedValue = statusPerkawinan,
                        options = statusPerkawinanOptions,
                        onOptionSelected = { statusPerkawinan = it },
                        modifier = Modifier.weight(1f)
                    )

                    FormDropdownFieldCustom(
                        label = "Kewarganegaraan",
                        selectedValue = kewarganegaraan,
                        options = kewarganegaraanOptions,
                        onOptionSelected = { kewarganegaraan = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormDropdownFieldCustom(
                        label = "Pendidikan",
                        selectedValue = pendidikan,
                        options = pendidikanOptions,
                        onOptionSelected = { pendidikan = it },
                        modifier = Modifier.weight(1f)
                    )

                    FormDropdownFieldCustom(
                        label = "Pekerjaan",
                        selectedValue = pekerjaan,
                        options = pekerjaanOptions,
                        onOptionSelected = { pekerjaan = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Disabilitas Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDisabilitas,
                        onCheckedChange = { isDisabilitas = it }
                    )
                    Text("Penyandang Disabilitas")
                }

                AnimatedVisibility(visible = isDisabilitas) {
                    OutlinedTextField(
                        value = jenisDisabilitas,
                        onValueChange = { jenisDisabilitas = it },
                        label = { Text("Jenis Disabilitas (misal: Tunawicara)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Program Penerima Bantuan Sosial Section
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "🎁 Program Penerima Bantuan Sosial",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pkhSelected, onCheckedChange = { pkhSelected = it })
                    Text("PKH (Program Keluarga Harapan)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bpntSelected, onCheckedChange = { bpntSelected = it })
                    Text("BPNT (Bantuan Pangan Non Tunai)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pbijkSelected, onCheckedChange = { pbijkSelected = it })
                    Text("PBI JK (Penerima Bantuan Iuran Jaminan Kesehatan)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bltddSelected, onCheckedChange = { bltddSelected = it })
                    Text("BLT DD (Bantuan Langsung Tunai Dana Desa)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bstSelected, onCheckedChange = { bstSelected = it })
                    Text("BST (Bantuan Sosial Tunai)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotEmpty() && nik.isNotEmpty() && noKk.isNotEmpty()) {
                        val activeBantuan = mutableListOf<String>()
                        if (pkhSelected) activeBantuan.add("PKH")
                        if (bpntSelected) activeBantuan.add("BPNT")
                        if (pbijkSelected) activeBantuan.add("PBI JK")
                        if (bltddSelected) activeBantuan.add("BLT DD")
                        if (bstSelected) activeBantuan.add("BST")
                        val completedBantuanList = activeBantuan.joinToString(", ")

                        viewModel.saveWarga(
                            id = warga?.id ?: 0,
                            nama = nama,
                            nik = nik,
                            noKk = noKk,
                            gender = gender,
                            tanggalLahir = tanggalLahir,
                            rt = rt,
                            rw = rw,
                            umur = age.toIntOrNull() ?: 30,
                            isDisabilitas = isDisabilitas,
                            jenisDisabilitas = if (isDisabilitas) jenisDisabilitas else "",
                            status = warga?.status ?: "Aktif",
                            tempatLahir = tempatLahir,
                            agama = agama,
                            statusPerkawinan = statusPerkawinan,
                            pekerjaan = pekerjaan,
                            kewarganegaraan = kewarganegaraan,
                            pendidikan = pendidikan,
                            golonganDarah = golonganDarah,
                            hubunganKeluarga = hubunganKeluarga,
                            bantuanList = completedBantuanList,
                            onCompleted = onDismiss
                        )
                    }
                },
                enabled = nama.isNotEmpty() && nik.isNotEmpty() && noKk.isNotEmpty()
            ) {
                Text("SIMPAN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
}

@Composable
fun FormDropdownFieldCustom(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.clickable { expanded = true }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = Color.Transparent
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(180.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ImportExportDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var importText by remember { mutableStateOf("") }
    var operationResult by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }
    var tabSelected by remember { mutableStateOf(0) } // 0 -> Export, 1 -> Import

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ImportExport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import/Export Excel RT-RW", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Format kependudukan disesuaikan dengan template standar Disdukcapil untuk pertukaran data antar tingkat RT, RW, dan Kelurahan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tab selectors inside dialog
                TabRow(selectedTabIndex = tabSelected) {
                    Tab(selected = tabSelected == 0, onClick = { tabSelected = 0 }) {
                        Text("📤 Export", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = tabSelected == 1, onClick = { tabSelected = 1 }) {
                        Text("📥 Import", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                }

                if (tabSelected == 0) {
                    // Export Section
                    val csvString = remember { viewModel.exportFilteredWargaToCsv() }
                    
                    Text(
                        text = "Data warga Anda telah diexport ke format CSV/Excel standar Disdukcapil.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = csvString,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Output CSV (Format Excel)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(csvString))
                            operationResult = "Data berhasil disalin ke Clipboard! Hubungkan ke file .csv atau .xlsx untuk Microsoft Excel."
                            isSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salin Data CSV (Untuk Excel)")
                    }

                    Button(
                        onClick = {
                            try {
                                val fileName = "backup_sensus_warga_${System.currentTimeMillis()}.csv"
                                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                val file = java.io.File(dir, fileName)
                                file.writeText(csvString)
                                operationResult = "Pencadangan BERHASIL!\nFile lokal disimpan di:\n${file.absolutePath}"
                                isSuccess = true
                            } catch (e: Exception) {
                                operationResult = "Gagal mencadangkan ke file lokal: ${e.message}"
                                isSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save file")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Cadangan ke File Lokal (.csv)")
                    }
                } else {
                    // Import Section
                    Text(
                        text = "Tempel data kependudukan berformat CSV di sini. Tiap kolom dipisahkan koma (,). Pastikan urutan baris sesuai template.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Template baris:\nNIK,No_KK,Nama,Gender,Tempat_Lahir,Tanggal_Lahir,Umur,Agama,Status_Nikah,Pekerjaan,Kewarganegaraan,Pendidikan,Gol_Darah,Hub_Keluarga,RT,RW,Disabilitas_Ya_Tidak,Jenis_Disabilitas,Status",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .padding(8.dp)
                    )

                    Button(
                        onClick = {
                            val templateSample = "3275012211930002,3275010111190002,Siti Aminah,Perempuan,Jakarta,1993-11-22,32,Islam,Kawin,Ibu Rumah Tangga,WNI,SMA/Sederajat,A,Istri,01,02,Tidak,,Aktif"
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(templateSample))
                            importText = templateSample
                            operationResult = "Contoh data disalin ke editor!"
                            isSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📋 Salin Contoh Baris Template", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("Tempel data kependudukan CSV di sini...") },
                        label = { Text("Pasted CSV Resident Data") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    Button(
                        onClick = {
                            if (importText.trim().isEmpty()) {
                                operationResult = "Isian data masih kosong!"
                                isSuccess = false
                            } else {
                                viewModel.importWargaFromCsv(importText) { count, err ->
                                    if (err != null) {
                                        operationResult = "Hasil: Sukses $count warga. Hubungi info error: $err"
                                        isSuccess = count > 0
                                    } else {
                                        operationResult = "Sukses! Berhasil memasukkan/mengupdate $count data warga ke database."
                                        isSuccess = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Upload")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mulai Import Sekarang")
                    }
                }

                if (operationResult.isNotEmpty()) {
                    Surface(
                        color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = operationResult,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WargaDetailDialog(
    warga: Warga,
    viewModel: MainViewModel,
    isAdmin: Boolean,
    actionEditEnabled: Boolean,
    actionHapusEnabled: Boolean,
    actionMeninggalEnabled: Boolean,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMeninggalClick: () -> Unit
) {
    val wargaList by viewModel.filteredWargaList.collectAsState()
    
    // Find all family members with same No KK
    val familyMembers = remember(wargaList, warga.noKk) {
        wargaList.filter { it.noKk == warga.noKk }
    }

    // AID helper state (reactive in dialog)
    var pkhSelected by remember { mutableStateOf(warga.bantuanList.contains("PKH")) }
    var bpntSelected by remember { mutableStateOf(warga.bantuanList.contains("BPNT")) }
    var pbijkSelected by remember { mutableStateOf(warga.bantuanList.contains("PBI JK")) }
    var bltddSelected by remember { mutableStateOf(warga.bantuanList.contains("BLT DD")) }
    var bstSelected by remember { mutableStateOf(warga.bantuanList.contains("BST")) }

    // Helper to update bantuan in database on toggles
    val updateBantuanOnDb = { label: String, isSelected: Boolean ->
        val currentList = mutableListOf<String>()
        if (if (label == "PKH") isSelected else pkhSelected) currentList.add("PKH")
        if (if (label == "BPNT") isSelected else bpntSelected) currentList.add("BPNT")
        if (if (label == "PBI JK") isSelected else pbijkSelected) currentList.add("PBI JK")
        if (if (label == "BLT DD") isSelected else bltddSelected) currentList.add("BLT DD")
        if (if (label == "BST") isSelected else bstSelected) currentList.add("BST")
        val joined = currentList.joinToString(", ")
        viewModel.saveWarga(
            id = warga.id,
            nama = warga.nama,
            nik = warga.nik,
            noKk = warga.noKk,
            gender = warga.gender,
            tanggalLahir = warga.tanggalLahir,
            rt = warga.rt,
            rw = warga.rw,
            umur = warga.umur,
            isDisabilitas = warga.isDisabilitas,
            jenisDisabilitas = warga.jenisDisabilitas,
            status = warga.status,
            tempatLahir = warga.tempatLahir,
            agama = warga.agama,
            statusPerkawinan = warga.statusPerkawinan,
            pekerjaan = warga.pekerjaan,
            kewarganegaraan = warga.kewarganegaraan,
            pendidikan = warga.pendidikan,
            golonganDarah = warga.golonganDarah,
            hubunganKeluarga = warga.hubunganKeluarga,
            bantuanList = joined,
            onCompleted = {}
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detail Lengkap Warga",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Details")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profil Warga
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = warga.nama.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = warga.nama,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Status Kebangsaan: ${warga.kewarganegaraan}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Biodata Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📌 INFORMASI BIODATA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    WargaDetailRow(label = "NIK (Nomor Induk Kependudukan)", value = warga.nik)
                    WargaDetailRow(label = "Nomor Kartu Keluarga (KK)", value = warga.noKk)
                    WargaDetailRow(label = "Hubungan Keluarga", value = warga.hubunganKeluarga)
                    WargaDetailRow(label = "Tempat, Tanggal Lahir", value = "${warga.tempatLahir}, ${warga.tanggalLahir}")
                    WargaDetailRow(label = "Umur", value = "${warga.umur} Tahun")
                    WargaDetailRow(label = "Jenis Kelamin", value = warga.gender)
                    WargaDetailRow(label = "Alamat SIKD", value = "RT ${warga.rt} / RW ${warga.rw}")
                    WargaDetailRow(label = "Agama", value = warga.agama)
                    WargaDetailRow(label = "Status Perkawinan", value = warga.statusPerkawinan)
                    WargaDetailRow(label = "Pekerjaan", value = warga.pekerjaan)
                    WargaDetailRow(label = "Pendidikan Terakhir", value = warga.pendidikan)
                    WargaDetailRow(label = "Golongan Darah", value = warga.golonganDarah)
                    if (warga.isDisabilitas) {
                        WargaDetailRow(label = "Penyandang Disabilitas", value = "Ya (Jenis: ${warga.jenisDisabilitas})")
                    } else {
                        WargaDetailRow(label = "Penyandang Disabilitas", value = "Tidak")
                    }
                }

                // Family Members (Same No KK)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "👨‍👩‍👧‍👦 ANGGOTA KELUARGA (KK: ${warga.noKk})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    
                    if (familyMembers.size <= 1) {
                        Text(
                            text = "Tidak ada anggota keluarga lain yang terdaftar di database local dengan nomor KK ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                familyMembers.forEach { member ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = member.nama,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (member.id == warga.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (member.id == warga.id) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            "Ini",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "NIK: ${member.nik}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = member.hubunganKeluarga,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "${member.umur} Thn",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (familyMembers.last() != member) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }

                // AID (Program Penerima Bantuan) Checkboxes & Action Toggle buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎁 STATUS PENERIMA BANTUAN SOSIAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    Text(
                        text = "Aktifkan atau nonaktifkan program bantuan sosial yang diterima oleh warga ini dengan menyentuh tombol di bawah:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BantuanSelectChip(
                                    label = "PKH",
                                    description = "Prog Keluarga Harapan",
                                    selected = pkhSelected,
                                    onClick = {
                                        val newVal = !pkhSelected
                                        pkhSelected = newVal
                                        updateBantuanOnDb("PKH", newVal)
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BantuanSelectChip(
                                    label = "BPNT",
                                    description = "Pangan Non-Tunai",
                                    selected = bpntSelected,
                                    onClick = {
                                        val newVal = !bpntSelected
                                        bpntSelected = newVal
                                        updateBantuanOnDb("BPNT", newVal)
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BantuanSelectChip(
                                    label = "PBI JK",
                                    description = "Jaminan Kesehatan",
                                    selected = pbijkSelected,
                                    onClick = {
                                        val newVal = !pbijkSelected
                                        pbijkSelected = newVal
                                        updateBantuanOnDb("PBI JK", newVal)
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BantuanSelectChip(
                                    label = "BLT DD",
                                    description = "BLT Dana Desa",
                                    selected = bltddSelected,
                                    onClick = {
                                        val newVal = !bltddSelected
                                        bltddSelected = newVal
                                        updateBantuanOnDb("BLT DD", newVal)
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BantuanSelectChip(
                                    label = "BST",
                                    description = "Bantuan Sos Tunai",
                                    selected = bstSelected,
                                    onClick = {
                                        val newVal = !bstSelected
                                        bstSelected = newVal
                                        updateBantuanOnDb("BST", newVal)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAdmin || actionMeninggalEnabled) {
                    Button(
                        onClick = onMeninggalClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Meninggal", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MENINGGAL")
                    }
                }
                if (isAdmin || actionEditEnabled) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UBAH")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("KEMBALI")
            }
        }
    )
}

@Composable
fun BantuanSelectChip(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
