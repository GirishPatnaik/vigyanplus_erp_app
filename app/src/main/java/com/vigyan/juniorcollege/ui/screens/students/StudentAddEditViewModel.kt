package com.vigyan.juniorcollege.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.*
import com.vigyan.juniorcollege.data.repository.MasterRepository
import com.vigyan.juniorcollege.data.repository.StudentRepository
import com.vigyan.juniorcollege.util.AadhaarCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentFormState(
    val id: Long = 0,
    val photoUri: String? = null,
    val admissionNo: String = "",
    val rollNo: String = "",
    val studentName: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val dob: String = "",
    val gender: Gender = Gender.MALE,
    val address: String = "",
    val mobileNo1: String = "",
    val mobileNo2: String = "",
    val whatsappNo: String = "",
    val boardName: String = "CHSE Odisha",
    val aadhaarNo: String = "",
    val courseId: Long? = null,
    val streamId: Long? = null,
    val classId: Long? = null,
    val batchId: Long? = null,
    val sectionId: Long? = null,
    val academicYearId: Long? = null,
    val status: StudentStatus = StudentStatus.ACTIVE,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

class StudentAddEditViewModel(
    private val studentRepository: StudentRepository,
    private val masterRepository: MasterRepository,
    private val editingId: Long? = null
) : ViewModel() {

    private val _state = MutableStateFlow(StudentFormState(isEditMode = editingId != null))
    val state: StateFlow<StudentFormState> = _state

    val courses = masterRepository.courses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStreams = masterRepository.allStreams().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = masterRepository.classes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val batches = masterRepository.batches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sections = masterRepository.sections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val academicYears = masterRepository.academicYears().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        editingId?.let { id ->
            viewModelScope.launch {
                studentRepository.getById(id)?.let { s ->
                    _state.value = StudentFormState(
                        id = s.id, photoUri = s.photoUri, admissionNo = s.admissionNo, rollNo = s.rollNo,
                        studentName = s.studentName, fatherName = s.fatherName, motherName = s.motherName,
                        dob = s.dob, gender = s.gender, address = s.address, mobileNo1 = s.mobileNo1,
                        mobileNo2 = s.mobileNo2, whatsappNo = s.whatsappNo, boardName = s.boardName,
                        aadhaarNo = AadhaarCrypto.decrypt(s.aadhaarEncrypted) ?: "",
                        courseId = s.courseId, streamId = s.streamId, classId = s.classId,
                        batchId = s.batchId, sectionId = s.sectionId, academicYearId = s.academicYearId,
                        status = s.status, isEditMode = true
                    )
                }
            }
        }
    }

    fun update(transform: (StudentFormState) -> StudentFormState) {
        _state.value = transform(_state.value).copy(error = null)
    }

    fun save() {
        val s = _state.value
        if (s.studentName.isBlank()) { _state.value = s.copy(error = "Student name is required"); return }
        if (s.admissionNo.isBlank()) { _state.value = s.copy(error = "Admission number is required"); return }
        val aadhaarDigits = s.aadhaarNo.filter { it.isDigit() }
        if (aadhaarDigits.isNotEmpty() && aadhaarDigits.length != 12) {
            _state.value = s.copy(error = "Aadhaar number must be 12 digits"); return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            try {
                val aadhaarEncrypted = AadhaarCrypto.encrypt(s.aadhaarNo)
                if (!s.isEditMode) {
                    if (studentRepository.isAdmissionNoTaken(s.admissionNo)) {
                        _state.value = s.copy(isSaving = false, error = "Admission number already exists")
                        return@launch
                    }
                    studentRepository.addStudent(
                        StudentEntity(
                            photoUri = s.photoUri, admissionNo = s.admissionNo, rollNo = s.rollNo,
                            studentName = s.studentName, fatherName = s.fatherName, motherName = s.motherName,
                            dob = s.dob, gender = s.gender, address = s.address, mobileNo1 = s.mobileNo1,
                            mobileNo2 = s.mobileNo2, whatsappNo = s.whatsappNo, boardName = s.boardName,
                            aadhaarEncrypted = aadhaarEncrypted,
                            courseId = s.courseId, streamId = s.streamId, classId = s.classId,
                            batchId = s.batchId, sectionId = s.sectionId, academicYearId = s.academicYearId,
                            status = s.status
                        )
                    )
                } else {
                    studentRepository.updateStudent(
                        StudentEntity(
                            id = s.id, photoUri = s.photoUri, admissionNo = s.admissionNo, rollNo = s.rollNo,
                            studentName = s.studentName, fatherName = s.fatherName, motherName = s.motherName,
                            dob = s.dob, gender = s.gender, address = s.address, mobileNo1 = s.mobileNo1,
                            mobileNo2 = s.mobileNo2, whatsappNo = s.whatsappNo, boardName = s.boardName,
                            aadhaarEncrypted = aadhaarEncrypted,
                            courseId = s.courseId, streamId = s.streamId, classId = s.classId,
                            batchId = s.batchId, sectionId = s.sectionId, academicYearId = s.academicYearId,
                            status = s.status
                        )
                    )
                }
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = "Save failed: ${e.message}")
            }
        }
    }
}
