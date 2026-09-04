package com.vigyan.juniorcollege.ui.screens.students

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.repository.StudentRepository
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecycleBinViewModel(private val repository: StudentRepository) : ViewModel() {
    val deletedStudents: StateFlow<List<StudentEntity>> = repository.observeRecycleBin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(id: Long) { viewModelScope.launch { repository.restore(id) } }
    fun deleteForever(id: Long) { viewModelScope.launch { repository.hardDelete(id) } }
}

@Composable
fun RecycleBinScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: RecycleBinViewModel = viewModel(
        factory = simpleFactory { RecycleBinViewModel(app.studentRepository) }
    )
    val students by viewModel.deletedStudents.collectAsState()
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    AppScaffold(
        navController = navController,
        title = "Recycle Bin",
        currentRoute = Routes.RECYCLE_BIN,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        if (students.isEmpty()) {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Recycle bin is empty")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.id }) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.studentName, fontWeight = FontWeight.SemiBold)
                                Text("Adm No: ${s.admissionNo}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.restore(s.id) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = { confirmDeleteId = s.id }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever")
                            }
                        }
                    }
                }
            }
        }

        confirmDeleteId?.let { id ->
            AlertDialog(
                onDismissRequest = { confirmDeleteId = null },
                title = { Text("Permanently Delete") },
                text = { Text("This cannot be undone. Delete this student record permanently?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteForever(id); confirmDeleteId = null }) { Text("Delete Forever") }
                },
                dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") } }
            )
        }
    }
}
