package com.vigyan.juniorcollege.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.repository.AttendanceRepository
import com.vigyan.juniorcollege.data.repository.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val totalStudents: Int = 0,
    val activeStudents: Int = 0,
    val inactiveStudents: Int = 0,
    val boys: Int = 0,
    val girls: Int = 0,
    val todayTotal: Int = 0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val todayAttendancePercent: Double = 0.0
)

class DashboardViewModel(
    studentRepository: StudentRepository,
    attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val state: StateFlow<DashboardUiState> = combine(
        listOf(
            studentRepository.totalCount(),
            studentRepository.activeCount(),
            studentRepository.inactiveCount(),
            studentRepository.boysCount(),
            studentRepository.girlsCount(),
            attendanceRepository.observeTotalForDate(today),
            attendanceRepository.observePresentForDate(today),
            attendanceRepository.observeAbsentForDate(today)
        )
    ) { values ->
        val total = values[0]
        val active = values[1]
        val inactive = values[2]
        val boys = values[3]
        val girls = values[4]
        val todayTotal = values[5]
        val todayPresent = values[6]
        val todayAbsent = values[7]
        DashboardUiState(
            totalStudents = total,
            activeStudents = active,
            inactiveStudents = inactive,
            boys = boys,
            girls = girls,
            todayTotal = todayTotal,
            todayPresent = todayPresent,
            todayAbsent = todayAbsent,
            todayAttendancePercent = if (todayTotal > 0) (todayPresent.toDouble() / todayTotal) * 100.0 else 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
