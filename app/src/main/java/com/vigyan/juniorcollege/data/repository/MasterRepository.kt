package com.vigyan.juniorcollege.data.repository

import com.vigyan.juniorcollege.data.local.dao.MasterDao
import com.vigyan.juniorcollege.data.local.entity.*
import kotlinx.coroutines.flow.first

class MasterRepository(private val masterDao: MasterDao) {

    fun academicYears() = masterDao.observeAcademicYears()
    suspend fun upsertAcademicYear(item: AcademicYearEntity) = masterDao.upsertAcademicYear(item)
    suspend fun deleteAcademicYear(item: AcademicYearEntity) = masterDao.deleteAcademicYear(item)

    fun courses() = masterDao.observeCourses()
    suspend fun upsertCourse(item: CourseEntity) = masterDao.upsertCourse(item)
    suspend fun deleteCourse(item: CourseEntity) = masterDao.deleteCourse(item)

    fun streamsForCourse(courseId: Long) = masterDao.observeStreamsForCourse(courseId)
    fun allStreams() = masterDao.observeAllStreams()
    suspend fun upsertStream(item: StreamEntity) = masterDao.upsertStream(item)
    suspend fun deleteStream(item: StreamEntity) = masterDao.deleteStream(item)

    fun classes() = masterDao.observeClasses()
    suspend fun upsertClass(item: ClassEntity) = masterDao.upsertClass(item)
    suspend fun deleteClass(item: ClassEntity) = masterDao.deleteClass(item)

    fun batches() = masterDao.observeBatches()
    suspend fun upsertBatch(item: BatchEntity) = masterDao.upsertBatch(item)
    suspend fun deleteBatch(item: BatchEntity) = masterDao.deleteBatch(item)

    fun sections() = masterDao.observeSections()
    suspend fun upsertSection(item: SectionEntity) = masterDao.upsertSection(item)
    suspend fun deleteSection(item: SectionEntity) = masterDao.deleteSection(item)

    fun collegeDetails() = masterDao.observeCollegeDetails()
    suspend fun upsertCollegeDetails(item: CollegeDetailsEntity) = masterDao.upsertCollegeDetails(item)

    suspend fun seedDefaults() {
        // Guarded: only seed each category the FIRST time it's ever empty.
        // Without these checks, this ran unconditionally on every app launch,
        // creating duplicate Class/Batch/Section/Course/Stream rows with new
        // IDs each time (breaking any student already saved against the
        // original IDs), and silently overwriting College Details edits.
        if (masterDao.observeClasses().first().isEmpty()) {
            upsertClass(ClassEntity(name = "+2 First Year"))
            upsertClass(ClassEntity(name = "+2 Second Year"))
        }
        if (masterDao.observeSections().first().isEmpty()) {
            upsertSection(SectionEntity(name = "A"))
            upsertSection(SectionEntity(name = "B"))
        }
        if (masterDao.observeCourses().first().isEmpty()) {
            val scienceId = upsertCourse(CourseEntity(name = "Science"))
            val commerceId = upsertCourse(CourseEntity(name = "Commerce"))
            upsertStream(StreamEntity(name = "PCM", courseId = scienceId))
            upsertStream(StreamEntity(name = "PCB", courseId = scienceId))
            upsertStream(StreamEntity(name = "CBA", courseId = commerceId))
        }
        if (masterDao.observeAcademicYears().first().isEmpty()) {
            upsertAcademicYear(AcademicYearEntity(label = "2025-2026", isCurrent = true))
        }
        if (masterDao.observeCollegeDetails().first() == null) {
            upsertCollegeDetails(CollegeDetailsEntity())
        }
    }
}
