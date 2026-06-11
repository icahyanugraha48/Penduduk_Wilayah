package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.CitizenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppNotification(
    val id: Long = System.currentTimeMillis() + (1..1000).random(),
    val title: String,
    val message: String,
    val timestamp: String = "Baru Saja"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository: CitizenRepository

    init {
        repository = CitizenRepository(
            db.wargaDao(),
            db.mutasiDao(),
            db.accountDao(),
            db.logDao(),
            db.backupDao(),
            db.announcementDao()
        )
    }

    // Auth States
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // Permissions State for RT/RW roles controlled by Admin (Kepala Wilayah)
    private val prefs = application.getSharedPreferences("rt_rw_permissions", Context.MODE_PRIVATE)

    private val _menuMutasiEnabled = MutableStateFlow(prefs.getBoolean("menu_mutasi", true))
    val menuMutasiEnabled = _menuMutasiEnabled.asStateFlow()

    private val _menuPelaporanEnabled = MutableStateFlow(prefs.getBoolean("menu_pelaporan", true))
    val menuPelaporanEnabled = _menuPelaporanEnabled.asStateFlow()

    private val _menuPengumumanEnabled = MutableStateFlow(prefs.getBoolean("menu_pengumuman", true))
    val menuPengumumanEnabled = _menuPengumumanEnabled.asStateFlow()

    private val _actionTambahEnabled = MutableStateFlow(prefs.getBoolean("action_tambah", true))
    val actionTambahEnabled = _actionTambahEnabled.asStateFlow()

    private val _actionEditEnabled = MutableStateFlow(prefs.getBoolean("action_edit", true))
    val actionEditEnabled = _actionEditEnabled.asStateFlow()

    private val _actionHapusEnabled = MutableStateFlow(prefs.getBoolean("action_hapus", true))
    val actionHapusEnabled = _actionHapusEnabled.asStateFlow()

    private val _actionMeninggalEnabled = MutableStateFlow(prefs.getBoolean("action_meninggal", true))
    val actionMeninggalEnabled = _actionMeninggalEnabled.asStateFlow()

    private val _actionImportExportEnabled = MutableStateFlow(prefs.getBoolean("action_import_export", true))
    val actionImportExportEnabled = _actionImportExportEnabled.asStateFlow()

    fun updatePermission(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
        when (key) {
            "menu_mutasi" -> _menuMutasiEnabled.value = enabled
            "menu_pelaporan" -> _menuPelaporanEnabled.value = enabled
            "menu_pengumuman" -> _menuPengumumanEnabled.value = enabled
            "action_tambah" -> _actionTambahEnabled.value = enabled
            "action_edit" -> _actionEditEnabled.value = enabled
            "action_hapus" -> _actionHapusEnabled.value = enabled
            "action_meninggal" -> _actionMeninggalEnabled.value = enabled
            "action_import_export" -> _actionImportExportEnabled.value = enabled
        }
    }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Real-Time Notification State
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Core Data Flows
    val wargaList: StateFlow<List<Warga>> = repository.allWarga
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredWargaList: StateFlow<List<Warga>> = combine(wargaList, currentUser) { list, user ->
        if (user == null) {
            emptyList()
        } else {
            when (user.role) {
                "Kepala Wilayah" -> list
                "RW" -> list.filter { it.rw.trim() == user.rw.trim() }
                "RT" -> list.filter { it.rt.trim() == user.rt.trim() && it.rw.trim() == user.rw.trim() }
                else -> list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mutasiList: StateFlow<List<MutasiWarga>> = repository.allMutasi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountsList: StateFlow<List<UserAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logsList: StateFlow<List<ActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backupsList: StateFlow<List<DatabaseBackup>> = repository.allBackups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcementsList: StateFlow<List<Announcement>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback State
    private val _operationStatus = MutableSharedFlow<String>()
    val operationStatus: SharedFlow<String> = _operationStatus.asSharedFlow()

    // Backup Schedule Settings Form State
    private val _backupInterval = MutableStateFlow("Mingguan (Otomatis)")
    val backupInterval: StateFlow<String> = _backupInterval.asStateFlow()

    fun setBackupInterval(interval: String) {
        _backupInterval.value = interval
        viewModelScope.launch {
            repository.insertLog(
                _currentUser.value?.username ?: "system",
                _currentUser.value?.role ?: "System",
                "Mengubah penjadwalan backup database menjadi: $interval"
            )
            _operationStatus.emit("Jadwal backup updated ke: $interval")
        }
    }

    // Google Sheets Cloud Sync State
    private val sharedPrefs = application.getSharedPreferences("rt_rw_settings", Context.MODE_PRIVATE)
    private val _googleSheetsUrl = MutableStateFlow(sharedPrefs.getString("google_sheets_url", "") ?: "")
    val googleSheetsUrl: StateFlow<String> = _googleSheetsUrl.asStateFlow()

    private val _syncLoading = MutableStateFlow(false)
    val syncLoading: StateFlow<Boolean> = _syncLoading.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun saveGoogleSheetsUrl(url: String) {
        _googleSheetsUrl.value = url
        sharedPrefs.edit().putString("google_sheets_url", url).apply()
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Third-Party WhatsApp Gateway Configuration State
    private val _whatsappUrl = MutableStateFlow(sharedPrefs.getString("whatsapp_url", "https://api.gateway-wa.id/v1/messages") ?: "https://api.gateway-wa.id/v1/messages")
    val whatsappUrl: StateFlow<String> = _whatsappUrl.asStateFlow()

    private val _whatsappToken = MutableStateFlow(sharedPrefs.getString("whatsapp_token", "TOKEN_SIKD_DEMO_911") ?: "TOKEN_SIKD_DEMO_911")
    val whatsappToken: StateFlow<String> = _whatsappToken.asStateFlow()

    private val _whatsappContacts = MutableStateFlow(sharedPrefs.getString("whatsapp_contacts", "RT01:081234567890,RT02:082134567891,RW01:085634567892,RW02:081934567893") ?: "RT01:081234567890,RT02:082134567891,RW01:085634567892,RW02:081934567893")
    val whatsappContacts: StateFlow<String> = _whatsappContacts.asStateFlow()

    fun saveWhatsappSettings(url: String, token: String, contacts: String) {
        _whatsappUrl.value = url
        _whatsappToken.value = token
        _whatsappContacts.value = contacts
        sharedPrefs.edit()
            .putString("whatsapp_url", url)
            .putString("whatsapp_token", token)
            .putString("whatsapp_contacts", contacts)
            .apply()
    }

    // Modern Third-Party WhatsApp Gateway API Automated Dispatch
    fun sendThirdPartyWhatsAppNotification(title: String, messageText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val endpoint = _whatsappUrl.value
            val token = _whatsappToken.value
            val contactsString = _whatsappContacts.value

            val contactsMap = contactsString.split(",")
                .mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                }.toMap()

            if (contactsMap.isEmpty()) {
                addNotification("WhatsApp Gateway Error", "Gagal auto-kirim: Kontak RT/RW kosong")
                return@launch
            }

            contactsMap.forEach { (roleOrName, phoneNumber) ->
                try {
                    val urlObject = java.net.URL(endpoint)
                    val connection = urlObject.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $token")

                    val payload = """
                        {
                          "device_id": "SIKD-GATEWAY-RT-RW",
                          "recipient": "$phoneNumber",
                          "message": "📢 *PENGUMUMAN RESMI RT/RW* 📢\n\nKepada Pengurus $roleOrName,\nBerikut adalah pengumuman penting:\n\n*${title.uppercase()}*\n\n$messageText\n\n_Disiarkan otomatis via SIKD Village App_",
                          "api_key": "$token"
                        }
                    """.trimIndent()

                    connection.outputStream.use { os ->
                        os.write(payload.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                    }
                    val responseCode = connection.responseCode
                    if (responseCode in 200..299) {
                        repository.insertLog("system", "WhatsApp Broadcast", "SUKSES kirim notifikasi otomatis WA ke $roleOrName ($phoneNumber)")
                    } else {
                        repository.insertLog("system", "WhatsApp Broadcast", "GAGAL kirim ke $roleOrName ($phoneNumber) - HTTP status $responseCode")
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    repository.insertLog("system", "WhatsApp Broadcast", "ERROR Gateway untuk $roleOrName: ${e.message}")
                }
            }
        }
    }

    // Logic Functions

    fun login(username: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = repository.getAccountByUsername(username)
            if (account == null) {
                _loginError.value = "Username tidak ditemukan!"
                repository.insertLog("unauthenticated", "Role Unknown", "Gagal login: username $username tidak terdaftar")
                onResult(false)
            } else if (account.password != password) {
                _loginError.value = "Password salah!"
                repository.insertLog(username, account.role, "Gagal login: password salah")
                onResult(false)
            } else if (!account.isEnabled) {
                _loginError.value = "Akun Anda dinonaktifkan oleh Kepala Wilayah!"
                repository.insertLog(username, account.role, "Gagal login: akun dinegasi/dinonaktifkan")
                onResult(false)
            } else {
                _currentUser.value = account
                _loginError.value = null
                repository.insertLog(username, account.role, "Berhasil login ke sistem")
                addNotification("Akses Masuk", "Selamat datang kembali, ${account.displayName}!")
                onResult(true)
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        _currentUser.value = null
        if (user != null) {
            viewModelScope.launch {
                repository.insertLog(user.username, user.role, "Berhasil logout dari sistem")
            }
        }
    }

    // Real-Time Notification Trigger
    fun addNotification(title: String, message: String) {
        val newNotif = AppNotification(title = title, message = message)
        _notifications.update { listOf(newNotif) + it }
    }

    fun dismissNotification(id: Long) {
        _notifications.update { list -> list.filter { it.id != id } }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    // Warga CRUD Operations
    fun saveWarga(
        id: Int = 0,
        nama: String,
        nik: String,
        noKk: String,
        gender: String,
        tanggalLahir: String,
        rt: String,
        rw: String,
        umur: Int,
        isDisabilitas: Boolean,
        jenisDisabilitas: String,
        status: String = "Aktif",
        tempatLahir: String = "Jakarta",
        agama: String = "Islam",
        statusPerkawinan: String = "Belum Kawin",
        pekerjaan: String = "Karyawan Swasta",
        kewarganegaraan: String = "WNI",
        pendidikan: String = "SMA/Sederajat",
        golonganDarah: String = "Tidak Tahu",
        hubunganKeluarga: String = "Anggota Keluarga",
        bantuanList: String = "",
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            val warga = Warga(
                id = id,
                nama = nama,
                nik = nik,
                noKk = noKk,
                gender = gender,
                tanggalLahir = tanggalLahir,
                rt = rt,
                rw = rw,
                umur = umur,
                isDisabilitas = isDisabilitas,
                jenisDisabilitas = jenisDisabilitas,
                status = status,
                tempatLahir = tempatLahir,
                agama = agama,
                statusPerkawinan = statusPerkawinan,
                pekerjaan = pekerjaan,
                kewarganegaraan = kewarganegaraan,
                pendidikan = pendidikan,
                golonganDarah = golonganDarah,
                hubunganKeluarga = hubunganKeluarga,
                bantuanList = bantuanList
            )

            if (id == 0) {
                repository.insertWarga(warga)
                val logDetail = "Menambahkan warga baru: $nama (NIK: $nik, KK: $noKk, Jk: $gender, RT/RW: $rt/$rw, Umur: $umur Thn)"
                repository.insertLog(user?.username ?: "system", user?.role ?: "System", logDetail)
                addNotification("Pembaruan Data", "Warga baru bernama $nama berhasil terdaftar di RT $rt")
            } else {
                val oldWarga = repository.getWargaById(id)
                val changeDetails = mutableListOf<String>()
                if (oldWarga != null) {
                    if (oldWarga.nama != nama) changeDetails.add("Nama: '${oldWarga.nama}' -> '$nama'")
                    if (oldWarga.nik != nik) changeDetails.add("NIK: '${oldWarga.nik}' -> '$nik'")
                    if (oldWarga.noKk != noKk) changeDetails.add("No KK: '${oldWarga.noKk}' -> '$noKk'")
                    if (oldWarga.gender != gender) changeDetails.add("Gender: '${oldWarga.gender}' -> '$gender'")
                    if (oldWarga.tanggalLahir != tanggalLahir) changeDetails.add("Tgl Lahir: '${oldWarga.tanggalLahir}' -> '$tanggalLahir'")
                    if (oldWarga.rt != rt) changeDetails.add("RT: '${oldWarga.rt}' -> '$rt'")
                    if (oldWarga.rw != rw) changeDetails.add("RW: '${oldWarga.rw}' -> '$rw'")
                    if (oldWarga.umur != umur) changeDetails.add("Umur: ${oldWarga.umur} -> $umur")
                    if (oldWarga.isDisabilitas != isDisabilitas) changeDetails.add("Disabilitas: ${oldWarga.isDisabilitas} -> $isDisabilitas")
                    if (oldWarga.jenisDisabilitas != jenisDisabilitas) changeDetails.add("Jenis Disabilitas: '${oldWarga.jenisDisabilitas}' -> '$jenisDisabilitas'")
                    if (oldWarga.status != status) changeDetails.add("Status: '${oldWarga.status}' -> '$status'")
                    if (oldWarga.tempatLahir != tempatLahir) changeDetails.add("Tempat Lahir: '${oldWarga.tempatLahir}' -> '$tempatLahir'")
                    if (oldWarga.agama != agama) changeDetails.add("Agama: '${oldWarga.agama}' -> '$agama'")
                    if (oldWarga.statusPerkawinan != statusPerkawinan) changeDetails.add("Status Perkawinan: '${oldWarga.statusPerkawinan}' -> '$statusPerkawinan'")
                    if (oldWarga.pekerjaan != pekerjaan) changeDetails.add("Pekerjaan: '${oldWarga.pekerjaan}' -> '$pekerjaan'")
                    if (oldWarga.kewarganegaraan != kewarganegaraan) changeDetails.add("Kewarganegaraan: '${oldWarga.kewarganegaraan}' -> '$kewarganegaraan'")
                    if (oldWarga.pendidikan != pendidikan) changeDetails.add("Pendidikan: '${oldWarga.pendidikan}' -> '$pendidikan'")
                    if (oldWarga.golonganDarah != golonganDarah) changeDetails.add("Golongan Darah: '${oldWarga.golonganDarah}' -> '$golonganDarah'")
                    if (oldWarga.hubunganKeluarga != hubunganKeluarga) changeDetails.add("Hubungan Keluarga: '${oldWarga.hubunganKeluarga}' -> '$hubunganKeluarga'")
                    if (oldWarga.bantuanList != bantuanList) changeDetails.add("Bantuan: '${oldWarga.bantuanList}' -> '$bantuanList'")
                }

                repository.updateWarga(warga)

                val detailsStr = if (changeDetails.isNotEmpty()) changeDetails.joinToString(", ") else "Tidak ada perubahan nilai"
                val logDetail = "Mengubah data warga: $nama (NIK: $nik). Perubahan: [$detailsStr]"
                repository.insertLog(user?.username ?: "system", user?.role ?: "System", logDetail)
                addNotification("Pembaruan Data", "Data warga $nama berhasil diubah")
            }

            _operationStatus.emit("Data warga berhasil disimpan!")
            onCompleted()
        }
    }

    fun deleteWarga(warga: Warga) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWarga(warga)
            val user = _currentUser.value
            val logDetail = "Menghapus data warga secara permanen: ${warga.nama} (NIK: ${warga.nik}, KK: ${warga.noKk}, RT/RW: ${warga.rt}/${warga.rw})"
            repository.insertLog(user?.username ?: "system", user?.role ?: "System", logDetail)
            addNotification("Penghapusan Data", "Data warga ${warga.nama} telah dihapus dari sistem")
            _operationStatus.emit("Data warga berhasil dihapus!")
        }
    }

    fun recordWargaMeninggal(warga: Warga, tanggalKematian: String, keterangan: String, onCompleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            
            // 1. Update warga status to "Meninggal"
            val updatedWarga = warga.copy(status = "Meninggal")
            repository.updateWarga(updatedWarga)
            
            // 2. Insert mutasi data of type "Kematian"
            val mutasi = MutasiWarga(
                id = 0,
                tipe = "Kematian",
                tanggal = tanggalKematian,
                namaWarga = warga.nama,
                nik = warga.nik,
                noKk = warga.noKk,
                keterangan = "Meninggal: $keterangan",
                operator = user?.username ?: "system"
            )
            repository.insertMutasi(mutasi)
            
            // 3. Log
            val logDetail = "Mencatat warga meninggal dunia & menonaktifkan data warga: ${warga.nama} (NIK: ${warga.nik}, KK: ${warga.noKk}, RT/RW: ${warga.rt}/${warga.rw}). Keterangan Kematian: $keterangan"
            repository.insertLog(
                user?.username ?: "system",
                user?.role ?: "RT/RW",
                logDetail
            )
            
            // 4. Send notification
            addNotification("Sistem Kependudukan", "Warga bernama ${warga.nama} tercatat Meninggal pada tanggal $tanggalKematian")
            
            _operationStatus.emit("Warga ${warga.nama} berhasil dicatat sebagai Meninggal!")
            
            viewModelScope.launch(Dispatchers.Main) {
                onCompleted()
            }
        }
    }

    // Mutasi Warga
    fun recordMutasi(
        tipe: String, // "Kelahiran" | "Kematian" | "Pendatang" | "Pindah Keluar"
        tanggal: String,
        namaWarga: String,
        nik: String,
        noKk: String,
        keterangan: String,
        rt: String = "",
        rw: String = "",
        gender: String = "Laki-laki",
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            val operatorName = user?.username ?: "system"

            // Save Mutation record
            repository.insertMutasi(
                MutasiWarga(
                    tipe = tipe,
                    tanggal = tanggal,
                    namaWarga = namaWarga,
                    nik = nik,
                    noKk = noKk,
                    keterangan = keterangan,
                    operator = operatorName
                )
            )

            // Dynamic logic: based on mutation type, automatically adjust citizens record!
            if (tipe == "Kematian") {
                // Find matching citizen by NIK
                val match = wargaList.value.find { it.nik == nik }
                if (match != null) {
                    // Update citizen status to "Meninggal"
                    val updatedWarga = match.copy(status = "Meninggal")
                    repository.updateWarga(updatedWarga)
                    addNotification(
                        "Mutasi Real-time: Kematian",
                        "Warga ${match.nama} (NIK: ${match.nik}) kini berstatus 'Meninggal' di database"
                    )
                } else {
                    addNotification(
                        "Mutasi Kematian Dicatat",
                        "Kematian warga non-terdaftar bernama $namaWarga berhasil dicatat"
                    )
                }
            } else if (tipe == "Kelahiran") {
                // Automatically add as a new child citizen in the database
                // Extract RT & RW from current logged in operator (default RT 01 / RW 02) or selected parent
                val userRt = if (rt.isNotEmpty()) rt else (user?.rt ?: "01")
                val userRw = if (rw.isNotEmpty()) rw else (user?.rw ?: "02")
                val babyWarga = Warga(
                    nama = namaWarga,
                    nik = nik.ifEmpty { "BABY-" + System.currentTimeMillis().toString().takeLast(8) },
                    noKk = noKk,
                    gender = gender,
                    tanggalLahir = tanggal,
                    rt = userRt,
                    rw = userRw,
                    umur = 0,
                    isDisabilitas = false,
                    jenisDisabilitas = "",
                    status = "Aktif"
                )
                repository.insertWarga(babyWarga)
                addNotification(
                    "Mutasi Real-time: Kelahiran",
                    "Kelahiran Bayi $namaWarga ($gender) berhasil dicatat dan didaftarkan otomatis ke RT $userRt"
                )
            } else if (tipe == "Pindah Keluar") {
                val match = wargaList.value.find { it.nik == nik }
                if (match != null) {
                    val updatedWarga = match.copy(status = "Pindah")
                    repository.updateWarga(updatedWarga)
                    addNotification(
                        "Mutasi Real-time: Pindah",
                        "Warga ${match.nama} berstatus 'Pindah' dan dikeluarkan dari daftar aktif"
                    )
                }
            }

            _operationStatus.emit("Mutasi warga berhasil direkam!")
            onCompleted()
        }
    }

    // Admin Control: Restricting Account Access
    fun toggleAccountAccess(username: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            if (user?.role != "Kepala Wilayah") {
                _operationStatus.emit("Hanya Kepala Wilayah yang berhak mengubah hak akses!")
                return@launch
            }

            val matchingAccount = accountsList.value.find { it.username == username }
            if (matchingAccount != null) {
                val updated = matchingAccount.copy(isEnabled = isEnabled)
                repository.updateAccount(updated)

                val statusText = if (isEnabled) "DIBERIKAN AKSES" else "DIBATASI (BLOCKED)"
                addNotification("Akses Akun Diubah", "Akun ${matchingAccount.displayName} ($username) sekarang $statusText")
                _operationStatus.emit("Akses akun $username berhasil diperbarui!")
            }
        }
    }

    // Database Backup Simulation
    fun triggerDatabaseBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            val timeStampString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupName = "backup_db_v1_${timeStampString}.sql"
            val mockSize = (100..450).random() / 10.0 // Size between 10.0 KB and 45.0 KB

            val backup = DatabaseBackup(
                backupName = backupName,
                status = "Sukses",
                sizeInKb = mockSize
            )

            repository.insertBackup(backup)
            repository.insertLog(
                user?.username ?: "system",
                user?.role ?: "System",
                "Membuat backup database manual: $backupName (${mockSize} KB)"
            )

            addNotification("Backup Database", "Database berhasil di-backup berkala ke file $backupName")
            _operationStatus.emit("Backup $backupName berhasil dibuat!")
        }
    }

    // Kelurahan announcements with WhatsApp auto-generation and deep-link triggering
    fun createKelurahanAnnouncement(
        title: String,
        content: String,
        isUrgent: Boolean,
        context: Context,
        triggerWhatsapp: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val ann = Announcement(
                title = title,
                content = content,
                date = dateFormat,
                isUrgent = isUrgent,
                wasSentToWhatsapp = triggerWhatsapp,
                whatsappStatus = if (triggerWhatsapp) "Terkirim ke semua RT/RW secara otomatis" else "Pending"
            )

            repository.insertAnnouncement(ann)
            addNotification(
                "Pengumuman Baru",
                "Pengumuman '${title}' telah diposting dari Kelurahan"
            )

            if (triggerWhatsapp) {
                // Call modern third-party automated API gateway broadcast
                sendThirdPartyWhatsAppNotification(title, content)

                // Create a simulated broadcast list of RT/RW contacts
                val textPayload = """
                    📢 *PENGUMUMAN RESMI KELURAHAN* 📢
                    -------------------------------------
                    *Judul:* ${title.uppercase()}
                    *Tanggal:* $dateFormat
                    *Kategori:* ${if (isUrgent) "🚨 PENTING / URGENT" else "📌 Pengumuman Biasa"}
                    
                    *Isi Pengumuman:*
                    $content
                    
                    -------------------------------------
                    _Sistem Aplikasi Kependudukan Digital RT-RW_
                """.trimIndent()

                // Trigger Android Share Intent so it displays WhatsApp selection or default intent
                viewModelScope.launch(Dispatchers.Main) {
                    try {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textPayload)
                            type = "text/plain"
                            // Specially target WhatsApp if installed, or just general text share
                            `package` = "com.whatsapp"
                        }
                        // Create selector fallback if whatsapp package was not found on active emulator emulator
                        val shareIntent = Intent.createChooser(sendIntent, "Kirim Pengumuman via WhatsApp")
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(shareIntent)
                        _operationStatus.emit("Menghubungi API WhatsApp / Membuka Share Intent...")
                    } catch (e: Exception) {
                        // WhatsApp not installed, fallback to general share chooser
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textPayload)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Hubungkan WhatsApp RT/RW")
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(shareIntent)
                        _operationStatus.emit("WhatsApp tidak terpasang, membuka pemilih aplikasi")
                    }
                }
            } else {
                _operationStatus.emit("Pengumuman berhasil dipublikasikan di dashboard!")
            }
        }
    }

    // CSV / Excel Data Import and Export for Disdukcapil Format Alignment
    fun exportFilteredWargaToCsv(): String {
        val list = filteredWargaList.value
        val sb = StringBuilder()
        // CSV Header
        sb.append("NIK,No KK,Nama,Gender,Tempat Lahir,Tanggal Lahir,Umur,Agama,Status Nikah,Pekerjaan,Kewarganegaraan,Pendidikan,Gol Darah,Hubungan Keluarga,RT,RW,Disabilitas,Jenis Disabilitas,Status\n")
        
        list.forEach { w ->
            val disabilitasStr = if (w.isDisabilitas) "Ya" else "Tidak"
            val line = listOf(
                w.nik.replace(",", " "),
                w.noKk.replace(",", " "),
                w.nama.replace(",", " "),
                w.gender.replace(",", " "),
                w.tempatLahir.replace(",", " "),
                w.tanggalLahir.replace(",", " "),
                w.umur.toString(),
                w.agama.replace(",", " "),
                w.statusPerkawinan.replace(",", " "),
                w.pekerjaan.replace(",", " "),
                w.kewarganegaraan.replace(",", " "),
                w.pendidikan.replace(",", " "),
                w.golonganDarah.replace(",", " "),
                w.hubunganKeluarga.replace(",", " "),
                w.rt.replace(",", " "),
                w.rw.replace(",", " "),
                disabilitasStr,
                w.jenisDisabilitas.replace(",", " "),
                w.status.replace(",", " ")
            ).joinToString(",")
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun importWargaFromCsv(csvText: String, onCompleted: (successCount: Int, errorMsg: String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = csvText.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) {
                viewModelScope.launch(Dispatchers.Main) {
                    onCompleted(0, "Format data kosong atau tidak valid!")
                }
                return@launch
            }

            var successCount = 0
            var errorLine: String? = null
            
            // Detect if first line is header, skip it if it starts with standard header keyword (e.g., NIK or No)
            val startIdx = if (lines[0].contains("NIK", ignoreCase = true) || lines[0].contains("No", ignoreCase = true)) 1 else 0

            for (i in startIdx until lines.size) {
                try {
                    val line = lines[i]
                    val tokens = line.split(",")
                    if (tokens.size < 3) continue // Minimum NIK, KK, Nama required
                    
                    val nik = tokens.getOrNull(0)?.trim() ?: ""
                    val noKk = tokens.getOrNull(1)?.trim() ?: ""
                    val nama = tokens.getOrNull(2)?.trim() ?: ""
                    if (nik.isEmpty() || noKk.isEmpty() || nama.isEmpty()) continue

                    val gender = tokens.getOrNull(3)?.trim() ?: "Laki-laki"
                    val tempatLahir = tokens.getOrNull(4)?.trim() ?: "Jakarta"
                    val tanggalLahir = tokens.getOrNull(5)?.trim() ?: "1995-01-01"
                    val umur = tokens.getOrNull(6)?.trim()?.toIntOrNull() ?: 30
                    val agama = tokens.getOrNull(7)?.trim() ?: "Islam"
                    val statusPerkawinan = tokens.getOrNull(8)?.trim() ?: "Belum Kawin"
                    val pekerjaan = tokens.getOrNull(9)?.trim() ?: "Karyawan Swasta"
                    val kewarganegaraan = tokens.getOrNull(10)?.trim() ?: "WNI"
                    val pendidikan = tokens.getOrNull(11)?.trim() ?: "SMA/Sederajat"
                    val golDarah = tokens.getOrNull(12)?.trim() ?: "Tidak Tahu"
                    val hubKeluarga = tokens.getOrNull(13)?.trim() ?: "Anggota Keluarga"
                    val rt = tokens.getOrNull(14)?.trim() ?: (_currentUser.value?.rt?.ifEmpty { "01" } ?: "01")
                    val rw = tokens.getOrNull(15)?.trim() ?: (_currentUser.value?.rw?.ifEmpty { "02" } ?: "02")
                    val disabilitasStr = tokens.getOrNull(16)?.trim() ?: "Tidak"
                    val isDisabilitas = disabilitasStr.contains("ya", ignoreCase = true) || disabilitasStr.contains("true", ignoreCase = true)
                    val jenisDisabilitas = tokens.getOrNull(17)?.trim() ?: ""
                    val status = tokens.getOrNull(18)?.trim() ?: "Aktif"

                    // Security restriction: RT and RW roles can only import users within their own RT/RW jurisdiction!
                    val user = _currentUser.value
                    val finalRt = if (user != null && user.role == "RT") user.rt else rt
                    val finalRw = if (user != null && (user.role == "RT" || user.role == "RW")) user.rw else rw

                    val listAll = wargaList.value
                    val wargaExist = listAll.find { it.nik == nik }
                    val warga = Warga(
                        id = wargaExist?.id ?: 0,
                        nama = nama,
                        nik = nik,
                        noKk = noKk,
                        gender = gender,
                        tempatLahir = tempatLahir,
                        tanggalLahir = tanggalLahir,
                        umur = umur,
                        agama = agama,
                        statusPerkawinan = statusPerkawinan,
                        pekerjaan = pekerjaan,
                        kewarganegaraan = kewarganegaraan,
                        pendidikan = pendidikan,
                        golonganDarah = golDarah,
                        hubunganKeluarga = hubKeluarga,
                        rt = finalRt,
                        rw = finalRw,
                        isDisabilitas = isDisabilitas,
                        jenisDisabilitas = jenisDisabilitas,
                        status = status
                    )
                    
                    if (warga.id == 0) {
                        repository.insertWarga(warga)
                    } else {
                        repository.updateWarga(warga)
                    }
                    successCount++
                } catch (e: Exception) {
                    errorLine = "Gagal parsing baris ${i + 1}: ${e.localizedMessage}"
                }
            }

            if (successCount > 0) {
                val user = _currentUser.value
                repository.insertLog(
                    user?.username ?: "system",
                    user?.role ?: "System",
                    "Melakukan import data kependudukan: Berhasil memasukkan/mengupdate $successCount data dari file Excel/CSV"
                )
                addNotification("Import Warga", "$successCount warga berhasil diimport dari Excel/CSV!")
            }

            viewModelScope.launch(Dispatchers.Main) {
                onCompleted(successCount, errorLine)
            }
        }
    }

    // Google Sheets Cloud Synchronization
    fun pushToGoogleSheets() {
        val url = _googleSheetsUrl.value.trim()
        if (url.isEmpty()) {
            _syncMessage.value = "Error: URL Google Apps Script belum dikonfigurasi!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _syncLoading.value = true
            _syncMessage.value = "Menghubungkan ke Google Sheets..."
            try {
                // We sync the filtered list based on user role (respects data separation)
                val listToSync = filteredWargaList.value
                val payload = makeWargaJsonPayload(listToSync)

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = okhttp3.RequestBody.create(mediaType, payload)

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    var currentResponse = response
                    if (response.code == 302 || response.code == 301 || response.code == 307 || response.code == 308) {
                        val redirectUrl = response.header("Location")
                        if (redirectUrl != null) {
                            val redirectRequest = okhttp3.Request.Builder()
                                .url(redirectUrl)
                                .post(body)
                                .build()
                            currentResponse = client.newCall(redirectRequest).execute()
                        }
                    }

                    if (!currentResponse.isSuccessful) {
                        _syncMessage.value = "Gagal mengunggah data (HTTP ${currentResponse.code})"
                    } else {
                        val responseBody = currentResponse.body?.string() ?: ""
                        if (responseBody.contains("error")) {
                            val errorJson = JSONObject(responseBody)
                            _syncMessage.value = "Gagal: ${errorJson.optString("message")}"
                        } else {
                            val resJson = JSONObject(responseBody)
                            val inserted = resJson.optInt("inserted", 0)
                            val updated = resJson.optInt("updated", 0)
                            var total = resJson.optInt("total", 0)
                            if (total == 0) total = listToSync.size

                            _syncMessage.value = "Berhasil Unggah! Menyinkronkan $total warga (Baru: $inserted, Update: $updated)."
                            
                            val user = _currentUser.value
                            repository.insertLog(
                                user?.username ?: "system",
                                user?.role ?: "System",
                                "Sinkronisasi Cloud Google Sheets: Mengunggah $total warga (Baru: $inserted, Update: $updated)"
                            )
                            addNotification("Sinkronisasi Sukses", "Data kependudukan berhasil diunggah ke Google Sheets Cloud.")
                        }
                    }
                }
            } catch (e: Exception) {
                _syncMessage.value = "Error koneksi: ${e.localizedMessage}"
            } finally {
                _syncLoading.value = false
            }
        }
    }

    fun pullFromGoogleSheets() {
        val url = _googleSheetsUrl.value.trim()
        if (url.isEmpty()) {
            _syncMessage.value = "Error: URL Google Apps Script belum dikonfigurasi!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _syncLoading.value = true
            _syncMessage.value = "Mengunduh data warga dari Google Sheets..."
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    var currentResponse = response
                    if (response.code == 302 || response.code == 301 || response.code == 307 || response.code == 308) {
                        val redirectUrl = response.header("Location")
                        if (redirectUrl != null) {
                            val redirectRequest = okhttp3.Request.Builder()
                                .url(redirectUrl)
                                .get()
                                .build()
                            currentResponse = client.newCall(redirectRequest).execute()
                        }
                    }

                    if (!currentResponse.isSuccessful) {
                        _syncMessage.value = "Gagal mengunduh data (HTTP ${currentResponse.code})"
                    } else {
                        val responseBody = currentResponse.body?.string() ?: ""
                        if (responseBody.startsWith("{") && responseBody.contains("error")) {
                            val errorJson = JSONObject(responseBody)
                            _syncMessage.value = "Gagal: ${errorJson.optString("message")}"
                        } else {
                            val jsonArray = JSONArray(responseBody)
                            var successCount = 0
                            val user = _currentUser.value
                            val allWargaInDb = wargaList.value

                            for (i in 0 until jsonArray.length()) {
                                try {
                                    val obj = jsonArray.getJSONObject(i)
                                    val nik = obj.optString("nik").trim()
                                    val noKk = obj.optString("noKk").trim().ifEmpty { obj.optString("no_kk").trim() }
                                    val nama = obj.optString("nama").trim()
                                    if (nik.isEmpty() || nama.isEmpty()) continue

                                    val gender = obj.optString("gender", "Laki-laki")
                                    val tempatLahir = obj.optString("tempatLahir", "Jakarta")
                                    val tanggalLahir = obj.optString("tanggalLahir", "1995-01-01")
                                    val umur = obj.optInt("umur", 30)
                                    val agama = obj.optString("agama", "Islam")
                                    val statusPerkawinan = obj.optString("statusPerkawinan", "Belum Kawin")
                                    val pekerjaan = obj.optString("pekerjaan", "Karyawan Swasta")
                                    val kewarganegaraan = obj.optString("kewarganegaraan", "WNI")
                                    val pendidikan = obj.optString("pendidikan", "SMA/Sederajat")
                                    val golDarah = obj.optString("golonganDarah", "Tidak Tahu").ifEmpty { obj.optString("golDarah", "Tidak Tahu") }
                                    val hubKeluarga = obj.optString("hubunganKeluarga", "Anggota Keluarga").ifEmpty { obj.optString("hubKeluarga", "Anggota Keluarga") }
                                    val rt = obj.optString("rt", "01")
                                    val rw = obj.optString("rw", "02")
                                    val disabilitasStr = obj.optString("isDisabilitas", "Tidak")
                                    val isDisabilitas = disabilitasStr.contains("ya", ignoreCase = true) || disabilitasStr.contains("true", ignoreCase = true)
                                    val jenisDisabilitas = obj.optString("jenisDisabilitas", "")
                                    val status = obj.optString("status", "Aktif")

                                    // Hierarchy scope restriction: RT and RW roles can only import users within their own RT/RW jurisdiction!
                                    if (user != null) {
                                        if (user.role == "RT" && (user.rt.trim() != rt.trim() || user.rw.trim() != rw.trim())) {
                                            continue
                                        }
                                        if (user.role == "RW" && user.rw.trim() != rw.trim()) {
                                            continue
                                        }
                                    }

                                    val match = allWargaInDb.find { it.nik == nik }
                                    val warga = Warga(
                                        id = match?.id ?: 0,
                                        nama = nama,
                                        nik = nik,
                                        noKk = noKk,
                                        gender = gender,
                                        tempatLahir = tempatLahir,
                                        tanggalLahir = tanggalLahir,
                                        umur = umur,
                                        agama = agama,
                                        statusPerkawinan = statusPerkawinan,
                                        pekerjaan = pekerjaan,
                                        kewarganegaraan = kewarganegaraan,
                                        pendidikan = pendidikan,
                                        golonganDarah = golDarah,
                                        hubunganKeluarga = hubKeluarga,
                                        rt = rt,
                                        rw = rw,
                                        isDisabilitas = isDisabilitas,
                                        jenisDisabilitas = jenisDisabilitas,
                                        status = status
                                    )

                                    if (warga.id == 0) {
                                        repository.insertWarga(warga)
                                    } else {
                                        repository.updateWarga(warga)
                                    }
                                    successCount++
                                } catch (e: Exception) {
                                    // Skip single row if invalid
                                }
                            }

                            _syncMessage.value = "Unduh Berhasil! Terimport/diupdate $successCount warga dari Google Sheets cloud."
                            repository.insertLog(
                                user?.username ?: "system",
                                user?.role ?: "System",
                                "Sinkronisasi Cloud Google Sheets: Mengunduh dan menggabungkan $successCount warga"
                            )
                            addNotification("Sinkronisasi Sukses", "Sinkronisasi unduh selesai: $successCount warga terintegrasi ke SQLite lokal.")
                        }
                    }
                }
            } catch (e: Exception) {
                _syncMessage.value = "Error unduh: ${e.localizedMessage}"
            } finally {
                _syncLoading.value = false
            }
        }
    }

    private fun makeWargaJsonPayload(list: List<Warga>): String {
        val sb = StringBuilder()
        sb.append("[")
        list.forEachIndexed { index, w ->
            sb.append("{")
            sb.append("\"id\":${w.id},")
            sb.append("\"nama\":\"${escapeJsonString(w.nama)}\",")
            sb.append("\"nik\":\"${escapeJsonString(w.nik)}\",")
            sb.append("\"noKk\":\"${escapeJsonString(w.noKk)}\",")
            sb.append("\"gender\":\"${escapeJsonString(w.gender)}\",")
            sb.append("\"tanggalLahir\":\"${escapeJsonString(w.tanggalLahir)}\",")
            sb.append("\"rt\":\"${escapeJsonString(w.rt)}\",")
            sb.append("\"rw\":\"${escapeJsonString(w.rw)}\",")
            sb.append("\"umur\":${w.umur},")
            sb.append("\"isDisabilitas\":${w.isDisabilitas},")
            sb.append("\"jenisDisabilitas\":\"${escapeJsonString(w.jenisDisabilitas)}\",")
            sb.append("\"status\":\"${escapeJsonString(w.status)}\",")
            sb.append("\"tempatLahir\":\"${escapeJsonString(w.tempatLahir)}\",")
            sb.append("\"agama\":\"${escapeJsonString(w.agama)}\",")
            sb.append("\"statusPerkawinan\":\"${escapeJsonString(w.statusPerkawinan)}\",")
            sb.append("\"pekerjaan\":\"${escapeJsonString(w.pekerjaan)}\",")
            sb.append("\"kewarganegaraan\":\"${escapeJsonString(w.kewarganegaraan)}\",")
            sb.append("\"pendidikan\":\"${escapeJsonString(w.pendidikan)}\",")
            sb.append("\"golonganDarah\":\"${escapeJsonString(w.golonganDarah)}\",")
            sb.append("\"hubunganKeluarga\":\"${escapeJsonString(w.hubunganKeluarga)}\"")
            sb.append("}")
            if (index < list.size - 1) {
                sb.append(",")
            }
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
    }
}
