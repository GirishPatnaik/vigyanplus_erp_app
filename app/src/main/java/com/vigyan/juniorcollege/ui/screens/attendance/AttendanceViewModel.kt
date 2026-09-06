package com.vigyan.juniorcollege.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.AttendanceEntity
import com.vigyan.juniorcollege.data.local.entity.AttendanceStatus
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.repository.AttendanceRepository
import com.vigyan.juniorcollege.data.repository.MasterRepository
import com.vigyan.juniorcollege.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AttendanceUiState(
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val classId: Long? = null,
    val batchId: Long? = null,
    val sectionId: Long? = null,
    val streamId: Long? = null,
    // Present unless explicitly marked otherwise — students only need to be
    // tapped when they're ABSENT, matching how attendance is actually taken.
    val markedStatus: Map<Long, AttendanceStatus> = emptyMap(),
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class AttendanceViewModel(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    masterRepository: MasterRepository,
    private val markedByUserId: Long?
) : ViewModel() {

    val classes = masterRepository.classes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val batches = masterRepository.batches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sections = masterRepository.sections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val streams = masterRepository.allStreams().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(AttendanceUiState())
    val state: StateFlow<AttendanceUiState> = _state

    val filteredStudents: StateFlow<List<StudentEntity>> = _state
        .flatMapLatest { s ->
            studentRepository.filter(s.classId, s.batchId, s.sectionId, s.streamId, "ACTIVE")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Whenever the filtered roster changes (new class/batch/section/stream
        // picked, or the screen first loads a list), default every student who
        // doesn't already have an explicit mark to PRESENT. Existing marks the
        // user already made are preserved — this only fills in the gaps.
        viewModelScope.launch {
            filteredStudents.collect { students ->
                val current = _state.value.markedStatus
                val defaults = students
                    .filter { it.id !in current }
                    .associate { it.id to AttendanceStatus.PRESENT }
                if (defaults.isNotEmpty()) {
                    _state.value = _state.value.copy(markedStatus = current + defaults)
                }
            }
        }
    }

    fun setClass(id: Long?) { _state.value = _state.value.copy(classId = id) }
    fun setBatch(id: Long?) { _state.value = _state.value.copy(batchId = id) }
    fun setSection(id: Long?) { _state.value = _state.value.copy(sectionId = id) }
    fun setStream(id: Long?) { _state.value = _state.value.copy(streamId = id) }

    fun mark(studentId: Long, status: AttendanceStatus) {
        _state.value = _state.value.copy(markedStatus = _state.value.markedStatus + (studentId to status))
    }

    fun markAllPresent(studentIds: List<Long>) {
        val map = studentIds.associateWith { AttendanceStatus.PRESENT }
        _state.value = _state.value.copy(markedStatus = _state.value.markedStatus + map)
    }

    fun markAllAbsent(studentIds: List<Long>) {
        val map = studentIds.associateWith { AttendanceStatus.ABSENT }
        _state.value = _state.value.copy(markedStatus = _state.value.markedStatus + map)
    }

    fun saveAttendance() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            val records = s.markedStatus.map { (studentId, status) ->
                AttendanceEntity(
                    studentId = studentId, date = s.date, status = status,
                    classId = s.classId, batchId = s.batchId, markedByUserId = markedByUserId
                )
            }
            attendanceRepository.saveAttendance(records)
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }
}
