package com.vigyan.juniorcollege.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.vigyan.juniorcollege.data.local.entity.AttendanceStatus
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.components.LabeledDropdown
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.ui.theme.VigyanGreen
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.first

@Composable
fun AttendanceScreen(navController: NavController, userName: String, userId: Long?, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: AttendanceViewModel = viewModel(
        factory = simpleFactory {
            AttendanceViewModel(app.studentRepository, app.attendanceRepository, app.masterRepository, userId)
        }
    )
    val state by viewModel.state.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val students by viewModel.filteredStudents.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) snackbarHostState.showSnackbar("Attendance saved for ${state.date}")
    }

    AppScaffold(
        navController = navController,
        title = "Attendance",
        currentRoute = Routes.ATTENDANCE_MARK,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Date: ${state.date}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = "Class", options = classes, selectedId = state.classId,
                    idOf = { it.id }, labelOf = { it.name },
                    onSelected = viewModel::setClass, modifier = Modifier.weight(1f)
                )
                LabeledDropdown(
                    label = "Batch", options = batches, selectedId = state.batchId,
                    idOf = { it.id }, labelOf = { it.name },
                    onSelected = viewModel::setBatch, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            LabeledDropdown(
                label = "Section", options = sections, selectedId = state.sectionId,
                idOf = { it.id }, labelOf = { it.name },
                onSelected = viewModel::setSection, modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.markAllPresent(students.map { it.id }) }) {
                    Text("Mark All Present")
                }
                OutlinedButton(onClick = { viewModel.markAllAbsent(students.map { it.id }) }) {
                    Text("Mark All Absent")
                }
            }

            Spacer(Modifier.height(12.dp))
            if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select class/batch/section to load students")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        AttendanceRow(
                            student = student,
                            status = state.markedStatus[student.id],
                            onMark = { status -> viewModel.mark(student.id, status) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                Button(
                    onClick = viewModel::saveAttendance,
                    enabled = !state.isSaving && state.markedStatus.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Save Attendance (${state.markedStatus.size}/${students.size})")
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun AttendanceRow(student: StudentEntity, status: AttendanceStatus?, onMark: (AttendanceStatus) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.studentName, fontWeight = FontWeight.SemiBold)
                Text("Roll: ${student.rollNo}", style = MaterialTheme.typography.bodySmall)
            }
            FilterChip(
                selected = status == AttendanceStatus.PRESENT,
                onClick = { onMark(AttendanceStatus.PRESENT) },
                label = { Text("Present") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VigyanGreen.copy(alpha = 0.25f))
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = status == AttendanceStatus.ABSENT,
                onClick = { onMark(AttendanceStatus.ABSENT) },
                label = { Text("Absent") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
            )
        }
    }
}
