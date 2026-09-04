package com.vigyan.juniorcollege.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.repository.AttendanceRepository
import com.vigyan.juniorcollege.data.repository.StudentRepository
import com.vigyan.juniorcollege.util.AadhaarCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudentProfileViewModel(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    studentId: Long
) : ViewModel() {

    val student: StateFlow<StudentEntity?> = studentRepository.observeById(studentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _attendancePercent = MutableStateFlow(0.0)
    val attendancePercent: StateFlow<Double> = _attendancePercent

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    private val _maskedAadhaar = MutableStateFlow("Not provided")
    val maskedAadhaar: StateFlow<String> = _maskedAadhaar

    init {
        viewModelScope.launch {
            _attendancePercent.value = attendanceRepository.attendancePercentage(studentId)
        }
        viewModelScope.launch {
            student.collect { s ->
                val decrypted = AadhaarCrypto.decrypt(s?.aadhaarEncrypted)
                _maskedAadhaar.value = if (decrypted != null) AadhaarCrypto.mask(decrypted) else "Not provided"
            }
        }
    }

    fun deleteStudent(id: Long) {
        viewModelScope.launch {
            studentRepository.softDelete(id)
            _deleted.value = true
        }
    }
}
