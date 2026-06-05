package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class CitizenRepository(
    private val wargaDao: WargaDao,
    private val mutasiDao: MutasiDao,
    private val accountDao: UserAccountDao,
    private val logDao: ActivityLogDao,
    private val backupDao: DatabaseBackupDao,
    private val announcementDao: AnnouncementDao
) {
    val allWarga: Flow<List<Warga>> = wargaDao.getAllWarga()
    val allMutasi: Flow<List<MutasiWarga>> = mutasiDao.getAllMutasi()
    val allAccounts: Flow<List<UserAccount>> = accountDao.getAllAccounts()
    val allLogs: Flow<List<ActivityLog>> = logDao.getAllLogs()
    val allBackups: Flow<List<DatabaseBackup>> = backupDao.getAllBackups()
    val allAnnouncements: Flow<List<Announcement>> = announcementDao.getAllAnnouncements()

    suspend fun insertWarga(warga: Warga): Long {
        val id = wargaDao.insertWarga(warga)
        insertLog("system", "System", "Menambahkan warga baru: ${warga.nama} NIK: ${warga.nik}")
        return id
    }

    suspend fun updateWarga(warga: Warga) {
        wargaDao.updateWarga(warga)
        insertLog("system", "System", "Mengubah data warga: ${warga.nama} NIK: ${warga.nik}")
    }

    suspend fun deleteWarga(warga: Warga) {
        wargaDao.deleteWarga(warga)
        insertLog("system", "System", "Menghapus warga: ${warga.nama} NIK: ${warga.nik}")
    }

    suspend fun getWargaById(id: Int): Warga? {
        return wargaDao.getWargaById(id)
    }

    suspend fun insertMutasi(mutasi: MutasiWarga): Long {
        val id = mutasiDao.insertMutasi(mutasi)
        // Also reflect the mutation status on corresponding citizen automatically if born/dead
        if (mutasi.tipe == "Kematian") {
            // Find citizen by NIK and set status to "Meninggal"
            // For birth, the admin adds a separate citizen entry.
            // Let's log it.
            insertLog(mutasi.operator, "RT/RW", "Mencatat mutasi kematian warga: ${mutasi.namaWarga}")
        } else if (mutasi.tipe == "Kelahiran") {
            insertLog(mutasi.operator, "RT/RW", "Mencatat mutasi kelahiran anak: ${mutasi.namaWarga}")
        }
        return id
    }

    suspend fun getAccountByUsername(username: String): UserAccount? {
        return accountDao.getAccountByUsername(username)
    }

    suspend fun insertAccount(account: UserAccount) {
        accountDao.insertAccount(account)
        insertLog("admin", "Admin", "Menambahkan/Mengupdate akun user: ${account.username} Role: ${account.role}")
    }

    suspend fun updateAccount(account: UserAccount) {
        accountDao.updateAccount(account)
        val statusStr = if (account.isEnabled) "Diaktifkan" else "Dinonaktifkan"
        insertLog("admin", "Admin", "Mengubah status akun ${account.username} menjadi $statusStr")
    }

    suspend fun insertLog(username: String, role: String, action: String) {
        logDao.insertLog(ActivityLog(username = username, role = role, action = action))
    }

    suspend fun insertBackup(backup: DatabaseBackup) {
        backupDao.insertBackup(backup)
        insertLog("system", "System", "Melakukan backup database berkala: ${backup.backupName}")
    }

    suspend fun insertAnnouncement(announcement: Announcement): Long {
        val id = announcementDao.insertAnnouncement(announcement)
        insertLog("admin", "Admin", "Membuat pengumuman baru: ${announcement.title}")
        return id
    }

    suspend fun updateAnnouncement(announcement: Announcement) {
        announcementDao.updateAnnouncement(announcement)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }
}
