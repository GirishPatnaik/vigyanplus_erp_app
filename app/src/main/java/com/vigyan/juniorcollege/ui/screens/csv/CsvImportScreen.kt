package com.vigyan.juniorcollege.ui.screens.csv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.vigyan.juniorcollege.data.repository.CsvImportSummary
import com.vigyan.juniorcollege.data.repository.CsvRepository
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.ui.theme.VigyanGreen
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CsvImportViewModel(private val repository: CsvRepository) : ViewModel() {
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _summary = MutableStateFlow<CsvImportSummary?>(null)
    val summary: StateFlow<CsvImportSummary?> = _summary

    fun downloadTemplate(uri: Uri) {
        viewModelScope.launch { repository.writeTemplate(uri) }
    }

    fun importFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isImporting.value = true
            _summary.value = repository.importFromCsv(uri, fileName)
            _isImporting.value = false
        }
    }
}

@Composable
fun CsvImportScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: CsvImportViewModel = viewModel(
        factory = simpleFactory { CsvImportViewModel(app.csvRepository) }
    )
    val isImporting by viewModel.isImporting.collectAsState()
    val summary by viewModel.summary.collectAsState()

    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.downloadTemplate(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importFile(it, "import.csv") } }

    AppScaffold(
        navController = navController,
        title = "CSV Import",
        currentRoute = Routes.CSV_IMPORT,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Bulk Student Import", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Download the CSV template, fill in student details, then import it back. The template's sample row " +
                    "uses your actual Master Settings values (Academic Year, Course, Stream, Class, Batch, Section) — " +
                    "delete that row before adding real students, and make sure each of those six columns matches an " +
                    "exact name from Master Settings, or the row will be skipped with an error below.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { templateLauncher.launch("student_import_template.csv") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Get Template")
                }
                Button(
                    onClick = { importLauncher.launch("text/*") },
                    enabled = !isImporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import CSV")
                }
            }

            if (isImporting) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            summary?.let { s ->
                Spacer(Modifier.height(20.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Import Summary", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SummaryStat("Total", s.totalRows.toString())
                            SummaryStat("Success", s.successful.toString(), VigyanGreen)
                            SummaryStat("Failed", s.failed.toString(), MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (s.errors.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Errors", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(s.errors) { err ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Row ${err.rowNumber}: ${err.reason}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
