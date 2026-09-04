package com.vigyan.juniorcollege.data.local.dao

import androidx.room.*
import com.vigyan.juniorcollege.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterDao {

    // Academic Year
    @Query("SELECT * FROM academic_years ORDER BY label DESC")
    fun observeAcademicYears(): Flow<List<AcademicYearEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAcademicYear(item: AcademicYearEntity): Long
    @Delete
    suspend fun deleteAcademicYear(item: AcademicYearEntity)

    // Course
    @Query("SELECT * FROM courses ORDER BY name")
    fun observeCourses(): Flow<List<CourseEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourse(item: CourseEntity): Long
    @Delete
    suspend fun deleteCourse(item: CourseEntity)

    // Stream
    @Query("SELECT * FROM streams WHERE courseId = :courseId ORDER BY name")
    fun observeStreamsForCourse(courseId: Long): Flow<List<StreamEntity>>
    @Query("SELECT * FROM streams ORDER BY name")
    fun observeAllStreams(): Flow<List<StreamEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStream(item: StreamEntity): Long
    @Delete
    suspend fun deleteStream(item: StreamEntity)

    // Class
    @Query("SELECT * FROM college_classes ORDER BY name")
    fun observeClasses(): Flow<List<ClassEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClass(item: ClassEntity): Long
    @Delete
    suspend fun deleteClass(item: ClassEntity)

    // Batch
    @Query("SELECT * FROM batches ORDER BY name DESC")
    fun observeBatches(): Flow<List<BatchEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(item: BatchEntity): Long
    @Delete
    suspend fun deleteBatch(item: BatchEntity)

    // Section
    @Query("SELECT * FROM sections ORDER BY name")
    fun observeSections(): Flow<List<SectionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(item: SectionEntity): Long
    @Delete
    suspend fun deleteSection(item: SectionEntity)

    // College details (singleton row)
    @Query("SELECT * FROM college_details WHERE id = 1")
    fun observeCollegeDetails(): Flow<CollegeDetailsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollegeDetails(item: CollegeDetailsEntity)
}
