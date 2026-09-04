package com.vigyan.juniorcollege.ui.screens.reports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
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
import com.vigyan.juniorcollege.data.repository.CsvRepository
import com.vigyan.juniorcollege.data.repository.StudentRepository
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReportsViewModel(
    studentRepository: StudentRepository,
    private val csvRepository: CsvRepository
) : ViewModel() {
    val students: StateFlow<List<StudentEntity>> = studentRepository.observeAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportCsv(uri: Uri) {
        viewModelScope.launch { csvRepository.exportStudents(uri, students.value) }
    }
}

@Composable
fun ReportsScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: ReportsViewModel = viewModel(
        factory = simpleFactory { ReportsViewModel(app.studentRepository, app.csvRepository) }
    )
    val students by viewModel.students.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(it) } }

    val genderCounts = students.groupingBy { it.gender }.eachCount()
    val statusCounts = students.groupingBy { it.status }.eachCount()

    AppScaffold(
        navController = navController,
        title = "Reports",
        currentRoute = Routes.REPORTS,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Student Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { exportLauncher.launch("students_report.csv") }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export CSV")
                }
            }

            Spacer(Modifier.height(16.dp))
            ReportCard("Total Students", students.size.toString())

            Spacer(Modifier.height(12.dp))
            Text("Gender-wise", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                items(genderCounts.entries.toList()) { (gender, count) ->
                    ReportRow(gender.name, count.toString())
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Status-wise", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(statusCounts.entries.toList()) { (status, count) ->
                    ReportRow(status.name, count.toString())
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Attendance reports (daily / date range / class-wise / student-wise) are available from each " +
                    "student's profile and the Attendance screen's history view.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ReportCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value)
    }
}
