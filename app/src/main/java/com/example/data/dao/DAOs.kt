package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WargaDao {
    @Query("SELECT * FROM warga ORDER BY nama ASC")
    fun getAllWarga(): Flow<List<Warga>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarga(warga: Warga): Long

    @Update
    suspend fun updateWarga(warga: Warga)

    @Delete
    suspend fun deleteWarga(warga: Warga)

    @Query("SELECT * FROM warga WHERE id = :id")
    suspend fun getWargaById(id: Int): Warga?
}

@Dao
interface MutasiDao {
    @Query("SELECT * FROM mutas_warga ORDER BY tanggal DESC, id DESC")
    fun getAllMutasi(): Flow<List<MutasiWarga>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutasi(mutasi: MutasiWarga): Long
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_account ORDER BY role ASC, username ASC")
    fun getAllAccounts(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_account WHERE username = :username LIMIT 1")
    suspend fun getAccountByUsername(username: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccount)

    @Update
    suspend fun updateAccount(account: UserAccount)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_log")
    suspend fun clearLogs()
}

@Dao
interface DatabaseBackupDao {
    @Query("SELECT * FROM database_backup ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<DatabaseBackup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: DatabaseBackup)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcement ORDER BY id DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement): Long

    @Update
    suspend fun updateAnnouncement(announcement: Announcement)
}
