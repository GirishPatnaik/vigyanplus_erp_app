package com.vigyan.juniorcollege.data.repository

import com.vigyan.juniorcollege.data.local.dao.AttendanceDao
import com.vigyan.juniorcollege.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    fun observeForDate(date: String): Flow<List<AttendanceEntity>> = attendanceDao.observeForDate(date)
    fun observeHistoryForStudent(studentId: Long) = attendanceDao.observeHistoryForStudent(studentId)
    fun observeForDateRange(start: String, end: String) = attendanceDao.observeForDateRange(start, end)

    fun observeTotalForDate(date: String) = attendanceDao.observeTotalMarkedForDate(date)
    fun observePresentForDate(date: String) = attendanceDao.observePresentForDate(date)
    fun observeAbsentForDate(date: String) = attendanceDao.observeAbsentForDate(date)

    suspend fun saveAttendance(records: List<AttendanceEntity>) = attendanceDao.upsertAll(records)

    suspend fun attendancePercentage(studentId: Long): Double {
        val total = attendanceDao.countTotalForStudent(studentId)
        if (total == 0) return 0.0
        val present = attendanceDao.countPresentForStudent(studentId)
        return (present.toDouble() / total.toDouble()) * 100.0
    }
}
