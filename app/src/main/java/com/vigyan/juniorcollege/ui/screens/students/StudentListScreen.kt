package com.vigyan.juniorcollege.ui.screens.students

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.local.entity.StudentStatus
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.ui.theme.VigyanGreen
import com.vigyan.juniorcollege.ui.theme.VigyanOrange
import com.vigyan.juniorcollege.util.simpleFactory

@Composable
fun StudentListScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: StudentListViewModel = viewModel(
        factory = simpleFactory { StudentListViewModel(app.studentRepository) }
    )
    val students by viewModel.students.collectAsState()
    val query by viewModel.queryState.collectAsState()

    AppScaffold(
        navController = navController,
        title = "Student Management",
        currentRoute = Routes.STUDENT_LIST,
        userName = userName,
        onLogout = onLogout,
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.STUDENT_ADD) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search by name, admission no, roll no, mobile") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentRow(student) {
                            navController.navigate(Routes.studentProfile(student.id))
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: StudentEntity, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.studentName, fontWeight = FontWeight.SemiBold)
                Text(
                    "Adm No: ${student.admissionNo}  •  Roll: ${student.rollNo}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(student.status.name) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (student.status == StudentStatus.ACTIVE) VigyanGreen.copy(alpha = 0.15f) else VigyanOrange.copy(alpha = 0.15f)
                )
            )
        }
    }
}
