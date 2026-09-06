package com.vigyan.juniorcollege.data.local.dao

import androidx.room.*
import com.vigyan.juniorcollege.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun observeForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun observeHistoryForStudent(studentId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :startDate AND :endDate")
    fun observeForDateRange(startDate: String, endDate: String): Flow<List<AttendanceEntity>>

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date")
    fun observeTotalMarkedForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND status = 'PRESENT'")
    fun observePresentForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND status = 'ABSENT'")
    fun observeAbsentForDate(date: String): Flow<Int>

    @Query(
        """SELECT COUNT(*) FROM attendance
           WHERE studentId = :studentId AND status = 'PRESENT'"""
    )
    suspend fun countPresentForStudent(studentId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId")
    suspend fun countTotalForStudent(studentId: Long): Int

    @Delete
    suspend fun delete(entity: AttendanceEntity)

    // --- Cloud sync support ---

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun findByStudentAndDate(studentId: Long, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): AttendanceEntity?

    @Query("SELECT * FROM attendance")
    suspend fun getAllForSync(): List<AttendanceEntity>

    @Query("UPDATE attendance SET remoteId = :remoteId WHERE id = :attendanceId")
    suspend fun setRemoteId(attendanceId: Long, remoteId: String)
}
