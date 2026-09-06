package com.vigyan.juniorcollege.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentListViewModel(
    private val repository: StudentRepository,
    filter: String = "all"
) : ViewModel() {

    private val query = MutableStateFlow("")
    fun onQueryChange(value: String) { query.value = value }

    val filterLabel: String = when (filter) {
        "active" -> "Active Students"
        "inactive" -> "Inactive Students"
        "boys" -> "Boys"
        "girls" -> "Girls"
        else -> "All Students"
    }

    private val statusFilter: String? = when (filter) {
        "active" -> "ACTIVE"
        "inactive" -> "INACTIVE"
        else -> null
    }
    private val genderFilter: String? = when (filter) {
        "boys" -> "MALE"
        "girls" -> "FEMALE"
        else -> null
    }

    val students: StateFlow<List<StudentEntity>> = query
        .debounce(200)
        .flatMapLatest { q ->
            // A typed search always searches everyone by name/admission/roll/mobile;
            // the dashboard filter only applies to the unfiltered baseline list.
            if (q.isBlank()) repository.filterForList(statusFilter, genderFilter) else repository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queryState: StateFlow<String> = query

    fun deleteStudent(id: Long) {
        viewModelScope.launch { repository.softDelete(id) }
    }
}
