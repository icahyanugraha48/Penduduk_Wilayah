package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLog
import com.example.data.model.DatabaseBackup
import com.example.data.model.UserAccount
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(
    viewModel: MainViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accountsList.collectAsState()
    val logs by viewModel.logsList.collectAsState()
    val backups by viewModel.backupsList.collectAsState()
    val backupIntervalState by viewModel.backupInterval.collectAsState()

    var activeSubMenu by remember { mutableStateOf(0) } // 0 -> Akun RT/RW, 1 -> Log Aktivitas, 2 -> Backup Database, 3 -> Google Sheets Cloud Sync, 4 -> Akses (Admin Only), 5 -> WhatsApp Gateway

    val isRegionalHead = currentUser?.role == "Kepala Wilayah"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pengaturan_screen")
    ) {
        // Upper Tab selection card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Akun
                Button(
                    onClick = { activeSubMenu = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubMenu == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeSubMenu == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🔑 Akun", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Tab 1: Logs
                Button(
                    onClick = { activeSubMenu = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubMenu == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeSubMenu == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("📜 Log", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Tab 2: Backups
                Button(
                    onClick = { activeSubMenu = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubMenu == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeSubMenu == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("💾 Backup", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Tab 3: Cloud Sync
                Button(
                    onClick = { activeSubMenu = 3 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubMenu == 3) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeSubMenu == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("☁️ Cloud", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Tab 4: Hak Akses (Admin Only)
                if (isRegionalHead) {
                    Button(
                        onClick = { activeSubMenu = 4 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSubMenu == 4) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (activeSubMenu == 4) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("🛡️ Akses", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Tab 5: WhatsApp Gateway (Admin / Operators)
                Button(
                    onClick = { activeSubMenu = 5 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubMenu == 5) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeSubMenu == 5) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("📞 WhatsApp API", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        // Submenu display switcher
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubMenu) {
                0 -> AccountManagementView(
                    accounts = accounts.filter { it.username != currentUser?.username },
                    isRegionalHead = isRegionalHead,
                    onToggleAccess = { username, isEnabled ->
                        viewModel.toggleAccountAccess(username, isEnabled)
                    }
                )
                1 -> LogActivityView(logs = logs)
                2 -> BackupDatabaseView(
                    backups = backups,
                    backupInterval = backupIntervalState,
                    onRunBackup = { viewModel.triggerDatabaseBackup() },
                    onSetInterval = { interval -> viewModel.setBackupInterval(interval) }
                )
                3 -> CloudSyncView(viewModel = viewModel)
                4 -> if (isRegionalHead) PermissionsManagementView(viewModel = viewModel)
                5 -> WhatsAppGatewayView(viewModel = viewModel)
            }
        }
    }
}

// Submenu 0: Account access management
@Composable
fun AccountManagementView(
    accounts: List<UserAccount>,
    isRegionalHead: Boolean,
    onToggleAccess: (String, Boolean) -> Unit
) {
    if (!isRegionalHead) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Akses Terbatas",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AKSES KHUSUS ADMIN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Daftar manajemen blokir & batasan akun RT/RW hanya dapat diakses oleh Kepala Wilayah (Admin).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Membatasi Hak Akses Akun RT & RW",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Username: @${acc.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                // Scope text
                                val scopeText = if (acc.role == "RT") "Unit RT ${acc.rt}/RW ${acc.rw}" else "Unit RW ${acc.rw}"
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$scopeText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Switch(
                                    checked = acc.isEnabled,
                                    onCheckedChange = { onToggleAccess(acc.username, it) }
                                )
                                Text(
                                    text = if (acc.isEnabled) "Aktif" else "Diblokir",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (acc.isEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Submenu 1: Activity log view
@Composable
fun LogActivityView(logs: List<ActivityLog>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Log Aktivitas Sistem Kependudukan",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log masih kosong.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    ActivityLogItem(log)
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: ActivityLog) {
    val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = log.role.take(1),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BY: ${log.username} (${log.role})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Submenu 2: Backup view database
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupDatabaseView(
    backups: List<DatabaseBackup>,
    backupInterval: String,
    onRunBackup: () -> Unit,
    onSetInterval: (String) -> Unit
) {
    var showIntervalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pengelolaan Backup Database",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        // Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Otomatisasi Backup Berkala",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pastikan data kependudukan aman dengan mengatur trigger backup berkala otomatis (SQLite ke SQL dump).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Jadwal Pencadangan:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = backupInterval,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = { showIntervalDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Ubah Jadwal", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Run backup trigger button
        Button(
            onClick = onRunBackup,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp) // Sleek rounded-2xl
        ) {
            Icon(Icons.Default.Backup, contentDescription = "Run backup now")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mulai Pencadangan Sekarang (Manual)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }

        // backup list history
        Text(
            text = "Riwayat File Backup Database (${backups.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (backups.isEmpty()) {
            Text(
                "Belum ada riwayat backup database.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            backups.forEach { bk ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(bk.timestamp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp), // Sleek rounded-3xl
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = bk.backupName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "Waktu pencadangan: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "SUKSES",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${bk.sizeInKb} KB",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // Interval Selector Dialog
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("Jadwal Pencadangan database") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val intervals = listOf("Setiap Hari (Otomatis)", "Mingguan (Otomatis)", "Bulanan (Otomatis)", "Manual Saja")
                    intervals.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetInterval(opt)
                                    showIntervalDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = backupInterval == opt, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(opt)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncView(viewModel: MainViewModel) {
    val googleSheetsUrl by viewModel.googleSheetsUrl.collectAsState()
    val syncLoading by viewModel.syncLoading.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var urlInput by remember(googleSheetsUrl) { mutableStateOf(googleSheetsUrl) }
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val appsScriptSource = """
// Script untuk Sinkronisasi SIKD RT-RW (Cloud Google Sheets)
function doGet(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var rows = sheet.getDataRange().getValues();
  if (rows.length < 2) {
    return ContentService.createTextOutput(JSON.stringify([])).setMimeType(ContentService.MimeType.JSON);
  }
  var headers = rows[0];
  var data = [];
  for (var i = 1; i < rows.length; i++) {
    var row = rows[i];
    var warga = {};
    for (var j = 0; j < headers.length; j++) {
      warga[headers[j]] = row[j];
    }
    data.push(warga);
  }
  return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  try {
    var body = JSON.parse(e.postData.contents);
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var rows = sheet.getDataRange().getValues();
    var headers = [
      "nik", "noKk", "nama", "gender", "tanggalLahir", "rt", "rw", 
      "umur", "isDisabilitas", "jenisDisabilitas", "status", "tempatLahir", 
      "agama", "statusPerkawinan", "pekerjaan", "kewarganegaraan", 
      "pendidikan", "golonganDarah", "hubunganKeluarga"
    ];
    
    if (rows.length === 0 || rows[0].length === 0 || rows[0][0] === "") {
      sheet.appendRow(headers);
      rows = [headers];
    }
    
    var sheetHeaders = rows[0];
    var nikColIndex = sheetHeaders.indexOf("nik");
    if (nikColIndex === -1) nikColIndex = 0;
    
    var nikRowMap = {};
    for (var i = 1; i < rows.length; i++) {
      var currentNik = rows[i][nikColIndex];
      if (currentNik) {
        nikRowMap[String(currentNik).trim()] = i + 1;
      }
    }
    
    var inserted = 0;
    var updated = 0;
    
    for (var i = 0; i < body.length; i++) {
      var item = body[i];
      var nik = String(item.nik).trim();
      var rowData = [];
      for (var j = 0; j < headers.length; j++) {
        var key = headers[j];
        var val = item[key];
        if (key === "isDisabilitas") {
          rowData.push(val === true || val === "true" || val === "Ya" ? "Ya" : "Tidak");
        } else {
          rowData.push(val !== undefined ? val : "");
        }
      }
      
      var existingRowIndex = nikRowMap[nik];
      if (existingRowIndex) {
        var range = sheet.getRange(existingRowIndex, 1, 1, headers.length);
        range.setValues([rowData]);
        updated++;
      } else {
        sheet.appendRow(rowData);
        inserted++;
      }
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success", 
      inserted: inserted, 
      updated: updated,
      total: body.length
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error", 
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sinkronisasi Cloud (Google Sheets)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        // Cloud sync explanation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hubungkan SIKD ke Google Spreadsheet",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gunakan integrasi Cloud Google Sheets untuk melakukan backup data kependudukan secara real-time ke awan. Kepala Wilayah (Admin) dapat memantau data yang diunggah oleh RT & RW di satu spreadsheet terpusat secara transparan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Configuration Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Konfigurasi URL Google Apps Script Web App",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("g_sheets_url_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        viewModel.saveGoogleSheetsUrl(urlInput)
                        viewModel.clearSyncMessage()
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan URL", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Action Status Feedback
        syncMessage?.let { msg ->
            val isError = msg.startsWith("Error", ignoreCase = true) || msg.startsWith("Gagal", ignoreCase = true)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color(0xFF81C784)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = "Status",
                        tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF1B5E20),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearSyncMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (syncLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("Sedang memproses data...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Pull and Push Actions Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Push upload button
            Button(
                onClick = { viewModel.pushToGoogleSheets() },
                modifier = Modifier.weight(1f).height(54.dp),
                enabled = !syncLoading && googleSheetsUrl.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload")
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Upload Cloud", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text("(Kirim Ke Sheets)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                }
            }

            // Pull download/merge button
            Button(
                onClick = { viewModel.pullFromGoogleSheets() },
                modifier = Modifier.weight(1f).height(54.dp),
                enabled = !syncLoading && googleSheetsUrl.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download")
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Unduh Cloud", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text("(Ambil Dari Sheets)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Step-by-Step setup Instructions
        HorizontalDivider()

        Text(
            text = "🛠️ Cara Membuat & Menghubungkan Google Sheet",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val steps = listOf(
                "1. Buat Google Spreadsheet baru di Google Drive Anda.",
                "2. Klik menu di bagian atas: Ekstensi > Apps Script.",
                "3. Hapus semua kode bawaan di dalam editor Apps Script.",
                "4. Salin kode Apps Script resmi di bawah menggunakan tombol Salin Kode Apps Script.",
                "5. Tempel (Paste) kode tersebut ke dalam editor Apps Script.",
                "6. Klik tombol Terapkan > Penerapan baru (Deploy > New Deployment).",
                "7. Klik ikon Roda Gigi (Jenis penerapan) lalu pilih Aplikasi Web (Web App).",
                "8. Set bagian Siapa yang memiliki akses (Who has access) menjadi: Siapa saja (Anyone).",
                "9. Klik Terapkan (Deploy). Berikan izin akses Google jika diminta.",
                "10. Salin URL Aplikasi Web yang ditampilkan, lalu tempelkan ke kolom input di atas dan klik Simpan URL!"
            )

            steps.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Copy Code Button
        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(appsScriptSource))
                isCopied = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Script")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isCopied) "Berhasil Disalin! ✓" else "📋 Salin Kode Apps Script",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionsManagementView(viewModel: MainViewModel) {
    val menuMutasiEnabled by viewModel.menuMutasiEnabled.collectAsState()
    val menuPelaporanEnabled by viewModel.menuPelaporanEnabled.collectAsState()
    val menuPengumumanEnabled by viewModel.menuPengumumanEnabled.collectAsState()

    val actionTambahEnabled by viewModel.actionTambahEnabled.collectAsState()
    val actionEditEnabled by viewModel.actionEditEnabled.collectAsState()
    val actionHapusEnabled by viewModel.actionHapusEnabled.collectAsState()
    val actionMeninggalEnabled by viewModel.actionMeninggalEnabled.collectAsState()
    val actionImportExportEnabled by viewModel.actionImportExportEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pengaturan Hak Akses RT / RW",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Sebagai Kepala Wilayah (Admin), Anda dapat mengatur menu dan aksi operasional apa saja yang aktif dan dapat dijalankan oleh akun pengurus RT dan RW demi keamanan data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Section 1: Visibility of Menus
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "🖥️ Visibilitas Menu Utama",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()

                PermissionToggleRow(
                    title = "Menu Mutasi Real-time",
                    description = "Menampilkan riwayat dan log mutasi kependudukan di tab navigasi bawah",
                    checked = menuMutasiEnabled,
                    onCheckedChange = { viewModel.updatePermission("menu_mutasi", it) }
                )

                PermissionToggleRow(
                    title = "Menu Laporan Kependudukan",
                    description = "Menampilkan fitur pembuatan laporan mutasi baru",
                    checked = menuPelaporanEnabled,
                    onCheckedChange = { viewModel.updatePermission("menu_pelaporan", it) }
                )

                PermissionToggleRow(
                    title = "Menu Undangan & Broadcast",
                    description = "Menampilkan fitur pengiriman undangan WhatsApp",
                    checked = menuPengumumanEnabled,
                    onCheckedChange = { viewModel.updatePermission("menu_pengumuman", it) }
                )
            }
        }

        // Section 2: Action permissions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "⚙️ Izin Tindakan Data Warga",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()

                PermissionToggleRow(
                    title = "Tambah Warga Baru",
                    description = "Mengizinkan RT/RW mendaftarkan data warga baru langsung di aplikasi",
                    checked = actionTambahEnabled,
                    onCheckedChange = { viewModel.updatePermission("action_tambah", it) }
                )

                PermissionToggleRow(
                    title = "Ubah Data Warga",
                    description = "Mengizinkan RT/RW mengedit biodata/informasi warga yang sudah ada",
                    checked = actionEditEnabled,
                    onCheckedChange = { viewModel.updatePermission("action_edit", it) }
                )

                PermissionToggleRow(
                    title = "Hapus Data Warga",
                    description = "Mengizinkan RT/RW menghapus baris warga dari database lokal",
                    checked = actionHapusEnabled,
                    onCheckedChange = { viewModel.updatePermission("action_hapus", it) }
                )

                PermissionToggleRow(
                    title = "Aksi Meninggal",
                    description = "Mengizinkan RT/RW menandai status warga menjadi 'Meninggal' di daftar warga",
                    checked = actionMeninggalEnabled,
                    onCheckedChange = { viewModel.updatePermission("action_meninggal", it) }
                )

                PermissionToggleRow(
                    title = "Import & Export CSV",
                    description = "Mengizinkan RT/RW mendownload database ke format excel atau mengunggah CSV massal",
                    checked = actionImportExportEnabled,
                    onCheckedChange = { viewModel.updatePermission("action_import_export", it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PermissionToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun WhatsAppGatewayView(viewModel: MainViewModel) {
    val whatsappUrl by viewModel.whatsappUrl.collectAsState()
    val whatsappToken by viewModel.whatsappToken.collectAsState()
    val whatsappContacts by viewModel.whatsappContacts.collectAsState()

    var urlInput by remember { mutableStateOf(whatsappUrl) }
    var tokenInput by remember { mutableStateOf(whatsappToken) }
    var contactsInput by remember { mutableStateOf(whatsappContacts) }

    var testRecipient by remember { mutableStateOf("") }
    var testMessage by remember { mutableStateOf("Halo, ini adalah pesan tes integrasi WhatsApp Gateway SIKD!") }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isSendingTest by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Konfigurasi WhatsApp Gateway",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Otomatisasi Kirim Notifikasi via WhatsApp",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gunakan gateway API pihak ketiga (seperti Whacenter, Wablas, Fondeye, dll) untuk menyiarkan info pengumuman kependudukan kelurahan secara otomatis kepada RT/RW.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Form Kredensial API Gateway",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("API Gateway Endpoint URL") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("API token / Auth Key") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = contactsInput,
                    onValueChange = { contactsInput = it },
                    label = { Text("Kontak Target RT/RW (Format: Nama:Nomor, dipisah koma)") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        viewModel.saveWhatsappSettings(urlInput, tokenInput, contactsInput)
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Konfigurasi", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🧪 Sandbox / Tes API Gateway",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = testRecipient,
                    onValueChange = { testRecipient = it },
                    placeholder = { Text("08123456789") },
                    label = { Text("Nomor WhatsApp Penerima Tes") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = testMessage,
                    onValueChange = { testMessage = it },
                    label = { Text("Isi Pesan Tes") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (testRecipient.isNotEmpty()) {
                            isSendingTest = true
                            testStatus = "Menghubungi endpoint..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val urlObject = java.net.URL(urlInput)
                                    val connection = urlObject.openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "POST"
                                    connection.connectTimeout = 4000
                                    connection.readTimeout = 4000
                                    connection.doOutput = true
                                    connection.setRequestProperty("Content-Type", "application/json")
                                    connection.setRequestProperty("Authorization", "Bearer $tokenInput")

                                    val payload = """
                                        {
                                          "device_id": "SIKD-SANDBOX-TEST",
                                          "recipient": "$testRecipient",
                                          "message": "$testMessage",
                                          "api_key": "$tokenInput"
                                        }
                                    """.trimIndent()

                                    connection.outputStream.use { os ->
                                        os.write(payload.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                                    }
                                    val responseCode = connection.responseCode
                                    if (responseCode in 200..299) {
                                        testStatus = "SUKSES! Gateway merespon status kode $responseCode."
                                    } else {
                                        testStatus = "GAGAL! Gateway merespon status kode $responseCode."
                                    }
                                    connection.disconnect()
                                } catch (e: Exception) {
                                    testStatus = "ERROR: ${e.message}"
                                } finally {
                                    isSendingTest = false
                                }
                            }
                        }
                    },
                    enabled = testRecipient.isNotEmpty() && !isSendingTest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isSendingTest) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send Test")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("KIRIM PESAN TES VIA API", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                testStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (status.startsWith("SUKSES")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
