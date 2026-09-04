package com.vigyan.juniorcollege.data.repository

import com.vigyan.juniorcollege.data.local.dao.MasterDao
import com.vigyan.juniorcollege.data.local.entity.*

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
        upsertClass(ClassEntity(name = "+2 First Year"))
        upsertClass(ClassEntity(name = "+2 Second Year"))
        upsertSection(SectionEntity(name = "A"))
        upsertSection(SectionEntity(name = "B"))
        val scienceId = upsertCourse(CourseEntity(name = "Science"))
        val commerceId = upsertCourse(CourseEntity(name = "Commerce"))
        upsertStream(StreamEntity(name = "PCM", courseId = scienceId))
        upsertStream(StreamEntity(name = "PCB", courseId = scienceId))
        upsertStream(StreamEntity(name = "CBA", courseId = commerceId))
        upsertAcademicYear(AcademicYearEntity(label = "2025-2026", isCurrent = true))
        upsertCollegeDetails(CollegeDetailsEntity())
    }
}
