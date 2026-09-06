package com.vigyan.juniorcollege.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
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
import com.vigyan.juniorcollege.data.repository.SyncRepository
import com.vigyan.juniorcollege.data.repository.SyncResult
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.util.SupabaseConfig
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncViewModel(private val repository: SyncRepository) : ViewModel() {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastResult = MutableStateFlow<SyncResult?>(null)
    val lastResult: StateFlow<SyncResult?> = _lastResult

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncAll()
            _lastResult.value = result
            if (result.success) _lastSyncedAt.value = System.currentTimeMillis()
            _isSyncing.value = false
        }
    }
}

@Composable
fun SyncScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: SyncViewModel = viewModel(
        factory = simpleFactory { SyncViewModel(app.syncRepository) }
    )
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()

    AppScaffold(
        navController = navController,
        title = "Cloud Sync",
        currentRoute = Routes.CLOUD_SYNC,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text("Sync with other devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pushes your changes (students, attendance, academic structure) to the cloud and pulls down anything " +
                    "new from other devices signed into the same college. Needs an internet connection — tap this when " +
                    "you have signal.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!SupabaseConfig.isConfigured) {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "Cloud sync isn't set up yet. Add your Supabase project URL and key in SupabaseConfig.kt to enable this.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::syncNow,
                enabled = !isSyncing && SupabaseConfig.isConfigured,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("Syncing…")
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Now")
                }
            }

            lastSyncedAt?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Last synced: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            lastResult?.let { result ->
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (result.success) "Sync successful" else "Sync failed",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(result.message, style = MaterialTheme.typography.bodySmall)
                        if (result.success) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${result.studentsSynced} student record(s) and ${result.attendanceSynced} attendance record(s) processed.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
