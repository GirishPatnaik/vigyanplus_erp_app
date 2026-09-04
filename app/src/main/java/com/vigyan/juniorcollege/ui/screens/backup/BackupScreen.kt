package com.vigyan.juniorcollege.ui.screens.backup

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
import com.vigyan.juniorcollege.data.repository.BackupFile
import com.vigyan.juniorcollege.data.repository.BackupRepository
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class BackupViewModel(private val repository: BackupRepository) : ViewModel() {
    private val _backups = MutableStateFlow<List<BackupFile>>(emptyList())
    val backups: StateFlow<List<BackupFile>> = _backups

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init { refresh() }

    fun refresh() { _backups.value = repository.listBackups() }

    fun createBackup() {
        viewModelScope.launch {
            repository.createManualBackup()
            refresh()
            _message.value = "Backup created successfully"
        }
    }

    fun exportTo(uri: Uri, source: BackupFile) {
        viewModelScope.launch {
            repository.exportDatabase(uri, source.file)
            _message.value = "Exported successfully"
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            repository.importDatabase(uri)
            _message.value = "Database imported. Please restart the app for changes to take effect."
        }
    }

    fun delete(backup: BackupFile) {
        repository.deleteBackup(backup)
        refresh()
    }

    fun clearMessage() { _message.value = null }
}

@Composable
fun BackupScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: BackupViewModel = viewModel(
        factory = simpleFactory { BackupViewModel(app.backupRepository) }
    )
    val backups by viewModel.backups.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExport by remember { mutableStateOf<BackupFile?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { pendingExport?.let { bf -> viewModel.exportTo(it, bf) } } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importFrom(it) } }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    AppScaffold(
        navController = navController,
        title = "Database & Backup",
        currentRoute = Routes.BACKUP,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.createBackup() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Backup Now")
                }
                OutlinedButton(onClick = { importLauncher.launch("*/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import DB")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Automatic backups run in the background on a schedule. Manual backups are stored on-device and can be exported to Downloads or shared drives.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(16.dp))
            Text("Stored Backups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (backups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No backups yet — tap \"Backup Now\" to create one")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(backups, key = { it.file.path }) { backup ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(backup.file.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(java.util.Date(backup.createdAt)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = {
                                    pendingExport = backup
                                    exportLauncher.launch(backup.file.name)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Export")
                                }
                                IconButton(onClick = { viewModel.delete(backup) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
