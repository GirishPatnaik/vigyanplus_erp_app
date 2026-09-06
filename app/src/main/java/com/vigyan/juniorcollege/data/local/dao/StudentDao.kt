package com.vigyan.juniorcollege.data.local.dao

import androidx.room.*
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: StudentEntity): Long

    @Update
    suspend fun update(student: StudentEntity)

    @Query("UPDATE students SET isDeleted = 1, updatedAt = :now WHERE id = :studentId")
    suspend fun softDelete(studentId: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE students SET isDeleted = 0, updatedAt = :now WHERE id = :studentId")
    suspend fun restore(studentId: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun hardDelete(studentId: Long)

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getById(studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    fun observeById(studentId: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY studentName")
    fun observeAllActive(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeRecycleBin(): Flow<List<StudentEntity>>

    @Query(
        """SELECT * FROM students WHERE isDeleted = 0 AND
           (studentName LIKE '%' || :query || '%'
            OR admissionNo LIKE '%' || :query || '%'
            OR rollNo LIKE '%' || :query || '%'
            OR mobileNo1 LIKE '%' || :query || '%')
           ORDER BY studentName"""
    )
    fun search(query: String): Flow<List<StudentEntity>>

    @Query(
        """SELECT * FROM students WHERE isDeleted = 0
           AND (:classId IS NULL OR classId = :classId)
           AND (:batchId IS NULL OR batchId = :batchId)
           AND (:sectionId IS NULL OR sectionId = :sectionId)
           AND (:streamId IS NULL OR streamId = :streamId)
           AND (:status IS NULL OR status = :status)
           ORDER BY studentName"""
    )
    fun filter(
        classId: Long?,
        batchId: Long?,
        sectionId: Long?,
        streamId: Long?,
        status: String?
    ): Flow<List<StudentEntity>>

    @Query(
        """SELECT * FROM students WHERE isDeleted = 0
           AND (:status IS NULL OR status = :status)
           AND (:gender IS NULL OR gender = :gender)
           ORDER BY studentName"""
    )
    fun filterForList(status: String?, gender: String?): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0 AND status = 'ACTIVE'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0 AND status = 'INACTIVE'")
    fun observeInactiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0 AND gender = 'MALE'")
    fun observeBoysCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0 AND gender = 'FEMALE'")
    fun observeGirlsCount(): Flow<Int>

    @Query(
        """SELECT classId AS groupId, COUNT(*) AS total FROM students
           WHERE isDeleted = 0 GROUP BY classId"""
    )
    fun observeClassWiseCounts(): Flow<List<GroupCount>>

    @Query(
        """SELECT batchId AS groupId, COUNT(*) AS total FROM students
           WHERE isDeleted = 0 GROUP BY batchId"""
    )
    fun observeBatchWiseCounts(): Flow<List<GroupCount>>

    @Query("SELECT admissionNo FROM students WHERE admissionNo = :admissionNo LIMIT 1")
    suspend fun findAdmissionNo(admissionNo: String): String?

    @Query("SELECT rollNo FROM students WHERE rollNo = :rollNo AND classId = :classId LIMIT 1")
    suspend fun findRollNoInClass(rollNo: String, classId: Long?): String?

    // --- Cloud sync support ---

    @Query("SELECT * FROM students WHERE admissionNo = :admissionNo LIMIT 1")
    suspend fun findByAdmissionNo(admissionNo: String): StudentEntity?

    @Query("SELECT * FROM students WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): StudentEntity?

    /** All students regardless of isDeleted — sync needs to push/pull deletions too. */
    @Query("SELECT * FROM students")
    suspend fun getAllForSync(): List<StudentEntity>

    @Query("UPDATE students SET remoteId = :remoteId WHERE id = :studentId")
    suspend fun setRemoteId(studentId: Long, remoteId: String)
}

data class GroupCount(val groupId: Long?, val total: Int)
