package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Warga::class,
        MutasiWarga::class,
        UserAccount::class,
        ActivityLog::class,
        DatabaseBackup::class,
        Announcement::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wargaDao(): WargaDao
    abstract fun mutasiDao(): MutasiDao
    abstract fun accountDao(): UserAccountDao
    abstract fun logDao(): ActivityLogDao
    abstract fun backupDao(): DatabaseBackupDao
    abstract fun announcementDao(): AnnouncementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kependudukan_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(
                        database.wargaDao(),
                        database.mutasiDao(),
                        database.accountDao(),
                        database.logDao(),
                        database.announcementDao()
                    )
                }
            }
        }

        suspend fun populateDatabase(
            wargaDao: WargaDao,
            mutasiDao: MutasiDao,
            accountDao: UserAccountDao,
            logDao: ActivityLogDao,
            announcementDao: AnnouncementDao
        ) {
            // Seed accounts
            accountDao.insertAccount(UserAccount("rt01", "Pak RT 01", "rt", "RT", "01", "02", true))
            accountDao.insertAccount(UserAccount("rt02", "Bu RT 02", "rt", "RT", "02", "02", true))
            accountDao.insertAccount(UserAccount("rw02", "Pak RW 02", "rw", "RW", "", "02", true))
            accountDao.insertAccount(UserAccount("admin", "Kepala Wilayah", "admin", "Kepala Wilayah", "", "", true))

            // Seed initial warga data with full Disdukcapil fields
            wargaDao.insertWarga(Warga(0, "Mochammad Ilham", "3275010605920001", "3275011212880001", "Laki-laki", "1992-05-06", "01", "02", 34, false, "", "Aktif", "Bekasi", "Islam", "Belum Kawin", "Wiraswasta", "WNI", "Diploma/S1/Sederajat", "O", "Kepala Keluarga", ""))
            wargaDao.insertWarga(Warga(0, "Siti Rahmawati", "3275014708550001", "3275011212880001", "Perempuan", "1955-08-07", "01", "02", 70, false, "", "Aktif", "Solo", "Islam", "Cerai Mati", "Ibu Rumah Tangga", "WNI", "SD/Sederajat", "AB", "Orang Tua", "PBI JK")) // Lansia
            wargaDao.insertWarga(Warga(0, "Budi Hartono", "3275012211600002", "3275012512990002", "Laki-laki", "1960-11-22", "01", "02", 65, true, "Tunawicara", "Aktif", "Semarang", "Kristen Protestan", "Kawin", "Wiraswasta", "WNI", "SMA/Sederajat", "A", "Kepala Keluarga", "PKH, BPNT")) // Lansia & Disabilitas
            wargaDao.insertWarga(Warga(0, "Ahmad Saputra", "3275021203050001", "3275022010150004", "Laki-laki", "2005-03-12", "02", "02", 21, false, "", "Aktif", "Bandung", "Islam", "Belum Kawin", "Pelajar/Mahasiswa", "WNI", "SMA/Sederajat", "O", "Anak", ""))
            wargaDao.insertWarga(Warga(0, "Ayu Lestari", "3275025510000002", "3275022010150004", "Perempuan", "2000-10-15", "02", "02", 25, true, "Tunanetra", "Aktif", "Surabaya", "Islam", "Belum Kawin", "Karyawan Swasta", "WNI", "Diploma/S1/Sederajat", "B", "Anak", "")) // Disabilitas
            wargaDao.insertWarga(Warga(0, "Kakek Sujono", "3275021804450003", "3275022010150055", "Laki-laki", "1945-04-18", "02", "02", 81, false, "", "Aktif", "Yogyakarta", "Islam", "Kawin", "Pensiunan", "WNI", "SMP/Sederajat", "AB", "Kepala Keluarga", "")) // Lansia
            wargaDao.insertWarga(Warga(0, "Balita Gemas", "3275011508240003", "3275011212880001", "Laki-laki", "2024-08-15", "01", "02", 1, false, "", "Aktif", "Bekasi", "Islam", "Belum Kawin", "Belum/Tidak Bekerja", "WNI", "Tidak/Belum Sekolah", "Tidak Tahu", "Anak", ""))

            // Seed initial mutations
            mutasiDao.insertMutasi(MutasiWarga(0, "Kelahiran", "2026-05-20", "Bayi Rezky", "", "3275011212880001", "Lahir sehat dengan berat badan 3.2 Kg", "rt01"))
            mutasiDao.insertMutasi(MutasiWarga(0, "Kematian", "2026-04-12", "Almarhum Mbah Karto", "3275010603400009", "3275011212880033", "Meninggal dunia karena sakit usia tua di rumah", "rt01"))

            // Seed announcements
            announcementDao.insertAnnouncement(
                Announcement(
                    0,
                    "Rapat Koordinasi RT-RW Kelurahan",
                    "Diharapkan seluruh RT dan RW hadir dalam rapat koordinasi bulanan membahas tata laksana keamanan lingkungan, bertempat di Pendopo Kantor Kelurahan pada Sabtu besok jam 09.00 WIB.",
                    "2026-06-04",
                    isUrgent = true,
                    wasSentToWhatsapp = true,
                    whatsappStatus = "Terkirim ke semua RT/RW"
                )
            )
            announcementDao.insertAnnouncement(
                Announcement(
                    0,
                    "Program Fogging Pencegahan DBD",
                    "Akan diadakan fogging serentak di lingkungan RW 02 untuk memberantas nyamuk DBD. Mohon warga mengamankan makanan dan hewan peliharaan.",
                    "2026-05-28",
                    isUrgent = false,
                    wasSentToWhatsapp = true,
                    whatsappStatus = "Terkirim ke semua RT/RW"
                )
            )

            // Seed activity log
            logDao.insertLog(ActivityLog(0, "system", "System", "Sistem Kependudukan RT-RW berhasil diinisialisasi"))
        }
    }
}
