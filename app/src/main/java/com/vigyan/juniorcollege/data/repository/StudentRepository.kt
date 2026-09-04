package com.vigyan.juniorcollege.data.repository

import com.vigyan.juniorcollege.data.local.dao.StudentDao
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

class StudentRepository(private val studentDao: StudentDao) {

    fun observeAllActive(): Flow<List<StudentEntity>> = studentDao.observeAllActive()
    fun observeRecycleBin(): Flow<List<StudentEntity>> = studentDao.observeRecycleBin()
    fun observeById(id: Long): Flow<StudentEntity?> = studentDao.observeById(id)
    fun search(query: String): Flow<List<StudentEntity>> = studentDao.search(query)

    fun filter(classId: Long?, batchId: Long?, sectionId: Long?, status: String?) =
        studentDao.filter(classId, batchId, sectionId, status)

    fun totalCount(): Flow<Int> = studentDao.observeTotalCount()
    fun activeCount(): Flow<Int> = studentDao.observeActiveCount()
    fun inactiveCount(): Flow<Int> = studentDao.observeInactiveCount()
    fun boysCount(): Flow<Int> = studentDao.observeBoysCount()
    fun girlsCount(): Flow<Int> = studentDao.observeGirlsCount()
    fun classWiseCounts() = studentDao.observeClassWiseCounts()
    fun batchWiseCounts() = studentDao.observeBatchWiseCounts()

    suspend fun isAdmissionNoTaken(admissionNo: String): Boolean =
        studentDao.findAdmissionNo(admissionNo) != null

    suspend fun addStudent(student: StudentEntity): Long = studentDao.insert(student)

    suspend fun updateStudent(student: StudentEntity) =
        studentDao.update(student.copy(updatedAt = System.currentTimeMillis()))

    suspend fun getById(id: Long): StudentEntity? = studentDao.getById(id)

    suspend fun softDelete(id: Long) = studentDao.softDelete(id)
    suspend fun restore(id: Long) = studentDao.restore(id)
    suspend fun hardDelete(id: Long) = studentDao.hardDelete(id)
}
