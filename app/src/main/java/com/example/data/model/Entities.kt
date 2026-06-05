package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warga")
data class Warga(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val nik: String,
    val noKk: String,
    val gender: String, // "Laki-laki" | "Perempuan"
    val tanggalLahir: String, // YYYY-MM-DD
    val rt: String, // e.g. "01"
    val rw: String, // e.g. "02"
    val umur: Int,
    val isDisabilitas: Boolean,
    val jenisDisabilitas: String = "", // empty if none
    val status: String = "Aktif", // "Aktif" | "Meninggal" | "Pindah"
    val tempatLahir: String = "Jakarta",
    val agama: String = "Islam",
    val statusPerkawinan: String = "Belum Kawin",
    val pekerjaan: String = "Karyawan Swasta",
    val kewarganegaraan: String = "WNI",
    val pendidikan: String = "SMA/Sederajat",
    val golonganDarah: String = "Tidak Tahu",
    val hubunganKeluarga: String = "Anggota Keluarga",
    val bantuanList: String = "" // comma-separated e.g. "PKH, BPNT"
) {
    // Computed helper for elderly (lansia) -> Indonesian standard is age >= 60
    val isLansia: Boolean
        get() = umur >= 60
}

@Entity(tableName = "mutas_warga")
data class MutasiWarga(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipe: String, // "Kelahiran" | "Kematian" | "Pendatang" | "Pindah Keluar"
    val tanggal: String, // YYYY-MM-DD
    val namaWarga: String,
    val nik: String = "",
    val noKk: String = "",
    val keterangan: String = "",
    val operator: String = "" // who logged it (e.g. "rt01")
)

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey val username: String,
    val displayName: String,
    val password: String,
    val role: String, // "RT" | "RW" | "Kepala Wilayah"
    val rt: String,   // e.g. "01" (for RT role)
    val rw: String,   // e.g. "02" (for RT & RW role)
    val isEnabled: Boolean = true
)

@Entity(tableName = "activity_log")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val role: String,
    val action: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "database_backup")
data class DatabaseBackup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val backupName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "Sukses" | "Gagal"
    val sizeInKb: Double
)

@Entity(tableName = "announcement")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val date: String,
    val isUrgent: Boolean = false,
    val wasSentToWhatsapp: Boolean = false,
    val whatsappStatus: String = "Pending" // "Pending" | "Terkirim ke semua RT/RW" | "Gagal"
)
