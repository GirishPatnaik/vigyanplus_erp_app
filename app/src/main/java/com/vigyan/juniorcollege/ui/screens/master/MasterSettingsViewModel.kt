package com.vigyan.juniorcollege.ui.screens.master

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.*
import com.vigyan.juniorcollege.data.repository.MasterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MasterSettingsViewModel(private val repository: MasterRepository) : ViewModel() {

    val academicYears = repository.academicYears().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val courses = repository.courses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val streams = repository.allStreams().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = repository.classes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val batches = repository.batches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sections = repository.sections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val collegeDetails = repository.collegeDetails().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addAcademicYear(label: String) { viewModelScope.launch { repository.upsertAcademicYear(AcademicYearEntity(label = label)) } }
    fun deleteAcademicYear(item: AcademicYearEntity) { viewModelScope.launch { repository.deleteAcademicYear(item) } }

    fun addCourse(name: String) { viewModelScope.launch { repository.upsertCourse(CourseEntity(name = name)) } }
    fun deleteCourse(item: CourseEntity) { viewModelScope.launch { repository.deleteCourse(item) } }

    fun addStream(name: String, courseId: Long) { viewModelScope.launch { repository.upsertStream(StreamEntity(name = name, courseId = courseId)) } }
    fun deleteStream(item: StreamEntity) { viewModelScope.launch { repository.deleteStream(item) } }

    fun addClass(name: String) { viewModelScope.launch { repository.upsertClass(ClassEntity(name = name)) } }
    fun deleteClass(item: ClassEntity) { viewModelScope.launch { repository.deleteClass(item) } }

    fun addBatch(name: String) { viewModelScope.launch { repository.upsertBatch(BatchEntity(name = name)) } }
    fun deleteBatch(item: BatchEntity) { viewModelScope.launch { repository.deleteBatch(item) } }

    fun addSection(name: String) { viewModelScope.launch { repository.upsertSection(SectionEntity(name = name)) } }
    fun deleteSection(item: SectionEntity) { viewModelScope.launch { repository.deleteSection(item) } }

    fun saveCollegeDetails(details: CollegeDetailsEntity) { viewModelScope.launch { repository.upsertCollegeDetails(details) } }
}
