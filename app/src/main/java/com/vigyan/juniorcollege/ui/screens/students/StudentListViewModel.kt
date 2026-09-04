package com.vigyan.juniorcollege.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentListViewModel(private val repository: StudentRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    fun onQueryChange(value: String) { query.value = value }

    val students: StateFlow<List<StudentEntity>> = query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeAllActive() else repository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queryState: StateFlow<String> = query

    fun deleteStudent(id: Long) {
        viewModelScope.launch { repository.softDelete(id) }
    }
}
